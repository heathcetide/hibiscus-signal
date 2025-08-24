package com.hibiscus.demo;

import com.hibiscus.demo.entity.Order;
import com.hibiscus.demo.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 演示启动器
 * 在应用启动后自动演示 Hibiscus Signal 框架的功能
 * 
 * @author heathcetide
 */
@Component
@Slf4j
public class DemoStarter implements CommandLineRunner {

    @Autowired
    private OrderService orderService;

    @Override
    public void run(String... args) throws Exception {
        log.info("🚀 开始演示 Hibiscus Signal 框架功能...");
        
        // 等待应用完全启动
        Thread.sleep(2000);
        
        try {
            // 演示1：创建订单
            log.info("📝 演示1：创建订单");
            Order order = orderService.createOrder(
                1001L, 
                2001L, 
                "iPhone 15 Pro", 
                1, 
                new BigDecimal("8999.00")
            );
            
            log.info("✅ 订单创建成功: {}", order.getOrderNo());
            
            // 等待事件处理
            Thread.sleep(3000);
            
            // 演示2：支付订单
            log.info("💰 演示2：支付订单");
            Order paidOrder = orderService.payOrder(order.getOrderNo());
            log.info("✅ 订单支付成功: {}", paidOrder.getOrderNo());
            
            // 等待事件处理
            Thread.sleep(3000);
            
            // 演示3：创建另一个订单（可能失败）
            log.info("📝 演示3：创建另一个订单（模拟失败场景）");
            try {
                Order order2 = orderService.createOrder(
                    1002L, 
                    2002L, 
                    "MacBook Pro", 
                    2, 
                    new BigDecimal("15999.00")
                );
                log.info("✅ 第二个订单创建成功: {}", order2.getOrderNo());
            } catch (Exception e) {
                log.info("⚠️ 第二个订单创建失败（这是预期的，用于演示重试机制）");
            }
            
            // 等待事件处理和重试
            Thread.sleep(5000);
            
            log.info("🎉 Hibiscus Signal 框架演示完成！");
            log.info("📊 请查看日志了解事件处理、事务隔离、重试机制等功能的运行情况");
            
        } catch (Exception e) {
            log.error("演示过程中发生错误", e);
        }
    }
}
