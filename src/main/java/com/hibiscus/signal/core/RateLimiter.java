package com.hibiscus.signal.core;

import java.util.Deque;
import java.util.LinkedList;

public class RateLimiter {
    private final int maxRequestsPerSecond;
    private final Deque<Long> timestamps = new LinkedList<>();

    public RateLimiter(int maxRequestsPerSecond) {
        this.maxRequestsPerSecond = maxRequestsPerSecond;
    }

    public synchronized boolean allowRequest() {
        long now = System.currentTimeMillis();
        long oneSecondAgo = now - 1000;

        while (!timestamps.isEmpty() && timestamps.peekFirst() < oneSecondAgo) {
            timestamps.pollFirst();
        }

        if (timestamps.size() < maxRequestsPerSecond) {
            timestamps.addLast(now);
            return true;
        } else {
            return false;
        }
    }
}
