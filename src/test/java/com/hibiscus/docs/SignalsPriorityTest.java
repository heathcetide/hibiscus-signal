package com.hibiscus.docs;

import com.hibiscus.docs.config.SignalConfig;
import com.hibiscus.docs.config.SignalPriority;
import com.hibiscus.docs.core.SignalContext;
import com.hibiscus.docs.core.SignalHandler;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SignalsPriorityTest {
    public static void main(String[] args) throws InterruptedException {
        // 1. 初始化信号系统
        ExecutorService executor = Executors.newFixedThreadPool(4);
        Signals signals = new Signals(executor);

        // 2. 注册不同优先级的处理器
        signals.connect("test.event", new SignalHandler() {
            @Override
            public void handle(Object sender, Object... params) {
                System.out.printf("[%s] 处理高优先级事件 | 参数: %s\n",
                        LocalTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME),
                        Arrays.toString(params));
            }
        }, new SignalConfig.Builder().priority(SignalPriority.HIGH).build());

        signals.connect("test.event", new SignalHandler() {
            @Override
            public void handle(Object sender, Object... params) {
                System.out.printf("[%s] 处理中优先级事件 | 参数: %s\n",
                        LocalTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME),
                        Arrays.toString(params));
            }
        }, new SignalConfig.Builder().priority(SignalPriority.MEDIUM).build());

        signals.connect("test.event", new SignalHandler() {
            @Override
            public void handle(Object sender, Object... params) {
                System.out.printf("[%s] 处理低优先级事件 | 参数: %s\n",
                        LocalTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME),
                        Arrays.toString(params));
            }
        }, new SignalConfig.Builder().priority(SignalPriority.LOW).build());

        // 3. 发送混合优先级事件
        System.out.println("-- 开始发送测试事件 --");
        for (int i = 1; i <= 5; i++) {
            signals.emit("test.event", signals, e -> {},
                    new SignalContext(), "低优先级数据"+i);
            signals.emit("test.event", signals, e -> {},
                    new SignalContext(), "中优先级数据"+i);
            signals.emit("test.event", signals, e -> {},
                    new SignalContext(), "高优先级数据"+i);
            Thread.sleep(100); // 确保事件顺序
        }

        // 4. 等待处理完成
        Thread.sleep(2000);
        executor.shutdown();
    }
}