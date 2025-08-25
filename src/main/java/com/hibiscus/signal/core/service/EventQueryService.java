package com.hibiscus.signal.core.service;

import com.hibiscus.signal.core.entity.EventStore;
import com.hibiscus.signal.core.repository.EventStoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 事件查询服务
 * 提供复杂的事件查询功能
 * 
 * @author heathcetide
 */
@Service
public class EventQueryService {
    
    private static final Logger log = LoggerFactory.getLogger(EventQueryService.class);
    
    @Autowired
    private EventStoreRepository eventStoreRepository;
    
    /**
     * 根据事件类型查询事件
     */
    public Page<EventStore> queryByEventType(String eventType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<EventStore> events = eventStoreRepository.findByEventType(eventType, pageable);
        
        log.debug("查询事件类型[{}]: 第{}页，每页{}条，共{}条", eventType, page, size, events.getTotalElements());
        return events;
    }
    
    /**
     * 根据时间范围查询事件
     */
    public Page<EventStore> queryByTimeRange(LocalDateTime startTime, LocalDateTime endTime, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<EventStore> events = eventStoreRepository.findByTimeRange(startTime, endTime, pageable);
        
        log.debug("查询时间范围[{} - {}]: 第{}页，每页{}条，共{}条", 
                startTime, endTime, page, size, events.getTotalElements());
        return events;
    }
    
    /**
     * 根据用户ID查询事件
     */
    public Page<EventStore> queryByUserId(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<EventStore> events = eventStoreRepository.findByUserId(userId, pageable);
        
        log.debug("查询用户[{}]事件: 第{}页，每页{}条，共{}条", userId, page, size, events.getTotalElements());
        return events;
    }
    
    /**
     * 根据关联ID查询事件
     */
    public List<EventStore> queryByCorrelationId(String correlationId) {
        List<EventStore> events = eventStoreRepository.findByCorrelationId(correlationId);
        
        log.debug("查询关联ID[{}]事件: 共{}条", correlationId, events.size());
        return events;
    }
    
    /**
     * 根据因果ID查询事件
     */
    public List<EventStore> queryByCausationId(String causationId) {
        List<EventStore> events = eventStoreRepository.findByCausationId(causationId);
        
        log.debug("查询因果ID[{}]事件: 共{}条", causationId, events.size());
        return events;
    }
    
    /**
     * 根据聚合根ID查询事件
     */
    public List<EventStore> queryByAggregateId(String aggregateId) {
        List<EventStore> events = eventStoreRepository.findByAggregateIdOrderByVersion(aggregateId);
        
        log.debug("查询聚合根[{}]事件: 共{}条", aggregateId, events.size());
        return events;
    }
    
    /**
     * 根据聚合根ID和版本范围查询事件
     */
    public List<EventStore> queryByAggregateIdAndVersionRange(String aggregateId, long fromVersion, long toVersion) {
        List<EventStore> events = eventStoreRepository.findByAggregateIdAndVersionRange(aggregateId, fromVersion, toVersion);
        
        log.debug("查询聚合根[{}]版本范围[{}-{}]事件: 共{}条", aggregateId, fromVersion, toVersion, events.size());
        return events;
    }
    
    /**
     * 复合查询：根据多个条件查询事件
     */
    public Page<EventStore> complexQuery(EventQuery query, int page, int size) {
        // 这里可以实现更复杂的查询逻辑
        // 暂时返回空结果，实际使用时需要实现具体的查询逻辑
        log.warn("复合查询功能需要根据具体需求实现");
        return Page.empty();
    }
    
    /**
     * 获取事件统计信息
     */
    public EventStatistics getEventStatistics() {
        EventStatistics stats = new EventStatistics();
        
        // 获取所有聚合根ID
        List<String> aggregateIds = eventStoreRepository.findAllAggregateIds();
        stats.setTotalAggregates(aggregateIds.size());
        
        // 获取所有事件类型
        List<String> eventTypes = eventStoreRepository.findAllEventTypes();
        stats.setTotalEventTypes(eventTypes.size());
        
        // 统计每种事件类型的数量
        Map<String, Long> eventTypeCounts = eventTypes.stream()
                .collect(Collectors.toMap(
                    eventType -> eventType,
                    eventType -> eventStoreRepository.countByEventType(eventType)
                ));
        stats.setEventTypeCounts(eventTypeCounts);
        
        // 统计总事件数
        long totalEvents = eventStoreRepository.count();
        stats.setTotalEvents(totalEvents);
        
        log.debug("事件统计信息: 聚合根[{}] 事件类型[{}] 总事件[{}]", 
                stats.getTotalAggregates(), stats.getTotalEventTypes(), stats.getTotalEvents());
        
        return stats;
    }
    
    /**
     * 获取聚合根统计信息
     */
    public AggregateStatistics getAggregateStatistics(String aggregateId) {
        AggregateStatistics stats = new AggregateStatistics();
        stats.setAggregateId(aggregateId);
        
        // 获取事件数量
        long eventCount = eventStoreRepository.countByAggregateId(aggregateId);
        stats.setEventCount(eventCount);
        
        // 获取最新版本
        long latestVersion = eventStoreRepository.findLatestVersionByAggregateId(aggregateId).orElse(0L);
        stats.setLatestVersion(latestVersion);
        
        // 检查是否存在
        boolean exists = eventStoreRepository.existsByAggregateId(aggregateId);
        stats.setExists(exists);
        
        log.debug("聚合根[{}]统计信息: 事件数[{}] 最新版本[{}] 存在[{}]", 
                aggregateId, eventCount, latestVersion, exists);
        
        return stats;
    }
    
    /**
     * 事件查询条件
     */
    public static class EventQuery {
        private String eventType;
        private String aggregateId;
        private String userId;
        private String correlationId;
        private String causationId;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Long fromVersion;
        private Long toVersion;
        private Map<String, Object> metadata;
        
        // Getters and Setters
        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }
        
        public String getAggregateId() { return aggregateId; }
        public void setAggregateId(String aggregateId) { this.aggregateId = aggregateId; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getCorrelationId() { return correlationId; }
        public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
        
        public String getCausationId() { return causationId; }
        public void setCausationId(String causationId) { this.causationId = causationId; }
        
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        
        public Long getFromVersion() { return fromVersion; }
        public void setFromVersion(Long fromVersion) { this.fromVersion = fromVersion; }
        
        public Long getToVersion() { return toVersion; }
        public void setToVersion(Long toVersion) { this.toVersion = toVersion; }
        
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }
    
    /**
     * 事件统计信息
     */
    public static class EventStatistics {
        private int totalAggregates;
        private int totalEventTypes;
        private long totalEvents;
        private Map<String, Long> eventTypeCounts;
        
        // Getters and Setters
        public int getTotalAggregates() { return totalAggregates; }
        public void setTotalAggregates(int totalAggregates) { this.totalAggregates = totalAggregates; }
        
        public int getTotalEventTypes() { return totalEventTypes; }
        public void setTotalEventTypes(int totalEventTypes) { this.totalEventTypes = totalEventTypes; }
        
        public long getTotalEvents() { return totalEvents; }
        public void setTotalEvents(long totalEvents) { this.totalEvents = totalEvents; }
        
        public Map<String, Long> getEventTypeCounts() { return eventTypeCounts; }
        public void setEventTypeCounts(Map<String, Long> eventTypeCounts) { this.eventTypeCounts = eventTypeCounts; }
    }
    
    /**
     * 聚合根统计信息
     */
    public static class AggregateStatistics {
        private String aggregateId;
        private long eventCount;
        private long latestVersion;
        private boolean exists;
        
        // Getters and Setters
        public String getAggregateId() { return aggregateId; }
        public void setAggregateId(String aggregateId) { this.aggregateId = aggregateId; }
        
        public long getEventCount() { return eventCount; }
        public void setEventCount(long eventCount) { this.eventCount = eventCount; }
        
        public long getLatestVersion() { return latestVersion; }
        public void setLatestVersion(long latestVersion) { this.latestVersion = latestVersion; }
        
        public boolean isExists() { return exists; }
        public void setExists(boolean exists) { this.exists = exists; }
    }
}
