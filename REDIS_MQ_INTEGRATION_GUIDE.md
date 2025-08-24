# Hibiscus Signal Redis & MQ 集成配置指南

## 📋 概述

本文档详细介绍如何在 Hibiscus Signal 框架中集成 Redis 和消息队列（RabbitMQ/Kafka），实现高性能的事件存储和异步处理。

## 🚀 快速开始

### 1. 添加依赖

在您的项目中添加相应的依赖：

#### Redis 集成
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

#### RabbitMQ 集成
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

#### Kafka 集成
```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

### 2. 配置示例

#### 仅使用 Redis
```yaml
hibiscus:
  # 启用Redis
  redis:
    enabled: true
    host: "localhost"
    port: 6379
    password: ""
    database: 0
    expireSeconds: 86400  # 24小时
  
  # 持久化策略
  persistenceStrategy: "redis"
```

#### 仅使用 RabbitMQ
```yaml
hibiscus:
  # 启用MQ
  mq:
    enabled: true
    type: "rabbitmq"
    host: "localhost"
    port: 5672
    username: "guest"
    password: "guest"
    virtualHost: "/"
  
  # 持久化策略
  persistenceStrategy: "mq"
```

#### 仅使用 Kafka
```yaml
hibiscus:
  # 启用MQ
  mq:
    enabled: true
    type: "kafka"
    host: "localhost"
    port: 9092
  
  # 持久化策略
  persistenceStrategy: "mq"
```

#### 组合使用（推荐）
```yaml
hibiscus:
  # 数据库持久化
  databasePersistent: true
  databaseTableName: "signal_events"
  databaseRetentionDays: 30
  
  # Redis缓存
  redis:
    enabled: true
    host: "localhost"
    port: 6379
    expireSeconds: 3600  # 1小时
  
  # MQ异步处理
  mq:
    enabled: true
    type: "rabbitmq"
    host: "localhost"
    port: 5672
  
  # 持久化策略
  persistenceStrategy: "database_redis_mq"
```

## 🔧 详细配置

### Redis 配置

#### 基础配置
```yaml
hibiscus:
  redis:
    enabled: true                    # 启用Redis
    host: "localhost"                # Redis主机
    port: 6379                       # Redis端口
    password: ""                     # Redis密码（可选）
    database: 0                      # 数据库编号
    expireSeconds: 86400             # 数据过期时间（秒）
```

#### 集群配置
```yaml
hibiscus:
  redis:
    enabled: true
    host: "redis-cluster"
    port: 6379
    # 集群模式会自动使用Redis集群配置
```

#### 哨兵配置
```yaml
spring:
  redis:
    sentinel:
      master: "mymaster"
      nodes: "localhost:26379,localhost:26380"
```

### RabbitMQ 配置

#### 基础配置
```yaml
hibiscus:
  mq:
    enabled: true
    type: "rabbitmq"
    host: "localhost"
    port: 5672
    username: "guest"
    password: "guest"
    virtualHost: "/"
```

#### 集群配置
```yaml
hibiscus:
  mq:
    enabled: true
    type: "rabbitmq"
    host: "rabbitmq-cluster"
    port: 5672
    username: "admin"
    password: "admin123"
```

#### Spring Boot RabbitMQ 配置
```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /
    
    # 连接池配置
    connection-timeout: 60000
    requested-heart-beat: 60
    
    # 发布确认
    publisher-confirm-type: correlated
    publisher-returns: true
```

### Kafka 配置

#### 基础配置
```yaml
hibiscus:
  mq:
    enabled: true
    type: "kafka"
    host: "localhost"
    port: 9092
```

#### 集群配置
```yaml
hibiscus:
  mq:
    enabled: true
    type: "kafka"
    host: "kafka-cluster"
    port: 9092
```

#### Spring Boot Kafka 配置
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      acks: all
      retries: 3
      batch-size: 16384
      linger-ms: 1
      buffer-memory: 33554432
```

## 📊 存储策略

### 1. 仅 Redis 存储
```yaml
hibiscus:
  persistenceStrategy: "redis"
  redis:
    enabled: true
    host: "localhost"
    port: 6379
```

**特点**：
- 高性能缓存
- 数据不持久化
- 适合实时查询
- 内存占用较高

### 2. 仅 MQ 存储
```yaml
hibiscus:
  persistenceStrategy: "mq"
  mq:
    enabled: true
    type: "rabbitmq"
    host: "localhost"
    port: 5672
```

**特点**：
- 异步处理
- 消息可靠性
- 支持重试机制
- 适合解耦场景

### 3. 数据库 + Redis
```yaml
hibiscus:
  persistenceStrategy: "database_redis"
  databasePersistent: true
  redis:
    enabled: true
    host: "localhost"
    port: 6379
```

**特点**：
- 数据持久化
- 高性能缓存
- 查询优先从Redis获取
- 适合读多写少场景

### 4. 数据库 + MQ
```yaml
hibiscus:
  persistenceStrategy: "database_mq"
  databasePersistent: true
  mq:
    enabled: true
    type: "rabbitmq"
    host: "localhost"
    port: 5672
```

**特点**：
- 数据持久化
- 异步处理
- 支持跨服务通信
- 适合微服务架构

### 5. 完整方案
```yaml
hibiscus:
  persistenceStrategy: "all"
  databasePersistent: true
  redis:
    enabled: true
    host: "localhost"
    port: 6379
  mq:
    enabled: true
    type: "rabbitmq"
    host: "localhost"
    port: 5672
```

**特点**：
- 数据持久化
- 高性能缓存
- 异步处理
- 适合企业级应用

## 🔄 使用示例

### 1. 基本使用
```java
@Service
public class OrderService {
    
    @Autowired
    private Signals signals;
    
    public void createOrder(Order order) {
        // 创建订单
        orderRepository.save(order);
        
        // 发送事件（会自动存储到Redis/MQ）
        signals.emit("order.created", this, order);
    }
}
```

### 2. 查询事件
```java
@Service
public class EventQueryService {
    
    @Autowired
    private UnifiedSignalPersistence persistence;
    
    public SignalPersistenceInfo getEvent(String eventId) {
        // 优先从Redis查询，如果不存在则从数据库查询
        return persistence.queryEvent(eventId);
    }
    
    public List<SignalPersistenceInfo> getEventsByType(String eventName, int limit) {
        return persistence.queryEvents(eventName, limit);
    }
}
```

### 3. 处理失败事件
```java
@Service
public class DeadLetterHandler {
    
    @Autowired
    private MqSignalPersistence mqPersistence;
    
    public void handleFailedEvent(SignalPersistenceInfo event, String reason) {
        // 发送到死信队列
        mqPersistence.publishToDeadLetterQueue(event, reason);
    }
    
    public void retryEvent(SignalPersistenceInfo event, int retryCount) {
        // 发送到重试队列
        mqPersistence.publishToRetryQueue(event, retryCount);
    }
}
```

## 🛠️ 监控和运维

### 1. Redis 监控
```java
@Component
public class RedisHealthIndicator {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    public boolean isHealthy() {
        try {
            redisTemplate.opsForValue().get("health_check");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

### 2. MQ 监控
```java
@Component
public class MqHealthIndicator {
    
    @Autowired
    private MqSignalPersistence mqPersistence;
    
    public boolean isHealthy() {
        return mqPersistence.isConnected();
    }
}
```

### 3. 数据清理
```java
@Component
@Scheduled(fixedRate = 3600000) // 每小时执行一次
public class DataCleanupTask {
    
    @Autowired
    private UnifiedSignalPersistence persistence;
    
    public void cleanupExpiredData() {
        persistence.cleanupExpiredData();
    }
}
```

## 🚨 故障处理

### 1. Redis 连接失败
```yaml
# 启用降级策略
hibiscus:
  redis:
    enabled: true
    host: "localhost"
    port: 6379
    # 连接失败时会自动降级到数据库存储
```

### 2. MQ 连接失败
```yaml
# 启用本地缓存
hibiscus:
  mq:
    enabled: true
    type: "rabbitmq"
    host: "localhost"
    port: 5672
    # 连接失败时会使用本地缓存
```

### 3. 数据不一致处理
```java
@Component
public class DataConsistencyChecker {
    
    @Autowired
    private UnifiedSignalPersistence persistence;
    
    @Scheduled(fixedRate = 300000) // 每5分钟检查一次
    public void checkDataConsistency() {
        // 检查Redis和数据库数据一致性
        // 如果不一致，进行数据同步
    }
}
```

## 📈 性能优化

### 1. Redis 优化
```yaml
hibiscus:
  redis:
    enabled: true
    host: "localhost"
    port: 6379
    expireSeconds: 3600  # 根据业务需求设置合适的过期时间
```

### 2. MQ 优化
```yaml
spring:
  rabbitmq:
    # 批量发送
    batch-size: 100
    # 连接池
    connection-timeout: 60000
    requested-heart-beat: 60
```

### 3. 异步处理
```java
@Configuration
public class AsyncConfig {
    
    @Bean
    public ExecutorService persistenceExecutor() {
        return Executors.newFixedThreadPool(10);
    }
}
```

## 🔐 安全配置

### 1. Redis 安全
```yaml
hibiscus:
  redis:
    enabled: true
    host: "localhost"
    port: 6379
    password: "your_secure_password"
    # 使用SSL连接
    ssl: true
```

### 2. MQ 安全
```yaml
hibiscus:
  mq:
    enabled: true
    type: "rabbitmq"
    host: "localhost"
    port: 5672
    username: "secure_user"
    password: "secure_password"
    # 使用SSL连接
    ssl: true
```

## 📝 最佳实践

### 1. 选择合适的存储策略
- 开发环境：使用文件存储
- 测试环境：使用数据库存储
- 生产环境：根据业务需求选择组合策略

### 2. 监控和告警
- 监控Redis内存使用情况
- 监控MQ消息堆积情况
- 设置合理的告警阈值

### 3. 数据备份
- 定期备份数据库数据
- 配置Redis持久化
- 监控MQ消息丢失

### 4. 性能调优
- 根据业务特点调整Redis过期时间
- 优化MQ批量发送参数
- 使用连接池提高性能

## 🎯 总结

通过集成 Redis 和 MQ，Hibiscus Signal 框架能够提供：

1. **高性能缓存**：Redis提供毫秒级查询
2. **异步处理**：MQ提供消息队列能力
3. **数据持久化**：数据库保证数据安全
4. **灵活配置**：支持多种存储策略组合
5. **企业级特性**：死信队列、重试机制、监控告警

选择合适的存储策略，能够显著提升系统性能和可靠性。
