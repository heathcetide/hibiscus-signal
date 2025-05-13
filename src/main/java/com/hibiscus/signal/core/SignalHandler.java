package com.hibiscus.signal.core;

@FunctionalInterface
public interface SignalHandler {

    /**
     * 信号处理
     *
     * @param sender 发送者
     * @param params 参数
     */
    void handle(Object sender, Object... params) throws InterruptedException;

}
