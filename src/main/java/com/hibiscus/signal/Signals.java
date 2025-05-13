package com.hibiscus.signal;

import com.hibiscus.signal.config.SignalConfig;
import com.hibiscus.signal.core.*;
import com.hibiscus.signal.exceptions.SignalProcessingException;
import com.hibiscus.signal.utils.SnowflakeIdGenerator;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static com.hibiscus.signal.core.EventType.ADD_HANDLER;

@Service
public class Signals implements SignalManager{

    /**
     * 单例
     */
    private static final Signals SIG = new Signals();

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
    private final PriorityQueue<SigHandler> eventQueue = new PriorityQueue<>(Comparator.comparingInt(event -> event.getPriority().getValue()));

    /**
     * 信号转换器集合
     */
    private final Map<String, List<SignalTransformer>> signalTransformers = new ConcurrentHashMap<>();

    /**
     * 线程池
     */
    private final ExecutorService executorService;

    private Signals() {
        this.executorService = Executors.newFixedThreadPool(4);
    }

    /**
     * 获取单例
     */
    public static Signals sig() {
        return SIG;
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
    public long connect(String event, SignalHandler handler, SignalConfig signalConfig){
        signalConfigs.put(event, signalConfig);
        long id = SnowflakeIdGenerator.nextId();
        SigHandler signalHandler = new SigHandler(id,ADD_HANDLER, event, handler, signalConfig.getPriority());
        eventQueue.offer(signalHandler);
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
        signalConfigs.put(event, signalConfig);
        long id = SnowflakeIdGenerator.nextId();
        SigHandler signalHandler = new SigHandler(id,ADD_HANDLER, event, handler, signalConfig.getPriority());
        signalHandler.setSignalContext(context);
        eventQueue.offer(signalHandler);
        processEvents();
        return id;
    }

    public void disconnect(String event, long id) {
        SignalConfig config = signalConfigs.getOrDefault(event, new SignalConfig.Builder().build());
        SigHandler ev = new SigHandler(id, EventType.REMOVE_HANDLER, event, null, config.getPriority());
        eventQueue.offer(ev);
        processEvents();
    }

    public void disconnect(String event, long id, SignalContext context) {
        SignalConfig config = signalConfigs.getOrDefault(event, new SignalConfig.Builder().build());
        SigHandler ev = new SigHandler(id, EventType.REMOVE_HANDLER, event, null, config.getPriority());
        ev.setSignalContext(context);
        eventQueue.offer(ev);
        processEvents();
    }

    /**
     * 处理事件
     */
    public void processEvents(){
        if (inLoop) return;

        synchronized (this){
            if (eventQueue.isEmpty()) return;
            inLoop = true;
        }

        try{
            SigHandler sigHandler;
            while((sigHandler = eventQueue.poll()) != null){
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
                    case PAUSE_SIGNAL:
                        // 处理暂停信号的逻辑
                        break;
                    case RESUME_SIGNAL:
                        // 处理恢复信号的逻辑
                        break;
                    case BROADCAST:
                        // 处理广播信号的逻辑
                        break;
                    case REFRESH_CONFIG:
                        // 处理刷新配置的逻辑
                        break;
                    default:
                        // 默认行为
                        break;
                }
            }
        }finally {
            inLoop = false;
        }
    }

    public void emit(String event, Object sender, Consumer<Throwable> errorHandler, Object... params) {
        // 获取事件的拦截器并按优先级排序
        List<SignalInterceptor> interceptors = signalInterceptors.get(event);

        // 遍历拦截器执行 beforeHandle
        for (SignalInterceptor interceptor : interceptors) {
            if (!interceptor.beforeHandle(event, sender, params)) {
                return; // 如果某个拦截器阻止了信号传播，退出
            }
        }

        // 先获取该事件的所有过滤器
        List<SignalFilter> filters = getSortedFilters(event);

        // 遍历过滤器，如果有任何一个过滤器返回 false，则停止传播
        for (SignalFilter filter : filters) {
            if (!filter.filter(event, sender, params)) {
                return;
            }
        }

        // 获取该事件的所有信号转换器并应用
        List<SignalTransformer> transformers = signalTransformers.get(event);
        if (transformers != null) {
            for (SignalTransformer transformer : transformers) {
                params = transformer.transform(event, sender, params);
            }
        }

        SignalConfig config = signalConfigs.getOrDefault(event, new SignalConfig.Builder().build());
        if (config.isRecordMetrics()) {
            metrics.recordEmit(event);
        }

        List<SigHandler> sigs = sigHandlers.get(event);
        if (sigs == null) return;

        if (config.isAsync()) {
            emitAsync(event, sender, sigs, config, errorHandler, null, params);
        } else {
            emitSync(event, sender, sigs, config, errorHandler, null, params);
        }
    }

    public void emit(String event, Object sender, SignalCallback callback, Consumer<Throwable> errorHandler, Object... params) {

        // 获取事件的拦截器并按优先级排序
        List<SignalInterceptor> interceptors = signalInterceptors.get(event);

        if (interceptors != null){
            // 遍历拦截器执行 beforeHandle
            for (SignalInterceptor interceptor : interceptors) {
                if (!interceptor.beforeHandle(event, sender, params)) {
                    return; // 如果某个拦截器阻止了信号传播，退出
                }
            }
        } else {
            interceptors = new ArrayList<>();
        }

        // 先获取该事件的所有过滤器
        List<SignalFilter> filters = getSortedFilters(event);

        // 遍历过滤器，如果有任何一个过滤器返回 false，则停止传播
        for (SignalFilter filter : filters) {
            if (!filter.filter(event, sender, params)) {
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
                params = transformer.transform(event, sender, params);
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

    private void emitSync(String event, Object sender, List<SigHandler> sigs,
                          SignalConfig config, Consumer<Throwable> errorHandler,SignalCallback callback,  Object... params) {
        for (SigHandler sig : sigs) {
            long startTime = System.currentTimeMillis();
            try {
                executeWithRetry(event, sig, sender, config, params);
                if (config.isRecordMetrics()) {
                    metrics.recordProcessingTime(event, System.currentTimeMillis() - startTime);
                }
                if (callback != null) {
                    callback.onSuccess(event, sender, params); // 调用成功回调
                }
            } catch (Exception e) {
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

    private void emitAsync(String event, Object sender, List<SigHandler> sigs,
                           SignalConfig config, Consumer<Throwable> errorHandler, SignalCallback callback,  Object... params) {
        for (SigHandler sig : sigs) {
            CompletableFuture.runAsync(() -> {
                long startTime = System.currentTimeMillis();
                try {
                    executeWithRetry(event, sig, sender, config, params);
                    if (config.isRecordMetrics()) {
                        metrics.recordProcessingTime(event, System.currentTimeMillis() - startTime);
                    }
                    if (callback != null) {
                        callback.onSuccess(event, sender, params); // 调用成功回调
                    }
                } catch (Exception e) {
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

    private void executeWithRetry(String event, SigHandler sig, Object sender,
                                  SignalConfig config, Object... params) throws Exception {
        int retries = 0;
        Exception lastException = null;

        while (retries <= config.getMaxRetries()) {
            try {
                if (config.getTimeoutMs() > 0) {
                    executeWithTimeout(sig, sender, config.getTimeoutMs(), params);
                } else {
                    sig.getHandler().handle(sender, params);
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

    private void executeWithTimeout(SigHandler sig, Object sender, long timeoutMs, Object... params)
            throws Exception {
        Future<?> future = executorService.submit(() -> {
            sig.getHandler().handle(sender, params);
            return null;
        });

        try {
            future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new SignalProcessingException("Signal handler execution timed out: "+ e.getMessage(), 1001);
        } catch (ExecutionException e) {
            throw new SignalProcessingException("Signal handler execution failed: "+ e.getCause(), 1001);
        }
    }

    private void handleError(String event, SignalConfig config,
                             Consumer<Throwable> errorHandler, Exception e) {
        if (config.isRecordMetrics()) {
            metrics.recordError(event);
        }
        if (errorHandler != null) {
            errorHandler.accept(e);
        }
    }

    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void clear(String... events) {
        for (String event : events) {
            sigHandlers.remove(event);
            signalConfigs.remove(event);
        }
    }

    public SignalMetrics getMetrics() {
        return metrics;
    }

    public void addFilter(String event, SignalFilter filter) {
        signalFilters.computeIfAbsent(event, k -> new ArrayList<>()).add(filter);
    }


    private List<SignalFilter> getSortedFilters(String event) {
        List<SignalFilter> filters = signalFilters.getOrDefault(event, Collections.emptyList());
        filters.sort(Comparator.comparingInt(SignalFilter::getPriority));
        return filters;
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
        // 按照优先级排序拦截器
        signalInterceptors.get(event).sort(Comparator.comparingInt(SignalInterceptor::getOrder));
    }
}
