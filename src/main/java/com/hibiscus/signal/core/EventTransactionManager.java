package com.hibiscus.signal.core;

import com.hibiscus.signal.config.SignalConfig;
import com.hibiscus.signal.core.entity.DeadLetterEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 事件事务管理器
 * 解决事件驱动架构中的事务隔离和事件重发问题
 */
public class EventTransactionManager {

    private static final Logger log = LoggerFactory.getLogger(EventTransactionManager.class);
    
    private final PlatformTransactionManager transactionManager;
    private final ConcurrentHashMap<String, EventTransactionInfo> eventTransactions = new ConcurrentHashMap<>();
    private final AtomicLong transactionCounter = new AtomicLong(0);
    private DeadLetterQueueManager deadLetterQueueManager;

    public EventTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    /**
     * 在独立事务中执行事件处理
     * 
     * @param eventName 事件名称
     * @param handler 事件处理器
     * @param config 信号配置
     * @param context 信号上下文
     * @param params 参数
     * @return 执行结果
     */
    public Object executeInTransaction(String eventName, SignalHandler handler, 
                                     SignalConfig config, SignalContext context, Object... params) {
        
        String transactionId = generateTransactionId(eventName);
        EventTransactionInfo transactionInfo = new EventTransactionInfo(transactionId, eventName, context);
        eventTransactions.put(transactionId, transactionInfo);
        
        TransactionStatus status = null;
        Object result = null;
        
        try {
            // 创建新事务
            DefaultTransactionDefinition def = new DefaultTransactionDefinition();
            def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            def.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
            def.setTimeout((int) (config.getTimeoutMs() / 1000)); // 转换为秒
            
            status = transactionManager.getTransaction(def);
            transactionInfo.setTransactionStatus(status);
            
            log.info("开始执行事件事务: {} - {}", eventName, transactionId);
            
            // 执行事件处理
            handler.handle(params);
            result = "SUCCESS"; // 返回成功标识
            
            // 提交事务
            transactionManager.commit(status);
            transactionInfo.setStatus(EventTransactionStatus.COMMITTED);
            
            log.info("事件事务执行成功: {} - {}", eventName, transactionId);
            
        } catch (Exception e) {
            // 回滚事务
            if (status != null && !status.isCompleted()) {
                transactionManager.rollback(status);
            }
            transactionInfo.setStatus(EventTransactionStatus.ROLLBACK);
            transactionInfo.setError(e);
            
            log.error("事件事务执行失败: {} - {}, 错误: {}", eventName, transactionId, e.getMessage(), e);
            
            // 根据配置决定是否重试
            if (config.getMaxRetries() > 0) {
                handleRetry(eventName, handler, config, context, params, e);
            }
            
            throw new RuntimeException("事件处理失败: " + eventName, e);
            
        } finally {
            // 清理事务信息
            eventTransactions.remove(transactionId);
        }
        
        return result;
    }

    /**
     * 处理事件重试
     */
    private void handleRetry(String eventName, SignalHandler handler, SignalConfig config, 
                           SignalContext context, Object[] params, Exception originalError) {
        
        int retryCount = 0;
        Exception lastError = originalError;
        
        while (retryCount < config.getMaxRetries()) {
            retryCount++;
            
            try {
                log.info("重试事件处理: {} - 第{}次重试", eventName, retryCount);
                
                // 等待重试延迟
                Thread.sleep(config.getRetryDelayMs());
                
                // 重新执行
                executeInTransaction(eventName, handler, config, context, params);
                
                log.info("事件重试成功: {} - 第{}次重试", eventName, retryCount);
                return;
                
            } catch (Exception e) {
                lastError = e;
                log.warn("事件重试失败: {} - 第{}次重试, 错误: {}", eventName, retryCount, e.getMessage());
            }
        }
        
        // 所有重试都失败了，记录到死信队列
        log.error("事件处理最终失败，进入死信队列: {} - 重试{}次后失败", eventName, config.getMaxRetries());
        handleDeadLetter(eventName, context, params, lastError);
    }

    /**
     * 处理死信事件
     */
    private void handleDeadLetter(String eventName, SignalContext context, Object[] params, Exception error) {
        try {
            // 创建死信事件 - 使用默认重试次数，因为这里没有config参数
            DeadLetterEvent deadLetterEvent = new DeadLetterEvent(
                eventName, 
                getHandlerName(context), 
                context, 
                params, 
                error, 
                3 // 默认重试3次
            );
            
            // 生成唯一ID
            deadLetterEvent.setId(generateTransactionId(eventName));
            
            // 添加到死信队列管理器
            if (deadLetterQueueManager != null) {
                deadLetterQueueManager.addDeadLetterEvent(deadLetterEvent);
                log.info("死信事件已添加到队列: {}", deadLetterEvent.getEventSummary());
            } else {
                log.warn("死信队列管理器未初始化，无法处理死信事件: {}", eventName);
            }
            
        } catch (Exception e) {
            log.error("处理死信事件时发生异常: {} - 原始错误: {}", e.getMessage(), error.getMessage(), e);
        }
    }
    
    /**
     * 获取处理器名称
     */
    private String getHandlerName(SignalContext context) {
        if (context != null) {
            // 从上下文中获取处理器信息，如果没有则使用默认值
            Object handlerInfo = context.getAttribute("handler");
            if (handlerInfo != null) {
                return handlerInfo.getClass().getSimpleName();
            }
        }
        return "UnknownHandler";
    }

    /**
     * 生成事务ID
     */
    private String generateTransactionId(String eventName) {
        return eventName + "_" + System.currentTimeMillis() + "_" + transactionCounter.incrementAndGet();
    }

    /**
     * 获取事务信息
     */
    public EventTransactionInfo getTransactionInfo(String transactionId) {
        return eventTransactions.get(transactionId);
    }

    /**
     * 获取所有活跃事务
     */
    public ConcurrentHashMap<String, EventTransactionInfo> getActiveTransactions() {
        return new ConcurrentHashMap<>(eventTransactions);
    }

    /**
     * 事件事务信息
     */
    public static class EventTransactionInfo {
        private final String transactionId;
        private final String eventName;
        private final SignalContext context;
        private TransactionStatus transactionStatus;
        private EventTransactionStatus status;
        private Exception error;
        private final long startTime;

        public EventTransactionInfo(String transactionId, String eventName, SignalContext context) {
            this.transactionId = transactionId;
            this.eventName = eventName;
            this.context = context;
            this.startTime = System.currentTimeMillis();
            this.status = EventTransactionStatus.RUNNING;
        }

        // Getters and Setters
        public String getTransactionId() { return transactionId; }
        public String getEventName() { return eventName; }
        public SignalContext getContext() { return context; }
        public TransactionStatus getTransactionStatus() { return transactionStatus; }
        public void setTransactionStatus(TransactionStatus transactionStatus) { this.transactionStatus = transactionStatus; }
        public EventTransactionStatus getStatus() { return status; }
        public void setStatus(EventTransactionStatus status) { this.status = status; }
        public Exception getError() { return error; }
        public void setError(Exception error) { this.error = error; }
        public long getStartTime() { return startTime; }
        public long getDuration() { return System.currentTimeMillis() - startTime; }
    }

    /**
     * 事件事务状态
     */
    public enum EventTransactionStatus {
        RUNNING,    // 运行中
        COMMITTED,  // 已提交
        ROLLBACK    // 已回滚
    }
}
