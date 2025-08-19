package com.hibiscus.docs.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.List;

@SpringBootApplication
public class ApiTestApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(ApiTestApplication.class, args);
        
        // 获取 MappingHandler 并手动测试
        MappingHandler mappingHandler = context.getBean(MappingHandler.class);
        System.out.println("手动测试 - RequestInfo 数量: " + mappingHandler.getRequestInfos().size());
        
        // 手动处理 TestController
        try {
            Class<?> testControllerClass = Class.forName("com.hibiscus.docs.core.TestController");
            System.out.println("找到 TestController 类: " + testControllerClass.getName());
            mappingHandler.handleClass(testControllerClass);
            System.out.println("手动处理后 - RequestInfo 数量: " + mappingHandler.getRequestInfos().size());
            
            // 重新写入 JSON 文件
            writeRequestInfosToJsonFile(mappingHandler.getRequestInfos(), "requestInfos.json");
            
        } catch (ClassNotFoundException e) {
            System.err.println("找不到 TestController 类: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("写入 JSON 文件失败: " + e.getMessage());
        }
    }
    
    private static void writeRequestInfosToJsonFile(List<RequestInfo> requestInfos, String fileName) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        File file = new File(fileName);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, requestInfos);
        System.out.println("成功写入 JSON 文件: " + file.getAbsolutePath());
        System.out.println("写入的 RequestInfo 数量: " + requestInfos.size());
    }
}
