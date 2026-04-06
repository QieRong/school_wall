package com.example.config;

import com.example.entity.User;
import com.example.mapper.UserMapper;
import com.example.utils.JwtUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.PrintWriter;

/**
 * 管理员权限拦截器
 * 拦截所有 /admin/** 请求，校验用户是否为管理员
 */
@Component
public class AdminInterceptor implements HandlerInterceptor {

  @Resource
  private JwtUtils jwtUtils;

  @Resource
  private UserMapper userMapper;

  @Override
  public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
      @NonNull Object handler) throws Exception {
    // 1. 获取Token
    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      sendError(response, 401, "请先登录");
      return false;
    }

    String token = authHeader.substring(7);

    // 2. 验证Token
    Long userId = jwtUtils.verifyToken(token);
    if (userId == null) {
      sendError(response, 401, "登录已过期，请重新登录");
      return false;
    }

    // 3. 检查是否为管理员
    User user = userMapper.selectById(userId);
    if (user == null || user.getRole() == null || user.getRole() != 1) {
      sendError(response, 403, "无权限访问");
      return false;
    }

    // 4. 放行
    return true;
  }

  private void sendError(HttpServletResponse response, int status, String message) throws Exception {
    response.setStatus(status);
    response.setContentType("application/json;charset=UTF-8");
    PrintWriter writer = response.getWriter();
    writer.write("{\"code\":\"" + status + "\",\"msg\":\"" + message + "\"}");
    writer.flush();
  }
}
