package com.hibiscus.signal.core.repository;

import com.hibiscus.signal.core.entity.EventRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 事件记录Repository
 * 提供事件记录的数据库操作接口
 */
@Repository
public interface EventRecordRepository extends JpaRepository<EventRecord, Long> {

    /**
     * 根据事件ID查找事件记录
     */
    Optional<EventRecord> findByEventId(String eventId);

    /**
     * 根据事件名称查找事件记录
     */
    List<EventRecord> findByEventName(String eventName);

    /**
     * 根据状态查找事件记录
     */
    List<EventRecord> findByStatus(EventRecord.EventStatus status);

    /**
     * 根据状态分页查找事件记录
     */
    Page<EventRecord> findByStatus(EventRecord.EventStatus status, Pageable pageable);

    /**
     * 查找需要重试的事件（失败状态且重试次数未达上限）
     */
    @Query("SELECT e FROM EventRecord e WHERE e.status = 'FAILED' AND e.retryCount < e.maxRetries AND (e.nextRetryTime IS NULL OR e.nextRetryTime <= :now)")
    List<EventRecord> findRetryableEvents(@Param("now") LocalDateTime now);

    /**
     * 查找死信事件
     */
    @Query("SELECT e FROM EventRecord e WHERE e.status = 'DEAD_LETTER'")
    List<EventRecord> findDeadLetterEvents();

    /**
     * 根据时间范围查找事件记录
     */
    @Query("SELECT e FROM EventRecord e WHERE e.createdTime BETWEEN :startTime AND :endTime")
    List<EventRecord> findByTimeRange(@Param("startTime") LocalDateTime startTime, 
                                     @Param("endTime") LocalDateTime endTime);

    /**
     * 根据事件名称和时间范围查找
     */
    @Query("SELECT e FROM EventRecord e WHERE e.eventName = :eventName AND e.createdTime BETWEEN :startTime AND :endTime")
    List<EventRecord> findByEventNameAndTimeRange(@Param("eventName") String eventName,
                                                 @Param("startTime") LocalDateTime startTime,
                                                 @Param("endTime") LocalDateTime endTime);

    /**
     * 查找处理中的事件
     */
    @Query("SELECT e FROM EventRecord e WHERE e.status = 'PROCESSING'")
    List<EventRecord> findProcessingEvents();

    /**
     * 查找长时间未完成的事件（可能卡住的事件）
     */
    @Query("SELECT e FROM EventRecord e WHERE e.status = 'PROCESSING' AND e.processStartTime < :timeout")
    List<EventRecord> findStuckEvents(@Param("timeout") LocalDateTime timeout);

    /**
     * 统计各状态的事件数量
     */
    @Query("SELECT e.status, COUNT(e) FROM EventRecord e GROUP BY e.status")
    List<Object[]> countByStatus();

    /**
     * 统计指定事件名称的处理情况
     */
    @Query("SELECT e.status, COUNT(e) FROM EventRecord e WHERE e.eventName = :eventName GROUP BY e.status")
    List<Object[]> countByEventNameAndStatus(@Param("eventName") String eventName);

    /**
     * 删除指定时间之前的事件记录
     */
    @Modifying
    @Query("DELETE FROM EventRecord e WHERE e.createdTime < :beforeTime")
    int deleteByCreatedTimeBefore(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * 删除指定状态的事件记录
     */
    @Modifying
    @Query("DELETE FROM EventRecord e WHERE e.status = :status")
    int deleteByStatus(@Param("status") EventRecord.EventStatus status);

    /**
     * 批量更新事件状态
     */
    @Modifying
    @Query("UPDATE EventRecord e SET e.status = :newStatus, e.updatedTime = :updatedTime WHERE e.id IN :ids")
    int updateStatusByIds(@Param("ids") List<Long> ids, 
                         @Param("newStatus") EventRecord.EventStatus newStatus,
                         @Param("updatedTime") LocalDateTime updatedTime);

    /**
     * 查找最近失败的事件
     */
    @Query("SELECT e FROM EventRecord e WHERE e.status = 'FAILED' ORDER BY e.updatedTime DESC")
    List<EventRecord> findRecentFailedEvents(Pageable pageable);

    /**
     * 查找指定事件名称的最近记录
     */
    @Query("SELECT e FROM EventRecord e WHERE e.eventName = :eventName ORDER BY e.createdTime DESC")
    List<EventRecord> findRecentByEventName(@Param("eventName") String eventName, Pageable pageable);

    /**
     * 检查事件ID是否存在
     */
    boolean existsByEventId(String eventId);

    /**
     * 根据错误信息查找事件
     */
    @Query("SELECT e FROM EventRecord e WHERE e.errorMessage LIKE %:errorPattern%")
    List<EventRecord> findByErrorMessageContaining(@Param("errorPattern") String errorPattern);
}
