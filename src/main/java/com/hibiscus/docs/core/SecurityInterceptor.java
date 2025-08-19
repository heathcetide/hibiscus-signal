package com.hibiscus.docs.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.InetAddress;
import java.util.List;

@Component
public class SecurityInterceptor implements HandlerInterceptor {

    @Autowired
    private AppConfigProperties appConfigProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 如果安全功能未启用，直接放行
        if (!appConfigProperties.getSecurity().isEnabled()) {
            return true;
        }

        String clientIp = getClientIpAddress(request);
        String accessToken = request.getHeader("X-Access-Token");
        String securityMode = appConfigProperties.getSecurity().getMode();

//        // 检查IP访问控制
//        if (securityMode.equals("ip") || securityMode.equals("both")) {
//            if (!isIpAllowed(clientIp)) {
//                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
//                response.getWriter().write("Access denied: IP not allowed");
//                return false;
//            }
//        }
//
//        // 检查令牌访问控制
//        if (securityMode.equals("token") || securityMode.equals("both")) {
//            if (!isTokenValid(accessToken)) {
//                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//                response.getWriter().write("Access denied: Invalid or missing token");
//                return false;
//            }
//        }

        return true;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    private boolean isIpAllowed(String clientIp) {
        // 允许本地访问
        if (appConfigProperties.getSecurity().isAllowLocalhost() && 
            (clientIp.equals("127.0.0.1") || clientIp.equals("::1") || clientIp.equals("localhost"))) {
            return true;
        }

        List<String> allowedIps = appConfigProperties.getSecurity().getAllowedIps();
        if (allowedIps == null || allowedIps.isEmpty()) {
            return true; // 如果没有配置允许的IP，则允许所有
        }

        for (String allowedIp : allowedIps) {
            if (isIpInRange(clientIp, allowedIp)) {
                return true;
            }
        }

        return false;
    }

    private boolean isIpInRange(String clientIp, String allowedIp) {
        if (allowedIp.contains("/")) {
            // CIDR格式检查
            return isIpInCidrRange(clientIp, allowedIp);
        } else {
            // 精确匹配
            return clientIp.equals(allowedIp);
        }
    }

    private boolean isIpInCidrRange(String clientIp, String cidr) {
        try {
            String[] parts = cidr.split("/");
            String networkIp = parts[0];
            int prefixLength = Integer.parseInt(parts[1]);

            InetAddress network = InetAddress.getByName(networkIp);
            InetAddress client = InetAddress.getByName(clientIp);

            byte[] networkBytes = network.getAddress();
            byte[] clientBytes = client.getAddress();

            if (networkBytes.length != clientBytes.length) {
                return false;
            }

            int numBytes = prefixLength / 8;
            int remainingBits = prefixLength % 8;

            // 检查完整字节
            for (int i = 0; i < numBytes; i++) {
                if (networkBytes[i] != clientBytes[i]) {
                    return false;
                }
            }

            // 检查剩余位
            if (remainingBits > 0) {
                int mask = (0xFF << (8 - remainingBits)) & 0xFF;
                if ((networkBytes[numBytes] & mask) != (clientBytes[numBytes] & mask)) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenValid(String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) {
            return false;
        }

        String expectedToken = appConfigProperties.getSecurity().getAccessToken();
        return accessToken.equals(expectedToken);
    }
}
