package com.hibiscus.signal.spring.configuration;

import com.hibiscus.signal.Signals;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;

/**
 * Spring Boot auto-configuration class for the Signal framework.
 * <p>
 * Automatically configures the {@link Signals} manager and {@link SignalAspect}
 * if no user-defined beans are provided.
 */
@Configuration
public class SignalAutoConfiguration {

    /**
     * Provides a default {@link Signals} bean if one is not already defined.
     *
     * @return a singleton instance of the signal manager
     */
    @ConditionalOnMissingBean
    @Primary
    @Bean
    public Signals signalManager() {
        return Signals.sig();
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