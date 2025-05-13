package com.hibiscus.signal.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SignalContext {

    private final Map<String, Object> attributes;

    private Map<String, Object> signalContext;

    private Map<String, Object> intermediateValues;

    public SignalContext(Map<String, Object> signalContext) {
        this.signalContext = signalContext;
        this.attributes = new ConcurrentHashMap<>();
        this.intermediateValues = new ConcurrentHashMap<>();
    }

    public SignalContext() {
        this.attributes = new ConcurrentHashMap<>();
        this.intermediateValues = new ConcurrentHashMap<>();
    }

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
        if (this.intermediateValues != null) {
            this.intermediateValues.clear();
        }
        if (values != null) {
            this.intermediateValues.putAll(values);
        }
    }


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

    public Map<String, Object> getSignalContext() {
        return signalContext;
    }

    public void setSignalContext(Map<String, Object> signalContext) {
        this.signalContext = signalContext;
    }

    public void setAttributes(Map<String, Object> attributes) {
        if (this.attributes != null) {
            this.attributes.clear();  // Clear existing attributes if any
        }
        if (attributes != null) {
            this.attributes.putAll(attributes);  // Add new attributes
        }
    }

    // Add the clearSignalContext method
    public void clearSignalContext() {
        if (this.signalContext != null) {
            this.signalContext.clear();
        }
    }


    // Add method to set nested context
    public void setNestedContext(String key, Map<String, Object> nestedContext) {
        if (this.signalContext == null) {
            this.signalContext = new ConcurrentHashMap<>();
        }
        this.signalContext.put(key, nestedContext);
    }

    @Override
    public String toString() {
        return "SignalContext{" +
                "attributes=" + attributes +
                ", signalContext=" + signalContext +
                ", intermediateValues=" + intermediateValues +
                '}';
    }
}
