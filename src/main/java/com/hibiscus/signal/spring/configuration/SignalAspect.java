package com.hibiscus.signal.spring.configuration;

import com.hibiscus.signal.Signals;
import com.hibiscus.signal.config.SignalConfig;
import com.hibiscus.signal.core.ErrorHandler;
import com.hibiscus.signal.core.SignalCallback;
import com.hibiscus.signal.core.SignalContext;
import com.hibiscus.signal.spring.anno.SignalEmitter;
import com.hibiscus.signal.spring.anno.SignalHandler;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

/**
 * Aspect class that handles the interception of methods annotated with
 * {@link SignalEmitter} and {@link SignalHandler}.
 * <p>
 * It integrates signal emission and registration with the {@link Signals} framework.
 */
@Aspect
@Order(1)
public class SignalAspect implements ApplicationContextAware {

    private final Signals signals;
    private ApplicationContext applicationContext;
    private volatile boolean initialized = false;

    private static final Logger log = LoggerFactory.getLogger(SignalAspect.class);

    /**
     * Constructs the SignalAspect with a provided Signals instance.
     *
     * @param signals the Signals manager to handle signal emission and processing
     */
    public SignalAspect(Signals signals) {
        this.signals = signals;
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * Intercepts methods annotated with {@link SignalEmitter} to emit signals after execution.
     *
     * @param joinPoint      the join point for the method invocation
     * @param signalEmitter  the annotation instance on the method
     * @return the method execution result
     * @throws Throwable in case of errors
     */
    @Around("@annotation(signalEmitter)")
    public Object handleSignalEmitter(ProceedingJoinPoint joinPoint, SignalEmitter signalEmitter) throws Throwable {
        String event = signalEmitter.value();

        Object[] args = joinPoint.getArgs();
        Parameter[] parameters = ((MethodSignature) joinPoint.getSignature()).getMethod().getParameters();

        // Build parameter map
        Map<String, Object> requestParams = new HashMap<>();
        if (args != null && parameters != null) {
            for (int i = 0; i < parameters.length && i < args.length; i++) {
                String paramName = parameters[i].getName();
                requestParams.put(paramName != null ? paramName : "arg" + i, args[i]);
            }
        }

        // Execute original method
        Object result = joinPoint.proceed();
        requestParams.put("result", result);

        // Instantiate callback and error handler
        Class<? extends ErrorHandler> errorHandlerClass = signalEmitter.errorHandler();
        ErrorHandler errorHandler = errorHandlerClass.getDeclaredConstructor().newInstance();

        Class<? extends SignalCallback> signalCallbackClass = signalEmitter.callback();
        SignalCallback signalCallback = signalCallbackClass.getDeclaredConstructor().newInstance();

        Map<String, Object> intermediateData = SignalContextCollector.getAndClear();

        SignalContext context = new SignalContext();
        context.setIntermediateValues(intermediateData);
        context.setAttributes(replaceNullValues(requestParams));

        context.initTrace(signalEmitter.value());
        SignalContextCollector.collectTraceInfo(context);
        signals.getMetrics().recordTrace(context);
        // Emit signal
        signals.emit(event, joinPoint.getTarget(),signalCallback, errorHandler::handle, context);
        return result;
    }

    /**
     * Intercepts methods annotated with {@link SignalHandler} for signal handling registration.
     *
     * @param signalHandler the signal handler annotation
     */
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

        if (method != null) {
            signals.connect(event, (sender, params) -> {
                try {
                    SignalContext context = findContext(params);
                    if (context == null) {
                        log.warn("Missing SignalContext for event: {}", event);
                        return;
                    }
                    method.invoke(sender, context);
                } catch (Exception e) {
                    log.error("Error invoking handler for event: " + event, e);
                }
            }, signalConfig);
            log.debug("Handler bound for event: " + event);
        }
    }

    /**
     * Resolves the target method defined in the {@link SignalHandler} annotation.
     * It expects the method to accept a single {@link SignalContext} parameter.
     *
     * @param signalHandler the annotation containing target class and method name
     * @return the resolved {@link Method} object, or null if the method is not found or signature mismatch
     */
    private Method getMethodFromAnnotation(SignalHandler signalHandler) {
        try {
            return signalHandler.target().getMethod(
                    signalHandler.methodName(),
                    SignalContext.class // 明确要求使用SignalContext参数
            );
        } catch (NoSuchMethodException e) {
            log.error("Method signature should be: {}(SignalContext)",
                    signalHandler.methodName());
            return null;
        }
    }

    /**
     * Automatically registers all signal handlers annotated with {@link SignalHandler}
     * on context startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initSignalHandlers() {
        if (initialized || applicationContext == null) {
            return;
        }

        try {
            String[] beanNames = applicationContext.getBeanDefinitionNames();

            for (String beanName : beanNames) {
                Object bean = applicationContext.getBean(beanName);
                Class<?> targetClass = AopUtils.getTargetClass(bean);

                for (Method method : targetClass.getDeclaredMethods()) {
                    SignalHandler annotation = AnnotationUtils.findAnnotation(method, SignalHandler.class);
                    if (annotation != null) {
                        Object targetBean = applicationContext.getBean(annotation.target());

                        Method targetMethod;
                        targetMethod = getMethodFromAnnotation(annotation);

                        SignalConfig signalConfig = new SignalConfig.Builder()
                                .async(annotation.async())
                                .groupName(annotation.groupName())
                                .maxHandlers(annotation.maxHandlers())
                                .maxRetries(annotation.maxRetries())
                                .retryDelayMs(annotation.retryDelayMs())
                                .timeoutMs(annotation.timeoutMs())
                                .recordMetrics(annotation.recordMetrics())
                                .priority(annotation.priority())
                                .persistent(annotation.persistent())
                                .build();

                        signals.connect(annotation.value(), (sender, params) -> {
                            try {
                                SignalContext context = (SignalContext) params[0];
                                targetMethod.invoke(targetBean, context);
                            } catch (Exception e) {
                                log.error("Signal processor execution failure: {}.{}()", annotation.target().getSimpleName(), annotation.methodName(), e);
                            }
                        }, signalConfig);

                        log.info("Registered signal processor: {} -> {}.{}", annotation.value(), annotation.target().getSimpleName(), annotation.methodName());
                    }
                }
            }

            initialized = true;
            log.info("All @SignalHandler processors have been successfully registered.");

        } catch (Exception e) {
            log.error("An exception occurred while initializing the signal processor.", e);
            throw e;
        }
    }

    /**
     * Replaces null values in a parameter map with the string "null".
     * This ensures that context attributes are fully populated for traceability,
     * and avoids NullPointerExceptions in downstream processing or logging.
     *
     * @param original the original map possibly containing null values
     * @return a new map with the same keys and no null values
     */
    private Map<String, Object> replaceNullValues(Map<String, Object> original) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : original.entrySet()) {
            result.put(entry.getKey(), entry.getValue() != null ? entry.getValue() : "null");
        }
        return result;
    }

    /**
     * Searches the provided parameter array for a {@link SignalContext} instance.
     * This is typically used to extract the context passed during signal emission.
     *
     * @param params the parameter list from a signal handler invocation
     * @return the first {@link SignalContext} found, or null if none exists
     */
    private SignalContext findContext(Object[] params) {
        for (Object param : params) {
            if (param instanceof SignalContext) {
                return (SignalContext) param;
            }
        }
        return null;
    }
}
