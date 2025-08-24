package com.hibiscus.demo.handler;

import com.hibiscus.signal.core.SignalContext;
import com.hibiscus.signal.spring.anno.SignalHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 订单事件处理器
 * 演示 Hibiscus Signal 框架的事件处理功能
 * 
 * @author heathcetide
 */
@Component
@Slf4j
public class OrderEventHandler {

    /**
     * 处理订单创建事件
     * 演示事务隔离：即使这里失败，订单创建事务不受影响
     */
    @SignalHandler(value = "order.created", target = OrderEventHandler.class, methodName = "handleOrderCreated")
    public void handleOrderCreated(SignalContext context) {
        Long orderId = (Long) context.getAttribute("orderId");
        String orderNo = (String) context.getAttribute("orderNo");
        Long userId = (Long) context.getAttribute("userId");
        Long productId = (Long) context.getAttribute("productId");
        Integer quantity = (Integer) context.getAttribute("quantity");
        BigDecimal totalAmount = (BigDecimal) context.getAttribute("totalAmount");

        log.info("🔄 处理订单创建事件: orderNo={}, userId={}, productId={}, quantity={}, totalAmount={}", 
                orderNo, userId, productId, quantity, totalAmount);

        // 模拟业务处理
        try {
            // 1. 库存检查
            checkInventory(productId, quantity);
            
            // 2. 库存扣减
            decreaseInventory(productId, quantity);
            
            // 3. 发送通知
            sendNotification(userId, "订单创建成功", "您的订单 " + orderNo + " 已创建成功");
            
            log.info("✅ 订单创建事件处理成功: {}", orderNo);
            
        } catch (Exception e) {
            log.error("❌ 订单创建事件处理失败: {}, 错误: {}", orderNo, e.getMessage(), e);
            throw new RuntimeException("订单创建事件处理失败", e);
        }
    }

    /**
     * 处理订单支付事件
     */
    @SignalHandler(value = "order.paid", target = OrderEventHandler.class, methodName = "handleOrderPaid")
    public void handleOrderPaid(SignalContext context) {
        Long orderId = (Long) context.getAttribute("orderId");
        String orderNo = (String) context.getAttribute("orderNo");
        Long userId = (Long) context.getAttribute("userId");
        BigDecimal totalAmount = (BigDecimal) context.getAttribute("totalAmount");

        log.info("🔄 处理订单支付事件: orderNo={}, userId={}, totalAmount={}", 
                orderNo, userId, totalAmount);

        try {
            // 1. 更新库存状态
            updateInventoryStatus(orderId);
            
            // 2. 生成发货单
            generateShippingOrder(orderId);
            
            // 3. 发送支付成功通知
            sendNotification(userId, "支付成功", "您的订单 " + orderNo + " 支付成功，金额: " + totalAmount);
            
            log.info("✅ 订单支付事件处理成功: {}", orderNo);
            
        } catch (Exception e) {
            log.error("❌ 订单支付事件处理失败: {}, 错误: {}", orderNo, e.getMessage(), e);
            throw new RuntimeException("订单支付事件处理失败", e);
        }
    }

    /**
     * 处理订单取消事件
     */
    @SignalHandler(value = "order.cancelled", target = OrderEventHandler.class, methodName = "handleOrderCancelled")
    public void handleOrderCancelled(SignalContext context) {
        Long orderId = (Long) context.getAttribute("orderId");
        String orderNo = (String) context.getAttribute("orderNo");
        Long userId = (Long) context.getAttribute("userId");
        BigDecimal totalAmount = (BigDecimal) context.getAttribute("totalAmount");

        log.info("🔄 处理订单取消事件: orderNo={}, userId={}, totalAmount={}", 
                orderNo, userId, totalAmount);

        try {
            // 1. 恢复库存
            restoreInventory(orderId);
            
            // 2. 退款处理
            processRefund(orderId, totalAmount);
            
            // 3. 发送取消通知
            sendNotification(userId, "订单取消", "您的订单 " + orderNo + " 已取消，退款金额: " + totalAmount);
            
            log.info("✅ 订单取消事件处理成功: {}", orderNo);
            
        } catch (Exception e) {
            log.error("❌ 订单取消事件处理失败: {}, 错误: {}", orderNo, e.getMessage(), e);
            throw new RuntimeException("订单取消事件处理失败", e);
        }
    }

    // 模拟业务方法

    private void checkInventory(Long productId, Integer quantity) {
        log.info("📦 检查库存: productId={}, quantity={}", productId, quantity);
        // 模拟库存检查逻辑
        if (Math.random() < 0.1) { // 10% 概率库存不足
            throw new RuntimeException("库存不足");
        }
    }

    private void decreaseInventory(Long productId, Integer quantity) {
        log.info("📦 扣减库存: productId={}, quantity={}", productId, quantity);
        // 模拟库存扣减逻辑
        if (Math.random() < 0.05) { // 5% 概率扣减失败
            throw new RuntimeException("库存扣减失败");
        }
    }

    private void updateInventoryStatus(Long orderId) {
        log.info("📦 更新库存状态: orderId={}", orderId);
        // 模拟更新库存状态逻辑
    }

    private void generateShippingOrder(Long orderId) {
        log.info("📦 生成发货单: orderId={}", orderId);
        // 模拟生成发货单逻辑
    }

    private void restoreInventory(Long orderId) {
        log.info("📦 恢复库存: orderId={}", orderId);
        // 模拟恢复库存逻辑
    }

    private void processRefund(Long orderId, BigDecimal amount) {
        log.info("💰 处理退款: orderId={}, amount={}", orderId, amount);
        // 模拟退款处理逻辑
    }

    private void sendNotification(Long userId, String title, String content) {
        log.info("📧 发送通知: userId={}, title={}, content={}", userId, title, content);
        // 模拟发送通知逻辑
    }
}
