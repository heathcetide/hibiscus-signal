package com.hibiscus.signal.core;

import com.hibiscus.signal.utils.SignalTracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of the SignalCallback interface.
 * Purpose:
 * - Logs the outcomes of signal processing (success, error, and completion).
 * - Provides a basic tracing mechanism when the signal processing is complete.
 */
public class DefaultSignalCallback implements SignalCallback {

    // Logger for recording the callback actions
    private static final Logger log = LoggerFactory.getLogger(DefaultSignalCallback.class);

    /**
     * Called when signal processing is successful.
     *
     * @param event  the name of the signal event
     * @param sender the sender object that emitted the signal
     * @param params additional parameters associated with the signal
     */
    @Override
    public void onSuccess(String event, Object sender, Object... params) {
        log.info("Signal [{}] processed successfully by [{}]. Parameters: {}",
                event, sender.getClass().getSimpleName(), params);
    }

    /**
     * Called when an error occurs during signal processing.
     *
     * @param event  the name of the signal event
     * @param sender the sender object that emitted the signal
     * @param error  the error that occurred
     * @param params additional parameters associated with the signal
     */
    @Override
    public void onError(String event, Object sender, Throwable error, Object... params) {
        log.error("Signal [{}] failed to process by [{}]. Error: {}. Parameters: {}",
                event, sender.getClass().getSimpleName(), error.getMessage(), params, error);
    }

    /**
     * Called after the signal processing (regardless of success or error).
     * If the signal's parameters include a SignalContext, prints the tracing tree.
     *
     * @param event  the name of the signal event
     * @param sender the sender object that emitted the signal
     * @param params additional parameters associated with the signal
     */
    @Override
    public void onComplete(String event, Object sender, Object... params) {
        // Check if the parameters include a SignalContext for tracing
        SignalContext context = null;
        for (Object param : params) {
            if (param instanceof SignalContext) {
                context = (SignalContext) param;
                break;
            }
        }

        // If context is present, print the trace tree
        if (context != null) {
            SignalTracer.printTraceTree(context);
        }

        log.info("Signal [{}] processing completed by [{}]. Parameters: {}",
                event, sender.getClass().getSimpleName(), params);
    }
}
