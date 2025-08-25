package com.hibiscus.signal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Hibiscus Signal 框架主应用程序类
 * 
 * @author heathcetide
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.hibiscus.signal")
public class HibiscusSignalApplication {

    public static void main(String[] args) {
        SpringApplication.run(HibiscusSignalApplication.class, args);
    }
}
