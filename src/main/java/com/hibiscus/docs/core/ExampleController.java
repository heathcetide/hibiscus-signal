package com.hibiscus.docs.core;

import com.hibiscus.docs.core.annotations.ApiParam;
import com.hibiscus.docs.core.annotations.CreateUserRequest;
import com.hibiscus.docs.core.annotations.UpdateUserRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * 示例控制器
 * 展示如何使用API参数注解
 */
@RestController
@RequestMapping("/test/api/example")
public class ExampleController {

    /**
     * 获取用户信息
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> getUserById(
            @ApiParam(name = "id", description = "用户ID", type = "number", defaultValue = "1", required = true, example = "123")
            @PathVariable String id,
            
            @ApiParam(name = "includeProfile", description = "是否包含用户资料", type = "boolean", defaultValue = "false", example = "true")
            @RequestParam(defaultValue = "false") String includeProfile,
            
            @ApiParam(name = "fields", description = "返回字段列表", type = "array", example = "id,name,email")
            @RequestParam(required = false) List<String> fields) {
        
        Map<String, Object> user = new HashMap<>();
        user.put("id", id);
        user.put("name", "张三");
        user.put("email", "zhangsan@example.com");
        user.put("age", 25);
        
        if ("true".equals(includeProfile)) {
            user.put("profile", "用户详细资料");
        }
        
        return ResponseEntity.ok(user);
    }

    /**
     * 创建用户
     */
    @PostMapping("/users")
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody CreateUserRequest request) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "用户创建成功");
        result.put("userId", "12345");
        result.put("user", request);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> updateUser(
            @ApiParam(name = "id", description = "用户ID", type = "number", required = true, example = "123")
            @PathVariable String id,
            
            @RequestBody UpdateUserRequest request) {
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "用户更新成功");
        result.put("userId", id);
        result.put("updatedFields", request);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> deleteUser(
            @ApiParam(name = "id", description = "用户ID", type = "number", required = true, example = "123")
            @PathVariable String id,
            
            @ApiParam(name = "force", description = "是否强制删除", type = "boolean", defaultValue = "false", example = "true")
            @RequestParam(defaultValue = "false") String force) {
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "用户删除成功");
        result.put("userId", id);
        result.put("force", force);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 搜索用户
     */
    @GetMapping("/users/search")
    public ResponseEntity<Map<String, Object>> searchUsers(
            @ApiParam(name = "keyword", description = "搜索关键词", type = "string", example = "张三")
            @RequestParam String keyword,
            
            @ApiParam(name = "page", description = "页码", type = "number", defaultValue = "1", min = "1", example = "1")
            @RequestParam(defaultValue = "1") int page,
            
            @ApiParam(name = "size", description = "每页大小", type = "number", defaultValue = "10", min = "1", max = "100", example = "20")
            @RequestParam(defaultValue = "10") int size,
            
            @ApiParam(name = "sortBy", description = "排序字段", type = "string", allowedValues = {"name", "age", "createTime"}, example = "name")
            @RequestParam(defaultValue = "name") String sortBy,
            
            @ApiParam(name = "order", description = "排序方向", type = "string", allowedValues = {"asc", "desc"}, example = "asc")
            @RequestParam(defaultValue = "asc") String order) {
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("keyword", keyword);
        result.put("page", page);
        result.put("size", size);
        result.put("sortBy", sortBy);
        result.put("order", order);
        
        List<Map<String, Object>> users = new ArrayList<>();
        for (int i = 0; i < Math.min(size, 5); i++) {
            Map<String, Object> user = new HashMap<>();
            user.put("id", String.valueOf(i + 1));
            user.put("name", "用户" + (i + 1));
            user.put("age", 20 + i);
            users.add(user);
        }
        
        result.put("users", users);
        result.put("total", 100);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 获取用户统计信息
     */
    @GetMapping("/users/stats")
    public ResponseEntity<Map<String, Object>> getUserStats(
            @ApiParam(name = "startDate", description = "开始日期", type = "date", format = "yyyy-MM-dd", example = "2024-01-01")
            @RequestParam String startDate,
            
            @ApiParam(name = "endDate", description = "结束日期", type = "date", format = "yyyy-MM-dd", example = "2024-12-31")
            @RequestParam String endDate,
            
            @ApiParam(name = "groupBy", description = "分组方式", type = "string", allowedValues = {"day", "week", "month"}, example = "month")
            @RequestParam(defaultValue = "month") String groupBy) {
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("startDate", startDate);
        result.put("endDate", endDate);
        result.put("groupBy", groupBy);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", 1000);
        stats.put("activeUsers", 800);
        stats.put("newUsers", 50);
        
        result.put("stats", stats);
        
        return ResponseEntity.ok(result);
    }
}

