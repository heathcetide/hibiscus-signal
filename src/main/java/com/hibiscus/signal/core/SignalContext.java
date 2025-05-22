package com.hibiscus.signal.core;

import com.hibiscus.signal.utils.SnowflakeIdGenerator;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SignalContext {

    private final Map<String, Object> attributes;
    private final Map<String, Object> intermediateValues;

    private String traceId;
    private String eventId;


    public SignalContext() {
        this.attributes = new ConcurrentHashMap<>();
        this.intermediateValues = new ConcurrentHashMap<>();
    }

    // -------- Attributes（业务传参） --------
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public void removeAttribute(String key) {
        attributes.remove(key);
    }

    public Map<String, Object> getAttributes() {
        return new ConcurrentHashMap<>(attributes);
    }

    public void setAttributes(Map<String, Object> newAttributes) {
        this.attributes.clear();
        if (newAttributes != null) {
            this.attributes.putAll(newAttributes);
        }
    }

    // -------- IntermediateValues（框架内部中间值） --------
    public void addIntermediateValue(String key, Object value) {
        intermediateValues.put(key, value);
    }

    public Object getIntermediateValue(String key) {
        return intermediateValues.get(key);
    }

    public Map<String, Object> getIntermediateValues() {
        return new ConcurrentHashMap<>(intermediateValues);
    }

    public void setIntermediateValues(Map<String, Object> values) {
        this.intermediateValues.clear();
        if (values != null) {
            this.intermediateValues.putAll(values);
        }
    }

    // 在现有方法后添加新方法
    public void initTrace(String eventName) {
        if (eventName == null) {
            eventName = "unknown";
        }
        this.traceId = UUID.randomUUID().toString();
        this.eventId = eventName + "_" + SnowflakeIdGenerator.nextId();
    }

    public String getTraceId() { return traceId; }
    public String getEventId() { return eventId; }

    @Override
    public String toString() {
        return "SignalContext{" +
                "attributes=" + attributes +
                ", intermediateValues=" + intermediateValues +
                '}';
    }
}
