package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 故事贡献度实体类
 * 对应数据库表 story_contribution
 */
@TableName("story_contribution")
public class StoryContribution implements Serializable {

  private static final long serialVersionUID = 1L;

  /** 贡献度点数常量 */
  public static final int POINTS_WRITE_PARAGRAPH = 10; // 续写段落基础分
  public static final int POINTS_PER_LIKE = 1; // 每个点赞
  public static final int POINTS_KEY_POINT = 50; // 成为关键转折点

  @TableId(type = IdType.AUTO)
  private Long id;

  /** 故事ID */
  private Long storyId;

  /** 用户ID */
  private Long userId;

  /** 贡献点数 */
  private Integer points;

  /** 续写段落数 */
  private Integer paragraphCount;

  /** 获得点赞数 */
  private Integer likeReceived;

  /** 关键转折点数 */
  private Integer keyPointCount;

  /** 更新时间 */
  private LocalDateTime updateTime;

  // ========== 非数据库字段 ==========

  /** 用户昵称 */
  @TableField(exist = false)
  private String nickname;

  /** 用户头像 */
  @TableField(exist = false)
  private String avatar;

  /** 排名 */
  @TableField(exist = false)
  private Integer rank;

  // ========== Getter/Setter ==========

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getStoryId() {
    return storyId;
  }

  public void setStoryId(Long storyId) {
    this.storyId = storyId;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public Integer getPoints() {
    return points;
  }

  public void setPoints(Integer points) {
    this.points = points;
  }

  public Integer getParagraphCount() {
    return paragraphCount;
  }

  public void setParagraphCount(Integer paragraphCount) {
    this.paragraphCount = paragraphCount;
  }

  public Integer getLikeReceived() {
    return likeReceived;
  }

  public void setLikeReceived(Integer likeReceived) {
    this.likeReceived = likeReceived;
  }

  public Integer getKeyPointCount() {
    return keyPointCount;
  }

  public void setKeyPointCount(Integer keyPointCount) {
    this.keyPointCount = keyPointCount;
  }

  public LocalDateTime getUpdateTime() {
    return updateTime;
  }

  public void setUpdateTime(LocalDateTime updateTime) {
    this.updateTime = updateTime;
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

  public Integer getRank() {
    return rank;
  }

  public void setRank(Integer rank) {
    this.rank = rank;
  }
}
