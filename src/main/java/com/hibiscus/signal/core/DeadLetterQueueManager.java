package com.hibiscus.signal.core;

import com.hibiscus.signal.core.entity.DeadLetterEvent;
import com.hibiscus.signal.core.entity.DeadLetterEvent.DeadLetterStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 死信队列管理器
 * 负责管理最终失败的事件，支持手动重试和问题排查
 * 
 * @author heathcetide
 */
public class DeadLetterQueueManager {
    
    private static final Logger log = LoggerFactory.getLogger(DeadLetterQueueManager.class);
    
    // 死信事件存储
    private final List<DeadLetterEvent> deadLetterEvents = new CopyOnWriteArrayList<>();
    
    // 事件处理器映射
    private final Map<String, SignalHandler> eventHandlers = new ConcurrentHashMap<>();
    
    // 定时清理任务
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
    
    // 配置参数
    private final int maxDeadLetterEvents;
    private final long eventRetentionDays;
    private final boolean enableAutoCleanup;
    
    public DeadLetterQueueManager() {
        this(10000, 30, true); // 默认保留10000个事件，30天，启用自动清理
    }
    
    public DeadLetterQueueManager(int maxDeadLetterEvents, long eventRetentionDays, boolean enableAutoCleanup) {
        this.maxDeadLetterEvents = maxDeadLetterEvents;
        this.eventRetentionDays = eventRetentionDays;
        this.enableAutoCleanup = enableAutoCleanup;
        
        if (enableAutoCleanup) {
            startCleanupTask();
        }
    }
    
    /**
     * 添加死信事件
     */
    public void addDeadLetterEvent(DeadLetterEvent event) {
        synchronized (deadLetterEvents) {
            // 检查容量限制
            if (deadLetterEvents.size() >= maxDeadLetterEvents) {
                // 移除最旧的事件
                DeadLetterEvent oldestEvent = deadLetterEvents.remove(0);
                log.warn("死信队列已满，移除最旧事件: {}", oldestEvent.getEventSummary());
            }
            
            deadLetterEvents.add(event);
            log.info("添加死信事件: {}", event.getEventSummary());
        }
    }
    
    /**
     * 注册事件处理器
     */
    public void registerEventHandler(String eventName, SignalHandler handler) {
        eventHandlers.put(eventName, handler);
        log.debug("注册事件处理器: {} -> {}", eventName, handler.getClass().getSimpleName());
    }
    
    /**
     * 手动重试死信事件
     */
    public boolean retryDeadLetterEvent(String eventId) {
        DeadLetterEvent event = findEventById(eventId);
        if (event == null) {
            log.warn("未找到死信事件: {}", eventId);
            return false;
        }
        
        if (!event.canRetry()) {
            log.warn("事件不可重试: {} - 状态: {}", eventId, event.getStatus());
            return false;
        }
        
        SignalHandler handler = eventHandlers.get(event.getEventName());
        if (handler == null) {
            log.error("未找到事件处理器: {}", event.getEventName());
            event.setFailureReason("未找到事件处理器");
            return false;
        }
        
        try {
            event.markAsRetrying();
            log.info("开始重试死信事件: {}", event.getEventSummary());
            
            // 执行事件处理
            handler.handle(event.getContext(), event.getParameters());
            
            event.markAsRetrySuccess();
            log.info("死信事件重试成功: {}", event.getEventSummary());
            return true;
            
        } catch (Exception e) {
            event.markAsRetryFailed();
            event.setErrorMessage(e.getMessage());
            event.setErrorStackTrace(getStackTrace(e));
            log.error("死信事件重试失败: {} - 错误: {}", event.getEventSummary(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 批量重试死信事件
     */
    public DeadLetterRetryResult batchRetryDeadLetterEvents(Predicate<DeadLetterEvent> filter) {
        List<DeadLetterEvent> eventsToRetry = deadLetterEvents.stream()
                .filter(filter)
                .filter(DeadLetterEvent::canRetry)
                .collect(Collectors.toList());
        
        DeadLetterRetryResult result = new DeadLetterRetryResult();
        result.setTotalEvents(eventsToRetry.size());
        
        for (DeadLetterEvent event : eventsToRetry) {
            try {
                if (retryDeadLetterEvent(event.getId())) {
                    result.incrementSuccessCount();
                } else {
                    result.incrementFailureCount();
                }
            } catch (Exception e) {
                result.incrementFailureCount();
                log.error("批量重试过程中发生异常: {}", e.getMessage(), e);
            }
        }
        
        log.info("批量重试完成: 总计{}个事件, 成功{}个, 失败{}个", 
                result.getTotalEvents(), result.getSuccessCount(), result.getFailureCount());
        
        return result;
    }
    
    /**
     * 查询死信事件
     */
    public List<DeadLetterEvent> queryDeadLetterEvents(Predicate<DeadLetterEvent> filter) {
        return deadLetterEvents.stream()
                .filter(filter)
                .collect(Collectors.toList());
    }
    
    /**
     * 根据事件名称查询
     */
    public List<DeadLetterEvent> queryByEventName(String eventName) {
        return queryDeadLetterEvents(event -> event.getEventName().equals(eventName));
    }
    
    /**
     * 根据状态查询
     */
    public List<DeadLetterEvent> queryByStatus(DeadLetterStatus status) {
        return queryDeadLetterEvents(event -> event.getStatus() == status);
    }
    
    /**
     * 根据失败原因查询
     */
    public List<DeadLetterEvent> queryByFailureReason(String failureReason) {
        return queryDeadLetterEvents(event -> event.getFailureReason().contains(failureReason));
    }
    
    /**
     * 获取死信队列统计信息
     */
    public DeadLetterQueueStats getQueueStats() {
        DeadLetterQueueStats stats = new DeadLetterQueueStats();
        stats.setTotalEvents(deadLetterEvents.size());
        stats.setPendingEvents((int) deadLetterEvents.stream()
                .filter(event -> event.getStatus() == DeadLetterStatus.PENDING)
                .count());
        stats.setRetryingEvents((int) deadLetterEvents.stream()
                .filter(event -> event.getStatus() == DeadLetterStatus.RETRYING)
                .count());
        stats.setRetrySuccessEvents((int) deadLetterEvents.stream()
                .filter(event -> event.getStatus() == DeadLetterStatus.RETRY_SUCCESS)
                .count());
        stats.setRetryFailedEvents((int) deadLetterEvents.stream()
                .filter(event -> event.getStatus() == DeadLetterStatus.RETRY_FAILED)
                .count());
        stats.setProcessedEvents((int) deadLetterEvents.stream()
                .filter(event -> event.getStatus() == DeadLetterStatus.PROCESSED)
                .count());
        
        return stats;
    }
    
    /**
     * 清理过期事件
     */
    public int cleanupExpiredEvents() {
        long cutoffTime = System.currentTimeMillis() - (eventRetentionDays * 24 * 60 * 60 * 1000L);
        
        List<DeadLetterEvent> expiredEvents = deadLetterEvents.stream()
                .filter(event -> event.getCreateTime().toInstant(java.time.ZoneOffset.UTC).toEpochMilli() < cutoffTime)
                .collect(Collectors.toList());
        
        deadLetterEvents.removeAll(expiredEvents);
        
        if (!expiredEvents.isEmpty()) {
            log.info("清理过期死信事件: {}个", expiredEvents.size());
        }
        
        return expiredEvents.size();
    }
    
    /**
     * 启动清理任务
     */
    private void startCleanupTask() {
        cleanupExecutor.scheduleWithFixedDelay(
                this::cleanupExpiredEvents,
                1, // 1小时后开始
                24, // 每24小时执行一次
                TimeUnit.HOURS
        );
        log.info("启动死信队列自动清理任务，保留天数: {}", eventRetentionDays);
    }
    
    /**
     * 关闭管理器
     */
    public void shutdown() {
        if (enableAutoCleanup) {
            cleanupExecutor.shutdown();
            try {
                if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    cleanupExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleanupExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        log.info("死信队列管理器已关闭");
    }
    
    /**
     * 根据ID查找事件
     */
    private DeadLetterEvent findEventById(String eventId) {
        return deadLetterEvents.stream()
                .filter(event -> eventId.equals(event.getId()))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 获取异常堆栈信息
     */
    private String getStackTrace(Exception error) {
        if (error == null) return "";
        
        StringBuilder sb = new StringBuilder();
        sb.append(error.toString()).append("\n");
        
        StackTraceElement[] elements = error.getStackTrace();
        int maxDepth = Math.min(elements.length, 10);
        
        for (int i = 0; i < maxDepth; i++) {
            sb.append("\tat ").append(elements[i]).append("\n");
        }
        
        if (elements.length > maxDepth) {
            sb.append("\t... ").append(elements.length - maxDepth).append(" more\n");
        }
        
        return sb.toString();
    }
    
    /**
     * 死信队列重试结果
     */
    public static class DeadLetterRetryResult {
        private int totalEvents;
        private int successCount;
        private int failureCount;
        
        public void incrementSuccessCount() { successCount++; }
        public void incrementFailureCount() { failureCount++; }
        
        // Getters and Setters
        public int getTotalEvents() { return totalEvents; }
        public void setTotalEvents(int totalEvents) { this.totalEvents = totalEvents; }
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        public int getFailureCount() { return failureCount; }
        public void setFailureCount(int failureCount) { this.failureCount = failureCount; }
    }
    
    /**
     * 死信队列统计信息
     */
    public static class DeadLetterQueueStats {
        private int totalEvents;
        private int pendingEvents;
        private int retryingEvents;
        private int retrySuccessEvents;
        private int retryFailedEvents;
        private int processedEvents;
        
        // Getters and Setters
        public int getTotalEvents() { return totalEvents; }
        public void setTotalEvents(int totalEvents) { this.totalEvents = totalEvents; }
        public int getPendingEvents() { return pendingEvents; }
        public void setPendingEvents(int pendingEvents) { this.pendingEvents = pendingEvents; }
        public int getRetryingEvents() { return retryingEvents; }
        public void setRetryingEvents(int retryingEvents) { this.retryingEvents = retryingEvents; }
        public int getRetrySuccessEvents() { return retrySuccessEvents; }
        public void setRetrySuccessEvents(int retrySuccessEvents) { this.retrySuccessEvents = retrySuccessEvents; }
        public int getRetryFailedEvents() { return retryFailedEvents; }
        public void setRetryFailedEvents(int retryFailedEvents) { this.retryFailedEvents = retryFailedEvents; }
        public int getProcessedEvents() { return processedEvents; }
        public void setProcessedEvents(int processedEvents) { this.processedEvents = processedEvents; }
    }
}
