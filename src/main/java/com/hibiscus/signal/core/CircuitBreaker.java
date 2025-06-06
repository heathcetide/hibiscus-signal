package com.hibiscus.signal.core;

/**
 * A simple Circuit Breaker implementation for managing fault tolerance.
 * Purpose:
 * - Protects the system from repeated failures by "tripping" the breaker.
 * - If failures exceed a threshold, the circuit opens, stopping further processing temporarily.
 * - After a timeout, it moves to HALF_OPEN state to test if recovery is possible.
 */
public class CircuitBreaker {

    /**
     * The possible states of the Circuit Breaker:
     * - CLOSED: Normal operation, requests pass through.
     * - OPEN: Circuit is open due to too many failures; requests are blocked.
     * - HALF_OPEN: Circuit is testing the waters to see if recovery is possible.
     */
    private enum State { CLOSED, OPEN, HALF_OPEN }

    private State state = State.CLOSED;      // Current state of the circuit
    private long lastFailureTime = 0;        // Timestamp of last failure (for OPEN state timeout tracking)
    private int failureCount = 0;            // Consecutive failure count

    private final int failureThreshold;      // Number of failures to trip the circuit
    private final long openTimeoutMs;        // How long the circuit stays OPEN before moving to HALF_OPEN
    private final int halfOpenTrialCount;    // Number of successful requests in HALF_OPEN to reset to CLOSED
    private int trialCount = 0;              // Successful trial count in HALF_OPEN

    /**
     * Constructor for the CircuitBreaker.
     *
     * @param failureThreshold   Number of failures to trip the circuit
     * @param openTimeoutMs      Time (ms) the circuit stays OPEN before transitioning to HALF_OPEN
     * @param halfOpenTrialCount Number of successful trials needed in HALF_OPEN to reset to CLOSED
     */
    public CircuitBreaker(int failureThreshold, long openTimeoutMs, int halfOpenTrialCount) {
        this.failureThreshold = failureThreshold;
        this.openTimeoutMs = openTimeoutMs;
        this.halfOpenTrialCount = halfOpenTrialCount;
    }

    /**
     * Checks whether the circuit is currently OPEN.
     * If the timeout for OPEN state has expired, transition to HALF_OPEN to test recovery.
     *
     * @return true if the circuit is OPEN (blocking requests), false otherwise
     */
    public synchronized boolean isOpen() {
        // If OPEN and timeout has passed, move to HALF_OPEN to allow trial requests
        if (state == State.OPEN && (System.currentTimeMillis() - lastFailureTime > openTimeoutMs)) {
            state = State.HALF_OPEN;
        }
        return state == State.OPEN;
    }

    /**
     * Called when a request succeeds.
     * If in HALF_OPEN, counts as a trial success.
     * If enough trials succeed, the circuit resets to CLOSED.
     * If in CLOSED, simply resets counters.
     */
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

    /**
     * Called when a request fails.
     * If in HALF_OPEN, immediately trip back to OPEN.
     * If in CLOSED, increment the failure count and trip if the threshold is reached.
     */
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

    /**
     * Trips the circuit to OPEN state, recording the last failure time
     * and resetting counters for future checks.
     */
    private void trip() {
        state = State.OPEN;
        lastFailureTime = System.currentTimeMillis();
        failureCount = 0;
        trialCount = 0;
    }

    /**
     * Resets the circuit to the normal CLOSED state, clearing all counters.
     */
    private void reset() {
        state = State.CLOSED;
        failureCount = 0;
        trialCount = 0;
    }
}