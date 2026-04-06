package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore; // 新增导入
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String account;

    @JsonIgnore // 【安全修复】防止密码哈希泄露到前端
    private String password;

    private String nickname;
    private String avatar;
    private Integer role; // 0:用户 1:管理员
    private Integer status; // 1:正常 0:封禁
    private Integer creditScore;
    private Integer violationCount;
    private LocalDateTime banEndTime;
    private LocalDateTime createTime;
    private LocalDateTime lastActiveTime;

    // 个人中心字段
    private String coverImage; // 封面图
    private LocalDateTime lastNicknameUpdate; // 上次改名时间

    // 非数据库统计字段 (用于个人中心展示)
    @TableField(exist = false)
    private Integer totalLikes; // 累计获赞
    @TableField(exist = false)
    private Integer totalVisits; // 累计被访问
    @TableField(exist = false)
    private Integer followingCount; // 关注数
    @TableField(exist = false)
    private Integer followerCount; // 粉丝数
    @TableField(exist = false)
    private Integer postCount; // 发帖数
    @TableField(exist = false)
    private Integer commentCount; // 评论数
    @TableField(exist = false)
    private String token; // 登录Token
}
