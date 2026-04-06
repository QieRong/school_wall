package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("hotword")
public class Hotword {

  @TableId(type = IdType.AUTO)
  private Long id;

  private String name;

  private String definition;

  private String example;

  private String tags;

  private String imageUrl;

  private Long userId;

  private Integer totalVotes;

  /** 状态：1-活跃，2-归档 */
  private Integer status;

  /** 是否官方推荐：0-否，1-是 */
  private Integer isRecommended;

  private LocalDateTime createTime;

  private LocalDateTime lastVoteTime;

  // 非数据库字段
  @TableField(exist = false)
  private String authorNickname;

  @TableField(exist = false)
  private String authorAvatar;

  @TableField(exist = false)
  private String heatLevel;
}
