package com.hibiscus.signal.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class SignalDefaultThreadPoolConfig {

    @Bean(name = "signalExecutor")
    @ConditionalOnMissingBean(name = "signalExecutor") // 只有用户没提供时才生效
    public ExecutorService defaultSignalExecutor() {
        return new ThreadPoolExecutor(
                Runtime.getRuntime().availableProcessors(),
                Runtime.getRuntime().availableProcessors() * 2 + 1,
                60,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(2000),
                new ThreadFactory() {
                    private final AtomicInteger count = new AtomicInteger();
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r);
                        t.setName("signal-thread-" + count.getAndIncrement());
                        t.setDaemon(true);
                        return t;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
