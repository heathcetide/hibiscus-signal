package com.hibiscus.signal.core.repository;

import com.hibiscus.signal.core.entity.EventStore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 事件存储仓库
 * 提供事件存储和查询功能
 * 
 * @author heathcetide
 */
@Repository
public interface EventStoreRepository extends JpaRepository<EventStore, String> {
    
    /**
     * 根据聚合根ID查找所有事件，按版本排序
     */
    @Query("SELECT e FROM EventStore e WHERE e.aggregateId = :aggregateId ORDER BY e.version ASC")
    List<EventStore> findByAggregateIdOrderByVersion(@Param("aggregateId") String aggregateId);
    
    /**
     * 根据聚合根ID和版本范围查找事件
     */
    @Query("SELECT e FROM EventStore e WHERE e.aggregateId = :aggregateId AND e.version >= :fromVersion AND e.version <= :toVersion ORDER BY e.version ASC")
    List<EventStore> findByAggregateIdAndVersionRange(@Param("aggregateId") String aggregateId, 
                                                     @Param("fromVersion") long fromVersion, 
                                                     @Param("toVersion") long toVersion);
    
    /**
     * 根据聚合根ID查找最新版本
     */
    @Query("SELECT MAX(e.version) FROM EventStore e WHERE e.aggregateId = :aggregateId")
    Optional<Long> findLatestVersionByAggregateId(@Param("aggregateId") String aggregateId);
    
    /**
     * 根据事件类型查找事件
     */
    @Query("SELECT e FROM EventStore e WHERE e.eventType = :eventType ORDER BY e.timestamp DESC")
    Page<EventStore> findByEventType(@Param("eventType") String eventType, Pageable pageable);
    
    /**
     * 根据时间范围查找事件
     */
    @Query("SELECT e FROM EventStore e WHERE e.timestamp >= :startTime AND e.timestamp <= :endTime ORDER BY e.timestamp DESC")
    Page<EventStore> findByTimeRange(@Param("startTime") LocalDateTime startTime, 
                                    @Param("endTime") LocalDateTime endTime, 
                                    Pageable pageable);
    
    /**
     * 根据关联ID查找事件
     */
    @Query("SELECT e FROM EventStore e WHERE e.correlationId = :correlationId ORDER BY e.timestamp ASC")
    List<EventStore> findByCorrelationId(@Param("correlationId") String correlationId);
    
    /**
     * 根据因果ID查找事件
     */
    @Query("SELECT e FROM EventStore e WHERE e.causationId = :causationId ORDER BY e.timestamp ASC")
    List<EventStore> findByCausationId(@Param("causationId") String causationId);
    
    /**
     * 根据用户ID查找事件
     */
    @Query("SELECT e FROM EventStore e WHERE e.userId = :userId ORDER BY e.timestamp DESC")
    Page<EventStore> findByUserId(@Param("userId") String userId, Pageable pageable);
    
    /**
     * 根据聚合根ID查找事件数量
     */
    @Query("SELECT COUNT(e) FROM EventStore e WHERE e.aggregateId = :aggregateId")
    long countByAggregateId(@Param("aggregateId") String aggregateId);
    
    /**
     * 根据事件类型查找事件数量
     */
    @Query("SELECT COUNT(e) FROM EventStore e WHERE e.eventType = :eventType")
    long countByEventType(@Param("eventType") String eventType);
    
    /**
     * 查找所有聚合根ID
     */
    @Query("SELECT DISTINCT e.aggregateId FROM EventStore e")
    List<String> findAllAggregateIds();
    
    /**
     * 查找所有事件类型
     */
    @Query("SELECT DISTINCT e.eventType FROM EventStore e")
    List<String> findAllEventTypes();
    
    /**
     * 根据聚合根ID和版本查找事件
     */
    Optional<EventStore> findByAggregateIdAndVersion(String aggregateId, long version);
    
    /**
     * 检查聚合根是否存在
     */
    boolean existsByAggregateId(String aggregateId);
    
    /**
     * 删除聚合根的所有事件
     */
    void deleteByAggregateId(String aggregateId);
}
