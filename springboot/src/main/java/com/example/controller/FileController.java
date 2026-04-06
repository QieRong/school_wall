// File: springboot/src/main/java/com/example/controller/FileController.java
package com.example.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.example.common.Result;
import com.example.entity.FileRecord;
import com.example.mapper.FileRecordMapper;
import com.example.utils.FileValidator;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Date;

@RestController
@RequestMapping("/file")
public class FileController {

    @Value("${file.upload-path}")
    private String uploadPath;

    @Value("${server.port}")
    private String port;

    // 【安全优化】支持环境变量配置基础URL
    @Value("${fileBaseUrl:http://localhost:${server.port}}")
    private String fileBaseUrl;

    @Resource
    private FileRecordMapper fileRecordMapper;

    // 【安全】允许的文件类型白名单
    private static final java.util.Set<String> ALLOWED_IMAGE_TYPES = java.util.Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "bmp");
    private static final java.util.Set<String> ALLOWED_VIDEO_TYPES = java.util.Set.of(
            "mp4", "webm", "mov", "avi");

    @PostMapping("/upload")
    public Result<String> upload(@RequestParam MultipartFile file,
            @RequestParam(defaultValue = "0") Long userId) throws IOException {

        // 使用FileValidator进行完整验证（魔数 → MIME → 大小 → 后缀）
        try {
            String originalFilename = file.getOriginalFilename();
            String suffix = FileUtil.getSuffix(originalFilename);
            String lowerSuffix = suffix != null ? suffix.toLowerCase() : "";

            boolean isImage = ALLOWED_IMAGE_TYPES.contains(lowerSuffix);
            boolean isVideo = ALLOWED_VIDEO_TYPES.contains(lowerSuffix);

            if (isImage) {
                FileValidator.validateImageFile(file);
            } else if (isVideo) {
                FileValidator.validateVideoFile(file);
            } else {
                return Result.error("不支持的文件类型");
            }
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }

        String originalFilename = file.getOriginalFilename();
        String suffix = FileUtil.getSuffix(originalFilename);
        String newFileName = System.currentTimeMillis() + "_" + IdUtil.fastSimpleUUID() + "." + suffix;

        // 【核心升级】按日期分子目录: files/{userId}/{yyyyMMdd}/
        String dateDir = DateUtil.format(new Date(), "yyyyMMdd");
        String relativePath = userId + "/" + dateDir + "/";

        // 【修复】使用绝对路径确保目录正确创建
        File baseDir = new File(uploadPath);
        if (!baseDir.isAbsolute()) {
            baseDir = new File(System.getProperty("user.dir"), uploadPath);
        }
        File finalDir = new File(baseDir, relativePath);
        if (!finalDir.exists()) {
            finalDir.mkdirs();
        }

        File dest = new File(finalDir, newFileName);
        file.transferTo(dest);
        String fullPath = dest.getAbsolutePath();

        // 【安全优化】使用配置的基础URL
        String url = fileBaseUrl + "/files/" + relativePath + newFileName;

        // 记录到数据库 (用于定期清理)
        FileRecord record = new FileRecord();
        record.setUserId(userId);
        record.setFilePath(fullPath);
        record.setFileUrl(url);
        record.setIsUsed(0); // 默认为垃圾文件，发帖成功后转正
        fileRecordMapper.insert(record);

        return Result.success(url);
    }
}