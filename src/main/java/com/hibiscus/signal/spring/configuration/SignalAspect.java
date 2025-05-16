package com.hibiscus.signal.spring.configuration;

import com.hibiscus.signal.Signals;
import com.hibiscus.signal.config.SignalConfig;
import com.hibiscus.signal.config.SignalPriority;
import com.hibiscus.signal.core.ErrorHandler;
import com.hibiscus.signal.core.SignalCallback;
import com.hibiscus.signal.core.SignalContext;
import com.hibiscus.signal.spring.anno.SignalEmitter;
import com.hibiscus.signal.spring.anno.SignalHandler;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Order(1)
public class SignalAspect implements ApplicationContextAware {

    private final Signals signals;
    private ApplicationContext applicationContext;
    private volatile boolean initialized = false;

    private static Logger log = LoggerFactory.getLogger(SignalAspect.class);

    public SignalAspect(Signals signals) {
        this.signals = signals;
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Around("@annotation(signalEmitter)")
    public Object handleSignalEmitter(ProceedingJoinPoint joinPoint, SignalEmitter signalEmitter) throws Throwable {
        String event = signalEmitter.value();
        Object[] args = joinPoint.getArgs();
        Parameter[] parameters = ((MethodSignature) joinPoint.getSignature()).getMethod().getParameters();

        Map<String, Object> requestParams = new HashMap<>();
        for (int i = 0; i < parameters.length; i++) {
            String paramName = parameters[i].getName();
            requestParams.put(paramName != null ? paramName : "arg" + i, args[i]);
        }

        Object result = joinPoint.proceed();
        requestParams.put("result", result);

        Class<? extends ErrorHandler> errorHandlerClass = signalEmitter.errorHandler();
        ErrorHandler errorHandler = errorHandlerClass.getDeclaredConstructor().newInstance();

        Class<? extends SignalCallback> signalCallbackClass = signalEmitter.callback();
        SignalCallback signalCallback = signalCallbackClass.getDeclaredConstructor().newInstance();

        Map<String, Object> intermediateData = SignalContextCollector.getAndClear();
        SignalContext context = new SignalContext();
        context.setIntermediateValues(intermediateData);
        context.setAttributes(requestParams);

        signals.emit(event, joinPoint.getTarget(),signalCallback, errorHandler::handle, context);

        return result;
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
            signals.connect(event, (sender, params) -> {
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
            Object bean = applicationContext.getBean(signalHandler.target());
            Class<?> originalClass = AopUtils.getTargetClass(bean); // 获取原始类
            return originalClass.getMethod(signalHandler.methodName(), SignalContext.class);
        } catch (NoSuchMethodException e) {
            log.error("No method found for event handler: " + event, e);
        } catch (Exception e) {
            log.error("Failed to resolve method for SignalHandler: " + event, e);
        }
        return null;
    }

    @EventListener(ContextRefreshedEvent.class)
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
                        try {
                            // 查找带 SignalContext 参数的方法
                            targetMethod = annotation.target()
                                    .getMethod(annotation.methodName(), SignalContext.class);
                        } catch (NoSuchMethodException e) {
                            log.error("找不到方法: {}.{}，方法签名应为 (SignalContext)", annotation.target().getSimpleName(), annotation.methodName(), e);
                            continue;
                        }

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
                                // 参数中查找 SignalContext 实例
                                for (Object param : params) {
                                    if (param instanceof SignalContext) {
                                        targetMethod.invoke(targetBean, param);
                                        return;
                                    }
                                }
                                log.warn("未找到 SignalContext 参数，跳过方法调用: {}.{}", annotation.target().getSimpleName(), annotation.methodName());
                            } catch (Exception e) {
                                log.error("信号处理器执行失败: {}.{}()", annotation.target().getSimpleName(), annotation.methodName(), e);
                            }
                        }, signalConfig);

                        log.info("已注册信号处理器: {} -> {}.{}", annotation.value(), annotation.target().getSimpleName(), annotation.methodName());
                    }
                }
            }

            initialized = true;
            log.info("所有 @SignalHandler 处理器已成功注册。");

        } catch (Exception e) {
            log.error("初始化信号处理器时发生异常", e);
            throw e;
        }
    }

}
