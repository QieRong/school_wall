package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 访客实体类
 */
@Data
@TableName("sys_visitor")
public class Visitor {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;      // 被看的人
    private Long visitorId;   // 看的人
    private LocalDateTime visitTime;

    // --- 非数据库字段 ---
    @TableField(exist = false)
    private String visitorName;
    @TableField(exist = false)
    private String visitorAvatar;
}