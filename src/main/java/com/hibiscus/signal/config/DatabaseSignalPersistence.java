package com.hibiscus.signal.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hibiscus.signal.core.SignalContext;
import com.hibiscus.signal.core.SigHandler;
import com.hibiscus.signal.core.entity.EventRecord;
import com.hibiscus.signal.core.repository.EventRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 数据库信号持久化服务
 * 提供基于数据库的事件持久化功能
 */
@Service
@ConditionalOnProperty(name = "hibiscus.databasePersistent", havingValue = "true")
public class DatabaseSignalPersistence {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSignalPersistence.class);

    @Autowired
    private EventRecordRepository eventRecordRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 保存事件记录到数据库
     */
    @Transactional
    public EventRecord saveEventRecord(SigHandler sigHandler, SignalConfig config, 
                                     SignalContext context, Object... params) {
        try {
            String eventId = generateEventId();
            
            EventRecord eventRecord = new EventRecord(eventId, sigHandler.getSignalName());
            eventRecord.setMaxRetries(config.getMaxRetries());
            
            // 序列化上下文数据
            if (context != null) {
                eventRecord.setContextData(objectMapper.writeValueAsString(context));
            }
            
            // 序列化参数数据
            if (params != null && params.length > 0) {
                eventRecord.setParamsData(objectMapper.writeValueAsString(params));
            }
            
            // 序列化处理器信息
            eventRecord.setHandlerInfo(objectMapper.writeValueAsString(sigHandler));
            
            // 序列化配置信息
            eventRecord.setConfigInfo(objectMapper.writeValueAsString(config));
            
            EventRecord savedRecord = eventRecordRepository.save(eventRecord);
            log.info("事件记录已保存到数据库: {} - {}", eventId, sigHandler.getSignalName());
            
            return savedRecord;
            
        } catch (JsonProcessingException e) {
            log.error("序列化事件数据失败: {}", e.getMessage(), e);
            throw new RuntimeException("序列化事件数据失败", e);
        } catch (Exception e) {
            log.error("保存事件记录失败: {}", e.getMessage(), e);
            throw new RuntimeException("保存事件记录失败", e);
        }
    }

    /**
     * 更新事件处理状态
     */
    @Transactional
    public void updateEventStatus(String eventId, EventRecord.EventStatus status) {
        Optional<EventRecord> optional = eventRecordRepository.findByEventId(eventId);
        if (optional.isPresent()) {
            EventRecord record = optional.get();
            record.setStatus(status);
            eventRecordRepository.save(record);
            log.debug("事件状态已更新: {} - {}", eventId, status);
        } else {
            log.warn("未找到事件记录: {}", eventId);
        }
    }

    /**
     * 更新事件处理开始状态
     */
    @Transactional
    public void updateEventProcessing(String eventId) {
        Optional<EventRecord> optional = eventRecordRepository.findByEventId(eventId);
        if (optional.isPresent()) {
            EventRecord record = optional.get();
            record.setProcessing();
            eventRecordRepository.save(record);
            log.debug("事件处理开始: {}", eventId);
        }
    }

    /**
     * 更新事件处理成功状态
     */
    @Transactional
    public void updateEventSuccess(String eventId) {
        Optional<EventRecord> optional = eventRecordRepository.findByEventId(eventId);
        if (optional.isPresent()) {
            EventRecord record = optional.get();
            record.setSuccess();
            eventRecordRepository.save(record);
            log.debug("事件处理成功: {}", eventId);
        }
    }

    /**
     * 更新事件处理失败状态
     */
    @Transactional
    public void updateEventFailed(String eventId, String errorMessage, String errorStack) {
        Optional<EventRecord> optional = eventRecordRepository.findByEventId(eventId);
        if (optional.isPresent()) {
            EventRecord record = optional.get();
            record.setFailed(errorMessage, errorStack);
            eventRecordRepository.save(record);
            log.debug("事件处理失败: {} - {}", eventId, errorMessage);
        }
    }

    /**
     * 更新事件重试状态
     */
    @Transactional
    public void updateEventRetrying(String eventId) {
        Optional<EventRecord> optional = eventRecordRepository.findByEventId(eventId);
        if (optional.isPresent()) {
            EventRecord record = optional.get();
            record.setRetrying();
            record.incrementRetryCount();
            eventRecordRepository.save(record);
            log.debug("事件重试: {} - 第{}次", eventId, record.getRetryCount());
        }
    }

    /**
     * 更新事件为死信状态
     */
    @Transactional
    public void updateEventDeadLetter(String eventId) {
        Optional<EventRecord> optional = eventRecordRepository.findByEventId(eventId);
        if (optional.isPresent()) {
            EventRecord record = optional.get();
            record.setDeadLetter();
            eventRecordRepository.save(record);
            log.warn("事件进入死信队列: {}", eventId);
        }
    }

    /**
     * 查找需要重试的事件
     */
    @Transactional(readOnly = true)
    public List<EventRecord> findRetryableEvents() {
        return eventRecordRepository.findRetryableEvents(LocalDateTime.now());
    }

    /**
     * 查找死信事件
     */
    @Transactional(readOnly = true)
    public List<EventRecord> findDeadLetterEvents() {
        return eventRecordRepository.findByStatus(EventRecord.EventStatus.DEAD_LETTER);
    }

    /**
     * 查找处理中的事件
     */
    @Transactional(readOnly = true)
    public List<EventRecord> findProcessingEvents() {
        return eventRecordRepository.findByStatus(EventRecord.EventStatus.PROCESSING);
    }

    /**
     * 查找卡住的事件（长时间未完成）
     */
    @Transactional(readOnly = true)
    public List<EventRecord> findStuckEvents(int timeoutMinutes) {
        LocalDateTime timeout = LocalDateTime.now().minusMinutes(timeoutMinutes);
        return eventRecordRepository.findStuckEvents(timeout);
    }

    /**
     * 根据事件ID查找事件记录
     */
    @Transactional(readOnly = true)
    public Optional<EventRecord> findByEventId(String eventId) {
        return eventRecordRepository.findByEventId(eventId);
    }

    /**
     * 根据事件名称查找事件记录
     */
    @Transactional(readOnly = true)
    public List<EventRecord> findByEventName(String eventName) {
        return eventRecordRepository.findByEventName(eventName);
    }

    /**
     * 根据状态查找事件记录
     */
    @Transactional(readOnly = true)
    public List<EventRecord> findByStatus(EventRecord.EventStatus status) {
        return eventRecordRepository.findByStatus(status);
    }

    /**
     * 根据时间范围查找事件记录
     */
    @Transactional(readOnly = true)
    public List<EventRecord> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return eventRecordRepository.findByTimeRange(startTime, endTime);
    }

    /**
     * 统计各状态的事件数量
     */
    @Transactional(readOnly = true)
    public List<Object[]> countByStatus() {
        return eventRecordRepository.countByStatus();
    }

    /**
     * 删除指定时间之前的事件记录
     */
    @Transactional
    public int deleteByCreatedTimeBefore(LocalDateTime beforeTime) {
        return eventRecordRepository.deleteByCreatedTimeBefore(beforeTime);
    }

    /**
     * 删除指定状态的事件记录
     */
    @Transactional
    public int deleteByStatus(EventRecord.EventStatus status) {
        return eventRecordRepository.deleteByStatus(status);
    }

    /**
     * 批量更新事件状态
     */
    @Transactional
    public int updateStatusByIds(List<Long> ids, EventRecord.EventStatus newStatus) {
        return eventRecordRepository.updateStatusByIds(ids, newStatus, LocalDateTime.now());
    }

    /**
     * 查找最近失败的事件
     */
    @Transactional(readOnly = true)
    public List<EventRecord> findRecentFailedEvents(int limit) {
        return eventRecordRepository.findRecentFailedEvents(
            org.springframework.data.domain.PageRequest.of(0, limit)
        );
    }

    /**
     * 检查事件ID是否存在
     */
    @Transactional(readOnly = true)
    public boolean existsByEventId(String eventId) {
        return eventRecordRepository.existsByEventId(eventId);
    }

    /**
     * 生成事件ID
     */
    private String generateEventId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 从事件记录恢复SignalContext
     */
    public SignalContext restoreSignalContext(EventRecord eventRecord) {
        try {
            if (eventRecord.getContextData() != null) {
                return objectMapper.readValue(eventRecord.getContextData(), SignalContext.class);
            }
        } catch (JsonProcessingException e) {
            log.error("反序列化SignalContext失败: {}", e.getMessage(), e);
        }
        return new SignalContext();
    }

    /**
     * 从事件记录恢复参数
     */
    public Object[] restoreParams(EventRecord eventRecord) {
        try {
            if (eventRecord.getParamsData() != null) {
                return objectMapper.readValue(eventRecord.getParamsData(), Object[].class);
            }
        } catch (JsonProcessingException e) {
            log.error("反序列化参数失败: {}", e.getMessage(), e);
        }
        return new Object[0];
    }

    /**
     * 从事件记录恢复SigHandler
     */
    public SigHandler restoreSigHandler(EventRecord eventRecord) {
        try {
            if (eventRecord.getHandlerInfo() != null) {
                return objectMapper.readValue(eventRecord.getHandlerInfo(), SigHandler.class);
            }
        } catch (JsonProcessingException e) {
            log.error("反序列化SigHandler失败: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * 从事件记录恢复SignalConfig
     */
    public SignalConfig restoreSignalConfig(EventRecord eventRecord) {
        try {
            if (eventRecord.getConfigInfo() != null) {
                return objectMapper.readValue(eventRecord.getConfigInfo(), SignalConfig.class);
            }
        } catch (JsonProcessingException e) {
            log.error("反序列化SignalConfig失败: {}", e.getMessage(), e);
        }
        return new SignalConfig.Builder().build();
    }
}
