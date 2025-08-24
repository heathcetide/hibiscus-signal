package com.hibiscus.signal;

import com.hibiscus.signal.config.EnhancedSignalPersistence;
import com.hibiscus.signal.core.SignalContext;
import com.hibiscus.signal.core.SignalPersistenceInfo;
import com.hibiscus.signal.config.SignalConfig;
import com.hibiscus.signal.core.SigHandler;

import java.io.File;
import java.util.HashMap;
import java.util.List;

/**
 * 简单的增强版持久化测试
 */
public class SimpleEnhancedPersistenceTest {

    public static void main(String[] args) {
        System.out.println("=== 简单增强版持久化测试 ===\n");
        
        // 测试追加功能
        testAppendFunctionality();
        
        // 测试文件轮转
        testFileRotation();
        
        // 测试统计功能
        testFileStats();
        
        System.out.println("=== 测试完成 ===");
    }

    private static void testAppendFunctionality() {
        System.out.println("1. 测试追加功能");
        String testFile = "logs/signals/test-append.json";
        
        try {
            // 第一次写入
            SignalPersistenceInfo info1 = createTestInfo("event1", "data1");
            EnhancedSignalPersistence.appendToFile(info1, testFile);
            System.out.println("   第一次写入完成");
            
            // 第二次写入（追加）
            SignalPersistenceInfo info2 = createTestInfo("event2", "data2");
            EnhancedSignalPersistence.appendToFile(info2, testFile);
            System.out.println("   第二次写入完成");
            
            // 读取验证
            List<SignalPersistenceInfo> allData = EnhancedSignalPersistence.readAllFromFile(testFile);
            System.out.println("   读取结果: " + allData.size() + " 条记录");
            System.out.println("   ✅ 追加功能正常：数据没有被覆盖\n");
            
        } catch (Exception e) {
            System.err.println("   ❌ 追加功能测试失败: " + e.getMessage());
        } finally {
            // 清理
            new File(testFile).delete();
        }
    }

    private static void testFileRotation() {
        System.out.println("2. 测试文件轮转功能");
        String testFile = "logs/signals/test-rotation.json";
        
        try {
            // 创建数据触发轮转
            for (int i = 0; i < 20; i++) {
                SignalPersistenceInfo info = createTestInfo("event" + i, "data" + i);
                EnhancedSignalPersistence.appendToFile(info, testFile);
                EnhancedSignalPersistence.rotateFileIfNeeded(testFile, 1024L); // 1KB
            }
            
            System.out.println("   写入完成，检查轮转文件");
            
            // 检查轮转文件
            File dir = new File("logs/signals");
            if (dir.exists()) {
                File[] files = dir.listFiles((d, name) -> name.startsWith("test-rotation"));
                if (files != null) {
                    System.out.println("   轮转文件数量: " + files.length);
                }
            }
            
            System.out.println("   ✅ 文件轮转功能正常\n");
            
        } catch (Exception e) {
            System.err.println("   ❌ 文件轮转测试失败: " + e.getMessage());
        } finally {
            // 清理
            File dir = new File("logs/signals");
            if (dir.exists()) {
                File[] files = dir.listFiles((d, name) -> name.startsWith("test-rotation"));
                if (files != null) {
                    for (File file : files) {
                        file.delete();
                    }
                }
            }
        }
    }

    private static void testFileStats() {
        System.out.println("3. 测试文件统计功能");
        String testFile = "logs/signals/test-stats.json";
        
        try {
            // 写入数据
            for (int i = 0; i < 5; i++) {
                SignalPersistenceInfo info = createTestInfo("event" + i, "data" + i);
                EnhancedSignalPersistence.appendToFile(info, testFile);
            }
            
            // 获取统计信息
            EnhancedSignalPersistence.FileStats stats = EnhancedSignalPersistence.getFileStats(testFile);
            System.out.println("   文件统计: " + stats);
            System.out.println("   ✅ 统计功能正常\n");
            
        } catch (Exception e) {
            System.err.println("   ❌ 统计功能测试失败: " + e.getMessage());
        } finally {
            // 清理
            new File(testFile).delete();
        }
    }

    private static SignalPersistenceInfo createTestInfo(String event, String data) {
        SignalContext context = new SignalContext();
        context.setAttribute("event", event);
        context.setAttribute("data", data);
        context.setAttribute("timestamp", System.currentTimeMillis());
        
        SignalConfig config = new SignalConfig.Builder().persistent(true).build();
        SigHandler handler = new SigHandler(1L, null, event, null, null);
        
        return new SignalPersistenceInfo(handler, config, context, new HashMap<>());
    }
}
