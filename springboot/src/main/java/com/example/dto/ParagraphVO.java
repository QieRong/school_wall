package com.example.dto;

import java.time.LocalDateTime;

/**
 * 段落展示VO
 */
public class ParagraphVO {

  private Long id;
  private Long storyId;
  private String content;
  private String imageUrl;
  private Long authorId;
  private String authorNickname;
  private String authorAvatar;
  private String penName;
  private Integer sequence;
  private Integer likeCount;
  private Boolean isHot;
  private Boolean isKeyPoint;
  private Boolean isLikedByMe; // 当前用户是否点赞
  private Long parentId;
  private String branchName;
  private LocalDateTime createTime;
  private Integer isAiAssisted;

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

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }

  public Long getAuthorId() {
    return authorId;
  }

  public void setAuthorId(Long authorId) {
    this.authorId = authorId;
  }

  public String getAuthorNickname() {
    return authorNickname;
  }

  public void setAuthorNickname(String authorNickname) {
    this.authorNickname = authorNickname;
  }

  public String getAuthorAvatar() {
    return authorAvatar;
  }

  public void setAuthorAvatar(String authorAvatar) {
    this.authorAvatar = authorAvatar;
  }

  public String getPenName() {
    return penName;
  }

  public void setPenName(String penName) {
    this.penName = penName;
  }

  public Integer getSequence() {
    return sequence;
  }

  public void setSequence(Integer sequence) {
    this.sequence = sequence;
  }

  public Integer getLikeCount() {
    return likeCount;
  }

  public void setLikeCount(Integer likeCount) {
    this.likeCount = likeCount;
  }

  public Boolean getIsHot() {
    return isHot;
  }

  public void setIsHot(Boolean isHot) {
    this.isHot = isHot;
  }

  public Boolean getIsKeyPoint() {
    return isKeyPoint;
  }

  public void setIsKeyPoint(Boolean isKeyPoint) {
    this.isKeyPoint = isKeyPoint;
  }

  public Boolean getIsLikedByMe() {
    return isLikedByMe;
  }

  public void setIsLikedByMe(Boolean isLikedByMe) {
    this.isLikedByMe = isLikedByMe;
  }

  public Long getParentId() {
    return parentId;
  }

  public void setParentId(Long parentId) {
    this.parentId = parentId;
  }

  public String getBranchName() {
    return branchName;
  }

  public void setBranchName(String branchName) {
    this.branchName = branchName;
  }

  public LocalDateTime getCreateTime() {
    return createTime;
  }

  public void setCreateTime(LocalDateTime createTime) {
    this.createTime = createTime;
  }

  public Integer getIsAiAssisted() {
    return isAiAssisted;
  }

  public void setIsAiAssisted(Integer isAiAssisted) {
    this.isAiAssisted = isAiAssisted;
  }
}
