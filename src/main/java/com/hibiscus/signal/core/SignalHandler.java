package com.hibiscus.signal.core;

/**
 * Functional interface representing a handler for signals.
 * Purpose:
 * - Defines the contract for any handler that responds to a signal event.
 * - The handle method is called when the signal is emitted.
 */
@FunctionalInterface
public interface SignalHandler {

    /**
     * Handles the signal event.
     *
     * @param sender the sender that emitted the signal
     * @param params additional parameters or context for the signal
     * @throws InterruptedException if the handler's execution is interrupted
     */
    void handle(Object sender, Object... params) throws InterruptedException;

}
