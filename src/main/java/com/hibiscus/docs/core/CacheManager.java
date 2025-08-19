package com.hibiscus.docs.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class CacheManager {

    @Autowired
    private AppConfigProperties appConfigProperties;

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<String> accessOrder = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

    public CacheManager() {
        // 构造函数中不启动任务，等依赖注入完成后再初始化
    }
    
    @PostConstruct
    public void init() {
        if (appConfigProperties != null && appConfigProperties.getAdvanced() != null && 
            appConfigProperties.getAdvanced().getCache() != null) {
            startCleanupTask();
        }
    }

    /**
     * 存储缓存项
     */
    public void put(String key, Object value) {
        if (appConfigProperties == null || appConfigProperties.getAdvanced() == null || 
            appConfigProperties.getAdvanced().getCache() == null || 
            !appConfigProperties.getAdvanced().getCache().isEnabled()) {
            return;
        }

        // 检查缓存大小限制
        if (cache.size() >= appConfigProperties.getAdvanced().getCache().getMaxSize()) {
            evictOldest();
        }

        CacheEntry entry = new CacheEntry(value, System.currentTimeMillis());
        cache.put(key, entry);
        accessOrder.offer(key);
        
        System.out.printf("[缓存] 存储: %s, 大小: %d%n", key, cache.size());
    }

    /**
     * 获取缓存项
     */
    public Object get(String key) {
        if (appConfigProperties == null || appConfigProperties.getAdvanced() == null || 
            appConfigProperties.getAdvanced().getCache() == null || 
            !appConfigProperties.getAdvanced().getCache().isEnabled()) {
            return null;
        }

        CacheEntry entry = cache.get(key);
        if (entry != null) {
            // 检查是否过期
            if (isExpired(entry)) {
                cache.remove(key);
                accessOrder.remove(key);
                return null;
            }
            
            // 更新访问时间
            entry.lastAccessTime = System.currentTimeMillis();
            accessOrder.remove(key);
            accessOrder.offer(key);
            
            System.out.printf("[缓存] 命中: %s%n", key);
            return entry.value;
        }
        
        System.out.printf("[缓存] 未命中: %s%n", key);
        return null;
    }

    /**
     * 删除缓存项
     */
    public void remove(String key) {
        cache.remove(key);
        accessOrder.remove(key);
        System.out.printf("[缓存] 删除: %s%n", key);
    }

    /**
     * 清空所有缓存
     */
    public void clear() {
        cache.clear();
        accessOrder.clear();
        System.out.println("[缓存] 清空所有缓存");
    }

    /**
     * 检查缓存项是否过期
     */
    private boolean isExpired(CacheEntry entry) {
        if (appConfigProperties == null || appConfigProperties.getAdvanced() == null || 
            appConfigProperties.getAdvanced().getCache() == null) {
            return true; // 如果配置不可用，认为已过期
        }
        long ttl = appConfigProperties.getAdvanced().getCache().getTtlSeconds() * 1000L;
        return System.currentTimeMillis() - entry.creationTime > ttl;
    }

    /**
     * 驱逐最旧的缓存项
     */
    private void evictOldest() {
        String oldestKey = accessOrder.poll();
        if (oldestKey != null) {
            cache.remove(oldestKey);
            System.out.printf("[缓存] 驱逐最旧项: %s%n", oldestKey);
        }
    }

    /**
     * 启动定期清理任务
     */
    private void startCleanupTask() {
        cleanupExecutor.scheduleAtFixedRate(() -> {
            try {
                cleanupExpiredEntries();
            } catch (Exception e) {
                System.err.println("[缓存] 清理任务异常: " + e.getMessage());
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    /**
     * 清理过期的缓存项
     */
    private void cleanupExpiredEntries() {
        int beforeSize = cache.size();
        cache.entrySet().removeIf(entry -> {
            if (isExpired(entry.getValue())) {
                accessOrder.remove(entry.getKey());
                return true;
            }
            return false;
        });
        
        int afterSize = cache.size();
        if (beforeSize != afterSize) {
            System.out.printf("[缓存] 清理过期项: %d -> %d%n", beforeSize, afterSize);
        }
    }

    /**
     * 获取缓存统计信息
     */
    public CacheStats getStats() {
        if (appConfigProperties == null || appConfigProperties.getAdvanced() == null || 
            appConfigProperties.getAdvanced().getCache() == null) {
            return new CacheStats(cache.size(), 0, 0);
        }
        return new CacheStats(
            cache.size(),
            appConfigProperties.getAdvanced().getCache().getMaxSize(),
            appConfigProperties.getAdvanced().getCache().getTtlSeconds()
        );
    }

    /**
     * 缓存项内部类
     */
    private static class CacheEntry {
        private final Object value;
        private final long creationTime;
        private long lastAccessTime;

        public CacheEntry(Object value, long creationTime) {
            this.value = value;
            this.creationTime = creationTime;
            this.lastAccessTime = creationTime;
        }
    }

    /**
     * 缓存统计信息
     */
    public static class CacheStats {
        private final int currentSize;
        private final int maxSize;
        private final int ttlSeconds;

        public CacheStats(int currentSize, int maxSize, int ttlSeconds) {
            this.currentSize = currentSize;
            this.maxSize = maxSize;
            this.ttlSeconds = ttlSeconds;
        }

        public int getCurrentSize() { return currentSize; }
        public int getMaxSize() { return maxSize; }
        public int getTtlSeconds() { return ttlSeconds; }
        public double getUsagePercentage() { return (double) currentSize / maxSize * 100; }
    }
}
