package com.hibiscus.signal.spring.configuration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.hibiscus.signal.spring.config.SignalProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis配置类
 * 提供Redis连接和序列化配置
 * 只有在明确启用Redis且存在Redis相关类时才加载
 * 
 * @author heathcetide
 */
@Configuration
@ConditionalOnClass({RedisTemplate.class, LettuceConnectionFactory.class})
@ConditionalOnProperty(name = "hibiscus.redis.enabled", havingValue = "true", matchIfMissing = false)
public class SignalRedisConfiguration {
    
    private static final Logger log = LoggerFactory.getLogger(SignalRedisConfiguration.class);
    
    private final SignalProperties signalProperties;
    
    public SignalRedisConfiguration(SignalProperties signalProperties) {
        this.signalProperties = signalProperties;
        log.info("Redis配置已启用，连接地址: {}:{}", 
            signalProperties.getRedisHost(), signalProperties.getRedisPort());
    }
    
    /**
     * Redis连接工厂
     */
    @Bean
    @ConditionalOnProperty(name = "hibiscus.redis.enabled", havingValue = "true")
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(signalProperties.getRedisHost());
        config.setPort(signalProperties.getRedisPort());
        config.setDatabase(signalProperties.getRedisDatabase());
        
        if (signalProperties.getRedisPassword() != null && !signalProperties.getRedisPassword().isEmpty()) {
            config.setPassword(signalProperties.getRedisPassword());
        }
        
        log.info("Redis连接配置: {}:{}, database: {}", 
            signalProperties.getRedisHost(), 
            signalProperties.getRedisPort(), 
            signalProperties.getRedisDatabase());
        
        return new LettuceConnectionFactory(config);
    }
    
    /**
     * Redis模板配置
     */
    @Bean
    @ConditionalOnProperty(name = "hibiscus.redis.enabled", havingValue = "true")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 配置序列化器
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper objectMapper = new ObjectMapper();
        
        // 设置可见性
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        
        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);
        
        // 设置key和value的序列化规则
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
        
        // 初始化RedisTemplate
        template.afterPropertiesSet();
        
        log.info("Redis模板配置完成");
        
        return template;
    }
    
    /**
     * ObjectMapper Bean - 仅在Redis启用时提供
     */
    @Bean("redisObjectMapper")
    @ConditionalOnProperty(name = "hibiscus.redis.enabled", havingValue = "true")
    public ObjectMapper redisObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        return objectMapper;
    }
}
