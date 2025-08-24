package com.hibiscus.signal.spring.configuration;

import com.hibiscus.signal.Signals;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.ExecutorService;

/**
 * Spring Boot auto-configuration class for the Signal framework.
 * <p>
 * Automatically configures the {@link Signals} manager and {@link SignalAspect}
 * if no user-defined beans are provided.
 */
@Configuration
@Import({SignalDatabaseConfiguration.class, SignalRedisConfiguration.class, SignalMqConfiguration.class})
public class SignalAutoConfiguration {

    /*
     * Provides a default {@link Signals} bean if one is not already defined.
     *
     * @return a singleton instance of the signal manager
     */
    /**
     * 提供默认的 Signal 管理器实例，使用线程池自动注入
     */
    @Bean(name = "signals") // 明确 Bean 名字
    @Primary
    @ConditionalOnMissingBean(Signals.class)
    public Signals signalManager(@Qualifier("signalExecutor") ExecutorService executorService) {
        return new Signals(executorService);
    }

    /**
     * Provides the {@link SignalAspect} to handle annotation-based signal emitting
     * and handler registration.
     *
     * @param signals the signal manager bean
     * @return the aspect that manages signal behavior
     */
    @Bean
    @DependsOn("signals")
    @ConditionalOnMissingBean
    public SignalAspect signalAspect(Signals signals) {
        return new SignalAspect(signals);
    }

}