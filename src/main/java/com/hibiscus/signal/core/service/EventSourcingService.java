package com.hibiscus.signal.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hibiscus.signal.core.entity.EventStore;
import com.hibiscus.signal.core.entity.EventSnapshot;
import com.hibiscus.signal.core.event.DomainEvent;
import com.hibiscus.signal.core.repository.EventStoreRepository;
import com.hibiscus.signal.core.repository.EventSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 事件溯源服务
 * 提供事件存储、查询和重放功能
 * 
 * @author heathcetide
 */
@Service
public class EventSourcingService {
    
    private static final Logger log = LoggerFactory.getLogger(EventSourcingService.class);
    
    @Autowired
    private EventStoreRepository eventStoreRepository;
    
    @Autowired
    private EventSnapshotRepository eventSnapshotRepository;
    
    @Autowired
    private ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, AtomicLong> versionCounters = new ConcurrentHashMap<>();
    
    /**
     * 存储领域事件
     */
    @Transactional
    public void storeEvent(DomainEvent event) {
        try {
            // 获取下一个版本号
            long nextVersion = getNextVersion(event.getAggregateId());
            
            // 创建事件存储实体
            EventStore eventStore = new EventStore(
                event.getEventId(),
                event.getEventType(),
                event.getAggregateId(),
                nextVersion,
                serializeEvent(event),
                event.getCorrelationId(),
                event.getCausationId(),
                event.getUserId(),
                event.getSource()
            );
            
            // 设置元数据
            if (event.getMetadata() != null && !event.getMetadata().isEmpty()) {
                eventStore.setMetadata(event.getMetadata());
            }
            
            // 保存事件
            eventStoreRepository.save(eventStore);
            
            // 更新版本计数器
            updateVersionCounter(event.getAggregateId(), nextVersion);
            
            log.info("事件存储成功: {}", eventStore.getEventSummary());
            
        } catch (Exception e) {
            log.error("事件存储失败: {} - 错误: {}", event.getEventSummary(), e.getMessage(), e);
            throw new RuntimeException("事件存储失败", e);
        }
    }
    
    /**
     * 批量存储事件
     */
    @Transactional
    public void storeEvents(List<DomainEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        
        for (DomainEvent event : events) {
            storeEvent(event);
        }
        
        log.info("批量事件存储完成，共{}个事件", events.size());
    }
    
    /**
     * 获取聚合根的所有事件
     */
    public List<EventStore> getEvents(String aggregateId) {
        List<EventStore> events = eventStoreRepository.findByAggregateIdOrderByVersion(aggregateId);
        log.debug("获取聚合根[{}]的事件，共{}个", aggregateId, events.size());
        return events;
    }
    
    /**
     * 获取聚合根的事件流
     */
    public EventStream getEventStream(String aggregateId) {
        List<EventStore> events = getEvents(aggregateId);
        return new EventStream(aggregateId, events);
    }
    
    /**
     * 获取聚合根的事件流（从指定版本开始）
     */
    public EventStream getEventStreamFromVersion(String aggregateId, long fromVersion) {
        List<EventStore> events = eventStoreRepository.findByAggregateIdAndVersionRange(
            aggregateId, fromVersion, Long.MAX_VALUE);
        return new EventStream(aggregateId, events);
    }
    
    /**
     * 获取聚合根的事件流（到指定版本结束）
     */
    public EventStream getEventStreamToVersion(String aggregateId, long toVersion) {
        List<EventStore> events = eventStoreRepository.findByAggregateIdAndVersionRange(
            aggregateId, 0, toVersion);
        return new EventStream(aggregateId, events);
    }
    
    /**
     * 获取聚合根的事件流（版本范围）
     */
    public EventStream getEventStreamInRange(String aggregateId, long fromVersion, long toVersion) {
        List<EventStore> events = eventStoreRepository.findByAggregateIdAndVersionRange(
            aggregateId, fromVersion, toVersion);
        return new EventStream(aggregateId, events);
    }
    
    /**
     * 获取聚合根的最新版本
     */
    public long getLatestVersion(String aggregateId) {
        Optional<Long> latestVersion = eventStoreRepository.findLatestVersionByAggregateId(aggregateId);
        return latestVersion.orElse(0L);
    }
    
    /**
     * 检查聚合根是否存在
     */
    public boolean exists(String aggregateId) {
        return eventStoreRepository.existsByAggregateId(aggregateId);
    }
    
    /**
     * 创建快照
     */
    @Transactional
    public void createSnapshot(String aggregateId, Object snapshot, String snapshotType, String description) {
        try {
            // 获取最新版本
            long latestVersion = getLatestVersion(aggregateId);
            if (latestVersion == 0) {
                log.warn("聚合根[{}]不存在，无法创建快照", aggregateId);
                return;
            }
            
            // 创建快照实体
            EventSnapshot eventSnapshot = new EventSnapshot(
                aggregateId,
                latestVersion,
                serializeSnapshot(snapshot),
                snapshotType,
                description
            );
            
            // 保存快照
            eventSnapshotRepository.save(eventSnapshot);
            
            log.info("快照创建成功: {}", eventSnapshot.getSnapshotSummary());
            
        } catch (Exception e) {
            log.error("快照创建失败: 聚合根[{}] - 错误: {}", aggregateId, e.getMessage(), e);
            throw new RuntimeException("快照创建失败", e);
        }
    }
    
    /**
     * 获取最新快照
     */
    public Optional<EventSnapshot> getLatestSnapshot(String aggregateId) {
        return eventSnapshotRepository.findLatestByAggregateId(aggregateId);
    }
    
    /**
     * 获取指定版本的快照
     */
    public Optional<EventSnapshot> getSnapshot(String aggregateId, long version) {
        return eventSnapshotRepository.findByAggregateIdAndVersion(aggregateId, version);
    }
    
    /**
     * 删除聚合根的所有事件和快照
     */
    @Transactional
    public void deleteAggregate(String aggregateId) {
        // 删除快照
        eventSnapshotRepository.deleteByAggregateId(aggregateId);
        
        // 删除事件
        eventStoreRepository.deleteByAggregateId(aggregateId);
        
        // 清理版本计数器
        versionCounters.remove(aggregateId);
        
        log.info("聚合根[{}]的所有事件和快照已删除", aggregateId);
    }
    
    /**
     * 获取下一个版本号
     */
    private long getNextVersion(String aggregateId) {
        AtomicLong counter = versionCounters.computeIfAbsent(aggregateId, k -> new AtomicLong(0));
        return counter.incrementAndGet();
    }
    
    /**
     * 更新版本计数器
     */
    private void updateVersionCounter(String aggregateId, long version) {
        AtomicLong counter = versionCounters.computeIfAbsent(aggregateId, k -> new AtomicLong(0));
        counter.set(version);
    }
    
    /**
     * 序列化事件
     */
    private String serializeEvent(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.error("事件序列化失败: {}", e.getMessage(), e);
            throw new RuntimeException("事件序列化失败", e);
        }
    }
    
    /**
     * 序列化快照
     */
    private String serializeSnapshot(Object snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            log.error("快照序列化失败: {}", e.getMessage(), e);
            throw new RuntimeException("快照序列化失败", e);
        }
    }
    
    /**
     * 事件流类
     */
    public static class EventStream {
        private final String aggregateId;
        private final List<EventStore> events;
        
        public EventStream(String aggregateId, List<EventStore> events) {
            this.aggregateId = aggregateId;
            this.events = events;
        }
        
        public String getAggregateId() { return aggregateId; }
        public List<EventStore> getEvents() { return events; }
        public int getEventCount() { return events.size(); }
        public boolean isEmpty() { return events.isEmpty(); }
        
        public EventStore getFirstEvent() {
            return events.isEmpty() ? null : events.get(0);
        }
        
        public EventStore getLastEvent() {
            return events.isEmpty() ? null : events.get(events.size() - 1);
        }
        
        public long getFirstVersion() {
            EventStore first = getFirstEvent();
            return first != null ? first.getVersion() : 0;
        }
        
        public long getLastVersion() {
            EventStore last = getLastEvent();
            return last != null ? last.getVersion() : 0;
        }
    }
}
