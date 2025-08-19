package com.hibiscus.signal.spring.anno;

import java.lang.annotation.*;

/**
 * Annotation to bind a signal transformer to one or more signal types.
 * <p>
 * Signal transformers allow modification or enrichment of signal payloads
 * before they are consumed by listeners.
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SignalTransformerBind {

    /**
     * The signal names this transformer should apply to. Supports wildcards.
     *
     * @return an array of signal names
     */
    String[] value() default "*";
}