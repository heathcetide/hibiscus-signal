package com.hibiscus.signal.spring.anno;

import com.hibiscus.signal.core.DefaultErrorHandler;
import com.hibiscus.signal.core.DefaultSignalCallback;
import com.hibiscus.signal.core.ErrorHandler;
import com.hibiscus.signal.core.SignalCallback;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a method or class as a signal emitter.
 * <p>
 * When the annotated method is invoked, a signal will be automatically emitted
 * using the {@link com.hibiscus.signal.Signals} framework.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SignalEmitter {

    /**
     * The name of the signal/event to emit.
     *
     * @return signal name
     */
    String value() default "";

    /**
     * The custom error handler class to handle exceptions during signal emission.
     * Defaults to {@link DefaultErrorHandler}.
     *
     * @return the error handler class
     */
    Class<? extends ErrorHandler> errorHandler() default DefaultErrorHandler.class;

    /**
     * The callback to execute after signal emission.
     * Defaults to {@link DefaultSignalCallback}.
     *
     * @return the callback class
     */
    Class<? extends SignalCallback> callback() default DefaultSignalCallback.class;
}