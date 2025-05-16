package com.hibiscus.signal.spring.configuration;

import com.hibiscus.signal.Signals;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;

@Configuration
public class SignalAutoConfiguration {

    @ConditionalOnMissingBean
    @Primary
    @Bean
    public Signals signalManager() {
        return Signals.sig();
    }

    @Bean
    @DependsOn("signals")
    @ConditionalOnMissingBean
    public SignalAspect signalAspect(Signals Signals) {
        return new SignalAspect(Signals);
    }
}