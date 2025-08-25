package com.hibiscus.signal.core.repository;

import com.hibiscus.signal.core.entity.EventSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 事件快照仓库
 * 提供快照存储和查询功能
 * 
 * @author heathcetide
 */
@Repository
public interface EventSnapshotRepository extends JpaRepository<EventSnapshot, Long> {
    
    /**
     * 根据聚合根ID查找最新快照
     */
    @Query("SELECT s FROM EventSnapshot s WHERE s.aggregateId = :aggregateId ORDER BY s.version DESC")
    List<EventSnapshot> findByAggregateIdOrderByVersionDesc(@Param("aggregateId") String aggregateId);
    
    /**
     * 根据聚合根ID查找最新快照
     */
    default Optional<EventSnapshot> findLatestByAggregateId(String aggregateId) {
        List<EventSnapshot> snapshots = findByAggregateIdOrderByVersionDesc(aggregateId);
        return snapshots.isEmpty() ? Optional.empty() : Optional.of(snapshots.get(0));
    }
    
    /**
     * 根据聚合根ID和版本查找快照
     */
    Optional<EventSnapshot> findByAggregateIdAndVersion(String aggregateId, long version);
    
    /**
     * 根据聚合根ID查找所有快照
     */
    List<EventSnapshot> findByAggregateId(String aggregateId);
    
    /**
     * 根据快照类型查找快照
     */
    List<EventSnapshot> findBySnapshotType(String snapshotType);
    
    /**
     * 根据聚合根ID查找快照数量
     */
    long countByAggregateId(String aggregateId);
    
    /**
     * 根据快照类型查找快照数量
     */
    long countBySnapshotType(String snapshotType);
    
    /**
     * 查找所有快照类型
     */
    @Query("SELECT DISTINCT s.snapshotType FROM EventSnapshot s")
    List<String> findAllSnapshotTypes();
    
    /**
     * 根据聚合根ID删除所有快照
     */
    void deleteByAggregateId(String aggregateId);
    
    /**
     * 根据聚合根ID和版本范围查找快照
     */
    @Query("SELECT s FROM EventSnapshot s WHERE s.aggregateId = :aggregateId AND s.version >= :fromVersion AND s.version <= :toVersion ORDER BY s.version ASC")
    List<EventSnapshot> findByAggregateIdAndVersionRange(@Param("aggregateId") String aggregateId, 
                                                        @Param("fromVersion") long fromVersion, 
                                                        @Param("toVersion") long toVersion);
}
