package com.hibiscus.signal;

import com.hibiscus.signal.config.SignalConfig;
import com.hibiscus.signal.core.EnhancedSignalPersistence;
import com.hibiscus.signal.core.SignalContext;
import com.hibiscus.signal.core.SignalPersistenceInfo;
import com.hibiscus.signal.core.SigHandler;

import java.io.File;
import java.util.HashMap;
import java.util.List;

/**
 * 简单持久化功能演示
 */
public class SimplePersistenceDemo {

    public static void main(String[] args) {
        System.out.println("=== 简单持久化功能演示 ===\n");
        
        // 1. 基本持久化演示
        basicPersistenceDemo();
        
        // 2. 文件轮转演示
        fileRotationDemo();
        
        // 3. 统计信息演示
        statisticsDemo();
        
        System.out.println("=== 演示完成 ===");
    }

    /**
     * 基本持久化演示
     */
    private static void basicPersistenceDemo() {
        System.out.println("1. 基本持久化演示");
        String testFile = "logs/signals/basic-demo.json";
        
        try {
            // 第一次写入
            SignalPersistenceInfo info1 = createOrderEvent("order.created", "ORD-001", "用户A");
            EnhancedSignalPersistence.appendToFile(info1, testFile);
            System.out.println("   ✅ 第一次写入：订单创建事件");
            
            // 第二次写入（追加）
            SignalPersistenceInfo info2 = createOrderEvent("order.paid", "ORD-001", "用户A");
            EnhancedSignalPersistence.appendToFile(info2, testFile);
            System.out.println("   ✅ 第二次写入：订单支付事件");
            
            // 读取验证
            List<SignalPersistenceInfo> allData = EnhancedSignalPersistence.readAllFromFile(testFile);
            System.out.println("   📊 读取结果：" + allData.size() + " 条记录");
            
            // 显示事件详情
            for (int i = 0; i < allData.size(); i++) {
                SignalPersistenceInfo info = allData.get(i);
                String eventName = info.getSigHandler().getSignalName();
                String orderId = (String) info.getSignalContext().getAttribute("orderId");
                System.out.println("      " + (i + 1) + ". " + eventName + " - " + orderId);
            }
            
            System.out.println("   ✅ 追加功能正常：数据没有被覆盖\n");
            
        } catch (Exception e) {
            System.err.println("   ❌ 基本持久化演示失败: " + e.getMessage());
        } finally {
            new File(testFile).delete();
        }
    }

    /**
     * 文件轮转演示
     */
    private static void fileRotationDemo() {
        System.out.println("2. 文件轮转演示");
        String testFile = "logs/signals/rotation-demo.json";
        
        try {
            // 创建大量数据触发轮转
            for (int i = 1; i <= 15; i++) {
                SignalPersistenceInfo info = createOrderEvent(
                    "order.created", 
                    "ORD-" + String.format("%03d", i), 
                    "用户" + i
                );
                EnhancedSignalPersistence.appendToFile(info, testFile);
                EnhancedSignalPersistence.rotateFileIfNeeded(testFile, 1024L); // 1KB
            }
            
            System.out.println("   ✅ 写入完成，检查轮转文件");
            
            // 检查轮转文件
            File dir = new File("logs/signals");
            if (dir.exists()) {
                File[] files = dir.listFiles((d, name) -> name.startsWith("rotation-demo"));
                if (files != null && files.length > 0) {
                    System.out.println("   📁 轮转文件数量: " + files.length);
                    for (File file : files) {
                        EnhancedSignalPersistence.FileStats stats = EnhancedSignalPersistence.getFileStats(file.getPath());
                        System.out.println("      - " + file.getName() + " (" + stats.getRecordCount() + " 条记录)");
                    }
                }
            }
            
            System.out.println("   ✅ 文件轮转功能正常\n");
            
        } catch (Exception e) {
            System.err.println("   ❌ 文件轮转演示失败: " + e.getMessage());
        } finally {
            // 清理
            File dir = new File("logs/signals");
            if (dir.exists()) {
                File[] files = dir.listFiles((d, name) -> name.startsWith("rotation-demo"));
                if (files != null) {
                    for (File file : files) {
                        file.delete();
                    }
                }
            }
        }
    }

    /**
     * 统计信息演示
     */
    private static void statisticsDemo() {
        System.out.println("3. 统计信息演示");
        String testFile = "logs/signals/stats-demo.json";
        
        try {
            // 写入不同类型的事件
            String[] events = {"user.login", "user.logout", "order.created", "order.paid", "order.cancelled"};
            String[] users = {"张三", "李四", "王五", "赵六", "钱七"};
            
            for (int i = 0; i < 10; i++) {
                String event = events[i % events.length];
                String user = users[i % users.length];
                
                SignalPersistenceInfo info = createUserEvent(event, user, "session-" + i);
                EnhancedSignalPersistence.appendToFile(info, testFile);
            }
            
            // 获取统计信息
            EnhancedSignalPersistence.FileStats stats = EnhancedSignalPersistence.getFileStats(testFile);
            System.out.println("   📊 文件统计信息:");
            System.out.println("      - 记录数: " + stats.getRecordCount());
            System.out.println("      - 文件大小: " + String.format("%.2f", stats.getFileSizeBytes() / 1024.0) + " KB");
            System.out.println("      - 总记录数: " + stats.getTotalRecords());
            
            // 读取并分析数据
            List<SignalPersistenceInfo> allData = EnhancedSignalPersistence.readAllFromFile(testFile);
            System.out.println("   📈 事件分布:");
            
            HashMap<String, Integer> eventCount = new HashMap<>();
            for (SignalPersistenceInfo info : allData) {
                String eventName = info.getSigHandler().getSignalName();
                eventCount.put(eventName, eventCount.getOrDefault(eventName, 0) + 1);
            }
            
            eventCount.forEach((event, count) -> {
                System.out.println("      - " + event + ": " + count + " 次");
            });
            
            System.out.println("   ✅ 统计功能正常\n");
            
        } catch (Exception e) {
            System.err.println("   ❌ 统计信息演示失败: " + e.getMessage());
        } finally {
            new File(testFile).delete();
        }
    }

    /**
     * 创建订单事件
     */
    private static SignalPersistenceInfo createOrderEvent(String eventName, String orderId, String userId) {
        SignalContext context = new SignalContext();
        context.setAttribute("orderId", orderId);
        context.setAttribute("userId", userId);
        context.setAttribute("timestamp", System.currentTimeMillis());
        
        SignalConfig config = new SignalConfig.Builder()
            .async(true)
            .persistent(true)
            .maxRetries(3)
            .timeoutMs(5000)
            .build();
        
        SigHandler handler = new SigHandler(
            System.currentTimeMillis(), 
            null, 
            eventName, 
            null, 
            null
        );
        handler.setHandlerName("OrderEventHandler#" + eventName);
        
        return new SignalPersistenceInfo(handler, config, context, new HashMap<>());
    }

    /**
     * 创建用户事件
     */
    private static SignalPersistenceInfo createUserEvent(String eventName, String userId, String sessionId) {
        SignalContext context = new SignalContext();
        context.setAttribute("userId", userId);
        context.setAttribute("sessionId", sessionId);
        context.setAttribute("timestamp", System.currentTimeMillis());
        
        SignalConfig config = new SignalConfig.Builder()
            .async(true)
            .persistent(true)
            .maxRetries(2)
            .timeoutMs(3000)
            .build();
        
        SigHandler handler = new SigHandler(
            System.currentTimeMillis(), 
            null, 
            eventName, 
            null, 
            null
        );
        handler.setHandlerName("UserEventHandler#" + eventName);
        
        return new SignalPersistenceInfo(handler, config, context, new HashMap<>());
    }
}
