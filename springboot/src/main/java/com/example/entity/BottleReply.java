// File: springboot/src/main/java/com/example/entity/BottleReply.java
package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 漂流瓶回复实体类
 * 对应数据库表 bottle_reply
 */
@TableName("bottle_reply")
public class BottleReply implements Serializable {

  private static final long serialVersionUID = 1L;

  @TableId(type = IdType.AUTO)
  private Long id;

  /** 漂流瓶ID */
  private Long bottleId;

  /** 回复者ID */
  private Long userId;

  /** 回复内容(1-50字) */
  private String content;

  /** 创建时间 */
  private LocalDateTime createTime;

  // ========== 非数据库字段 ==========

  /** 回复者昵称 */
  @TableField(exist = false)
  private String nickname;

  /** 回复者头像 */
  @TableField(exist = false)
  private String avatar;

  // ========== Getter/Setter ==========

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getBottleId() {
    return bottleId;
  }

  public void setBottleId(Long bottleId) {
    this.bottleId = bottleId;
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

  public LocalDateTime getCreateTime() {
    return createTime;
  }

  public void setCreateTime(LocalDateTime createTime) {
    this.createTime = createTime;
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
}
