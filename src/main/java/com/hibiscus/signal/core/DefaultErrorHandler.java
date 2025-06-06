package com.hibiscus.signal.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default error handler implementation for the Signal system.
 * Purpose:
 * - Provides a fallback mechanism to handle errors that occur during signal processing.
 * - By default, logs the error using SLF4J.
 */
public class DefaultErrorHandler implements ErrorHandler {

    // Logger for recording error details
    private static final Logger log = LoggerFactory.getLogger(DefaultErrorHandler.class);

    /**
     * Handles the given error by logging it.
     *
     * @param error the throwable error that occurred
     */
    @Override
    public void handle(Throwable error) {
        // Default error handling logic: log the error
        log.error("Error occurred during signal processing: ", error);
    }
}
