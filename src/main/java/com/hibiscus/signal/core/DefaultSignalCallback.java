package com.hibiscus.signal.core;

import com.hibiscus.signal.spring.configuration.SignalContextCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class DefaultSignalCallback implements SignalCallback {

    private static Logger log = LoggerFactory.getLogger(DefaultSignalCallback.class);

    @Override
    public void onSuccess(String event, Object sender, Object... params) {
        log.info("Signal [{}] processed successfully by [{}]. Parameters: {}", event, sender.getClass().getSimpleName(), params);
    }

    @Override
    public void onError(String event, Object sender, Throwable error, Object... params) {
        log.error("Signal [{}] failed to process by [{}]. Error: {}. Parameters: {}", event, sender.getClass().getSimpleName(), error.getMessage(), params, error);
    }

    @Override
    public void onComplete(String event, Object sender, Object... params) {
        SignalContext context = null;
        for (Object param : params) {
            if (param instanceof SignalContext) {
                context = (SignalContext) param;
                break;
            }
        }

        if (context != null) {
            SignalContextCollector.logSpanTrace(context);
        }
        log.info("Signal [{}] processing completed by [{}]. Parameters: {}", event, sender.getClass().getSimpleName(), params);
    }
}
