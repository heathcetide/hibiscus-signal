package com.hibiscus.signal.config;

/**
 * 信号优先级
 */
public enum SignalPriority {

    /**
     * 高优先级
     */
    HIGH(0),

    /**
     * 中优先级
     */
    MEDIUM(1),

    /**
     * 低优先级
     */
    LOW(2);

    private final int value;

    SignalPriority(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}