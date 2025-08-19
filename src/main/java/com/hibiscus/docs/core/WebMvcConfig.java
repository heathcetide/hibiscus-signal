package com.hibiscus.docs.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private SecurityInterceptor securityInterceptor;

    @Autowired
    private RequestLoggingInterceptor requestLoggingInterceptor;

    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 添加请求日志拦截器（最先执行）
        if (requestLoggingInterceptor != null) {
            registry.addInterceptor(requestLoggingInterceptor)
                    .addPathPatterns("/**")
                    .excludePathPatterns("/error", "/favicon.ico", "/static/**", "/css/**", "/js/**", "/images/**");
        }

        // 添加限流拦截器
        if (rateLimitInterceptor != null) {
            registry.addInterceptor(rateLimitInterceptor)
                    .addPathPatterns("/**")
                    .excludePathPatterns("/error", "/favicon.ico", "/static/**", "/css/**", "/js/**", "/images/**");
        }

        // 添加安全拦截器
        if (securityInterceptor != null) {
            registry.addInterceptor(securityInterceptor)
                    .addPathPatterns("/**")
                    .excludePathPatterns("/error", "/favicon.ico", "/static/**", "/css/**", "/js/**", "/images/**");
        }
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
