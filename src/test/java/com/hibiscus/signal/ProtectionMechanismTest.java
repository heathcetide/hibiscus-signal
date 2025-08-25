package com.hibiscus.signal;

import com.hibiscus.signal.config.SignalConfig;
import com.hibiscus.signal.core.CircuitBreaker;
import com.hibiscus.signal.core.RateLimiter;
import com.hibiscus.signal.core.SignalHandler;
import com.hibiscus.signal.core.SignalProtectionManager;
import com.hibiscus.signal.core.SignalMetrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 保护机制测试类
 * 验证熔断器和限流器功能是否正常工作
 */
@DisplayName("保护机制功能测试")
public class ProtectionMechanismTest {

    private SignalProtectionManager protectionManager;
    private SignalMetrics metrics;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        protectionManager = new SignalProtectionManager();
        metrics = new SignalMetrics();
        executor = Executors.newFixedThreadPool(4);
    }
    
    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    @Test
    @DisplayName("测试熔断器基本功能")
    void testCircuitBreakerBasicFunctionality() {
        // 创建熔断器：5次失败后熔断，60秒后半开，3次成功试验后关闭
        CircuitBreaker breaker = new CircuitBreaker(5, 60000, 3);
        String eventName = "test.event";
        
        // 注册熔断器
        protectionManager.registerCircuitBreaker(eventName, breaker);
        
        // 初始状态应该是关闭的
        assertFalse(protectionManager.isBlocked(eventName));
        
        // 模拟5次失败 - 直接调用熔断器的recordFailure方法
        for (int i = 0; i < 5; i++) {
            protectionManager.recordFailure(eventName);
        }
        
        // 此时熔断器应该打开
        assertTrue(protectionManager.isBlocked(eventName));
        
        // 获取熔断器实例验证状态
        CircuitBreaker registeredBreaker = protectionManager.getCircuitBreaker(eventName);
        assertNotNull(registeredBreaker);
        assertTrue(registeredBreaker.isOpen());
    }

    @Test
    @DisplayName("测试限流器基本功能")
    void testRateLimiterBasicFunctionality() {
        // 创建限流器：每秒最多10个请求（使用较小的数字便于测试）
        RateLimiter limiter = new RateLimiter(10);
        String eventName = "test.event";
        
        // 注册限流器
        protectionManager.registerRateLimiter(eventName, limiter);
        
        // 初始状态应该允许请求
        assertFalse(protectionManager.isBlocked(eventName));
        
        // 模拟10个请求
        for (int i = 0; i < 10; i++) {
            assertTrue(limiter.allowRequest());
        }
        
        // 第11个请求应该被拒绝
        assertFalse(limiter.allowRequest());
        
        // 此时应该被限流器阻塞
        assertTrue(protectionManager.isBlocked(eventName));
        
        // 验证限流器状态
        RateLimiter registeredLimiter = protectionManager.getRateLimiter(eventName);
        assertNotNull(registeredLimiter);
        assertFalse(registeredLimiter.allowRequest());
        
        // 等待1秒后限流器应该重置
        try {
            Thread.sleep(1100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 限流器应该重置，允许新请求
        assertTrue(limiter.allowRequest());
        assertFalse(protectionManager.isBlocked(eventName));
    }

    @Test
    @DisplayName("测试熔断器和限流器组合使用")
    void testCircuitBreakerAndRateLimiterCombination() {
        String eventName = "test.event";
        
        // 创建并注册熔断器
        CircuitBreaker breaker = new CircuitBreaker(3, 1000, 2); // 快速熔断用于测试
        protectionManager.registerCircuitBreaker(eventName, breaker);
        
        // 创建并注册限流器
        RateLimiter limiter = new RateLimiter(10); // 每秒10个请求
        protectionManager.registerRateLimiter(eventName, limiter);
        
        // 初始状态：两个保护机制都未触发
        assertFalse(protectionManager.isBlocked(eventName));
        
        // 测试限流器：发送10个请求
        for (int i = 0; i < 10; i++) {
            assertTrue(limiter.allowRequest());
        }
        
        // 第11个请求被限流器拒绝
        assertFalse(limiter.allowRequest());
        assertTrue(protectionManager.isBlocked(eventName));
        
        // 等待1秒后限流器重置
        try {
            Thread.sleep(1100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 限流器应该重置，允许新请求
        assertTrue(limiter.allowRequest());
        assertFalse(protectionManager.isBlocked(eventName));
        
        // 测试熔断器：触发熔断
        for (int i = 0; i < 3; i++) {
            protectionManager.recordFailure(eventName);
        }
        
        // 熔断器应该打开
        assertTrue(protectionManager.isBlocked(eventName));
        assertTrue(breaker.isOpen());
    }

    @Test
    @DisplayName("测试熔断器状态转换")
    void testCircuitBreakerStateTransitions() {
        CircuitBreaker breaker = new CircuitBreaker(2, 1000, 2); // 2次失败熔断，1秒后半开
        String eventName = "test.event";
        
        protectionManager.registerCircuitBreaker(eventName, breaker);
        
        // 初始状态：CLOSED
        assertFalse(breaker.isOpen());
        
        // 1次失败，状态仍为CLOSED
        protectionManager.recordFailure(eventName);
        assertFalse(breaker.isOpen());
        
        // 2次失败，状态变为OPEN
        protectionManager.recordFailure(eventName);
        assertTrue(breaker.isOpen());
        
        // 等待1秒后，状态变为HALF_OPEN
        try {
            Thread.sleep(1100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 检查是否变为半开状态
        assertFalse(breaker.isOpen()); // 半开状态下允许试验请求
        
        // 1次成功试验
        protectionManager.recordSuccess(eventName);
        assertFalse(breaker.isOpen());
        
        // 2次成功试验，状态重置为CLOSED
        protectionManager.recordSuccess(eventName);
        assertFalse(breaker.isOpen());
    }

    @Test
    @DisplayName("测试保护机制的性能影响")
    void testProtectionMechanismPerformance() {
        String eventName = "test.event";
        
        // 创建保护机制
        CircuitBreaker breaker = new CircuitBreaker(100, 60000, 10);
        RateLimiter limiter = new RateLimiter(90000);
        
        protectionManager.registerCircuitBreaker(eventName, breaker);
        protectionManager.registerRateLimiter(eventName, limiter);
        
        // 测试大量请求的性能
        long startTime = System.currentTimeMillis();
        int requestCount = 100000;
        
        for (int i = 0; i < requestCount; i++) {
            protectionManager.isBlocked(eventName);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // 100000次检查应该在合理时间内完成（比如小于1秒）
        assertTrue(duration < 1000, "保护机制检查性能不达标，耗时: " + duration + "ms");
        
        System.out.println("100000次保护机制检查耗时: " + duration + "ms");
    }
    
    @Test
    @DisplayName("测试保护机制的基本注册和获取")
    void testProtectionMechanismRegistration() {
        String eventName = "test.event";
        
        // 创建保护机制
        CircuitBreaker breaker = new CircuitBreaker(5, 60000, 3);
        RateLimiter limiter = new RateLimiter(1000);
        
        // 注册保护机制
        protectionManager.registerCircuitBreaker(eventName, breaker);
        protectionManager.registerRateLimiter(eventName, limiter);
        
        // 验证注册成功
        assertNotNull(protectionManager.getCircuitBreaker(eventName));
        assertNotNull(protectionManager.getRateLimiter(eventName));
        
        // 验证初始状态
        assertFalse(protectionManager.isBlocked(eventName));
        
        // 移除保护机制
        protectionManager.removeProtection(eventName);
        
        // 验证移除成功
        assertNull(protectionManager.getCircuitBreaker(eventName));
        assertNull(protectionManager.getRateLimiter(eventName));
        assertFalse(protectionManager.isBlocked(eventName));
    }

    @Test
    @DisplayName("测试并发环境下的保护机制")
    void testProtectionMechanismConcurrency() throws InterruptedException {
        String eventName = "test.event";
        
        // 创建保护机制
        CircuitBreaker breaker = new CircuitBreaker(50, 60000, 10);
        RateLimiter limiter = new RateLimiter(10000); // 增加限流器容量
        
        protectionManager.registerCircuitBreaker(eventName, breaker);
        protectionManager.registerRateLimiter(eventName, limiter);
        
        // 并发测试
        int threadCount = 10;
        int requestsPerThread = 1000;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger blockedCount = new AtomicInteger(0);
        
        Thread[] threads = new Thread[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < requestsPerThread; j++) {
                    if (protectionManager.isBlocked(eventName)) {
                        blockedCount.incrementAndGet();
                    } else {
                        successCount.incrementAndGet();
                    }
                }
            });
        }
        
        // 启动所有线程
        for (Thread thread : threads) {
            thread.start();
        }
        
        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }
        
        // 验证结果
        int totalRequests = threadCount * requestsPerThread;
        assertEquals(totalRequests, successCount.get() + blockedCount.get());
        
        System.out.println("并发测试结果:");
        System.out.println("  总请求数: " + totalRequests);
        System.out.println("  成功请求: " + successCount.get());
        System.out.println("  被阻塞请求: " + blockedCount.get());
        
        // 验证保护机制状态 - 由于限流器容量足够，应该不会被阻塞
        assertFalse(protectionManager.isBlocked(eventName));
        assertFalse(breaker.isOpen());
        assertTrue(limiter.allowRequest());
    }
}
