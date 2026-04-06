package com.example.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 故事详情展示VO
 */
public class StoryDetailVO {

    private Long id;
    private String title;
    private Integer category;
    private String categoryName;
    private String worldSetting;
    private String firstParagraph;
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
    private LocalDateTime finishTime;
    private List<ParagraphVO> paragraphs;
    private List<ContributionRankVO> contributionRank;
    private FinishVoteVO currentVote; // 当前进行中的完结投票
    private Boolean isFavorited; // 当前用户是否收藏

    // ========== 锁定状态 ==========
    private Boolean lockedByOther;
    private String lockingUserNickname;
    private Integer lockExpireSeconds;

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

    public String getWorldSetting() {
        return worldSetting;
    }

    public void setWorldSetting(String worldSetting) {
        this.worldSetting = worldSetting;
    }

    public String getFirstParagraph() {
        return firstParagraph;
    }

    public void setFirstParagraph(String firstParagraph) {
        this.firstParagraph = firstParagraph;
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

    public LocalDateTime getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(LocalDateTime finishTime) {
        this.finishTime = finishTime;
    }

    public List<ParagraphVO> getParagraphs() {
        return paragraphs;
    }

    public void setParagraphs(List<ParagraphVO> paragraphs) {
        this.paragraphs = paragraphs;
    }

    public List<ContributionRankVO> getContributionRank() {
        return contributionRank;
    }

    public void setContributionRank(List<ContributionRankVO> contributionRank) {
        this.contributionRank = contributionRank;
    }

    public FinishVoteVO getCurrentVote() {
        return currentVote;
    }

    public void setCurrentVote(FinishVoteVO currentVote) {
        this.currentVote = currentVote;
    }

    public Boolean getIsFavorited() {
        return isFavorited;
    }

    public void setIsFavorited(Boolean isFavorited) {
        this.isFavorited = isFavorited;
    }

    public Boolean getLockedByOther() {
        return lockedByOther;
    }

    public void setLockedByOther(Boolean lockedByOther) {
        this.lockedByOther = lockedByOther;
    }

    public String getLockingUserNickname() {
        return lockingUserNickname;
    }

    public void setLockingUserNickname(String lockingUserNickname) {
        this.lockingUserNickname = lockingUserNickname;
    }

    public Integer getLockExpireSeconds() {
        return lockExpireSeconds;
    }

    public void setLockExpireSeconds(Integer lockExpireSeconds) {
        this.lockExpireSeconds = lockExpireSeconds;
    }
}
