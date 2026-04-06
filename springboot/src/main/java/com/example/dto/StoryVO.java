package com.example.dto;

import java.time.LocalDateTime;

/**
 * 故事列表展示VO
 */
public class StoryVO {

  private Long id;
  private String title;
  private Integer category;
  private String categoryName;
  private Integer status;
  private String statusName;
  private Integer paragraphCount;
  private Integer participantCount;
  private Integer totalLikes;
  private Boolean isRecommended;
  private Long creatorId;
  private String creatorNickname;
  private String creatorAvatar;
  private LocalDateTime createTime;
  private String firstParagraphPreview; // 开篇预览(前100字)
  private LocalDateTime updateTime;
  private Integer myParagraphCount;
  private Integer myContribution;

  // ========== Getter/Setter ==========

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Integer getCategory() {
    return category;
  }

  public void setCategory(Integer category) {
    this.category = category;
  }

  public String getCategoryName() {
    return categoryName;
  }

  public void setCategoryName(String categoryName) {
    this.categoryName = categoryName;
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

  public Integer getParagraphCount() {
    return paragraphCount;
  }

  public void setParagraphCount(Integer paragraphCount) {
    this.paragraphCount = paragraphCount;
  }

  public Integer getParticipantCount() {
    return participantCount;
  }

  public void setParticipantCount(Integer participantCount) {
    this.participantCount = participantCount;
  }

  public Integer getTotalLikes() {
    return totalLikes;
  }

  public void setTotalLikes(Integer totalLikes) {
    this.totalLikes = totalLikes;
  }

  public Boolean getIsRecommended() {
    return isRecommended;
  }

  public void setIsRecommended(Boolean isRecommended) {
    this.isRecommended = isRecommended;
  }

  public Long getCreatorId() {
    return creatorId;
  }

  public void setCreatorId(Long creatorId) {
    this.creatorId = creatorId;
  }

  public String getCreatorNickname() {
    return creatorNickname;
  }

  public void setCreatorNickname(String creatorNickname) {
    this.creatorNickname = creatorNickname;
  }

  public String getCreatorAvatar() {
    return creatorAvatar;
  }

  public void setCreatorAvatar(String creatorAvatar) {
    this.creatorAvatar = creatorAvatar;
  }

  public LocalDateTime getCreateTime() {
    return createTime;
  }

  public void setCreateTime(LocalDateTime createTime) {
    this.createTime = createTime;
  }

  public String getFirstParagraphPreview() {
    return firstParagraphPreview;
  }

  public void setFirstParagraphPreview(String firstParagraphPreview) {
    this.firstParagraphPreview = firstParagraphPreview;
  }

  public LocalDateTime getUpdateTime() {
    return updateTime;
  }

  public void setUpdateTime(LocalDateTime updateTime) {
    this.updateTime = updateTime;
  }

  public Integer getMyParagraphCount() {
    return myParagraphCount;
  }

  public void setMyParagraphCount(Integer myParagraphCount) {
    this.myParagraphCount = myParagraphCount;
  }

  public Integer getMyContribution() {
    return myContribution;
  }

  public void setMyContribution(Integer myContribution) {
    this.myContribution = myContribution;
  }
}
