package com.hibiscus.signal.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultErrorHandler implements ErrorHandler {
    private static final Logger log = LoggerFactory.getLogger(DefaultErrorHandler.class);

    @Override
    public void handle(Throwable error) {
        // 默认的错误处理逻辑
        log.error("Error occurred during signal processing: ", error);
    }
}
