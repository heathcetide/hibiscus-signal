package com.hibiscus.signal.core;

import com.hibiscus.signal.config.SignalConfig;

import java.util.Map;

public class SignalPersistenceInfo {

    private SigHandler sigHandler;

    private SignalConfig signalConfig;

    private SignalContext signalContext;

    private Map<String, Map<String, Object>> metrics;

    // 添加无参构造函数，支持Jackson反序列化
    public SignalPersistenceInfo() {
    }

    public SignalPersistenceInfo(SigHandler sigHandler, SignalConfig signalConfig, SignalContext signalContext, Map<String, Map<String, Object>> metrics) {
        this.sigHandler = sigHandler;
        this.signalConfig = signalConfig;
        this.signalContext = signalContext;
        this.metrics = metrics;
    }

    public SigHandler getSigHandler() {
        return sigHandler;
    }

    public void setSigHandler(SigHandler sigHandler) {
        this.sigHandler = sigHandler;
    }

    public SignalConfig getSignalConfig() {
        return signalConfig;
    }

    public void setSignalConfig(SignalConfig signalConfig) {
        this.signalConfig = signalConfig;
    }

    public SignalContext getSignalContext() {
        return signalContext;
    }

    public void setSignalContext(SignalContext signalContext) {
        this.signalContext = signalContext;
    }

    public Map<String, Map<String, Object>> getMetrics() {
        return metrics;
    }

    public void setMetrics(Map<String, Map<String, Object>> metrics) {
        this.metrics = metrics;
    }

    @Override
    public String toString() {
        return "SignalPersistenceInfo{" +
                "sigHandler=" + sigHandler +
                ", signalConfig=" + signalConfig +
                ", signalContext=" + signalContext +
                ", metrics=" + metrics +
                '}';
    }
}
