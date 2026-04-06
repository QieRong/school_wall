package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 故事段落实体类
 * 对应数据库表 story_paragraph
 */
@TableName("story_paragraph")
public class StoryParagraph implements Serializable {

  private static final long serialVersionUID = 1L;

  @TableId(type = IdType.AUTO)
  private Long id;

  /** 故事ID */
  private Long storyId;

  /** 段落内容 */
  private String content;

  /** 插画URL */
  private String imageUrl;

  /** 作者ID */
  private Long authorId;

  /** 笔名 */
  private String penName;

  /** 段落序号 */
  private Integer sequence;

  /** 点赞数 */
  private Integer likeCount;

  /** 是否热门(点赞>10) */
  private Integer isHot;

  /** 是否关键转折点(点赞>50或作者标记) */
  private Integer isKeyPoint;

  /** 是否作者标记为关键点 */
  private Integer isAuthorMarked;

  /** 父段落ID(用于分支) */
  private Long parentId;

  /** 分支名称 */
  private String branchName;

  /** 是否经过AI润色：0=否 1=是 */
  private Integer isAiAssisted;

  /** 创建时间 */
  private LocalDateTime createTime;

  // ========== 非数据库字段 ==========

  /** 作者昵称 */
  @TableField(exist = false)
  private String authorNickname;

  /** 作者头像 */
  @TableField(exist = false)
  private String authorAvatar;

  /** 当前用户是否点赞 */
  @TableField(exist = false)
  private Boolean isLikedByMe;

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

  public Integer getIsHot() {
    return isHot;
  }

  public void setIsHot(Integer isHot) {
    this.isHot = isHot;
  }

  public Integer getIsKeyPoint() {
    return isKeyPoint;
  }

  public void setIsKeyPoint(Integer isKeyPoint) {
    this.isKeyPoint = isKeyPoint;
  }

  public Integer getIsAuthorMarked() {
    return isAuthorMarked;
  }

  public void setIsAuthorMarked(Integer isAuthorMarked) {
    this.isAuthorMarked = isAuthorMarked;
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

  public Integer getIsAiAssisted() {
    return isAiAssisted;
  }

  public void setIsAiAssisted(Integer isAiAssisted) {
    this.isAiAssisted = isAiAssisted;
  }

  public LocalDateTime getCreateTime() {
    return createTime;
  }

  public void setCreateTime(LocalDateTime createTime) {
    this.createTime = createTime;
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

  public Boolean getIsLikedByMe() {
    return isLikedByMe;
  }

  public void setIsLikedByMe(Boolean isLikedByMe) {
    this.isLikedByMe = isLikedByMe;
  }
}
