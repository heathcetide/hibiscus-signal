package com.hibiscus.signal.core;

public enum EventType {
    ADD_HANDLER(0),
    REMOVE_HANDLER(1),
    PAUSE_SIGNAL(2),
    RESUME_SIGNAL(3),
    BROADCAST(4),
    REFRESH_CONFIG(5);

    private final int value;

    // 构造器
    EventType(int value) {
        this.value = value;
    }

    // 获取枚举值
    public int getValue() {
        return value;
    }

    // 根据值获取枚举
    public static EventType fromValue(int value) {
        for (EventType type : values()) {
            if (type.getValue() == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid EventType value: " + value);
    }
}
