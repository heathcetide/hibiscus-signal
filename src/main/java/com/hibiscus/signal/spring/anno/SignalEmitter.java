package com.hibiscus.signal.spring.anno;

import com.hibiscus.signal.core.DefaultErrorHandler;
import com.hibiscus.signal.core.DefaultSignalCallback;
import com.hibiscus.signal.core.ErrorHandler;
import com.hibiscus.signal.core.SignalCallback;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SignalEmitter {

    String value() default "";

    Class<? extends ErrorHandler> errorHandler() default DefaultErrorHandler.class;

    Class<? extends SignalCallback> callback() default DefaultSignalCallback.class;
}