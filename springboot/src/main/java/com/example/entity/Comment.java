package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论实体类
 */
@Data
@TableName("comment")
public class Comment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long postId; // 关联帖子
    private Long userId; // 评论者
    private String content; // 内容
    private String imgUrl; // 图片评论(可选)
    private Long parentId; // 父评论ID (一级评论为0/null)
    private Long replyUserId; // 被回复者ID
    private Integer likeCount;// 点赞数
    private LocalDateTime createTime;

    // --- 非数据库字段 (用于前端展示) ---
    @TableField(exist = false)
    private String nickname; // 评论者昵称
    @TableField(exist = false)
    private String avatar; // 评论者头像
    @TableField(exist = false)
    private String replyNickname; // 被回复者昵称(楼中楼用)
    @TableField(exist = false)
    private java.util.List<Comment> children; // 【优化】子评论列表(树形结构用)
}