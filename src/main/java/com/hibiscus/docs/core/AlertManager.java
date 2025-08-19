package com.hibiscus.docs.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

/**
 * 告警管理器
 * 负责监控系统性能指标并在达到阈值时发送告警
 */
@Component
public class AlertManager {

    @Autowired
    private AppConfigProperties appConfigProperties;

    // 告警配置
    private static final double HEAP_USAGE_CRITICAL = 90.0; // 严重告警：90%
    private static final double HEAP_USAGE_WARNING = 80.0;  // 警告告警：80%
    private static final double SYSTEM_LOAD_CRITICAL = 0.9; // 严重告警：0.9
    private static final double SYSTEM_LOAD_WARNING = 0.8;  // 警告告警：0.8
    private static final long RESPONSE_TIME_CRITICAL = 10000; // 严重告警：10秒
    private static final long RESPONSE_TIME_WARNING = 5000;   // 警告告警：5秒
    private static final int THREAD_COUNT_CRITICAL = 300;    // 严重告警：300
    private static final int THREAD_COUNT_WARNING = 200;     // 警告告警：200

    // 告警冷却时间（毫秒）
    private static final long CRITICAL_ALERT_COOLDOWN = 30000;  // 严重告警：30秒
    private static final long WARNING_ALERT_COOLDOWN = 300000;  // 警告告警：5分钟

    // 告警历史记录
    private final ConcurrentLinkedQueue<AlertRecord> alertHistory = new ConcurrentLinkedQueue<>();
    private static final int MAX_ALERT_HISTORY = 1000; // 最多保存1000条告警记录

    // 告警状态跟踪
    private final Map<String, AlertStatus> alertStatuses = new ConcurrentHashMap<>();
    private final AtomicLong lastCriticalAlertTime = new AtomicLong(0);
    private final AtomicLong lastWarningAlertTime = new AtomicLong(0);

    // 定时任务执行器
    private final ScheduledExecutorService alertExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    public void init() {
        if (appConfigProperties != null && appConfigProperties.getAdvanced() != null && 
            appConfigProperties.getAdvanced().isEnablePerformanceMonitoring()) {
            startAlertMonitoring();
            startCleanupTask();
        }
    }

    @PreDestroy
    public void cleanup() {
        if (alertExecutor != null && !alertExecutor.isShutdown()) {
            alertExecutor.shutdown();
        }
        if (cleanupExecutor != null && !cleanupExecutor.isShutdown()) {
            cleanupExecutor.shutdown();
        }
    }

    /**
     * 启动告警监控
     */
    private void startAlertMonitoring() {
        // 每10秒检查一次告警条件
        alertExecutor.scheduleAtFixedRate(() -> {
            try {
                checkAllAlerts();
            } catch (Exception e) {
                System.err.println("[告警系统] 监控异常: " + e.getMessage());
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    /**
     * 启动清理任务
     */
    private void startCleanupTask() {
        // 每小时清理一次告警历史
        cleanupExecutor.scheduleAtFixedRate(() -> {
            try {
                cleanupAlertHistory();
            } catch (Exception e) {
                System.err.println("[告警系统] 清理任务异常: " + e.getMessage());
            }
        }, 1, 1, TimeUnit.HOURS);
    }

    /**
     * 检查所有告警条件
     */
    public void checkAllAlerts() {
        // 这里会从PerformanceMonitor获取最新的性能指标
        // 实际使用时需要注入PerformanceMonitor或通过事件机制获取
    }

    /**
     * 检查堆内存告警
     */
    public void checkHeapMemoryAlert(double heapUsage) {
        long currentTime = System.currentTimeMillis();
        
        if (heapUsage >= HEAP_USAGE_CRITICAL) {
            if (currentTime - lastCriticalAlertTime.get() >= CRITICAL_ALERT_COOLDOWN) {
                sendAlert(AlertLevel.CRITICAL, "内存告警", 
                    String.format("堆内存使用率严重过高: %.2f%%", heapUsage));
                lastCriticalAlertTime.set(currentTime);
            }
        } else if (heapUsage >= HEAP_USAGE_WARNING) {
            if (currentTime - lastWarningAlertTime.get() >= WARNING_ALERT_COOLDOWN) {
                sendAlert(AlertLevel.WARNING, "内存告警", 
                    String.format("堆内存使用率过高: %.2f%%", heapUsage));
                lastWarningAlertTime.set(currentTime);
            }
        }
    }

    /**
     * 检查系统负载告警
     */
    public void checkSystemLoadAlert(double systemLoad) {
        long currentTime = System.currentTimeMillis();
        
        if (systemLoad >= SYSTEM_LOAD_CRITICAL) {
            if (currentTime - lastCriticalAlertTime.get() >= CRITICAL_ALERT_COOLDOWN) {
                sendAlert(AlertLevel.CRITICAL, "系统负载告警", 
                    String.format("系统负载严重过高: %.2f", systemLoad));
                lastCriticalAlertTime.set(currentTime);
            }
        } else if (systemLoad >= SYSTEM_LOAD_WARNING) {
            if (currentTime - lastWarningAlertTime.get() >= WARNING_ALERT_COOLDOWN) {
                sendAlert(AlertLevel.WARNING, "系统负载告警", 
                    String.format("系统负载过高: %.2f", systemLoad));
                lastWarningAlertTime.set(currentTime);
            }
        }
    }

    /**
     * 检查响应时间告警
     */
    public void checkResponseTimeAlert(String endpoint, long responseTime) {
        long currentTime = System.currentTimeMillis();
        
        if (responseTime >= RESPONSE_TIME_CRITICAL) {
            if (currentTime - lastCriticalAlertTime.get() >= CRITICAL_ALERT_COOLDOWN) {
                sendAlert(AlertLevel.CRITICAL, "响应时间告警", 
                    String.format("端点 %s 响应时间严重过长: %dms", endpoint, responseTime));
                lastCriticalAlertTime.set(currentTime);
            }
        } else if (responseTime >= RESPONSE_TIME_WARNING) {
            if (currentTime - lastWarningAlertTime.get() >= WARNING_ALERT_COOLDOWN) {
                sendAlert(AlertLevel.WARNING, "响应时间告警", 
                    String.format("端点 %s 响应时间过长: %dms", endpoint, responseTime));
                lastWarningAlertTime.set(currentTime);
            }
        }
    }

    /**
     * 检查线程数告警
     */
    public void checkThreadCountAlert(int threadCount) {
        long currentTime = System.currentTimeMillis();
        
        if (threadCount >= THREAD_COUNT_CRITICAL) {
            if (currentTime - lastCriticalAlertTime.get() >= CRITICAL_ALERT_COOLDOWN) {
                sendAlert(AlertLevel.CRITICAL, "线程数告警", 
                    String.format("线程数严重过多: %d", threadCount));
                lastCriticalAlertTime.set(currentTime);
            }
        } else if (threadCount >= THREAD_COUNT_WARNING) {
            if (currentTime - lastWarningAlertTime.get() >= WARNING_ALERT_COOLDOWN) {
                sendAlert(AlertLevel.WARNING, "线程数告警", 
                    String.format("线程数过多: %d", threadCount));
                lastWarningAlertTime.set(currentTime);
            }
        }
    }

    /**
     * 发送告警
     */
    private void sendAlert(AlertLevel level, String type, String message) {
        AlertRecord alert = new AlertRecord(level, type, message);
        alertHistory.offer(alert);
        
        // 限制告警历史记录数量
        if (alertHistory.size() > MAX_ALERT_HISTORY) {
            alertHistory.poll();
        }

        // 更新告警状态
        updateAlertStatus(type, level);

        // 输出告警信息
        String emoji = level == AlertLevel.CRITICAL ? "🚨" : "⚠️";
        System.err.println(emoji + " [" + level.name() + "] " + type + ": " + message);
        System.err.println("时间: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        // 这里可以集成其他告警方式：
        // - 发送邮件
        // - 发送钉钉/企业微信消息
        // - 写入专门的告警日志文件
        // - 调用外部告警API
        // - 发送Webhook通知
    }

    /**
     * 更新告警状态
     */
    private void updateAlertStatus(String alertType, AlertLevel level) {
        AlertStatus status = alertStatuses.computeIfAbsent(alertType, k -> new AlertStatus());
        status.updateStatus(level);
    }

    /**
     * 清理告警历史
     */
    private void cleanupAlertHistory() {
        // 保留最近24小时的告警记录
        long cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
        alertHistory.removeIf(alert -> alert.timestamp < cutoffTime);
    }

    /**
     * 获取告警统计信息
     */
    public Map<String, Object> getAlertStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 统计各级别告警数量
        long criticalCount = alertHistory.stream()
            .filter(alert -> alert.level == AlertLevel.CRITICAL)
            .count();
        long warningCount = alertHistory.stream()
            .filter(alert -> alert.level == AlertLevel.WARNING)
            .count();
        
        stats.put("totalAlerts", alertHistory.size());
        stats.put("criticalAlerts", criticalCount);
        stats.put("warningAlerts", warningCount);
        stats.put("alertStatuses", new HashMap<>(alertStatuses));
        
        return stats;
    }

    /**
     * 获取最近的告警记录
     */
    public List<Map<String, Object>> getRecentAlerts(int limit) {
        return alertHistory.stream()
            .limit(limit)
            .map(alert -> {
                Map<String, Object> alertMap = new HashMap<>();
                alertMap.put("level", alert.level.name());
                alertMap.put("type", alert.type);
                alertMap.put("message", alert.message);
                alertMap.put("timestamp", alert.timestamp);
                alertMap.put("time", LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(alert.timestamp), 
                    java.time.ZoneId.systemDefault()
                ).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                return alertMap;
            })
            .collect(Collectors.toList());
    }

    /**
     * 告警级别枚举
     */
    public enum AlertLevel {
        WARNING,    // 警告
        CRITICAL    // 严重
    }

    /**
     * 告警记录内部类
     */
    private static class AlertRecord {
        final AlertLevel level;
        final String type;
        final String message;
        final long timestamp;

        AlertRecord(AlertLevel level, String type, String message) {
            this.level = level;
            this.type = type;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * 告警状态内部类
     */
    private static class AlertStatus {
        AlertLevel currentLevel = null;
        long lastOccurrence = 0;
        int occurrenceCount = 0;

        void updateStatus(AlertLevel level) {
            this.currentLevel = level;
            this.lastOccurrence = System.currentTimeMillis();
            this.occurrenceCount++;
        }

        Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("currentLevel", currentLevel != null ? currentLevel.name() : "NORMAL");
            map.put("lastOccurrence", lastOccurrence);
            map.put("occurrenceCount", occurrenceCount);
            return map;
        }
    }
}
