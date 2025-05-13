package com.hibiscus.signal.spring.configuration;
import com.hibiscus.signal.Signals;
import com.hibiscus.signal.config.SignalConfig;
import com.hibiscus.signal.config.SignalPriority;
import com.hibiscus.signal.core.ErrorHandler;
import com.hibiscus.signal.core.SignalContext;
import com.hibiscus.signal.spring.anno.SignalEmitter;
import com.hibiscus.signal.spring.anno.SignalHandler;
import com.sun.org.slf4j.internal.Logger;
import com.sun.org.slf4j.internal.LoggerFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.Order;

import java.lang.reflect.Method;

@Aspect
@Order(1)
public class SignalAspect implements ApplicationContextAware {

    private final Signals signalManager;
    private ApplicationContext applicationContext;
    private volatile boolean initialized = false;

    private static Logger log = LoggerFactory.getLogger(SignalAspect.class);

    public SignalAspect(Signals signalManager) {
        this.signalManager = signalManager;
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Around("@annotation(signalEmitter)")
    public Object handleSignalEmitter(ProceedingJoinPoint joinPoint, SignalEmitter signalEmitter) throws Throwable {
        String event = signalEmitter.value();
        // 获取注解中的 errorHandler 参数
        Class<? extends ErrorHandler> errorHandlerClass = signalEmitter.errorHandler();
        ErrorHandler errorHandler = errorHandlerClass.getDeclaredConstructor().newInstance(); // 使用反射实例化回调类

        // 发射信号
        signalManager.emit(event, joinPoint.getTarget(), errorHandler::handle, joinPoint.getArgs());
        return joinPoint.proceed();
    }

    // 切面拦截所有带有 @SignalHandler 注解的方法
    @Before("@annotation(signalHandler)")
    public void handleSignalHandler(SignalHandler signalHandler) {
        String event = signalHandler.value();
        SignalConfig signalConfig = new SignalConfig.Builder()
                .async(signalHandler.async())
                .groupName(signalHandler.groupName())
                .maxHandlers(signalHandler.maxHandlers())
                .maxRetries(signalHandler.maxRetries())
                .retryDelayMs(signalHandler.retryDelayMs())
                .timeoutMs(signalHandler.timeoutMs())
                .recordMetrics(signalHandler.recordMetrics())
                .priority(signalHandler.priority())
                .persistent(signalHandler.persistent())
                .build();

        Method method = getMethodFromAnnotation(signalHandler);

        // 绑定事件处理器
        if (method != null) {
            signalManager.connect(event, (sender, params) -> {
                try {
                    method.invoke(sender, params);
                } catch (Exception e) {
                    log.error("Error invoking handler for event: " + event, e);
                }
            },signalConfig);
            log.debug("Handler bound for event: " + event);
        }
    }

    // 获取方法信息
    private Method getMethodFromAnnotation(SignalHandler signalHandler) {
        String event = signalHandler.value();
        try {
            return applicationContext.getBean(signalHandler.target()).getClass().getMethod(signalHandler.methodName(), SignalContext.class);
        } catch (NoSuchMethodException e) {
            log.error("No method found for event handler: " + event, e);
        }
        return null;
    }
}
