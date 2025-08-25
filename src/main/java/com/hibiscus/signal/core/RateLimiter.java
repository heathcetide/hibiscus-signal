package com.hibiscus.signal.core;

import java.util.Deque;
import java.util.LinkedList;

/**
 * A simple rate limiter that restricts the number of allowed requests per second.
 * Purpose:
 * - Prevents excessive requests from overwhelming the system.
 * - Uses a sliding window approach to track requests in the last 1 second.
 */
public class RateLimiter {

    /**
     * Maximum number of allowed requests per second
     */
    private final int maxRequestsPerSecond;

    /**
     * Timestamps of recent requests
     */
    private final Deque<Long> timestamps = new LinkedList<>();

    /**
     * Constructs a RateLimiter with the specified maximum requests per second.
     *
     * @param maxRequestsPerSecond the maximum allowed requests per second
     */
    public RateLimiter(int maxRequestsPerSecond) {
        this.maxRequestsPerSecond = maxRequestsPerSecond;
    }

    /**
     * Checks whether a new request is allowed at the current time.
     * - Removes outdated timestamps that are older than 1 second.
     * - If the number of requests in the last second is below the limit, the request is allowed.
     *
     * @return true if the request is allowed, false otherwise
     */
    public synchronized boolean allowRequest() {
        long now = System.currentTimeMillis();
        long oneSecondAgo = now - 1000;

        // Remove timestamps that are outside the 1-second window
        while (!timestamps.isEmpty() && timestamps.peekFirst() < oneSecondAgo) {
            timestamps.pollFirst();
        }

        // If the number of requests is below the threshold, allow the new request
        if (timestamps.size() < maxRequestsPerSecond) {
            timestamps.addLast(now);
            return true;
        } else {
            // Rate limit exceeded
            return false;
        }
    }
    
    /**
     * Checks whether a request can be allowed without actually allowing it.
     * This is a read-only operation that doesn't change the internal state.
     *
     * @return true if a request can be allowed, false otherwise
     */
    public synchronized boolean canAllowRequest() {
        long now = System.currentTimeMillis();
        long oneSecondAgo = now - 1000;

        // Remove timestamps that are outside the 1-second window
        while (!timestamps.isEmpty() && timestamps.peekFirst() < oneSecondAgo) {
            timestamps.pollFirst();
        }

        // Return whether a request can be allowed (without adding the timestamp)
        return timestamps.size() < maxRequestsPerSecond;
    }
}
