package com.hibiscus.signal.core.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 领域事件基类
 * 所有领域事件都应该继承此类
 * 
 * @author heathcetide
 */
public abstract class DomainEvent {
    
    /**
     * 事件ID
     */
    private final String eventId;
    
    /**
     * 聚合根ID
     */
    private final String aggregateId;
    
    /**
     * 事件版本
     */
    private final long version;
    
    /**
     * 事件发生时间
     */
    private final LocalDateTime occurredOn;
    
    /**
     * 关联ID（用于关联多个事件）
     */
    private String correlationId;
    
    /**
     * 因果ID（用于追踪事件因果关系）
     */
    private String causationId;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 事件来源
     */
    private String source;
    
    /**
     * 事件元数据
     */
    private final Map<String, Object> metadata;
    
    @JsonCreator
    protected DomainEvent(@JsonProperty("aggregateId") String aggregateId, 
                         @JsonProperty("version") long version) {
        this.eventId = UUID.randomUUID().toString();
        this.aggregateId = aggregateId;
        this.version = version;
        this.occurredOn = LocalDateTime.now();
        this.metadata = new HashMap<>();
    }
    
    /**
     * 获取事件类型
     */
    public abstract String getEventType();
    
    /**
     * 获取事件摘要
     */
    public String getEventSummary() {
        return String.format("事件[%s] 类型[%s] 聚合根[%s] 版本[%d] 时间[%s]", 
                           eventId, getEventType(), aggregateId, version, occurredOn);
    }
    
    /**
     * 添加元数据
     */
    public void addMetadata(String key, Object value) {
        metadata.put(key, value);
    }
    
    /**
     * 获取元数据
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }
    
    /**
     * 获取所有元数据
     */
    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }
    
    /**
     * 设置关联ID
     */
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
    
    /**
     * 设置因果ID
     */
    public void setCausationId(String causationId) {
        this.causationId = causationId;
    }
    
    /**
     * 设置用户ID
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    /**
     * 设置事件来源
     */
    public void setSource(String source) {
        this.source = source;
    }
    
    // Getters
    public String getEventId() { return eventId; }
    public String getAggregateId() { return aggregateId; }
    public long getVersion() { return version; }
    public LocalDateTime getOccurredOn() { return occurredOn; }
    public String getCorrelationId() { return correlationId; }
    public String getCausationId() { return causationId; }
    public String getUserId() { return userId; }
    public String getSource() { return source; }
    
    @Override
    public String toString() {
        return getEventSummary();
    }
}
