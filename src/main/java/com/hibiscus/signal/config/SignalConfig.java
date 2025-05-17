package com.hibiscus.signal.config;

/**
 * Configuration class that defines behavior for signal handling.
 * <p>
 * Includes properties such as retry policy, execution mode,
 * priority level, timeout, and metrics options.
 * Use {@link SignalConfig.Builder} to construct an instance.
 */
public class SignalConfig {

    /** Whether to handle the signal asynchronously. */
    private final boolean async;

    /** Maximum number of retry attempts. */
    private final int maxRetries;

    /** Delay between retries in milliseconds. */
    private final long retryDelayMs;

    /** Maximum number of concurrent handlers allowed. */
    private final int maxHandlers;

    /** Timeout for signal processing in milliseconds. */
    private final long timeoutMs;

    /** Whether to record processing metrics. */
    private final boolean recordMetrics;

    /** Priority of the signal. */
    private final SignalPriority priority;

    /** Logical group name for signal categorization. */
    private final String groupName;

    /** Whether the signal is persistent across restarts. */
    private final boolean persistent;

    /**
     * Private constructor used by the builder.
     *
     * @param builder the builder instance with configuration values
     */
    private SignalConfig(Builder builder) {
        this.async = builder.async;
        this.maxRetries = builder.maxRetries;
        this.retryDelayMs = builder.retryDelayMs;
        this.maxHandlers = builder.maxHandlers;
        this.timeoutMs = builder.timeoutMs;
        this.recordMetrics = builder.recordMetrics;
        this.priority = builder.priority;
        this.groupName = builder.groupName;
        this.persistent = builder.persistent;
    }

    /**
     * Builder class for creating {@link SignalConfig} instances with custom settings.
     */
    public static class Builder {

        private boolean async = false;
        private int maxRetries = 3;
        private long retryDelayMs = 1000;
        private int maxHandlers = 100;
        private long timeoutMs = 5000;
        private boolean recordMetrics = true;
        private SignalPriority priority = SignalPriority.MEDIUM;
        private String groupName = null;
        private boolean persistent = false;

        /**
         * Sets whether the signal should be handled asynchronously.
         *
         * @param async true for async execution
         * @return the builder instance
         */
        public Builder async(boolean async) {
            this.async = async;
            return this;
        }

        /**
         * Sets the maximum number of retry attempts.
         *
         * @param maxRetries number of retries
         * @return the builder instance
         */
        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        /**
         * Sets the delay between retries in milliseconds.
         *
         * @param retryDelayMs delay in ms
         * @return the builder instance
         */
        public Builder retryDelayMs(long retryDelayMs) {
            this.retryDelayMs = retryDelayMs;
            return this;
        }

        /**
         * Sets the maximum number of concurrent signal handlers.
         *
         * @param maxHandlers handler limit
         * @return the builder instance
         */
        public Builder maxHandlers(int maxHandlers) {
            this.maxHandlers = maxHandlers;
            return this;
        }

        /**
         * Sets the signal handling timeout in milliseconds.
         *
         * @param timeoutMs timeout in ms
         * @return the builder instance
         */
        public Builder timeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }

        /**
         * Sets whether metrics should be recorded.
         *
         * @param recordMetrics true to enable metrics
         * @return the builder instance
         */
        public Builder recordMetrics(boolean recordMetrics) {
            this.recordMetrics = recordMetrics;
            return this;
        }

        /**
         * Sets the signal priority.
         *
         * @param priority priority level
         * @return the builder instance
         */
        public Builder priority(SignalPriority priority) {
            this.priority = priority;
            return this;
        }

        /**
         * Sets the signal group name.
         *
         * @param groupName group identifier
         * @return the builder instance
         */
        public Builder groupName(String groupName) {
            this.groupName = groupName;
            return this;
        }

        /**
         * Sets whether the signal should be persistent.
         *
         * @param persistent true to enable persistence
         * @return the builder instance
         */
        public Builder persistent(boolean persistent) {
            this.persistent = persistent;
            return this;
        }

        /**
         * Builds and returns a {@link SignalConfig} instance with the specified settings.
         *
         * @return a new SignalConfig object
         */
        public SignalConfig build() {
            return new SignalConfig(this);
        }
    }

    // Getters

    public boolean isAsync() {
        return async;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public long getRetryDelayMs() {
        return retryDelayMs;
    }

    public int getMaxHandlers() {
        return maxHandlers;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public boolean isRecordMetrics() {
        return recordMetrics;
    }

    public SignalPriority getPriority() {
        return priority;
    }

    public String getGroupName() {
        return groupName;
    }

    public boolean isPersistent() {
        return persistent;
    }
}
