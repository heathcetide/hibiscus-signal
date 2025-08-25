package com.hibiscus.signal.core.event;

import java.time.LocalDateTime;

/**
 * 用户相关领域事件
 * 演示事件溯源的使用
 * 
 * @author heathcetide
 */
public class UserEvents {
    
    /**
     * 用户创建事件
     */
    public static class UserCreated extends DomainEvent {
        private final String username;
        private final String email;
        private final String fullName;
        private final LocalDateTime createdAt;
        
        public UserCreated(String aggregateId, long version, String username, String email, String fullName) {
            super(aggregateId, version);
            this.username = username;
            this.email = email;
            this.fullName = fullName;
            this.createdAt = LocalDateTime.now();
        }
        
        @Override
        public String getEventType() {
            return "UserCreated";
        }
        
        // Getters
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public String getFullName() { return fullName; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }
    
    /**
     * 用户信息更新事件
     */
    public static class UserUpdated extends DomainEvent {
        private final String username;
        private final String email;
        private final String fullName;
        private final LocalDateTime updatedAt;
        private final String updateReason;
        
        public UserUpdated(String aggregateId, long version, String username, String email, 
                         String fullName, String updateReason) {
            super(aggregateId, version);
            this.username = username;
            this.email = email;
            this.fullName = fullName;
            this.updatedAt = LocalDateTime.now();
            this.updateReason = updateReason;
        }
        
        @Override
        public String getEventType() {
            return "UserUpdated";
        }
        
        // Getters
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public String getFullName() { return fullName; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public String getUpdateReason() { return updateReason; }
    }
    
    /**
     * 用户状态变更事件
     */
    public static class UserStatusChanged extends DomainEvent {
        private final String username;
        private final String oldStatus;
        private final String newStatus;
        private final LocalDateTime changedAt;
        private final String changeReason;
        
        public UserStatusChanged(String aggregateId, long version, String username, 
                               String oldStatus, String newStatus, String changeReason) {
            super(aggregateId, version);
            this.username = username;
            this.oldStatus = oldStatus;
            this.newStatus = newStatus;
            this.changedAt = LocalDateTime.now();
            this.changeReason = changeReason;
        }
        
        @Override
        public String getEventType() {
            return "UserStatusChanged";
        }
        
        // Getters
        public String getUsername() { return username; }
        public String getOldStatus() { return oldStatus; }
        public String getNewStatus() { return newStatus; }
        public LocalDateTime getChangedAt() { return changedAt; }
        public String getChangeReason() { return changeReason; }
    }
    
    /**
     * 用户登录事件
     */
    public static class UserLoggedIn extends DomainEvent {
        private final String username;
        private final String loginIp;
        private final String userAgent;
        private final LocalDateTime loginAt;
        
        public UserLoggedIn(String aggregateId, long version, String username, 
                           String loginIp, String userAgent) {
            super(aggregateId, version);
            this.username = username;
            this.loginIp = loginIp;
            this.userAgent = userAgent;
            this.loginAt = LocalDateTime.now();
        }
        
        @Override
        public String getEventType() {
            return "UserLoggedIn";
        }
        
        // Getters
        public String getUsername() { return username; }
        public String getLoginIp() { return loginIp; }
        public String getUserAgent() { return userAgent; }
        public LocalDateTime getLoginAt() { return loginAt; }
    }
    
    /**
     * 用户注销事件
     */
    public static class UserLoggedOut extends DomainEvent {
        private final String username;
        private final LocalDateTime logoutAt;
        private final String logoutReason;
        
        public UserLoggedOut(String aggregateId, long version, String username, String logoutReason) {
            super(aggregateId, version);
            this.username = username;
            this.logoutAt = LocalDateTime.now();
            this.logoutReason = logoutReason;
        }
        
        @Override
        public String getEventType() {
            return "UserLoggedOut";
        }
        
        // Getters
        public String getUsername() { return username; }
        public LocalDateTime getLogoutAt() { return logoutAt; }
        public String getLogoutReason() { return logoutReason; }
    }
    
    /**
     * 用户删除事件
     */
    public static class UserDeleted extends DomainEvent {
        private final String username;
        private final LocalDateTime deletedAt;
        private final String deleteReason;
        private final String deletedBy;
        
        public UserDeleted(String aggregateId, long version, String username, 
                          String deleteReason, String deletedBy) {
            super(aggregateId, version);
            this.username = username;
            this.deletedAt = LocalDateTime.now();
            this.deleteReason = deleteReason;
            this.deletedBy = deletedBy;
        }
        
        @Override
        public String getEventType() {
            return "UserDeleted";
        }
        
        // Getters
        public String getUsername() { return username; }
        public LocalDateTime getDeletedAt() { return deletedAt; }
        public String getDeleteReason() { return deleteReason; }
        public String getDeletedBy() { return deletedBy; }
    }
}
