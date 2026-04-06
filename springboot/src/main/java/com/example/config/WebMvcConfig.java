package com.example.config;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 * 注册拦截器
 */
@Configuration
@SuppressWarnings("null")
public class WebMvcConfig implements WebMvcConfigurer {

  @Resource
  private AdminInterceptor adminInterceptor;

  @Resource
  private TokenInterceptor tokenInterceptor;

  @Override
  public void addInterceptors(@NonNull InterceptorRegistry registry) {
    // Token拦截器：拦截需要认证的API
    registry.addInterceptor(tokenInterceptor)
        .addPathPatterns("/**")
        .excludePathPatterns(
            // 登录注册
            "/user/login", "/user/register",
            // 公开接口
            "/index/**", "/files/**",
            // 帖子公开接口
            "/post/list", "/post/detail", "/post/hot",
            // 评论公开接口
            "/comment/list",
            // 热词公开接口
            "/hotword/**",
            // 故事公开接口
            "/story/list", "/story/detail", "/story/paragraphs",
            // WebSocket
            "/ws/**",
            // 静态资源
            "/error", "/favicon.ico");

    // 管理员拦截器：拦截所有 /admin/** 请求
    registry.addInterceptor(adminInterceptor)
        .addPathPatterns("/admin/**");
  }

  @org.springframework.beans.factory.annotation.Value("${file.upload-path}")
  private String uploadPath;

  @Override
  public void addResourceHandlers(
      @NonNull org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry registry) {
    // 映射 /files/** 到本地文件目录
    // 确保路径以 / 结尾
    String path = uploadPath.endsWith("/") ? uploadPath : uploadPath + "/";
    registry.addResourceHandler("/files/**")
        .addResourceLocations("file:" + path);
  }
}
