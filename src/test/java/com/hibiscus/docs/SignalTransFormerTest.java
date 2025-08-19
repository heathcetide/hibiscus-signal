package com.hibiscus.docs;

import com.hibiscus.docs.core.SignalCallback;
import com.hibiscus.docs.core.SignalTransformer;

public class SignalTransFormerTest {
//    public static void main(String[] args) {
//        // 创建 Signals 实例
//        Signals signals = Signals.sig();
//        long connect = signals.connect("myEvent", (sender, params) -> {
//            System.out.println("Sender: " + sender + " Params: " + Arrays.toString(params));
//        });
//        // 定义信号转换器，将信号参数转换为大写
//        SignalTransformer uppercaseTransformer = (event, sender, params) -> {
//            if (params != null && params.length > 0 && params[0] instanceof String) {
//                params[0] = ((String) params[0]).toUpperCase();
//            }
//            return params;
//        };
//
//        // 将信号转换器绑定到某个事件
//        signals.addSignalTransformer("myEvent", uppercaseTransformer);
//
//        // 发送信号，并传递参数
//        signals.emit("myEvent", new Object(), new SignalCallback() {
//            @Override
//            public void onSuccess(String event, Object sender, Object... params) {
//                System.out.println("Signal emitted successfully with params: " + Arrays.toString(params));
//            }
//        }, error -> {
//            System.out.println("Error: " + error.getMessage());
//        }, "hello world");
//    }
}
