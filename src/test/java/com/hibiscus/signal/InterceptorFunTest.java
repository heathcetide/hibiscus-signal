package com.hibiscus.signal;

import com.hibiscus.signal.core.SignalCallback;

import java.util.Arrays;

public class InterceptorFunTest {
    public static void main(String[] args) {
        // 获取 Signals 单例
        Signals signals = Signals.sig();
        signals.connect("myEvent", (sender, params) -> {
            System.out.println("Sender: " + sender + " Params: " + Arrays.toString(params));
        });
        signals.connect("sensitiveEvent", (sender, params) -> {
            System.out.println("Sender: " + sender + " Params: " + Arrays.toString(params));
        });

        // 创建并添加拦截器
        signals.addSignalInterceptor("myEvent", new MySignalInterceptor());
        signals.addSignalInterceptor("myEvent", new LoggingInterceptor());
        signals.addSignalInterceptor("sensitiveEvent", new MySignalInterceptor());
        signals.addSignalInterceptor("sensitiveEvent", new LoggingInterceptor());
        // 创建一个处理信号的回调
        SignalCallback callback = new SignalCallback() {
            @Override
            public void onSuccess(String event, Object sender, Object... params) {
                System.out.println("Signal processed successfully: " + event);
            }

            @Override
            public void onError(String event, Object sender, Throwable error, Object... params) {
                System.out.println("Error while processing signal: " + event);
            }

            @Override
            public void onComplete(String event, Object sender, Object... params) {
                System.out.println("Signal processing completed: " + event);
            }
        };

        // 触发信号
        signals.emit("myEvent", "参数", callback, Throwable::printStackTrace);

        // 触发一个会被拦截的事件
        signals.emit("sensitiveEvent","参数", callback, Throwable::printStackTrace);
    }
}
