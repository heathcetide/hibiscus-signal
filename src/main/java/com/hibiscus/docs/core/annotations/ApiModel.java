package com.hibiscus.docs.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API模型注解
 * 用于标记请求和响应模型类
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiModel {
    
    /**
     * 模型名称
     */
    String name() default "";
    
    /**
     * 模型描述
     */
    String description() default "";
    
    /**
     * 模型示例（JSON格式）
     */
    String example() default "";
}
