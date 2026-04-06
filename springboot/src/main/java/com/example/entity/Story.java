package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 故事实体类
 * 对应数据库表 story
 */
@TableName("story")
public class Story implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 故事状态常量 */
    public static final int STATUS_ONGOING = 1;    // 进行中
    public static final int STATUS_FINISHED = 2;   // 已完结
    public static final int STATUS_ARCHIVED = 3;   // 已归档

    /** 故事分类常量 */
    public static final int CATEGORY_FANTASY = 1;      // 奇幻校园
    public static final int CATEGORY_MYSTERY = 2;      // 悬疑推理
    public static final int CATEGORY_ROMANCE = 3;      // 浪漫物语
    public static final int CATEGORY_COMEDY = 4;       // 搞笑日常
    public static final int CATEGORY_SCIFI = 5;        // 科幻未来

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 故事标题 */
    private String title;

    /** 分类: 1奇幻校园 2悬疑推理 3浪漫物语 4搞笑日常 5科幻未来 */
    private Integer category;

    /** 世界观设定 */
    private String worldSetting;

    /** 开篇内容 */
    private String firstParagraph;

    /** 创建者ID */
    private Long creatorId;

    /** 状态: 1进行中 2已完结 3已归档 */
    private Integer status;

    /** 段落数量 */
    private Integer paragraphCount;

    /** 参与人数 */
    private Integer participantCount;

    /** 总点赞数 */
    private Integer totalLikes;

    /** 是否官方推荐 */
    private Integer isRecommended;

    /** 当前完结投票ID */
    private Long finishVoteId;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 完结时间 */
    private LocalDateTime finishTime;

    /** 当前正在编辑的用户ID */
    private Long editingUserId;

    /** 编辑锁过期时间 */
    private LocalDateTime editingExpireTime;

    // ========== 非数据库字段 ==========

    /** 创建者昵称 */
    @TableField(exist = false)
    private String creatorNickname;

    /** 创建者头像 */
    @TableField(exist = false)
    private String creatorAvatar;

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

    public Long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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

    public Integer getIsRecommended() {
        return isRecommended;
    }

    public void setIsRecommended(Integer isRecommended) {
        this.isRecommended = isRecommended;
    }

    public Long getFinishVoteId() {
        return finishVoteId;
    }

    public void setFinishVoteId(Long finishVoteId) {
        this.finishVoteId = finishVoteId;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public LocalDateTime getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(LocalDateTime finishTime) {
        this.finishTime = finishTime;
    }

    public String getCreatorNickname() {
        return creatorNickname;
    }

    public void setCreatorNickname(String creatorNickname) {
        this.creatorNickname = creatorNickname;
    }

    public Long getEditingUserId() {
        return editingUserId;
    }

    public void setEditingUserId(Long editingUserId) {
        this.editingUserId = editingUserId;
    }

    public LocalDateTime getEditingExpireTime() {
        return editingExpireTime;
    }

    public void setEditingExpireTime(LocalDateTime editingExpireTime) {
        this.editingExpireTime = editingExpireTime;
    }

    public String getCreatorAvatar() {
        return creatorAvatar;
    }

    public void setCreatorAvatar(String creatorAvatar) {
        this.creatorAvatar = creatorAvatar;
    }

    /** 获取分类名称 */
    public String getCategoryName() {
        return switch (this.category) {
            case CATEGORY_FANTASY -> "奇幻校园";
            case CATEGORY_MYSTERY -> "悬疑推理";
            case CATEGORY_ROMANCE -> "浪漫物语";
            case CATEGORY_COMEDY -> "搞笑日常";
            case CATEGORY_SCIFI -> "科幻未来";
            default -> "未知";
        };
    }

    /** 获取状态名称 */
    public String getStatusName() {
        return switch (this.status) {
            case STATUS_ONGOING -> "进行中";
            case STATUS_FINISHED -> "已完结";
            case STATUS_ARCHIVED -> "已归档";
            default -> "未知";
        };
    }
}
