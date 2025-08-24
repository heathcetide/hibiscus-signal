package com.hibiscus.signal;

import com.hibiscus.signal.config.EnhancedSignalPersistence;
import com.hibiscus.signal.config.SignalConfig;
import com.hibiscus.signal.config.SignalPriority;
import com.hibiscus.signal.core.*;
import com.hibiscus.signal.exceptions.SignalProcessingException;
import com.hibiscus.signal.spring.config.SignalProperties;
import com.hibiscus.signal.utils.SnowflakeIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static com.hibiscus.signal.core.EventType.ADD_HANDLER;

/**
 * The central component for managing signal connections, emissions,
 * event handling, filtering, transformation, and metrics.
 * Also integrates protection mechanisms like circuit breakers and rate limiters.
 */
@Service
public class Signals {

    /**
     * 监听器集合
     */
    private final Map<String, List<SigHandler>> sigHandlers = new ConcurrentHashMap<>();

    /**
     * 存储每个事件的拦截器
     */
    private final Map<String, List<SignalInterceptor>> signalInterceptors = new ConcurrentHashMap<>();

    /**
     * 信号配置
     */
    private final Map<String, SignalConfig> signalConfigs = new ConcurrentHashMap<>();

    /**
     * 信号统计
     */
    private final SignalMetrics metrics = new SignalMetrics();

    /**
     * 信号过滤器集合
     */
    private final Map<String, List<SignalFilter>> signalFilters = new ConcurrentHashMap<>();

    /**
     * 是否在循环中
     */
    private volatile boolean inLoop = false;

    /**
     * 事件队列
     */
    private final EnumMap<SignalPriority, BlockingQueue<SigHandler>> priorityQueues = new EnumMap<>(SignalPriority.class);

    /**
     * 信号转换器集合
     */
    private final Map<String, List<SignalTransformer>> signalTransformers = new ConcurrentHashMap<>();

    /**
     * 线程池
     */
    private final ExecutorService executorService;


    private final SignalProtectionManager protectionManager = new SignalProtectionManager();

    /**
     * Signals Properties
     */
    @Autowired
    private SignalProperties signalProperties;

    /**
     * 日志记录器
     */
    private static final Logger log = LoggerFactory.getLogger(Signals.class);

    public Signals(@Qualifier("signalExecutor") ExecutorService executorService) {
        this.executorService = executorService;
        for (SignalPriority p : SignalPriority.values()) {
            priorityQueues.put(p, new LinkedBlockingQueue<>());
        }
    }

    /**
     * 绑定事件
     */
    public long connect(String event, SignalHandler handler){
        return connect(event, handler, new SignalConfig.Builder().build());
    }

    /**
     * 绑定事件
     */
    public void connect(String event, SignalHandler handler, SignalConfig signalConfig, String handlerName) {
        signalConfigs.computeIfAbsent(event, k -> signalConfig);
        long id = SnowflakeIdGenerator.nextId();
        SigHandler signalHandler = new SigHandler(id, ADD_HANDLER, event, handler, signalConfig.getPriority());

        // 设置 handlerName
        signalHandler.setHandlerName(handlerName != null ? handlerName : handler.getClass().getName());
        priorityQueues.get(signalConfig.getPriority()).offer(signalHandler);
        processEvents();
    }


    /**
     * 绑定事件
     */
    public long connect(String event, SignalHandler handler, SignalConfig signalConfig){
        signalConfigs.computeIfAbsent(event, k -> signalConfig);
        long id = SnowflakeIdGenerator.nextId();
        SigHandler signalHandler = new SigHandler(id,ADD_HANDLER, event, handler, signalConfig.getPriority());
        // 根据配置的优先级放入对应队列
        priorityQueues.get(signalConfig.getPriority()).offer(signalHandler);
        processEvents();
        return id;
    }

    /**
     * 绑定事件
     */
    public long connect(String event, SignalHandler handler, SignalContext context){
        return connect(event, handler, new SignalConfig.Builder().build(), context);
    }

    /**
     * 绑定事件
     */
    public long connect(String event, SignalHandler handler, SignalConfig signalConfig, SignalContext context){
        signalConfigs.computeIfAbsent(event, k -> signalConfig);
        long id = SnowflakeIdGenerator.nextId();
        SigHandler signalHandler = new SigHandler(id,ADD_HANDLER, event, handler, signalConfig.getPriority());
        signalHandler.setSignalContext(context);
        // 根据配置的优先级放入对应队列
        priorityQueues.get(signalConfig.getPriority()).offer(signalHandler);
        processEvents();
        return id;
    }

    /**
     * 解开连接
     */
    public void disconnect(String event, long id) {
        SignalConfig config = signalConfigs.getOrDefault(event, new SignalConfig.Builder().build());
        SigHandler ev = new SigHandler(id, EventType.REMOVE_HANDLER, event, null, config.getPriority());
        priorityQueues.get(config.getPriority()).offer(ev);
        processEvents();
    }

    /**
     * 解开连接
     */
    public void disconnect(String event, long id, SignalContext context) {
        SignalConfig config = signalConfigs.getOrDefault(event, new SignalConfig.Builder().build());
        SigHandler ev = new SigHandler(id, EventType.REMOVE_HANDLER, event, null, config.getPriority());
        ev.setSignalContext(context);
        priorityQueues.get(config.getPriority()).offer(ev);
        processEvents();
    }

    /**
     * 处理事件
     */
    public void processEvents(){
        if (inLoop) return;
        synchronized (this) {
            if (allQueuesEmpty()) return;
            inLoop = true;
        }
        try {
            // 按优先级顺序处理：HIGH -> MEDIUM -> LOW
            processPriorityQueue(SignalPriority.HIGH);
            processPriorityQueue(SignalPriority.MEDIUM);
            processPriorityQueue(SignalPriority.LOW);
        } finally {
            inLoop = false;
        }
    }

    /**
     * 处理队列为空的情况
     */
    private void processPriorityQueue(SignalPriority priority) {
        BlockingQueue<SigHandler> queue = priorityQueues.get(priority);
        SigHandler sigHandler;
        while ((sigHandler = queue.poll()) != null) {
            List<SigHandler> sigs = sigHandlers.computeIfAbsent(sigHandler.getSignalName(), k -> new CopyOnWriteArrayList<>());
            SignalConfig config = signalConfigs.computeIfAbsent(sigHandler.getSignalName(), k -> new SignalConfig.Builder().build());
            switch (sigHandler.getEvType()){
                case ADD_HANDLER: // evTypeAdd
                    if (sigs.size() < config.getMaxHandlers()) {
                        sigs.add(sigHandler);
                        if (config.isRecordMetrics()) {
                            metrics.recordHandlerAdded(sigHandler.getSignalName());
                        }
                    }
                    break;
                case REMOVE_HANDLER: // evTypeRemove
                    SigHandler finalEvent = sigHandler;
                    sigs.removeIf(sh -> sh.getId() == finalEvent.getId());
                    if (config.isRecordMetrics()) {
                        metrics.recordHandlerRemoved(sigHandler.getSignalName());
                    }
                    break;
                default:
                    log.warn("未知事件类型 {}", sigHandler.getEvType());
            }
        }
    }

    /**
     * 判断队列是否为空
     */
    private boolean allQueuesEmpty() {
        return priorityQueues.values().stream().allMatch(Queue::isEmpty);
    }

    /**
     * 发射信号
     */
    public void emit(String event, Object sender, Consumer<Throwable> errorHandler, Object... params) {

        if (protectionManager.isBlocked(event)) {
            return;
        }

        SignalContext context = findContext(params);
        if (context == null) {
            context = new SignalContext();
            Object[] newParams = new Object[params.length + 1];
            newParams[0] = context;
            System.arraycopy(params, 0, newParams, 1, params.length);
            params = newParams;
        }

        // 获取事件的拦截器并按优先级排序
        List<SignalInterceptor> interceptors = signalInterceptors.get(event);

        if (interceptors != null){
            // 遍历拦截器执行 beforeHandle
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
                    return;
                }
            }
        }

        // 先获取该事件的所有过滤器
        List<SignalFilter> filters = getSortedFilters(event);

        // 遍历过滤器，如果有任何一个过滤器返回 false，则停止传播
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
                return;
            }
        }

        // 获取该事件的所有信号转换器并应用
        List<SignalTransformer> transformers = signalTransformers.get(event);
        if (transformers != null) {
            for (SignalTransformer transformer : transformers) {
                String spanId = UUID.randomUUID().toString();
                String parentSpanId = context.getParentSpanId() != null ? context.getParentSpanId() : context.getEventId();

                SignalContext.Span span = new SignalContext.Span();
                span.setSpanId(spanId);
                span.setParentSpanId(parentSpanId);
                span.setOperation("Transformer: " + transformer.getClass().getSimpleName());
                span.setStartTime(System.currentTimeMillis());

                context.setParentSpanId(spanId);
                params = transformer.transform(event, sender, params);

                span.setEndTime(System.currentTimeMillis());
                context.addSpan(span);
            }
        }

        SignalConfig config = signalConfigs.getOrDefault(event, new SignalConfig.Builder().build());
        if (config.isRecordMetrics()) {
            metrics.recordEmit(event);
        }

        List<SigHandler> sigs = sigHandlers.get(event);
        if (sigs == null || sigs.isEmpty()) return;

        if (config.isAsync()) {
            emitAsync(event, sender, sigs, config, errorHandler, null, params);
        } else {
            emitSync(event, sender, sigs, config, errorHandler, null, params);
        }
    }

    /**
     * 发射信号
     */
    public void emit(String event, Object sender, SignalCallback callback, Consumer<Throwable> errorHandler, Object... params) {
        if (protectionManager.isBlocked(event)) {
            if (callback != null) {
                callback.onError(event, sender, new RuntimeException("Signal blocked (circuit open or rate limited)"), params);
                callback.onComplete(event, sender, params);
            }
            return;
        }

        SignalContext context = findContext(params);
        if (context == null) {
            context = new SignalContext();
            Object[] newParams = new Object[params.length + 1];
            newParams[0] = context;
            System.arraycopy(params, 0, newParams, 1, params.length);
            params = newParams;
        }

        // 获取事件的拦截器并按优先级排序
        List<SignalInterceptor> interceptors = signalInterceptors.get(event);

        if (interceptors != null){
            // 遍历拦截器执行 beforeHandle
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
                    return;
                }
            }
        } else {
            interceptors = new ArrayList<>();
        }

        // 先获取该事件的所有过滤器
        List<SignalFilter> filters = getSortedFilters(event);

        // 遍历过滤器，如果有任何一个过滤器返回 false，则停止传播
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
                if (callback != null) {
                    callback.onError(event, sender, new RuntimeException("Signal propagation stopped by filter"), params);
                    callback.onComplete(event, sender, params);
                }
                return;
            }
        }

        // 获取该事件的所有信号转换器并应用
        List<SignalTransformer> transformers = signalTransformers.get(event);
        if (transformers != null) {
            for (SignalTransformer transformer : transformers) {
                String spanId = UUID.randomUUID().toString();
                String parentSpanId = context.getParentSpanId() != null ? context.getParentSpanId() : context.getEventId();

                SignalContext.Span span = new SignalContext.Span();
                span.setSpanId(spanId);
                span.setParentSpanId(parentSpanId);
                span.setOperation("Transformer: " + transformer.getClass().getSimpleName());
                span.setStartTime(System.currentTimeMillis());

                context.setParentSpanId(spanId);
                params = transformer.transform(event, sender, params);

                span.setEndTime(System.currentTimeMillis());
                context.addSpan(span);
            }
        }

        SignalConfig config = signalConfigs.getOrDefault(event, new SignalConfig.Builder().build());
        if (config.isRecordMetrics()) {
            metrics.recordEmit(event);
        }

        List<SigHandler> sigs = sigHandlers.get(event);
        if (sigs == null) {
            if (callback != null) {
                callback.onError(event, sender, new RuntimeException("No handlers for event: " + event), params);
                callback.onComplete(event, sender, params);
            }
            return; // 没有处理器，直接返回
        }

        if (config.isAsync()) {
            emitAsync(event, sender, sigs, config, errorHandler, callback, params);
        } else {
            emitSync(event, sender, sigs, config, errorHandler, callback, params);
        }
    }

    /**
     * 同步发射信号
     */
    private void emitSync(String event, Object sender, List<SigHandler> sigs,
                          SignalConfig config, Consumer<Throwable> errorHandler,SignalCallback callback,  Object... params) {
        for (SigHandler sig : sigs) {
            long startTime = System.currentTimeMillis();
            try {
                executeWithTracing(event, sig, sender, config, params);
                if (config.isRecordMetrics()) {
                    metrics.recordProcessingTime(event, System.currentTimeMillis() - startTime);
                }
                if (callback != null) {
                    callback.onSuccess(event, sender, params); // 调用成功回调
                }
                protectionManager.update(event, metrics);
            } catch (Exception e) {
                protectionManager.update(event, metrics);
                handleError(event, config, errorHandler, e);
                if (callback != null) {
                    callback.onError(event, sender, e, params); // 调用失败回调
                }
            } finally {
                if (callback != null) {
                    callback.onComplete(event, sender, params); // 信号处理完成回调
                }
            }
        }
    }

    /**
     * 异步发送信号
     */
    private void emitAsync(String event, Object sender, List<SigHandler> sigs,
                           SignalConfig config, Consumer<Throwable> errorHandler, SignalCallback callback,  Object... params) {
        for (SigHandler sig : sigs) {
            CompletableFuture.runAsync(() -> {
                long startTime = System.currentTimeMillis();
                try {
                    executeWithTracing(event, sig, sender, config, params);
                    if (config.isRecordMetrics()) {
                        metrics.recordProcessingTime(event, System.currentTimeMillis() - startTime);
                    }
                    if (callback != null) {
                        callback.onSuccess(event, sender, params); // 调用成功回调
                    }
                    protectionManager.update(event, metrics);
                } catch (Exception e) {
                    protectionManager.update(event, metrics);
                    handleError(event, config, errorHandler, e);
                    if (callback != null) {
                        callback.onError(event, sender, e, params); // 调用失败回调
                    }
                }finally {
                    if (callback != null) {
                        callback.onComplete(event, sender, params); // 信号处理完成回调
                    }
                }
            }, executorService);
        }
    }

    /**
     * 执行信号处理函数，并记录处理时间
     */
    private void executeWithRetry(String event, SigHandler sig, Object sender,
                                  SignalConfig config, Object... params) throws Exception {
        int retries = 0;
        Exception lastException = null;

        while (retries <= config.getMaxRetries()) {
            try {
                if (config.getTimeoutMs() > 0) {
                    executeWithTimeout(sig, sender, config.getTimeoutMs(), params);
                } else {
                    executeHandler(sig, sender, params);
                }
                return;
            } catch (Exception e) {
                lastException = e;
                retries++;
                if (retries <= config.getMaxRetries()) {
                    Thread.sleep(config.getRetryDelayMs());
                }
            }
        }

        if (lastException != null) {
            throw lastException;
        }
    }

    /**
     * 执行信号处理函数，并记录处理时间
     */
    private void executeWithTracing(String event, SigHandler sig, Object sender, SignalConfig config, Object... params) throws Exception {
        SignalContext context = findContext(params);
        if (context == null) {
            context = new SignalContext();
            Object[] newParams = new Object[params.length + 1];
            newParams[0] = context;
            System.arraycopy(params, 0, newParams, 1, params.length);
            params = newParams;
        }

        String spanId = UUID.randomUUID().toString();
        String parentSpanId = context.getParentSpanId() != null ? context.getParentSpanId() : context.getEventId();

        SignalContext.Span span = new SignalContext.Span();
        span.setSpanId(spanId);
        span.setParentSpanId(parentSpanId);
        String op = sig.getHandlerName() != null ? sig.getHandlerName() : "Handler: Unknown";
        span.setOperation(op);

        span.setStartTime(System.currentTimeMillis());

        context.setParentSpanId(spanId);

        try {
            executeWithRetry(event, sig, sender, config, params);
        } finally {
            span.setEndTime(System.currentTimeMillis());
            context.addSpan(span);
            if (signalProperties != null && signalProperties.getPersistent()){
                // 使用增强版持久化，支持追加写入
                String fullPath = signalProperties.getPersistenceDirectory() + "/" + signalProperties.getPersistenceFile();
                EnhancedSignalPersistence.appendToFile(
                    new SignalPersistenceInfo(sig, config, context, metrics.getAllMetrics()), 
                    fullPath
                );
                
                // 文件轮转检查
                if (signalProperties.getEnableFileRotation()) {
                    EnhancedSignalPersistence.rotateFileIfNeeded(fullPath, signalProperties.getMaxFileSizeBytes());
                }
            }
        }
    }

    /**
     * 执行超时处理
     */
    private void executeWithTimeout(SigHandler sig, Object sender, long timeoutMs, Object... params)
            throws Exception {
        // 如果线程池已关闭，回退到同步执行
        if (executorService.isShutdown() || executorService.isTerminated()) {
            executeHandler(sig, sender, params); // 直接同步执行
            return;
        }

        CompletableFuture<Void> task = CompletableFuture.runAsync(() -> {
            try {
                executeHandler(sig, sender, params);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executorService);

        try {
            task.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            task.cancel(true); // 尝试中断
            throw new SignalProcessingException("Signal handler execution timed out", 1001);
        }
    }

    /**
     * 处理错误
     */
    private void handleError(String event, SignalConfig config,
                             Consumer<Throwable> errorHandler, Exception e) {
        if (config.isRecordMetrics()) {
            metrics.recordError(event);
        }
        log.error("Signal [{}] handler error: {}", event, e.getMessage(), e);
        if (errorHandler != null) {
            errorHandler.accept(e);
        }
    }

    /**
     * close the executor service
     */
    // 应用关闭时的处理逻辑
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                // 等待一段时间让现有任务完成
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    executorService.shutdownNow(); // 强制关闭
                    if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                        System.err.println("Thread pool did not terminate");
                    }
                }
            } catch (InterruptedException ie) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 删除信号
     */
    public void clear(String... events) {
        for (String event : events) {
            sigHandlers.remove(event);
            signalConfigs.remove(event);
        }
    }

    /**
     * 获取信号统计信息
     */
    public SignalMetrics getMetrics() {
        return metrics;
    }

    /**
     * 绑定信号过滤器
     */
    public void addFilter(String event, SignalFilter filter) {
        signalFilters.computeIfAbsent(event, k -> new ArrayList<>()).add(filter);
    }

    /**
     * 获取信号过滤器
     */
    private List<SignalFilter> getSortedFilters(String event) {
        List<SignalFilter> filters = signalFilters.getOrDefault(event, Collections.emptyList());
        List<SignalFilter> copy = new ArrayList<>(filters);
        copy.sort(Comparator.comparingInt(SignalFilter::getPriority));
        return copy;
    }

    /**
     * 绑定信号转换器
     */
    public void addSignalTransformer(String event, SignalTransformer transformer) {
        signalTransformers.computeIfAbsent(event, k -> new ArrayList<>()).add(transformer);
    }

    /**
     * 绑定信号拦截器
     */
    public void addSignalInterceptor(String event, SignalInterceptor interceptor) {
        signalInterceptors.computeIfAbsent(event, k -> new ArrayList<>()).add(interceptor);
        // 不在原列表排序
        log.info("Interceptor [{}] added to event [{}]", interceptor.getClass().getSimpleName(), event);
    }

    /**
     * 配置信号保护
     */
    public void configureProtection(String event, CircuitBreaker breaker, RateLimiter limiter) {
        protectionManager.registerCircuitBreaker(event, breaker);
        protectionManager.registerRateLimiter(event, limiter);
    }

    /**
     * 执行信号处理程序
     */
    private void executeHandler(SigHandler handler, Object sender, Object... params) throws Exception {
        try {
            SignalContext context = findContext(params);
            if (context == null) {
                context = new SignalContext();
                Object[] newParams = new Object[params.length + 1];
                newParams[0] = context;
                System.arraycopy(params, 0, newParams, 1, params.length);
                params = newParams;
            }

            handler.getHandler().handle(sender, params);
        } catch (Exception e) {
            // 区分不同类型的异常，提供更明确的错误信息
            String errorMessage = "Signal handler execution failed: " + e.getMessage();
            if (e instanceof RuntimeException && e.getMessage().contains("支付处理失败")) {
                // 对于支付相关的异常，可以特殊处理
                throw new SignalProcessingException(errorMessage, 2001, e);
            } else {
                throw new SignalProcessingException(errorMessage, 1001, e);
            }
        }
    }


    /**
     * 找到上下文
     */
    private SignalContext findContext(Object... params) {
        for (Object param : params) {
            if (param instanceof SignalContext) {
                return (SignalContext) param;
            }
        }
        return null;
    }

    /**
     * 获取已注册的事件列表
     */
    public Set<String> getRegisteredEvents() {
        return Collections.unmodifiableSet(sigHandlers.keySet());
    }
}
