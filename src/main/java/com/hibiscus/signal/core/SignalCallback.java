package com.hibiscus.signal.core;

/**
 * Callback interface for observing signal processing outcomes.
 * Purpose:
 * - Provides hooks for reacting to success, errors, and overall completion.
 * - Can be customized to implement logging, tracing, or additional business logic.
 */
public interface SignalCallback {

    /**
     * Called when the signal has been processed successfully.
     *
     * @param event  the name of the signal event
     * @param sender the object that emitted the signal
     * @param params additional parameters associated with the signal
     */
    default void onSuccess(String event, Object sender, Object... params) {}

    /**
     * Called when an error occurs during signal processing.
     *
     * @param event  the name of the signal event
     * @param sender the object that emitted the signal
     * @param error  the error that occurred
     * @param params additional parameters associated with the signal
     */
    default void onError(String event, Object sender, Throwable error, Object... params) {}

    /**
     * Called after signal processing is complete, regardless of success or error.
     *
     * @param event  the name of the signal event
     * @param sender the object that emitted the signal
     * @param params additional parameters associated with the signal
     */
    default void onComplete(String event, Object sender, Object... params) {}
}
