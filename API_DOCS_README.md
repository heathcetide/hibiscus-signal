# Hibiscus API 接口文档系统

## 概述

这是一个基于 Spring Boot 的自动 API 接口文档生成系统，能够自动扫描项目中的控制器类，生成详细的接口文档，并支持多种格式导出。

## 主要功能

### 1. 自动接口扫描
- 自动扫描 `@RestController` 注解的控制器类
- 识别 `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping` 等注解
- 自动解析请求参数、路径变量、请求体等信息

### 2. 接口文档展示
- 美观的 Web 界面展示所有 API 接口
- 按控制器分组显示
- 支持搜索和筛选功能
- 显示详细的参数信息

### 3. 多种文档格式
- **Markdown**: 适合在 Git 仓库中维护
- **HTML**: 可以直接在浏览器中查看
- **OpenAPI 3.0 JSON**: 符合 OpenAPI 规范，可导入到 Swagger UI 等工具

### 4. 接口测试功能
- 内置接口测试工具
- 支持多种 HTTP 方法
- 可配置请求头、请求体等
- 实时显示响应结果

### 5. 性能监控
- 请求统计信息
- 响应时间监控
- 系统资源使用情况
- 缓存状态监控

## 快速开始

### 1. 启动应用
```bash
mvn spring-boot:run
```

### 2. 访问接口文档
打开浏览器访问: `http://localhost:8080`

### 3. 查看 API 列表
系统会自动扫描并显示所有可用的 API 接口。

### 4. 下载文档
点击"下载文档"按钮，选择需要的格式进行下载。

## API 端点

### 文档相关
- `GET /test/api/docs` - 获取所有 API 接口信息
- `GET /test/api/docs/markdown` - 生成 Markdown 格式文档
- `GET /test/api/docs/html` - 生成 HTML 格式文档
- `GET /test/api/docs/openapi` - 生成 OpenAPI 3.0 规范文档
- `GET /test/api/docs/download/{format}` - 下载指定格式的文档

### 统计信息
- `GET /test/api/stats` - 获取 API 统计信息
- `GET /test/api/search` - 搜索 API 接口

### 性能监控
- `GET /test/api/performance` - 获取性能监控数据
- `GET /test/api/cache/stats` - 获取缓存统计信息

## 配置说明

### 应用配置 (application.yaml)
```yaml
hibiscus:
  helper:
    scanPath: "com.hibiscus.docs.core"  # 扫描的包路径
  
  security:
    enabled: true                        # 是否启用安全控制
    mode: "both"                         # 安全模式: ip, token, both
  
  advanced:
    enableRequestLogging: true           # 启用请求日志
    enablePerformanceMonitoring: true    # 启用性能监控
    enableRateLimiting: true            # 启用限流
    
    cache:
      enabled: true                      # 启用缓存
      ttlSeconds: 300                   # 缓存过期时间
      maxSize: 1000                     # 最大缓存条目数
```

## 使用方法

### 1. 添加控制器
在你的控制器类上添加 `@RestController` 注解：

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        // 实现逻辑
    }
    
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        // 实现逻辑
    }
}
```

### 2. 添加文档注解（可选）
为了生成更详细的文档，可以添加文档注解：

```java
@ApiOperation(value = "获取用户信息", notes = "根据用户ID获取用户详细信息")
@GetMapping("/{id}")
public ResponseEntity<User> getUserById(
    @ApiParam(value = "用户ID", required = true) @PathVariable Long id) {
    // 实现逻辑
}
```

### 3. 自定义参数信息
系统会自动识别以下注解：
- `@RequestParam` - 查询参数
- `@PathVariable` - 路径变量
- `@RequestBody` - 请求体

## 高级功能

### 1. 自定义扫描路径
在配置文件中修改 `hibiscus.helper.scanPath` 来指定要扫描的包路径。

### 2. 性能监控
系统会自动收集以下指标：
- 请求总数和成功率
- 平均响应时间
- 系统资源使用情况
- 端点级别的性能数据

### 3. 缓存管理
- 自动缓存管理
- 可配置的 TTL 和最大容量
- 缓存统计信息

### 4. 安全控制
- IP 白名单控制
- 访问令牌验证
- 可配置的安全策略

## 故障排除

### 1. 接口未显示
- 检查控制器类是否有 `@RestController` 注解
- 确认包路径是否在扫描范围内
- 查看控制台日志中的扫描信息

### 2. 文档生成失败
- 检查 `ApiDocsGenerator` 类是否正确注入
- 确认 `MappingHandler` 是否正常工作
- 查看异常堆栈信息

### 3. 性能监控无数据
- 确认是否启用了性能监控
- 检查是否有请求经过系统
- 查看监控配置是否正确

## 扩展开发

### 1. 添加新的文档格式
继承 `ApiDocsGenerator` 类，实现新的文档生成方法。

### 2. 自定义参数解析
修改 `MappingHandler` 类中的参数处理逻辑。

### 3. 增强监控功能
扩展 `PerformanceMonitor` 类，添加更多监控指标。

## 技术架构

- **Spring Boot**: 基础框架
- **反射机制**: 自动扫描和解析注解
- **Jackson**: JSON 处理
- **Tailwind CSS**: 前端样式
- **原生 JavaScript**: 前端交互

## 版本历史

- **v1.0.0**: 基础功能实现
  - 自动接口扫描
  - 基础文档生成
  - Web 界面展示
  - 接口测试工具

## 贡献指南

欢迎提交 Issue 和 Pull Request 来改进这个项目。

## 许可证

本项目采用 MIT 许可证。
