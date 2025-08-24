package com.hibiscus.signal.core;

import com.hibiscus.signal.Signals;
import com.hibiscus.signal.config.EnhancedSignalPersistence;
import com.hibiscus.signal.config.SignalConfig;
import com.hibiscus.signal.spring.config.SignalProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 事件恢复管理器
 * 处理事件重发、恢复和补发机制
 */
@Component
public class EventRecoveryManager {

    private static final Logger log = LoggerFactory.getLogger(EventRecoveryManager.class);
    
    private final Signals signals;
    private final SignalProperties signalProperties;
    private final ConcurrentHashMap<String, EventRecoveryInfo> recoveryEvents = new ConcurrentHashMap<>();
    private final AtomicLong recoveryCounter = new AtomicLong(0);
    
    // 事件处理状态
    private volatile boolean isRecovering = false;
    private volatile long lastRecoveryTime = 0;

    public EventRecoveryManager(Signals signals, SignalProperties signalProperties) {
        this.signals = signals;
        this.signalProperties = signalProperties;
    }

    /**
     * 记录事件处理状态，用于恢复
     */
    public void recordEventProcessing(String eventId, String eventName, SignalContext context, 
                                    SignalConfig config, EventProcessingStatus status) {
        
        EventRecoveryInfo recoveryInfo = new EventRecoveryInfo(
            eventId, eventName, context, config, status, System.currentTimeMillis()
        );
        
        recoveryEvents.put(eventId, recoveryInfo);
        
        // 如果处理失败，标记为需要重试
        if (status == EventProcessingStatus.FAILED) {
            recoveryInfo.setRetryCount(0);
            recoveryInfo.setNextRetryTime(System.currentTimeMillis() + config.getRetryDelayMs());
        }
        
        log.info("记录事件处理状态: {} - {} - {}", eventId, eventName, status);
    }

    /**
     * 手动触发事件重发
     */
    public void replayEvent(String eventId) {
        EventRecoveryInfo recoveryInfo = recoveryEvents.get(eventId);
        if (recoveryInfo == null) {
            log.warn("未找到事件记录: {}", eventId);
            return;
        }
        
        if (recoveryInfo.getStatus() == EventProcessingStatus.SUCCESS) {
            log.info("事件已成功处理，无需重发: {}", eventId);
            return;
        }
        
        replayEventInternal(recoveryInfo);
    }

    /**
     * 重发所有失败的事件
     */
    public void replayAllFailedEvents() {
        log.info("开始重发所有失败的事件...");
        
        recoveryEvents.values().stream()
            .filter(info -> info.getStatus() == EventProcessingStatus.FAILED)
            .filter(info -> System.currentTimeMillis() >= info.getNextRetryTime())
            .forEach(this::replayEventInternal);
    }

    /**
     * 从持久化文件恢复事件
     */
    public void recoverEventsFromPersistence() {
        if (!signalProperties.getPersistent()) {
            log.warn("持久化未启用，无法从文件恢复事件");
            return;
        }
        
        if (isRecovering) {
            log.warn("事件恢复正在进行中，跳过本次恢复");
            return;
        }
        
        isRecovering = true;
        
        try {
            log.info("开始从持久化文件恢复事件...");
            
            String persistenceFile = signalProperties.getPersistenceDirectory() + "/" + signalProperties.getPersistenceFile();
            File file = new File(persistenceFile);
            
            if (!file.exists()) {
                log.info("持久化文件不存在，无需恢复: {}", persistenceFile);
                return;
            }
            
            // 读取持久化的事件
            List<SignalPersistenceInfo> events = EnhancedSignalPersistence.readAllFromFile(persistenceFile);
            
            log.info("从文件恢复 {} 个事件", events.size());
            
            for (SignalPersistenceInfo eventInfo : events) {
                try {
                    // 重新发送事件
                    String eventName = eventInfo.getSigHandler().getSignalName();
                    SignalContext context = eventInfo.getSignalContext();
                    SignalConfig config = eventInfo.getSignalConfig();
                    
                    // 检查事件是否已经处理过
                    String eventId = context.getEventId();
                    if (eventId != null && recoveryEvents.containsKey(eventId)) {
                        EventRecoveryInfo existingInfo = recoveryEvents.get(eventId);
                        if (existingInfo.getStatus() == EventProcessingStatus.SUCCESS) {
                            log.info("事件已成功处理，跳过恢复: {}", eventId);
                            continue;
                        }
                    }
                    
                    // 重新发送事件
                    signals.emit(eventName, new Object(), (error) -> {
                        log.error("恢复事件处理失败: {} - {}", eventName, error.getMessage());
                    }, context);
                    
                    log.info("成功恢复事件: {} - {}", eventName, eventId);
                    
                } catch (Exception e) {
                    log.error("恢复事件时发生错误: {}", e.getMessage(), e);
                }
            }
            
            lastRecoveryTime = System.currentTimeMillis();
            log.info("事件恢复完成");
            
        } catch (Exception e) {
            log.error("事件恢复过程中发生错误: {}", e.getMessage(), e);
        } finally {
            isRecovering = false;
        }
    }

    /**
     * 补发指定时间范围内的事件
     */
    public void replayEventsInTimeRange(long startTime, long endTime) {
        log.info("补发时间范围内的事件: {} - {}", 
                formatTime(startTime), formatTime(endTime));
        
        recoveryEvents.values().stream()
            .filter(info -> info.getProcessingTime() >= startTime && info.getProcessingTime() <= endTime)
            .filter(info -> info.getStatus() != EventProcessingStatus.SUCCESS)
            .forEach(this::replayEventInternal);
    }

    /**
     * 内部重发事件方法
     */
    private void replayEventInternal(EventRecoveryInfo recoveryInfo) {
        String eventId = recoveryInfo.getEventId();
        String eventName = recoveryInfo.getEventName();
        SignalContext context = recoveryInfo.getContext();
        SignalConfig config = recoveryInfo.getConfig();
        
        // 检查重试次数
        if (recoveryInfo.getRetryCount() >= config.getMaxRetries()) {
            log.warn("事件重试次数已达上限，进入死信队列: {} - 重试{}次", eventId, config.getMaxRetries());
            recoveryInfo.setStatus(EventProcessingStatus.DEAD_LETTER);
            return;
        }
        
        // 更新重试信息
        recoveryInfo.setRetryCount(recoveryInfo.getRetryCount() + 1);
        recoveryInfo.setLastRetryTime(System.currentTimeMillis());
        recoveryInfo.setNextRetryTime(System.currentTimeMillis() + config.getRetryDelayMs());
        
        log.info("重发事件: {} - {} - 第{}次重试", eventId, eventName, recoveryInfo.getRetryCount());
        
        try {
            // 重新发送事件
            signals.emit(eventName, new Object(), (error) -> {
                log.error("重发事件处理失败: {} - {}", eventName, error.getMessage());
                recoveryInfo.setStatus(EventProcessingStatus.FAILED);
            }, context);
            
            recoveryInfo.setStatus(EventProcessingStatus.RETRYING);
            
        } catch (Exception e) {
            log.error("重发事件时发生错误: {} - {}", eventId, e.getMessage(), e);
            recoveryInfo.setStatus(EventProcessingStatus.FAILED);
        }
    }

    /**
     * 定时任务：定期重试失败的事件
     */
    @Scheduled(fixedDelay = 30000) // 每30秒执行一次
    public void scheduledRetry() {
        if (signalProperties.getPersistent()) {
            replayAllFailedEvents();
        }
    }

    /**
     * 定时任务：定期从持久化文件恢复事件
     */
    @Scheduled(fixedDelay = 60000) // 每60秒执行一次
    public void scheduledRecovery() {
        if (signalProperties.getPersistent()) {
            recoverEventsFromPersistence();
        }
    }

    /**
     * 获取恢复统计信息
     */
    public RecoveryStatistics getRecoveryStatistics() {
        long totalEvents = recoveryEvents.size();
        long successEvents = recoveryEvents.values().stream()
            .filter(info -> info.getStatus() == EventProcessingStatus.SUCCESS)
            .count();
        long failedEvents = recoveryEvents.values().stream()
            .filter(info -> info.getStatus() == EventProcessingStatus.FAILED)
            .count();
        long retryingEvents = recoveryEvents.values().stream()
            .filter(info -> info.getStatus() == EventProcessingStatus.RETRYING)
            .count();
        long deadLetterEvents = recoveryEvents.values().stream()
            .filter(info -> info.getStatus() == EventProcessingStatus.DEAD_LETTER)
            .count();
        
        return new RecoveryStatistics(totalEvents, successEvents, failedEvents, retryingEvents, deadLetterEvents);
    }

    /**
     * 清理已成功处理的事件记录
     */
    public void cleanupSuccessfulEvents() {
        recoveryEvents.entrySet().removeIf(entry -> 
            entry.getValue().getStatus() == EventProcessingStatus.SUCCESS);
    }

    private String formatTime(long timestamp) {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 事件处理状态
     */
    public enum EventProcessingStatus {
        PENDING,    // 待处理
        PROCESSING, // 处理中
        SUCCESS,    // 成功
        FAILED,     // 失败
        RETRYING,   // 重试中
        DEAD_LETTER // 死信
    }

    /**
     * 事件恢复信息
     */
    public static class EventRecoveryInfo {
        private final String eventId;
        private final String eventName;
        private final SignalContext context;
        private final SignalConfig config;
        private EventProcessingStatus status;
        private final long processingTime;
        private int retryCount;
        private long lastRetryTime;
        private long nextRetryTime;

        public EventRecoveryInfo(String eventId, String eventName, SignalContext context, 
                               SignalConfig config, EventProcessingStatus status, long processingTime) {
            this.eventId = eventId;
            this.eventName = eventName;
            this.context = context;
            this.config = config;
            this.status = status;
            this.processingTime = processingTime;
            this.retryCount = 0;
        }

        // Getters and Setters
        public String getEventId() { return eventId; }
        public String getEventName() { return eventName; }
        public SignalContext getContext() { return context; }
        public SignalConfig getConfig() { return config; }
        public EventProcessingStatus getStatus() { return status; }
        public void setStatus(EventProcessingStatus status) { this.status = status; }
        public long getProcessingTime() { return processingTime; }
        public int getRetryCount() { return retryCount; }
        public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
        public long getLastRetryTime() { return lastRetryTime; }
        public void setLastRetryTime(long lastRetryTime) { this.lastRetryTime = lastRetryTime; }
        public long getNextRetryTime() { return nextRetryTime; }
        public void setNextRetryTime(long nextRetryTime) { this.nextRetryTime = nextRetryTime; }
    }

    /**
     * 恢复统计信息
     */
    public static class RecoveryStatistics {
        private final long totalEvents;
        private final long successEvents;
        private final long failedEvents;
        private final long retryingEvents;
        private final long deadLetterEvents;

        public RecoveryStatistics(long totalEvents, long successEvents, long failedEvents, 
                                long retryingEvents, long deadLetterEvents) {
            this.totalEvents = totalEvents;
            this.successEvents = successEvents;
            this.failedEvents = failedEvents;
            this.retryingEvents = retryingEvents;
            this.deadLetterEvents = deadLetterEvents;
        }

        // Getters
        public long getTotalEvents() { return totalEvents; }
        public long getSuccessEvents() { return successEvents; }
        public long getFailedEvents() { return failedEvents; }
        public long getRetryingEvents() { return retryingEvents; }
        public long getDeadLetterEvents() { return deadLetterEvents; }
        public double getSuccessRate() { 
            return totalEvents > 0 ? (double) successEvents / totalEvents * 100 : 0; 
        }
    }
}
