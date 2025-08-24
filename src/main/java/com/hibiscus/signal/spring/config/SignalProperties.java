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

    // 新增配置项
    private Long maxFileSizeBytes = 10 * 1024 * 1024L; // 10MB
    private Boolean enableFileRotation = true;
    private Integer maxBackupFiles = 10;
    private String persistenceDirectory = "logs/signals";

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

    public Long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public void setMaxFileSizeBytes(Long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public Boolean getEnableFileRotation() {
        return enableFileRotation;
    }

    public void setEnableFileRotation(Boolean enableFileRotation) {
        this.enableFileRotation = enableFileRotation;
    }

    public Integer getMaxBackupFiles() {
        return maxBackupFiles;
    }

    public void setMaxBackupFiles(Integer maxBackupFiles) {
        this.maxBackupFiles = maxBackupFiles;
    }

    public String getPersistenceDirectory() {
        return persistenceDirectory;
    }

    public void setPersistenceDirectory(String persistenceDirectory) {
        this.persistenceDirectory = persistenceDirectory;
    }

    @Override
    public String toString() {
        return "SignalProperties{" +
                "persistent=" + persistent +
                ", persistenceFile='" + persistenceFile + '\'' +
                ", maxFileSizeBytes=" + maxFileSizeBytes +
                ", enableFileRotation=" + enableFileRotation +
                ", maxBackupFiles=" + maxBackupFiles +
                ", persistenceDirectory='" + persistenceDirectory + '\'' +
                '}';
    }
}
