package com.hibiscus.signal.core;

import com.hibiscus.signal.config.SignalPriority;

/**
 * 信号核心
 */
public class SigHandler {

    /**
     * 信号唯一id
     */
    private final long id;

    /**
     * 信号处理事件类型
     */
    private final EventType evType;

    /**
     * 信号名称
     */
    private final String signalName;

    /**
     * 信号处理器
     */
    private final SignalHandler handler;

    /**
     * 优先级
     */
    private final SignalPriority priority;

    private SignalContext signalContext;

    public SigHandler(long id, EventType evType, String signalName, SignalHandler handler, SignalPriority priority) {
        this.id = id;
        this.evType = evType;
        this.signalName = signalName;
        this.handler = handler;
        this.priority = priority;
    }

    public long getId() {
        return id;
    }

    public SignalHandler getHandler() {
        return handler;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SigHandler that = (SigHandler) o;
        return id == that.id;
    }

    @Override
    public String toString() {
        return "SigHandler{" +
                "id=" + id +
                ", evType=" + evType +
                ", signalName='" + signalName + '\'' +
                ", handler=" + handler +
                '}';
    }

    public EventType getEvType() {
        return evType;
    }

    public String getSignalName() {
        return signalName;
    }

    public SignalPriority getPriority() {
        return priority;
    }

    public SignalContext getSignalContext() {
        return signalContext;
    }

    public void setSignalContext(SignalContext signalContext) {
        this.signalContext = signalContext;
    }
}
