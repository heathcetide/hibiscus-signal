package com.hibiscus.docs.core;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;
import java.lang.annotation.Annotation;
import java.util.Date;

@Component
public class MappingHandler {

    private final List<RequestInfo> requestInfos = new ArrayList<>();
    private final Map<String, Object> parameters = new HashMap<>();

    public List<RequestInfo> getRequestInfos() {
        return new ArrayList<>(requestInfos);
    }

    public void handleClass(Class<?> clazz) {
        System.out.println("正在处理类: " + clazz.getName());
        String classMapping = getClassMapping(clazz);
        System.out.println("类映射路径: " + classMapping);

        Method[] methods = clazz.getDeclaredMethods();
        System.out.println("类中的方法数量: " + methods.length);
        
        long annotatedMethods = Arrays.stream(methods)
                .filter(this::isAnnotatedWithMapping)
                .peek(method -> System.out.println("找到映射方法: " + method.getName()))
                .count();
        
        System.out.println("带映射注解的方法数量: " + annotatedMethods);

        Arrays.stream(methods)
                .filter(this::isAnnotatedWithMapping)
                .forEach(method -> handleMethodMapping(clazz, method, classMapping));
    }

    private String getClassMapping(Class<?> clazz) {
        if (clazz.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
            return requestMapping.value().length > 0 ? requestMapping.value()[0] : "";
        }
        return "";
    }

    private boolean isAnnotatedWithMapping(Method method) {
        return method.isAnnotationPresent(GetMapping.class) ||
                method.isAnnotationPresent(PostMapping.class) ||
                method.isAnnotationPresent(PutMapping.class) ||
                method.isAnnotationPresent(DeleteMapping.class) ||
                method.isAnnotationPresent(PatchMapping.class) ||
                method.isAnnotationPresent(RequestMapping.class);
    }

    private void handleMethodMapping(Class<?> aClass, Method method, String classMapping) {
        System.out.println("正在处理方法: " + method.getName() + " 在类: " + aClass.getName());
        
        String methodType = getMethodType(method);
        System.out.println("方法类型: " + methodType);

        for (String path : getMethodPaths(method)) {
            String fullPath = (classMapping.isEmpty() ? "" : classMapping) + path;
            System.out.println("完整路径: " + fullPath);

            // 为每个路径创建独立的参数集合
            Map<String, Object> methodParameters = new HashMap<>();

            // ✅ 先填充参数
            handleMethodParameters(method, methodParameters);
            System.out.println("方法参数: " + methodParameters);

            // ✅ 再创建 RequestInfo，确保参数已填充
            RequestInfo requestInfo = new RequestInfo(
                    aClass.getName(),
                    method.getName(),
                    Collections.singletonList(fullPath),
                    new HashMap<>(methodParameters),
                    methodType
            );
            
            // 增强接口文档信息
            enhanceRequestInfo(requestInfo, aClass, method);
            requestInfos.add(requestInfo);
            
            System.out.println("已添加 RequestInfo: " + requestInfo.getClassName() + "." + requestInfo.getMethodName() + " -> " + fullPath);
        }
    }

    private String getMethodType(Method method) {
        if (method.isAnnotationPresent(GetMapping.class)) return "GET";
        if (method.isAnnotationPresent(PostMapping.class)) return "POST";
        if (method.isAnnotationPresent(PutMapping.class)) return "PUT";
        if (method.isAnnotationPresent(DeleteMapping.class)) return "DELETE";
        if (method.isAnnotationPresent(PatchMapping.class)) return "PATCH";
        if (method.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
            if (requestMapping.method().length > 0) {
                return requestMapping.method()[0].name();
            }
            return "GET"; // 默认方法
        }
        return "";
    }

    private List<String> getMethodPaths(Method method) {
        List<String> paths = new ArrayList<>();
        
        if (method.isAnnotationPresent(GetMapping.class)) {
            GetMapping annotation = method.getAnnotation(GetMapping.class);
            if (annotation.value().length > 0) {
                paths.addAll(Arrays.asList(annotation.value()));
            } else {
                paths.add(""); // 空路径
            }
        } else if (method.isAnnotationPresent(PostMapping.class)) {
            PostMapping annotation = method.getAnnotation(PostMapping.class);
            if (annotation.value().length > 0) {
                paths.addAll(Arrays.asList(annotation.value()));
            } else {
                paths.add(""); // 空路径
            }
        } else if (method.isAnnotationPresent(PutMapping.class)) {
            PutMapping annotation = method.getAnnotation(PutMapping.class);
            if (annotation.value().length > 0) {
                paths.addAll(Arrays.asList(annotation.value()));
            } else {
                paths.add(""); // 空路径
            }
        } else if (method.isAnnotationPresent(DeleteMapping.class)) {
            DeleteMapping annotation = method.getAnnotation(DeleteMapping.class);
            if (annotation.value().length > 0) {
                paths.addAll(Arrays.asList(annotation.value()));
            } else {
                paths.add(""); // 空路径
            }
        } else if (method.isAnnotationPresent(PatchMapping.class)) {
            PatchMapping annotation = method.getAnnotation(PatchMapping.class);
            if (annotation.value().length > 0) {
                paths.addAll(Arrays.asList(annotation.value()));
            } else {
                paths.add(""); // 空路径
            }
        } else if (method.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping annotation = method.getAnnotation(RequestMapping.class);
            if (annotation.value().length > 0) {
                paths.addAll(Arrays.asList(annotation.value()));
            } else {
                paths.add(""); // 空路径
            }
        }
        
        // 如果没有找到任何路径，添加默认路径
        if (paths.isEmpty()) {
            paths.add("");
        }
        
        System.out.println("方法 " + method.getName() + " 的路径: " + paths);
        return paths;
    }

    /**
     * 处理没有注解的参数
     */
    private void handleUnannotatedParameter(Parameter parameter, String paramName, Class<?> paramType, Map<String, Object> methodParameters) {
        methodParameters.put(paramName, paramType.getSimpleName());
        methodParameters.put(paramName + "_required", false); // 默认非必填
        methodParameters.put(paramName + "_type", "unannotated");
    }

    /**
     * 获取参数名，优先从注解获取，否则使用反射获取
     */
    private String getParameterName(Parameter parameter) {
        // 尝试从 RequestParam 注解获取 name
        if (parameter.isAnnotationPresent(RequestParam.class)) {
            RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
            if (!requestParam.name().isEmpty()) {
                return requestParam.name();
            }
        }
        
        // 尝试从 PathVariable 注解获取 name
        if (parameter.isAnnotationPresent(PathVariable.class)) {
            PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
            if (!pathVariable.name().isEmpty()) {
                return pathVariable.name();
            }
        }
        
        // 使用反射获取参数名，如果失败则使用智能推断
        String paramName = parameter.getName();
        if (paramName == null || paramName.isEmpty() || paramName.startsWith("arg")) {
            // 如果参数名为空或者是默认的 arg0，使用智能推断
            // 这里需要传入方法信息，所以暂时返回一个占位符，在 handleMethodParameters 中处理
            return "smart_infer";
        }
        
        return paramName;
    }
    
    /**
     * 推断参数名，基于参数类型和位置
     */
    private String inferParameterName(Parameter parameter) {
        Class<?> paramType = parameter.getType();
        String typeName = paramType.getSimpleName().toLowerCase();
        
        // 基于常见类型推断参数名，添加位置信息避免重复
        if (paramType == String.class) {
            return "stringParam";
        } else if (paramType == Integer.class || paramType == int.class) {
            return "intParam";
        } else if (paramType == Long.class || paramType == long.class) {
            return "longParam";
        } else if (paramType == Boolean.class || paramType == boolean.class) {
            return "boolParam";
        } else if (paramType == Double.class || paramType == double.class) {
            return "doubleParam";
        } else if (paramType == Float.class || paramType == float.class) {
            return "floatParam";
        } else if (paramType == Date.class) {
            return "dateParam";
        } else if (paramType.isArray()) {
            return "arrayParam";
        } else if (Collection.class.isAssignableFrom(paramType)) {
            return "collectionParam";
        } else if (Map.class.isAssignableFrom(paramType)) {
            return "mapParam";
        } else {
            // 对于自定义类，使用类名的小写形式
            return typeName + "Param";
        }
    }

    /**
     * 智能推断参数名，基于方法名和参数类型
     */
    private String smartInferParameterName(Parameter parameter, Method method, int parameterIndex) {
        Class<?> paramType = parameter.getType();
        String methodName = method.getName().toLowerCase();
        
        // 基于方法名推断参数名，但优先考虑参数位置避免重复
        if (methodName.contains("get") || methodName.contains("find") || methodName.contains("search")) {
            if (paramType == String.class) {
                // 对于搜索方法，第一个 String 参数是 keyword，后续的是其他描述性名称
                if (parameterIndex == 0) {
                    return "keyword";
                } else if (parameterIndex == 1) {
                    return "filter";
                } else if (parameterIndex == 2) {
                    return "sort";
                } else {
                    return "param" + (parameterIndex + 1);
                }
            } else if (paramType == Integer.class || paramType == int.class || 
                       paramType == Long.class || paramType == long.class) {
                if (parameterIndex == 0) {
                    return "id";
                } else {
                    return "page";
                }
            } else if (paramType == Boolean.class || paramType == boolean.class) {
                return "enabled";
            }
        } else if (methodName.contains("create") || methodName.contains("add") || methodName.contains("save")) {
            if (paramType == String.class) {
                if (parameterIndex == 0) {
                    return "name";
                } else if (parameterIndex == 1) {
                    return "description";
                } else {
                    return "field" + (parameterIndex + 1);
                }
            } else if (paramType == Integer.class || paramType == int.class) {
                if (parameterIndex == 0) {
                    return "value";
                } else {
                    return "count";
                }
            }
        } else if (methodName.contains("update") || methodName.contains("modify")) {
            if (paramType == String.class) {
                if (parameterIndex == 0) {
                    return "name";
                } else {
                    return "newValue";
                }
            } else if (paramType == Integer.class || paramType == int.class) {
                if (parameterIndex == 0) {
                    return "id";
                } else {
                    return "version";
                }
            }
        } else if (methodName.contains("delete") || methodName.contains("remove")) {
            if (paramType == Integer.class || paramType == int.class || 
                paramType == Long.class || paramType == long.class) {
                return "id";
            }
        }
        
        // 基于参数类型推断，始终添加位置信息避免重复
        if (paramType == String.class) {
            return "string" + (parameterIndex + 1);
        } else if (paramType == Integer.class || paramType == int.class) {
            return "int" + (parameterIndex + 1);
        } else if (paramType == Long.class || paramType == long.class) {
            return "long" + (parameterIndex + 1);
        } else if (paramType == Boolean.class || paramType == boolean.class) {
            return "bool" + (parameterIndex + 1);
        } else if (paramType == Double.class || paramType == double.class) {
            return "double" + (parameterIndex + 1);
        } else if (paramType == Float.class || paramType == float.class) {
            return "float" + (parameterIndex + 1);
        } else if (paramType == Date.class) {
            return "date" + (parameterIndex + 1);
        } else if (paramType.isArray()) {
            return "array" + (parameterIndex + 1);
        } else if (Collection.class.isAssignableFrom(paramType)) {
            return "collection" + (parameterIndex + 1);
        } else if (Map.class.isAssignableFrom(paramType)) {
            return "map" + (parameterIndex + 1);
        } else {
            // 对于自定义类，使用类名的小写形式
            return paramType.getSimpleName().toLowerCase() + (parameterIndex + 1);
        }
    }

    private void handleMethodParameters(Method method, Map<String, Object> methodParameters) {
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            String paramName = getParameterName(parameter);
            
            // 如果参数名是智能推断占位符，使用智能推断
            if ("smart_infer".equals(paramName)) {
                paramName = smartInferParameterName(parameter, method, i);
            }
            
            // 如果参数名重复，添加位置后缀
            String finalParamName = paramName;
            int suffix = 1;
            while (methodParameters.containsKey(finalParamName)) {
                finalParamName = paramName + suffix;
                suffix++;
            }
            
            handleParameter(parameter, finalParamName, methodParameters);
        }
    }

    private void handleParameter(Parameter parameter, String paramName, Map<String, Object> methodParameters) {
        Class<?> paramType = parameter.getType();

        if (parameter.isAnnotationPresent(RequestParam.class)) {
            handleRequestParam(parameter, paramName, paramType, methodParameters);
        } else if (parameter.isAnnotationPresent(PathVariable.class)) {
            handlePathVariable(parameter, paramName, paramType, methodParameters);
        } else if (parameter.isAnnotationPresent(RequestBody.class)) {
            handleRequestBody(paramType, methodParameters);
        } else {
            // 处理没有注解的参数
            handleUnannotatedParameter(parameter, paramName, paramType, methodParameters);
        }
    }

    private void handleRequestParam(Parameter parameter, String paramName, Class<?> paramType, Map<String, Object> methodParameters) {
        RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
        String defaultValue = requestParam.defaultValue();
        
        System.out.println("处理 @RequestParam: " + paramName + ", 原始 defaultValue: '" + defaultValue + "'");
        
        methodParameters.put(paramName, paramType.getSimpleName());
        methodParameters.put(paramName + "_required", requestParam.required());
        
        // 只有当默认值不是空字符串且不是默认的占位符时才添加
        if (defaultValue != null && !defaultValue.isEmpty() && !defaultValue.trim().isEmpty() && 
            !defaultValue.equals("\\u0000") && !defaultValue.equals("\u0000")) {
            
            String cleanDefaultValue = defaultValue.trim();
            // 检查是否包含不可见字符
            if (cleanDefaultValue.matches(".*[\\p{Cntrl}].*")) {
                System.out.println("检测到不可见字符，跳过 defaultValue: " + cleanDefaultValue);
            } else {
                methodParameters.put(paramName + "_defaultValue", cleanDefaultValue);
                System.out.println("设置 defaultValue: " + cleanDefaultValue);
            }
        } else {
            System.out.println("跳过无效的 defaultValue: '" + defaultValue + "'");
        }
    }

    private void handlePathVariable(Parameter parameter, String paramName, Class<?> paramType, Map<String, Object> methodParameters) {
        PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
        methodParameters.put(paramName, paramType.getSimpleName());
        methodParameters.put(paramName + "_required", pathVariable.required());
    }

    private void handleRequestBody(Class<?> paramType, Map<String, Object> methodParameters) {
        if (paramType != null) {
            methodParameters.put("body", paramType.getSimpleName());
            
            // 只处理自定义类，跳过 Java 核心类
            if (!isJavaCoreClass(paramType)) {
                List<String> relevantFields = Arrays.stream(paramType.getDeclaredFields())
                        .filter(this::isRelevantField)
                        .map(Field::getName)
                        .collect(Collectors.toList());
                
                if (!relevantFields.isEmpty()) {
                    methodParameters.put("bodyFields", relevantFields);
                }
            }
        }
    }
    
    /**
     * 判断是否为 Java 核心类
     */
    private boolean isJavaCoreClass(Class<?> clazz) {
        Package pkg = clazz.getPackage();
        if (pkg == null) {
            return false;
        }
        String packageName = pkg.getName();
        return packageName.startsWith("java.") || 
               packageName.startsWith("javax.") || 
               packageName.startsWith("sun.") ||
               packageName.startsWith("com.sun.");
    }
    
    /**
     * 判断字段是否相关（过滤掉内部字段）
     */
    private boolean isRelevantField(Field field) {
        String fieldName = field.getName();
        int modifiers = field.getModifiers();
        
        // 过滤掉静态字段、合成字段、内部字段
        return !java.lang.reflect.Modifier.isStatic(modifiers) &&
               !field.isSynthetic() &&
               !fieldName.contains("$") &&
               !fieldName.equals("serialVersionUID") &&
               !fieldName.equals("serialPersistentFields");
    }
    
    /**
     * 增强 RequestInfo 的接口文档信息
     */
    private void enhanceRequestInfo(RequestInfo requestInfo, Class<?> clazz, Method method) {
        // 尝试从方法注解中提取文档信息
        extractMethodDocumentation(requestInfo, method);
        
        // 尝试从类注解中提取文档信息
        extractClassDocumentation(requestInfo, clazz);
        
        // 设置默认值
        setDefaultValues(requestInfo);
    }
    
    /**
     * 从方法注解中提取文档信息
     */
    private void extractMethodDocumentation(RequestInfo requestInfo, Method method) {
        // 检查是否有 @ApiOperation 或类似的文档注解
        Annotation[] annotations = method.getAnnotations();
        for (Annotation annotation : annotations) {
            String annotationName = annotation.annotationType().getSimpleName();
            
            // 这里可以根据实际的文档注解来提取信息
            // 例如 Swagger 的 @ApiOperation, @ApiParam 等
            if (annotationName.contains("ApiOperation") || annotationName.contains("Operation")) {
                // 尝试提取描述信息
                try {
                    Method valueMethod = annotation.annotationType().getMethod("value");
                    if (valueMethod != null) {
                        String value = (String) valueMethod.invoke(annotation);
                        if (value != null && !value.isEmpty()) {
                            requestInfo.setSummary(value);
                        }
                    }
                    
                    Method notesMethod = annotation.annotationType().getMethod("notes");
                    if (notesMethod != null) {
                        String notes = (String) notesMethod.invoke(annotation);
                        if (notes != null && !notes.isEmpty()) {
                            requestInfo.setDescription(notes);
                        }
                    }
                } catch (Exception e) {
                    // 忽略反射异常
                }
            }
        }
    }
    
    /**
     * 从类注解中提取文档信息
     */
    private void extractClassDocumentation(RequestInfo requestInfo, Class<?> clazz) {
        // 检查类级别的文档注解
        Annotation[] annotations = clazz.getAnnotations();
        for (Annotation annotation : annotations) {
            String annotationName = annotation.annotationType().getSimpleName();
            
            if (annotationName.contains("Api") || annotationName.contains("Tag")) {
                // 尝试提取标签信息
                try {
                    Method tagsMethod = annotation.annotationType().getMethod("tags");
                    if (tagsMethod != null) {
                        String[] tags = (String[]) tagsMethod.invoke(annotation);
                        if (tags != null && tags.length > 0) {
                            requestInfo.setTags(Arrays.asList(tags));
                        }
                    }
                } catch (Exception e) {
                    // 忽略反射异常
                }
            }
        }
    }
    
    /**
     * 设置默认值
     */
    private void setDefaultValues(RequestInfo requestInfo) {
        if (requestInfo.getDescription() == null) {
            requestInfo.setDescription("API接口 - " + requestInfo.getMethodName());
        }
        
        if (requestInfo.getSummary() == null) {
            requestInfo.setSummary(requestInfo.getMethodName());
        }
        
        if (requestInfo.getTags() == null) {
            requestInfo.setTags(Arrays.asList("api", "default"));
        }
        
        if (requestInfo.getProduces() == null) {
            requestInfo.setProduces(Arrays.asList("application/json"));
        }
        
        if (requestInfo.getConsumes() == null) {
            if ("POST".equals(requestInfo.getMethodType()) || 
                "PUT".equals(requestInfo.getMethodType()) || 
                "PATCH".equals(requestInfo.getMethodType())) {
                requestInfo.setConsumes(Arrays.asList("application/json"));
            } else {
                requestInfo.setConsumes(Arrays.asList("*/*"));
            }
        }
        
        if (requestInfo.getVersion() == null) {
            requestInfo.setVersion("1.0");
        }
    }
}