package com.hibiscus.docs.core;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/test")
public class TestApiController {

    /**
     * 获取用户信息
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> getUserById(
            @PathVariable Long id,
            @RequestParam(required = false) String fields) {
        
        Map<String, Object> user = new HashMap<>();
        user.put("id", id);
        user.put("name", "测试用户" + id);
        user.put("email", "user" + id + "@example.com");
        user.put("status", "active");
        
        return ResponseEntity.ok(user);
    }

    /**
     * 创建新用户
     */
    @PostMapping("/users")
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody Map<String, Object> userData) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", System.currentTimeMillis());
        response.put("message", "用户创建成功");
        response.put("user", userData);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable Long id,
            @RequestBody Map<String, Object> userData) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("message", "用户更新成功");
        response.put("user", userData);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("message", "用户删除成功");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 搜索用户
     */
    @GetMapping("/users/search")
    public ResponseEntity<List<Map<String, Object>>> searchUsers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        List<Map<String, Object>> users = new ArrayList<>();
        for (int i = 1; i <= Math.min(size, 5); i++) {
            Map<String, Object> user = new HashMap<>();
            user.put("id", page * 100 + i);
            user.put("name", "用户" + (page * 100 + i));
            user.put("keyword", keyword);
            users.add(user);
        }
        
        return ResponseEntity.ok(users);
    }

    /**
     * 获取系统状态
     */
    @GetMapping("/system/status")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "running");
        status.put("uptime", System.currentTimeMillis());
        status.put("version", "1.0.0");
        status.put("timestamp", java.time.LocalDateTime.now().toString());
        
        return ResponseEntity.ok(status);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(health);
    }
}
