package com.hibiscus.docs.core;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;
import java.util.Map;

public class RequestInfo {
    private String className;
    private String methodName;
    private List<String> paths;

//    @JsonIgnore
    private Map<String, Object> parameters;
    private String methodType;
    
    // 新增字段用于接口文档
    private String description;
    private String summary;
    private List<String> tags;
    private String requestBodySchema;
    private String responseSchema;
    private List<String> produces;
    private List<String> consumes;
    private boolean deprecated;
    private String version;

    public RequestInfo(String className, String methodName, List<String> paths,
                       Map<String, Object> parameters, String methodType) {
        this.className = className;
        this.methodName = methodName;
        this.paths = paths;
        this.parameters = parameters;
        this.methodType = methodType;
    }

    // Getters and Setters
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }

    public List<String> getPaths() { return paths; }
    public void setPaths(List<String> paths) { this.paths = paths; }

    public Map<String, Object> getParameters() { return parameters; }
    public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }

    public String getMethodType() { return methodType; }
    public void setMethodType(String methodType) { this.methodType = methodType; }
    
    // 新增字段的 getter 和 setter
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    
    public String getRequestBodySchema() { return requestBodySchema; }
    public void setRequestBodySchema(String requestBodySchema) { this.requestBodySchema = requestBodySchema; }
    
    public String getResponseSchema() { return responseSchema; }
    public void setResponseSchema(String responseSchema) { this.responseSchema = responseSchema; }
    
    public List<String> getProduces() { return produces; }
    public void setProduces(List<String> produces) { this.produces = produces; }
    
    public List<String> getConsumes() { return consumes; }
    public void setConsumes(List<String> consumes) { this.consumes = consumes; }
    
    public boolean isDeprecated() { return deprecated; }
    public void setDeprecated(boolean deprecated) { this.deprecated = deprecated; }
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    @Override
    public String toString() {
        return "RequestInfo{" +
                "className='" + className + '\'' +
                ", methodName='" + methodName + '\'' +
                ", paths=" + paths +
                ", parameters=" + parameters +
                ", methodType='" + methodType + '\'' +
                '}';
    }
}
