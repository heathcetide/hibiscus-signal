package com.hibiscus.docs.core;

import org.springframework.http.HttpHeaders;
import java.util.Map;

/**
 * 测试结果
 * 存储API测试的执行结果
 */
public class TestResult {
    
    private String endpoint;
    private String method;
    private Map<String, Object> parameters;
    private long startTime;
    private long endTime;
    private long duration;
    private boolean success;
    private int statusCode;
    private String responseBody;
    private HttpHeaders responseHeaders;
    private String errorMessage;
    private Exception exception;

    // 构造函数
    public TestResult() {}

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

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public HttpHeaders getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(HttpHeaders responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    @Override
    public String toString() {
        return "TestResult{" +
                "endpoint='" + endpoint + '\'' +
                ", method='" + method + '\'' +
                ", parameters=" + parameters +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", duration=" + duration +
                ", success=" + success +
                ", statusCode=" + statusCode +
                ", responseBody='" + responseBody + '\'' +
                ", responseHeaders=" + responseHeaders +
                ", errorMessage='" + errorMessage + '\'' +
                ", exception=" + exception +
                '}';
    }
}
