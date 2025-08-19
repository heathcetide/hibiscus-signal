package com.hibiscus.docs.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 捕获所有异常并记录到错误监控系统
 */
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Autowired
    private ErrorMonitor errorMonitor;

    @Autowired
    private PerformanceMonitor performanceMonitor;

    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex, WebRequest request, HttpServletRequest httpRequest) {
        // 记录错误到监控系统
        recordError(ex, request, httpRequest, "GENERAL_EXCEPTION", HttpStatus.INTERNAL_SERVER_ERROR.value());
        
        // 返回错误响应
        Map<String, Object> errorResponse = createErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex, WebRequest request, HttpServletRequest httpRequest) {
        recordError(ex, request, httpRequest, "RUNTIME_EXCEPTION", HttpStatus.INTERNAL_SERVER_ERROR.value());
        
        Map<String, Object> errorResponse = createErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request, HttpServletRequest httpRequest) {
        recordError(ex, request, httpRequest, "ILLEGAL_ARGUMENT", HttpStatus.BAD_REQUEST.value());
        
        Map<String, Object> errorResponse = createErrorResponse(ex, HttpStatus.BAD_REQUEST);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<Map<String, Object>> handleNullPointerException(NullPointerException ex, WebRequest request, HttpServletRequest httpRequest) {
        recordError(ex, request, httpRequest, "NULL_POINTER", HttpStatus.INTERNAL_SERVER_ERROR.value());
        
        Map<String, Object> errorResponse = createErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * 处理数字格式异常
     */
    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<Map<String, Object>> handleNumberFormatException(NumberFormatException ex, WebRequest request, HttpServletRequest httpRequest) {
        recordError(ex, request, httpRequest, "NUMBER_FORMAT", HttpStatus.BAD_REQUEST.value());
        
        Map<String, Object> errorResponse = createErrorResponse(ex, HttpStatus.BAD_REQUEST);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * 处理数组越界异常
     */
    @ExceptionHandler(ArrayIndexOutOfBoundsException.class)
    public ResponseEntity<Map<String, Object>> handleArrayIndexOutOfBoundsException(ArrayIndexOutOfBoundsException ex, WebRequest request, HttpServletRequest httpRequest) {
        recordError(ex, request, httpRequest, "ARRAY_INDEX_OUT_OF_BOUNDS", HttpStatus.INTERNAL_SERVER_ERROR.value());
        
        Map<String, Object> errorResponse = createErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * 处理类转换异常
     */
    @ExceptionHandler(ClassCastException.class)
    public ResponseEntity<Map<String, Object>> handleClassCastException(ClassCastException ex, WebRequest request, HttpServletRequest httpRequest) {
        recordError(ex, request, httpRequest, "CLASS_CAST", HttpStatus.INTERNAL_SERVER_ERROR.value());
        
        Map<String, Object> errorResponse = createErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * 处理安全异常
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleSecurityException(SecurityException ex, WebRequest request, HttpServletRequest httpRequest) {
        recordError(ex, request, httpRequest, "SECURITY", HttpStatus.FORBIDDEN.value());
        
        Map<String, Object> errorResponse = createErrorResponse(ex, HttpStatus.FORBIDDEN);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * 记录错误到监控系统
     */
    private void recordError(Exception ex, WebRequest request, HttpServletRequest httpRequest, String errorType, int statusCode) {
        try {
            String endpoint = httpRequest.getRequestURI();
            String method = httpRequest.getMethod();
            String userAgent = httpRequest.getHeader("User-Agent");
            String ipAddress = getClientIpAddress(httpRequest);
            String errorMessage = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            
            // 记录到错误监控器
            errorMonitor.recordError(endpoint, method, errorType, errorMessage, statusCode, 0, userAgent, ipAddress);
            
            // 记录到性能监控器（如果可用）
            if (performanceMonitor != null) {
                performanceMonitor.recordRequestComplete(endpoint, 0, true);
            }
            
        } catch (Exception monitorEx) {
            // 避免监控系统异常影响主流程
            System.err.println("[全局异常处理器] 记录错误失败: " + monitorEx.getMessage());
        }
    }

    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(Exception ex, HttpStatus status) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("status", status.value());
        errorResponse.put("error", status.getReasonPhrase());
        errorResponse.put("message", ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
        errorResponse.put("path", "未知");
        
        // 开发环境显示详细错误信息
        if (isDevelopmentMode()) {
            errorResponse.put("exception", ex.getClass().getName());
            errorResponse.put("stackTrace", getStackTrace(ex));
        }
        
        return errorResponse;
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * 获取异常堆栈信息
     */
    private String getStackTrace(Exception ex) {
        StringBuilder sb = new StringBuilder();
        StackTraceElement[] elements = ex.getStackTrace();
        
        for (int i = 0; i < Math.min(elements.length, 10); i++) {
            sb.append(elements[i].toString()).append("\n");
        }
        
        return sb.toString();
    }

    /**
     * 判断是否为开发模式
     */
    private boolean isDevelopmentMode() {
        try {
            String profiles = System.getProperty("spring.profiles.active");
            return profiles != null && (profiles.contains("dev") || profiles.contains("local"));
        } catch (Exception e) {
            return false;
        }
    }
}
