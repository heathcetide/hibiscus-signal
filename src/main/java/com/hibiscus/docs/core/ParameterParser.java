package com.hibiscus.docs.core;

import com.hibiscus.docs.core.annotations.ApiParam;
import com.hibiscus.docs.core.annotations.ApiModel;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 参数解析器
 * 负责解析方法参数上的注解信息，提取参数详情
 */
@Component
public class ParameterParser {

    // 缓存已解析的参数信息
    private final Map<String, List<ParameterInfo>> parameterCache = new ConcurrentHashMap<>();

    /**
     * 解析方法的参数信息
     */
    public List<ParameterInfo> parseMethodParameters(Method method) {
        String cacheKey = method.getDeclaringClass().getName() + "#" + method.getName();
        
        // 检查缓存
        if (parameterCache.containsKey(cacheKey)) {
            return parameterCache.get(cacheKey);
        }

        List<ParameterInfo> parameters = new ArrayList<>();
        Parameter[] methodParameters = method.getParameters();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        Type[] genericTypes = method.getGenericParameterTypes();

        for (int i = 0; i < methodParameters.length; i++) {
            Parameter parameter = methodParameters[i];
            Annotation[] annotations = parameterAnnotations[i];
            Type genericType = genericTypes[i];

            ParameterInfo paramInfo = parseParameter(parameter, annotations, genericType);
            if (paramInfo != null) {
                parameters.add(paramInfo);
            }
        }

        // 缓存结果
        parameterCache.put(cacheKey, parameters);
        return parameters;
    }

    /**
     * 解析单个参数
     */
    private ParameterInfo parseParameter(Parameter parameter, Annotation[] annotations, Type genericType) {
        ParameterInfo paramInfo = new ParameterInfo();
        
        // 设置基本信息
        paramInfo.setName(parameter.getName());
        paramInfo.setType(getParameterType(parameter.getType(), genericType));
        paramInfo.setRequired(!parameter.isAnnotationPresent(org.springframework.web.bind.annotation.RequestParam.class) || 
                            parameter.getAnnotation(org.springframework.web.bind.annotation.RequestParam.class).required());

        // 解析@ApiParam注解
        ApiParam apiParam = parameter.getAnnotation(ApiParam.class);
        if (apiParam != null) {
            if (!apiParam.name().isEmpty()) {
                paramInfo.setName(apiParam.name());
            }
            paramInfo.setDescription(apiParam.description());
            paramInfo.setType(apiParam.type());
            paramInfo.setDefaultValue(apiParam.defaultValue());
            paramInfo.setRequired(apiParam.required());
            paramInfo.setExample(apiParam.example());
            paramInfo.setFormat(apiParam.format());
            paramInfo.setMin(apiParam.min());
            paramInfo.setMax(apiParam.max());
            paramInfo.setPattern(apiParam.pattern());
            paramInfo.setAllowedValues(Arrays.asList(apiParam.allowedValues()));
        }

        // 解析Spring注解
        parseSpringAnnotations(parameter, annotations, paramInfo);

        return paramInfo;
    }

    /**
     * 解析Spring相关注解
     */
    private void parseSpringAnnotations(Parameter parameter, Annotation[] annotations, ParameterInfo paramInfo) {
        for (Annotation annotation : annotations) {
            String annotationName = annotation.annotationType().getSimpleName();
            
            switch (annotationName) {
                case "RequestParam":
                    org.springframework.web.bind.annotation.RequestParam requestParam = 
                        parameter.getAnnotation(org.springframework.web.bind.annotation.RequestParam.class);
                    if (requestParam != null) {
                        if (!requestParam.value().isEmpty()) {
                            paramInfo.setName(requestParam.value());
                        }
                        paramInfo.setRequired(requestParam.required());
                        if (!requestParam.defaultValue().isEmpty()) {
                            paramInfo.setDefaultValue(requestParam.defaultValue());
                        }
                    }
                    break;
                    
                case "PathVariable":
                    org.springframework.web.bind.annotation.PathVariable pathVariable = 
                        parameter.getAnnotation(org.springframework.web.bind.annotation.PathVariable.class);
                    if (pathVariable != null && !pathVariable.value().isEmpty()) {
                        paramInfo.setName(pathVariable.value());
                    }
                    paramInfo.setRequired(true); // PathVariable通常是必填的
                    break;
                    
                case "RequestHeader":
                    org.springframework.web.bind.annotation.RequestHeader requestHeader = 
                        parameter.getAnnotation(org.springframework.web.bind.annotation.RequestHeader.class);
                    if (requestHeader != null) {
                        if (!requestHeader.value().isEmpty()) {
                            paramInfo.setName(requestHeader.value());
                        }
                        paramInfo.setRequired(requestHeader.required());
                        if (!requestHeader.defaultValue().isEmpty()) {
                            paramInfo.setDefaultValue(requestHeader.defaultValue());
                        }
                    }
                    break;
                    
                case "RequestBody":
                    paramInfo.setBodyParameter(true);
                    // 尝试解析请求体模型
                    parseRequestBodyModel(parameter, paramInfo);
                    break;
            }
        }
    }

    /**
     * 解析请求体模型
     */
    private void parseRequestBodyModel(Parameter parameter, ParameterInfo paramInfo) {
        Class<?> paramType = parameter.getType();
        
        // 检查是否有@ApiModel注解
        ApiModel apiModel = paramType.getAnnotation(ApiModel.class);
        if (apiModel != null) {
            paramInfo.setModelName(apiModel.name().isEmpty() ? paramType.getSimpleName() : apiModel.name());
            paramInfo.setModelDescription(apiModel.description());
            paramInfo.setModelExample(apiModel.example());
        } else {
            paramInfo.setModelName(paramType.getSimpleName());
        }

        // 解析模型字段
        paramInfo.setModelFields(parseModelFields(paramType));
    }

    /**
     * 解析模型字段
     */
    private List<ModelField> parseModelFields(Class<?> modelClass) {
        List<ModelField> fields = new ArrayList<>();
        
        // 获取所有公共字段
        java.lang.reflect.Field[] declaredFields = modelClass.getDeclaredFields();
        for (java.lang.reflect.Field field : declaredFields) {
            if (java.lang.reflect.Modifier.isPublic(field.getModifiers())) {
                ModelField modelField = new ModelField();
                modelField.setName(field.getName());
                modelField.setType(field.getType().getSimpleName());
                
                // 检查字段上的@ApiParam注解
                ApiParam apiParam = field.getAnnotation(ApiParam.class);
                if (apiParam != null) {
                    modelField.setDescription(apiParam.description());
                    modelField.setDefaultValue(apiParam.defaultValue());
                    modelField.setRequired(apiParam.required());
                    modelField.setExample(apiParam.example());
                    modelField.setFormat(apiParam.format());
                    modelField.setMin(apiParam.min());
                    modelField.setMax(apiParam.max());
                    modelField.setPattern(apiParam.pattern());
                    modelField.setAllowedValues(Arrays.asList(apiParam.allowedValues()));
                }
                
                fields.add(modelField);
            }
        }
        
        return fields;
    }

    /**
     * 获取参数类型
     */
    private String getParameterType(Class<?> type, Type genericType) {
        if (type.isPrimitive()) {
            return type.getSimpleName();
        } else if (type == String.class) {
            return "string";
        } else if (type == Integer.class || type == int.class || 
                   type == Long.class || type == long.class ||
                   type == Double.class || type == double.class ||
                   type == Float.class || type == float.class) {
            return "number";
        } else if (type == Boolean.class || type == boolean.class) {
            return "boolean";
        } else if (type == Date.class || type == java.time.LocalDateTime.class ||
                   type == java.time.LocalDate.class) {
            return "date";
        } else if (type.isArray() || Collection.class.isAssignableFrom(type)) {
            return "array";
        } else if (type == Object.class) {
            return "object";
        } else {
            return type.getSimpleName();
        }
    }

    /**
     * 清除缓存
     */
    public void clearCache() {
        parameterCache.clear();
    }

    /**
     * 清除特定方法的缓存
     */
    public void clearMethodCache(String methodKey) {
        parameterCache.remove(methodKey);
    }
}
