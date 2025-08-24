package com.hibiscus.demo.controller;

import com.hibiscus.demo.entity.Order;
import com.hibiscus.demo.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 订单控制器
 * 提供 REST API 接口
 * 
 * @author heathcetide
 */
@RestController
@RequestMapping("/api/orders")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 创建订单
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> createOrder() {
        try {
            Random random = new Random();
            // 使用较小的随机数范围
            int userId = 1 + random.nextInt(1000);
            int productId = 1 + random.nextInt(1000);
            String productName = "product - " + (1 + random.nextInt(1000));
            int quantity = 1 + random.nextInt(10); // 更小的数量范围
            // 限制单价不超过9999，确保总金额在范围内
            BigDecimal unitPrice = BigDecimal.valueOf(random.nextInt(9999) + 1);

            Order order = orderService.createOrder(
                    (long) userId,
                    (long) productId,
                    productName,
                    quantity,
                    unitPrice
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "订单创建成功");
            response.put("data", order);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("创建订单失败", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "订单创建失败: " + e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }


    /**
     * 支付订单
     */
    @PostMapping("/{orderNo}/pay")
    public ResponseEntity<Map<String, Object>> payOrder(@PathVariable String orderNo) {
        try {
            Order order = orderService.payOrder(orderNo);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "订单支付成功");
            response.put("data", order);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("支付订单失败: {}", orderNo, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "订单支付失败: " + e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 取消订单
     */
    @PostMapping("/{orderNo}/cancel")
    public ResponseEntity<Map<String, Object>> cancelOrder(@PathVariable String orderNo) {
        try {
            Order order = orderService.cancelOrder(orderNo);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "订单取消成功");
            response.put("data", order);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("取消订单失败: {}", orderNo, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "订单取消失败: " + e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "Hibiscus Signal Demo 运行正常");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 创建订单请求
     */
    public static class CreateOrderRequest {
        private Long userId;
        private Long productId;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;

        // Getters and Setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    }
}
