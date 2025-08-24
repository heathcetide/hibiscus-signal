package com.hibiscus.signal.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hibiscus.signal.core.SignalPersistenceInfo;
import com.hibiscus.signal.spring.config.SignalProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis信号持久化服务
 * 提供基于Redis的事件存储功能
 * 只有在Redis依赖存在且明确启用时才加载
 * 
 * @author heathcetide
 */
@Service
@ConditionalOnClass(RedisTemplate.class)
@ConditionalOnProperty(name = "hibiscus.redis.enabled", havingValue = "true", matchIfMissing = false)
public class RedisSignalPersistence {
    
    private static final Logger log = LoggerFactory.getLogger(RedisSignalPersistence.class);
    
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private SignalProperties signalProperties;
    
    @Autowired(required = false)
    private ObjectMapper objectMapper;
    
    // Redis Key 前缀
    private static final String EVENT_KEY_PREFIX = "signal:event:";
    private static final String EVENT_LIST_KEY_PREFIX = "signal:events:";
    private static final String EVENT_COUNT_KEY_PREFIX = "signal:count:";
    
    /**
     * 检查Redis是否可用
     */
    private boolean isRedisAvailable() {
        return redisTemplate != null && objectMapper != null;
    }
    
    /**
     * 保存事件到Redis
     */
    public void saveEvent(SignalPersistenceInfo info) {
        if (!isRedisAvailable()) {
            log.warn("Redis不可用，跳过事件保存: {}", info.getSigHandler().getSignalName());
            return;
        }
        
        try {
            String eventId = info.getSignalContext().getEventId();
            String eventName = info.getSigHandler().getSignalName();
            
            // 1. 保存事件详情到Hash
            String eventKey = EVENT_KEY_PREFIX + eventId;
            String eventJson = objectMapper.writeValueAsString(info);
            redisTemplate.opsForHash().put(eventKey, "data", eventJson);
            redisTemplate.opsForHash().put(eventKey, "eventName", eventName);
            redisTemplate.opsForHash().put(eventKey, "status", "PROCESSING");
            redisTemplate.opsForHash().put(eventKey, "createTime", LocalDateTime.now().toString());
            
            // 设置过期时间
            redisTemplate.expire(eventKey, signalProperties.getRedisExpireSeconds(), TimeUnit.SECONDS);
            
            // 2. 添加到事件类型的有序集合（按时间排序）
            String eventListKey = EVENT_LIST_KEY_PREFIX + eventName;
            double score = System.currentTimeMillis();
            redisTemplate.opsForZSet().add(eventListKey, eventId, score);
            
            // 3. 更新事件计数
            String countKey = EVENT_COUNT_KEY_PREFIX + eventName;
            redisTemplate.opsForValue().increment(countKey);
            
            log.debug("事件已保存到Redis: {} - {}", eventName, eventId);
            
        } catch (JsonProcessingException e) {
            log.error("序列化事件数据失败: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("保存事件到Redis失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 更新事件状态
     */
    public void updateEventStatus(String eventId, String status) {
        if (!isRedisAvailable()) {
            log.warn("Redis不可用，跳过状态更新: {} - {}", eventId, status);
            return;
        }
        
        try {
            String eventKey = EVENT_KEY_PREFIX + eventId;
            
            // 更新状态
            redisTemplate.opsForHash().put(eventKey, "status", status);
            redisTemplate.opsForHash().put(eventKey, "updateTime", LocalDateTime.now().toString());
            
            // 如果是失败状态，记录到失败事件集合
            if ("FAILED".equals(status) || "DEAD_LETTER".equals(status)) {
                String failedEventsKey = "signal:failed:events";
                redisTemplate.opsForZSet().add(failedEventsKey, eventId, System.currentTimeMillis());
            }
            
            log.debug("事件状态已更新到Redis: {} - {}", eventId, status);
            
        } catch (Exception e) {
            log.error("更新Redis事件状态失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 获取事件信息
     */
    public SignalPersistenceInfo getEvent(String eventId) {
        if (!isRedisAvailable()) {
            log.warn("Redis不可用，无法获取事件: {}", eventId);
            return null;
        }
        
        try {
            String eventKey = EVENT_KEY_PREFIX + eventId;
            
            // 从Hash中获取事件数据
            Object eventData = redisTemplate.opsForHash().get(eventKey, "data");
            if (eventData != null) {
                return objectMapper.readValue(eventData.toString(), SignalPersistenceInfo.class);
            }
            
        } catch (Exception e) {
            log.error("从Redis获取事件失败: {}", e.getMessage(), e);
        }
        
        return null;
    }
    
    /**
     * 根据事件类型获取事件列表
     */
    public List<SignalPersistenceInfo> getEventsByType(String eventName, int limit) {
        if (!isRedisAvailable()) {
            log.warn("Redis不可用，无法获取事件列表: {}", eventName);
            return new ArrayList<>();
        }
        
        List<SignalPersistenceInfo> events = new ArrayList<>();
        
        try {
            String eventListKey = EVENT_LIST_KEY_PREFIX + eventName;
            
            // 从有序集合中获取最新的事件ID
            Set<Object> eventIds = redisTemplate.opsForZSet().reverseRange(eventListKey, 0, limit - 1);
            
            if (eventIds != null) {
                for (Object eventId : eventIds) {
                    SignalPersistenceInfo event = getEvent(eventId.toString());
                    if (event != null) {
                        events.add(event);
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("从Redis批量获取事件失败: {}", e.getMessage(), e);
        }
        
        return events;
    }
    
    /**
     * 删除事件
     */
    public void deleteEvent(String eventId) {
        if (!isRedisAvailable()) {
            log.warn("Redis不可用，无法删除事件: {}", eventId);
            return;
        }
        
        try {
            String eventKey = EVENT_KEY_PREFIX + eventId;
            
            // 获取事件名称，用于从事件列表中删除
            Object eventName = redisTemplate.opsForHash().get(eventKey, "eventName");
            
            // 删除事件详情
            redisTemplate.delete(eventKey);
            
            // 从事件列表中删除
            if (eventName != null) {
                String eventListKey = EVENT_LIST_KEY_PREFIX + eventName.toString();
                redisTemplate.opsForZSet().remove(eventListKey, eventId);
            }
            
            // 从失败事件列表中删除
            String failedEventsKey = "signal:failed:events";
            redisTemplate.opsForZSet().remove(failedEventsKey, eventId);
            
            log.debug("事件已从Redis删除: {}", eventId);
            
        } catch (Exception e) {
            log.error("从Redis删除事件失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 清理过期数据
     */
    public void cleanupExpiredData() {
        if (!isRedisAvailable()) {
            log.warn("Redis不可用，无法清理过期数据");
            return;
        }
        
        try {
            // 清理过期的失败事件（保留最近7天）
            String failedEventsKey = "signal:failed:events";
            long cutoffTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L);
            
            Set<Object> expiredFailedEvents = redisTemplate.opsForZSet()
                .rangeByScore(failedEventsKey, 0, cutoffTime);
            
            if (expiredFailedEvents != null && !expiredFailedEvents.isEmpty()) {
                redisTemplate.opsForZSet().removeRangeByScore(failedEventsKey, 0, cutoffTime);
                log.info("清理了 {} 个过期的失败事件", expiredFailedEvents.size());
            }
            
            log.debug("Redis过期数据清理完成");
            
        } catch (Exception e) {
            log.error("清理Redis过期数据失败: {}", e.getMessage(), e);
        }
    }
}
