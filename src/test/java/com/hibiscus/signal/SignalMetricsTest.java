package com.hibiscus.signal;

import com.hibiscus.signal.config.SignalConfig;
import com.hibiscus.signal.config.SignalPriority;
import com.hibiscus.signal.core.SignalContext;
import com.hibiscus.signal.core.SignalHandler;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SignalMetricsTest {
    public static void main(String[] args) throws InterruptedException {
        // 1. 初始化信号系统
        ExecutorService executor = Executors.newFixedThreadPool(4);
        Signals signals = new Signals(executor);

        // 2. 注册不同优先级的处理器
        signals.connect("test.event1", new SignalHandler() {
            @Override
            public void handle(Object sender, Object... params) {
                System.out.printf("[%s] 处理高优先级事件 | 参数: %s\n",
                        LocalTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME),
                        Arrays.toString(params));
            }
        }, new SignalConfig.Builder().priority(SignalPriority.HIGH).build());

        signals.connect("test.event2", new SignalHandler() {
            @Override
            public void handle(Object sender, Object... params) {
                System.out.printf("[%s] 处理中优先级事件 | 参数: %s\n",
                        LocalTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME),
                        Arrays.toString(params));
            }
        }, new SignalConfig.Builder().priority(SignalPriority.MEDIUM).build());

        signals.connect("test.event3", new SignalHandler() {
            @Override
            public void handle(Object sender, Object... params) {
                System.out.printf("[%s] 处理低优先级事件 | 参数: %s\n",
                        LocalTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME),
                        Arrays.toString(params));
            }
        }, new SignalConfig.Builder().priority(SignalPriority.LOW).build());

        // 3. 发送混合优先级事件
        System.out.println("-- 开始发送测试事件 --");
        for (int i = 1; i <= 50; i++) {
            signals.emit("test.event1", signals, e -> {},
                    new SignalContext(), "低优先级数据"+i);
            signals.emit("test.event2", signals, e -> {},
                    new SignalContext(), "中优先级数据"+i);
            signals.emit("test.event3", signals, e -> {},
                    new SignalContext(), "高优先级数据"+i);
            Thread.sleep(100); // 确保事件顺序
        }

        // 等待处理完成
        Thread.sleep(2000);

        // 输出指标信息
        System.out.println("\n========== 信号指标统计 ==========");
        printMetrics(signals, "test.event1");
        printMetrics(signals, "test.event2");
        printMetrics(signals, "test.event3");
        executor.shutdown();
    }

    private static void printMetrics(Signals signals, String eventName) {
        Map<String, Object> metrics = signals.getMetrics().getMetrics(eventName);
        System.out.println("\n事件 [" + eventName + "] 指标：");
        System.out.println("发射次数: " + metrics.getOrDefault("emitCount", 0));
        System.out.println("处理器数量: " + metrics.getOrDefault("handlerCount", 0));
        System.out.println("总处理时间(ms): " + metrics.getOrDefault("totalProcessingTime", 0));
        System.out.println("错误次数: " + metrics.getOrDefault("errorCount", 0));
        System.out.println("最后发射时间: " + new Date((Long)metrics.getOrDefault("lastEmitTime", 0L)));
    }
}