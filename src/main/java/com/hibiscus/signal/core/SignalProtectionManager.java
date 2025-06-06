package com.hibiscus.signal.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages circuit breakers and rate limiters for signals.
 * Purpose:
 * - Protects the system from overload (rate limiting) and repeated errors (circuit breaker).
 * - Tracks protection mechanisms for each signal individually.
 */
public class SignalProtectionManager {

    // Map of signal names to their CircuitBreaker instances
    private final Map<String, CircuitBreaker> breakers = new ConcurrentHashMap<>();

    // Map of signal names to their RateLimiter instances
    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();

    /**
     * Checks if a signal is currently blocked by a circuit breaker or rate limiter.
     *
     * @param signal the name of the signal
     * @return true if the signal is blocked (either by circuit breaker or rate limiter), false otherwise
     */
    public boolean isBlocked(String signal) {
        CircuitBreaker cb = breakers.get(signal);
        RateLimiter rl = limiters.get(signal);
        return (cb != null && cb.isOpen()) || (rl != null && !rl.allowRequest());
    }

    /**
     * Updates the state of the circuit breaker for a signal based on its error and emit metrics.
     *
     * @param signal  the name of the signal
     * @param metrics the SignalMetrics object tracking signal statistics
     */
    public void update(String signal, SignalMetrics metrics) {
        Map<String, Object> m = metrics.getMetrics(signal);
        long errorCount = (Long) m.getOrDefault("errorCount", 0L);
        long emitCount = (Long) m.getOrDefault("emitCount", 0L);

        CircuitBreaker cb = breakers.get(signal);
        if (cb != null) {
            if (emitCount == 0) return; // No emit data yet, skip
            double errorRate = (double) errorCount / emitCount;
            if (errorRate > 0.5) { // Threshold for tripping circuit breaker; can be configurable
                cb.recordFailure();
            } else {
                cb.recordSuccess();
            }
        }

        // Rate limiter does not depend on metrics; it uses allowRequest directly
    }

    /**
     * Registers a circuit breaker for a specific signal.
     *
     * @param signal   the name of the signal
     * @param breaker  the CircuitBreaker instance
     */
    public void registerCircuitBreaker(String signal, CircuitBreaker breaker) {
        breakers.put(signal, breaker);
    }

    /**
     * Registers a rate limiter for a specific signal.
     *
     * @param signal   the name of the signal
     * @param limiter  the RateLimiter instance
     */
    public void registerRateLimiter(String signal, RateLimiter limiter) {
        limiters.put(signal, limiter);
    }

    /**
     * Retrieves the CircuitBreaker for a given signal.
     *
     * @param signal the name of the signal
     * @return the CircuitBreaker or null if none is registered
     */
    public CircuitBreaker getCircuitBreaker(String signal) {
        return breakers.get(signal);
    }

    /**
     * Retrieves the RateLimiter for a given signal.
     *
     * @param signal the name of the signal
     * @return the RateLimiter or null if none is registered
     */
    public RateLimiter getRateLimiter(String signal) {
        return limiters.get(signal);
    }

    /**
     * Removes protection (both CircuitBreaker and RateLimiter) for a specific signal.
     *
     * @param signal the name of the signal
     */
    public void removeProtection(String signal) {
        breakers.remove(signal);
        limiters.remove(signal);
    }

    /**
     * Clears all circuit breakers and rate limiters for all signals.
     */
    public void clearAll() {
        breakers.clear();
        limiters.clear();
    }
}
