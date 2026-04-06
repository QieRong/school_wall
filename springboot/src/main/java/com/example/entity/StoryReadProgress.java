package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 故事阅读进度实体类
 * 对应数据库表 story_read_progress
 */
@TableName("story_read_progress")
public class StoryReadProgress implements Serializable {

  private static final long serialVersionUID = 1L;

  @TableId(type = IdType.AUTO)
  private Long id;

  /** 故事ID */
  private Long storyId;

  /** 用户ID */
  private Long userId;

  /** 最后阅读的段落序号 */
  private Integer lastReadSequence;

  /** 更新时间 */
  private LocalDateTime updateTime;

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

  public Integer getLastReadSequence() {
    return lastReadSequence;
  }

  public void setLastReadSequence(Integer lastReadSequence) {
    this.lastReadSequence = lastReadSequence;
  }

  public LocalDateTime getUpdateTime() {
    return updateTime;
  }

  public void setUpdateTime(LocalDateTime updateTime) {
    this.updateTime = updateTime;
  }
}
