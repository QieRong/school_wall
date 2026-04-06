package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_report")
public class Report {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;   // 举报人
    private Long postId;   // 被举报帖子
    private String reason; // 举报理由 (类型 + 详情)
    private Integer status; // 0:待处理
    private LocalDateTime createTime;
}