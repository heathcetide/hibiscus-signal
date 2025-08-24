package com.hibiscus.signal.core;

import com.hibiscus.signal.config.SignalConfig;
import com.hibiscus.signal.exceptions.SignalProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * 信号处理器
 * 负责信号的具体处理逻辑，包括重试、超时、追踪等功能
 */
public class SignalProcessor {
    
    private static final Logger log = LoggerFactory.getLogger(SignalProcessor.class);
    
    private final ExecutorService executorService;
    
    public SignalProcessor(ExecutorService executorService) {
        this.executorService = executorService;
    }
    
    /**
     * 执行信号处理，包含重试逻辑
     */
    public void executeWithRetry(String event, SigHandler sig, Object sender,
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
     * 执行带追踪的信号处理
     */
    public void executeWithTracing(String event, SigHandler sig, Object sender, 
                                  SignalConfig config, SignalContext context, Object... params) throws Exception {
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
        }
    }
    
    /**
     * 执行超时处理
     */
    private void executeWithTimeout(SigHandler sig, Object sender, long timeoutMs, Object... params)
            throws Exception {
        if (executorService.isShutdown() || executorService.isTerminated()) {
            log.warn("线程池已关闭，直接同步执行处理器: {}", sig.getHandlerName());
            executeHandler(sig, sender, params);
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
            log.warn("信号处理器执行超时: {} ({}ms)", sig.getHandlerName(), timeoutMs);
            task.cancel(true);
            throw new SignalProcessingException("Signal handler execution timed out", 1001);
        } catch (InterruptedException e) {
            log.warn("信号处理器执行被中断: {}", sig.getHandlerName());
            task.cancel(true);
            Thread.currentThread().interrupt();
            throw new SignalProcessingException("Signal handler execution interrupted", 1002);
        }
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
            String errorMessage = "Signal handler execution failed: " + e.getMessage();
            if (e instanceof RuntimeException && e.getMessage().contains("支付处理失败")) {
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
}

