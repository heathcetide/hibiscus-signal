package com.hibiscus.signal.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.hibiscus.signal.core.SignalPersistenceInfo;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 增强版信号持久化类
 * 支持追加写入、文件轮转、批量操作等功能
 */
public class EnhancedSignalPersistence {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final ReentrantReadWriteLock fileLock = new ReentrantReadWriteLock();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    /**
     * 追加写入持久化信息到文件
     */
    public static void appendToFile(SignalPersistenceInfo info, String filePath) {
        fileLock.writeLock().lock();
        try {
            Path path = Paths.get(filePath);
            Files.createDirectories(path.getParent());
            
            List<SignalPersistenceInfo> existingData = readAllFromFile(filePath);
            existingData.add(info);
            
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), existingData);
            
        } catch (IOException e) {
            System.err.println("追加写入失败: " + e.getMessage());
        } finally {
            fileLock.writeLock().unlock();
        }
    }

    /**
     * 批量追加写入多个持久化信息
     */
    public static void appendBatchToFile(List<SignalPersistenceInfo> infoList, String filePath) {
        fileLock.writeLock().lock();
        try {
            Path path = Paths.get(filePath);
            
            // 确保目录存在
            Files.createDirectories(path.getParent());
            
            // 读取现有数据
            List<SignalPersistenceInfo> existingData = readAllFromFile(filePath);
            
            // 添加新数据
            existingData.addAll(infoList);
            
            // 写入所有数据
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), existingData);
            
        } catch (IOException e) {
            System.err.println("批量追加写入失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            fileLock.writeLock().unlock();
        }
    }

    /**
     * 从文件读取所有持久化信息
     */
    public static List<SignalPersistenceInfo> readAllFromFile(String filePath) {
        fileLock.readLock().lock();
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return new ArrayList<>();
            }
            
            CollectionType listType = objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, SignalPersistenceInfo.class);
            
            return objectMapper.readValue(path.toFile(), listType);
            
        } catch (IOException e) {
            System.err.println("读取文件失败: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            fileLock.readLock().unlock();
        }
    }

    /**
     * 文件轮转：当文件过大时，创建新文件
     */
    public static void rotateFileIfNeeded(String filePath, long maxSizeBytes) {
        fileLock.writeLock().lock();
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return;
            }
            
            long fileSize = Files.size(path);
            if (fileSize > maxSizeBytes) {
                // 创建备份文件
                String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
                String backupPath = filePath.replace(".json", "_" + timestamp + ".json");
                
                Files.move(path, Paths.get(backupPath));
                System.out.println("文件轮转: " + filePath + " -> " + backupPath);
            }
            
        } catch (IOException e) {
            System.err.println("文件轮转失败: " + e.getMessage());
        } finally {
            fileLock.writeLock().unlock();
        }
    }

    /**
     * 清理旧文件：删除指定天数之前的备份文件
     */
    public static void cleanupOldFiles(String directory, int daysToKeep) {
        try {
            Path dir = Paths.get(directory);
            if (!Files.exists(dir)) {
                return;
            }
            
            long cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L);
            
            Files.list(dir)
                    .filter(path -> path.toString().endsWith(".json"))
                    .filter(path -> path.toString().contains("_"))
                    .forEach(path -> {
                        try {
                            if (Files.getLastModifiedTime(path).toMillis() < cutoffTime) {
                                Files.delete(path);
                                System.out.println("删除旧文件: " + path);
                            }
                        } catch (IOException e) {
                            System.err.println("删除文件失败: " + path + ", " + e.getMessage());
                        }
                    });
                    
        } catch (IOException e) {
            System.err.println("清理旧文件失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件统计信息
     */
    public static FileStats getFileStats(String filePath) {
        fileLock.readLock().lock();
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return new FileStats(0, 0, 0);
            }
            
            List<SignalPersistenceInfo> data = readAllFromFile(filePath);
            long fileSize = Files.size(path);
            
            return new FileStats(data.size(), fileSize, data.size());
            
        } catch (IOException e) {
            System.err.println("获取文件统计失败: " + e.getMessage());
            return new FileStats(0, 0, 0);
        } finally {
            fileLock.readLock().unlock();
        }
    }

    /**
     * 文件统计信息
     */
    public static class FileStats {
        private final int recordCount;
        private final long fileSizeBytes;
        private final int totalRecords;

        public FileStats(int recordCount, long fileSizeBytes, int totalRecords) {
            this.recordCount = recordCount;
            this.fileSizeBytes = fileSizeBytes;
            this.totalRecords = totalRecords;
        }

        public int getRecordCount() { return recordCount; }
        public long getFileSizeBytes() { return fileSizeBytes; }
        public int getTotalRecords() { return totalRecords; }

        @Override
        public String toString() {
            return String.format("FileStats{records=%d, size=%.2fMB, total=%d}", 
                    recordCount, fileSizeBytes / 1024.0 / 1024.0, totalRecords);
        }
    }

    /**
     * 兼容原有方法
     */
    public static void saveToFile(Object data, String filePath) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(filePath), data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 兼容原有方法
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
