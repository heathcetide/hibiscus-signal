package com.hibiscus.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Hibiscus Signal 演示应用程序
 * 
 * @author heathcetide
 */
@SpringBootApplication(scanBasePackages = {"com.hibiscus.demo", "com.hibiscus.signal"})
@EnableScheduling
@EntityScan(basePackages = {
        "com.hibiscus.demo.entity",
        "com.hibiscus.signal.core.entity"
})
@EnableJpaRepositories(basePackages = "com.hibiscus.demo.repository") // 明确指定repository包
public class HibiscusSignalDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(HibiscusSignalDemoApplication.class, args);
        System.out.println("🚀 Hibiscus Signal Demo 启动成功！");
        System.out.println("📖 访问地址: http://localhost:8080");
        System.out.println("📊 健康检查: http://localhost:8080/actuator/health");
    }
}
