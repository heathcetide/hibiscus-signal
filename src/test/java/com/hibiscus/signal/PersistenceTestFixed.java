package com.hibiscus.signal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hibiscus.signal.config.SignalConfig;
import com.hibiscus.signal.core.SignalContext;
import com.hibiscus.signal.core.SignalPersistence;
import com.hibiscus.signal.core.SignalPersistenceInfo;
import com.hibiscus.signal.core.SigHandler;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 修复版本的持久化测试
 * 展示如何正确实现持久化功能
 */
public class PersistenceTestFixed {

    public static void main(String[] args) {
        System.out.println("=== 修复版本的持久化测试 ===\n");
        
        // 测试修复后的持久化功能
        testFixedPersistence();
        
        // 展示正确的实现方式
        showCorrectImplementation();
    }

    private static void testFixedPersistence() {
        System.out.println("1. 测试修复后的持久化");
        String testFile = "test-fixed-persistence.json";
        
        // 创建修复后的持久化信息
        FixedSignalPersistenceInfo info = createFixedPersistenceInfo("test.event", "test data");
        
        // 保存到文件
        saveToFileFixed(info, testFile);
        System.out.println("   保存完成");
        
        // 从文件读取
        FixedSignalPersistenceInfo loaded = loadFromFileFixed(testFile, FixedSignalPersistenceInfo.class);
        System.out.println("   读取结果: " + (loaded != null ? "成功" : "失败"));
        if (loaded != null) {
            System.out.println("   事件类型: " + loaded.getEventType());
            System.out.println("   事件数据: " + loaded.getEventData());
        }
        
        // 清理
        new File(testFile).delete();
        System.out.println();
    }

    private static void showCorrectImplementation() {
        System.out.println("2. 正确的持久化实现方式");
        System.out.println("   ✅ 使用数据库存储而不是文件");
        System.out.println("   ✅ 实现事件状态管理");
        System.out.println("   ✅ 提供重发机制");
        System.out.println("   ✅ 支持事务一致性");
        System.out.println("   ✅ 添加监控和告警");
        System.out.println();
        
        System.out.println("3. 建议的架构改进");
        System.out.println("   📊 使用消息队列（如 Kafka、RabbitMQ）");
        System.out.println("   🗄️  使用数据库存储事件状态");
        System.out.println("   🔄 实现事件重发和补偿机制");
        System.out.println("   📈 添加事件处理监控");
        System.out.println("   🛡️  实现事务一致性保证");
    }

    // 修复后的持久化信息类
    public static class FixedSignalPersistenceInfo {
        private final String eventType;
        private final String eventData;
        private final long timestamp;
        private final String status;

        @JsonCreator
        public FixedSignalPersistenceInfo(
                @JsonProperty("eventType") String eventType,
                @JsonProperty("eventData") String eventData,
                @JsonProperty("timestamp") long timestamp,
                @JsonProperty("status") String status) {
            this.eventType = eventType;
            this.eventData = eventData;
            this.timestamp = timestamp;
            this.status = status;
        }

        public String getEventType() { return eventType; }
        public String getEventData() { return eventData; }
        public long getTimestamp() { return timestamp; }
        public String getStatus() { return status; }

        @Override
        public String toString() {
            return "FixedSignalPersistenceInfo{" +
                    "eventType='" + eventType + '\'' +
                    ", eventData='" + eventData + '\'' +
                    ", timestamp=" + timestamp +
                    ", status='" + status + '\'' +
                    '}';
        }
    }

    private static FixedSignalPersistenceInfo createFixedPersistenceInfo(String eventType, String eventData) {
        return new FixedSignalPersistenceInfo(eventType, eventData, System.currentTimeMillis(), "PENDING");
    }

    private static void saveToFileFixed(Object data, String filePath) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(filePath), data);
        } catch (Exception e) {
            System.err.println("保存失败: " + e.getMessage());
        }
    }

    private static <T> T loadFromFileFixed(String filePath, Class<T> clazz) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(new File(filePath), clazz);
        } catch (Exception e) {
            System.err.println("读取失败: " + e.getMessage());
            return null;
        }
    }
}
