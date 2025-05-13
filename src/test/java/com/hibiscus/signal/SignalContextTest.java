package com.hibiscus.signal;

import com.hibiscus.signal.core.SignalCallback;
import com.hibiscus.signal.core.SignalContext;

import java.util.Arrays;

public class SignalContextTest {
    public static void main(String[] args) {
        SignalContext context = new SignalContext();
        context.setAttribute("test", "test");
        context.setAttribute("test1", "test1");

        Signals sig = Signals.sig();
        long connect = sig.connect("event001", (sender, params) -> System.out.println("Sender: " + sender + " Params: " + Arrays.toString(params)),context);
        sig.emit("event001", new Object(), new SignalCallback() {
            @Override
            public void onSuccess(String event, Object sender, Object... params) {
                System.out.println("event： " + event);
                System.out.println("Sender: " + sender);
                SignalContext context = (SignalContext)params[0];
                context.getAttributes().forEach((key, value) -> System.out.println("Key: " + key + " Value: " + value));
                System.out.println("Signal emitted successfully with params: " + Arrays.toString(params));
            }
        }, error -> {
            System.out.println("Error: " + error.getMessage());
        }, context,"hello world");
    }
}
