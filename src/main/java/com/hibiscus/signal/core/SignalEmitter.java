package com.hibiscus.signal.core;

import com.hibiscus.signal.config.SignalConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * 信号发射器
 * 负责信号的发射逻辑，包括同步和异步发射
 */
public class SignalEmitter {
    
    private static final Logger log = LoggerFactory.getLogger(SignalEmitter.class);
    
    private final ExecutorService executorService;
    private final SignalProcessor signalProcessor;
    
    public SignalEmitter(ExecutorService executorService, SignalProcessor signalProcessor) {
        this.executorService = executorService;
        this.signalProcessor = signalProcessor;
    }
    
    /**
     * 同步发射信号
     */
    public void emitSync(String event, Object sender, List<SigHandler> sigs,
                        SignalConfig config, Consumer<Throwable> errorHandler, 
                        SignalCallback callback, SignalProtectionManager protectionManager,
                        SignalMetrics metrics, Object... params) {
        for (SigHandler sig : sigs) {
            long startTime = System.currentTimeMillis();
            try {
                SignalContext context = findContext(params);
                if (context == null) {
                    context = new SignalContext();
                }
                signalProcessor.executeWithTracingAndProtection(event, sig, sender, config, context, 
                                                             protectionManager, metrics, params);
                if (config.isRecordMetrics()) {
                    // 记录处理时间
                    long processingTime = System.currentTimeMillis() - startTime;
                    log.debug("Signal [{}] processed in {}ms", event, processingTime);
                }
                if (callback != null) {
                    callback.onSuccess(event, sender, params);
                }
            } catch (Exception e) {
                handleError(event, config, errorHandler, e);
                if (callback != null) {
                    callback.onError(event, sender, e, params);
                }
            } finally {
                if (callback != null) {
                    callback.onComplete(event, sender, params);
                }
            }
        }
    }
    
    /**
     * 异步发射信号
     */
    public void emitAsync(String event, Object sender, List<SigHandler> sigs,
                         SignalConfig config, Consumer<Throwable> errorHandler, 
                         SignalCallback callback, SignalProtectionManager protectionManager,
                         SignalMetrics metrics, Object... params) {
        for (SigHandler sig : sigs) {
            CompletableFuture.runAsync(() -> {
                long startTime = System.currentTimeMillis();
                try {
                    SignalContext context = findContext(params);
                    if (context == null) {
                        context = new SignalContext();
                    }
                    signalProcessor.executeWithTracingAndProtection(event, sig, sender, config, context, 
                                                                 protectionManager, metrics, params);
                    if (config.isRecordMetrics()) {
                        long processingTime = System.currentTimeMillis() - startTime;
                        log.debug("Signal [{}] processed asynchronously in {}ms", event, processingTime);
                    }
                    if (callback != null) {
                        callback.onSuccess(event, sender, params);
                    }
                } catch (Exception e) {
                    handleError(event, config, errorHandler, e);
                    if (callback != null) {
                        callback.onError(event, sender, e, params);
                    }
                } finally {
                    if (callback != null) {
                        callback.onComplete(event, sender, params);
                    }
                }
            }, executorService);
        }
    }
    
    /**
     * 处理错误
     */
    private void handleError(String event, SignalConfig config,
                           Consumer<Throwable> errorHandler, Exception e) {
        if (config.isRecordMetrics()) {
            log.error("Signal [{}] processing error recorded", event);
        }
        log.error("Signal [{}] handler error: {}", event, e.getMessage(), e);
        if (errorHandler != null) {
            errorHandler.accept(e);
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
}
