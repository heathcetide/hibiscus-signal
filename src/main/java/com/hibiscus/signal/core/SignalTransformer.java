package com.hibiscus.signal.core;

/**
 * Functional interface for signal parameter transformation.
 * Purpose:
 * - Allows modifying or transforming signal parameters before they are handled.
 * - Typically used for data adaptation or enrichment.
 */
@FunctionalInterface
public interface SignalTransformer {

    /**
     * Transforms the parameters of a signal before passing them to the handler.
     *
     * @param event   the name of the signal
     * @param sender  the object that emitted the signal
     * @param params  the original parameters of the signal
     * @return the transformed parameters
     */
    Object[] transform(String event, Object sender, Object... params);
}
