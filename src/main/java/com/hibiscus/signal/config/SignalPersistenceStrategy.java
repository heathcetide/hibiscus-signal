package com.hibiscus.signal.config;

/**
 * 信号持久化策略
 * 支持多种存储方式的组合使用
 * 
 * @author heathcetide
 */
public enum SignalPersistenceStrategy {
    
    /**
     * 仅文件存储
     */
    FILE_ONLY("file"),
    
    /**
     * 仅数据库存储
     */
    DATABASE_ONLY("database"),
    
    /**
     * 仅Redis存储
     */
    REDIS_ONLY("redis"),
    
    /**
     * 仅MQ存储
     */
    MQ_ONLY("mq"),
    
    /**
     * 文件 + 数据库（双重保障）
     */
    FILE_AND_DATABASE("file,database"),
    
    /**
     * 数据库 + Redis（高性能缓存）
     */
    DATABASE_AND_REDIS("database,redis"),
    
    /**
     * 数据库 + MQ（异步解耦）
     */
    DATABASE_AND_MQ("database,mq"),
    
    /**
     * Redis + MQ（高性能异步）
     */
    REDIS_AND_MQ("redis,mq"),
    
    /**
     * 数据库 + Redis + MQ（完整方案）
     */
    ALL("database,redis,mq"),
    
    /**
     * 自定义组合
     */
    CUSTOM("custom");
    
    private final String value;
    
    SignalPersistenceStrategy(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * 检查是否包含指定存储方式
     */
    public boolean contains(String storage) {
        return value.contains(storage);
    }
    
    /**
     * 检查是否支持文件存储
     */
    public boolean supportsFile() {
        return contains("file");
    }
    
    /**
     * 检查是否支持数据库存储
     */
    public boolean supportsDatabase() {
        return contains("database");
    }
    
    /**
     * 检查是否支持Redis存储
     */
    public boolean supportsRedis() {
        return contains("redis");
    }
    
    /**
     * 检查是否支持MQ存储
     */
    public boolean supportsMq() {
        return contains("mq");
    }
    
    /**
     * 从字符串创建策略
     */
    public static SignalPersistenceStrategy fromString(String value) {
        for (SignalPersistenceStrategy strategy : values()) {
            if (strategy.value.equals(value)) {
                return strategy;
            }
        }
        return CUSTOM;
    }
}
