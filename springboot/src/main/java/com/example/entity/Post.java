// File: springboot/src/main/java/com/example/entity/Post.java
package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@TableName("post")
public class Post implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String content;
    private String images;
    private String video;
    private String location; // 地理位置
    private Integer category;
    private Integer isAnonymous;
    private Integer visibility;
    private LocalDateTime endTime;
    private BigDecimal reward;
    private LocalDateTime deadline;
    private Integer status;
    private LocalDateTime createTime;
    private Integer viewCount;

    private Integer likeCount;
    private Integer commentCount;
    private Integer shareCount;

    private String pollOptions;
    private LocalDateTime pollEndTime;
    private LocalDateTime scheduledTime;
    private String tags;
    private Integer isAiAssisted;  // 是否经过AI润色：0=否 1=是

    @TableField(exist = false)
    private String nickname;
    @TableField(exist = false)
    private String avatar;
    @TableField(exist = false)
    private String userAccount; // 发帖人账号（管理员可见）
    @TableField(exist = false)
    private List<String> imgList;
    @TableField(exist = false)
    private boolean forceSubmit = false;
    @TableField(exist = false)
    private Boolean isLiked; // 当前用户是否点赞
    @TableField(exist = false)
    private List<String> parsedPoll; // 解析后的投票选项
    @TableField(exist = false)
    private String filteredContent; // 【优化】敏感词过滤后的内容，用于提示用户

    // ================= Getter/Setter =================
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

    public String getImages() {
        return images;
    }

    public void setImages(String images) {
        this.images = images;
    }

    public String getVideo() {
        return video;
    }

    public void setVideo(String video) {
        this.video = video;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getCategory() {
        return category;
    }

    public void setCategory(Integer category) {
        this.category = category;
    }

    public Integer getIsAnonymous() {
        return isAnonymous;
    }

    public void setIsAnonymous(Integer isAnonymous) {
        this.isAnonymous = isAnonymous;
    }

    public Integer getVisibility() {
        return visibility;
    }

    public void setVisibility(Integer visibility) {
        this.visibility = visibility;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public BigDecimal getReward() {
        return reward;
    }

    public void setReward(BigDecimal reward) {
        this.reward = reward;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public Integer getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(Integer commentCount) {
        this.commentCount = commentCount;
    }

    public Integer getShareCount() {
        return shareCount;
    }

    public void setShareCount(Integer shareCount) {
        this.shareCount = shareCount;
    }

    public String getPollOptions() {
        return pollOptions;
    }

    public void setPollOptions(String pollOptions) {
        this.pollOptions = pollOptions;
    }

    public LocalDateTime getPollEndTime() {
        return pollEndTime;
    }

    public void setPollEndTime(LocalDateTime pollEndTime) {
        this.pollEndTime = pollEndTime;
    }

    public LocalDateTime getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(LocalDateTime scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public Integer getIsAiAssisted() {
        return isAiAssisted;
    }

    public void setIsAiAssisted(Integer isAiAssisted) {
        this.isAiAssisted = isAiAssisted;
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

    public List<String> getImgList() {
        return imgList;
    }

    public void setImgList(List<String> imgList) {
        this.imgList = imgList;
    }

    public boolean isForceSubmit() {
        return forceSubmit;
    }

    public void setForceSubmit(boolean forceSubmit) {
        this.forceSubmit = forceSubmit;
    }

    public Boolean getIsLiked() {
        return isLiked;
    }

    public void setIsLiked(Boolean isLiked) {
        this.isLiked = isLiked;
    }

    public List<String> getParsedPoll() {
        return parsedPoll;
    }

    public void setParsedPoll(List<String> parsedPoll) {
        this.parsedPoll = parsedPoll;
    }

    public String getFilteredContent() {
        return filteredContent;
    }

    public void setFilteredContent(String filteredContent) {
        this.filteredContent = filteredContent;
    }

    public String getUserAccount() {
        return userAccount;
    }

    public void setUserAccount(String userAccount) {
        this.userAccount = userAccount;
    }
}