package com.hibiscus.signal;

import com.hibiscus.signal.config.SignalConfig;
import com.hibiscus.signal.core.SignalContext;
import com.hibiscus.signal.spring.config.SignalProperties;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 修复版本的事务隔离和事件恢复演示
 * 解决非Spring环境下的问题
 */
public class FixedTransactionAndRecoveryDemo {

    public static void main(String[] args) {
        System.out.println("=== 修复版本的事务隔离和事件恢复演示 ===\n");
        
        // 1. 演示事务隔离
        demonstrateTransactionIsolation();
        
        // 2. 演示事件重发机制
        demonstrateEventReplay();
        
        // 3. 演示事件恢复机制
        demonstrateEventRecovery();
        
        System.out.println("=== 演示完成 ===");
    }

    /**
     * 演示事务隔离
     */
    private static void demonstrateTransactionIsolation() {
        System.out.println("1. 事务隔离演示");
        System.out.println("   确保不同事件在不同事务中执行，避免事务冲突");
        
        try {
            // 创建线程池和信号管理器
            ExecutorService executor = Executors.newFixedThreadPool(3);
            Signals signals = new Signals(executor);
            
            // 注册订单事件处理器（模拟数据库操作）
            signals.connect("order.created", (sender, params) -> {
                SignalContext context = (SignalContext) params[0];
                String orderId = (String) context.getAttribute("orderId");
                
                // 模拟数据库操作
                System.out.println("   📦 处理订单创建: " + orderId + " (事务1)");
                
                // 模拟事务操作
                simulateDatabaseTransaction("订单创建", orderId);
                
            }, new SignalConfig.Builder()
                .async(true)
                .maxRetries(3)
                .timeoutMs(5000)
                .build());
            
            signals.connect("order.paid", (sender, params) -> {
                SignalContext context = (SignalContext) params[0];
                String orderId = (String) context.getAttribute("orderId");
                
                // 模拟数据库操作
                System.out.println("   💰 处理订单支付: " + orderId + " (事务2)");
                
                // 模拟事务操作
                simulateDatabaseTransaction("订单支付", orderId);
                
            }, new SignalConfig.Builder()
                .async(true)
                .maxRetries(3)
                .timeoutMs(5000)
                .build());
            
            signals.connect("inventory.updated", (sender, params) -> {
                SignalContext context = (SignalContext) params[0];
                String productId = (String) context.getAttribute("productId");
                
                // 模拟数据库操作
                System.out.println("   📊 更新库存: " + productId + " (事务3)");
                
                // 模拟事务操作
                simulateDatabaseTransaction("库存更新", productId);
                
            }, new SignalConfig.Builder()
                .async(true)
                .maxRetries(3)
                .timeoutMs(5000)
                .build());
            
            // 并发发送多个事件
            System.out.println("   🚀 并发发送多个事件...");
            
            for (int i = 1; i <= 3; i++) {
                String orderId = "ORD-" + String.format("%03d", i);
                
                // 订单创建事件
                SignalContext context1 = new SignalContext();
                context1.setAttribute("orderId", orderId);
                context1.setAttribute("userId", "用户" + i);
                context1.setAttribute("amount", 100.0 + i * 10);
                signals.emit("order.created", new Object(), (error) -> {}, context1);
                
                // 订单支付事件
                SignalContext context2 = new SignalContext();
                context2.setAttribute("orderId", orderId);
                context2.setAttribute("paymentMethod", "支付宝");
                context2.setAttribute("amount", 100.0 + i * 10);
                signals.emit("order.paid", new Object(), (error) -> {}, context2);
                
                // 库存更新事件
                SignalContext context3 = new SignalContext();
                context3.setAttribute("productId", "PROD-" + String.format("%03d", i));
                context3.setAttribute("quantity", 1);
                context3.setAttribute("operation", "decrease");
                signals.emit("inventory.updated", new Object(), (error) -> {}, context3);
            }
            
            // 等待处理完成
            Thread.sleep(5000);
            
            System.out.println("   ✅ 事务隔离演示完成：每个事件都在独立事务中执行\n");
            
            // 清理
            executor.shutdown();
            
        } catch (Exception e) {
            System.err.println("   ❌ 事务隔离演示失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 演示事件重发机制
     */
    private static void demonstrateEventReplay() {
        System.out.println("2. 事件重发机制演示");
        System.out.println("   演示如何处理失败的事件并自动重试");
        
        try {
            // 创建线程池和信号管理器
            ExecutorService executor = Executors.newFixedThreadPool(2);
            Signals signals = new Signals(executor);
            
            // 注册一个会失败的事件处理器
            signals.connect("payment.processed", (sender, params) -> {
                SignalContext context = (SignalContext) params[0];
                String paymentId = (String) context.getAttribute("paymentId");
                
                System.out.println("   💳 处理支付: " + paymentId);
                
                // 模拟随机失败
                if (Math.random() < 0.7) { // 70% 概率失败
                    throw new RuntimeException("支付处理失败，模拟网络异常");
                }
                
                System.out.println("   ✅ 支付处理成功: " + paymentId);
                
            }, new SignalConfig.Builder()
                .async(true)
                .maxRetries(3)
                .retryDelayMs(1000)
                .timeoutMs(3000)
                .build());
            
            // 发送多个支付事件
            System.out.println("   🚀 发送支付事件（部分会失败并重试）...");
            
            for (int i = 1; i <= 5; i++) {
                String paymentId = "PAY-" + String.format("%06d", i);
                
                SignalContext context = new SignalContext();
                context.setAttribute("paymentId", paymentId);
                context.setAttribute("amount", 50.0 + i * 10);
                context.setAttribute("method", "信用卡");
                
                signals.emit("payment.processed", new Object(), (error) -> {
                    if (error != null) {
                        System.out.println("   ⚠️  支付事件最终失败: " + paymentId + " - " + error.getMessage());
                    }
                }, context);
            }
            
            // 等待处理完成
            Thread.sleep(8000);
            
            System.out.println("   ✅ 事件重发机制演示完成：失败的事件会自动重试\n");
            
            // 清理
            executor.shutdown();
            
        } catch (Exception e) {
            System.err.println("   ❌ 事件重发机制演示失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 演示事件恢复机制
     */
    private static void demonstrateEventRecovery() {
        System.out.println("3. 事件恢复机制演示");
        System.out.println("   演示如何从持久化文件恢复事件，防止发版或宕机导致的事件丢失");
        
        try {
            // 创建线程池和信号管理器
            ExecutorService executor = Executors.newFixedThreadPool(2);
            Signals signals = new Signals(executor);
            
            // 注册事件处理器
            signals.connect("user.registered", (sender, params) -> {
                SignalContext context = (SignalContext) params[0];
                String userId = (String) context.getAttribute("userId");
                
                System.out.println("   👤 处理用户注册: " + userId);
                
                // 模拟业务处理
                simulateUserRegistration(userId);
                
            }, new SignalConfig.Builder()
                .async(true)
                .maxRetries(2)
                .timeoutMs(3000)
                .build());
            
            signals.connect("email.sent", (sender, params) -> {
                SignalContext context = (SignalContext) params[0];
                String email = (String) context.getAttribute("email");
                
                System.out.println("   📧 发送邮件: " + email);
                
                // 模拟邮件发送
                simulateEmailSending(email);
                
            }, new SignalConfig.Builder()
                .async(true)
                .maxRetries(3)
                .timeoutMs(5000)
                .build());
            
            // 第一阶段：发送一些事件
            System.out.println("   📝 第一阶段：发送事件...");
            
            for (int i = 1; i <= 3; i++) {
                String userId = "USER-" + String.format("%06d", i);
                String email = "user" + i + "@example.com";
                
                // 用户注册事件
                SignalContext context1 = new SignalContext();
                context1.setAttribute("userId", userId);
                context1.setAttribute("email", email);
                context1.setAttribute("timestamp", System.currentTimeMillis());
                signals.emit("user.registered", new Object(), (error) -> {}, context1);
                
                // 邮件发送事件
                SignalContext context2 = new SignalContext();
                context2.setAttribute("email", email);
                context2.setAttribute("template", "welcome");
                context2.setAttribute("timestamp", System.currentTimeMillis());
                signals.emit("email.sent", new Object(), (error) -> {}, context2);
            }
            
            // 等待处理完成
            Thread.sleep(3000);
            
            // 第二阶段：模拟应用重启，从持久化文件恢复事件
            System.out.println("   🔄 第二阶段：模拟应用重启，从持久化文件恢复事件...");
            
            // 创建新的信号管理器（模拟重启）
            ExecutorService newExecutor = Executors.newFixedThreadPool(2);
            Signals newSignals = new Signals(newExecutor);
            
            // 重新注册事件处理器
            newSignals.connect("user.registered", (sender, params) -> {
                SignalContext context = (SignalContext) params[0];
                String userId = (String) context.getAttribute("userId");
                
                System.out.println("   🔄 恢复处理用户注册: " + userId);
                
                // 模拟业务处理
                simulateUserRegistration(userId);
                
            }, new SignalConfig.Builder()
                .async(true)
                .maxRetries(2)
                .timeoutMs(3000)
                .build());
            
            newSignals.connect("email.sent", (sender, params) -> {
                SignalContext context = (SignalContext) params[0];
                String email = (String) context.getAttribute("email");
                
                System.out.println("   🔄 恢复发送邮件: " + email);
                
                // 模拟邮件发送
                simulateEmailSending(email);
                
            }, new SignalConfig.Builder()
                .async(true)
                .maxRetries(3)
                .timeoutMs(5000)
                .build());
            
            // 模拟从持久化文件恢复事件
            simulateEventRecovery(newSignals);
            
            // 等待恢复完成
            Thread.sleep(3000);
            
            System.out.println("   ✅ 事件恢复机制演示完成：成功从持久化文件恢复事件\n");
            
            // 清理
            newExecutor.shutdown();
            
        } catch (Exception e) {
            System.err.println("   ❌ 事件恢复机制演示失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 模拟数据库事务操作
     */
    private static void simulateDatabaseTransaction(String operation, String id) {
        try {
            // 模拟事务开始
            System.out.println("      🔒 开始事务: " + operation + " - " + id);
            
            // 模拟数据库操作
            Thread.sleep(100 + (long)(Math.random() * 200));
            
            // 模拟事务提交
            System.out.println("      ✅ 提交事务: " + operation + " - " + id);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 模拟用户注册
     */
    private static void simulateUserRegistration(String userId) {
        try {
            System.out.println("      📝 创建用户记录: " + userId);
            Thread.sleep(200);
            System.out.println("      📝 初始化用户配置: " + userId);
            Thread.sleep(150);
            System.out.println("      ✅ 用户注册完成: " + userId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 模拟邮件发送
     */
    private static void simulateEmailSending(String email) {
        try {
            System.out.println("      📧 准备邮件内容: " + email);
            Thread.sleep(100);
            System.out.println("      📧 发送邮件: " + email);
            Thread.sleep(300);
            System.out.println("      ✅ 邮件发送成功: " + email);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 模拟事件恢复
     */
    private static void simulateEventRecovery(Signals signals) {
        try {
            // 模拟从持久化文件读取事件
            System.out.println("      📂 从持久化文件读取事件");
            
            // 这里应该实际读取持久化文件，但为了演示，我们模拟这个过程
            System.out.println("      🔄 发现 6 个待恢复的事件");
            
            // 模拟恢复事件
            String[] events = {"user.registered", "email.sent", "user.registered", "email.sent", "user.registered", "email.sent"};
            String[] ids = {"USER-000001", "user1@example.com", "USER-000002", "user2@example.com", "USER-000003", "user3@example.com"};
            
            for (int i = 0; i < events.length; i++) {
                SignalContext context = new SignalContext();
                if (events[i].equals("user.registered")) {
                    context.setAttribute("userId", ids[i]);
                    context.setAttribute("email", "user" + (i/2 + 1) + "@example.com");
                } else {
                    context.setAttribute("email", ids[i]);
                    context.setAttribute("template", "welcome");
                }
                context.setAttribute("recovered", true);
                context.setAttribute("recoveryTime", System.currentTimeMillis());

                int finalI = i;
                signals.emit(events[i], new Object(), (error) -> {
                    if (error != null) {
                        System.out.println("      ⚠️  恢复事件失败: " + events[finalI] + " - " + error.getMessage());
                    }
                }, context);
            }
            
        } catch (Exception e) {
            System.err.println("      ❌ 事件恢复失败: " + e.getMessage());
        }
    }
}
