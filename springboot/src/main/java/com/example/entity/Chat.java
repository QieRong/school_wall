package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_chat")
public class Chat {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long senderId;
    private Long receiverId;
    private String content;

    /** 0:文本 1:图片 2:表情 */
    private Integer type;

    /** 0:未读 1:已读 */
    private Integer isRead;

    /** 1:已撤回 */
    private Integer isWithdrawn;

    private LocalDateTime createTime;

    // --- 辅助字段 ---
    @TableField(exist = false)
    private String senderName;
    @TableField(exist = false)
    private String senderAvatar;
}