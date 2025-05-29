package com.hibiscus.signal.core;

import com.hibiscus.signal.config.SignalConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class SignalMetrics {

    /**
     * 统计信号量相关的指标
     */
    private final Map<String, AtomicLong> emitCount = new ConcurrentHashMap<>();

    /**
     * 统计信号量处理相关的指标
     */
    private final Map<String, AtomicLong> handlerCount = new ConcurrentHashMap<>();

    /**
     * 统计信号量处理时间相关的指标
     */
    private final Map<String, AtomicLong> processingTime = new ConcurrentHashMap<>();

    /**
     * 统计信号量处理错误相关的指标
     */
    private final Map<String, AtomicLong> errorCount = new ConcurrentHashMap<>();

    /**
     * 统计信号量最后发射时间相关的指标
     */
    private final Map<String, Long> lastEmitTime = new ConcurrentHashMap<>();

    private final Map<String, SignalContext> eventTraces = new ConcurrentHashMap<>();

    // 新增处理计数
    private final AtomicLong processedCount = new AtomicLong();
    private final Map<String, AtomicLong> processedCountByEvent = new ConcurrentHashMap<>();
    private final Map<String, Long> lastProcessTime = new ConcurrentHashMap<>();

    public void recordEmit(String signalName) {
        emitCount.computeIfAbsent(signalName, k -> new AtomicLong()).incrementAndGet();
        lastEmitTime.put(signalName, System.currentTimeMillis());
    }

    public void recordHandlerAdded(String signalName) {
        handlerCount.computeIfAbsent(signalName, k -> new AtomicLong()).incrementAndGet();
    }

    public void recordHandlerRemoved(String signalName) {
        handlerCount.computeIfAbsent(signalName, k -> new AtomicLong()).decrementAndGet();
    }

    public void recordProcessingTime(String signalName, long timeInMillis) {
        processingTime.computeIfAbsent(signalName, k -> new AtomicLong()).addAndGet(timeInMillis);
    }

    public void recordError(String signalName) {
        errorCount.computeIfAbsent(signalName, k -> new AtomicLong()).incrementAndGet();
    }

    public Map<String, Object> getMetrics(String signalName) {
        Map<String, Object> metrics = new ConcurrentHashMap<>();
        metrics.put("emitCount", emitCount.getOrDefault(signalName, new AtomicLong()).get());
        metrics.put("handlerCount", handlerCount.getOrDefault(signalName, new AtomicLong()).get());
        metrics.put("totalProcessingTime", processingTime.getOrDefault(signalName, new AtomicLong()).get());
        metrics.put("errorCount", errorCount.getOrDefault(signalName, new AtomicLong()).get());
        metrics.put("lastEmitTime", lastEmitTime.getOrDefault(signalName, 0L));
        return metrics;
    }

    public Map<String, Map<String, Object>> getAllMetrics() {
        Map<String, Map<String, Object>> allMetrics = new ConcurrentHashMap<>();
        emitCount.keySet().forEach(signalName ->
                allMetrics.put(signalName, getMetrics(signalName))
        );
        return allMetrics;
    }


    public void recordTrace(SignalContext context) {
        eventTraces.put(context.getTraceId(), context);
    }

    public SignalContext getTrace(String traceId) {
        return eventTraces.get(traceId);
    }

    public void recordProcessed() {
        processedCount.incrementAndGet();
    }

    public void recordProcessed(String event) {
        processedCountByEvent.computeIfAbsent(event, k -> new AtomicLong()).incrementAndGet();
        lastProcessTime.put(event, System.currentTimeMillis());
    }

    public long getProcessedCount() {
        return processedCount.get();
    }

    public long getProcessedCount(String event) {
        return processedCountByEvent.getOrDefault(event, new AtomicLong()).get();
    }

    public Long getLastProcessTime(String event) {
        return lastProcessTime.get(event);
    }
}