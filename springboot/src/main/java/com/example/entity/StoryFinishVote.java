package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 完结投票实体类
 * 对应数据库表 story_finish_vote
 */
@TableName("story_finish_vote")
public class StoryFinishVote implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 投票状态常量 */
    public static final int STATUS_ONGOING = 1;   // 进行中
    public static final int STATUS_PASSED = 2;    // 已通过
    public static final int STATUS_REJECTED = 3;  // 已否决
    public static final int STATUS_EXPIRED = 4;   // 已过期

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 故事ID */
    private Long storyId;

    /** 发起者ID */
    private Long initiatorId;

    /** 同意数 */
    private Integer agreeCount;

    /** 反对数 */
    private Integer disagreeCount;

    /** 有投票权的总人数 */
    private Integer totalVoters;

    /** 状态: 1进行中 2已通过 3已否决 4已过期 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 过期时间 */
    private LocalDateTime expireTime;

    // ========== 非数据库字段 ==========

    /** 发起者昵称 */
    @TableField(exist = false)
    private String initiatorNickname;

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

    public String getInitiatorNickname() {
        return initiatorNickname;
    }

    public void setInitiatorNickname(String initiatorNickname) {
        this.initiatorNickname = initiatorNickname;
    }

    /** 获取状态名称 */
    public String getStatusName() {
        return switch (this.status) {
            case STATUS_ONGOING -> "进行中";
            case STATUS_PASSED -> "已通过";
            case STATUS_REJECTED -> "已否决";
            case STATUS_EXPIRED -> "已过期";
            default -> "未知";
        };
    }
}
