package com.hibiscus.docs;

import com.hibiscus.docs.core.SignalPersistence;

import java.io.IOException;

public class TestPersistence {

    public static void main(String[] args) throws IOException {
        SignalPersistence signalPersistence = new SignalPersistence();
        signalPersistence.saveToFile("test", "test.json");
    }
}
