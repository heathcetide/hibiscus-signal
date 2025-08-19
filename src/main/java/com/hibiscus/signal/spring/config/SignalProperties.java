package com.hibiscus.signal.spring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan
@ConfigurationProperties("hibiscus")
public class SignalProperties {

    private Boolean persistent = false;

    private String persistenceFile = "signal.json";

    public Boolean getPersistent() {
        return persistent;
    }

    public void setPersistent(Boolean persistent) {
        this.persistent = persistent;
    }

    public String getPersistenceFile() {
        return persistenceFile;
    }

    public void setPersistenceFile(String persistenceFile) {
        this.persistenceFile = persistenceFile;
    }
}
