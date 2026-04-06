package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 黑名单实体类
 * 用于屏蔽用户，阻断交互
 */
@Data
@TableName("sys_blacklist")
public class Blacklist {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;     // 主动拉黑方
    private Long targetId;   // 被拉黑方
    private LocalDateTime createTime;
}