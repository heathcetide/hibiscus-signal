package com.hibiscus.signal;

import com.hibiscus.signal.core.SignalPersistence;

import java.io.IOException;

public class TestPersistence {

    public static void main(String[] args) throws IOException {
        SignalPersistence signalPersistence = new SignalPersistence();
        signalPersistence.saveToFile("test", "test.json");
    }
}
