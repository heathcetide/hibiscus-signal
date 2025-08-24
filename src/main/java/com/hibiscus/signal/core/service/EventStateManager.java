package com.hibiscus.signal.core.service;

import com.hibiscus.signal.config.DatabaseSignalPersistence;
import com.hibiscus.signal.config.SignalConfig;
import com.hibiscus.signal.core.SignalContext;
import com.hibiscus.signal.core.SigHandler;
import com.hibiscus.signal.core.entity.EventRecord;
import com.hibiscus.signal.spring.config.SignalProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 事件状态管理器
 * 统一管理事件的持久化和状态跟踪
 */
@Service
@ConditionalOnProperty(name = "hibiscus.databasePersistent", havingValue = "true")
public class EventStateManager {

    private static final Logger log = LoggerFactory.getLogger(EventStateManager.class);

    @Autowired
    private DatabaseSignalPersistence databasePersistence;

    @Autowired
    private SignalProperties signalProperties;

    // 内存中的事件状态缓存
    private final ConcurrentHashMap<String, EventStateInfo> eventStateCache = new ConcurrentHashMap<>();
    private final AtomicLong eventCounter = new AtomicLong(0);

    /**
     * 记录事件开始处理
     */
    public void recordEventStart(SigHandler sigHandler, SignalConfig config, 
                               SignalContext context, Object... params) {
        String eventId = generateEventId();
        
        // 创建事件状态信息
        EventStateInfo stateInfo = new EventStateInfo(eventId, sigHandler.getSignalName());
        stateInfo.setStatus(EventRecord.EventStatus.PROCESSING);
        stateInfo.setStartTime(System.currentTimeMillis());
        
        // 保存到缓存
        eventStateCache.put(eventId, stateInfo);
        
        // 数据库持久化
        if (signalProperties.getDatabasePersistent()) {
            EventRecord record = databasePersistence.saveEventRecord(sigHandler, config, context, params);
            stateInfo.setDatabaseRecord(record);
            databasePersistence.updateEventProcessing(eventId);
        }
        
        log.debug("事件开始处理: {} - {}", eventId, sigHandler.getSignalName());
    }

    /**
     * 记录事件处理成功
     */
    public void recordEventSuccess(String eventId) {
        EventStateInfo stateInfo = eventStateCache.get(eventId);
        if (stateInfo != null) {
            stateInfo.setStatus(EventRecord.EventStatus.SUCCESS);
            stateInfo.setEndTime(System.currentTimeMillis());
            
            if (signalProperties.getDatabasePersistent() && stateInfo.getDatabaseRecord() != null) {
                databasePersistence.updateEventSuccess(eventId);
            }
            
            log.debug("事件处理成功: {}", eventId);
        }
    }

    /**
     * 记录事件处理失败
     */
    public void recordEventFailed(String eventId, String errorMessage, String errorStack) {
        EventStateInfo stateInfo = eventStateCache.get(eventId);
        if (stateInfo != null) {
            stateInfo.setStatus(EventRecord.EventStatus.FAILED);
            stateInfo.setEndTime(System.currentTimeMillis());
            stateInfo.setErrorMessage(errorMessage);
            stateInfo.setErrorStack(errorStack);
            
            if (signalProperties.getDatabasePersistent() && stateInfo.getDatabaseRecord() != null) {
                databasePersistence.updateEventFailed(eventId, errorMessage, errorStack);
            }
            
            log.debug("事件处理失败: {} - {}", eventId, errorMessage);
        }
    }

    /**
     * 记录事件重试
     */
    public void recordEventRetry(String eventId) {
        EventStateInfo stateInfo = eventStateCache.get(eventId);
        if (stateInfo != null) {
            stateInfo.setStatus(EventRecord.EventStatus.RETRYING);
            stateInfo.incrementRetryCount();
            
            if (signalProperties.getDatabasePersistent() && stateInfo.getDatabaseRecord() != null) {
                databasePersistence.updateEventRetrying(eventId);
            }
            
            log.debug("事件重试: {} - 第{}次", eventId, stateInfo.getRetryCount());
        }
    }

    /**
     * 记录事件进入死信队列
     */
    public void recordEventDeadLetter(String eventId) {
        EventStateInfo stateInfo = eventStateCache.get(eventId);
        if (stateInfo != null) {
            stateInfo.setStatus(EventRecord.EventStatus.DEAD_LETTER);
            
            if (signalProperties.getDatabasePersistent() && stateInfo.getDatabaseRecord() != null) {
                databasePersistence.updateEventDeadLetter(eventId);
            }
            
            log.warn("事件进入死信队列: {}", eventId);
        }
    }

    /**
     * 获取事件状态信息
     */
    public EventStateInfo getEventState(String eventId) {
        return eventStateCache.get(eventId);
    }

    /**
     * 获取所有事件状态
     */
    public Map<String, EventStateInfo> getAllEventStates() {
        return new ConcurrentHashMap<>(eventStateCache);
    }

    /**
     * 从数据库恢复事件状态
     */
    public void recoverEventStatesFromDatabase() {
        if (!signalProperties.getDatabasePersistent()) {
            return;
        }
        
        try {
            log.info("开始从数据库恢复事件状态...");
            
            List<EventRecord> processingEvents = databasePersistence.findProcessingEvents();
            for (EventRecord record : processingEvents) {
                EventStateInfo stateInfo = new EventStateInfo(record.getEventId(), record.getEventName());
                stateInfo.setStatus(record.getStatus());
                stateInfo.setDatabaseRecord(record);
                eventStateCache.put(record.getEventId(), stateInfo);
            }
            
            log.info("从数据库恢复了 {} 个事件状态", eventStateCache.size());
            
        } catch (Exception e) {
            log.error("从数据库恢复事件状态失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 定时任务：清理过期的事件状态
     */
    @Scheduled(fixedDelay = 300000) // 每5分钟执行一次
    public void scheduledCleanup() {
        try {
            if (signalProperties.getDatabasePersistent()) {
                LocalDateTime beforeTime = LocalDateTime.now().minusDays(7);
                int deletedCount = databasePersistence.deleteByCreatedTimeBefore(beforeTime);
                if (deletedCount > 0) {
                    log.info("清理了 {} 条过期的数据库记录", deletedCount);
                }
            }
        } catch (Exception e) {
            log.error("定时清理事件状态失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 生成事件ID
     */
    private String generateEventId() {
        return "EVT_" + System.currentTimeMillis() + "_" + eventCounter.incrementAndGet();
    }

    /**
     * 事件状态信息
     */
    public static class EventStateInfo {
        private final String eventId;
        private final String eventName;
        private EventRecord.EventStatus status;
        private long startTime;
        private long endTime;
        private int retryCount = 0;
        private String errorMessage;
        private String errorStack;
        private EventRecord databaseRecord;

        public EventStateInfo(String eventId, String eventName) {
            this.eventId = eventId;
            this.eventName = eventName;
            this.status = EventRecord.EventStatus.PENDING;
        }

        // Getters and Setters
        public String getEventId() { return eventId; }
        public String getEventName() { return eventName; }
        
        public EventRecord.EventStatus getStatus() { return status; }
        public void setStatus(EventRecord.EventStatus status) { this.status = status; }
        
        public long getStartTime() { return startTime; }
        public void setStartTime(long startTime) { this.startTime = startTime; }
        
        public long getEndTime() { return endTime; }
        public void setEndTime(long endTime) { this.endTime = endTime; }
        
        public int getRetryCount() { return retryCount; }
        public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
        public void incrementRetryCount() { this.retryCount++; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public String getErrorStack() { return errorStack; }
        public void setErrorStack(String errorStack) { this.errorStack = errorStack; }
        
        public EventRecord getDatabaseRecord() { return databaseRecord; }
        public void setDatabaseRecord(EventRecord databaseRecord) { this.databaseRecord = databaseRecord; }
        
        public long getDuration() {
            if (startTime > 0 && endTime > 0) {
                return endTime - startTime;
            }
            return 0;
        }

        @Override
        public String toString() {
            return "EventStateInfo{" +
                    "eventId='" + eventId + '\'' +
                    ", eventName='" + eventName + '\'' +
                    ", status=" + status +
                    ", retryCount=" + retryCount +
                    ", duration=" + getDuration() + "ms" +
                    '}';
        }
    }
}
