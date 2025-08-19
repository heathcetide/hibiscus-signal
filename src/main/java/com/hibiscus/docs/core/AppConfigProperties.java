package com.hibiscus.docs.core;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "hibiscus")
public class AppConfigProperties {
    
    private Helper helper = new Helper();
    private Security security = new Security();
    private Advanced advanced = new Advanced();
    private Testing testing = new Testing();

    public static class Helper {
        private String scanPath = "com.hibiscus.docs.core";

        public String getScanPath() {
            return scanPath;
        }

        public void setScanPath(String scanPath) {
            this.scanPath = scanPath;
        }
    }

    public static class Security {
        private boolean enabled = true;
        private List<String> allowedIps;
        private boolean allowLocalhost = true;
        private String mode = "both"; // ip, token, both
        private String accessToken = "hibiscus-api-2024";
        private int tokenExpireHours = 24;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getAllowedIps() {
            return allowedIps;
        }

        public void setAllowedIps(List<String> allowedIps) {
            this.allowedIps = allowedIps;
        }

        public boolean isAllowLocalhost() {
            return allowLocalhost;
        }

        public void setAllowLocalhost(boolean allowLocalhost) {
            this.allowLocalhost = allowLocalhost;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public int getTokenExpireHours() {
            return tokenExpireHours;
        }

        public void setTokenExpireHours(int tokenExpireHours) {
            this.tokenExpireHours = tokenExpireHours;
        }
    }

    public static class Advanced {
        private boolean enableRequestLogging = true;
        private boolean enablePerformanceMonitoring = true;
        private boolean enableRateLimiting = true;
        private RateLimit rateLimit = new RateLimit();
        private Cache cache = new Cache();

        public boolean isEnableRequestLogging() {
            return enableRequestLogging;
        }

        public void setEnableRequestLogging(boolean enableRequestLogging) {
            this.enableRequestLogging = enableRequestLogging;
        }

        public boolean isEnablePerformanceMonitoring() {
            return enablePerformanceMonitoring;
        }

        public void setEnablePerformanceMonitoring(boolean enablePerformanceMonitoring) {
            this.enablePerformanceMonitoring = enablePerformanceMonitoring;
        }

        public boolean isEnableRateLimiting() {
            return enableRateLimiting;
        }

        public void setEnableRateLimiting(boolean enableRateLimiting) {
            this.enableRateLimiting = enableRateLimiting;
        }

        public RateLimit getRateLimit() {
            return rateLimit;
        }

        public void setRateLimit(RateLimit rateLimit) {
            this.rateLimit = rateLimit;
        }

        public Cache getCache() {
            return cache;
        }

        public void setCache(Cache cache) {
            this.cache = cache;
        }
    }

    public static class RateLimit {
        private int requestsPerMinute = 100;
        private int burstCapacity = 20;

        public int getRequestsPerMinute() {
            return requestsPerMinute;
        }

        public void setRequestsPerMinute(int requestsPerMinute) {
            this.requestsPerMinute = requestsPerMinute;
        }

        public int getBurstCapacity() {
            return burstCapacity;
        }

        public void setBurstCapacity(int burstCapacity) {
            this.burstCapacity = burstCapacity;
        }
    }

    public static class Cache {
        private boolean enabled = true;
        private int ttlSeconds = 300;
        private int maxSize = 1000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getTtlSeconds() {
            return ttlSeconds;
        }

        public void setTtlSeconds(int ttlSeconds) {
            this.ttlSeconds = ttlSeconds;
        }

        public int getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }
    }

    public static class Testing {
        private Map<String, String> defaultHeaders;
        private Timeout timeout = new Timeout();
        private boolean allowCorsTesting = true;
        private Map<String, Environment> environments;

        public Map<String, String> getDefaultHeaders() {
            return defaultHeaders;
        }

        public void setDefaultHeaders(Map<String, String> defaultHeaders) {
            this.defaultHeaders = defaultHeaders;
        }

        public Timeout getTimeout() {
            return timeout;
        }

        public void setTimeout(Timeout timeout) {
            this.timeout = timeout;
        }

        public boolean isAllowCorsTesting() {
            return allowCorsTesting;
        }

        public void setAllowCorsTesting(boolean allowCorsTesting) {
            this.allowCorsTesting = allowCorsTesting;
        }

        public Map<String, Environment> getEnvironments() {
            return environments;
        }

        public void setEnvironments(Map<String, Environment> environments) {
            this.environments = environments;
        }
    }

    public static class Timeout {
        private int connect = 5000;
        private int read = 30000;

        public int getConnect() {
            return connect;
        }

        public void setConnect(int connect) {
            this.connect = connect;
        }

        public int getRead() {
            return read;
        }

        public void setRead(int read) {
            this.read = read;
        }
    }

    public static class Environment {
        private String baseUrl;
        private String description;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    // Getters and Setters
    public Helper getHelper() {
        return helper;
    }

    public void setHelper(Helper helper) {
        this.helper = helper;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public Advanced getAdvanced() {
        return advanced;
    }

    public void setAdvanced(Advanced advanced) {
        this.advanced = advanced;
    }

    public Testing getTesting() {
        return testing;
    }

    public void setTesting(Testing testing) {
        this.testing = testing;
    }
}
