package com.hibiscus.signal;

import com.hibiscus.signal.config.DatabaseSignalPersistence;
import com.hibiscus.signal.config.SignalConfig;
import com.hibiscus.signal.core.*;
import com.hibiscus.signal.core.service.EventStateManager;
import com.hibiscus.signal.spring.config.SignalProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.DisposableBean;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 重构后的信号管理器
 * 采用职责分离的设计模式，将不同功能拆分到专门的组件中
 */
@Service
public class Signals implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(Signals.class);

    // 核心组件
    private final SignalRegistry signalRegistry;
    private final SignalPipeline signalPipeline;
    private final SignalProcessor signalProcessor;
    private final SignalEmitter signalEmitter;
    private final SignalProtectionManager protectionManager;
    private final SignalMetrics metrics;

    // 依赖注入
    private final ExecutorService executorService;
    
    @Autowired
    private SignalProperties signalProperties;

    @Autowired(required = false)
    private DatabaseSignalPersistence databasePersistence;

    @Autowired(required = false)
    private EventStateManager eventStateManager;

    public Signals(@Qualifier("signalExecutor") ExecutorService executorService) {
        this.executorService = executorService;
        
        // 初始化核心组件
        this.signalRegistry = new SignalRegistry();
        this.signalProcessor = new SignalProcessor(executorService);
        this.signalPipeline = new SignalPipeline();
        this.signalEmitter = new SignalEmitter(executorService, signalProcessor);
        this.protectionManager = new SignalProtectionManager();
        this.metrics = new SignalMetrics();
    }

    // ==================== 信号注册相关方法 ====================
    
    /**
     * 绑定事件处理器
     */
    public long connect(String event, SignalHandler handler) {
        return connect(event, handler, new SignalConfig.Builder().build());
    }

    /**
     * 绑定事件处理器（带配置）
     */
    public long connect(String event, SignalHandler handler, SignalConfig signalConfig) {
        // 自动配置保护机制
        autoConfigureProtection(event);
        return signalRegistry.registerHandler(event, handler, signalConfig);
    }

    /**
     * 绑定事件处理器（带处理器名称）
     */
    public void connect(String event, SignalHandler handler, SignalConfig signalConfig, String handlerName) {
        signalRegistry.registerHandler(event, handler, signalConfig, handlerName);
    }

    /**
     * 绑定事件处理器（带上下文）
     */
    public long connect(String event, SignalHandler handler, SignalContext context) {
        return connect(event, handler, new SignalConfig.Builder().build(), context);
    }

    /**
     * 绑定事件处理器（带配置和上下文）
     */
    public long connect(String event, SignalHandler handler, SignalConfig signalConfig, SignalContext context) {
        return signalRegistry.registerHandler(event, handler, signalConfig, context);
    }

    /**
     * 解绑事件处理器
     */
    public void disconnect(String event, long id) {
        signalRegistry.unregisterHandler(event, id);
    }

    /**
     * 解绑事件处理器（带上下文）
     */
    public void disconnect(String event, long id, SignalContext context) {
        signalRegistry.unregisterHandler(event, id, context);
    }

    /**
     * 处理事件队列
     */
    public void processEvents() {
        signalRegistry.processEvents();
    }

    // ==================== 信号发射相关方法 ====================
    
    /**
     * 发射信号
     */
    public void emit(String event, Object sender, Consumer<Throwable> errorHandler, Object... params) {
        // 1. 检查保护机制
        if (protectionManager.isBlocked(event)) {
            log.debug("Signal [{}] blocked by protection manager", event);
            return;
        }

        // 2. 准备上下文
        SignalContext context = prepareContext(params);
        if (context == null) {
            log.warn("Failed to prepare context for signal [{}]", event);
            return;
        }

        // 3. 执行管道处理
        Object[] processedParams = signalPipeline.processPipeline(event, sender, context, params);
        if (processedParams == null) {
            log.debug("Signal [{}] blocked by pipeline", event);
            return;
        }

        // 4. 记录指标
        SignalConfig config = signalRegistry.getConfig(event);
        if (config.isRecordMetrics()) {
            metrics.recordEmit(event);
        }

        // 5. 获取处理器并发射
        List<SigHandler> sigs = signalRegistry.getHandlers(event);
        if (!signalRegistry.hasHandlers(event)) {
            log.debug("No handlers found for signal [{}]", event);
            return;
        }

        // 6. 根据配置选择同步或异步发射
        if (config.isAsync()) {
            signalEmitter.emitAsync(event, sender, sigs, config, errorHandler, null, protectionManager, metrics, processedParams);
        } else {
            signalEmitter.emitSync(event, sender, sigs, config, errorHandler, null, protectionManager, metrics, processedParams);
        }
    }

    /**
     * 发射信号（带回调）
     */
    public void emit(String event, Object sender, SignalCallback callback, Consumer<Throwable> errorHandler, Object... params) {
        // 1. 检查保护机制
        if (protectionManager.isBlocked(event)) {
            if (callback != null) {
                callback.onError(event, sender, new RuntimeException("Signal blocked (circuit open or rate limited)"), params);
                callback.onComplete(event, sender, params);
            }
            return;
        }

        // 2. 准备上下文
        SignalContext context = prepareContext(params);
        if (context == null) {
            if (callback != null) {
                callback.onError(event, sender, new RuntimeException("Failed to prepare context"), params);
                callback.onComplete(event, sender, params);
            }
            return;
        }

        // 3. 执行管道处理
        Object[] processedParams = signalPipeline.processPipeline(event, sender, context, params);
        if (processedParams == null) {
            if (callback != null) {
                callback.onError(event, sender, new RuntimeException("Signal blocked by pipeline"), params);
                callback.onComplete(event, sender, params);
            }
            return;
        }

        // 4. 记录指标
        SignalConfig config = signalRegistry.getConfig(event);
        if (config.isRecordMetrics()) {
            metrics.recordEmit(event);
        }

        // 5. 获取处理器并发射
        List<SigHandler> sigs = signalRegistry.getHandlers(event);
        if (!signalRegistry.hasHandlers(event)) {
            if (callback != null) {
                callback.onError(event, sender, new RuntimeException("No handlers for event: " + event), params);
                callback.onComplete(event, sender, params);
            }
            return;
        }

        // 6. 根据配置选择同步或异步发射
        if (config.isAsync()) {
            signalEmitter.emitAsync(event, sender, sigs, config, errorHandler, callback, protectionManager, metrics, processedParams);
        } else {
            signalEmitter.emitSync(event, sender, sigs, config, errorHandler, callback, protectionManager, metrics, processedParams);
        }
    }

    // ==================== 辅助方法 ====================
    
    /**
     * 准备上下文
     */
    private SignalContext prepareContext(Object... params) {
        SignalContext context = findContext(params);
        if (context == null) {
            context = new SignalContext();
            Object[] newParams = new Object[params.length + 1];
            newParams[0] = context;
            System.arraycopy(params, 0, newParams, 1, params.length);
            return context;
        }
        return context;
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

    // ==================== 其他方法 ====================
    
    /**
     * Spring容器销毁时自动调用，确保线程池优雅关闭
     */
    @Override
    public void destroy() throws Exception {
        log.info("正在关闭Signal框架，清理资源...");
        
        // 1. 清理所有事件处理器
        signalRegistry.clearAll();
        
        // 2. 关闭线程池
        shutdown();
        
        log.info("Signal框架资源清理完成");
    }
    
    /**
     * 关闭执行器服务
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            log.info("正在关闭信号处理线程池...");
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    log.warn("线程池未在60秒内关闭，强制关闭...");
                    executorService.shutdownNow();
                    if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                        log.error("线程池强制关闭失败");
                    }
                } else {
                    log.info("线程池已优雅关闭");
                }
            } catch (InterruptedException ie) {
                log.warn("线程池关闭被中断，强制关闭...");
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 清除指定事件
     */
    public void clear(String... events) {
        signalRegistry.clear(events);
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
        signalPipeline.addFilter(event, filter);
    }

    /**
     * 绑定信号转换器
     */
    public void addSignalTransformer(String event, SignalTransformer transformer) {
        signalPipeline.addTransformer(event, transformer);
    }

    /**
     * 绑定信号拦截器
     */
    public void addSignalInterceptor(String event, SignalInterceptor interceptor) {
        signalPipeline.addInterceptor(event, interceptor);
    }

    /**
     * 配置信号保护
     */
    public void configureProtection(String event, CircuitBreaker breaker, RateLimiter limiter) {
        protectionManager.registerCircuitBreaker(event, breaker);
        protectionManager.registerRateLimiter(event, limiter);
    }
    
    /**
     * 根据配置自动配置保护机制
     */
    public void autoConfigureProtection(String event) {
        if (signalProperties != null && signalProperties.getProtectionEnabled()) {
            // 自动创建熔断器
            CircuitBreaker breaker = new CircuitBreaker(
                signalProperties.getCircuitBreakerFailureThreshold(),
                signalProperties.getCircuitBreakerOpenTimeoutMs(),
                signalProperties.getCircuitBreakerHalfOpenTrialCount()
            );
            
            // 自动创建限流器
            RateLimiter limiter = new RateLimiter(
                signalProperties.getRateLimiterMaxRequestsPerSecond()
            );
            
            // 注册保护机制
            protectionManager.registerCircuitBreaker(event, breaker);
            protectionManager.registerRateLimiter(event, limiter);
            
            log.info("自动配置保护机制完成: {} - 熔断器阈值:{}, 限流器QPS:{}", 
                    event, 
                    signalProperties.getCircuitBreakerFailureThreshold(),
                    signalProperties.getRateLimiterMaxRequestsPerSecond());
        }
    }

    /**
     * 获取已注册的事件列表
     */
    public Set<String> getRegisteredEvents() {
        return signalRegistry.getRegisteredEvents();
    }
}
