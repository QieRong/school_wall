package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 完结投票记录实体类
 * 对应数据库表 story_finish_vote_record
 */
@TableName("story_finish_vote_record")
public class StoryFinishVoteRecord implements Serializable {

  private static final long serialVersionUID = 1L;

  @TableId(type = IdType.AUTO)
  private Long id;

  /** 投票ID */
  private Long voteId;

  /** 用户ID */
  private Long userId;

  /** 是否同意: 1同意 0反对 */
  private Integer agree;

  /** 投票时间 */
  private LocalDateTime createTime;

  // ========== Getter/Setter ==========

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getVoteId() {
    return voteId;
  }

  public void setVoteId(Long voteId) {
    this.voteId = voteId;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public Integer getAgree() {
    return agree;
  }

  public void setAgree(Integer agree) {
    this.agree = agree;
  }

  public LocalDateTime getCreateTime() {
    return createTime;
  }

  public void setCreateTime(LocalDateTime createTime) {
    this.createTime = createTime;
  }
}
