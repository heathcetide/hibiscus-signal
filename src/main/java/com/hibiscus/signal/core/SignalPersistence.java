package com.hibiscus.signal.core;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

/**
 * SignalPersistence
 */
public class SignalPersistence {

    /**
     * 对象映射器
     */
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 保存数据到文件
     */
    public static void saveToFile(Object data, String filePath) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(filePath), data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 从文件中加载数据
     */
    public static <T> T loadFromFile(String filePath, Class<T> clazz) {
        try {
            return objectMapper.readValue(new File(filePath), clazz);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
