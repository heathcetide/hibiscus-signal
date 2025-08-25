package com.hibiscus.signal.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hibiscus.signal.core.entity.EventSnapshot;
import com.hibiscus.signal.core.event.DomainEvent;
import com.hibiscus.signal.core.repository.EventStoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 事件重放服务
 * 提供从事件重建聚合根状态的功能
 * 
 * @author heathcetide
 */
@Service
public class EventReplayService {
    
    private static final Logger log = LoggerFactory.getLogger(EventReplayService.class);
    
    @Autowired
    private EventSourcingService eventSourcingService;
    
    @Autowired
    private EventStoreRepository eventStoreRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 从事件重建聚合根
     */
    public <T> T replayEvents(String aggregateId, Class<T> aggregateType) {
        return replayEvents(aggregateId, aggregateType, null);
    }
    
    /**
     * 从事件重建聚合根（从指定版本开始）
     */
    public <T> T replayEventsFromVersion(String aggregateId, Class<T> aggregateType, long fromVersion) {
        return replayEvents(aggregateId, aggregateType, null, fromVersion, Long.MAX_VALUE);
    }
    
    /**
     * 从事件重建聚合根（到指定版本结束）
     */
    public <T> T replayEventsToVersion(String aggregateId, Class<T> aggregateType, long toVersion) {
        return replayEvents(aggregateId, aggregateType, null, 0, toVersion);
    }
    
    /**
     * 从事件重建聚合根（版本范围）
     */
    public <T> T replayEventsInRange(String aggregateId, Class<T> aggregateType, long fromVersion, long toVersion) {
        return replayEvents(aggregateId, aggregateType, null, fromVersion, toVersion);
    }
    
    /**
     * 从事件重建聚合根（使用快照优化）
     */
    public <T> T replayEvents(String aggregateId, Class<T> aggregateType, EventSnapshot snapshot) {
        return replayEvents(aggregateId, aggregateType, snapshot, 0, Long.MAX_VALUE);
    }
    
    /**
     * 从事件重建聚合根（核心方法）
     */
    @SuppressWarnings("unchecked")
    public <T> T replayEvents(String aggregateId, Class<T> aggregateType, EventSnapshot snapshot, 
                              long fromVersion, long toVersion) {
        try {
            log.debug("开始重放事件: 聚合根[{}] 类型[{}] 版本范围[{}-{}]", 
                     aggregateId, aggregateType.getSimpleName(), fromVersion, toVersion);
            
            // 1. 尝试从快照开始重建
            T aggregate = null;
            long startVersion = fromVersion;
            
            if (snapshot != null && snapshot.getVersion() >= fromVersion && snapshot.getVersion() <= toVersion) {
                try {
                    aggregate = deserializeSnapshot(snapshot.getSnapshotData(), aggregateType);
                    startVersion = snapshot.getVersion() + 1;
                    log.debug("从快照[版本{}]开始重建聚合根[{}]", snapshot.getVersion(), aggregateId);
                } catch (Exception e) {
                    log.warn("快照反序列化失败，从事件开始重建: {}", e.getMessage());
                    startVersion = fromVersion;
                }
            }
            
            // 2. 获取需要重放的事件
            List<com.hibiscus.signal.core.entity.EventStore> events;
            if (startVersion == fromVersion) {
                // 从开始版本获取事件
                events = eventStoreRepository.findByAggregateIdAndVersionRange(aggregateId, fromVersion, toVersion);
            } else {
                // 从快照版本后开始获取事件
                events = eventStoreRepository.findByAggregateIdAndVersionRange(aggregateId, startVersion, toVersion);
            }
            
            if (events.isEmpty()) {
                log.debug("聚合根[{}]在版本范围[{}-{}]内没有事件", aggregateId, fromVersion, toVersion);
                return aggregate;
            }
            
            // 3. 重放事件
            aggregate = replayEventList(events, aggregate, aggregateType);
            
            log.info("事件重放完成: 聚合根[{}] 版本范围[{}-{}] 事件数量[{}]", 
                    aggregateId, fromVersion, toVersion, events.size());
            
            return aggregate;
            
        } catch (Exception e) {
            log.error("事件重放失败: 聚合根[{}] - 错误: {}", aggregateId, e.getMessage(), e);
            throw new RuntimeException("事件重放失败", e);
        }
    }
    
    /**
     * 重放事件列表
     */
    @SuppressWarnings("unchecked")
    private <T> T replayEventList(List<com.hibiscus.signal.core.entity.EventStore> events, 
                                 T aggregate, Class<T> aggregateType) {
        try {
            for (com.hibiscus.signal.core.entity.EventStore eventStore : events) {
                // 反序列化事件
                DomainEvent event = deserializeEvent(eventStore.getEventData());
                
                // 应用事件到聚合根
                aggregate = applyEvent(event, aggregate, aggregateType);
                
                log.debug("重放事件: {} -> 版本[{}]", event.getEventSummary(), eventStore.getVersion());
            }
            
            return aggregate;
            
        } catch (Exception e) {
            log.error("事件列表重放失败: {}", e.getMessage(), e);
            throw new RuntimeException("事件列表重放失败", e);
        }
    }
    
    /**
     * 应用事件到聚合根
     */
    @SuppressWarnings("unchecked")
    private <T> T applyEvent(DomainEvent event, T aggregate, Class<T> aggregateType) {
        try {
            // 如果聚合根为空，尝试创建新实例
            if (aggregate == null) {
                aggregate = createAggregateInstance(aggregateType);
            }
            
            // 检查聚合根是否实现了事件处理器接口
            if (aggregate instanceof EventHandler) {
                ((EventHandler) aggregate).handleEvent(event);
            } else {
                // 使用反射调用事件处理方法
                applyEventByReflection(event, aggregate);
            }
            
            return aggregate;
            
        } catch (Exception e) {
            log.error("应用事件失败: {} - 错误: {}", event.getEventSummary(), e.getMessage(), e);
            throw new RuntimeException("应用事件失败", e);
        }
    }
    
    /**
     * 创建聚合根实例
     */
    private <T> T createAggregateInstance(Class<T> aggregateType) {
        try {
            return aggregateType.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            log.error("创建聚合根实例失败: {} - 错误: {}", aggregateType.getSimpleName(), e.getMessage(), e);
            throw new RuntimeException("创建聚合根实例失败", e);
        }
    }
    
    /**
     * 使用反射应用事件
     */
    private void applyEventByReflection(DomainEvent event, Object aggregate) {
        try {
            // 构建方法名：handle + 事件类型
            String methodName = "handle" + event.getEventType();
            
            // 查找方法
            java.lang.reflect.Method method = aggregate.getClass().getMethod(methodName, event.getClass());
            
            // 调用方法
            method.invoke(aggregate, event);
            
        } catch (NoSuchMethodException e) {
            log.warn("聚合根[{}]没有找到事件处理方法: {}", 
                    aggregate.getClass().getSimpleName(), e.getMessage());
        } catch (Exception e) {
            log.error("反射调用事件处理方法失败: {}", e.getMessage(), e);
            throw new RuntimeException("反射调用事件处理方法失败", e);
        }
    }
    
    /**
     * 反序列化事件
     */
    private DomainEvent deserializeEvent(String eventData) {
        try {
            // 这里需要根据事件类型进行反序列化
            // 暂时返回null，实际使用时需要实现具体的反序列化逻辑
            log.warn("事件反序列化功能需要根据具体事件类型实现");
            return null;
        } catch (Exception e) {
            log.error("事件反序列化失败: {}", e.getMessage(), e);
            throw new RuntimeException("事件反序列化失败", e);
        }
    }
    
    /**
     * 反序列化快照
     */
    private <T> T deserializeSnapshot(String snapshotData, Class<T> snapshotType) {
        try {
            return objectMapper.readValue(snapshotData, snapshotType);
        } catch (JsonProcessingException e) {
            log.error("快照反序列化失败: {}", e.getMessage(), e);
            throw new RuntimeException("快照反序列化失败", e);
        }
    }
    
    /**
     * 事件处理器接口
     */
    public interface EventHandler {
        void handleEvent(DomainEvent event);
    }
}
