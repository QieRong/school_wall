package com.example.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class FileConfig implements WebMvcConfigurer {

    // 读取 yml 中的文件上传路径
    @Value("${file.upload-path:./files/}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // 【修复】将相对路径转换为绝对路径
        File uploadDir = new File(uploadPath);
        if (!uploadDir.isAbsolute()) {
            uploadDir = new File(System.getProperty("user.dir"), uploadPath);
        }

        // 确保路径以 / 结尾
        String absolutePath = uploadDir.getAbsolutePath();
        if (!absolutePath.endsWith(File.separator)) {
            absolutePath = absolutePath + File.separator;
        }

        System.out.println("[FileConfig] 文件资源映射: /files/** -> file:" + absolutePath);

        // 将 /files/** 请求映射到本地磁盘路径
        // 注意：Windows路径需要加 file:/ 前缀
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + absolutePath);
    }
}
