package com.example.utils;

import org.apache.tika.Tika;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * 文件验证工具类
 * 提供基于魔数和MIME类型的文件验证，防止文件伪造攻击
 */
public class FileValidator {

    private static final Tika tika = new Tika();

    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final long MAX_VIDEO_SIZE = 100 * 1024 * 1024; // 100MB

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "bmp");

    private static final Set<String> ALLOWED_VIDEO_EXTENSIONS = Set.of(
            "mp4", "webm", "mov", "avi");

    private static final Set<String> ALLOWED_IMAGE_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp");

    private static final Set<String> ALLOWED_VIDEO_MIME_TYPES = Set.of(
            "video/mp4", "video/webm", "video/quicktime", "video/x-msvideo");

    /**
     * 图片文件魔数签名
     */
    private static final byte[][] IMAGE_MAGIC_NUMBERS = {
            { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF }, // JPEG
            { (byte) 0x89, 0x50, 0x4E, 0x47 }, // PNG
            { 0x47, 0x49, 0x46, 0x38 }, // GIF
            { 0x52, 0x49, 0x46, 0x46 }, // WEBP (RIFF header)
            { 0x42, 0x4D } // BMP
    };

    /**
     * 验证图片文件
     * 按顺序执行：魔数验证 → MIME类型验证 → 大小验证 → 后缀验证
     */
    public static void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        // 1. 魔数验证
        validateMagicNumber(file, IMAGE_MAGIC_NUMBERS, "图片");

        // 2. MIME类型验证
        String detectedMimeType = validateMimeType(file);
        if (!ALLOWED_IMAGE_MIME_TYPES.contains(detectedMimeType)) {
            throw new IllegalArgumentException(
                    "文件类型不匹配：检测到的MIME类型为 " + detectedMimeType + "，不是有效的图片格式");
        }

        // 3. 大小验证
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException(
                    "图片文件大小不能超过 " + (MAX_IMAGE_SIZE / 1024 / 1024) + "MB");
        }

        // 4. 后缀验证
        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("不支持的图片文件扩展名: " + extension);
        }
    }

    /**
     * 验证视频文件
     */
    public static void validateVideoFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        // MIME类型验证
        String detectedMimeType = validateMimeType(file);
        if (!ALLOWED_VIDEO_MIME_TYPES.contains(detectedMimeType)) {
            throw new IllegalArgumentException(
                    "文件类型不匹配：检测到的MIME类型为 " + detectedMimeType + "，不是有效的视频格式");
        }

        // 大小验证
        if (file.getSize() > MAX_VIDEO_SIZE) {
            throw new IllegalArgumentException(
                    "视频文件大小不能超过 " + (MAX_VIDEO_SIZE / 1024 / 1024) + "MB");
        }

        // 后缀验证
        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_VIDEO_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("不支持的视频文件扩展名: " + extension);
        }
    }

    /**
     * 验证文件魔数
     * 通过读取文件头部字节判断真实文件类型
     */
    public static void validateMagicNumber(MultipartFile file, byte[][] allowedMagicNumbers, String fileType) {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] fileHeader = new byte[12];
            int bytesRead = inputStream.read(fileHeader);

            if (bytesRead < 2) {
                throw new IllegalArgumentException("文件内容无效");
            }

            boolean isValid = false;
            for (byte[] magicNumber : allowedMagicNumbers) {
                if (matchesMagicNumber(fileHeader, magicNumber)) {
                    isValid = true;
                    break;
                }
            }

            if (!isValid) {
                throw new IllegalArgumentException(
                        "文件魔数验证失败：文件头部不匹配任何有效的" + fileType + "格式");
            }

        } catch (IOException e) {
            throw new IllegalArgumentException("读取文件失败: " + e.getMessage());
        }
    }

    /**
     * 使用Apache Tika检测文件的真实MIME类型
     */
    public static String validateMimeType(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            String detectedMimeType = tika.detect(inputStream, file.getOriginalFilename());
            return detectedMimeType;
        } catch (IOException e) {
            throw new IllegalArgumentException("MIME类型检测失败: " + e.getMessage());
        }
    }

    /**
     * 匹配魔数
     */
    private static boolean matchesMagicNumber(byte[] fileHeader, byte[] magicNumber) {
        if (fileHeader.length < magicNumber.length) {
            return false;
        }
        for (int i = 0; i < magicNumber.length; i++) {
            if (fileHeader[i] != magicNumber[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取文件扩展名
     */
    private static String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }
}
