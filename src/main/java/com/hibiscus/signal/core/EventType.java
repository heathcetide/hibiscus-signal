package com.hibiscus.signal.core;

/**
 * Enumeration of the different types of events in the Signal framework.
 * Purpose:
 * - Defines the possible operations that can occur in the signal system,
 *   such as adding handlers, removing them, pausing signals, etc.
 */
public enum EventType {
    ADD_HANDLER(0),
    REMOVE_HANDLER(1),
    PAUSE_SIGNAL(2),
    RESUME_SIGNAL(3),
    BROADCAST(4),
    REFRESH_CONFIG(5);

    // Numeric value associated with the event type
    private final int value;

    /**
     * Constructor for the EventType enum.
     *
     * @param value numeric value of the event type
     */
    EventType(int value) {
        this.value = value;
    }

    /**
     * Gets the numeric value of this event type.
     *
     * @return the numeric value
     */
    public int getValue() {
        return value;
    }

    /**
     * Retrieves the EventType corresponding to a numeric value.
     *
     * @param value the numeric value to look up
     * @return the matching EventType
     * @throws IllegalArgumentException if the value is invalid
     */
    public static EventType fromValue(int value) {
        for (EventType type : values()) {
            if (type.getValue() == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid EventType value: " + value);
    }
}

