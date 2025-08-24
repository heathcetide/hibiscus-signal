package com.hibiscus.demo.service;

import com.hibiscus.demo.entity.Order;
import com.hibiscus.demo.repository.OrderRepository;
import com.hibiscus.signal.Signals;
import com.hibiscus.signal.core.SignalContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 订单服务
 * 演示 Hibiscus Signal 框架的使用
 * 
 * @author heathcetide
 */
@Service
@Slf4j
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private Signals signals;

    /**
     * 创建订单
     * 演示事务隔离和事件发送
     */
    @Transactional
    public Order createOrder(Long userId, Long productId, String productName, 
                           Integer quantity, BigDecimal unitPrice) {
        
        // 1. 生成订单号
        String orderNo = generateOrderNo();
        
        // 2. 创建订单
        Order order = new Order(orderNo, userId, productId, productName, quantity, unitPrice);
        order = orderRepository.save(order);
        
        log.info("✅ 订单创建成功: {}", orderNo);
        
        // 3. 发送订单创建事件（在独立事务中处理）
        SignalContext context = new SignalContext();
        context.setAttribute("orderId", order.getId());
        context.setAttribute("orderNo", order.getOrderNo());
        context.setAttribute("userId", order.getUserId());
        context.setAttribute("productId", order.getProductId());
        context.setAttribute("quantity", order.getQuantity());
        context.setAttribute("totalAmount", order.getTotalAmount());
        
        signals.emit("order.created", this, error -> System.out.println("Error: " + error.getMessage()), context);
        
        log.info("📤 订单创建事件已发送: {}", orderNo);
        
        return order;
    }

    /**
     * 支付订单
     * 演示事件发送和状态更新
     */
    @Transactional
    public Order payOrder(String orderNo) {
        
        // 1. 查找订单
        Order order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new RuntimeException("订单不存在: " + orderNo));
        
        // 2. 更新订单状态
        order.setStatus(Order.OrderStatus.PAID);
        order.setUpdatedTime(java.time.LocalDateTime.now());
        order = orderRepository.save(order);
        
        log.info("✅ 订单支付成功: {}", orderNo);
        
        // 3. 发送订单支付事件
        SignalContext context = new SignalContext();
        context.setAttribute("orderId", order.getId());
        context.setAttribute("orderNo", order.getOrderNo());
        context.setAttribute("userId", order.getUserId());
        context.setAttribute("totalAmount", order.getTotalAmount());
        
        signals.emit("order.paid", this, error -> System.out.println("Error: " + error.getMessage()), context);
        
        log.info("📤 订单支付事件已发送: {}", orderNo);
        
        return order;
    }

    /**
     * 取消订单
     * 演示事件发送和状态更新
     */
    @Transactional
    public Order cancelOrder(String orderNo) {
        
        // 1. 查找订单
        Order order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new RuntimeException("订单不存在: " + orderNo));
        
        // 2. 更新订单状态
        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setUpdatedTime(java.time.LocalDateTime.now());
        order = orderRepository.save(order);
        
        log.info("❌ 订单取消成功: {}", orderNo);
        
        // 3. 发送订单取消事件
        SignalContext context = new SignalContext();
        context.setAttribute("orderId", order.getId());
        context.setAttribute("orderNo", order.getOrderNo());
        context.setAttribute("userId", order.getUserId());
        context.setAttribute("totalAmount", order.getTotalAmount());
        
        signals.emit("order.cancelled", this, error -> System.out.println("Error: " + error.getMessage()), context);
        
        log.info("📤 订单取消事件已发送: {}", orderNo);
        
        return order;
    }

    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8);
    }
}
