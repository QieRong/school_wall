package com.example.utils;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 权限上下文工具类
 * 防治垂直越权漏洞的核心组件
 */
public class AuthUtil {
    
    /**
     * 获取当前实际登录的绝对可信 UserId
     * 依赖于 TokenInterceptor 放行时的设定
     */
    public static Long getCurrentUserId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            Object userId = request.getAttribute("currentUserId");
            if (userId != null) {
                return (Long) userId;
            }
        }
        return null;
    }
}
