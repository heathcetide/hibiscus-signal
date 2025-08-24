package com.hibiscus.signal.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 信号管道处理器
 * 负责拦截器、过滤器、转换器的链式处理
 */
public class SignalPipeline {
    
    private static final Logger log = LoggerFactory.getLogger(SignalPipeline.class);
    
    private final Map<String, List<SignalInterceptor>> signalInterceptors = new ConcurrentHashMap<>();
    private final Map<String, List<SignalFilter>> signalFilters = new ConcurrentHashMap<>();
    private final Map<String, List<SignalTransformer>> signalTransformers = new ConcurrentHashMap<>();
    
    /**
     * 执行信号管道处理
     */
    public Object[] processPipeline(String event, Object sender, SignalContext context, Object... params) {
        // 1. 执行拦截器
        if (!executeInterceptors(event, sender, context, params)) {
            return null; // 被拦截器阻止
        }
        
        // 2. 执行过滤器
        if (!executeFilters(event, sender, context, params)) {
            return null; // 被过滤器阻止
        }
        
        // 3. 执行转换器
        return executeTransformers(event, sender, context, params);
    }
    
    /**
     * 执行拦截器链
     */
    private boolean executeInterceptors(String event, Object sender, SignalContext context, Object... params) {
        List<SignalInterceptor> interceptors = signalInterceptors.get(event);
        if (interceptors == null || interceptors.isEmpty()) {
            return true;
        }
        
        for (SignalInterceptor interceptor : interceptors) {
            String spanId = UUID.randomUUID().toString();
            String parentSpanId = context.getParentSpanId() != null ? context.getParentSpanId() : context.getEventId();
            
            SignalContext.Span span = new SignalContext.Span();
            span.setSpanId(spanId);
            span.setParentSpanId(parentSpanId);
            span.setOperation("Interceptor: " + interceptor.getClass().getSimpleName());
            span.setStartTime(System.currentTimeMillis());
            
            context.setParentSpanId(spanId);
            boolean allowed = interceptor.beforeHandle(event, sender, params);
            span.setEndTime(System.currentTimeMillis());
            context.addSpan(span);
            
            if (!allowed) {
                log.debug("Signal [{}] blocked by interceptor: {}", event, interceptor.getClass().getSimpleName());
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 执行过滤器链
     */
    private boolean executeFilters(String event, Object sender, SignalContext context, Object... params) {
        List<SignalFilter> filters = getSortedFilters(event);
        if (filters.isEmpty()) {
            return true;
        }
        
        for (SignalFilter filter : filters) {
            String spanId = UUID.randomUUID().toString();
            String parentSpanId = context.getParentSpanId() != null ? context.getParentSpanId() : context.getEventId();
            
            SignalContext.Span span = new SignalContext.Span();
            span.setSpanId(spanId);
            span.setParentSpanId(parentSpanId);
            span.setOperation("Filter: " + filter.getClass().getSimpleName());
            span.setStartTime(System.currentTimeMillis());
            
            context.setParentSpanId(spanId);
            boolean pass = filter.filter(event, sender, params);
            span.setEndTime(System.currentTimeMillis());
            context.addSpan(span);
            
            if (!pass) {
                log.debug("Signal [{}] filtered out by: {}", event, filter.getClass().getSimpleName());
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 执行转换器链
     */
    private Object[] executeTransformers(String event, Object sender, SignalContext context, Object... params) {
        List<SignalTransformer> transformers = signalTransformers.get(event);
        if (transformers == null || transformers.isEmpty()) {
            return params;
        }
        
        Object[] transformedParams = params;
        for (SignalTransformer transformer : transformers) {
            String spanId = UUID.randomUUID().toString();
            String parentSpanId = context.getParentSpanId() != null ? context.getParentSpanId() : context.getEventId();
            
            SignalContext.Span span = new SignalContext.Span();
            span.setSpanId(spanId);
            span.setParentSpanId(parentSpanId);
            span.setOperation("Transformer: " + transformer.getClass().getSimpleName());
            span.setStartTime(System.currentTimeMillis());
            
            context.setParentSpanId(spanId);
            transformedParams = transformer.transform(event, sender, transformedParams);
            span.setEndTime(System.currentTimeMillis());
            context.addSpan(span);
        }
        
        return transformedParams;
    }
    
    /**
     * 获取排序后的过滤器列表
     */
    private List<SignalFilter> getSortedFilters(String event) {
        List<SignalFilter> filters = signalFilters.getOrDefault(event, Collections.emptyList());
        List<SignalFilter> copy = new ArrayList<>(filters);
        copy.sort(Comparator.comparingInt(SignalFilter::getPriority));
        return copy;
    }
    
    /**
     * 添加拦截器
     */
    public void addInterceptor(String event, SignalInterceptor interceptor) {
        signalInterceptors.computeIfAbsent(event, k -> new ArrayList<>()).add(interceptor);
        log.info("Interceptor [{}] added to event [{}]", interceptor.getClass().getSimpleName(), event);
    }
    
    /**
     * 添加过滤器
     */
    public void addFilter(String event, SignalFilter filter) {
        signalFilters.computeIfAbsent(event, k -> new ArrayList<>()).add(filter);
    }
    
    /**
     * 添加转换器
     */
    public void addTransformer(String event, SignalTransformer transformer) {
        signalTransformers.computeIfAbsent(event, k -> new ArrayList<>()).add(transformer);
    }
    
    /**
     * 获取拦截器
     */
    public Map<String, List<SignalInterceptor>> getSignalInterceptors() {
        return new ConcurrentHashMap<>(signalInterceptors);
    }
    
    /**
     * 获取过滤器
     */
    public Map<String, List<SignalFilter>> getSignalFilters() {
        return new ConcurrentHashMap<>(signalFilters);
    }
    
    /**
     * 获取转换器
     */
    public Map<String, List<SignalTransformer>> getSignalTransformers() {
        return new ConcurrentHashMap<>(signalTransformers);
    }
}
