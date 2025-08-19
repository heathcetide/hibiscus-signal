# Hibiscus API 文档 Web 界面

## 概述

这是一个使用 Tailwind CSS 设计的现代化、美观的 API 文档展示和测试平台，提供了完整的 API 文档展示、搜索、筛选和接口测试功能。

## ✨ 新特性

### 🎨 **现代化设计**
- 使用 **Tailwind CSS** 框架，界面更加美观现代
- 渐变背景、玻璃效果、悬停动画
- 响应式设计，支持各种屏幕尺寸
- 优雅的卡片布局和颜色编码

### 🔧 **集成接口测试**
- 内置接口测试工具，无需切换页面
- 支持所有 HTTP 方法（GET、POST、PUT、DELETE、PATCH）
- 实时显示响应状态、响应头、响应体和响应时间
- 一键快速测试 API 接口

### 📱 **标签页切换**
- API 文档和接口测试集成在一个页面
- 平滑的标签页切换动画
- 统一的用户界面体验

### 📂 **智能分类管理**
- **按控制器分组**: 自动将 API 按控制器类进行分组
- **折叠展开**: 支持点击控制器头部展开/折叠 API 列表
- **统计信息**: 每个控制器显示包含的 API 数量和方法分布
- **视觉层次**: 清晰的层级结构，便于快速定位

### 📄 **分页功能**
- **灵活分页**: 支持每页显示 5、10、20、50 个控制器
- **导航控制**: 上一页/下一页按钮，带状态指示
- **实时统计**: 显示当前页码、总页数和总控制器数
- **性能优化**: 大量 API 时保持良好的页面性能

## 功能特性

### 🎯 核心功能
- **自动扫描**: 自动扫描 Spring Boot 应用中的控制器和 API 端点
- **实时展示**: 实时显示扫描到的 API 信息
- **参数解析**: 智能解析和展示 API 参数信息
- **类型识别**: 自动识别参数类型、是否必需、默认值等

### 🔍 搜索和筛选
- **全文搜索**: 支持按 API 名称、类名、路径进行搜索
- **方法筛选**: 按 HTTP 方法（GET、POST、PUT、DELETE、PATCH）筛选
- **实时过滤**: 搜索结果实时更新

### 📊 统计信息
- **API 总数**: 显示当前扫描到的 API 总数
- **控制器数量**: 显示涉及的控制器类数量
- **方法数量**: 显示 API 方法数量
- **更新时间**: 显示最后更新时间

### 🧪 接口测试
- **HTTP 方法**: 支持所有标准 HTTP 方法
- **请求配置**: 可配置请求 URL、请求头、请求体
- **响应展示**: 显示状态码、响应头、响应体、响应时间
- **快速测试**: 从 API 卡片一键跳转到测试工具

## 文件结构

```
src/main/resources/static/
├── index.html          # 主页面（完整功能 + 接口测试）
├── simple.html         # 简单版本（仅文档展示）
├── cors-test.html      # CORS 测试页面
└── js/
    └── api-docs.js     # 主要 JavaScript 逻辑
```

## 使用方法

### 1. 启动应用

```bash
mvn spring-boot:run
```

### 2. 访问页面

- **主页面**: http://localhost:8080/index.html
- **简单版本**: http://localhost:8080/simple.html
- **CORS 测试页面**: http://localhost:8080/cors-test.html

### 3. 查看 API 文档

页面会自动加载并显示扫描到的 API 信息，包括：
- API 方法名和 HTTP 方法（颜色编码）
- 完整的 API 路径
- 参数详细信息（类型、是否必需、默认值）
- 请求体字段信息

### 4. 搜索和筛选

- 在搜索框中输入关键词进行搜索
- 使用下拉菜单选择特定的 HTTP 方法
- 点击"刷新数据"按钮重新加载数据

### 5. 分类管理

- **控制器分组**: API 自动按控制器类进行分组显示
- **折叠展开**: 点击控制器头部可以展开/折叠该组的所有 API
- **快速定位**: 通过控制器名称快速找到相关 API
- **统计概览**: 每个控制器显示包含的 API 数量和方法分布

### 6. 分页浏览

- **分页控制**: 使用上一页/下一页按钮浏览不同页面
- **显示数量**: 选择每页显示的控制器数量（5/10/20/50）
- **页面信息**: 实时显示当前页码、总页数和总控制器数
- **性能优化**: 大量 API 时分页显示，保持页面响应速度

### 7. 接口测试

- 点击导航栏的"接口测试"标签页
- 配置后端服务端口（如果与默认端口不同）
- 配置 HTTP 方法、请求 URL、请求头、请求体
- 点击"发送请求"按钮测试接口
- 查看响应结果和响应时间

### 8. 快速测试

- 在 API 文档卡片上点击"测试"按钮
- 自动跳转到测试工具并填充相关信息
- 快速进行接口测试

### 9. CORS 测试

- 访问 CORS 测试页面
- 配置后端服务端口
- 选择要测试的端点
- 点击"测试 CORS"按钮验证跨域访问是否正常

## 技术栈

- **前端**: HTML5, CSS3, JavaScript (ES6+)
- **CSS 框架**: Tailwind CSS
- **图标**: Font Awesome 6.4.0
- **后端**: Spring Boot 2.4.5
- **数据格式**: JSON

## 数据获取方式

### 推荐：从内存中获取
- **端点**: `GET /test/api/docs`
- **优势**: 实时数据、性能更好、数据一致
- **说明**: 从 `MappingHandler` 的内存中直接获取扫描结果

### 备选：从文件读取
- **文件**: `requestInfos.json`
- **优势**: 离线可用、数据持久化
- **说明**: 适合静态展示或离线环境

## 自定义配置

### 修改扫描路径

在 `application.yaml` 中配置：

```yaml
hibiscus:
  helper:
    scan-path: com.your.package
```

### 修改 API 端点

在 `TestController` 中修改 `@GetMapping` 注解：

```java
@GetMapping("/your/api/docs")
public List<RequestInfo> getApiDocs() {
    return mappingHandler.getRequestInfos();
}
```

### CORS 配置

项目已包含全局 CORS 配置，支持跨域访问：

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

如果需要自定义 CORS 策略，可以修改 `CorsConfig.java` 文件。

### 自定义 Tailwind 配置

在 `index.html` 中修改 Tailwind 配置：

```javascript
tailwind.config = {
    theme: {
        extend: {
            colors: {
                primary: {
                    500: '#your-color',
                    // 更多自定义颜色
                }
            }
        }
    }
}
```

## 故障排除

### 页面显示空白
- 检查后端服务是否正常运行
- 确认 API 端点是否可访问
- 查看浏览器控制台是否有错误信息

### 无法加载数据
- 检查网络请求是否成功
- 确认 API 端点路径是否正确
- 查看后端日志是否有异常

### 搜索功能不工作
- 确认 JavaScript 文件是否正确加载
- 检查浏览器控制台是否有错误
- 验证事件监听器是否正确绑定

### 接口测试失败
- 检查请求 URL 是否正确
- 确认请求头和请求体格式
- 查看浏览器控制台的错误信息

## 扩展功能

### 添加新的 HTTP 方法支持

在 `MappingHandler.java` 中添加新的方法类型支持：

```java
private boolean isAnnotatedWithMapping(Method method) {
    return method.isAnnotationPresent(GetMapping.class) ||
           method.isAnnotationPresent(PostMapping.class) ||
           method.isAnnotationPresent(PutMapping.class) ||
           method.isAnnotationPresent(DeleteMapping.class) ||
           method.isAnnotationPresent(PatchMapping.class) ||
           method.isAnnotationPresent(YourNewMapping.class); // 添加新方法
}
```

### 自定义参数解析

在 `MappingHandler.java` 中添加新的参数类型处理：

```java
private void handleParameter(Parameter parameter, Map<String, Object> methodParameters) {
    // ... 现有代码 ...
    
    if (parameter.isAnnotationPresent(YourNewAnnotation.class)) {
        handleYourNewParameter(parameter, methodParameters);
    }
}
```

### 添加新的测试功能

在 `api-docs.js` 中添加新的测试功能：

```javascript
// 添加新的测试方法
function customTestFunction() {
    // 自定义测试逻辑
}
```

## 性能优化

### 前端优化
- 使用 Tailwind CSS 的 JIT 编译
- 延迟加载和动画效果
- 响应式图片和图标
- 分页加载，减少 DOM 节点数量
- 智能折叠，提升大量数据的浏览体验

### 后端优化
- 内存缓存 API 数据
- 异步扫描和更新
- 智能参数解析
- 按需加载，支持大规模 API 集合

## 贡献

欢迎提交 Issue 和 Pull Request 来改进这个项目！

### 贡献指南
1. Fork 项目
2. 创建功能分支
3. 提交更改
4. 推送到分支
5. 创建 Pull Request

## 许可证

本项目采用 [许可证名称] 许可证。

## 更新日志

### v3.0.0 (当前版本)
- 📂 添加智能分类管理功能
- 📄 实现分页浏览系统
- 🎨 优化页面样式和布局
- 🔍 改进搜索和筛选体验
- 📱 增强响应式设计
- 🚀 提升大量数据的性能表现

### v2.0.0
- ✨ 使用 Tailwind CSS 重新设计界面
- 🧪 集成接口测试功能
- 📱 添加标签页切换
- 🎨 现代化 UI 设计
- 🚀 性能优化和改进

### v1.0.0
- 📚 基础 API 文档展示
- 🔍 搜索和筛选功能
- 📊 统计信息展示