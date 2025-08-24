# Hibiscus Signal Demo

这是一个演示项目，展示了如何使用 Hibiscus Signal 框架来实现事件驱动架构。

## 🎯 项目概述

本项目演示了 Hibiscus Signal 框架的核心功能：

- **事务隔离**：事件处理在独立事务中执行，不影响主业务事务
- **事件持久化**：事件数据持久化到数据库，支持事件恢复
- **智能重试**：失败事件自动重试，支持指数退避算法
- **状态管理**：完整的事件处理状态跟踪
- **Spring Boot 集成**：无缝集成，开箱即用

## 🚀 快速开始

### 1. 环境准备

- Java 8+
- Maven 3.6+
- MySQL 5.7+

### 2. 数据库配置

创建数据库：

```sql
CREATE DATABASE hibiscus_signal_demo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

修改 `application.yml` 中的数据库配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/hibiscus_signal_demo?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: your_username
    password: your_password
```

### 3. 运行项目

```bash
# 编译项目
mvn clean compile

# 运行项目
mvn spring-boot:run
```

### 4. 访问应用

- 应用地址：http://localhost:8080
- 健康检查：http://localhost:8080/api/orders/health

## 📖 功能演示

### 1. 自动演示

应用启动后会自动运行演示，包括：

1. **创建订单**：演示事件发送和事务隔离
2. **支付订单**：演示事件处理和状态更新
3. **失败重试**：演示智能重试机制

### 2. REST API 演示

#### 创建订单

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1001,
    "productId": 2001,
    "productName": "iPhone 15 Pro",
    "quantity": 1,
    "unitPrice": 8999.00
  }'
```

#### 支付订单

```bash
curl -X POST http://localhost:8080/api/orders/{orderNo}/pay
```

#### 取消订单

```bash
curl -X POST http://localhost:8080/api/orders/{orderNo}/cancel
```

## 🏗️ 项目结构

```
src/main/java/com/hibiscus/demo/
├── HibiscusSignalDemoApplication.java  # 主应用程序
├── DemoStarter.java                    # 演示启动器
├── controller/
│   └── OrderController.java            # REST 控制器
├── entity/
│   └── Order.java                      # 订单实体
├── handler/
│   └── OrderEventHandler.java          # 事件处理器
├── repository/
│   └── OrderRepository.java            # 数据访问层
└── service/
    └── OrderService.java               # 业务服务层
```

## 🔧 核心功能演示

### 1. 事务隔离

在 `OrderService.createOrder()` 方法中：

```java
@Transactional
public Order createOrder(...) {
    // 1. 创建订单（主事务）
    Order order = orderRepository.save(new Order(...));
    
    // 2. 发送事件（独立事务）
    signals.emit("order.created", this, context);
    
    // 即使事件处理失败，订单创建事务不受影响
    return order;
}
```

### 2. 事件处理

在 `OrderEventHandler` 中：

```java
@SignalHandler("order.created")
public void handleOrderCreated(SignalContext context) {
    // 在独立事务中处理事件
    // 支持自动重试和状态管理
}
```

### 3. 配置说明

在 `application.yml` 中配置 Hibiscus Signal：

```yaml
hibiscus:
  database-persistent: true              # 启用数据库持久化
  database-table-name: signal_events     # 事件表名
  database-retention-days: 30           # 数据保留天数
  enable-database-cleanup: true         # 启用自动清理
```

## 📊 监控和日志

### 1. 日志级别

```yaml
logging:
  level:
    com.hibiscus.signal: DEBUG          # 框架调试日志
    org.springframework.transaction: DEBUG  # 事务调试日志
```

### 2. 事件状态查询

可以通过数据库查询事件处理状态：

```sql
-- 查看所有事件
SELECT * FROM signal_events ORDER BY created_time DESC;

-- 查看失败事件
SELECT * FROM signal_events WHERE status = 'FAILED';

-- 查看重试事件
SELECT * FROM signal_events WHERE status = 'RETRYING';
```

## 🎯 演示要点

### 1. 事务隔离演示

- 订单创建成功，但事件处理失败时，订单数据不受影响
- 事件处理在独立事务中执行，支持自动回滚

### 2. 重试机制演示

- 事件处理失败时自动重试
- 使用指数退避算法，避免雪崩效应
- 最终失败的事件进入死信队列

### 3. 状态管理演示

- 事件处理状态实时可见
- 支持事件恢复和重发
- 完整的处理记录和错误信息

## 🔍 故障排查

### 1. 常见问题

**问题**：数据库连接失败
**解决**：检查数据库配置和连接信息

**问题**：事件处理失败
**解决**：查看日志了解具体错误原因

**问题**：重试机制不工作
**解决**：检查 Hibiscus Signal 配置

### 2. 日志分析

重点关注以下日志：

- `✅ 订单创建成功`：主业务成功
- `📤 订单创建事件已发送`：事件发送成功
- `🔄 处理订单创建事件`：事件处理开始
- `✅ 订单创建事件处理成功`：事件处理成功
- `❌ 订单创建事件处理失败`：事件处理失败

## 📈 性能测试

可以通过以下方式测试性能：

1. **并发测试**：同时创建多个订单
2. **压力测试**：大量事件处理
3. **故障测试**：模拟网络抖动和系统重启

## 🎉 总结

这个演示项目展示了 Hibiscus Signal 框架的核心优势：

- **简单易用**：与 Spring Boot 无缝集成
- **功能强大**：支持事务隔离、持久化、重试等企业级特性
- **可靠性高**：确保事件不丢失，支持自动恢复
- **性能优异**：异步处理，支持高并发

通过这个演示，您可以深入了解事件驱动架构的设计理念和实现方式。