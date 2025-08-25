package com.hibiscus.signal.core.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 事件快照实体
 * 用于存储聚合根的快照，优化事件重放性能
 * 
 * @author heathcetide
 */
@Entity
@Table(name = "event_snapshots", indexes = {
    @Index(name = "idx_snapshot_aggregate_id", columnList = "aggregate_id"),
    @Index(name = "idx_snapshot_timestamp", columnList = "timestamp")
})
@EntityListeners(AuditingEntityListener.class)
public class EventSnapshot {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "aggregate_id", length = 64, nullable = false)
    private String aggregateId;
    
    @Column(name = "version", nullable = false)
    private long version;
    
    @Column(name = "snapshot_data", columnDefinition = "TEXT", nullable = false)
    private String snapshotData;
    
    @Column(name = "snapshot_type", length = 128, nullable = false)
    private String snapshotType;
    
    @Column(name = "timestamp", nullable = false)
    @CreatedDate
    private LocalDateTime timestamp;
    
    @Column(name = "description", length = 512)
    private String description;
    
    @JsonCreator
    public EventSnapshot() {
        this.timestamp = LocalDateTime.now();
    }
    
    public EventSnapshot(String aggregateId, long version, String snapshotData, 
                        String snapshotType, String description) {
        this();
        this.aggregateId = aggregateId;
        this.version = version;
        this.snapshotData = snapshotData;
        this.snapshotType = snapshotType;
        this.description = description;
    }
    
    /**
     * 获取快照摘要
     */
    public String getSnapshotSummary() {
        return String.format("快照[%d] 聚合根[%s] 版本[%d] 类型[%s] 时间[%s]", 
                           id, aggregateId, version, snapshotType, timestamp);
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getAggregateId() { return aggregateId; }
    public void setAggregateId(String aggregateId) { this.aggregateId = aggregateId; }
    
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
    
    public String getSnapshotData() { return snapshotData; }
    public void setSnapshotData(String snapshotData) { this.snapshotData = snapshotData; }
    
    public String getSnapshotType() { return snapshotType; }
    public void setSnapshotType(String snapshotType) { this.snapshotType = snapshotType; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    @Override
    public String toString() {
        return "EventSnapshot{" +
                "id=" + id +
                ", aggregateId='" + aggregateId + '\'' +
                ", version=" + version +
                ", snapshotType='" + snapshotType + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
