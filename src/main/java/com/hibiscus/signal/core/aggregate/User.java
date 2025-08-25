package com.hibiscus.signal.core.aggregate;

import com.hibiscus.signal.core.event.DomainEvent;
import com.hibiscus.signal.core.event.UserEvents;
import com.hibiscus.signal.core.service.EventReplayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户聚合根
 * 演示如何实现事件处理器接口
 * 
 * @author heathcetide
 */
public class User implements EventReplayService.EventHandler {
    
    private static final Logger log = LoggerFactory.getLogger(User.class);
    
    private String userId;
    private String username;
    private String email;
    private String fullName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;
    private int loginCount;
    private boolean deleted;
    private String deletedBy;
    private LocalDateTime deletedAt;
    
    // 未提交的事件
    private final List<DomainEvent> uncommittedEvents = new ArrayList<>();
    
    public User() {
        this.status = "ACTIVE";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.loginCount = 0;
        this.deleted = false;
    }
    
    /**
     * 创建用户
     */
    public static User create(String userId, String username, String email, String fullName) {
        User user = new User();
        user.userId = userId;
        user.username = username;
        user.email = email;
        user.fullName = fullName;
        
        // 创建用户创建事件
        UserEvents.UserCreated event = new UserEvents.UserCreated(userId, 0, username, email, fullName);
        user.recordEvent(event);
        
        return user;
    }
    
    /**
     * 更新用户信息
     */
    public void updateInfo(String email, String fullName, String updateReason) {
        if (deleted) {
            throw new IllegalStateException("用户已删除，无法更新信息");
        }
        
        this.email = email;
        this.fullName = fullName;
        this.updatedAt = LocalDateTime.now();
        
        // 创建用户更新事件
        UserEvents.UserUpdated event = new UserEvents.UserUpdated(userId, getNextVersion(), 
            username, email, fullName, updateReason);
        recordEvent(event);
    }
    
    /**
     * 变更用户状态
     */
    public void changeStatus(String newStatus, String changeReason) {
        if (deleted) {
            throw new IllegalStateException("用户已删除，无法变更状态");
        }
        
        String oldStatus = this.status;
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
        
        // 创建状态变更事件
        UserEvents.UserStatusChanged event = new UserEvents.UserStatusChanged(userId, getNextVersion(), 
            username, oldStatus, newStatus, changeReason);
        recordEvent(event);
    }
    
    /**
     * 用户登录
     */
    public void login(String loginIp, String userAgent) {
        if (deleted) {
            throw new IllegalStateException("用户已删除，无法登录");
        }
        
        if (!"ACTIVE".equals(status)) {
            throw new IllegalStateException("用户状态非活跃，无法登录");
        }
        
        this.lastLoginAt = LocalDateTime.now();
        this.loginCount++;
        this.updatedAt = LocalDateTime.now();
        
        // 创建登录事件
        UserEvents.UserLoggedIn event = new UserEvents.UserLoggedIn(userId, getNextVersion(), 
            username, loginIp, userAgent);
        recordEvent(event);
    }
    
    /**
     * 用户注销
     */
    public void logout(String logoutReason) {
        if (deleted) {
            return; // 已删除用户无需处理注销
        }
        
        this.updatedAt = LocalDateTime.now();
        
        // 创建注销事件
        UserEvents.UserLoggedOut event = new UserEvents.UserLoggedOut(userId, getNextVersion(), 
            username, logoutReason);
        recordEvent(event);
    }
    
    /**
     * 删除用户
     */
    public void delete(String deleteReason, String deletedBy) {
        if (deleted) {
            return; // 已删除用户无需重复删除
        }
        
        this.deleted = true;
        this.deletedBy = deletedBy;
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        
        // 创建删除事件
        UserEvents.UserDeleted event = new UserEvents.UserDeleted(userId, getNextVersion(), 
            username, deleteReason, deletedBy);
        recordEvent(event);
    }
    
    /**
     * 记录事件
     */
    private void recordEvent(DomainEvent event) {
        uncommittedEvents.add(event);
        log.debug("记录事件: {}", event.getEventSummary());
    }
    
    /**
     * 获取未提交的事件
     */
    public List<DomainEvent> getUncommittedEvents() {
        return new ArrayList<>(uncommittedEvents);
    }
    
    /**
     * 标记事件已提交
     */
    public void markEventsAsCommitted() {
        uncommittedEvents.clear();
        log.debug("事件已标记为已提交");
    }
    
    /**
     * 获取下一个版本号
     */
    private long getNextVersion() {
        return uncommittedEvents.size();
    }
    
    /**
     * 实现事件处理器接口
     */
    @Override
    public void handleEvent(DomainEvent event) {
        if (event instanceof UserEvents.UserCreated) {
            handleUserCreated((UserEvents.UserCreated) event);
        } else if (event instanceof UserEvents.UserUpdated) {
            handleUserUpdated((UserEvents.UserUpdated) event);
        } else if (event instanceof UserEvents.UserStatusChanged) {
            handleUserStatusChanged((UserEvents.UserStatusChanged) event);
        } else if (event instanceof UserEvents.UserLoggedIn) {
            handleUserLoggedIn((UserEvents.UserLoggedIn) event);
        } else if (event instanceof UserEvents.UserLoggedOut) {
            handleUserLoggedOut((UserEvents.UserLoggedOut) event);
        } else if (event instanceof UserEvents.UserDeleted) {
            handleUserDeleted((UserEvents.UserDeleted) event);
        } else {
            log.warn("未知事件类型: {}", event.getEventType());
        }
    }
    
    /**
     * 处理用户创建事件
     */
    private void handleUserCreated(UserEvents.UserCreated event) {
        this.userId = event.getAggregateId();
        this.username = event.getUsername();
        this.email = event.getEmail();
        this.fullName = event.getFullName();
        this.createdAt = event.getCreatedAt();
        this.updatedAt = event.getCreatedAt();
        log.debug("处理用户创建事件: {}", event.getEventSummary());
    }
    
    /**
     * 处理用户更新事件
     */
    private void handleUserUpdated(UserEvents.UserUpdated event) {
        this.email = event.getEmail();
        this.fullName = event.getFullName();
        this.updatedAt = event.getUpdatedAt();
        log.debug("处理用户更新事件: {}", event.getEventSummary());
    }
    
    /**
     * 处理用户状态变更事件
     */
    private void handleUserStatusChanged(UserEvents.UserStatusChanged event) {
        this.status = event.getNewStatus();
        this.updatedAt = event.getChangedAt();
        log.debug("处理用户状态变更事件: {}", event.getEventSummary());
    }
    
    /**
     * 处理用户登录事件
     */
    private void handleUserLoggedIn(UserEvents.UserLoggedIn event) {
        this.lastLoginAt = event.getLoginAt();
        this.loginCount++;
        this.updatedAt = event.getLoginAt();
        log.debug("处理用户登录事件: {}", event.getEventSummary());
    }
    
    /**
     * 处理用户注销事件
     */
    private void handleUserLoggedOut(UserEvents.UserLoggedOut event) {
        this.updatedAt = event.getLogoutAt();
        log.debug("处理用户注销事件: {}", event.getEventSummary());
    }
    
    /**
     * 处理用户删除事件
     */
    private void handleUserDeleted(UserEvents.UserDeleted event) {
        this.deleted = true;
        this.deletedBy = event.getDeletedBy();
        this.deletedAt = event.getDeletedAt();
        this.updatedAt = event.getDeletedAt();
        log.debug("处理用户删除事件: {}", event.getEventSummary());
    }
    
    // Getters
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public int getLoginCount() { return loginCount; }
    public boolean isDeleted() { return deleted; }
    public String getDeletedBy() { return deletedBy; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    
    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", status='" + status + '\'' +
                ", deleted=" + deleted +
                ", loginCount=" + loginCount +
                '}';
    }
}
