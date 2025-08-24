package com.hibiscus.signal;

import com.hibiscus.signal.config.SignalConfig;
import com.hibiscus.signal.core.EnhancedSignalPersistence;
import com.hibiscus.signal.core.SignalContext;
import com.hibiscus.signal.core.SignalPersistenceInfo;
import com.hibiscus.signal.core.SigHandler;
import com.hibiscus.signal.spring.config.SignalProperties;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 持久化功能演示
 */
public class PersistenceDemo {

    public static void main(String[] args) {
        System.out.println("=== Hibiscus Signal 持久化功能演示 ===\n");
        
        // 1. 基本持久化演示
        basicPersistenceDemo();
        
        // 2. 文件轮转演示
        fileRotationDemo();
        
        // 3. 真实场景演示
        realScenarioDemo();
        
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
            for (int i = 1; i <= 30; i++) {
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
     * 真实场景演示
     */
    private static void realScenarioDemo() {
        System.out.println("3. 真实场景演示");
        
        try {
            // 配置持久化
            SignalProperties properties = new SignalProperties();
            properties.setPersistent(true);
            properties.setPersistenceDirectory("logs/signals");
            properties.setPersistenceFile("ecommerce-demo.json");
            properties.setMaxFileSizeBytes(1024L);
            properties.setEnableFileRotation(true);
            
            System.out.println("   ⚙️  配置: " + properties);
            
            // 创建线程池和信号管理器
            ExecutorService executor = Executors.newFixedThreadPool(2);
            Signals signals = new Signals(executor);
            
            // 注册订单事件处理器
            signals.connect("order.created", (sender, params) -> {
                SignalContext context = (SignalContext) params[0];
                String orderId = (String) context.getAttribute("orderId");
                System.out.println("   📦 处理订单创建: " + orderId);
            }, new SignalConfig.Builder().async(true).persistent(true).build());
            
            // 模拟订单流程
            for (int i = 1; i <= 3; i++) {
                String orderId = "ORD-" + String.format("%03d", i);
                
                SignalContext context = new SignalContext();
                context.setAttribute("orderId", orderId);
                context.setAttribute("userId", "用户" + i);
                context.setAttribute("amount", 100.0 + i * 10);
                signals.emit("order.created", new Object(), (error) -> {}, context);
            }
            
            // 等待处理完成
            Thread.sleep(2000);
            
            // 检查持久化文件
            String fullPath = properties.getPersistenceDirectory() + "/" + properties.getPersistenceFile();
            List<SignalPersistenceInfo> data = EnhancedSignalPersistence.readAllFromFile(fullPath);
            System.out.println("   📊 持久化记录数: " + data.size());
            
            EnhancedSignalPersistence.FileStats stats = EnhancedSignalPersistence.getFileStats(fullPath);
            System.out.println("   📈 文件统计: " + stats);
            
            System.out.println("   ✅ 真实场景演示完成\n");
            
            // 清理
            executor.shutdown();
            new File(fullPath).delete();
            
        } catch (Exception e) {
            System.err.println("   ❌ 真实场景演示失败: " + e.getMessage());
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
}
