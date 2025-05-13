package com.hibiscus.signal;

import com.hibiscus.signal.core.SignalCallback;

public class MySignalHandler implements SignalCallback {

    @Override
    public void onSuccess(String event, Object sender, Object... params) {
        System.out.println("Signal " + event + " handled successfully by " + sender);
    }

    @Override
    public void onError(String event, Object sender, Throwable error, Object... params) {
        System.err.println("Error handling signal " + event + " from " + sender + ": " + error.getMessage());
    }

    @Override
    public void onComplete(String event, Object sender, Object... params) {
        System.out.println("Signal " + event + " completed.");
    }
}
