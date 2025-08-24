package com.hibiscus.signal.spring.configuration;

import com.hibiscus.signal.spring.config.SignalProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * MQ配置类
 * 提供RabbitMQ和Kafka配置
 * 只有在明确启用MQ且存在相关类时才加载
 * 
 * @author heathcetide
 */
@Configuration
@ConditionalOnProperty(name = "hibiscus.mq.enabled", havingValue = "true", matchIfMissing = false)
public class SignalMqConfiguration {
    
    private static final Logger log = LoggerFactory.getLogger(SignalMqConfiguration.class);
    
    private final SignalProperties signalProperties;
    
    public SignalMqConfiguration(SignalProperties signalProperties) {
        this.signalProperties = signalProperties;
        log.info("MQ配置已启用，类型: {}", signalProperties.getMqType());
    }
    
    /**
     * RabbitMQ配置
     */
    @Configuration
    @ConditionalOnClass({RabbitTemplate.class, ConnectionFactory.class})
    @ConditionalOnProperty(name = "hibiscus.mq.type", havingValue = "rabbitmq")
    static class RabbitMQConfiguration {
        
        private final SignalProperties signalProperties;
        
        public RabbitMQConfiguration(SignalProperties signalProperties) {
            this.signalProperties = signalProperties;
            log.info("RabbitMQ配置已启用，连接地址: {}:{}", 
                signalProperties.getMqHost(), signalProperties.getMqPort());
        }
        
        /**
         * 信号事件交换机
         */
        @Bean
        public DirectExchange signalEventsExchange() {
            return new DirectExchange("signal.events", true, false);
        }
        
        /**
         * 死信交换机
         */
        @Bean
        public DirectExchange deadLetterExchange() {
            return new DirectExchange("signal.dead.letter", true, false);
        }
        
        /**
         * 重试交换机
         */
        @Bean
        public DirectExchange retryExchange() {
            return new DirectExchange("signal.retry", true, false);
        }
        
        /**
         * 死信队列
         */
        @Bean
        public Queue deadLetterQueue() {
            return new Queue("signal.dead.letter.queue", true);
        }
        
        /**
         * 重试队列
         */
        @Bean
        public Queue retryQueue() {
            Map<String, Object> args = new HashMap<>();
            args.put("x-message-ttl", 60000); // 1分钟后重试
            args.put("x-dead-letter-exchange", "signal.events");
            args.put("x-dead-letter-routing-key", "retry");
            
            return new Queue("signal.retry.queue", true, false, false, args);
        }
        
        /**
         * 死信队列绑定
         */
        @Bean
        public Binding deadLetterBinding() {
            return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with("dead.letter");
        }
        
        /**
         * 重试队列绑定
         */
        @Bean
        public Binding retryBinding() {
            return BindingBuilder.bind(retryQueue())
                .to(retryExchange())
                .with("retry");
        }
        
        /**
         * RabbitTemplate配置
         */
        @Bean
        public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
            RabbitTemplate template = new RabbitTemplate(connectionFactory);
            template.setMessageConverter(new Jackson2JsonMessageConverter());
            
            // 启用确认机制
            template.setConfirmCallback((correlationData, ack, cause) -> {
                if (ack) {
                    log.debug("RabbitMQ消息发送成功: {}", correlationData);
                } else {
                    log.error("RabbitMQ消息发送失败: {}, 原因: {}", correlationData, cause);
                }
            });
            
            // 启用返回机制
            template.setReturnsCallback(returned -> {
                log.warn("RabbitMQ消息被退回: exchange={}, routingKey={}, replyCode={}, replyText={}",
                    returned.getExchange(), returned.getRoutingKey(), 
                    returned.getReplyCode(), returned.getReplyText());
            });
            
            log.info("RabbitTemplate配置完成");
            return template;
        }
    }
    
    /**
     * Kafka配置
     */
    @Configuration
    @ConditionalOnClass({KafkaTemplate.class, ProducerFactory.class})
    @ConditionalOnProperty(name = "hibiscus.mq.type", havingValue = "kafka")
    static class KafkaConfiguration {
        
        private final SignalProperties signalProperties;
        
        public KafkaConfiguration(SignalProperties signalProperties) {
            this.signalProperties = signalProperties;
            log.info("Kafka配置已启用，连接地址: {}:{}", 
                signalProperties.getMqHost(), signalProperties.getMqPort());
        }
        
        /**
         * Kafka生产者工厂
         */
        @Bean
        public ProducerFactory<String, String> producerFactory() {
            Map<String, Object> configProps = new HashMap<>();
            configProps.put("bootstrap.servers", signalProperties.getMqHost() + ":" + signalProperties.getMqPort());
            configProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            configProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            configProps.put("acks", "all");
            configProps.put("retries", 3);
            configProps.put("batch.size", 16384);
            configProps.put("linger.ms", 1);
            configProps.put("buffer.memory", 33554432);
            
            log.info("Kafka生产者配置: {}:{}", signalProperties.getMqHost(), signalProperties.getMqPort());
            
            return new DefaultKafkaProducerFactory<>(configProps);
        }
        
        /**
         * Kafka模板
         */
        @Bean
        public KafkaTemplate<String, String> kafkaTemplate() {
            KafkaTemplate<String, String> template = new KafkaTemplate<>(producerFactory());
            
            // 设置默认主题
            template.setDefaultTopic("signal-events");
            
            log.info("Kafka模板配置完成");
            return template;
        }
    }
    
    /**
     * 通用MQ配置
     */
    @Bean
    @ConditionalOnProperty(name = "hibiscus.mq.enabled", havingValue = "true")
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
