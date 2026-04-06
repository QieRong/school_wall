package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内通知实体类
 */
@Data
@TableName("sys_notice")
public class SysNotice {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long receiverId;  // 接收者ID
    private Long senderId;    // 发送者ID (0代表系统)

    /**
     * 通知类型:
     * 1: 系统通知 (System)
     * 2: 收到评论 (Comment)
     * 3: 收到点赞 (Like)
     * 4: @我的 (At)
     */
    private Integer type;

    private Long relatedId;   // 关联ID (通常是帖子ID，点击跳转用)
    private String content;   // 通知摘要 (如：评论了你...)
    private Integer isRead;   // 0:未读 1:已读
    private LocalDateTime createTime;

    // --- 非数据库字段 ---
    @TableField(exist = false)
    private String senderName;   // 发送者昵称
    @TableField(exist = false)
    private String senderAvatar; // 发送者头像
}