package com.hibiscus.signal.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Configuration class that provides a default ExecutorService for the Signal framework.
 * This executor is used to handle asynchronous signal emissions and handler executions.
 * If the user has not defined their own "signalExecutor" bean, this default implementation will be used.
 */
@Configuration
public class SignalDefaultThreadPoolConfig {

    /**
     * Creates a default ExecutorService (ThreadPoolExecutor) for the Signal system.
     * Configuration details:
     * - Core thread count: number of CPU cores
     * - Maximum thread count: (CPU cores * 2) + 1
     * - Idle thread timeout: 60 seconds
     * - Queue capacity: 2000 tasks
     * - Rejection policy: CallerRunsPolicy (task runs in the caller's thread if the queue is full)
     * - Custom thread factory with descriptive thread names and daemon threads
     *
     * @return ExecutorService to handle Signal tasks asynchronously
     */
    @Bean(name = "signalExecutor")
    @ConditionalOnMissingBean(name = "signalExecutor") // Only active if user hasn't defined their own bean
    public ExecutorService defaultSignalExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                // Core thread pool size: number of CPU cores
                Runtime.getRuntime().availableProcessors(),
                // Maximum thread pool size: (CPU cores * 2) + 1
                Runtime.getRuntime().availableProcessors() * 2 + 1,
                // Time for which idle threads wait for new tasks before terminating
                60,
                TimeUnit.SECONDS,
                // Bounded task queue with a capacity of 2000
                new LinkedBlockingQueue<>(2000),
                // Custom thread factory that sets thread names and daemon status
                new ThreadFactory() {
                    private final AtomicInteger count = new AtomicInteger();

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r);
                        // Naming pattern: "signal-thread-<number>"
                        t.setName("signal-thread-" + count.getAndIncrement());
                        // Set as daemon threads so they don't block JVM shutdown
                        t.setDaemon(true);
                        return t;
                    }
                },
                // If the pool and queue are full, let the caller's thread execute the task
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        // 添加关闭钩子，确保JVM退出时线程池能正确关闭
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!executor.isShutdown()) {
                executor.shutdown();
                try {
                    if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                        executor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        }, "signal-executor-shutdown-hook"));
        
        return executor;
    }
}
