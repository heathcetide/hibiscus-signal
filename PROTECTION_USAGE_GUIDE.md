# 🔒 Hibiscus Signal 保护机制使用指南

## 📖 概述

Hibiscus Signal 框架提供了完整的保护机制，包括熔断器（Circuit Breaker）和限流器（Rate Limiter），用于保护系统免受故障和过载的影响。

## 🚀 快速开始

### 1. 启用保护机制

在 `application.yml` 中启用保护机制：

```yaml
hibiscus:
  protection:
    enabled: true
    
    # 熔断器配置
    circuit-breaker:
      failure-threshold: 5          # 失败次数阈值
      open-timeout-ms: 60000        # 熔断器打开时间（毫秒）
      half-open-trial-count: 3      # 半开状态下的试验次数
      error-rate-threshold: 0.5     # 错误率阈值（50%）
    
    # 限流器配置
    rate-limiter:
      max-requests-per-second: 1000 # 每秒最大请求数
```

### 2. 自动配置保护机制

框架会自动为每个注册的事件配置保护机制：

```java
@Service
public class OrderService {
    
    @Autowired
    private Signals signals;
    
    public void createOrder(OrderRequest request) {
        // 注册事件处理器时，框架自动配置保护机制
        signals.connect("order.created", this::handleOrderCreated, 
                       new SignalConfig.Builder().async(true).build());
        
        // 发送事件
        signals.emit("order.created", this, request);
    }
    
    public void handleOrderCreated(Object sender, Object... params) {
        // 事件处理逻辑
        OrderRequest request = (OrderRequest) params[0];
        // ... 处理订单创建
    }
}
```

## 🔧 手动配置保护机制

### 1. 手动创建熔断器

```java
// 创建熔断器：5次失败后熔断，60秒后半开，3次成功试验后关闭
CircuitBreaker breaker = new CircuitBreaker(5, 60000, 3);

// 创建限流器：每秒最多1000个请求
RateLimiter limiter = new RateLimiter(1000);

// 配置保护机制
signals.configureProtection("order.created", breaker, limiter);
```

### 2. 自定义保护策略

```java
@Component
public class CustomProtectionConfig {
    
    @Autowired
    private Signals signals;
    
    @PostConstruct
    public void configureProtection() {
        // 为不同事件配置不同的保护策略
        
        // 订单事件：严格保护
        CircuitBreaker orderBreaker = new CircuitBreaker(3, 30000, 2);
        RateLimiter orderLimiter = new RateLimiter(500);
        signals.configureProtection("order.*", orderBreaker, orderLimiter);
        
        // 通知事件：宽松保护
        CircuitBreaker notificationBreaker = new CircuitBreaker(10, 60000, 5);
        RateLimiter notificationLimiter = new RateLimiter(2000);
        signals.configureProtection("notification.*", notificationBreaker, notificationLimiter);
    }
}
```

## 📊 保护机制工作原理

### 1. 熔断器状态转换

```
CLOSED → OPEN → HALF_OPEN → CLOSED
  ↓        ↓        ↓         ↓
 正常    熔断     半开      恢复
```

- **CLOSED（关闭）**：正常状态，请求正常通过
- **OPEN（打开）**：熔断状态，请求被拒绝
- **HALF_OPEN（半开）**：试验状态，允许少量请求通过

### 2. 限流器滑动窗口

限流器使用滑动窗口算法，每秒最多允许指定数量的请求：

```
时间窗口：1秒
最大请求数：1000
当前请求数：850
状态：允许新请求 ✅
```

## 🧪 测试保护机制

### 1. 运行测试用例

```bash
# 运行保护机制测试
mvn test -Dtest=ProtectionMechanismTest

# 运行特定测试方法
mvn test -Dtest=ProtectionMechanismTest#testCircuitBreakerBasicFunctionality
```

### 2. 手动测试熔断器

```java
@Test
public void testCircuitBreaker() {
    // 创建熔断器
    CircuitBreaker breaker = new CircuitBreaker(2, 1000, 2);
    
    // 初始状态：关闭
    assertFalse(breaker.isOpen());
    
    // 1次失败：仍关闭
    breaker.recordFailure();
    assertFalse(breaker.isOpen());
    
    // 2次失败：熔断器打开
    breaker.recordFailure();
    assertTrue(breaker.isOpen());
    
    // 等待1秒后：变为半开
    Thread.sleep(1100);
    assertFalse(breaker.isOpen());
    
    // 2次成功：恢复关闭状态
    breaker.recordSuccess();
    breaker.recordSuccess();
    assertFalse(breaker.isOpen());
}
```

## 📈 监控和指标

### 1. 查看保护机制状态

```java
@RestController
@RequestMapping("/api/protection")
public class ProtectionController {
    
    @Autowired
    private Signals signals;
    
    @GetMapping("/status/{eventName}")
    public Map<String, Object> getProtectionStatus(@PathVariable String eventName) {
        SignalProtectionManager protectionManager = signals.getProtectionManager();
        
        CircuitBreaker breaker = protectionManager.getCircuitBreaker(eventName);
        RateLimiter limiter = protectionManager.getRateLimiter(eventName);
        
        Map<String, Object> status = new HashMap<>();
        status.put("eventName", eventName);
        status.put("circuitBreakerOpen", breaker != null && breaker.isOpen());
        status.put("rateLimiterBlocked", limiter != null && !limiter.allowRequest());
        status.put("isBlocked", protectionManager.isBlocked(eventName));
        
        return status;
    }
}
```

### 2. 监控指标

框架自动收集以下指标：

- **熔断器状态变化次数**
- **限流器拒绝请求次数**
- **各事件的错误率**
- **保护机制响应时间**

## ⚠️ 注意事项

### 1. 配置建议

- **熔断器阈值**：根据业务特点设置，建议5-10次失败
- **打开时间**：建议30-60秒，避免频繁切换状态
- **限流QPS**：根据系统容量设置，建议不超过系统最大处理能力

### 2. 性能影响

- 保护机制检查对性能影响很小（< 1微秒）
- 建议在生产环境中启用保护机制
- 定期监控保护机制的状态和效果

### 3. 故障排查

- 检查配置文件中的保护机制设置
- 查看日志中的保护机制状态变化
- 使用监控接口查看实时状态

## 🔄 版本更新

### v1.0.0
- ✅ 基础熔断器功能
- ✅ 基础限流器功能
- ✅ 自动配置支持
- ✅ 配置属性支持

### 计划功能
- 🔄 动态配置更新
- 🔄 更多限流算法（令牌桶、漏桶）
- 🔄 保护机制集群同步
- 🔄 可视化监控面板

## 📞 技术支持

如果在使用过程中遇到问题，请：

1. 查看日志输出
2. 运行测试用例验证功能
3. 检查配置文件设置
4. 提交 Issue 描述问题

---

**保护机制是系统稳定性的重要保障，建议在生产环境中启用并合理配置！** 🛡️
