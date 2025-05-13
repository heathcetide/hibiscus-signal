package com.hibiscus.signal;

import com.hibiscus.signal.config.SignalConfig;
import com.hibiscus.signal.core.*;
import java.util.function.Consumer;

public interface SignalManager {

    /**
     * 绑定事件
     */
    long connect(String event, SignalHandler handler);

    /**
     * 绑定事件
     */
    long connect(String event, SignalHandler handler, SignalConfig signalConfig);

    /**
     * 绑定事件
     */
    long connect(String event, SignalHandler handler, SignalContext context);

    /**
     * 绑定事件
     */
    long connect(String event, SignalHandler handler, SignalConfig signalConfig, SignalContext context);

    /**
     * 解绑事件
     */
    void disconnect(String event, long id);

    /**
     * 解绑事件
     */
    void disconnect(String event, long id, SignalContext context);

    /**
     * 触发事件
     */
    void processEvents();

    /**
     * 发送信号
     */
    void emit(String event, Object sender, Consumer<Throwable> errorHandler, Object... params);

    /**
     * 发送信号
     */
    void emit(String event, Object sender, SignalCallback callback, Consumer<Throwable> errorHandler, Object... params);

    /**
     * 关闭
     */
    void shutdown();

    /**
     * 清空
     */
    void clear(String... events);

    /**
     * 添加过滤器
     */
    void addFilter(String event, SignalFilter filter);

    /**
     * 添加转换器
     */
    void addSignalTransformer(String event, SignalTransformer transformer);

    /**
     * 添加拦截器
     */
    void addSignalInterceptor(String event, SignalInterceptor interceptor);
}
