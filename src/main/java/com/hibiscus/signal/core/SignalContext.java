package com.hibiscus.signal.core;

import com.hibiscus.signal.utils.SnowflakeIdGenerator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Context information for a signal as it flows through the system.
 *
 * Purpose:
 * - Stores user-defined attributes and intermediate values during signal processing.
 * - Supports tracing (spans) to visualize the flow of signal handling.
 */
public class SignalContext {

    // User-defined attributes (e.g., business parameters)
    private final Map<String, Object> attributes;

    // Intermediate values (used internally by the framework)
    private final Map<String, Object> intermediateValues;

    // Unique identifiers for tracing and correlation
    private String traceId;
    private String eventId;

    // List of spans to track the execution path
    private final List<Span> spans = new CopyOnWriteArrayList<>();

    // ID of the current parent span for nested tracing
    private String parentSpanId;

    /**
     * Constructs a new, empty SignalContext.
     */
    public SignalContext() {
        this.attributes = new ConcurrentHashMap<>();
        this.intermediateValues = new ConcurrentHashMap<>();
    }

    // ---------- Attribute handling ----------

    /**
     * Sets a user-defined attribute.
     *
     * @param key   attribute key
     * @param value attribute value
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    /**
     * Retrieves a user-defined attribute.
     *
     * @param key attribute key
     * @return attribute value
     */
    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    /**
     * Removes a user-defined attribute.
     *
     * @param key attribute key
     */
    public void removeAttribute(String key) {
        attributes.remove(key);
    }

    /**
     * Gets a copy of all attributes.
     *
     * @return attributes map
     */
    public Map<String, Object> getAttributes() {
        return new ConcurrentHashMap<>(attributes);
    }

    /**
     * Sets the entire attribute map, replacing any existing values.
     *
     * @param newAttributes new attributes to set
     */
    public void setAttributes(Map<String, Object> newAttributes) {
        this.attributes.clear();
        if (newAttributes != null) {
            this.attributes.putAll(newAttributes);
        }
    }

    // ---------- Intermediate value handling ----------

    /**
     * Adds an intermediate value (for internal framework use).
     *
     * @param key   intermediate value key
     * @param value intermediate value
     */
    public void addIntermediateValue(String key, Object value) {
        intermediateValues.put(key, value);
    }

    /**
     * Retrieves an intermediate value.
     *
     * @param key intermediate value key
     * @return intermediate value
     */
    public Object getIntermediateValue(String key) {
        return intermediateValues.get(key);
    }

    /**
     * Gets a copy of all intermediate values.
     *
     * @return intermediate values map
     */
    public Map<String, Object> getIntermediateValues() {
        return new ConcurrentHashMap<>(intermediateValues);
    }

    /**
     * Sets the entire intermediate values map, replacing any existing values.
     *
     * @param values new intermediate values to set
     */
    public void setIntermediateValues(Map<String, Object> values) {
        this.intermediateValues.clear();
        if (values != null) {
            this.intermediateValues.putAll(values);
        }
    }

    // ---------- Tracing / Spans ----------

    /**
     * Initializes a trace ID and event ID for the signal context.
     *
     * @param eventName the name of the signal event
     */
    public void initTrace(String eventName) {
        if (eventName == null) {
            eventName = "unknown";
        }
        this.traceId = UUID.randomUUID().toString();
        this.eventId = eventName + "_" + SnowflakeIdGenerator.nextId();
    }

    public String getTraceId() {
        return traceId;
    }

    public String getEventId() {
        return eventId;
    }

    public String getParentSpanId() {
        return parentSpanId;
    }

    public void setParentSpanId(String parentSpanId) {
        this.parentSpanId = parentSpanId;
    }

    /**
     * Adds a span to the tracing list.
     *
     * @param span the span to add
     */
    public void addSpan(Span span) {
        spans.add(span);
    }

    /**
     * Gets a list of recorded spans (copied for safety).
     *
     * @return list of spans
     */
    public List<Span> getSpans() {
        return new ArrayList<>(spans);
    }

    @Override
    public String toString() {
        return "SignalContext{" +
                "attributes=" + attributes +
                ", intermediateValues=" + intermediateValues +
                '}';
    }

    /**
     * Represents a tracing span, capturing the operation details.
     */
    public static class Span {
        private String spanId;
        private String parentSpanId;
        private String operation;
        private long startTime;
        private long endTime;
        private Map<String, Object> metadata = new HashMap<>();

        public String getSpanId() {
            return spanId;
        }

        public void setSpanId(String spanId) {
            this.spanId = spanId;
        }

        public String getParentSpanId() {
            return parentSpanId;
        }

        public void setParentSpanId(String parentSpanId) {
            this.parentSpanId = parentSpanId;
        }

        public String getOperation() {
            return operation;
        }

        public void setOperation(String operation) {
            this.operation = operation;
        }

        public long getStartTime() {
            return startTime;
        }

        public void setStartTime(long startTime) {
            this.startTime = startTime;
        }

        public long getEndTime() {
            return endTime;
        }

        public void setEndTime(long endTime) {
            this.endTime = endTime;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
    }
}
