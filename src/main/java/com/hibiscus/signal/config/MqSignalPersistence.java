package com.hibiscus.signal.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hibiscus.signal.core.SignalPersistenceInfo;
import com.hibiscus.signal.spring.config.SignalProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * MQ信号持久化服务
 * 提供基于消息队列的事件发布功能
 * 
 * @author heathcetide
 */
@Service
@ConditionalOnProperty(name = "hibiscus.mq.enabled", havingValue = "true", matchIfMissing = false)
public class MqSignalPersistence {
    
    private static final Logger log = LoggerFactory.getLogger(MqSignalPersistence.class);
    
    @Autowired
    private SignalProperties signalProperties;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // RabbitMQ相关
    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;
    
    // Kafka相关
    @Autowired(required = false)
    private KafkaTemplate<String, String> kafkaTemplate;
    
    // 默认交换机名称
    private static final String DEFAULT_EXCHANGE = "signal.events";
    private static final String DEFAULT_TOPIC = "signal-events";
    
    /**
     * 发布事件到消息队列
     */
    public void publishEvent(SignalPersistenceInfo info) {
        try {
            String eventName = info.getSigHandler().getSignalName();
            String eventJson = objectMapper.writeValueAsString(info);
            
            // 根据配置的MQ类型选择发布方式
            switch (signalProperties.getMqType().toLowerCase()) {
                case "rabbitmq":
                    publishToRabbitMQ(eventName, eventJson);
                    break;
                case "kafka":
                    publishToKafka(eventName, eventJson);
                    break;
                default:
                    log.warn("不支持的MQ类型: {}", signalProperties.getMqType());
            }
            
            log.debug("事件已发布到MQ: {} - {}", signalProperties.getMqType(), eventName);
            
        } catch (JsonProcessingException e) {
            log.error("序列化事件数据失败: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("发布事件到MQ失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 发布事件到指定主题
     */
    public void publishEventToTopic(SignalPersistenceInfo info, String topic) {
        try {
            String eventJson = objectMapper.writeValueAsString(info);
            
            switch (signalProperties.getMqType().toLowerCase()) {
                case "rabbitmq":
                    publishToRabbitMQ(topic, eventJson);
                    break;
                case "kafka":
                    publishToKafka(topic, eventJson);
                    break;
                default:
                    log.warn("不支持的MQ类型: {}", signalProperties.getMqType());
            }
            
            log.debug("事件已发布到指定主题: {} - {}", topic, signalProperties.getMqType());
            
        } catch (JsonProcessingException e) {
            log.error("序列化事件数据失败: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("发布事件到指定主题失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 发布事件到指定队列
     */
    public void publishEventToQueue(SignalPersistenceInfo info, String queueName) {
        try {
            String eventJson = objectMapper.writeValueAsString(info);
            
            if ("rabbitmq".equals(signalProperties.getMqType().toLowerCase())) {
                // RabbitMQ直接发送到队列
                rabbitTemplate.convertAndSend("", queueName, eventJson);
                log.debug("事件已发布到RabbitMQ队列: {}", queueName);
            } else {
                log.warn("当前MQ类型不支持直接队列发布: {}", signalProperties.getMqType());
            }
            
        } catch (JsonProcessingException e) {
            log.error("序列化事件数据失败: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("发布事件到指定队列失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 批量发布事件
     */
    public void publishEvents(List<SignalPersistenceInfo> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        
        // 异步批量发布
        CompletableFuture.runAsync(() -> {
            for (SignalPersistenceInfo event : events) {
                try {
                    publishEvent(event);
                    // 添加小延迟避免消息堆积
                    Thread.sleep(10);
                } catch (Exception e) {
                    log.error("批量发布事件失败: {}", e.getMessage(), e);
                }
            }
            log.info("批量发布完成，共发布 {} 个事件", events.size());
        });
    }
    
    /**
     * 发布到RabbitMQ
     */
    private void publishToRabbitMQ(String routingKey, String message) {
        if (rabbitTemplate == null) {
            log.warn("RabbitTemplate未配置，无法发布消息");
            return;
        }
        
        try {
            // 发送到默认交换机，使用事件名称作为路由键
            rabbitTemplate.convertAndSend(DEFAULT_EXCHANGE, routingKey, message);
            
            // 确认消息发送
            if (rabbitTemplate.isConfirmListener()) {
                log.debug("RabbitMQ消息发送确认: {}", routingKey);
            }
            
        } catch (Exception e) {
            log.error("发布到RabbitMQ失败: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 发布到Kafka
     */
    private void publishToKafka(String topic, String message) {
        if (kafkaTemplate == null) {
            log.warn("KafkaTemplate未配置，无法发布消息");
            return;
        }
        
        try {
            // 使用事件名称作为消息key，确保相同事件类型的消息进入相同分区
            String key = topic;
            kafkaTemplate.send(topic, key, message);
            
            log.debug("Kafka消息发送成功: topic={}, key={}", topic, key);
            
        } catch (Exception e) {
            log.error("发布到Kafka失败: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 异步发布事件
     */
    public void publishEventAsync(SignalPersistenceInfo info) {
        CompletableFuture.runAsync(() -> {
            try {
                publishEvent(info);
            } catch (Exception e) {
                log.error("异步发布事件失败: {}", e.getMessage(), e);
            }
        });
    }
    
            /**
         * 发布事件到死信队列
         */
        public void publishToDeadLetterQueue(SignalPersistenceInfo info, String reason) {
            try {
                // 添加失败原因到事件信息
                info.getSignalContext().setAttribute("failureReason", reason);
                info.getSignalContext().setAttribute("deadLetterTime", System.currentTimeMillis());
                
                String deadLetterTopic = "signal.dead.letter";
                publishEventToTopic(info, deadLetterTopic);
                
                log.warn("事件已发送到死信队列: {} - 原因: {}", 
                    info.getSigHandler().getSignalName(), reason);
                
            } catch (Exception e) {
                log.error("发送到死信队列失败: {}", e.getMessage(), e);
            }
        }
        
        /**
         * 发布事件到重试队列
         */
        public void publishToRetryQueue(SignalPersistenceInfo info, int retryCount) {
            try {
                // 添加重试信息
                info.getSignalContext().setAttribute("retryCount", retryCount);
                info.getSignalContext().setAttribute("retryTime", System.currentTimeMillis());
                
                String retryTopic = "signal.retry";
                publishEventToTopic(info, retryTopic);
                
                log.info("事件已发送到重试队列: {} - 重试次数: {}", 
                    info.getSigHandler().getSignalName(), retryCount);
                
            } catch (Exception e) {
                log.error("发送到重试队列失败: {}", e.getMessage(), e);
            }
        }
    
    /**
     * 检查MQ连接状态
     */
    public boolean isConnected() {
        try {
            switch (signalProperties.getMqType().toLowerCase()) {
                case "rabbitmq":
                    return rabbitTemplate != null && rabbitTemplate.getConnectionFactory() != null;
                case "kafka":
                    return kafkaTemplate != null;
                default:
                    return false;
            }
        } catch (Exception e) {
            log.error("检查MQ连接状态失败: {}", e.getMessage(), e);
            return false;
        }
    }
}
