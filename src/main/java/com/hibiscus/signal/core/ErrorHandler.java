package com.hibiscus.signal.core;

@FunctionalInterface
public interface ErrorHandler {

    void handle(Throwable error);

}
