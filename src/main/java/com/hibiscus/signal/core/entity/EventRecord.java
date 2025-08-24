package com.hibiscus.signal.core.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 事件记录实体
 * 用于数据库持久化存储事件信息
 */
@Entity
@Table(name = "signal_events", indexes = {
    @Index(name = "idx_event_name", columnList = "event_name"),
    @Index(name = "idx_event_status", columnList = "status"),
    @Index(name = "idx_created_time", columnList = "created_time"),
    @Index(name = "idx_event_id", columnList = "event_id", unique = true)
})
public class EventRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 64)
    private String eventId;

    @Column(name = "event_name", nullable = false, length = 128)
    private String eventName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private EventStatus status;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "max_retries", nullable = false)
    private Integer maxRetries = 3;

    @Column(name = "context_data", columnDefinition = "TEXT")
    private String contextData;

    @Column(name = "params_data", columnDefinition = "TEXT")
    private String paramsData;

    @Column(name = "handler_info", columnDefinition = "TEXT")
    private String handlerInfo;

    @Column(name = "config_info", columnDefinition = "TEXT")
    private String configInfo;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "error_stack", columnDefinition = "TEXT")
    private String errorStack;

    @Column(name = "process_start_time")
    private LocalDateTime processStartTime;

    @Column(name = "process_end_time")
    private LocalDateTime processEndTime;

    @Column(name = "next_retry_time")
    private LocalDateTime nextRetryTime;

    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;

    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime;

    @Version
    @Column(name = "version")
    private Long version = 0L;

    @Column(name = "extended_properties", columnDefinition = "TEXT")
    private String extendedProperties;

    public EventRecord() {
        this.createdTime = LocalDateTime.now();
        this.updatedTime = LocalDateTime.now();
        this.status = EventStatus.PENDING;
    }

    public EventRecord(String eventId, String eventName) {
        this();
        this.eventId = eventId;
        this.eventName = eventName;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    public EventStatus getStatus() { return status; }
    public void setStatus(EventStatus status) { 
        this.status = status; 
        this.updatedTime = LocalDateTime.now();
    }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }

    public Integer getMaxRetries() { return maxRetries; }
    public void setMaxRetries(Integer maxRetries) { this.maxRetries = maxRetries; }

    public String getContextData() { return contextData; }
    public void setContextData(String contextData) { this.contextData = contextData; }

    public String getParamsData() { return paramsData; }
    public void setParamsData(String paramsData) { this.paramsData = paramsData; }

    public String getHandlerInfo() { return handlerInfo; }
    public void setHandlerInfo(String handlerInfo) { this.handlerInfo = handlerInfo; }

    public String getConfigInfo() { return configInfo; }
    public void setConfigInfo(String configInfo) { this.configInfo = configInfo; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getErrorStack() { return errorStack; }
    public void setErrorStack(String errorStack) { this.errorStack = errorStack; }

    public LocalDateTime getProcessStartTime() { return processStartTime; }
    public void setProcessStartTime(LocalDateTime processStartTime) { this.processStartTime = processStartTime; }

    public LocalDateTime getProcessEndTime() { return processEndTime; }
    public void setProcessEndTime(LocalDateTime processEndTime) { this.processEndTime = processEndTime; }

    public LocalDateTime getNextRetryTime() { return nextRetryTime; }
    public void setNextRetryTime(LocalDateTime nextRetryTime) { this.nextRetryTime = nextRetryTime; }

    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }

    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public String getExtendedProperties() { return extendedProperties; }
    public void setExtendedProperties(String extendedProperties) { this.extendedProperties = extendedProperties; }

    /**
     * 事件状态枚举
     */
    public enum EventStatus {
        PENDING("待处理"),
        PROCESSING("处理中"),
        SUCCESS("处理成功"),
        FAILED("处理失败"),
        RETRYING("重试中"),
        DEAD_LETTER("死信"),
        CANCELLED("已取消");

        private final String description;

        EventStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 检查是否可以重试
     */
    public boolean canRetry() {
        return status == EventStatus.FAILED && retryCount < maxRetries;
    }

    /**
     * 检查是否应该重试
     */
    public boolean shouldRetry() {
        return canRetry() && (nextRetryTime == null || LocalDateTime.now().isAfter(nextRetryTime));
    }

    /**
     * 增加重试次数
     */
    public void incrementRetryCount() {
        this.retryCount++;
        this.updatedTime = LocalDateTime.now();
    }

    /**
     * 设置处理开始
     */
    public void setProcessing() {
        this.status = EventStatus.PROCESSING;
        this.processStartTime = LocalDateTime.now();
        this.updatedTime = LocalDateTime.now();
    }

    /**
     * 设置处理成功
     */
    public void setSuccess() {
        this.status = EventStatus.SUCCESS;
        this.processEndTime = LocalDateTime.now();
        this.updatedTime = LocalDateTime.now();
    }

    /**
     * 设置处理失败
     */
    public void setFailed(String errorMessage, String errorStack) {
        this.status = EventStatus.FAILED;
        this.errorMessage = errorMessage;
        this.errorStack = errorStack;
        this.processEndTime = LocalDateTime.now();
        this.updatedTime = LocalDateTime.now();
    }

    /**
     * 设置重试状态
     */
    public void setRetrying() {
        this.status = EventStatus.RETRYING;
        this.updatedTime = LocalDateTime.now();
    }

    /**
     * 设置死信状态
     */
    public void setDeadLetter() {
        this.status = EventStatus.DEAD_LETTER;
        this.updatedTime = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "EventRecord{" +
                "id=" + id +
                ", eventId='" + eventId + '\'' +
                ", eventName='" + eventName + '\'' +
                ", status=" + status +
                ", retryCount=" + retryCount +
                ", createdTime=" + createdTime +
                '}';
    }
}
