package com.hibiscus.signal.utils;

public class SnowflakeIdGenerator {

    private static final long START_TIMESTAMP = 1625155600000L;

    private static final long MACHINE_ID_BITS = 5L;
    private static final long DATA_CENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;

    private static final long MAX_MACHINE_ID = ~(-1L << MACHINE_ID_BITS);
    private static final long MAX_DATA_CENTER_ID = ~(-1L << DATA_CENTER_ID_BITS);
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS + DATA_CENTER_ID_BITS;
    private static final long MACHINE_ID_LEFT_SHIFT = SEQUENCE_BITS + DATA_CENTER_ID_BITS;
    private static final long DATA_CENTER_ID_LEFT_SHIFT = SEQUENCE_BITS;

    private static long lastTimestamp = -1L;
    private static long sequence = 0L;

    // 默认配置（可改成读取配置）
    private static final long MACHINE_ID = 1L;
    private static final long DATA_CENTER_ID = 1L;

    static {
        if (MACHINE_ID > MAX_MACHINE_ID || DATA_CENTER_ID > MAX_DATA_CENTER_ID) {
            throw new IllegalArgumentException("Machine ID or Data Center ID is out of range.");
        }
    }

    public static synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards. Refusing to generate id.");
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = waitUntilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0;
        }

        lastTimestamp = timestamp;

        return ((timestamp - START_TIMESTAMP) << TIMESTAMP_LEFT_SHIFT)
                | (DATA_CENTER_ID << DATA_CENTER_ID_LEFT_SHIFT)
                | (MACHINE_ID << MACHINE_ID_LEFT_SHIFT)
                | sequence;
    }

    private static long waitUntilNextMillis(long lastTs) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTs) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}
