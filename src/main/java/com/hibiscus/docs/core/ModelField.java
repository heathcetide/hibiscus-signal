package com.hibiscus.docs.core;

import java.util.List;

/**
 * 模型字段信息
 * 存储请求/响应模型字段的详细信息
 */
public class ModelField {
    
    private String name;
    private String type;
    private String description;
    private String defaultValue;
    private boolean required;
    private String example;
    private String format;
    private String min;
    private String max;
    private String pattern;
    private List<String> allowedValues;

    // 构造函数
    public ModelField() {}

    // Getter和Setter方法
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    @Override
    public String toString() {
        return "ModelField{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", description='" + description + '\'' +
                ", defaultValue='" + defaultValue + '\'' +
                ", required=" + required +
                ", example='" + example + '\'' +
                ", format='" + format + '\'' +
                ", min='" + min + '\'' +
                ", max='" + max + '\'' +
                ", pattern='" + pattern + '\'' +
                ", allowedValues=" + allowedValues +
                '}';
    }
}
