package com.hibiscus.signal;

import com.hibiscus.signal.core.SignalInterceptor;

public class LoggingInterceptor implements SignalInterceptor {

    @Override
    public boolean beforeHandle(String event, Object sender, Object... params) {
        System.out.println("Logging: About to handle signal: " + event);
        return true; // 继续传播
    }

    @Override
    public void afterHandle(String event, Object sender, Object[] params, Throwable error) {
        System.out.println("Logging: Finished handling signal: " + event);
    }

    @Override
    public int getOrder() {
        return 2;  // 优先级 2
    }
}
