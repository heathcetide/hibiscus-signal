package com.hibiscus.signal.config;

/**
 * Enumeration for signal processing priority levels.
 * <p>
 * Priorities are used to control the execution order or urgency
 * of signal handlers in the system.
 */
public enum SignalPriority {

    /** High priority (most urgent) */
    HIGH(0),

    /** Medium priority (default) */
    MEDIUM(1),

    /** Low priority (least urgent) */
    LOW(2);

    private final int value;

    SignalPriority(int value) {
        this.value = value;
    }

    /**
     * Returns the integer value of the priority level.
     *
     * @return numerical representation of the priority
     */
    public int getValue() {
        return value;
    }
}