package com.hibiscus.signal.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SignalProtectionManager {
    private final Map<String, CircuitBreaker> breakers = new ConcurrentHashMap<>();
    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();

    public boolean isBlocked(String signal) {
        CircuitBreaker cb = breakers.get(signal);
        RateLimiter rl = limiters.get(signal);
        return (cb != null && cb.isOpen()) || (rl != null && !rl.allowRequest());
    }

    public void update(String signal, SignalMetrics metrics) {
        Map<String, Object> m = metrics.getMetrics(signal);
        long errorCount = (Long) m.getOrDefault("errorCount", 0L);
        long emitCount = (Long) m.getOrDefault("emitCount", 0L);

        CircuitBreaker cb = breakers.get(signal);
        if (cb != null) {
            if (emitCount == 0) return;
            double errorRate = (double) errorCount / emitCount;
            if (errorRate > 0.5) { // 可配置阈值
                cb.recordFailure();
            } else {
                cb.recordSuccess();
            }
        }

        // 限流器一般由 allowRequest 控制，不需要 metrics 驱动
    }

    public void registerCircuitBreaker(String signal, CircuitBreaker breaker) {
        breakers.put(signal, breaker);
    }

    public void registerRateLimiter(String signal, RateLimiter limiter) {
        limiters.put(signal, limiter);
    }

    public CircuitBreaker getCircuitBreaker(String signal) {
        return breakers.get(signal);
    }

    public RateLimiter getRateLimiter(String signal) {
        return limiters.get(signal);
    }

    public void removeProtection(String signal) {
        breakers.remove(signal);
        limiters.remove(signal);
    }

    public void clearAll() {
        breakers.clear();
        limiters.clear();
    }
}
