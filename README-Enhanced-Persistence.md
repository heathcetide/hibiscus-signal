# Hibiscus Signal 增强版持久化功能

## 🎯 功能概述

为了解决原有持久化功能的缺陷，我们实现了增强版持久化功能，支持：

- ✅ **追加写入**：不会覆盖之前的数据
- ✅ **文件轮转**：自动创建新文件避免文件过大
- ✅ **线程安全**：使用读写锁保证并发安全
- ✅ **统计功能**：提供文件大小和记录数统计
- ✅ **配置灵活**：支持多种持久化配置选项

## 🔧 核心改进

### 1. 修复序列化问题

```java
// 修复前：缺少无参构造函数
public class SignalPersistenceInfo {
    // 只有带参构造函数，Jackson无法反序列化
}

// 修复后：添加无参构造函数
public class SignalPersistenceInfo {
    // 添加无参构造函数，支持Jackson反序列化
    public SignalPersistenceInfo() {
    }
    
    // 原有构造函数保持不变
    public SignalPersistenceInfo(SigHandler sigHandler, SignalConfig signalConfig, 
                                SignalContext signalContext, Map<String, Map<String, Object>> metrics) {
        // ...
    }
}
```

### 2. 实现追加写入

```java
// 新增：EnhancedSignalPersistence.java
public class EnhancedSignalPersistence {
    
    /**
     * 追加写入持久化信息到文件
     * 不会覆盖之前的数据，而是追加到文件末尾
     */
    public static void appendToFile(SignalPersistenceInfo info, String filePath) {
        fileLock.writeLock().lock();
        try {
            // 读取现有数据
            List<SignalPersistenceInfo> existingData = readAllFromFile(filePath);
            
            // 添加新数据
            existingData.add(info);
            
            // 写入所有数据
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), existingData);
            
        } finally {
            fileLock.writeLock().unlock();
        }
    }
}
```

### 3. 文件轮转机制

```java
/**
 * 文件轮转：当文件过大时，创建新文件
 */
public static void rotateFileIfNeeded(String filePath, long maxSizeBytes) {
    fileLock.writeLock().lock();
    try {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            return;
        }
        
        long fileSize = Files.size(path);
        if (fileSize > maxSizeBytes) {
            // 创建备份文件
            String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
            String backupPath = filePath.replace(".json", "_" + timestamp + ".json");
            Files.move(path, Paths.get(backupPath));
            System.out.println("文件轮转: " + filePath + " -> " + backupPath);
        }
        
    } finally {
        fileLock.writeLock().unlock();
    }
}
```

### 4. 增强配置选项

```java
@ConfigurationProperties("hibiscus")
public class SignalProperties {
    private Boolean persistent = false;                    // 是否启用持久化
    private String persistenceFile = "signal.json";        // 持久化文件名
    private String persistenceDirectory = "logs/signals";  // 持久化目录
    private Long maxFileSizeBytes = 10 * 1024 * 1024L;    // 最大文件大小（10MB）
    private Boolean enableFileRotation = true;            // 是否启用文件轮转
    private Integer maxBackupFiles = 10;                  // 最大备份文件数
}
```

## 📝 配置示例

### application.yml

```yaml
hibiscus:
  # 启用持久化功能
  persistent: true
  
  # 持久化文件配置
  persistence-file: signal.json
  persistence-directory: logs/signals
  
  # 文件轮转配置
  max-file-size-bytes: 10485760  # 10MB
  enable-file-rotation: true
  max-backup-files: 10
```

### application.properties

```properties
# 启用持久化功能
hibiscus.persistent=true

# 持久化文件配置
hibiscus.persistence-file=signal.json
hibiscus.persistence-directory=logs/signals

# 文件轮转配置
hibiscus.max-file-size-bytes=10485760
hibiscus.enable-file-rotation=true
hibiscus.max-backup-files=10
```

## 🚀 使用示例

### 1. 基本使用

```java
@Service
public class OrderService {
    
    @SignalEmitter("order.created")
    public Order createOrder(OrderRequest request) {
        Order order = orderRepository.save(new Order(request));
        
        // 框架会自动持久化事件信息
        return order;
    }
}
```

### 2. 自定义配置

```java
@Component
public class CustomEventHandler {
    
    @SignalHandler(
        value = "order.created",
        target = CustomEventHandler.class,
        methodName = "handleOrderCreated",
        persistent = true  // 启用单个事件的持久化
    )
    public void handleOrderCreated(SignalContext context) {
        // 处理订单创建事件
        Order order = (Order) context.getAttribute("order");
        sendNotification(order);
    }
}
```

### 3. 查看持久化数据

```java
@Component
public class PersistenceMonitor {
    
    @Autowired
    private SignalProperties signalProperties;
    
    public void checkPersistenceStatus() {
        String fullPath = signalProperties.getPersistenceDirectory() + "/" + signalProperties.getPersistenceFile();
        
        // 读取所有持久化数据
        List<SignalPersistenceInfo> allData = EnhancedSignalPersistence.readAllFromFile(fullPath);
        System.out.println("持久化记录数: " + allData.size());
        
        // 获取文件统计信息
        EnhancedSignalPersistence.FileStats stats = EnhancedSignalPersistence.getFileStats(fullPath);
        System.out.println("文件统计: " + stats);
    }
}
```

## 📊 文件结构

启用持久化后，会在指定目录生成以下文件：

```
logs/signals/
├── signal.json                    # 当前持久化文件
├── signal_2024-01-15_10-30-45.json  # 轮转备份文件
├── signal_2024-01-15_11-15-20.json  # 轮转备份文件
└── ...
```

## 🔍 监控和统计

### 文件统计信息

```java
EnhancedSignalPersistence.FileStats stats = EnhancedSignalPersistence.getFileStats(filePath);
System.out.println(stats);
// 输出: FileStats{records=150, size=2.45MB, total=150}
```

### 持久化数据内容

```json
[
  {
    "sigHandler": {
      "id": 123456789,
      "signalName": "order.created",
      "handlerName": "OrderEventHandler#handleOrderCreated"
    },
    "signalConfig": {
      "async": true,
      "persistent": true,
      "maxRetries": 3
    },
    "signalContext": {
      "attributes": {
        "orderId": "ORD-001",
        "userId": "USER-123"
      },
      "traceId": "uuid-123",
      "eventId": "order.created_123456789"
    },
    "metrics": {
      "order.created": {
        "emitCount": 1,
        "handlerCount": 1,
        "processingTime": 150
      }
    }
  }
]
```

## ⚠️ 注意事项

1. **性能影响**：持久化会增加一定的I/O开销
2. **磁盘空间**：需要定期清理旧的备份文件
3. **并发安全**：使用读写锁保证线程安全
4. **错误处理**：持久化失败不会影响业务逻辑执行

## 🔧 故障排除

### 常见问题

1. **持久化文件为空**
   - 检查 `hibiscus.persistent` 配置是否为 `true`
   - 确认事件处理器配置了 `persistent = true`

2. **文件轮转不工作**
   - 检查 `hibiscus.enable-file-rotation` 配置
   - 确认 `max-file-size-bytes` 设置合理

3. **权限问题**
   - 确保应用有写入 `persistence-directory` 的权限
   - 检查磁盘空间是否充足

### 调试模式

```yaml
logging:
  level:
    com.hibiscus.signal: DEBUG
    com.hibiscus.signal.config.EnhancedSignalPersistence: DEBUG
```

## 🎉 总结

增强版持久化功能解决了原有功能的缺陷：

- ✅ **解决了文件覆盖问题**：使用追加写入
- ✅ **修复了序列化问题**：添加无参构造函数
- ✅ **提供了文件管理**：自动轮转和备份
- ✅ **增强了配置选项**：更灵活的配置
- ✅ **保证了线程安全**：使用读写锁
- ✅ **提供了监控功能**：文件统计和状态查看

现在您可以安全地在生产环境中使用持久化功能了！
