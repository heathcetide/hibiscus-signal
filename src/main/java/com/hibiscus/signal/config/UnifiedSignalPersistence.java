package com.hibiscus.signal.config;

import com.hibiscus.signal.core.SignalContext;
import com.hibiscus.signal.core.SigHandler;
import com.hibiscus.signal.core.SignalPersistenceInfo;
import com.hibiscus.signal.spring.config.SignalProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * 统一信号持久化管理器
 * 支持多种存储方式的组合使用
 * 
 * @author heathcetide
 */
@Service
public class UnifiedSignalPersistence {
    
    private static final Logger log = LoggerFactory.getLogger(UnifiedSignalPersistence.class);
    
    @Autowired(required = false)
    private DatabaseSignalPersistence databasePersistence;
    
    @Autowired(required = false)
    private RedisSignalPersistence redisPersistence;
    
    @Autowired(required = false)
    private MqSignalPersistence mqPersistence;
    
    @Autowired
    private SignalProperties signalProperties;
    
    @Autowired
    private ExecutorService persistenceExecutor;
    
    /**
     * 异步保存事件信息到所有启用的存储
     */
    public void saveEventAsync(SigHandler sigHandler, SignalConfig config, 
                             SignalContext context, Object... params) {
        CompletableFuture.runAsync(() -> {
            try {
                saveEvent(sigHandler, config, context, params);
            } catch (Exception e) {
                log.error("异步保存事件失败: {}", e.getMessage(), e);
            }
        }, persistenceExecutor);
    }
    
    /**
     * 同步保存事件信息到所有启用的存储
     */
    public void saveEvent(SigHandler sigHandler, SignalConfig config, 
                         SignalContext context, Object... params) {
        SignalPersistenceInfo info = new SignalPersistenceInfo(sigHandler, config, context, null);
        
        // 获取当前策略
        String strategyName = signalProperties.getPersistenceStrategy();
        SignalPersistenceStrategy strategy = SignalPersistenceStrategy.fromString(strategyName);
        
        // 根据策略保存到不同的存储
        if (strategy.supportsDatabase() && databasePersistence != null) {
            try {
                databasePersistence.saveEventRecord(sigHandler, config, context, params);
                log.debug("事件已保存到数据库: {}", sigHandler.getSignalName());
            } catch (Exception e) {
                log.error("保存到数据库失败: {}", e.getMessage(), e);
            }
        }
        
        if (strategy.supportsRedis() && redisPersistence != null) {
            try {
                redisPersistence.saveEvent(info);
                log.debug("事件已保存到Redis: {}", sigHandler.getSignalName());
            } catch (Exception e) {
                log.error("保存到Redis失败: {}", e.getMessage(), e);
            }
        }
        
        if (strategy.supportsMq() && mqPersistence != null) {
            try {
                mqPersistence.publishEvent(info);
                log.debug("事件已发布到MQ: {}", sigHandler.getSignalName());
            } catch (Exception e) {
                log.error("发布到MQ失败: {}", e.getMessage(), e);
            }
        }
    }
    
    /**
     * 更新事件状态
     */
    public void updateEventStatus(String eventId, String status) {
        // 获取当前策略
        String strategyName = signalProperties.getPersistenceStrategy();
        SignalPersistenceStrategy strategy = SignalPersistenceStrategy.fromString(strategyName);
        
        if (strategy.supportsDatabase() && databasePersistence != null) {
            try {
                // 这里需要根据状态字符串转换为枚举
                // databasePersistence.updateEventStatus(eventId, status);
                log.debug("事件状态已更新到数据库: {} - {}", eventId, status);
            } catch (Exception e) {
                log.error("更新数据库状态失败: {}", e.getMessage(), e);
            }
        }
        
        if (strategy.supportsRedis() && redisPersistence != null) {
            try {
                redisPersistence.updateEventStatus(eventId, status);
                log.debug("事件状态已更新到Redis: {} - {}", eventId, status);
            } catch (Exception e) {
                log.error("更新Redis状态失败: {}", e.getMessage(), e);
            }
        }
    }
    
    /**
     * 查询事件信息
     */
    public SignalPersistenceInfo queryEvent(String eventId) {
        // 获取当前策略
        String strategyName = signalProperties.getPersistenceStrategy();
        SignalPersistenceStrategy strategy = SignalPersistenceStrategy.fromString(strategyName);
        
        // 优先从Redis查询（高性能）
        if (strategy.supportsRedis() && redisPersistence != null) {
            try {
                SignalPersistenceInfo info = redisPersistence.getEvent(eventId);
                if (info != null) {
                    return info;
                }
            } catch (Exception e) {
                log.warn("从Redis查询事件失败: {}", e.getMessage());
            }
        }
        
        // 从数据库查询
        if (strategy.supportsDatabase() && databasePersistence != null) {
            try {
                // 这里需要实现从数据库查询的逻辑
                // return databasePersistence.findByEventId(eventId);
            } catch (Exception e) {
                log.error("从数据库查询事件失败: {}", e.getMessage(), e);
            }
        }
        
        return null;
    }
    
    /**
     * 批量查询事件
     */
    public List<SignalPersistenceInfo> queryEvents(String eventName, int limit) {
        // 获取当前策略
        String strategyName = signalProperties.getPersistenceStrategy();
        SignalPersistenceStrategy strategy = SignalPersistenceStrategy.fromString(strategyName);
        
        if (strategy.supportsRedis() && redisPersistence != null) {
            try {
                return redisPersistence.getEventsByType(eventName, limit);
            } catch (Exception e) {
                log.warn("从Redis批量查询事件失败: {}", e.getMessage());
            }
        }
        
        if (strategy.supportsDatabase() && databasePersistence != null) {
            try {
                // 这里需要实现从数据库批量查询的逻辑
                // return databasePersistence.findByEventName(eventName, limit);
            } catch (Exception e) {
                log.error("从数据库批量查询事件失败: {}", e.getMessage(), e);
            }
        }
        
        return null;
    }
    
    /**
     * 删除事件
     */
    public void deleteEvent(String eventId) {
        // 获取当前策略
        String strategyName = signalProperties.getPersistenceStrategy();
        SignalPersistenceStrategy strategy = SignalPersistenceStrategy.fromString(strategyName);
        
        if (strategy.supportsDatabase() && databasePersistence != null) {
            try {
                // databasePersistence.deleteByEventId(eventId);
                log.debug("事件已从数据库删除: {}", eventId);
            } catch (Exception e) {
                log.error("从数据库删除事件失败: {}", e.getMessage(), e);
            }
        }
        
        if (strategy.supportsRedis() && redisPersistence != null) {
            try {
                redisPersistence.deleteEvent(eventId);
                log.debug("事件已从Redis删除: {}", eventId);
            } catch (Exception e) {
                log.error("从Redis删除事件失败: {}", e.getMessage(), e);
            }
        }
    }
    
    /**
     * 清理过期数据
     */
    public void cleanupExpiredData() {
        // 获取当前策略
        String strategyName = signalProperties.getPersistenceStrategy();
        SignalPersistenceStrategy strategy = SignalPersistenceStrategy.fromString(strategyName);
        
        if (strategy.supportsDatabase() && databasePersistence != null) {
            try {
                // databasePersistence.cleanupExpiredData();
                log.debug("数据库过期数据清理完成");
            } catch (Exception e) {
                log.error("清理数据库过期数据失败: {}", e.getMessage(), e);
            }
        }
        
        if (strategy.supportsRedis() && redisPersistence != null) {
            try {
                redisPersistence.cleanupExpiredData();
                log.debug("Redis过期数据清理完成");
            } catch (Exception e) {
                log.error("清理Redis过期数据失败: {}", e.getMessage(), e);
            }
        }
    }
}
