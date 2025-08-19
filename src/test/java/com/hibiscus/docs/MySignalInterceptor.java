package com.hibiscus.docs;

import com.hibiscus.docs.core.SignalInterceptor;

public class MySignalInterceptor implements SignalInterceptor {

    @Override
    public boolean beforeHandle(String event, Object sender, Object... params) {
        System.out.println("Before handling signal: " + event);
        // 可以根据逻辑返回 false 来阻止信号传播
        if (event.equals("sensitiveEvent")) {
            System.out.println("Intercepted sensitive event, stopping propagation");
            return false; // 阻止信号继续传播
        }
        return true; // 继续传播
    }

    @Override
    public void afterHandle(String event, Object sender, Object[] params, Throwable error) {
        System.out.println("After handling signal: " + event);
        if (error != null) {
            System.out.println("Error occurred during signal processing: " + error.getMessage());
        } else {
            System.out.println("Signal processed successfully: " + event);
        }
    }

    @Override
    public int getOrder() {
        return 1;  // 优先级 1
    }
}
