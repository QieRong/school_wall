package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_file_record")
public class FileRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String filePath; // 磁盘绝对路径
    private String fileUrl;  // http链接
    private Integer isUsed;  // 0:未使用 1:已使用
    private LocalDateTime createTime;
}