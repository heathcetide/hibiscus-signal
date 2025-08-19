package com.hibiscus.docs.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Component
public class ApiDocsGenerator {

    @Autowired
    private MappingHandler mappingHandler;

    /**
     * 生成 Markdown 格式的 API 文档
     */
    public String generateMarkdownDocs() {
        List<RequestInfo> apis = mappingHandler.getRequestInfos();
        if (apis.isEmpty()) {
            return "# API 文档\n\n暂无 API 接口信息。";
        }

        StringBuilder markdown = new StringBuilder();
        markdown.append("# API 接口文档\n\n");
        markdown.append("## 概述\n\n");
        markdown.append("- 总接口数: ").append(apis.size()).append("\n");
        markdown.append("- 生成时间: ").append(java.time.LocalDateTime.now()).append("\n\n");

        // 按控制器分组
        Map<String, List<RequestInfo>> groupedApis = apis.stream()
            .collect(Collectors.groupingBy(RequestInfo::getClassName));

        for (Map.Entry<String, List<RequestInfo>> entry : groupedApis.entrySet()) {
            String controllerName = entry.getKey();
            List<RequestInfo> controllerApis = entry.getValue();

            markdown.append("## ").append(controllerName).append("\n\n");
            markdown.append("### 接口列表\n\n");

            for (RequestInfo api : controllerApis) {
                markdown.append("#### ").append(api.getSummary() != null ? api.getSummary() : api.getMethodName()).append("\n\n");
                
                // 基本信息
                markdown.append("- **方法**: `").append(api.getMethodType()).append("`\n");
                markdown.append("- **路径**: `").append(api.getPaths().get(0)).append("`\n");
                markdown.append("- **描述**: ").append(api.getDescription() != null ? api.getDescription() : "无描述").append("\n");
                
                if (api.getTags() != null && !api.getTags().isEmpty()) {
                    markdown.append("- **标签**: ").append(String.join(", ", api.getTags())).append("\n");
                }
                
                if (api.getVersion() != null) {
                    markdown.append("- **版本**: ").append(api.getVersion()).append("\n");
                }
                
                markdown.append("\n");

                // 参数信息
                if (api.getParameters() != null && !api.getParameters().isEmpty()) {
                    markdown.append("**参数说明:**\n\n");
                    markdown.append("| 参数名 | 类型 | 必需 | 默认值 | 说明 |\n");
                    markdown.append("|--------|------|------|--------|------|\n");
                    
                    api.getParameters().forEach((key, value) -> {
                        if (!key.contains("_") && key != "body" && key != "bodyFields") {
                            String paramName = key;
                            String paramType = value.toString();
                            String required = api.getParameters().get(key + "_required") != null ? 
                                api.getParameters().get(key + "_required").toString() : "false";
                            String defaultValue = api.getParameters().get(key + "_defaultValue") != null ? 
                                api.getParameters().get(key + "_defaultValue").toString() : "-";
                            
                            markdown.append("| ").append(paramName).append(" | ")
                                   .append(paramType).append(" | ")
                                   .append(required).append(" | ")
                                   .append(defaultValue).append(" | - |\n");
                        }
                    });
                    
                    // 请求体信息
                    if (api.getParameters().get("body") != null) {
                        markdown.append("| body | ").append(api.getParameters().get("body")).append(" | - | - | 请求体 |\n");
                        
                        if (api.getParameters().get("bodyFields") != null) {
                            @SuppressWarnings("unchecked")
                            List<String> bodyFields = (List<String>) api.getParameters().get("bodyFields");
                            markdown.append("\n**请求体字段:**\n\n");
                            for (String field : bodyFields) {
                                markdown.append("- `").append(field).append("`\n");
                            }
                            markdown.append("\n");
                        }
                    }
                    
                    markdown.append("\n");
                }

                // 响应信息
                if (api.getProduces() != null && !api.getProduces().isEmpty()) {
                    markdown.append("**响应格式:** ").append(String.join(", ", api.getProduces())).append("\n\n");
                }
                
                markdown.append("---\n\n");
            }
        }

        return markdown.toString();
    }

    /**
     * 生成 OpenAPI 3.0 规范的 JSON 文档
     */
    public Map<String, Object> generateOpenApiSpec() {
        List<RequestInfo> apis = mappingHandler.getRequestInfos();
        
        Map<String, Object> openApi = new HashMap<>();
        openApi.put("openapi", "3.0.0");
        openApi.put("info", createInfo());
        openApi.put("servers", createServers());
        openApi.put("paths", createPaths(apis));
        openApi.put("components", createComponents());
        openApi.put("tags", createTags(apis));
        
        return openApi;
    }

    private Map<String, Object> createInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("title", "Hibiscus API");
        info.put("description", "Hibiscus 项目 API 接口文档");
        info.put("version", "1.0.0");
        
        Map<String, String> contact = new HashMap<>();
        contact.put("name", "Hibiscus Team");
        info.put("contact", contact);
        
        return info;
    }

    private List<Map<String, String>> createServers() {
        List<Map<String, String>> servers = new java.util.ArrayList<>();
        
        Map<String, String> localServer = new HashMap<>();
        localServer.put("url", "http://localhost:8080");
        localServer.put("description", "本地开发环境");
        servers.add(localServer);
        
        Map<String, String> devServer = new HashMap<>();
        devServer.put("url", "https://dev-api.example.com");
        devServer.put("description", "开发测试环境");
        servers.add(devServer);
        
        return servers;
    }

    private Map<String, Object> createPaths(List<RequestInfo> apis) {
        Map<String, Object> paths = new HashMap<>();
        
        for (RequestInfo api : apis) {
            for (String path : api.getPaths()) {
                String pathKey = path.startsWith("/") ? path : "/" + path;
                String method = api.getMethodType().toLowerCase();
                
                if (!paths.containsKey(pathKey)) {
                    paths.put(pathKey, new HashMap<>());
                }
                
                @SuppressWarnings("unchecked")
                Map<String, Object> pathItem = (Map<String, Object>) paths.get(pathKey);
                
                Map<String, Object> operation = new HashMap<>();
                operation.put("summary", api.getSummary() != null ? api.getSummary() : api.getMethodName());
                operation.put("description", api.getDescription() != null ? api.getDescription() : "");
                
                if (api.getTags() != null && !api.getTags().isEmpty()) {
                    operation.put("tags", api.getTags());
                }
                
                if (api.getParameters() != null && !api.getParameters().isEmpty()) {
                    operation.put("parameters", createParameters(api));
                }
                
                if (api.getRequestBodySchema() != null) {
                    operation.put("requestBody", createRequestBody(api));
                }
                
                operation.put("responses", createResponses(api));
                
                pathItem.put(method, operation);
            }
        }
        
        return paths;
    }

    private List<Map<String, Object>> createParameters(RequestInfo api) {
        List<Map<String, Object>> parameters = new java.util.ArrayList<>();
        
        api.getParameters().forEach((key, value) -> {
            if (!key.contains("_") && key != "body" && key != "bodyFields") {
                Map<String, Object> param = new HashMap<>();
                param.put("name", key);
                param.put("in", "query");
                param.put("required", api.getParameters().get(key + "_required") != null ? 
                    Boolean.parseBoolean(api.getParameters().get(key + "_required").toString()) : false);
                param.put("schema", createSchema(getOpenApiType(value.toString())));
                parameters.add(param);
            }
        });
        
        return parameters;
    }

    private Map<String, Object> createSchema(String type) {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", type);
        return schema;
    }

    private Map<String, Object> createRequestBody(RequestInfo api) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("required", true);
        
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        
        if (api.getParameters().get("bodyFields") != null) {
            @SuppressWarnings("unchecked")
            List<String> bodyFields = (List<String>) api.getParameters().get("bodyFields");
            Map<String, Object> properties = new HashMap<>();
            
            for (String field : bodyFields) {
                properties.put(field, createSchema("string"));
            }
            
            schema.put("properties", properties);
        }
        
        content.put("application/json", createContentSchema(schema));
        requestBody.put("content", content);
        
        return requestBody;
    }

    private Map<String, Object> createContentSchema(Map<String, Object> schema) {
        Map<String, Object> contentSchema = new HashMap<>();
        contentSchema.put("schema", schema);
        return contentSchema;
    }

    private Map<String, Object> createResponses(RequestInfo api) {
        Map<String, Object> responses = new HashMap<>();
        
        // 成功响应
        Map<String, Object> successResponse = new HashMap<>();
        successResponse.put("description", "成功响应");
        
        Map<String, Object> content = new HashMap<>();
        content.put("application/json", createContentSchema(createSchema("object")));
        successResponse.put("content", content);
        
        responses.put("200", successResponse);
        
        // 错误响应
        responses.put("400", createErrorResponse("请求参数错误"));
        responses.put("500", createErrorResponse("服务器内部错误"));
        
        return responses;
    }

    private Map<String, Object> createErrorResponse(String description) {
        Map<String, Object> response = new HashMap<>();
        response.put("description", description);
        return response;
    }

    private Map<String, Object> createComponents() {
        Map<String, Object> components = new HashMap<>();
        components.put("schemas", new HashMap<>());
        components.put("securitySchemes", new HashMap<>());
        return components;
    }

    private List<Map<String, String>> createTags(List<RequestInfo> apis) {
        return apis.stream()
            .filter(api -> api.getTags() != null && !api.getTags().isEmpty())
            .flatMap(api -> api.getTags().stream())
            .distinct()
            .map(tag -> {
                Map<String, String> tagMap = new HashMap<>();
                tagMap.put("name", tag);
                tagMap.put("description", tag + " 相关接口");
                return tagMap;
            })
            .collect(Collectors.toList());
    }

    private String getOpenApiType(String javaType) {
        switch (javaType.toLowerCase()) {
            case "string": return "string";
            case "integer": case "int": case "long": return "integer";
            case "double": case "float": return "number";
            case "boolean": case "bool": return "boolean";
            default: return "string";
        }
    }

    /**
     * 生成 HTML 格式的 API 文档
     */
    public String generateHtmlDocs() {
        String markdown = generateMarkdownDocs();
        
        // 简单的 Markdown 到 HTML 转换
        String html = markdown
            .replace("# ", "<h1>").replace("\n# ", "</h1>\n<h1>")
            .replace("## ", "<h2>").replace("\n## ", "</h2>\n<h2>")
            .replace("### ", "<h3>").replace("\n### ", "</h3>\n<h3>")
            .replace("#### ", "<h4>").replace("\n#### ", "</h4>\n<h4>")
            .replace("**", "<strong>").replace("**", "</strong>")
            .replace("`", "<code>").replace("`", "</code>")
            .replace("\n- ", "\n<li>").replace("\n", "</li>\n")
            .replace("---", "<hr>");
        
        return "<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"UTF-8\">\n" +
               "<title>API 文档</title>\n<style>\n" +
               "body { font-family: Arial, sans-serif; margin: 40px; }\n" +
               "h1, h2, h3, h4 { color: #333; }\n" +
               "code { background: #f4f4f4; padding: 2px 4px; border-radius: 3px; }\n" +
               "table { border-collapse: collapse; width: 100%; }\n" +
               "th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n" +
               "th { background-color: #f2f2f2; }\n" +
               "</style>\n</head>\n<body>\n" + html + "\n</body>\n</html>";
    }
}
