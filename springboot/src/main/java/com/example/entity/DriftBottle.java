// File: springboot/src/main/java/com/example/entity/DriftBottle.java
package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 漂流瓶实体类
 * 对应数据库表 drift_bottle
 */
@TableName("drift_bottle")
public class DriftBottle implements Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * 漂流瓶状态常量
   */
  public static final int STATUS_DRIFTING = 0; // 漂流中
  public static final int STATUS_OPENED = 1; // 被打开
  public static final int STATUS_COLLECTED = 2; // 被珍藏
  public static final int STATUS_SUNKEN = 3; // 已沉没

  /**
   * 漂流方向常量
   */
  public static final int DIRECTION_SAKURA = 1; // 樱花海岸(文艺抒情)
  public static final int DIRECTION_DEEP_SEA = 2; // 深海秘境(心事秘密)
  public static final int DIRECTION_STARS = 3; // 星辰大海(梦想寄语)

  @TableId(type = IdType.AUTO)
  private Long id;

  /** 投放者ID */
  private Long userId;

  /** 瓶中内容(1-200字) */
  private String content;

  /** 漂流方向: 1樱花海岸 2深海秘境 3星辰大海 */
  private Integer direction;

  /** 状态: 0漂流中 1被打开 2被珍藏 3已沉没 */
  private Integer status;

  /** 被打开次数 */
  private Integer viewCount;

  /** 是否被标记审核: 0否 1是 */
  private Integer isFlagged;

  /** 创建时间 */
  private LocalDateTime createTime;

  /** 过期时间(创建时间+7天) */
  private LocalDateTime expireTime;

  /** 是否匿名: 0否 1是 */
  private Integer isAnonymous;

  // ========== 非数据库字段 ==========

  /** 投放者昵称(用于显示) */
  @TableField(exist = false)
  private String nickname;

  /** 投放者头像 */
  @TableField(exist = false)
  private String avatar;

  /** 瓶子的回复列表 */
  @TableField(exist = false)
  private List<BottleReply> replies;

  /** 回复数量 */
  @TableField(exist = false)
  private Integer replyCount;

  // ========== Getter/Setter ==========

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public Integer getDirection() {
    return direction;
  }

  public void setDirection(Integer direction) {
    this.direction = direction;
  }

  public Integer getStatus() {
    return status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }

  public Integer getViewCount() {
    return viewCount;
  }

  public void setViewCount(Integer viewCount) {
    this.viewCount = viewCount;
  }

  public Integer getIsFlagged() {
    return isFlagged;
  }

  public void setIsFlagged(Integer isFlagged) {
    this.isFlagged = isFlagged;
  }

  public LocalDateTime getCreateTime() {
    return createTime;
  }

  public void setCreateTime(LocalDateTime createTime) {
    this.createTime = createTime;
  }

  public LocalDateTime getExpireTime() {
    return expireTime;
  }

  public void setExpireTime(LocalDateTime expireTime) {
    this.expireTime = expireTime;
  }

  public Integer getIsAnonymous() {
    return isAnonymous;
  }

  public void setIsAnonymous(Integer isAnonymous) {
    this.isAnonymous = isAnonymous;
  }

  public String getNickname() {
    return nickname;
  }

  public void setNickname(String nickname) {
    this.nickname = nickname;
  }

  public String getAvatar() {
    return avatar;
  }

  public void setAvatar(String avatar) {
    this.avatar = avatar;
  }

  public List<BottleReply> getReplies() {
    return replies;
  }

  public void setReplies(List<BottleReply> replies) {
    this.replies = replies;
  }

  public Integer getReplyCount() {
    return replyCount;
  }

  public void setReplyCount(Integer replyCount) {
    this.replyCount = replyCount;
  }

  /**
   * 获取漂流方向的中文名称
   */
  public String getDirectionName() {
    return switch (this.direction) {
      case DIRECTION_SAKURA -> "樱花海岸";
      case DIRECTION_DEEP_SEA -> "深海秘境";
      case DIRECTION_STARS -> "星辰大海";
      default -> "未知";
    };
  }

  /**
   * 获取状态的中文名称
   */
  public String getStatusName() {
    return switch (this.status) {
      case STATUS_DRIFTING -> "漂流中";
      case STATUS_OPENED -> "被打开";
      case STATUS_COLLECTED -> "被珍藏";
      case STATUS_SUNKEN -> "已沉没";
      default -> "未知";
    };
  }
}
