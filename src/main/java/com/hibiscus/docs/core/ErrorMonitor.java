package com.hibiscus.docs.core;

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
 * 错误监控器
 * 负责详细跟踪和记录系统中的错误请求
 */
@Component
public class ErrorMonitor {

    // 错误记录存储
    private final ConcurrentLinkedQueue<ErrorRecord> errorHistory = new ConcurrentLinkedQueue<>();
    private static final int MAX_ERROR_HISTORY = 1000; // 最多保存1000条错误记录

    // 错误统计
    private final AtomicLong totalErrors = new AtomicLong(0);
    private final AtomicLong criticalErrors = new AtomicLong(0);
    private final AtomicLong warningErrors = new AtomicLong(0);

    // 错误类型统计
    private final Map<String, Long> errorTypeCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> endpointErrorCounts = new ConcurrentHashMap<>();

    // 定时清理任务
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    public void init() {
        startCleanupTask();
    }

    @PreDestroy
    public void cleanup() {
        if (cleanupExecutor != null && !cleanupExecutor.isShutdown()) {
            cleanupExecutor.shutdown();
        }
    }

    /**
     * 启动清理任务
     */
    private void startCleanupTask() {
        // 每小时清理一次错误历史
        cleanupExecutor.scheduleAtFixedRate(() -> {
            try {
                cleanupErrorHistory();
            } catch (Exception e) {
                System.err.println("[错误监控] 清理任务异常: " + e.getMessage());
            }
        }, 1, 1, TimeUnit.HOURS);
    }

    /**
     * 记录错误请求
     */
    public void recordError(String endpoint, String method, String errorType, String errorMessage, 
                           int statusCode, long responseTime, String userAgent, String ipAddress) {
        
        ErrorRecord error = new ErrorRecord(endpoint, method, errorType, errorMessage, 
                                          statusCode, responseTime, userAgent, ipAddress);
        
        errorHistory.offer(error);
        
        // 限制错误历史记录数量
        if (errorHistory.size() > MAX_ERROR_HISTORY) {
            errorHistory.poll();
        }

        // 更新统计信息
        totalErrors.incrementAndGet();
        
        // 根据状态码分类错误
        if (statusCode >= 500) {
            criticalErrors.incrementAndGet();
        } else if (statusCode >= 400) {
            warningErrors.incrementAndGet();
        }

        // 更新错误类型统计
        errorTypeCounts.merge(errorType, 1L, Long::sum);
        
        // 更新端点错误统计
        endpointErrorCounts.merge(endpoint, 1L, Long::sum);

        // 输出错误信息
        System.err.println("❌ [错误监控] " + method + " " + endpoint + " - " + statusCode + ": " + errorMessage);
        System.err.println("时间: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.err.println("响应时间: " + responseTime + "ms");
        System.err.println("用户代理: " + userAgent);
        System.err.println("IP地址: " + ipAddress);
        System.err.println("---");
    }

    /**
     * 获取错误统计信息
     */
    public Map<String, Object> getErrorStats() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalErrors", totalErrors.get());
        stats.put("criticalErrors", criticalErrors.get());
        stats.put("warningErrors", warningErrors.get());
        stats.put("errorTypeCounts", new HashMap<>(errorTypeCounts));
        stats.put("endpointErrorCounts", new HashMap<>(endpointErrorCounts));
        stats.put("recentErrorCount", errorHistory.size());
        
        return stats;
    }

    /**
     * 获取最近的错误记录
     */
    public List<Map<String, Object>> getRecentErrors(int limit) {
        return errorHistory.stream()
            .limit(limit)
            .map(error -> {
                Map<String, Object> errorMap = new HashMap<>();
                errorMap.put("endpoint", error.endpoint);
                errorMap.put("method", error.method);
                errorMap.put("errorType", error.errorType);
                errorMap.put("errorMessage", error.errorMessage);
                errorMap.put("statusCode", error.statusCode);
                errorMap.put("responseTime", error.responseTime);
                errorMap.put("userAgent", error.userAgent);
                errorMap.put("ipAddress", error.ipAddress);
                errorMap.put("timestamp", error.timestamp);
                errorMap.put("time", LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(error.timestamp), 
                    java.time.ZoneId.systemDefault()
                ).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                return errorMap;
            })
            .collect(Collectors.toList());
    }

    /**
     * 获取错误趋势分析
     */
    public Map<String, Object> getErrorTrends() {
        Map<String, Object> trends = new HashMap<>();
        
        // 按小时统计错误数量
        Map<String, Long> hourlyErrors = new HashMap<>();
        long currentHour = System.currentTimeMillis() / (60 * 60 * 1000);
        
        for (int i = 0; i < 24; i++) {
            long hour = currentHour - i;
            String hourKey = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(hour * 60 * 60 * 1000), 
                java.time.ZoneId.systemDefault()
            ).format(DateTimeFormatter.ofPattern("MM-dd HH"));
            
            final long targetHour = hour;
            long count = errorHistory.stream()
                .filter(error -> error.timestamp / (60 * 60 * 1000) == targetHour)
                .count();
            
            hourlyErrors.put(hourKey, count);
        }
        
        trends.put("hourlyErrors", hourlyErrors);
        
        // 错误类型分布
        trends.put("errorTypeDistribution", new HashMap<>(errorTypeCounts));
        
        // 端点错误分布
        trends.put("endpointErrorDistribution", new HashMap<>(endpointErrorCounts));
        
        return trends;
    }

    /**
     * 清理错误历史
     */
    private void cleanupErrorHistory() {
        // 保留最近24小时的错误记录
        long cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
        errorHistory.removeIf(error -> error.timestamp < cutoffTime);
    }

    /**
     * 错误记录内部类
     */
    private static class ErrorRecord {
        final String endpoint;
        final String method;
        final String errorType;
        final String errorMessage;
        final int statusCode;
        final long responseTime;
        final String userAgent;
        final String ipAddress;
        final long timestamp;

        ErrorRecord(String endpoint, String method, String errorType, String errorMessage,
                   int statusCode, long responseTime, String userAgent, String ipAddress) {
            this.endpoint = endpoint;
            this.method = method;
            this.errorType = errorType;
            this.errorMessage = errorMessage;
            this.statusCode = statusCode;
            this.responseTime = responseTime;
            this.userAgent = userAgent;
            this.ipAddress = ipAddress;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
