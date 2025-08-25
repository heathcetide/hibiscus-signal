package com.hibiscus.signal;

import com.hibiscus.signal.core.aggregate.User;
import com.hibiscus.signal.core.entity.EventSnapshot;
import com.hibiscus.signal.core.event.DomainEvent;
import com.hibiscus.signal.core.event.UserEvents;
import com.hibiscus.signal.core.service.EventSourcingService;
import com.hibiscus.signal.core.service.EventReplayService;
import com.hibiscus.signal.core.service.EventQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 事件溯源功能测试
 * 
 * @author heathcetide
 */
@Transactional
@SpringBootTest(classes = HibiscusSignalApplication.class)
@ActiveProfiles("test")
public class EventSourcingTest {
    
    @Autowired
    private EventSourcingService eventSourcingService;
    
    @Autowired
    private EventReplayService eventReplayService;
    
    @Autowired
    private EventQueryService eventQueryService;
    
    private String userId;
    private User user;
    
    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID().toString();
        user = User.create(userId, "testuser", "test@example.com", "Test User");
    }
    
    @Test
    void testUserCreationAndEventStorage() {
        // 获取未提交的事件
        List<DomainEvent> uncommittedEvents = user.getUncommittedEvents();
        assertEquals(1, uncommittedEvents.size());
        
        // 验证事件类型
        DomainEvent event = uncommittedEvents.get(0);
        assertTrue(event instanceof UserEvents.UserCreated);
        assertEquals("UserCreated", event.getEventType());
        assertEquals(userId, event.getAggregateId());
        
        // 存储事件
        eventSourcingService.storeEvent(event);
        
        // 标记事件已提交
        user.markEventsAsCommitted();
        
        // 验证事件已存储
        assertTrue(eventSourcingService.exists(userId));
        assertEquals(1, eventSourcingService.getLatestVersion(userId));
    }
    
    @Test
    void testUserUpdateAndEventReplay() {
        // 创建并存储用户
        List<DomainEvent> events = user.getUncommittedEvents();
        eventSourcingService.storeEvent(events.get(0));
        user.markEventsAsCommitted();
        
        // 更新用户信息
        user.updateInfo("newemail@example.com", "Updated User", "测试更新");
        
        // 存储更新事件
        events = user.getUncommittedEvents();
        assertEquals(1, events.size());
        eventSourcingService.storeEvent(events.get(0));
        user.markEventsAsCommitted();
        
        // 验证版本号
        assertEquals(2, eventSourcingService.getLatestVersion(userId));
        
        // 从事件重建用户
        User replayedUser = eventReplayService.replayEvents(userId, User.class);
        
        // 验证重建后的状态
        assertNotNull(replayedUser);
        assertEquals("newemail@example.com", replayedUser.getEmail());
        assertEquals("Updated User", replayedUser.getFullName());
        assertEquals(2, eventSourcingService.getLatestVersion(userId));
    }
    
    @Test
    void testUserStatusChange() {
        // 创建并存储用户
        List<DomainEvent> events = user.getUncommittedEvents();
        eventSourcingService.storeEvent(events.get(0));
        user.markEventsAsCommitted();
        
        // 变更用户状态
        user.changeStatus("SUSPENDED", "账户被暂停");
        
        // 存储状态变更事件
        events = user.getUncommittedEvents();
        eventSourcingService.storeEvent(events.get(0));
        user.markEventsAsCommitted();
        
        // 验证状态变更
        assertEquals("SUSPENDED", user.getStatus());
        assertEquals(2, eventSourcingService.getLatestVersion(userId));
    }
    
    @Test
    void testUserLoginAndLogout() {
        // 创建并存储用户
        List<DomainEvent> events = user.getUncommittedEvents();
        eventSourcingService.storeEvent(events.get(0));
        user.markEventsAsCommitted();
        
        // 用户登录
        user.login("192.168.1.1", "Mozilla/5.0");
        
        // 存储登录事件
        events = user.getUncommittedEvents();
        eventSourcingService.storeEvent(events.get(0));
        user.markEventsAsCommitted();
        
        // 验证登录信息
        assertEquals(1, user.getLoginCount());
        assertNotNull(user.getLastLoginAt());
        
        // 用户注销
        user.logout("用户主动注销");
        
        // 存储注销事件
        events = user.getUncommittedEvents();
        eventSourcingService.storeEvent(events.get(0));
        user.markEventsAsCommitted();
        
        // 验证版本号
        assertEquals(3, eventSourcingService.getLatestVersion(userId));
    }
    
    @Test
    void testUserDeletion() {
        // 创建并存储用户
        List<DomainEvent> events = user.getUncommittedEvents();
        eventSourcingService.storeEvent(events.get(0));
        user.markEventsAsCommitted();
        
        // 删除用户
        user.delete("测试删除", "admin");
        
        // 存储删除事件
        events = user.getUncommittedEvents();
        eventSourcingService.storeEvent(events.get(0));
        user.markEventsAsCommitted();
        
        // 验证删除状态
        assertTrue(user.isDeleted());
        assertEquals("admin", user.getDeletedBy());
        assertNotNull(user.getDeletedAt());
        assertEquals(2, eventSourcingService.getLatestVersion(userId));
    }
    
    @Test
    void testEventStreamRetrieval() {
        // 创建并存储用户
        List<DomainEvent> events = user.getUncommittedEvents();
        eventSourcingService.storeEvent(events.get(0));
        user.markEventsAsCommitted();
        
        // 获取事件流
        EventSourcingService.EventStream eventStream = eventSourcingService.getEventStream(userId);
        
        // 验证事件流
        assertNotNull(eventStream);
        assertEquals(userId, eventStream.getAggregateId());
        assertEquals(1, eventStream.getEventCount());
        assertEquals(1, eventStream.getFirstVersion());
        assertEquals(1, eventStream.getLastVersion());
        assertFalse(eventStream.isEmpty());
    }
    
    @Test
    void testSnapshotCreation() {
        // 创建并存储用户
        List<DomainEvent> events = user.getUncommittedEvents();
        eventSourcingService.storeEvent(events.get(0));
        user.markEventsAsCommitted();
        
        // 创建快照
        eventSourcingService.createSnapshot(userId, user, "UserSnapshot", "用户状态快照");
        
        // 获取快照
        EventSnapshot snapshot = eventSourcingService.getLatestSnapshot(userId).orElse(null);
        
        // 验证快照
        assertNotNull(snapshot);
        assertEquals(userId, snapshot.getAggregateId());
        assertEquals(1, snapshot.getVersion());
        assertEquals("UserSnapshot", snapshot.getSnapshotType());
    }
    
    @Test
    void testEventQueryByType() {
        // 创建并存储用户
        List<DomainEvent> events = user.getUncommittedEvents();
        eventSourcingService.storeEvent(events.get(0));
        user.markEventsAsCommitted();
        
        // 查询用户创建事件
        org.springframework.data.domain.Page<com.hibiscus.signal.core.entity.EventStore> page = eventQueryService.queryByEventType("UserCreated", 0, 10);
        
        // 验证查询结果
        assertNotNull(page);
        assertTrue(page.getTotalElements() > 0);
        assertEquals("UserCreated", page.getContent().get(0).getEventType());
    }
    
    @Test
    void testEventQueryByAggregateId() {
        // 创建并存储用户
        List<DomainEvent> events = user.getUncommittedEvents();
        eventSourcingService.storeEvent(events.get(0));
        user.markEventsAsCommitted();
        
        // 查询聚合根事件
        java.util.List<com.hibiscus.signal.core.entity.EventStore> eventsList = eventQueryService.queryByAggregateId(userId);
        
        // 验证查询结果
        assertNotNull(eventsList);
        assertEquals(1, eventsList.size());
        assertEquals(userId, eventsList.get(0).getAggregateId());
    }
    
    @Test
    void testEventStatistics() {
        // 创建并存储用户
        List<DomainEvent> events = user.getUncommittedEvents();
        eventSourcingService.storeEvent(events.get(0));
        user.markEventsAsCommitted();
        
        // 获取事件统计信息
        EventQueryService.EventStatistics stats = eventQueryService.getEventStatistics();
        
        // 验证统计信息
        assertNotNull(stats);
        assertTrue(stats.getTotalAggregates() > 0);
        assertTrue(stats.getTotalEventTypes() > 0);
        assertTrue(stats.getTotalEvents() > 0);
        assertTrue(stats.getEventTypeCounts().containsKey("UserCreated"));
    }
    
    @Test
    void testAggregateStatistics() {
        // 创建并存储用户
        List<DomainEvent> events = user.getUncommittedEvents();
        eventSourcingService.storeEvent(events.get(0));
        user.markEventsAsCommitted();
        
        // 获取聚合根统计信息
        EventQueryService.AggregateStatistics stats = eventQueryService.getAggregateStatistics(userId);
        
        // 验证统计信息
        assertNotNull(stats);
        assertEquals(userId, stats.getAggregateId());
        assertEquals(1, stats.getEventCount());
        assertEquals(1, stats.getLatestVersion());
        assertTrue(stats.isExists());
    }
    
    @Test
    void testEventReplayFromSnapshot() {
        // 创建并存储用户
        List<DomainEvent> events = user.getUncommittedEvents();
        eventSourcingService.storeEvent(events.get(0));
        user.markEventsAsCommitted();
        
        // 创建快照
        eventSourcingService.createSnapshot(userId, user, "UserSnapshot", "用户状态快照");
        
        // 更新用户信息
        user.updateInfo("newemail@example.com", "Updated User", "测试更新");
        events = user.getUncommittedEvents();
        eventSourcingService.storeEvent(events.get(0));
        user.markEventsAsCommitted();
        
        // 从快照重建用户
        EventSnapshot snapshot = eventSourcingService.getLatestSnapshot(userId).orElse(null);
        User replayedUser = eventReplayService.replayEvents(userId, User.class, snapshot);
        
        // 验证重建后的状态
        assertNotNull(replayedUser);
        assertEquals("newemail@example.com", replayedUser.getEmail());
        assertEquals("Updated User", replayedUser.getFullName());
    }
    
    @Test
    void testEventReplayInVersionRange() {
        // 创建并存储用户
        List<DomainEvent> events = user.getUncommittedEvents();
        eventSourcingService.storeEvent(events.get(0));
        user.markEventsAsCommitted();
        
        // 更新用户信息
        user.updateInfo("newemail@example.com", "Updated User", "测试更新");
        events = user.getUncommittedEvents();
        eventSourcingService.storeEvent(events.get(0));
        user.markEventsAsCommitted();
        
        // 从版本1开始重建用户
        User replayedUser = eventReplayService.replayEventsFromVersion(userId, User.class, 1);
        
        // 验证重建后的状态
        assertNotNull(replayedUser);
        assertEquals("newemail@example.com", replayedUser.getEmail());
        assertEquals("Updated User", replayedUser.getFullName());
    }
}
