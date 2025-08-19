package com.hibiscus.docs.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class ClassScanner {

    @Autowired
    private MappingHandler mappingHandler;

    @Autowired
    private AppConfigProperties appConfigProperties;

    private final Lock lock = new ReentrantLock();

    @PostConstruct
    public void init() {
        lock.lock();
        try {
            System.out.println("开始初始化 ClassScanner...");
            
            // 获取扫描路径
            String scanPath = appConfigProperties.getHelper().getScanPath();
            System.out.println("配置的扫描路径: " + scanPath);
            
            // 使用 Reflections 扫描所有带有 Spring 注解的类
            scanSpringClasses(scanPath);
            
            // 也尝试扫描主类
            Class<?> mainClass = findMainClass();
            if (mainClass != null) {
                System.out.println("找到主类: " + mainClass.getName());
                scanApplication(mainClass);
            }
            
            List<RequestInfo> requestInfos = mappingHandler.getRequestInfos();
            System.out.println("扫描到的 RequestInfo 数量: " + requestInfos.size());
            
            writeRequestInfosToJsonFile(requestInfos, "requestInfos.json");
        } finally {
            lock.unlock();
        }
    }

    /**
     * 使用 Reflections 扫描所有带有 Spring 注解的类
     */
    private void scanSpringClasses(String basePackage) {
        try {
            // 如果配置的包路径为空，使用默认包
            if (basePackage == null || basePackage.isEmpty()) {
                basePackage = "com.hibiscus.docs";
            }
            
            System.out.println("开始扫描包: " + basePackage);
            
            // 创建 Reflections 配置
            ConfigurationBuilder config = new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage(basePackage))
                .setScanners(Scanners.TypesAnnotated);
            
            Reflections reflections = new Reflections(config);
            
            // 扫描所有带有 @Controller 注解的类
            Set<Class<?>> controllers = reflections.getTypesAnnotatedWith(Controller.class);
            System.out.println("找到 @Controller 类数量: " + controllers.size());
            for (Class<?> controller : controllers) {
                System.out.println("处理 @Controller 类: " + controller.getName());
                mappingHandler.handleClass(controller);
            }
            
            // 扫描所有带有 @RestController 注解的类
            Set<Class<?>> restControllers = reflections.getTypesAnnotatedWith(RestController.class);
            System.out.println("找到 @RestController 类数量: " + restControllers.size());
            for (Class<?> controller : restControllers) {
                System.out.println("处理 @RestController 类: " + controller.getName());
                mappingHandler.handleClass(controller);
            }
            
            // 扫描所有带有 @Component 注解的类（可能包含其他类型的组件）
            Set<Class<?>> components = reflections.getTypesAnnotatedWith(org.springframework.stereotype.Component.class);
            System.out.println("找到 @Component 类数量: " + components.size());
            for (Class<?> component : components) {
                // 检查是否有 @RequestMapping 相关注解
                if (hasRequestMappingAnnotation(component)) {
                    System.out.println("处理有 @RequestMapping 的 @Component 类: " + component.getName());
                    mappingHandler.handleClass(component);
                }
            }
            
        } catch (Exception e) {
            System.err.println("扫描 Spring 类时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 检查类是否有 @RequestMapping 相关注解
     */
    private boolean hasRequestMappingAnnotation(Class<?> clazz) {
        return clazz.isAnnotationPresent(org.springframework.web.bind.annotation.RequestMapping.class) ||
               clazz.isAnnotationPresent(org.springframework.web.bind.annotation.GetMapping.class) ||
               clazz.isAnnotationPresent(org.springframework.web.bind.annotation.PostMapping.class) ||
               clazz.isAnnotationPresent(org.springframework.web.bind.annotation.PutMapping.class) ||
               clazz.isAnnotationPresent(org.springframework.web.bind.annotation.DeleteMapping.class) ||
               clazz.isAnnotationPresent(org.springframework.web.bind.annotation.PatchMapping.class);
    }

    private void writeRequestInfosToJsonFile(List<RequestInfo> requestInfos, String fileName) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            // 将 List<RequestInfo> 写入 JSON 文件
            File file = new File(fileName);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, requestInfos);
            System.out.println("成功写入 JSON 文件: " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("写入 JSON 文件失败: " + e.getMessage());
        }
    }

    public Class<?> findMainClass() {
        String scanBasePackages = appConfigProperties.getHelper().getScanPath();
        if (scanBasePackages == null || scanBasePackages.isEmpty()) {
            scanBasePackages = "com.hibiscus.docs";
        }

        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage(scanBasePackages))
                .setScanners(Scanners.TypesAnnotated));

        Set<Class<?>> classes = reflections.getTypesAnnotatedWith(SpringBootApplication.class);
        for (Class<?> clazz : classes) {
            if (hasMainMethod(clazz)) {
                return clazz;
            }
        }
        return null;
    }

    private boolean hasMainMethod(Class<?> clazz) {
        try {
            Method method = clazz.getMethod("main", String[].class);
            return method.getReturnType().equals(Void.TYPE);
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    public void scanApplication(Class<?> mainClass) {
        if (mainClass != null && mainClass.isAnnotationPresent(SpringBootApplication.class)) {
            SpringBootApplication annotation = mainClass.getAnnotation(SpringBootApplication.class);
            String[] scanBasePackages = annotation.scanBasePackages();
            if (scanBasePackages.length == 0) {
                scanSpringClasses(mainClass.getPackage().getName());
            } else {
                for (String basePackage : scanBasePackages) {
                    scanSpringClasses(basePackage);
                }
            }
        }
    }

    // 移除旧的扫描方法，因为我们现在使用 Reflections
    // private void scanPackage(String basePackage) { ... }
    // private void scanDirectory(File directory, String basePackage) { ... }
}
