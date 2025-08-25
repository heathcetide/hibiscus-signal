package com.hibiscus.signal.core;

import com.hibiscus.signal.core.entity.DeadLetterEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 事件监控和告警系统
 * 负责监控事件处理状态、性能指标和异常情况，并发送相应的告警
 * 
 * @author heathcetide
 */
public class EventMonitoringSystem {
    
    private static final Logger log = LoggerFactory.getLogger(EventMonitoringSystem.class);
    
    // 监控指标
    private final Map<String, EventMetrics> eventMetricsMap = new ConcurrentHashMap<>();
    private final Map<String, AlertRule> alertRules = new ConcurrentHashMap<>();
    
    // 告警发送器
    private final List<AlertSender> alertSenders = new CopyOnWriteArrayList<>();
    
    // 定时监控任务
    private final ScheduledExecutorService monitoringExecutor = Executors.newSingleThreadScheduledExecutor();
    
    // 全局统计
    private final AtomicLong totalEventsProcessed = new AtomicLong(0);
    private final AtomicLong totalEventsFailed = new AtomicLong(0);
    private final AtomicLong totalAlertsSent = new AtomicLong(0);
    
    // 配置参数
    private final boolean enableMonitoring;
    private final long monitoringIntervalMs;
    private final int maxEventMetrics;
    
    public EventMonitoringSystem() {
        this(true, 60000, 1000); // 默认启用监控，60秒间隔，最多1000个事件指标
    }
    
    public EventMonitoringSystem(boolean enableMonitoring, long monitoringIntervalMs, int maxEventMetrics) {
        this.enableMonitoring = enableMonitoring;
        this.monitoringIntervalMs = monitoringIntervalMs;
        this.maxEventMetrics = maxEventMetrics;
        
        if (enableMonitoring) {
            startMonitoring();
        }
    }
    
    /**
     * 记录事件处理成功
     */
    public void recordEventSuccess(String eventName, long processingTimeMs) {
        if (!enableMonitoring) return;
        
        EventMetrics metrics = getOrCreateMetrics(eventName);
        metrics.recordSuccess(processingTimeMs);
        totalEventsProcessed.incrementAndGet();
        
        // 检查是否需要发送告警
        checkAndSendAlerts(eventName, metrics);
    }
    
    /**
     * 记录事件处理失败
     */
    public void recordEventFailure(String eventName, Exception error, long processingTimeMs) {
        if (!enableMonitoring) return;
        
        EventMetrics metrics = getOrCreateMetrics(eventName);
        metrics.recordFailure(error, processingTimeMs);
        totalEventsFailed.incrementAndGet();
        
        // 检查是否需要发送告警
        checkAndSendAlerts(eventName, metrics);
    }
    
    /**
     * 记录死信事件
     */
    public void recordDeadLetterEvent(DeadLetterEvent deadLetterEvent) {
        if (!enableMonitoring) return;
        
        String eventName = deadLetterEvent.getEventName();
        EventMetrics metrics = getOrCreateMetrics(eventName);
        metrics.recordDeadLetter();
        
        // 发送死信事件告警
        sendDeadLetterAlert(deadLetterEvent);
    }
    
    /**
     * 添加告警规则
     */
    public void addAlertRule(String eventName, AlertRule rule) {
        alertRules.put(eventName, rule);
        log.info("添加告警规则: {} -> {}", eventName, rule.getDescription());
    }
    
    /**
     * 添加告警发送器
     */
    public void addAlertSender(AlertSender sender) {
        alertSenders.add(sender);
        log.info("添加告警发送器: {}", sender.getClass().getSimpleName());
    }
    
    /**
     * 获取事件指标
     */
    public EventMetrics getEventMetrics(String eventName) {
        return eventMetricsMap.get(eventName);
    }
    
    /**
     * 获取全局统计信息
     */
    public GlobalStats getGlobalStats() {
        GlobalStats stats = new GlobalStats();
        stats.setTotalEventsProcessed(totalEventsProcessed.get());
        stats.setTotalEventsFailed(totalEventsFailed.get());
        stats.setTotalAlertsSent(totalAlertsSent.get());
        stats.setActiveEventMetrics(eventMetricsMap.size());
        stats.setActiveAlertRules(alertRules.size());
        stats.setActiveAlertSenders(alertSenders.size());
        
        return stats;
    }
    
    /**
     * 获取或创建事件指标
     */
    private EventMetrics getOrCreateMetrics(String eventName) {
        return eventMetricsMap.computeIfAbsent(eventName, k -> {
            // 检查容量限制
            if (eventMetricsMap.size() >= maxEventMetrics) {
                // 移除最旧的指标
                String oldestEvent = eventMetricsMap.keySet().iterator().next();
                eventMetricsMap.remove(oldestEvent);
                log.warn("事件指标数量超限，移除最旧指标: {}", oldestEvent);
            }
            return new EventMetrics(eventName);
        });
    }
    
    /**
     * 检查并发送告警
     */
    private void checkAndSendAlerts(String eventName, EventMetrics metrics) {
        AlertRule rule = alertRules.get(eventName);
        if (rule == null) return;
        
        try {
            if (rule.shouldAlert(metrics)) {
                Alert alert = createAlert(eventName, rule, metrics);
                sendAlert(alert);
            }
        } catch (Exception e) {
            log.error("检查告警规则时发生异常: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 创建告警
     */
    private Alert createAlert(String eventName, AlertRule rule, EventMetrics metrics) {
        Alert alert = new Alert();
        alert.setEventName(eventName);
        alert.setAlertType(rule.getAlertType());
        alert.setSeverity(rule.getSeverity());
        alert.setMessage(rule.formatMessage(metrics));
        alert.setTimestamp(System.currentTimeMillis());
        alert.setMetrics(metrics);
        
        return alert;
    }
    
    /**
     * 发送告警
     */
    private void sendAlert(Alert alert) {
        if (alertSenders.isEmpty()) {
            log.warn("没有配置告警发送器，无法发送告警: {}", alert.getMessage());
            return;
        }
        
        for (AlertSender sender : alertSenders) {
            try {
                sender.sendAlert(alert);
                totalAlertsSent.incrementAndGet();
                log.info("告警发送成功: {} -> {}", alert.getMessage(), sender.getClass().getSimpleName());
            } catch (Exception e) {
                log.error("告警发送失败: {} -> {}, 错误: {}", 
                         alert.getMessage(), sender.getClass().getSimpleName(), e.getMessage(), e);
            }
        }
    }
    
    /**
     * 发送死信事件告警
     */
    private void sendDeadLetterAlert(DeadLetterEvent deadLetterEvent) {
        Alert alert = new Alert();
        alert.setEventName(deadLetterEvent.getEventName());
        alert.setAlertType(AlertType.DEAD_LETTER);
        alert.setSeverity(AlertSeverity.HIGH);
        alert.setMessage("死信事件: " + deadLetterEvent.getEventSummary());
        alert.setTimestamp(System.currentTimeMillis());
        alert.setDeadLetterEvent(deadLetterEvent);
        
        sendAlert(alert);
    }
    
    /**
     * 启动监控
     */
    private void startMonitoring() {
        monitoringExecutor.scheduleWithFixedDelay(
                this::performMonitoring,
                monitoringIntervalMs, // 延迟启动
                monitoringIntervalMs, // 固定间隔
                TimeUnit.MILLISECONDS
        );
        log.info("事件监控系统已启动，监控间隔: {}ms", monitoringIntervalMs);
    }
    
    /**
     * 执行监控任务
     */
    private void performMonitoring() {
        try {
            // 清理过期指标
            cleanupExpiredMetrics();
            
            // 检查全局告警规则
            checkGlobalAlertRules();
            
            // 输出监控日志
            logMonitoringInfo();
            
        } catch (Exception e) {
            log.error("执行监控任务时发生异常: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 清理过期指标
     */
    private void cleanupExpiredMetrics() {
        long cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000L); // 24小时
        
        eventMetricsMap.entrySet().removeIf(entry -> {
            EventMetrics metrics = entry.getValue();
            if (metrics.getLastUpdateTime() < cutoffTime) {
                log.debug("清理过期事件指标: {}", entry.getKey());
                return true;
            }
            return false;
        });
    }
    
    /**
     * 检查全局告警规则
     */
    private void checkGlobalAlertRules() {
        // 检查总失败率
        long totalProcessed = totalEventsProcessed.get();
        long totalFailed = totalEventsFailed.get();
        
        if (totalProcessed > 0) {
            double failureRate = (double) totalFailed / totalProcessed;
            if (failureRate > 0.1) { // 失败率超过10%
                Alert alert = new Alert();
                alert.setEventName("GLOBAL");
                alert.setAlertType(AlertType.HIGH_FAILURE_RATE);
                alert.setSeverity(AlertSeverity.CRITICAL);
                alert.setMessage(String.format("全局事件失败率过高: %.2f%%", failureRate * 100));
                alert.setTimestamp(System.currentTimeMillis());
                
                sendAlert(alert);
            }
        }
    }
    
    /**
     * 输出监控信息
     */
    private void logMonitoringInfo() {
        if (log.isDebugEnabled()) {
            GlobalStats stats = getGlobalStats();
            log.debug("监控统计 - 总处理: {}, 总失败: {}, 告警数: {}, 活跃指标: {}", 
                     stats.getTotalEventsProcessed(), stats.getTotalEventsFailed(), 
                     stats.getTotalAlertsSent(), stats.getActiveEventMetrics());
        }
    }
    
    /**
     * 关闭监控系统
     */
    public void shutdown() {
        if (enableMonitoring) {
            monitoringExecutor.shutdown();
            try {
                if (!monitoringExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    monitoringExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                monitoringExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        log.info("事件监控系统已关闭");
    }
    
    /**
     * 事件指标类
     */
    public static class EventMetrics {
        private final String eventName;
        private final AtomicLong successCount = new AtomicLong(0);
        private final AtomicLong failureCount = new AtomicLong(0);
        private final AtomicLong deadLetterCount = new AtomicLong(0);
        private final AtomicLong totalProcessingTime = new AtomicLong(0);
        private final AtomicLong maxProcessingTime = new AtomicLong(0);
        private final AtomicLong minProcessingTime = new AtomicLong(Long.MAX_VALUE);
        private volatile long lastUpdateTime = System.currentTimeMillis();
        private volatile long lastSuccessTime = 0;
        private volatile long lastFailureTime = 0;
        
        public EventMetrics(String eventName) {
            this.eventName = eventName;
        }
        
        public void recordSuccess(long processingTimeMs) {
            successCount.incrementAndGet();
            totalProcessingTime.addAndGet(processingTimeMs);
            updateProcessingTimeStats(processingTimeMs);
            lastSuccessTime = System.currentTimeMillis();
            lastUpdateTime = System.currentTimeMillis();
        }
        
        public void recordFailure(Exception error, long processingTimeMs) {
            failureCount.incrementAndGet();
            totalProcessingTime.addAndGet(processingTimeMs);
            updateProcessingTimeStats(processingTimeMs);
            lastFailureTime = System.currentTimeMillis();
            lastUpdateTime = System.currentTimeMillis();
        }
        
        public void recordDeadLetter() {
            deadLetterCount.incrementAndGet();
            lastUpdateTime = System.currentTimeMillis();
        }
        
        private void updateProcessingTimeStats(long processingTimeMs) {
            long currentMax = maxProcessingTime.get();
            while (processingTimeMs > currentMax && 
                   !maxProcessingTime.compareAndSet(currentMax, processingTimeMs)) {
                currentMax = maxProcessingTime.get();
            }
            
            long currentMin = minProcessingTime.get();
            while (processingTimeMs < currentMin && 
                   !minProcessingTime.compareAndSet(currentMin, processingTimeMs)) {
                currentMin = minProcessingTime.get();
            }
        }
        
        // Getters
        public String getEventName() { return eventName; }
        public long getSuccessCount() { return successCount.get(); }
        public long getFailureCount() { return failureCount.get(); }
        public long getDeadLetterCount() { return deadLetterCount.get(); }
        public long getTotalCount() { return successCount.get() + failureCount.get(); }
        public double getSuccessRate() { 
            long total = getTotalCount();
            return total > 0 ? (double) successCount.get() / total : 0.0;
        }
        public double getFailureRate() { 
            long total = getTotalCount();
            return total > 0 ? (double) failureCount.get() / total : 0.0;
        }
        public long getAverageProcessingTime() {
            long total = getTotalCount();
            return total > 0 ? totalProcessingTime.get() / total : 0;
        }
        public long getMaxProcessingTime() { return maxProcessingTime.get(); }
        public long getMinProcessingTime() { return minProcessingTime.get(); }
        public long getLastUpdateTime() { return lastUpdateTime; }
        public long getLastSuccessTime() { return lastSuccessTime; }
        public long getLastFailureTime() { return lastFailureTime; }
    }
    
    /**
     * 告警规则接口
     */
    public interface AlertRule {
        boolean shouldAlert(EventMetrics metrics);
        AlertType getAlertType();
        AlertSeverity getSeverity();
        String getDescription();
        String formatMessage(EventMetrics metrics);
    }
    
    /**
     * 告警发送器接口
     */
    public interface AlertSender {
        void sendAlert(Alert alert) throws Exception;
    }
    
    /**
     * 告警类型
     */
    public enum AlertType {
        HIGH_FAILURE_RATE("高失败率"),
        HIGH_PROCESSING_TIME("高处理时间"),
        DEAD_LETTER("死信事件"),
        LOW_SUCCESS_RATE("低成功率"),
        NO_EVENTS("无事件处理");
        
        private final String description;
        
        AlertType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 告警严重程度
     */
    public enum AlertSeverity {
        LOW("低"),
        MEDIUM("中"),
        HIGH("高"),
        CRITICAL("严重");
        
        private final String description;
        
        AlertSeverity(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 告警信息
     */
    public static class Alert {
        private String eventName;
        private AlertType alertType;
        private AlertSeverity severity;
        private String message;
        private long timestamp;
        private EventMetrics metrics;
        private DeadLetterEvent deadLetterEvent;
        
        // Getters and Setters
        public String getEventName() { return eventName; }
        public void setEventName(String eventName) { this.eventName = eventName; }
        
        public AlertType getAlertType() { return alertType; }
        public void setAlertType(AlertType alertType) { this.alertType = alertType; }
        
        public AlertSeverity getSeverity() { return severity; }
        public void setSeverity(AlertSeverity severity) { this.severity = severity; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        
        public EventMetrics getMetrics() { return metrics; }
        public void setMetrics(EventMetrics metrics) { this.metrics = metrics; }
        
        public DeadLetterEvent getDeadLetterEvent() { return deadLetterEvent; }
        public void setDeadLetterEvent(DeadLetterEvent deadLetterEvent) { this.deadLetterEvent = deadLetterEvent; }
    }
    
    /**
     * 全局统计信息
     */
    public static class GlobalStats {
        private long totalEventsProcessed;
        private long totalEventsFailed;
        private long totalAlertsSent;
        private int activeEventMetrics;
        private int activeAlertRules;
        private int activeAlertSenders;
        
        // Getters and Setters
        public long getTotalEventsProcessed() { return totalEventsProcessed; }
        public void setTotalEventsProcessed(long totalEventsProcessed) { this.totalEventsProcessed = totalEventsProcessed; }
        
        public long getTotalEventsFailed() { return totalEventsFailed; }
        public void setTotalEventsFailed(long totalEventsFailed) { this.totalEventsFailed = totalEventsFailed; }
        
        public long getTotalAlertsSent() { return totalAlertsSent; }
        public void setTotalAlertsSent(long totalAlertsSent) { this.totalAlertsSent = totalAlertsSent; }
        
        public int getActiveEventMetrics() { return activeEventMetrics; }
        public void setActiveEventMetrics(int activeEventMetrics) { this.activeEventMetrics = activeEventMetrics; }
        
        public int getActiveAlertRules() { return activeAlertRules; }
        public void setActiveAlertRules(int activeAlertRules) { this.activeAlertRules = activeAlertRules; }
        
        public int getActiveAlertSenders() { return activeAlertSenders; }
        public void setActiveAlertSenders(int activeAlertSenders) { this.activeAlertSenders = activeAlertSenders; }
    }
}
