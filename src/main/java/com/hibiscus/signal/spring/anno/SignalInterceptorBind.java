package com.hibiscus.signal.spring.anno;

import java.lang.annotation.*;

/**
 * Annotation to register a class as a signal interceptor.
 * <p>
 * Interceptors allow pre- and post-processing of signals before/after dispatch.
 * This annotation maps the interceptor to specific signal types.
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SignalInterceptorBind {

    /**
     * The signal types this interceptor should be applied to. Wildcards supported.
     *
     * @return an array of signal identifiers
     */
    String[] value() default "*";
}