package com.hibiscus.signal.core;

/**
 * Functional interface for signal filtering.
 * Purpose:
 * - Determines whether a signal should be allowed to propagate.
 * - Used to block certain signals based on custom business logic.
 */
@FunctionalInterface
public interface SignalFilter {

    /**
     * Filters the signal.
     *
     * @param event  the name of the signal
     * @param sender the object that emitted the signal
     * @param params additional parameters of the signal
     * @return true if the signal should continue propagating, false if it should be blocked
     */
    boolean filter(String event, Object sender, Object... params);

    /**
     * Specifies the priority of the filter.
     * Filters with lower priority values will be executed first.
     *
     * @return priority value (default is 0)
     */
    default int getPriority() {
        return 0;
    }
}
