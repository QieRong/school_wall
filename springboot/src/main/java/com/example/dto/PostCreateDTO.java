package com.example.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 发帖请求DTO
 */
public class PostCreateDTO {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Size(max = 5000, message = "内容长度不能超过5000字")
    private String content;

    private String images;
    private String video;

    @Size(max = 100, message = "位置信息过长")
    private String location;

    private Integer category;
    private Integer isAnonymous;
    private Integer visibility;
    private LocalDateTime endTime;
    private BigDecimal reward;
    private LocalDateTime deadline;
    private String pollOptions;
    private LocalDateTime pollEndTime;
    private LocalDateTime scheduledTime;
    private String tags;
    private boolean forceSubmit = false;

    private Integer isAiAssisted;

    // Getters and Setters
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

    public boolean isForceSubmit() {
        return forceSubmit;
    }

    public void setForceSubmit(boolean forceSubmit) {
        this.forceSubmit = forceSubmit;
    }

    public Integer getIsAiAssisted() {
        return isAiAssisted;
    }

    public void setIsAiAssisted(Integer isAiAssisted) {
        this.isAiAssisted = isAiAssisted;
    }
}
