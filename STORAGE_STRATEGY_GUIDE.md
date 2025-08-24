# Hibiscus Signal 存储策略配置指南

## 📋 概述

Hibiscus Signal 框架支持多种存储策略，可以根据不同的业务场景选择合适的存储方式或组合使用。

## 🎯 存储策略选择

### 1. **仅文件存储** (`FILE_ONLY`)
```yaml
hibiscus:
  persistenceStrategy: "file"
  persistent: true
  persistenceFile: "signal.json"
  enableFileRotation: true
  maxFileSizeBytes: 10485760  # 10MB
```

**适用场景**：
- 单机应用
- 开发测试环境
- 简单的日志记录需求

### 2. **仅数据库存储** (`DATABASE_ONLY`)
```yaml
hibiscus:
  persistenceStrategy: "database"
  databasePersistent: true
  databaseTableName: "signal_events"
  databaseRetentionDays: 30
  enableDatabaseCleanup: true
```

**适用场景**：
- 需要数据持久化
- 需要复杂查询
- 数据一致性要求高

### 3. **仅Redis存储** (`REDIS_ONLY`)
```yaml
hibiscus:
  persistenceStrategy: "redis"
  redis:
    enabled: true
    host: "localhost"
    port: 6379
    password: ""
    database: 0
    expireSeconds: 86400  # 24小时
```

**适用场景**：
- 高性能缓存需求
- 实时数据查询
- 分布式锁场景

### 4. **仅MQ存储** (`MQ_ONLY`)
```yaml
hibiscus:
  persistenceStrategy: "mq"
  mq:
    enabled: true
    type: "rabbitmq"  # rabbitmq, kafka, rocketmq
    host: "localhost"
    port: 5672
    username: "guest"
    password: "guest"
    virtualHost: "/"
```

**适用场景**：
- 异步解耦
- 跨服务通信
- 削峰填谷

## 🔄 组合策略

### 5. **数据库 + Redis** (`DATABASE_AND_REDIS`)
```yaml
hibiscus:
  persistenceStrategy: "database_redis"
  databasePersistent: true
  redis:
    enabled: true
    host: "localhost"
    port: 6379
```

**优势**：
- 数据库保证数据持久化
- Redis提供高性能缓存
- 查询时优先从Redis获取

### 6. **数据库 + MQ** (`DATABASE_AND_MQ`)
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

**优势**：
- 数据库保证数据持久化
- MQ提供异步处理能力
- 支持跨服务事件传递

### 7. **Redis + MQ** (`REDIS_AND_MQ`)
```yaml
hibiscus:
  persistenceStrategy: "redis_mq"
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

**优势**：
- Redis提供高性能缓存
- MQ提供异步处理能力
- 适合高并发场景

### 8. **完整方案** (`ALL`)
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

**优势**：
- 数据库保证数据持久化
- Redis提供高性能缓存
- MQ提供异步处理能力
- 适合企业级应用

## 🚀 性能对比

| 存储方式 | 写入性能 | 读取性能 | 持久化 | 成本 | 适用场景 |
|---------|---------|---------|--------|------|----------|
| 文件 | 高 | 低 | 是 | 低 | 开发测试 |
| 数据库 | 中 | 中 | 是 | 中 | 生产环境 |
| Redis | 高 | 高 | 否 | 中 | 缓存场景 |
| MQ | 高 | 中 | 是 | 高 | 异步解耦 |

## 📊 配置示例

### 高并发电商场景
```yaml
hibiscus:
  persistenceStrategy: "database_redis"
  databasePersistent: true
  databaseTableName: "order_events"
  databaseRetentionDays: 90
  redis:
    enabled: true
    host: "redis-cluster"
    port: 6379
    expireSeconds: 3600  # 1小时
```

### 微服务架构
```yaml
hibiscus:
  persistenceStrategy: "database_mq"
  databasePersistent: true
  databaseTableName: "service_events"
  mq:
    enabled: true
    type: "rabbitmq"
    host: "rabbitmq-cluster"
    port: 5672
    username: "service_user"
    password: "service_pass"
```

### 实时监控系统
```yaml
hibiscus:
  persistenceStrategy: "redis_mq"
  redis:
    enabled: true
    host: "redis-sentinel"
    port: 6379
    expireSeconds: 1800  # 30分钟
  mq:
    enabled: true
    type: "kafka"
    host: "kafka-cluster"
    port: 9092
```

## 🔧 最佳实践

### 1. **选择合适的策略**
- 开发环境：使用文件存储
- 测试环境：使用数据库存储
- 生产环境：根据业务需求选择组合策略

### 2. **性能优化**
- 使用Redis缓存热点数据
- 使用MQ处理异步任务
- 定期清理过期数据

### 3. **监控告警**
- 监控存储空间使用情况
- 监控读写性能指标
- 设置合理的告警阈值

### 4. **数据备份**
- 定期备份数据库数据
- 配置Redis持久化
- 监控MQ消息堆积

## 🛠️ 故障处理

### 1. **存储服务不可用**
- 启用降级策略
- 使用本地缓存
- 记录错误日志

### 2. **数据不一致**
- 定期数据校验
- 启用数据同步机制
- 提供数据修复工具

### 3. **性能问题**
- 优化查询语句
- 增加缓存层
- 扩容存储资源
