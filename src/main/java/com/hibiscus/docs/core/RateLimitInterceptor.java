package com.hibiscus.docs.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    @Autowired
    private AppConfigProperties appConfigProperties;

    // 存储每个IP的限流信息
    private final ConcurrentHashMap<String, RateLimitInfo> rateLimitMap = new ConcurrentHashMap<>();
    
    // 清理过期限流信息的间隔（毫秒）
    private static final long CLEANUP_INTERVAL = 60000; // 1分钟
    private volatile long lastCleanupTime = System.currentTimeMillis();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!appConfigProperties.getAdvanced().isEnableRateLimiting()) {
            return true;
        }

        String clientIp = getClientIpAddress(request);
        String endpoint = request.getMethod() + ":" + request.getRequestURI();
        
        // 检查是否需要清理过期数据
        cleanupExpiredData();
        
        // 检查限流
        if (!isAllowed(clientIp, endpoint)) {
            response.setStatus(429); // 429 Too Many Requests
            response.setHeader("Retry-After", "60");
            response.getWriter().write("Rate limit exceeded. Please try again later.");
            
            System.out.printf("[限流] IP: %s, 端点: %s - 请求被限流%n", clientIp, endpoint);
            return false;
        }
        
        return true;
    }

    private boolean isAllowed(String clientIp, String endpoint) {
        RateLimitInfo info = rateLimitMap.computeIfAbsent(clientIp + ":" + endpoint, 
            k -> new RateLimitInfo());
        
        long currentTime = System.currentTimeMillis();
        
        // 检查是否在新的时间窗口内
        if (currentTime - info.windowStart > 60000) { // 1分钟窗口
            info.windowStart = currentTime;
            info.requestCount.set(0);
            info.burstTokens.set(appConfigProperties.getAdvanced().getRateLimit().getBurstCapacity());
        }
        
        // 检查是否超过每分钟限制
        if (info.requestCount.get() >= appConfigProperties.getAdvanced().getRateLimit().getRequestsPerMinute()) {
            return false;
        }
        
        // 检查突发容量
        if (info.burstTokens.get() <= 0) {
            return false;
        }
        
        // 允许请求
        info.requestCount.incrementAndGet();
        info.burstTokens.decrementAndGet();
        
        // 每秒恢复一个令牌（突发容量）
        long tokensToRestore = (currentTime - info.lastTokenRestore) / 1000;
        if (tokensToRestore > 0) {
            int maxTokens = appConfigProperties.getAdvanced().getRateLimit().getBurstCapacity();
            int currentTokens = info.burstTokens.get();
            int newTokens = Math.min(maxTokens, currentTokens + (int) tokensToRestore);
            info.burstTokens.set(newTokens);
            info.lastTokenRestore = currentTime;
        }
        
        return true;
    }

    private void cleanupExpiredData() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCleanupTime > CLEANUP_INTERVAL) {
            rateLimitMap.entrySet().removeIf(entry -> 
                currentTime - entry.getValue().windowStart > 300000); // 5分钟过期
            lastCleanupTime = currentTime;
        }
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

    // 获取限流统计信息
    public ConcurrentHashMap<String, RateLimitInfo> getRateLimitStats() {
        return rateLimitMap;
    }

    // 限流信息内部类
    private static class RateLimitInfo {
        private long windowStart = System.currentTimeMillis();
        private long lastTokenRestore = System.currentTimeMillis();
        private final AtomicInteger requestCount = new AtomicInteger(0);
        private final AtomicInteger burstTokens = new AtomicInteger(20); // 默认突发容量
    }
}
