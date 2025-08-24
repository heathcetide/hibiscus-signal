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

    // 数据库持久化配置
    private Boolean databasePersistent = false;
    private String databaseTableName = "signal_events";
    private Integer databaseRetentionDays = 7;
    private Boolean enableDatabaseCleanup = true;
    
    // Redis配置
    private Boolean redisEnabled = false;
    private String redisHost = "localhost";
    private Integer redisPort = 6379;
    private String redisPassword = "";
    private Integer redisDatabase = 0;
    private Integer redisExpireSeconds = 86400; // 24小时
    
    // MQ配置
    private Boolean mqEnabled = false;
    private String mqType = "rabbitmq"; // rabbitmq, kafka, rocketmq
    private String mqHost = "localhost";
    private Integer mqPort = 5672;
    private String mqUsername = "guest";
    private String mqPassword = "guest";
    private String mqVirtualHost = "/";
    
    // 持久化策略
    private String persistenceStrategy = "database"; // file, database, redis, mq, database_redis, database_mq, all

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

    public Boolean getDatabasePersistent() {
        return databasePersistent;
    }

    public void setDatabasePersistent(Boolean databasePersistent) {
        this.databasePersistent = databasePersistent;
    }

    public String getDatabaseTableName() {
        return databaseTableName;
    }

    public void setDatabaseTableName(String databaseTableName) {
        this.databaseTableName = databaseTableName;
    }

    public Integer getDatabaseRetentionDays() {
        return databaseRetentionDays;
    }

    public void setDatabaseRetentionDays(Integer databaseRetentionDays) {
        this.databaseRetentionDays = databaseRetentionDays;
    }

    public Boolean getEnableDatabaseCleanup() {
        return enableDatabaseCleanup;
    }

    public void setEnableDatabaseCleanup(Boolean enableDatabaseCleanup) {
        this.enableDatabaseCleanup = enableDatabaseCleanup;
    }
    
    // Redis配置的getter和setter
    public Boolean getRedisEnabled() {
        return redisEnabled;
    }
    
    public void setRedisEnabled(Boolean redisEnabled) {
        this.redisEnabled = redisEnabled;
    }
    
    public String getRedisHost() {
        return redisHost;
    }
    
    public void setRedisHost(String redisHost) {
        this.redisHost = redisHost;
    }
    
    public Integer getRedisPort() {
        return redisPort;
    }
    
    public void setRedisPort(Integer redisPort) {
        this.redisPort = redisPort;
    }
    
    public String getRedisPassword() {
        return redisPassword;
    }
    
    public void setRedisPassword(String redisPassword) {
        this.redisPassword = redisPassword;
    }
    
    public Integer getRedisDatabase() {
        return redisDatabase;
    }
    
    public void setRedisDatabase(Integer redisDatabase) {
        this.redisDatabase = redisDatabase;
    }
    
    public Integer getRedisExpireSeconds() {
        return redisExpireSeconds;
    }
    
    public void setRedisExpireSeconds(Integer redisExpireSeconds) {
        this.redisExpireSeconds = redisExpireSeconds;
    }
    
    // MQ配置的getter和setter
    public Boolean getMqEnabled() {
        return mqEnabled;
    }
    
    public void setMqEnabled(Boolean mqEnabled) {
        this.mqEnabled = mqEnabled;
    }
    
    public String getMqType() {
        return mqType;
    }
    
    public void setMqType(String mqType) {
        this.mqType = mqType;
    }
    
    public String getMqHost() {
        return mqHost;
    }
    
    public void setMqHost(String mqHost) {
        this.mqHost = mqHost;
    }
    
    public Integer getMqPort() {
        return mqPort;
    }
    
    public void setMqPort(Integer mqPort) {
        this.mqPort = mqPort;
    }
    
    public String getMqUsername() {
        return mqUsername;
    }
    
    public void setMqUsername(String mqUsername) {
        this.mqUsername = mqUsername;
    }
    
    public String getMqPassword() {
        return mqPassword;
    }
    
    public void setMqPassword(String mqPassword) {
        this.mqPassword = mqPassword;
    }
    
    public String getMqVirtualHost() {
        return mqVirtualHost;
    }
    
    public void setMqVirtualHost(String mqVirtualHost) {
        this.mqVirtualHost = mqVirtualHost;
    }
    
    // 持久化策略的getter和setter
    public String getPersistenceStrategy() {
        return persistenceStrategy;
    }
    
    public void setPersistenceStrategy(String persistenceStrategy) {
        this.persistenceStrategy = persistenceStrategy;
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
                ", databasePersistent=" + databasePersistent +
                ", databaseTableName='" + databaseTableName + '\'' +
                ", databaseRetentionDays=" + databaseRetentionDays +
                ", enableDatabaseCleanup=" + enableDatabaseCleanup +
                '}';
    }
}
