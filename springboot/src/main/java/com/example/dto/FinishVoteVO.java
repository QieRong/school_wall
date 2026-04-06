package com.example.dto;

import java.time.LocalDateTime;

/**
 * 完结投票信息VO
 */
public class FinishVoteVO {

  private Long id;
  private Long storyId;
  private Long initiatorId;
  private String initiatorNickname;
  private Integer agreeCount;
  private Integer disagreeCount;
  private Integer totalVoters;
  private Integer status;
  private String statusName;
  private LocalDateTime createTime;
  private LocalDateTime expireTime;
  private Boolean hasVoted; // 当前用户是否已投票
  private Boolean myVote; // 当前用户的投票(true=同意, false=反对, null=未投票)

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

  public Long getInitiatorId() {
    return initiatorId;
  }

  public void setInitiatorId(Long initiatorId) {
    this.initiatorId = initiatorId;
  }

  public String getInitiatorNickname() {
    return initiatorNickname;
  }

  public void setInitiatorNickname(String initiatorNickname) {
    this.initiatorNickname = initiatorNickname;
  }

  public Integer getAgreeCount() {
    return agreeCount;
  }

  public void setAgreeCount(Integer agreeCount) {
    this.agreeCount = agreeCount;
  }

  public Integer getDisagreeCount() {
    return disagreeCount;
  }

  public void setDisagreeCount(Integer disagreeCount) {
    this.disagreeCount = disagreeCount;
  }

  public Integer getTotalVoters() {
    return totalVoters;
  }

  public void setTotalVoters(Integer totalVoters) {
    this.totalVoters = totalVoters;
  }

  public Integer getStatus() {
    return status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }

  public String getStatusName() {
    return statusName;
  }

  public void setStatusName(String statusName) {
    this.statusName = statusName;
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

  public Boolean getHasVoted() {
    return hasVoted;
  }

  public void setHasVoted(Boolean hasVoted) {
    this.hasVoted = hasVoted;
  }

  public Boolean getMyVote() {
    return myVote;
  }

  public void setMyVote(Boolean myVote) {
    this.myVote = myVote;
  }
}
