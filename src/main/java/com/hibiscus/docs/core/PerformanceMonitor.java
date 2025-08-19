package com.hibiscus.docs.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PerformanceMonitor {

    @Autowired
    private AppConfigProperties appConfigProperties;

    // 性能指标存储 - 使用LRU策略限制内存占用
    private final ConcurrentHashMap<String, EndpointMetrics> endpointMetrics = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<String> endpointAccessOrder = new ConcurrentLinkedQueue<>();
    
    // 统计数据 - 使用原子类型避免锁竞争
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);
    private final AtomicLong totalResponseTime = new AtomicLong(0);
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    
    // 告警阈值配置
    private final AtomicLong lastAlertTime = new AtomicLong(0);
    private static final long ALERT_COOLDOWN_MS = 60000; // 告警冷却时间：1分钟
    
    // 内存限制配置
    private static final int MAX_ENDPOINTS = 1000; // 最大端点数量
    private static final int MAX_METRICS_HISTORY = 100; // 每个端点的最大历史记录数
    
    // 告警阈值配置
    private static final double HEAP_USAGE_THRESHOLD = 80.0; // 堆内存使用率阈值：80%
    private static final double SYSTEM_LOAD_THRESHOLD = 0.8; // 系统负载阈值：0.8
    private static final long RESPONSE_TIME_THRESHOLD = 5000; // 响应时间阈值：5秒
    private static final int THREAD_COUNT_THRESHOLD = 200; // 线程数阈值：200
    
    private final ScheduledExecutorService metricsExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

    public PerformanceMonitor() {
        // 构造函数中不进行配置检查，等依赖注入完成后再初始化
    }
    
    @PostConstruct
    public void init() {
        if (appConfigProperties != null && appConfigProperties.getAdvanced() != null && 
            appConfigProperties.getAdvanced().isEnablePerformanceMonitoring()) {
            startMetricsCollection();
            startCleanupTask();
        }
    }
    
    @PreDestroy
    public void cleanup() {
        if (metricsExecutor != null && !metricsExecutor.isShutdown()) {
            metricsExecutor.shutdown();
        }
        if (cleanupExecutor != null && !cleanupExecutor.isShutdown()) {
            cleanupExecutor.shutdown();
        }
    }

    /**
     * 记录请求开始
     */
    public void recordRequestStart(String endpoint) {
        if (appConfigProperties == null || appConfigProperties.getAdvanced() == null || 
            !appConfigProperties.getAdvanced().isEnablePerformanceMonitoring()) {
            return;
        }

        totalRequests.incrementAndGet();
        activeConnections.incrementAndGet();
        
        // 限制端点数量，避免内存无限增长
        if (endpointMetrics.size() >= MAX_ENDPOINTS) {
            evictOldestEndpoint();
        }
        
        EndpointMetrics metrics = endpointMetrics.computeIfAbsent(endpoint, k -> new EndpointMetrics());
        metrics.recordRequestStart();
        
        // 更新访问顺序
        updateEndpointAccessOrder(endpoint);
    }

    /**
     * 记录请求完成
     */
    public void recordRequestComplete(String endpoint, long responseTime, boolean isError) {
        if (appConfigProperties == null || appConfigProperties.getAdvanced() == null || 
            !appConfigProperties.getAdvanced().isEnablePerformanceMonitoring()) {
            return;
        }

        activeConnections.decrementAndGet();
        totalResponseTime.addAndGet(responseTime);
        
        if (isError) {
            totalErrors.incrementAndGet();
        }
        
        EndpointMetrics metrics = endpointMetrics.get(endpoint);
        if (metrics != null) {
            metrics.recordRequestComplete(responseTime, isError);
            
            // 检查响应时间告警
            if (responseTime > RESPONSE_TIME_THRESHOLD) {
                checkAndSendAlert("响应时间告警", 
                    String.format("端点 %s 响应时间过长: %dms", endpoint, responseTime));
            }
        }
        
        // 检查系统性能告警
        checkSystemPerformanceAlerts();
    }

    /**
     * 获取性能统计信息
     */
    public PerformanceStats getPerformanceStats() {
        long total = totalRequests.get();
        long errors = totalErrors.get();
        long avgResponseTime = total > 0 ? totalResponseTime.get() / total : 0;
        
        return new PerformanceStats(
            total,
            errors,
            total - errors,
            avgResponseTime,
            activeConnections.get(),
            getSystemMetrics(),
            getEndpointMetrics()
        );
    }

    /**
     * 获取系统指标
     */
    private SystemMetrics getSystemMetrics() {
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax();
        double heapUsage = heapMax > 0 ? (double) heapUsed / heapMax * 100 : 0;
        
        return new SystemMetrics(
            heapUsed,
            heapMax,
            heapUsage,
            osBean.getSystemLoadAverage(),
            threadBean.getThreadCount(),
            threadBean.getPeakThreadCount()
        );
    }

    /**
     * 获取端点指标
     */
    private ConcurrentHashMap<String, EndpointMetrics> getEndpointMetrics() {
        return new ConcurrentHashMap<>(endpointMetrics);
    }

    /**
     * 启动指标收集任务
     */
    private void startMetricsCollection() {
        metricsExecutor.scheduleAtFixedRate(() -> {
            try {
                printPerformanceSummary();
                checkSystemPerformanceAlerts();
            } catch (Exception e) {
                System.err.println("[性能监控] 指标收集异常: " + e.getMessage());
            }
        }, 30, 30, TimeUnit.SECONDS);
    }
    
    /**
     * 启动清理任务
     */
    private void startCleanupTask() {
        cleanupExecutor.scheduleAtFixedRate(() -> {
            try {
                cleanupOldMetrics();
                evictOldestEndpoints();
            } catch (Exception e) {
                System.err.println("[性能监控] 清理任务异常: " + e.getMessage());
            }
        }, 60, 60, TimeUnit.SECONDS); // 每分钟清理一次
    }

    /**
     * 打印性能摘要
     */
    private void printPerformanceSummary() {
        PerformanceStats stats = getPerformanceStats();
        System.out.println("=== 性能监控摘要 ===");
        System.out.printf("总请求数: %d, 成功: %d, 错误: %d%n", 
            stats.totalRequests, stats.successfulRequests, stats.errorRequests);
        System.out.printf("平均响应时间: %dms, 活跃连接: %d%n", 
            stats.averageResponseTime, stats.activeConnections);
        System.out.printf("堆内存使用: %.2f%% (%dMB/%dMB)%n", 
            stats.systemMetrics.heapUsage, 
            stats.systemMetrics.heapUsed / 1024 / 1024,
            stats.systemMetrics.heapMax / 1024 / 1024);
        System.out.printf("系统负载: %.2f, 线程数: %d%n", 
            stats.systemMetrics.systemLoad, stats.systemMetrics.threadCount);
        System.out.printf("监控端点数量: %d%n", endpointMetrics.size());
        System.out.println("==================");
    }
    
    /**
     * 检查系统性能告警
     */
    private void checkSystemPerformanceAlerts() {
        SystemMetrics metrics = getSystemMetrics();
        long currentTime = System.currentTimeMillis();
        
        // 检查告警冷却时间
        if (currentTime - lastAlertTime.get() < ALERT_COOLDOWN_MS) {
            return;
        }
        
        // 检查堆内存使用率
        if (metrics.heapUsage > HEAP_USAGE_THRESHOLD) {
            checkAndSendAlert("内存告警", 
                String.format("堆内存使用率过高: %.2f%%", metrics.heapUsage));
        }
        
        // 检查系统负载
        if (metrics.systemLoad > SYSTEM_LOAD_THRESHOLD) {
            checkAndSendAlert("系统负载告警", 
                String.format("系统负载过高: %.2f", metrics.systemLoad));
        }
        
        // 检查线程数
        if (metrics.threadCount > THREAD_COUNT_THRESHOLD) {
            checkAndSendAlert("线程数告警", 
                String.format("线程数过多: %d", metrics.threadCount));
        }
    }
    
    /**
     * 检查并发送告警
     */
    private void checkAndSendAlert(String alertType, String message) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAlertTime.get() >= ALERT_COOLDOWN_MS) {
            lastAlertTime.set(currentTime);
            sendAlert(alertType, message);
        }
    }
    
    /**
     * 发送告警
     */
    private void sendAlert(String alertType, String message) {
        System.err.println("🚨 [" + alertType + "] " + message);
        System.err.println("时间: " + new java.util.Date());
        
        // 这里可以集成其他告警方式，如：
        // - 发送邮件
        // - 发送钉钉/企业微信消息
        // - 写入日志文件
        // - 调用告警API
    }
    
    /**
     * 清理旧的指标数据
     */
    private void cleanupOldMetrics() {
        endpointMetrics.values().forEach(EndpointMetrics::cleanupOldData);
    }
    
    /**
     * 驱逐最旧的端点
     */
    private void evictOldestEndpoints() {
        while (endpointMetrics.size() > MAX_ENDPOINTS * 0.8) { // 保持80%的容量
            String oldestEndpoint = endpointAccessOrder.poll();
            if (oldestEndpoint != null) {
                endpointMetrics.remove(oldestEndpoint);
            }
        }
    }
    
    /**
     * 驱逐单个最旧端点
     */
    private void evictOldestEndpoint() {
        String oldestEndpoint = endpointAccessOrder.poll();
        if (oldestEndpoint != null) {
            endpointMetrics.remove(oldestEndpoint);
        }
    }
    
    /**
     * 更新端点访问顺序
     */
    private void updateEndpointAccessOrder(String endpoint) {
        endpointAccessOrder.remove(endpoint);
        endpointAccessOrder.offer(endpoint);
    }

    /**
     * 端点指标内部类 - 优化内存使用
     */
    public static class EndpointMetrics {
        private final AtomicLong requestCount = new AtomicLong(0);
        private final AtomicLong errorCount = new AtomicLong(0);
        private final AtomicLong totalResponseTime = new AtomicLong(0);
        private final AtomicLong minResponseTime = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong maxResponseTime = new AtomicLong(0);
        
        // 使用环形缓冲区存储最近的响应时间，避免内存无限增长
        private final ConcurrentLinkedQueue<Long> recentResponseTimes = new ConcurrentLinkedQueue<>();
        private final AtomicLong lastCleanupTime = new AtomicLong(0);
        private static final long CLEANUP_INTERVAL_MS = 300000; // 5分钟清理一次
        private static final int MAX_RECENT_TIMES = 50; // 最多保存50个最近的响应时间

        public void recordRequestStart() {
            requestCount.incrementAndGet();
        }

        public void recordRequestComplete(long responseTime, boolean isError) {
            if (isError) {
                errorCount.incrementAndGet();
            }
            
            totalResponseTime.addAndGet(responseTime);
            
            // 更新最小响应时间
            minResponseTime.updateAndGet(current -> Math.min(current, responseTime));
            
            // 更新最大响应时间
            maxResponseTime.updateAndGet(current -> Math.max(current, responseTime));
            
            // 添加到最近响应时间列表
            recentResponseTimes.offer(responseTime);
            
            // 定期清理旧数据
            cleanupOldData();
        }
        
        /**
         * 清理旧数据
         */
        public void cleanupOldData() {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastCleanupTime.get() > CLEANUP_INTERVAL_MS) {
                lastCleanupTime.set(currentTime);
                
                // 保持最近响应时间列表在合理大小
                while (recentResponseTimes.size() > MAX_RECENT_TIMES) {
                    recentResponseTimes.poll();
                }
            }
        }

        public long getRequestCount() { return requestCount.get(); }
        public long getErrorCount() { return errorCount.get(); }
        public long getTotalResponseTime() { return totalResponseTime.get(); }
        public long getMinResponseTime() { return minResponseTime.get(); }
        public long getMaxResponseTime() { return maxResponseTime.get(); }
        public double getAverageResponseTime() { 
            return requestCount.get() > 0 ? (double) totalResponseTime.get() / requestCount.get() : 0; 
        }
        public double getErrorRate() { 
            return requestCount.get() > 0 ? (double) errorCount.get() / requestCount.get() * 100 : 0; 
        }
        
        /**
         * 获取最近的响应时间统计
         */
        public Map<String, Object> getRecentStats() {
            Map<String, Object> stats = new java.util.HashMap<>();
            List<Long> times = new ArrayList<>(recentResponseTimes);
            
            if (!times.isEmpty()) {
                stats.put("recentCount", times.size());
                stats.put("recentAverage", times.stream().mapToLong(Long::longValue).average().orElse(0));
                stats.put("recentMin", times.stream().mapToLong(Long::longValue).min().orElse(0));
                stats.put("recentMax", times.stream().mapToLong(Long::longValue).max().orElse(0));
            }
            
            return stats;
        }
    }

    /**
     * 系统指标内部类
     */
    public static class SystemMetrics {
        private final long heapUsed;
        private final long heapMax;
        private final double heapUsage;
        private final double systemLoad;
        private final int threadCount;
        private final int peakThreadCount;

        public SystemMetrics(long heapUsed, long heapMax, double heapUsage, 
                           double systemLoad, int threadCount, int peakThreadCount) {
            this.heapUsed = heapUsed;
            this.heapMax = heapMax;
            this.heapUsage = heapUsage;
            this.systemLoad = systemLoad;
            this.threadCount = threadCount;
            this.peakThreadCount = peakThreadCount;
        }

        public long getHeapUsed() { return heapUsed; }
        public long getHeapMax() { return heapMax; }
        public double getHeapUsage() { return heapUsage; }
        public double getSystemLoad() { return systemLoad; }
        public int getThreadCount() { return threadCount; }
        public int getPeakThreadCount() { return peakThreadCount; }
    }

    /**
     * 性能统计信息
     */
    public static class PerformanceStats {
        private final long totalRequests;
        private final long errorRequests;
        private final long successfulRequests;
        private final long averageResponseTime;
        private final int activeConnections;
        private final SystemMetrics systemMetrics;
        private final ConcurrentHashMap<String, EndpointMetrics> endpointMetrics;

        public PerformanceStats(long totalRequests, long errorRequests, long successfulRequests,
                              long averageResponseTime, int activeConnections, 
                              SystemMetrics systemMetrics, ConcurrentHashMap<String, EndpointMetrics> endpointMetrics) {
            this.totalRequests = totalRequests;
            this.errorRequests = errorRequests;
            this.successfulRequests = successfulRequests;
            this.averageResponseTime = averageResponseTime;
            this.activeConnections = activeConnections;
            this.systemMetrics = systemMetrics;
            this.endpointMetrics = endpointMetrics;
        }

        public long getTotalRequests() { return totalRequests; }
        public long getErrorRequests() { return errorRequests; }
        public long getSuccessfulRequests() { return successfulRequests; }
        public long getAverageResponseTime() { return averageResponseTime; }
        public int getActiveConnections() { return activeConnections; }
        public SystemMetrics getSystemMetrics() { return systemMetrics; }
        public ConcurrentHashMap<String, EndpointMetrics> getEndpointMetrics() { return endpointMetrics; }
        public double getErrorRate() { 
            return totalRequests > 0 ? (double) errorRequests / totalRequests * 100 : 0; 
        }
        
        /**
         * 获取内存使用统计
         */
        public Map<String, Object> getMemoryStats() {
            Map<String, Object> stats = new java.util.HashMap<>();
            stats.put("heapUsedMB", systemMetrics.heapUsed / 1024 / 1024);
            stats.put("heapMaxMB", systemMetrics.heapMax / 1024 / 1024);
            stats.put("heapUsagePercent", systemMetrics.heapUsage);
            stats.put("endpointCount", endpointMetrics.size());
            return stats;
        }
        
        /**
         * 获取告警状态
         */
        public Map<String, Object> getAlertStatus() {
            Map<String, Object> alerts = new java.util.HashMap<>();
            alerts.put("heapUsageAlert", systemMetrics.heapUsage > HEAP_USAGE_THRESHOLD);
            alerts.put("systemLoadAlert", systemMetrics.systemLoad > SYSTEM_LOAD_THRESHOLD);
            alerts.put("responseTimeAlert", averageResponseTime > RESPONSE_TIME_THRESHOLD);
            alerts.put("threadCountAlert", systemMetrics.threadCount > THREAD_COUNT_THRESHOLD);
            return alerts;
        }
    }
}
