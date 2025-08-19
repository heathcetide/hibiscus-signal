package com.hibiscus.signal.spring.configuration;


import com.hibiscus.signal.Signals;
import com.hibiscus.signal.core.SignalFilter;
import com.hibiscus.signal.core.SignalInterceptor;
import com.hibiscus.signal.core.SignalTransformer;
import com.hibiscus.signal.spring.anno.SignalFilterBind;
import com.hibiscus.signal.spring.anno.SignalInterceptorBind;
import com.hibiscus.signal.spring.anno.SignalTransformerBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

/**
 * Automatically detects and registers Signal-related components such as
 * interceptors, filters, and transformers based on their annotations.
 */
@Component
public class SignalComponentRegistrar implements BeanPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(SignalComponentRegistrar.class);

    private final Signals signals;

    public SignalComponentRegistrar(Signals signals) {
        this.signals = signals;
    }

    /**
     * Intercepts all Spring-initialized beans and automatically registers those that
     * implement {@link SignalInterceptor}, {@link SignalFilter}, or {@link SignalTransformer}
     * if they are annotated with their respective binding annotations.
     *
     * @param bean     the fully initialized bean instance
     * @param beanName the name of the bean in the Spring context
     * @return the same bean instance, possibly modified
     * @throws BeansException if any post-processing fails
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // Register signal interceptors
        if (bean instanceof SignalInterceptor) {
            SignalInterceptorBind annotation = bean.getClass().getAnnotation(SignalInterceptorBind.class);
            if (annotation != null) {
                registerInterceptor((SignalInterceptor) bean, annotation);
            }
        }
        // Register signal filters
        if (bean instanceof SignalFilter) {
            SignalFilterBind annotation = bean.getClass().getAnnotation(SignalFilterBind.class);
            if (annotation != null) {
                registerFilter((SignalFilter) bean, annotation);
            }
        }
        // Register signal transformers
        if (bean instanceof SignalTransformer) {
            SignalTransformerBind annotation = bean.getClass().getAnnotation(SignalTransformerBind.class);
            if (annotation != null) {
                registerTransformer((SignalTransformer) bean, annotation);
            }
        }
        return bean;
    }

    /**
     * Registers a signal interceptor to one or more events based on the annotation.
     * Supports wildcard `"*"` to apply the interceptor to all currently registered events.
     *
     * @param interceptor the SignalInterceptor instance
     * @param annotation  the associated {@link SignalInterceptorBind} annotation
     */
    private void registerInterceptor(SignalInterceptor interceptor, SignalInterceptorBind annotation) {
        for (String event : annotation.value()) {
            if ("*".equals(event)) {
                signals.getRegisteredEvents().forEach(e ->
                        signals.addSignalInterceptor(e, interceptor)
                );
            } else {
                signals.addSignalInterceptor(event, interceptor);
            }
            logger.info("Registered interceptor: {} for event: {}", interceptor.getClass().getSimpleName(), event);
        }
    }

    /**
     * Registers a signal filter to the specified events based on the annotation.
     *
     * @param filter      the SignalFilter instance
     * @param annotation  the associated {@link SignalFilterBind} annotation
     */
    private void registerFilter(SignalFilter filter, SignalFilterBind annotation) {
        for (String event : annotation.value()) {
            signals.addFilter(event, filter);
            logger.info("filter : {} are registered to event {}", filter, event );
        }
    }

    /**
     * Registers a signal transformer to the specified events based on the annotation.
     *
     * @param transformer the SignalTransformer instance
     * @param annotation  the associated {@link SignalTransformerBind} annotation
     */
    private void registerTransformer(SignalTransformer transformer, SignalTransformerBind annotation) {
        for (String event : annotation.value()) {
            signals.addSignalTransformer(event, transformer);
            logger.info("transformers : {} are registered to event {}", transformer, event );
        }
    }
}