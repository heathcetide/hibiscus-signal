package com.hibiscus.signal.spring.anno;

import java.lang.annotation.*;

/**
 * Annotation to mark a class as a signal filter binding.
 * <p>
 * This binds a {@link com.hibiscus.signal.core.SignalFilter} implementation
 * to one or more signal types, enabling pre-processing of signals before delivery.
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SignalFilterBind {

    /**
     * The signal types this filter applies to. Wildcards ("*") are supported.
     *
     * @return an array of signal names
     */
    String[] value() default "*";
}