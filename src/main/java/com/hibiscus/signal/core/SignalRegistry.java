package com.hibiscus.signal.core;

import com.hibiscus.signal.config.SignalConfig;
import com.hibiscus.signal.config.SignalPriority;
import com.hibiscus.signal.utils.SnowflakeIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import static com.hibiscus.signal.core.EventType.ADD_HANDLER;

/**
 * 信号注册管理器
 * 负责信号的注册、解绑和队列管理
 */
public class SignalRegistry {
    
    private static final Logger log = LoggerFactory.getLogger(SignalRegistry.class);
    
    /**
     * 监听器集合
     */
    private final Map<String, List<SigHandler>> sigHandlers = new ConcurrentHashMap<>();
    
    /**
     * 信号配置
     */
    private final Map<String, SignalConfig> signalConfigs = new ConcurrentHashMap<>();
    
    /**
     * 事件队列
     */
    private final EnumMap<SignalPriority, BlockingQueue<SigHandler>> priorityQueues = new EnumMap<>(SignalPriority.class);
    
    /**
     * 是否在循环中
     */
    private volatile boolean inLoop = false;
    
    public SignalRegistry() {
        for (SignalPriority p : SignalPriority.values()) {
            priorityQueues.put(p, new LinkedBlockingQueue<>());
        }
    }
    
    /**
     * 注册信号处理器
     */
    public long registerHandler(String event, SignalHandler handler, SignalConfig signalConfig) {
        signalConfigs.computeIfAbsent(event, k -> signalConfig);
        long id = SnowflakeIdGenerator.nextId();
        SigHandler signalHandler = new SigHandler(id, ADD_HANDLER, event, handler, signalConfig.getPriority());
        
        priorityQueues.get(signalConfig.getPriority()).offer(signalHandler);
        processEvents();
        return id;
    }
    
    /**
     * 注册信号处理器（带处理器名称）
     */
    public void registerHandler(String event, SignalHandler handler, SignalConfig signalConfig, String handlerName) {
        signalConfigs.computeIfAbsent(event, k -> signalConfig);
        long id = SnowflakeIdGenerator.nextId();
        SigHandler signalHandler = new SigHandler(id, ADD_HANDLER, event, handler, signalConfig.getPriority());
        signalHandler.setHandlerName(handlerName != null ? handlerName : handler.getClass().getName());
        
        priorityQueues.get(signalConfig.getPriority()).offer(signalHandler);
        processEvents();
    }
    
    /**
     * 注册信号处理器（带上下文）
     */
    public long registerHandler(String event, SignalHandler handler, SignalConfig signalConfig, SignalContext context) {
        signalConfigs.computeIfAbsent(event, k -> signalConfig);
        long id = SnowflakeIdGenerator.nextId();
        SigHandler signalHandler = new SigHandler(id, ADD_HANDLER, event, handler, signalConfig.getPriority());
        signalHandler.setSignalContext(context);
        
        priorityQueues.get(signalConfig.getPriority()).offer(signalHandler);
        processEvents();
        return id;
    }
    
    /**
     * 解绑信号处理器
     */
    public void unregisterHandler(String event, long id) {
        SignalConfig config = signalConfigs.getOrDefault(event, new SignalConfig.Builder().build());
        SigHandler ev = new SigHandler(id, EventType.REMOVE_HANDLER, event, null, config.getPriority());
        priorityQueues.get(config.getPriority()).offer(ev);
        processEvents();
    }
    
    /**
     * 解绑信号处理器（带上下文）
     */
    public void unregisterHandler(String event, long id, SignalContext context) {
        SignalConfig config = signalConfigs.getOrDefault(event, new SignalConfig.Builder().build());
        SigHandler ev = new SigHandler(id, EventType.REMOVE_HANDLER, event, null, config.getPriority());
        ev.setSignalContext(context);
        priorityQueues.get(config.getPriority()).offer(ev);
        processEvents();
    }
    
    /**
     * 处理事件队列
     */
    public void processEvents() {
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
     * 处理优先级队列
     */
    private void processPriorityQueue(SignalPriority priority) {
        BlockingQueue<SigHandler> queue = priorityQueues.get(priority);
        SigHandler sigHandler;
        while ((sigHandler = queue.poll()) != null) {
            List<SigHandler> sigs = sigHandlers.computeIfAbsent(sigHandler.getSignalName(), k -> new CopyOnWriteArrayList<>());
            SignalConfig config = signalConfigs.computeIfAbsent(sigHandler.getSignalName(), k -> new SignalConfig.Builder().build());
            
            switch (sigHandler.getEvType()) {
                case ADD_HANDLER:
                    if (sigs.size() < config.getMaxHandlers()) {
                        sigs.add(sigHandler);
                        log.debug("Handler registered for event: {}", sigHandler.getSignalName());
                    }
                    break;
                case REMOVE_HANDLER:
                    SigHandler finalEvent = sigHandler;
                    sigs.removeIf(sh -> sh.getId() == finalEvent.getId());
                    log.debug("Handler unregistered for event: {}", sigHandler.getSignalName());
                    break;
                default:
                    log.warn("Unknown event type: {}", sigHandler.getEvType());
            }
        }
    }
    
    /**
     * 判断所有队列是否为空
     */
    private boolean allQueuesEmpty() {
        return priorityQueues.values().stream().allMatch(Queue::isEmpty);
    }
    
    /**
     * 获取事件处理器列表
     */
    public List<SigHandler> getHandlers(String event) {
        return sigHandlers.getOrDefault(event, Collections.emptyList());
    }
    
    /**
     * 获取信号配置
     */
    public SignalConfig getConfig(String event) {
        return signalConfigs.getOrDefault(event, new SignalConfig.Builder().build());
    }
    
    /**
     * 获取已注册的事件列表
     */
    public Set<String> getRegisteredEvents() {
        return Collections.unmodifiableSet(sigHandlers.keySet());
    }
    
    /**
     * 清除指定事件
     */
    public void clear(String... events) {
        for (String event : events) {
            sigHandlers.remove(event);
            signalConfigs.remove(event);
            log.debug("已清除事件: {}", event);
        }
    }
    
    /**
     * 清除所有事件（用于应用关闭时清理资源）
     */
    public void clearAll() {
        int handlerCount = sigHandlers.size();
        int configCount = signalConfigs.size();
        
        sigHandlers.clear();
        signalConfigs.clear();
        
        // 清空所有优先级队列
        for (SignalPriority priority : SignalPriority.values()) {
            priorityQueues.get(priority).clear();
        }
        
        log.info("已清除所有事件处理器: {} 个处理器, {} 个配置", handlerCount, configCount);
    }
    
    /**
     * 检查事件是否有处理器
     */
    public boolean hasHandlers(String event) {
        List<SigHandler> handlers = sigHandlers.get(event);
        return handlers != null && !handlers.isEmpty();
    }
}
