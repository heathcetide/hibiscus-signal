package com.hibiscus.signal.spring.configuration;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.hibiscus.signal.core.SignalContext;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for managing per-thread signal context using {@link ThreadLocal}.
 * <p>
 * This enables signal handlers and emitters to share contextual data
 * safely within the same thread during execution.
 */
@Component
public class SignalContextCollector {

    /**
     * Thread-local holder for storing signal-related context data.
     */
    private static final TransmittableThreadLocal<Map<String, Object>> contextHolder = new TransmittableThreadLocal<>();

    /**
     * Collects a key-value pair into the current thread's context map.
     *
     * @param key   the key to store
     * @param value the associated value
     */
    public static void collect(String key, Object value) {
        if (contextHolder.get() == null) {
            contextHolder.set(new HashMap<>());
        }
        contextHolder.get().put(key, value);
    }

    /**
     * Retrieves the current thread's context map and clears it.
     *
     * @return the context map or an empty map if none exists
     */
    public static Map<String, Object> getAndClear() {
        Map<String, Object> context = contextHolder.get();
        contextHolder.remove();
        return context != null ? context : new HashMap<>();
    }

    /**
     * Retrieves a value by key from the current thread's context.
     *
     * @param key the context key
     * @return the value associated with the key, or {@code null} if not found
     */
    public static Object getValue(String key) {
        Map<String, Object> context = contextHolder.get();
        return context != null ? context.get(key) : null;
    }

    /**
     * Clears the entire context map for the current thread.
     */
    public static void clear() {
        contextHolder.remove();
    }

    /**
     * Retrieves the current thread's context map.
     *
     * @return the context map or an empty map if none exists
     */
    public static Map<String, Object> getContext() {
        return contextHolder.get() != null ? contextHolder.get() : new HashMap<>();
    }

    /**
     * Collects multiple key-value pairs into the current thread's context map.
     * <p>
     * If the context map does not yet exist, it will be initialized.
     *
     * @param values the map of key-value pairs to add to the thread-local context
     */
    public static void collectAll(Map<String, Object> values) {
        if (contextHolder.get() == null) {
            contextHolder.set(new HashMap<>());
        }
        contextHolder.get().putAll(values);
    }

    /**
     * Collects tracing information from the given SignalContext and logs or stores it
     * under a predefined key (e.g., "_trace"). This method is useful for debugging,
     * monitoring, or auditing signal processing across distributed components.
     *
     * @param context the SignalContext containing metadata for tracing (e.g., traceId, eventId)
     */
    public static void collectTraceInfo(SignalContext context) {
        Map<String, Object> traceInfo = new HashMap<>();
        traceInfo.put("traceId", context.getTraceId());
        traceInfo.put("eventId", context.getEventId());
        collect("_trace", traceInfo);
    }
}