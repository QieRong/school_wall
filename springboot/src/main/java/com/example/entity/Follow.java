package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 关注实体类
 * 记录用户之间的单向关注关系
 */
@Data
@TableName("sys_follow")
public class Follow {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long followerId; // 粉丝ID
    private Long targetId;   // 被关注者ID
    private LocalDateTime createTime;

    // --- 辅助字段 (用于列表展示) ---
    @TableField(exist = false)
    private String nickname; // 对方昵称
    @TableField(exist = false)
    private String avatar;   // 对方头像
    @TableField(exist = false)
    private String signature; // 对方签名(预留)
    @TableField(exist = false)
    private boolean isMutual; // 是否互关
}