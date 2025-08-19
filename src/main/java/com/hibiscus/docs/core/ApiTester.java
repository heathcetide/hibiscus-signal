package com.hibiscus.docs.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * API接口测试器
 * 提供自动接口测试功能
 */
@Component
public class ApiTester {

    @Autowired
    private ParameterParser parameterParser;

    @Autowired
    private ErrorMonitor errorMonitor;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    
    // 测试结果缓存
    private final Map<String, TestResult> testResultCache = new ConcurrentHashMap<>();

    /**
     * 执行接口测试
     */
    public TestResult testApi(String endpoint, String method, Map<String, Object> parameters) {
        String cacheKey = method + ":" + endpoint + ":" + parameters.hashCode();
        
        // 检查缓存
        if (testResultCache.containsKey(cacheKey)) {
            return testResultCache.get(cacheKey);
        }

        TestResult result = new TestResult();
        result.setEndpoint(endpoint);
        result.setMethod(method);
        result.setParameters(parameters);
        result.setStartTime(System.currentTimeMillis());

        try {
            // 构建请求
            HttpEntity<?> requestEntity = buildRequest(method, parameters);
            
            // 执行请求
            ResponseEntity<String> response = executeRequest(endpoint, method, requestEntity);
            
            // 处理响应
            processResponse(result, response);
            
        } catch (Exception e) {
            handleTestError(result, e);
        }

        result.setEndTime(System.currentTimeMillis());
        result.setDuration(result.getEndTime() - result.getStartTime());

        // 缓存结果
        testResultCache.put(cacheKey, result);
        
        return result;
    }

    /**
     * 批量测试接口
     */
    public List<TestResult> batchTestApis(List<ApiTestRequest> testRequests) {
        List<CompletableFuture<TestResult>> futures = new ArrayList<>();
        
        for (ApiTestRequest testRequest : testRequests) {
            CompletableFuture<TestResult> future = CompletableFuture.supplyAsync(() -> 
                testApi(testRequest.getEndpoint(), testRequest.getMethod(), testRequest.getParameters()),
                executorService
            );
            futures.add(future);
        }

        // 等待所有测试完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        List<TestResult> results = new ArrayList<>();
        for (CompletableFuture<TestResult> future : futures) {
            try {
                results.add(future.get());
            } catch (Exception e) {
                // 处理异常
                TestResult errorResult = new TestResult();
                errorResult.setSuccess(false);
                errorResult.setErrorMessage("测试执行异常: " + e.getMessage());
                results.add(errorResult);
            }
        }

        return results;
    }

    /**
     * 自动生成测试用例
     */
    public List<ApiTestRequest> generateTestCases(Method method) {
        List<ApiTestRequest> testCases = new ArrayList<>();
        List<ParameterInfo> parameters = parameterParser.parseMethodParameters(method);
        
        // 生成基础测试用例（使用默认值）
        ApiTestRequest baseCase = new ApiTestRequest();
        baseCase.setEndpoint(""); // 需要从方法注解中获取
        baseCase.setMethod("GET"); // 需要从方法注解中获取
        
        Map<String, Object> baseParams = new HashMap<>();
        for (ParameterInfo param : parameters) {
            if (param.getDefaultValue() != null && !param.getDefaultValue().isEmpty()) {
                baseParams.put(param.getName(), param.getDefaultValue());
            } else if (param.getExample() != null && !param.getExample().isEmpty()) {
                baseParams.put(param.getName(), param.getExample());
            } else {
                // 根据类型生成默认值
                baseParams.put(param.getName(), generateDefaultValue(param));
            }
        }
        baseCase.setParameters(baseParams);
        testCases.add(baseCase);

        // 生成边界值测试用例
        for (ParameterInfo param : parameters) {
            if (param.getMin() != null || param.getMax() != null) {
                ApiTestRequest boundaryCase = new ApiTestRequest();
                boundaryCase.setEndpoint(baseCase.getEndpoint());
                boundaryCase.setMethod(baseCase.getMethod());
                
                Map<String, Object> boundaryParams = new HashMap<>(baseParams);
                if (param.getMin() != null) {
                    boundaryParams.put(param.getName(), param.getMin());
                }
                if (param.getMax() != null) {
                    boundaryParams.put(param.getName(), param.getMax());
                }
                boundaryCase.setParameters(boundaryParams);
                testCases.add(boundaryCase);
            }
        }

        // 生成枚举值测试用例
        for (ParameterInfo param : parameters) {
            if (param.getAllowedValues() != null && !param.getAllowedValues().isEmpty()) {
                for (String allowedValue : param.getAllowedValues()) {
                    ApiTestRequest enumCase = new ApiTestRequest();
                    enumCase.setEndpoint(baseCase.getEndpoint());
                    enumCase.setMethod(baseCase.getMethod());
                    
                    Map<String, Object> enumParams = new HashMap<>(baseParams);
                    enumParams.put(param.getName(), allowedValue);
                    enumCase.setParameters(enumParams);
                    testCases.add(enumCase);
                }
            }
        }

        return testCases;
    }

    /**
     * 构建HTTP请求
     */
    private HttpEntity<?> buildRequest(String method, Map<String, Object> parameters) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        if ("GET".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method)) {
            // GET和DELETE请求通常没有请求体
            return new HttpEntity<>(headers);
        } else {
            // POST、PUT、PATCH请求有请求体
            return new HttpEntity<>(parameters, headers);
        }
    }

    /**
     * 执行HTTP请求
     */
    private ResponseEntity<String> executeRequest(String endpoint, String method, HttpEntity<?> requestEntity) {
        String url = "http://localhost:8080" + endpoint; // 这里需要配置基础URL
        
        switch (method.toUpperCase()) {
            case "GET":
                return restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
            case "POST":
                return restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            case "PUT":
                return restTemplate.exchange(url, HttpMethod.PUT, requestEntity, String.class);
            case "DELETE":
                return restTemplate.exchange(url, HttpMethod.DELETE, requestEntity, String.class);
            case "PATCH":
                return restTemplate.exchange(url, HttpMethod.PATCH, requestEntity, String.class);
            default:
                throw new IllegalArgumentException("不支持的HTTP方法: " + method);
        }
    }

    /**
     * 处理响应
     */
    private void processResponse(TestResult result, ResponseEntity<String> response) {
        result.setStatusCode(response.getStatusCodeValue());
        result.setResponseBody(response.getBody());
        result.setResponseHeaders(response.getHeaders());
        result.setSuccess(response.getStatusCode().is2xxSuccessful());
        
        if (!result.isSuccess()) {
            result.setErrorMessage("HTTP状态码: " + response.getStatusCode());
        }
    }

    /**
     * 处理测试错误
     */
    private void handleTestError(TestResult result, Exception e) {
        result.setSuccess(false);
        result.setErrorMessage(e.getMessage());
        result.setException(e);
        
        // 记录错误到监控系统
        errorMonitor.recordError(result.getEndpoint(), result.getMethod(), "API_TEST_ERROR", 
                               e.getMessage(), 500, 0, "ApiTester", "localhost");
    }

    /**
     * 生成默认值
     */
    private Object generateDefaultValue(ParameterInfo param) {
        switch (param.getType().toLowerCase()) {
            case "string":
                return "test_value";
            case "number":
            case "int":
            case "long":
            case "double":
            case "float":
                return 0;
            case "boolean":
                return false;
            case "date":
                return new Date().toString();
            case "array":
                return new ArrayList<>();
            case "object":
                return new HashMap<>();
            default:
                return null;
        }
    }

    /**
     * 清理缓存
     */
    public void clearCache() {
        testResultCache.clear();
    }

    /**
     * 关闭执行器
     */
    public void shutdown() {
        executorService.shutdown();
    }
}
