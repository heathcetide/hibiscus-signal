package com.hibiscus.signal.core.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hibiscus.signal.core.SignalContext;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;

/**
 * 死信事件实体
 * 用于存储最终失败的事件信息，支持手动重试和问题排查
 * 
 * @author heathcetide
 */
public class DeadLetterEvent {
    
    private String id;
    private String eventName;
    private String handlerName;
    private SignalContext context;
    private Object[] parameters;
    private String errorMessage;
    private String errorStackTrace;
    private int retryCount;
    private LocalDateTime createTime;
    private LocalDateTime lastRetryTime;
    private DeadLetterStatus status;
    private String failureReason;
    private Map<String, Object> metadata;
    
    @JsonCreator
    public DeadLetterEvent() {
        this.createTime = LocalDateTime.now();
        this.status = DeadLetterStatus.PENDING;
    }
    
    public DeadLetterEvent(String eventName, String handlerName, SignalContext context, 
                          Object[] parameters, Exception error, int retryCount) {
        this();
        this.eventName = eventName;
        this.handlerName = handlerName;
        this.context = context;
        this.parameters = parameters;
        this.errorMessage = error.getMessage();
        this.errorStackTrace = getStackTrace(error);
        this.retryCount = retryCount;
        this.failureReason = determineFailureReason(error);
    }
    
    /**
     * 确定失败原因
     */
    private String determineFailureReason(Exception error) {
        if (error == null) return "未知错误";
        
        String className = error.getClass().getSimpleName();
        String message = error.getMessage();
        
        if (message != null && message.contains("timeout")) {
            return "处理超时";
        } else if (message != null && message.contains("connection")) {
            return "连接失败";
        } else if (message != null && message.contains("validation")) {
            return "数据验证失败";
        } else if (className.contains("NullPointer")) {
            return "空指针异常";
        } else if (className.contains("IllegalArgument")) {
            return "参数错误";
        } else {
            return className + ": " + message;
        }
    }
    
    /**
     * 获取异常堆栈信息
     */
    private String getStackTrace(Exception error) {
        if (error == null) return "";
        
        StringBuilder sb = new StringBuilder();
        sb.append(error.toString()).append("\n");
        
        StackTraceElement[] elements = error.getStackTrace();
        int maxDepth = Math.min(elements.length, 10); // 只保留前10层堆栈
        
        for (int i = 0; i < maxDepth; i++) {
            sb.append("\tat ").append(elements[i]).append("\n");
        }
        
        if (elements.length > maxDepth) {
            sb.append("\t... ").append(elements.length - maxDepth).append(" more\n");
        }
        
        return sb.toString();
    }
    
    /**
     * 是否可以重试
     */
    public boolean canRetry() {
        return status == DeadLetterStatus.PENDING || status == DeadLetterStatus.RETRY_FAILED;
    }
    
    /**
     * 标记为重试中
     */
    public void markAsRetrying() {
        this.status = DeadLetterStatus.RETRYING;
        this.lastRetryTime = LocalDateTime.now();
    }
    
    /**
     * 标记重试成功
     */
    public void markAsRetrySuccess() {
        this.status = DeadLetterStatus.RETRY_SUCCESS;
    }
    
    /**
     * 标记重试失败
     */
    public void markAsRetryFailed() {
        this.status = DeadLetterStatus.RETRY_FAILED;
    }
    
    /**
     * 标记为已处理
     */
    public void markAsProcessed() {
        this.status = DeadLetterStatus.PROCESSED;
    }
    
    /**
     * 获取事件摘要信息
     */
    public String getEventSummary() {
        return String.format("事件[%s] 处理器[%s] 重试次数[%d] 失败原因[%s]", 
                           eventName, handlerName, retryCount, failureReason);
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }
    
    public String getHandlerName() { return handlerName; }
    public void setHandlerName(String handlerName) { this.handlerName = handlerName; }
    
    public SignalContext getContext() { return context; }
    public void setContext(SignalContext context) { this.context = context; }
    
    public Object[] getParameters() { return parameters; }
    public void setParameters(Object[] parameters) { this.parameters = parameters; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public String getErrorStackTrace() { return errorStackTrace; }
    public void setErrorStackTrace(String errorStackTrace) { this.errorStackTrace = errorStackTrace; }
    
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    
    public LocalDateTime getLastRetryTime() { return lastRetryTime; }
    public void setLastRetryTime(LocalDateTime lastRetryTime) { this.lastRetryTime = lastRetryTime; }
    
    public DeadLetterStatus getStatus() { return status; }
    public void setStatus(DeadLetterStatus status) { this.status = status; }
    
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    @Override
    public String toString() {
        return "DeadLetterEvent{" +
                "id='" + id + '\'' +
                ", eventName='" + eventName + '\'' +
                ", handlerName='" + handlerName + '\'' +
                ", retryCount=" + retryCount +
                ", status=" + status +
                ", failureReason='" + failureReason + '\'' +
                ", createTime=" + createTime +
                '}';
    }
    
    /**
     * 死信事件状态
     */
    public enum DeadLetterStatus {
        PENDING("待处理"),
        RETRYING("重试中"),
        RETRY_SUCCESS("重试成功"),
        RETRY_FAILED("重试失败"),
        PROCESSED("已处理"),
        IGNORED("已忽略");
        
        private final String description;
        
        DeadLetterStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}
