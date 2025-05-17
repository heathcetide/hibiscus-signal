package com.hibiscus.signal.spring.anno;

import com.hibiscus.signal.config.SignalPriority;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to register a method as a signal handler.
 * <p>
 * This is used to configure and bind methods to specific signal events.
 * The annotated method must be declared in the target class specified.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SignalHandler {

    /**
     * The name of the signal this handler listens to.
     *
     * @return signal name
     */
    String value();

    /**
     * The class that contains the method to invoke.
     *
     * @return the target class
     */
    Class<?> target();

    /**
     * The name of the method to invoke when the signal is emitted.
     * Must accept a single parameter of type {@link com.hibiscus.signal.core.SignalContext}.
     *
     * @return method name
     */
    String methodName();

    /**
     * Whether the handler should be executed asynchronously.
     *
     * @return true if async, false otherwise
     */
    boolean async() default true;

    /**
     * Maximum number of retry attempts in case of failure.
     *
     * @return retry count
     */
    int maxRetries() default 0;

    /**
     * Delay in milliseconds between retry attempts.
     *
     * @return delay in ms
     */
    long retryDelayMs() default 1000;

    /**
     * Maximum number of concurrent handlers allowed for this signal.
     *
     * @return max handlers
     */
    int maxHandlers() default 10;

    /**
     * Timeout in milliseconds for the handler execution.
     *
     * @return timeout duration
     */
    long timeoutMs() default 2000;

    /**
     * Whether metrics should be collected for this handler.
     *
     * @return true if metrics are recorded
     */
    boolean recordMetrics() default true;

    /**
     * The priority level of the signal handler.
     *
     * @return signal priority
     */
    SignalPriority priority() default SignalPriority.MEDIUM;

    /**
     * Logical group name for categorizing handlers.
     *
     * @return group name
     */
    String groupName() default "";

    /**
     * Whether the handler is persistent across application restarts.
     *
     * @return true if persistent
     */
    boolean persistent() default false;
}