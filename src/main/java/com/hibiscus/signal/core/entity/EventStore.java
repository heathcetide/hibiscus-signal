package com.hibiscus.signal.core.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 事件存储实体
 * 用于存储所有领域事件，支持事件溯源
 * 
 * @author heathcetide
 */
@Entity
@Table(name = "event_store", indexes = {
    @Index(name = "idx_aggregate_id", columnList = "aggregate_id"),
    @Index(name = "idx_event_type", columnList = "event_type"),
    @Index(name = "idx_timestamp", columnList = "timestamp"),
    @Index(name = "idx_correlation_id", columnList = "correlation_id")
})
@EntityListeners(AuditingEntityListener.class)
public class EventStore {
    
    @Id
    @Column(length = 64)
    private String eventId;
    
    @Column(name = "event_type", length = 128, nullable = false)
    private String eventType;
    
    @Column(name = "aggregate_id", length = 64, nullable = false)
    private String aggregateId;
    
    @Column(name = "version", nullable = false)
    private long version;
    
    @Column(name = "event_data", columnDefinition = "TEXT", nullable = false)
    private String eventData;
    
    @Column(name = "timestamp", nullable = false)
    @CreatedDate
    private LocalDateTime timestamp;
    
    @Column(name = "correlation_id", length = 64)
    private String correlationId;
    
    @Column(name = "causation_id", length = 64)
    private String causationId;
    
    @Column(name = "user_id", length = 64)
    private String userId;
    
    @Column(name = "source", length = 128)
    private String source;
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadataJson;
    
    @Transient
    private Map<String, Object> metadata;
    
    @JsonCreator
    public EventStore() {
        this.timestamp = LocalDateTime.now();
        this.metadata = new HashMap<>();
    }
    
    public EventStore(String eventId, String eventType, String aggregateId, long version, 
                     String eventData, String correlationId, String causationId, 
                     String userId, String source) {
        this();
        this.eventId = eventId;
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.version = version;
        this.eventData = eventData;
        this.correlationId = correlationId;
        this.causationId = causationId;
        this.userId = userId;
        this.source = source;
    }
    
    /**
     * 添加元数据
     */
    public void addMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
    }
    
    /**
     * 获取元数据
     */
    public Object getMetadata(String key) {
        return metadata != null ? metadata.get(key) : null;
    }
    
    /**
     * 获取所有元数据
     */
    public Map<String, Object> getMetadata() {
        if (metadata == null && metadataJson != null) {
            // 这里可以添加JSON反序列化逻辑
            metadata = new HashMap<>();
        }
        return metadata;
    }
    
    /**
     * 设置元数据
     */
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    /**
     * 获取事件摘要
     */
    public String getEventSummary() {
        return String.format("事件[%s] 类型[%s] 聚合根[%s] 版本[%d] 时间[%s]", 
                           eventId, eventType, aggregateId, version, timestamp);
    }
    
    // Getters and Setters
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    
    public String getAggregateId() { return aggregateId; }
    public void setAggregateId(String aggregateId) { this.aggregateId = aggregateId; }
    
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
    
    public String getEventData() { return eventData; }
    public void setEventData(String eventData) { this.eventData = eventData; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    
    public String getCausationId() { return causationId; }
    public void setCausationId(String causationId) { this.causationId = causationId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    
    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }
    
    @Override
    public String toString() {
        return "EventStore{" +
                "eventId='" + eventId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", aggregateId='" + aggregateId + '\'' +
                ", version=" + version +
                ", timestamp=" + timestamp +
                ", correlationId='" + correlationId + '\'' +
                '}';
    }
}
