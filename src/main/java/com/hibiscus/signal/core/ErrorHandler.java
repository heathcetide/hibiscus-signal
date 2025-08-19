package com.hibiscus.signal.core;

/**
 * Functional interface for handling errors during signal processing.
 * Purpose:
 * - Provides a way to plug in custom error handling strategies.
 * - Typically used to log, report, or handle errors in a way that aligns with business needs.
 */
@FunctionalInterface
public interface ErrorHandler {

    /**
     * Handles the given error.
     *
     * @param error the throwable error that occurred during signal processing
     */
    void handle(Throwable error);

}
