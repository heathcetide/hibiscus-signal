package com.hibiscus.signal.spring.anno;

import com.hibiscus.signal.config.SignalPriority;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SignalHandler {

    // The name of the signal
    String value();

    // The class containing the method to be invoked
    Class<?> target();

    // The name of the method to invoke
    String methodName();

    // Whether to invoke the method asynchronously
    boolean async() default true;

    // The maximum number of retries
    int maxRetries() default 0;

    // The interval between retries
    long retryDelayMs() default 1000;

    // The maximum number of handlers
    int maxHandlers() default 0;

    // The timeout for the handler
    long timeoutMs() default 2000;

    // Whether to record metrics
    boolean recordMetrics() default true;

    // The priority of the handler
    SignalPriority priority() default SignalPriority.MEDIUM;

    // The group name
    String groupName() default "";

    // Whether the handler is persistent
    boolean persistent() default false;
}