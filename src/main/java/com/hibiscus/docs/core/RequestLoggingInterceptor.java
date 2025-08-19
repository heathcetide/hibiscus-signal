package com.hibiscus.docs.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    @Autowired
    private AppConfigProperties appConfigProperties;

    @Autowired
    private PerformanceMonitor performanceMonitor;

    private final ConcurrentHashMap<String, AtomicLong> requestCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> responseTimes = new ConcurrentHashMap<>();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!appConfigProperties.getAdvanced().isEnableRequestLogging()) {
            return true;
        }

        String requestId = generateRequestId();
        request.setAttribute("requestId", requestId);
        request.setAttribute("startTime", System.currentTimeMillis());

        // 记录请求信息
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        // 更新请求计数
        String key = method + ":" + uri;
        requestCounts.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();

        // 记录性能监控
        if (performanceMonitor != null) {
            performanceMonitor.recordRequestStart(key);
        }

        // 打印请求日志
        System.out.printf("[%s] [%s] %s %s - IP: %s - UA: %s%n", 
            LocalDateTime.now().format(formatter), 
            requestId, 
            method, 
            uri, 
            clientIp, 
            userAgent != null ? userAgent.substring(0, Math.min(userAgent.length(), 50)) : "Unknown"
        );

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        if (!appConfigProperties.getAdvanced().isEnableRequestLogging()) {
            return;
        }

        String requestId = (String) request.getAttribute("requestId");
        Long startTime = (Long) request.getAttribute("startTime");
        
        if (requestId != null && startTime != null) {
            long responseTime = System.currentTimeMillis() - startTime;
            int status = response.getStatus();
            boolean isError = status >= 400 || ex != null;
            
            // 记录响应日志
            System.out.printf("[%s] [%s] Response: %d - Time: %dms%n", 
                LocalDateTime.now().format(formatter), 
                requestId, 
                status, 
                responseTime
            );

            // 记录响应时间统计
            String key = request.getMethod() + ":" + request.getRequestURI();
            responseTimes.put(key, responseTime);

            // 记录性能监控
            if (performanceMonitor != null) {
                performanceMonitor.recordRequestComplete(key, responseTime, isError);
            }
        }
    }

    private String generateRequestId() {
        return "req_" + System.currentTimeMillis() + "_" + Thread.currentThread().getId();
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    // 获取统计信息
    public ConcurrentHashMap<String, AtomicLong> getRequestCounts() {
        return requestCounts;
    }

    public ConcurrentHashMap<String, Long> getResponseTimes() {
        return responseTimes;
    }

    // 获取平均响应时间
    public double getAverageResponseTime(String endpoint) {
        Long time = responseTimes.get(endpoint);
        return time != null ? time.doubleValue() : 0.0;
    }

    // 获取总请求数
    public long getTotalRequests() {
        return requestCounts.values().stream()
                .mapToLong(AtomicLong::get)
                .sum();
    }
}
