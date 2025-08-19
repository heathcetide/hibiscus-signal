package com.hibiscus.docs;

import com.hibiscus.docs.config.SignalConfig;
import com.hibiscus.docs.core.SignalMetrics;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class StepLoadTest {

    private static final SignalMetrics metrics = new SignalMetrics();

    public static void main(String[] args) throws Exception {
        // ✅ 输出重定向
        try {
            PrintStream fileOut = new PrintStream(new FileOutputStream("output.txt", false));
            System.setOut(fileOut);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        ExecutorService executor = new ThreadPoolExecutor(
                200, 500,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(200_000),
                new ThreadPoolExecutor.AbortPolicy()
        );

        Signals signals = new Signals(executor);

        // 注册处理器
        signals.connect("test.event", (sender, params) -> {
            metrics.recordProcessed();
        }, new SignalConfig.Builder()
                .async(true)
                .maxRetries(1)
                .timeoutMs(1000)
                .build());

        performStepLoadTest(signals, executor);

        executor.shutdownNow();
    }

    private static void performStepLoadTest(Signals signals, ExecutorService executor) throws InterruptedException {
        int[] stages = {1000, 2000, 5000, 10000, 20000}; // QPS 阶梯
        for (int qps : stages) {
            System.out.println("\n=== 开始测试: " + qps + " QPS (持续 2 秒) ===");

            AtomicInteger success = new AtomicInteger();
            AtomicInteger failure = new AtomicInteger();

            CountDownLatch latch = new CountDownLatch(qps * 2); // 总请求数 = QPS * 秒数

            long start = System.currentTimeMillis();

            // 并发发射信号
            for (int i = 0; i < qps * 2; i++) {
                executor.execute(() -> {
                    try {
                        signals.emit("test.event", signals, e -> failure.incrementAndGet());
                        success.incrementAndGet();
                    } catch (Exception e) {
                        failure.incrementAndGet();
                        e.printStackTrace(); // ✅ 异常也输出到 output.txt
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(); // 等待所有请求结束
            long elapsed = System.currentTimeMillis() - start;

            System.out.printf("阶段 [%d QPS]:\n", qps);
            System.out.println("  成功请求: " + success);
            System.out.println("  失败请求: " + failure);
            System.out.printf("  实际QPS: %.2f\n", success.get() / (elapsed / 1000.0));
            printJvmStats();
            printThreadPoolStats(executor);
            System.out.println("-----------------------------");

            Thread.sleep(1000); // 小休息，防止连续冲击
        }
    }

    private static void printJvmStats() {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        long max = runtime.maxMemory();
        long gcCount = ManagementFactory.getGarbageCollectorMXBeans().stream()
                .mapToLong(GarbageCollectorMXBean::getCollectionCount)
                .sum();

        System.out.printf("[JVM] 内存使用: %.1f / %.1f MB | GC次数: %d%n",
                used / 1024.0 / 1024,
                max / 1024.0 / 1024,
                gcCount);
    }

    private static void printThreadPoolStats(ExecutorService executor) {
        if (executor instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor tpe = (ThreadPoolExecutor) executor;
            System.out.printf("[线程池] 活跃: %d 核心: %d 最大: %d 队列: %d%n",
                    tpe.getActiveCount(),
                    tpe.getCorePoolSize(),
                    tpe.getMaximumPoolSize(),
                    tpe.getQueue().size());
        }
    }
}
