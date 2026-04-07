package com.example.config;

import com.example.utils.JwtUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.PrintWriter;

/**
 * Token验证拦截器
 * 拦截所有需要认证的API请求，验证JWT Token的有效性
 */
@Component
public class TokenInterceptor implements HandlerInterceptor {

  @Resource
  private JwtUtils jwtUtils;

  @Override
  public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
      @NonNull Object handler) throws Exception {
    // 1. 获取Token
    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      sendError(response, 401, "未登录或Token已过期");
      return false;
    }

    String token = authHeader.substring(7);

    // 2. 验证Token
    Long userId = jwtUtils.verifyToken(token);
    if (userId == null) {
      sendError(response, 401, "未登录或Token已过期");
      return false;
    }

    // 3. Token有效，将 userId 放进 request，供后续 Controller 鉴权使用
    request.setAttribute("currentUserId", userId);
    
    // 4. 放行
    return true;
  }

  private void sendError(HttpServletResponse response, int status, String message) throws Exception {
    response.setStatus(status);
    response.setContentType("application/json;charset=UTF-8");
    PrintWriter writer = response.getWriter();
    writer.write("{\"code\":" + status + ",\"msg\":\"" + message + "\"}");
    writer.flush();
  }
}
