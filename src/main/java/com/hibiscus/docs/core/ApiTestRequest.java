package com.hibiscus.docs.core;

import java.util.Map;

/**
 * API测试请求
 * 存储API测试的请求信息
 */
public class ApiTestRequest {
    
    private String endpoint;
    private String method;
    private Map<String, Object> parameters;
    private String description;
    private String testCaseName;

    // 构造函数
    public ApiTestRequest() {}

    // Getter和Setter方法
    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTestCaseName() {
        return testCaseName;
    }

    public void setTestCaseName(String testCaseName) {
        this.testCaseName = testCaseName;
    }

    @Override
    public String toString() {
        return "ApiTestRequest{" +
                "endpoint='" + endpoint + '\'' +
                ", method='" + method + '\'' +
                ", parameters=" + parameters +
                ", description='" + description + '\'' +
                ", testCaseName='" + testCaseName + '\'' +
                '}';
    }
}
