package com.hibiscus.signal.spring.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 信号数据库配置
 * 启用JPA和事务管理
 */
@Configuration
@EntityScan(basePackages = "com.hibiscus.signal.core.entity")
@EnableJpaRepositories(basePackages = "com.hibiscus.signal.core.repository")
@EnableTransactionManagement
@ConditionalOnProperty(name = "hibiscus.databasePersistent", havingValue = "true")
public class SignalDatabaseConfiguration {

    /**
     * 配置ObjectMapper，支持LocalDateTime序列化
     */
    @Bean
    public ObjectMapper signalObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }
}
