package com.hibiscus.docs.core;

import java.util.List;

/**
 * 参数信息
 * 存储接口参数的详细信息
 */
public class ParameterInfo {
    
    private String name;
    private String description;
    private String type;
    private String defaultValue;
    private boolean required;
    private String example;
    private String format;
    private String min;
    private String max;
    private String pattern;
    private List<String> allowedValues;
    private boolean bodyParameter;
    private String modelName;
    private String modelDescription;
    private String modelExample;
    private List<ModelField> modelFields;

    // 构造函数
    public ParameterInfo() {}

    // Getter和Setter方法
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getExample() {
        return example;
    }

    public void setExample(String example) {
        this.example = example;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getMin() {
        return min;
    }

    public void setMin(String min) {
        this.min = min;
    }

    public String getMax() {
        return max;
    }

    public void setMax(String max) {
        this.max = max;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public List<String> getAllowedValues() {
        return allowedValues;
    }

    public void setAllowedValues(List<String> allowedValues) {
        this.allowedValues = allowedValues;
    }

    public boolean isBodyParameter() {
        return bodyParameter;
    }

    public void setBodyParameter(boolean bodyParameter) {
        this.bodyParameter = bodyParameter;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getModelDescription() {
        return modelDescription;
    }

    public void setModelDescription(String modelDescription) {
        this.modelDescription = modelDescription;
    }

    public String getModelExample() {
        return modelExample;
    }

    public void setModelExample(String modelExample) {
        this.modelExample = modelExample;
    }

    public List<ModelField> getModelFields() {
        return modelFields;
    }

    public void setModelFields(List<ModelField> modelFields) {
        this.modelFields = modelFields;
    }

    @Override
    public String toString() {
        return "ParameterInfo{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", type='" + type + '\'' +
                ", defaultValue='" + defaultValue + '\'' +
                ", required=" + required +
                ", example='" + example + '\'' +
                ", format='" + format + '\'' +
                ", min='" + min + '\'' +
                ", max='" + max + '\'' +
                ", pattern='" + pattern + '\'' +
                ", allowedValues=" + allowedValues +
                ", bodyParameter=" + bodyParameter +
                ", modelName='" + modelName + '\'' +
                ", modelDescription='" + modelDescription + '\'' +
                ", modelExample='" + modelExample + '\'' +
                ", modelFields=" + modelFields +
                '}';
    }
}
