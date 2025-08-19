package com.hibiscus.docs.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API参数注解
 * 用于标记接口参数的信息，包括默认值、描述、类型等
 */
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiParam {
    
    /**
     * 参数名称
     */
    String name() default "";
    
    /**
     * 参数描述
     */
    String description() default "";
    
    /**
     * 参数类型
     */
    String type() default "string";
    
    /**
     * 默认值
     */
    String defaultValue() default "";
    
    /**
     * 是否必填
     */
    boolean required() default false;
    
    /**
     * 示例值
     */
    String example() default "";
    
    /**
     * 参数格式（如：email, date, uuid等）
     */
    String format() default "";
    
    /**
     * 最小值（用于数字类型）
     */
    String min() default "";
    
    /**
     * 最大值（用于数字类型）
     */
    String max() default "";
    
    /**
     * 正则表达式验证
     */
    String pattern() default "";
    
    /**
     * 枚举值（用逗号分隔）
     */
    String[] allowedValues() default {};
}
