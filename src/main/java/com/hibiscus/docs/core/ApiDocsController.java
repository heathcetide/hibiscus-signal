package com.hibiscus.docs.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.hibiscus.docs.core.AlertManager;
import com.hibiscus.docs.core.PerformanceMonitor.PerformanceStats;
import java.time.LocalDateTime;
import java.util.ArrayList;
import com.hibiscus.docs.core.ApiTester;
import com.hibiscus.docs.core.TestResult;
import com.hibiscus.docs.core.ApiTestRequest;

@RestController
@RequestMapping("/test/api")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080", "http://localhost:8081"}, allowCredentials = "false")
public class ApiDocsController {

    @Autowired
    private MappingHandler mappingHandler;

    @Autowired
    private AppConfigProperties appConfigProperties;

    @Autowired
    private PerformanceMonitor performanceMonitor;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private AlertManager alertManager;
    
    @Autowired
    private ErrorMonitor errorMonitor;
    
    @Autowired
    private ApiTester apiTester;

    @Autowired
    private ApiDocsGenerator apiDocsGenerator;

    /**
     * 获取所有API接口信息
     */
    @GetMapping("/docs")
    public ResponseEntity<List<RequestInfo>> getApiDocs() {
        List<RequestInfo> requestInfos = mappingHandler.getRequestInfos();
        return ResponseEntity.ok(requestInfos);
    }

    /**
     * 获取环境配置信息
     */
    @GetMapping("/environments")
    public ResponseEntity<Map<String, Object>> getEnvironmentConfigs() {
        Map<String, Object> config = new HashMap<>();
        
        // 环境配置
        Map<String, Object> environments = new HashMap<>();
        Map<String, String> localEnv = new HashMap<>();
        localEnv.put("baseUrl", "http://localhost:8080");
        localEnv.put("description", "本地开发环境");
        environments.put("local", localEnv);
        
        Map<String, String> devEnv = new HashMap<>();
        devEnv.put("baseUrl", "https://dev-api.example.com");
        devEnv.put("description", "开发测试环境");
        environments.put("dev", devEnv);
        
        Map<String, String> prodEnv = new HashMap<>();
        prodEnv.put("baseUrl", "https://api.example.com");
        prodEnv.put("description", "生产环境");
        environments.put("prod", prodEnv);
        
        config.put("environments", environments);
        
        // 默认请求头
        Map<String, String> defaultHeaders = new HashMap<>();
        defaultHeaders.put("User-Agent", "Hibiscus-API-Test/1.0");
        defaultHeaders.put("Accept", "application/json");
        config.put("defaultHeaders", defaultHeaders);
        
        // 超时配置
        Map<String, Object> timeout = new HashMap<>();
        timeout.put("connect", appConfigProperties.getTesting().getTimeout().getConnect());
        timeout.put("read", appConfigProperties.getTesting().getTimeout().getRead());
        config.put("timeout", timeout);
        
        return ResponseEntity.ok(config);
    }

    /**
     * 获取安全配置状态
     */
    @GetMapping("/security/status")
    public ResponseEntity<Map<String, Object>> getSecurityStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", appConfigProperties.getSecurity().isEnabled());
        status.put("mode", appConfigProperties.getSecurity().getMode());
        status.put("allowLocalhost", appConfigProperties.getSecurity().isAllowLocalhost());
        status.put("tokenExpireHours", appConfigProperties.getSecurity().getTokenExpireHours());
        return ResponseEntity.ok(status);
    }

    /**
     * 获取性能监控数据
     */
    @GetMapping("/performance")
    public ResponseEntity<Map<String, Object>> getPerformanceData() {
        Map<String, Object> data = new HashMap<>();
        data.put("performance", performanceMonitor.getPerformanceStats());
        return ResponseEntity.ok(data);
    }

    /**
     * 获取缓存统计信息
     */
    @GetMapping("/cache/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        Map<String, Object> data = new HashMap<>();
        data.put("cache", cacheManager.getStats());
        return ResponseEntity.ok(data);
    }

    /**
     * 测试缓存功能
     */
    @PostMapping("/cache/test")
    public ResponseEntity<Map<String, Object>> testCache() {
        String testKey = "test_key_" + System.currentTimeMillis();
        String testValue = "test_value_" + System.currentTimeMillis();
        
        // 存储测试值
        cacheManager.put(testKey, testValue);
        
        // 读取测试值
        Object retrievedValue = cacheManager.get(testKey);
        
        Map<String, Object> result = new HashMap<>();
        result.put("cacheHit", retrievedValue != null);
        result.put("testValue", testValue);
        result.put("retrievedValue", retrievedValue);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 清空缓存
     */
    @PostMapping("/cache/clear")
    public ResponseEntity<Map<String, String>> clearCache() {
        cacheManager.clear();
        Map<String, String> result = new HashMap<>();
        result.put("message", "缓存已清空");
        return ResponseEntity.ok(result);
    }

    /**
     * 获取API统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getApiStats() {
        List<RequestInfo> requestInfos = mappingHandler.getRequestInfos();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalApis", requestInfos.size());
        
        // 按控制器分组统计
        Map<String, Long> controllerCounts = requestInfos.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                RequestInfo::getClassName,
                java.util.stream.Collectors.counting()
            ));
        stats.put("totalControllers", controllerCounts.size());
        
        // 按HTTP方法统计
        Map<String, Long> methodCounts = requestInfos.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                RequestInfo::getMethodType,
                java.util.stream.Collectors.counting()
            ));
        stats.put("methodCounts", methodCounts);
        
        // 按参数类型统计
        Map<String, Long> paramTypeCounts = new HashMap<>();
        requestInfos.forEach(api -> {
            if (api.getParameters() != null) {
                api.getParameters().forEach((key, value) -> {
                    if (!key.contains("_") && key != "body" && key != "bodyFields") {
                        String type = value.toString();
                        paramTypeCounts.put(type, paramTypeCounts.getOrDefault(type, 0L) + 1);
                    }
                });
            }
        });
        stats.put("paramTypeCounts", paramTypeCounts);
        
        return ResponseEntity.ok(stats);
    }

    /**
     * 搜索API接口
     */
    @GetMapping("/search")
    public ResponseEntity<List<RequestInfo>> searchApis(
            @RequestParam String query,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) String controller) {
        
        List<RequestInfo> allApis = mappingHandler.getRequestInfos();
        
        List<RequestInfo> filteredApis = allApis.stream()
            .filter(api -> {
                // 查询条件匹配
                boolean matchesQuery = query == null || query.isEmpty() ||
                    api.getMethodName().toLowerCase().contains(query.toLowerCase()) ||
                    api.getClassName().toLowerCase().contains(query.toLowerCase()) ||
                    api.getPaths().stream().anyMatch(path -> 
                        path.toLowerCase().contains(query.toLowerCase())
                    );
                
                // 方法过滤
                boolean matchesMethod = method == null || method.isEmpty() ||
                    api.getMethodType().equalsIgnoreCase(method);
                
                // 控制器过滤
                boolean matchesController = controller == null || controller.isEmpty() ||
                    api.getClassName().toLowerCase().contains(controller.toLowerCase());
                
                return matchesQuery && matchesMethod && matchesController;
            })
            .collect(java.util.stream.Collectors.toList());
        
        return ResponseEntity.ok(filteredApis);
    }

    /**
     * 获取API接口详情
     */
    @GetMapping("/docs/{className}/{methodName}")
    public ResponseEntity<RequestInfo> getApiDetail(
            @PathVariable String className,
            @PathVariable String methodName) {
        
        List<RequestInfo> allApis = mappingHandler.getRequestInfos();
        
        RequestInfo targetApi = allApis.stream()
            .filter(api -> api.getClassName().equals(className) && 
                          api.getMethodName().equals(methodName))
            .findFirst()
            .orElse(null);
        
        if (targetApi != null) {
            return ResponseEntity.ok(targetApi);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 生成 Markdown 格式的 API 文档
     */
    @GetMapping("/docs/markdown")
    public ResponseEntity<String> generateMarkdownDocs() {
        try {
            String markdown = apiDocsGenerator.generateMarkdownDocs();
            return ResponseEntity.ok()
                .header("Content-Type", "text/markdown; charset=utf-8")
                .body(markdown);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("生成 Markdown 文档失败: " + e.getMessage());
        }
    }
    
    /**
     * 生成 HTML 格式的 API 文档
     */
    @GetMapping("/docs/html")
    public ResponseEntity<String> generateHtmlDocs() {
        try {
            String html = apiDocsGenerator.generateHtmlDocs();
            return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=utf-8")
                .body(html);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("生成 HTML 文档失败: " + e.getMessage());
        }
    }
    
    /**
     * 生成 OpenAPI 3.0 规范的 JSON 文档
     */
    @GetMapping("/docs/openapi")
    public ResponseEntity<Map<String, Object>> generateOpenApiSpec() {
        try {
            Map<String, Object> openApi = apiDocsGenerator.generateOpenApiSpec();
            return ResponseEntity.ok(openApi);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "生成 OpenAPI 规范失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 下载 API 文档文件
     */
    @GetMapping("/docs/download/{format}")
    public ResponseEntity<String> downloadDocs(@PathVariable String format) {
        try {
            String content;
            String filename;
            String contentType;
            
            switch (format.toLowerCase()) {
                case "markdown":
                    content = apiDocsGenerator.generateMarkdownDocs();
                    filename = "api-docs.md";
                    contentType = "text/markdown";
                    break;
                case "html":
                    content = apiDocsGenerator.generateHtmlDocs();
                    filename = "api-docs.html";
                    contentType = "text/html";
                    break;
                case "json":
                    Map<String, Object> openApi = apiDocsGenerator.generateOpenApiSpec();
                    content = new com.fasterxml.jackson.databind.ObjectMapper()
                        .writerWithDefaultPrettyPrinter()
                        .writeValueAsString(openApi);
                    filename = "openapi-spec.json";
                    contentType = "application/json";
                    break;
                default:
                    return ResponseEntity.badRequest().body("不支持的文档格式: " + format);
            }
            
            return ResponseEntity.ok()
                .header("Content-Type", contentType + "; charset=utf-8")
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(content);
                
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("下载文档失败: " + e.getMessage());
        }
    }

    /**
     * 获取告警统计信息
     */
    @GetMapping("/alerts/stats")
    public Map<String, Object> getAlertStats() {
        Map<String, Object> result = new HashMap<>();
        result.put("alerts", alertManager.getAlertStats());
        result.put("timestamp", LocalDateTime.now().toString());
        return result;
    }

    /**
     * 获取最近的告警记录
     */
    @GetMapping("/alerts/recent")
    public Map<String, Object> getRecentAlerts(@RequestParam(defaultValue = "10") int limit) {
        Map<String, Object> result = new HashMap<>();
        result.put("alerts", alertManager.getRecentAlerts(limit));
        result.put("timestamp", LocalDateTime.now().toString());
        return result;
    }

    /**
     * 获取错误统计信息
     */
    @GetMapping("/errors/stats")
    public Map<String, Object> getErrorStats() {
        Map<String, Object> result = new HashMap<>();
        result.put("errors", errorMonitor.getErrorStats());
        result.put("timestamp", LocalDateTime.now().toString());
        return result;
    }

    /**
     * 获取最近的错误记录
     */
    @GetMapping("/errors/recent")
    public Map<String, Object> getRecentErrors(@RequestParam(defaultValue = "20") int limit) {
        Map<String, Object> result = new HashMap<>();
        result.put("errors", errorMonitor.getRecentErrors(limit));
        result.put("timestamp", LocalDateTime.now().toString());
        return result;
    }

    /**
     * 获取错误趋势分析
     */
    @GetMapping("/errors/trends")
    public Map<String, Object> getErrorTrends() {
        Map<String, Object> result = new HashMap<>();
        result.put("trends", errorMonitor.getErrorTrends());
        result.put("timestamp", LocalDateTime.now().toString());
        return result;
    }

    /**
     * 获取系统健康状态
     */
    @GetMapping("/health")
    public Map<String, Object> getSystemHealth() {
        Map<String, Object> result = new HashMap<>();
        
        // 获取性能指标
        PerformanceStats perfStats = performanceMonitor.getPerformanceStats();
        
        // 获取告警状态
        Map<String, Object> alertStats = alertManager.getAlertStats();
        
        // 计算健康分数 (0-100)
        int healthScore = 100;
        
        // 根据告警数量扣分
        long criticalAlerts = (Long) alertStats.get("criticalAlerts");
        long warningAlerts = (Long) alertStats.get("warningAlerts");
        
        if (criticalAlerts > 0) {
            healthScore -= Math.min(50, criticalAlerts * 10); // 每个严重告警扣10分，最多扣50分
        }
        if (warningAlerts > 0) {
            healthScore -= Math.min(30, warningAlerts * 3); // 每个警告告警扣3分，最多扣30分
        }
        
        // 根据响应时间扣分
        if (perfStats.getAverageResponseTime() > 5000) {
            healthScore -= Math.min(20, (perfStats.getAverageResponseTime() - 5000) / 1000 * 5);
        }
        
        // 根据内存使用率扣分
        if (perfStats.getSystemMetrics().getHeapUsage() > 80) {
            healthScore -= Math.min(20, (perfStats.getSystemMetrics().getHeapUsage() - 80) * 2);
        }
        
        healthScore = Math.max(0, healthScore);
        
        result.put("healthScore", healthScore);
        result.put("status", healthScore >= 80 ? "HEALTHY" : healthScore >= 50 ? "WARNING" : "CRITICAL");
        result.put("performance", perfStats);
        result.put("alerts", alertStats);
        result.put("timestamp", LocalDateTime.now().toString());
        
        return result;
    }

    /**
     * 获取接口参数信息
     */
    @GetMapping("/api/{className}/{methodName}/parameters")
    public Map<String, Object> getApiParameters(@PathVariable String className, @PathVariable String methodName) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 这里需要根据类名和方法名找到对应的方法
            // 暂时返回示例数据
            List<Map<String, Object>> parameters = new ArrayList<>();
            
            // 示例参数
            Map<String, Object> param1 = new HashMap<>();
            param1.put("name", "id");
            param1.put("type", "number");
            param1.put("description", "用户ID");
            param1.put("defaultValue", "1");
            param1.put("required", true);
            param1.put("example", "123");
            parameters.add(param1);
            
            Map<String, Object> param2 = new HashMap<>();
            param2.put("name", "name");
            param2.put("type", "string");
            param2.put("description", "用户名称");
            param2.put("defaultValue", "");
            param2.put("required", false);
            param2.put("example", "张三");
            parameters.add(param2);
            
            result.put("parameters", parameters);
            result.put("success", true);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        result.put("timestamp", LocalDateTime.now().toString());
        return result;
    }

    /**
     * 执行接口测试
     */
    @PostMapping("/api/test")
    public Map<String, Object> testApi(@RequestBody Map<String, Object> testRequest) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String endpoint = (String) testRequest.get("endpoint");
            String method = (String) testRequest.get("method");
            @SuppressWarnings("unchecked")
            Map<String, Object> parameters = (Map<String, Object>) testRequest.get("parameters");
            
            if (endpoint == null || method == null) {
                result.put("success", false);
                result.put("error", "缺少必要参数：endpoint 或 method");
                return result;
            }
            
            TestResult testResult = apiTester.testApi(endpoint, method, parameters);
            
            Map<String, Object> testResultMap = new HashMap<>();
            testResultMap.put("endpoint", testResult.getEndpoint());
            testResultMap.put("method", testResult.getMethod());
            testResultMap.put("parameters", testResult.getParameters());
            testResultMap.put("success", testResult.isSuccess());
            testResultMap.put("statusCode", testResult.getStatusCode());
            testResultMap.put("responseBody", testResult.getResponseBody());
            testResultMap.put("duration", testResult.getDuration());
            testResultMap.put("errorMessage", testResult.getErrorMessage());
            
            result.put("testResult", testResultMap);
            result.put("success", true);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        result.put("timestamp", LocalDateTime.now().toString());
        return result;
    }

    /**
     * 批量测试接口
     */
    @PostMapping("/api/test/batch")
    public Map<String, Object> batchTestApis(@RequestBody List<Map<String, Object>> testRequests) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<ApiTestRequest> apiTestRequests = new ArrayList<>();
            
            for (Map<String, Object> testRequest : testRequests) {
                ApiTestRequest apiTestRequest = new ApiTestRequest();
                apiTestRequest.setEndpoint((String) testRequest.get("endpoint"));
                apiTestRequest.setMethod((String) testRequest.get("method"));
                @SuppressWarnings("unchecked")
                Map<String, Object> parameters = (Map<String, Object>) testRequest.get("parameters");
                apiTestRequest.setParameters(parameters);
                apiTestRequest.setDescription((String) testRequest.get("description"));
                apiTestRequest.setTestCaseName((String) testRequest.get("testCaseName"));
                
                apiTestRequests.add(apiTestRequest);
            }
            
            List<TestResult> testResults = apiTester.batchTestApis(apiTestRequests);
            
            List<Map<String, Object>> testResultMaps = new ArrayList<>();
            for (TestResult testResult : testResults) {
                Map<String, Object> testResultMap = new HashMap<>();
                testResultMap.put("endpoint", testResult.getEndpoint());
                testResultMap.put("method", testResult.getMethod());
                testResultMap.put("parameters", testResult.getParameters());
                testResultMap.put("success", testResult.isSuccess());
                testResultMap.put("statusCode", testResult.getStatusCode());
                testResultMap.put("responseBody", testResult.getResponseBody());
                testResultMap.put("duration", testResult.getDuration());
                testResultMap.put("errorMessage", testResult.getErrorMessage());
                
                testResultMaps.add(testResultMap);
            }
            
            result.put("testResults", testResultMaps);
            result.put("success", true);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        result.put("timestamp", LocalDateTime.now().toString());
        return result;
    }

    /**
     * 生成测试用例
     */
    @GetMapping("/api/{className}/{methodName}/testcases")
    public Map<String, Object> generateTestCases(@PathVariable String className, @PathVariable String methodName) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 这里需要根据类名和方法名找到对应的方法
            // 暂时返回示例测试用例
            List<Map<String, Object>> testCases = new ArrayList<>();
            
            // 基础测试用例
            Map<String, Object> baseCase = new HashMap<>();
            baseCase.put("testCaseName", "基础测试");
            baseCase.put("description", "使用默认参数值进行测试");
            baseCase.put("endpoint", "/test/api/users");
            baseCase.put("method", "GET");
            Map<String, Object> baseParams = new HashMap<>();
            baseParams.put("id", "1");
            baseParams.put("name", "张三");
            baseCase.put("parameters", baseParams);
            testCases.add(baseCase);
            
            // 边界值测试用例
            Map<String, Object> boundaryCase = new HashMap<>();
            boundaryCase.put("testCaseName", "边界值测试");
            boundaryCase.put("description", "测试参数边界值");
            boundaryCase.put("endpoint", "/test/api/users");
            boundaryCase.put("method", "GET");
            Map<String, Object> boundaryParams = new HashMap<>();
            boundaryParams.put("id", "999999");
            boundaryParams.put("name", "");
            boundaryCase.put("parameters", boundaryParams);
            testCases.add(boundaryCase);
            
            result.put("testCases", testCases);
            result.put("success", true);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        result.put("timestamp", LocalDateTime.now().toString());
        return result;
    }
}
