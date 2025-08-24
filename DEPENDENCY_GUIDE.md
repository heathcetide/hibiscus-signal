# Hibiscus Signal 依赖配置指南

## 概述

Hibiscus Signal 框架的 Redis 和 MQ 功能采用可选依赖设计，确保其他项目引入时不会因为缺少配置而导致启动失败。

## 依赖设计原则

### 1. 可选依赖
- Redis 和 MQ 相关依赖设置为 `<optional>true</optional>`
- 只有在明确需要时才传递依赖
- 避免强制引入不必要的依赖

### 2. 条件化配置
- 使用 `@ConditionalOnClass` 检查类是否存在
- 使用 `@ConditionalOnProperty` 检查配置是否启用
- 使用 `matchIfMissing = false` 确保默认不启用

## 使用场景

### 场景1：仅使用核心功能
```xml
<dependency>
    <groupId>io.github.heathcetide</groupId>
    <artifactId>cetide.hibiscus.signal</artifactId>
    <version>1.0.6</version>
</dependency>
```

配置：
```yaml
hibiscus:
  persistent: true
  persistenceFile: "signal.json"
```

### 场景2：使用Redis
```xml
<dependency>
    <groupId>io.github.heathcetide</groupId>
    <artifactId>cetide.hibiscus.signal</artifactId>
    <version>1.0.6</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

配置：
```yaml
hibiscus:
  redis:
    enabled: true
    host: "localhost"
    port: 6379
```

### 场景3：使用RabbitMQ
```xml
<dependency>
    <groupId>io.github.heathcetide</groupId>
    <artifactId>cetide.hibiscus.signal</artifactId>
    <version>1.0.6</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

配置：
```yaml
hibiscus:
  mq:
    enabled: true
    type: "rabbitmq"
    host: "localhost"
    port: 5672
```

## 条件化配置

### Redis 配置
```java
@ConditionalOnClass({RedisTemplate.class, LettuceConnectionFactory.class})
@ConditionalOnProperty(name = "hibiscus.redis.enabled", havingValue = "true", matchIfMissing = false)
```

### MQ 配置
```java
@ConditionalOnProperty(name = "hibiscus.mq.enabled", havingValue = "true", matchIfMissing = false)
@ConditionalOnClass({RabbitTemplate.class, ConnectionFactory.class})
```

## 最佳实践

1. **开发环境**：使用文件存储，不引入额外依赖
2. **测试环境**：根据需要选择性引入依赖
3. **生产环境**：引入完整依赖，使用集群配置
4. **微服务**：根据服务特点选择存储策略

## 常见问题

### 问题1：Redis连接错误
**解决**：添加Redis依赖
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### 问题2：RabbitMQ连接错误
**解决**：添加RabbitMQ依赖
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

### 问题3：功能不生效
**解决**：确保配置中明确启用
```yaml
hibiscus:
  redis:
    enabled: true
  mq:
    enabled: true
```

## 总结

这种设计确保了：
- 最小化依赖
- 按需加载
- 优雅降级
- 灵活配置
- 生产就绪
