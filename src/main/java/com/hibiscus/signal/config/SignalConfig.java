package com.hibiscus.signal.config;

/**
 * 配置类，用于定义信号处理的各种参数和行为。
 */
public class SignalConfig {

    /**
     * 是否异步处理信号。
     */
    private boolean async;

    /**
     * 最大重试次数。
     */
    private int maxRetries;

    /**
     * 重试延迟时间（毫秒）。
     */
    private long retryDelayMs;

    /**
     * 最大信号处理器数量。
     */
    private int maxHandlers;

    /**
     * 信号处理超时时间（毫秒）。
     */
    private long timeoutMs;

    /**
     * 是否记录指标。
     */
    private boolean recordMetrics;

    /**
     * 信号优先级。
     */
    private SignalPriority priority;

    /**
     * 信号组名称。
     */
    private String groupName;

    /**
     * 是否持久化信号。
     */
    private boolean persistent;

    /**
     * 私有构造方法，通过 Builder 构建实例。
     * // TODO 建造者模式
     *
     * @param builder 包含配置参数的 Builder 实例
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
     * 用于构建 SignalConfig 实例的内部静态类。
     */
    public static class Builder {
        /**
         * 是否异步处理信号，默认为 false。
         */
        private boolean async = false;

        /**
         * 最大重试次数，默认为 3。
         */
        private int maxRetries = 3;

        /**
         * 重试延迟时间（毫秒），默认为 1000。
         */
        private long retryDelayMs = 1000;

        /**
         * 最大信号处理器数量，默认为 100。
         */
        private int maxHandlers = 100;

        /**
         * 信号处理超时时间（毫秒），默认为 5000。
         */
        private long timeoutMs = 5000;

        /**
         * 是否记录指标，默认为 true。
         */
        private boolean recordMetrics = true;

        /**
         * 信号优先级，默认为 SignalPriority.MEDIUM。
         */
        private SignalPriority priority = SignalPriority.MEDIUM;

        /**
         * 信号组名称，默认为 null。
         */
        private String groupName = null;

        /**
         * 是否持久化信号，默认为 false。
         */
        private boolean persistent = false;

        /**
         * 设置是否异步处理信号。
         *
         * @param async 是否异步处理信号
         * @return Builder 实例
         */
        public Builder async(boolean async) {
            this.async = async;
            return this;
        }

        /**
         * 设置最大重试次数。
         *
         * @param maxRetries 最大重试次数
         * @return Builder 实例
         */
        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        /**
         * 设置重试延迟时间（毫秒）。
         *
         * @param retryDelayMs 重试延迟时间（毫秒）
         * @return Builder 实例
         */
        public Builder retryDelayMs(long retryDelayMs) {
            this.retryDelayMs = retryDelayMs;
            return this;
        }

        /**
         * 设置最大信号处理器数量。
         *
         * @param maxHandlers 最大信号处理器数量
         * @return Builder 实例
         */
        public Builder maxHandlers(int maxHandlers) {
            this.maxHandlers = maxHandlers;
            return this;
        }

        /**
         * 设置信号处理超时时间（毫秒）。
         *
         * @param timeoutMs 信号处理超时时间（毫秒）
         * @return Builder 实例
         */
        public Builder timeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }

        /**
         * 设置是否记录指标。
         *
         * @param recordMetrics 是否记录指标
         * @return Builder 实例
         */
        public Builder recordMetrics(boolean recordMetrics) {
            this.recordMetrics = recordMetrics;
            return this;
        }

        /**
         * 设置信号优先级。
         *
         * @param priority 信号优先级
         * @return Builder 实例
         */
        public Builder priority(SignalPriority priority) {
            this.priority = priority;
            return this;
        }

        /**
         * 设置信号组名称。
         *
         * @param groupName 信号组名称
         * @return Builder 实例
         */
        public Builder groupName(String groupName) {
            this.groupName = groupName;
            return this;
        }

        /**
         * 设置是否持久化信号。
         *
         * @param persistent 是否持久化信号
         * @return Builder 实例
         */
        public Builder persistent(boolean persistent) {
            this.persistent = persistent;
            return this;
        }

        /**
         * 构建并返回 SignalConfig 实例。
         *
         * @return SignalConfig 实例
         */
        public SignalConfig build() {
            return new SignalConfig(this);
        }
    }

    /**
     * 获取是否异步处理信号。
     *
     * @return 是否异步处理信号
     */
    public boolean isAsync() {
        return async;
    }

    /**
     * 获取最大重试次数。
     *
     * @return 最大重试次数
     */
    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * 获取重试延迟时间（毫秒）。
     *
     * @return 重试延迟时间（毫秒）
     */
    public long getRetryDelayMs() {
        return retryDelayMs;
    }

    /**
     * 获取最大信号处理器数量。
     *
     * @return 最大信号处理器数量
     */
    public int getMaxHandlers() {
        return maxHandlers;
    }

    /**
     * 获取信号处理超时时间（毫秒）。
     *
     * @return 信号处理超时时间（毫秒）
     */
    public long getTimeoutMs() {
        return timeoutMs;
    }

    /**
     * 获取是否记录指标。
     *
     * @return 是否记录指标
     */
    public boolean isRecordMetrics() {
        return recordMetrics;
    }

    /**
     * 获取信号优先级。
     *
     * @return 信号优先级
     */
    public SignalPriority getPriority() {
        return priority;
    }

    /**
     * 获取信号组名称。
     *
     * @return 信号组名称
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * 获取是否持久化信号。
     *
     * @return 是否持久化信号
     */
    public boolean isPersistent() {
        return persistent;
    }
}
