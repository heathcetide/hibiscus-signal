package com.hibiscus.signal.core;

public class CircuitBreaker {
    private enum State { CLOSED, OPEN, HALF_OPEN }

    private State state = State.CLOSED;
    private long lastFailureTime = 0;
    private int failureCount = 0;

    private final int failureThreshold;
    private final long openTimeoutMs;
    private final int halfOpenTrialCount;
    private int trialCount = 0;

    public CircuitBreaker(int failureThreshold, long openTimeoutMs, int halfOpenTrialCount) {
        this.failureThreshold = failureThreshold;
        this.openTimeoutMs = openTimeoutMs;
        this.halfOpenTrialCount = halfOpenTrialCount;
    }

    public synchronized boolean isOpen() {
        if (state == State.OPEN && (System.currentTimeMillis() - lastFailureTime > openTimeoutMs)) {
            state = State.HALF_OPEN;
        }
        return state == State.OPEN;
    }

    public synchronized void recordSuccess() {
        if (state == State.HALF_OPEN) {
            trialCount++;
            if (trialCount >= halfOpenTrialCount) {
                reset();
            }
        } else if (state == State.CLOSED) {
            reset();
        }
    }

    public synchronized void recordFailure() {
        if (state == State.HALF_OPEN) {
            trip();
        } else {
            failureCount++;
            if (failureCount >= failureThreshold) {
                trip();
            }
        }
    }

    private void trip() {
        state = State.OPEN;
        lastFailureTime = System.currentTimeMillis();
        failureCount = 0;
        trialCount = 0;
    }

    private void reset() {
        state = State.CLOSED;
        failureCount = 0;
        trialCount = 0;
    }
}
