package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 故事成就实体类
 * 对应数据库表 story_achievement
 */
@TableName("story_achievement")
public class StoryAchievement implements Serializable {

  private static final long serialVersionUID = 1L;

  /** 成就类型常量 */
  public static final String TYPE_OPENER = "opener"; // 开篇者 - 创建第一个故事
  public static final String TYPE_KEY_PUZZLE = "key_puzzle"; // 关键拼图 - 段落成为关键转折点
  public static final String TYPE_LONG_CREATOR = "long_creator"; // 长篇缔造者 - 单故事贡献10段以上
  public static final String TYPE_STORY_KING = "story_king"; // 故事之王 - 总贡献度第一

  @TableId(type = IdType.AUTO)
  private Long id;

  /** 用户ID */
  private Long userId;

  /** 成就类型 */
  private String achievementType;

  /** 关联故事ID(可选) */
  private Long storyId;

  /** 获得时间 */
  private LocalDateTime createTime;

  // ========== Getter/Setter ==========

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

  public String getAchievementType() {
    return achievementType;
  }

  public void setAchievementType(String achievementType) {
    this.achievementType = achievementType;
  }

  public Long getStoryId() {
    return storyId;
  }

  public void setStoryId(Long storyId) {
    this.storyId = storyId;
  }

  public LocalDateTime getCreateTime() {
    return createTime;
  }

  public void setCreateTime(LocalDateTime createTime) {
    this.createTime = createTime;
  }

  /** 获取成就名称 */
  public String getAchievementName() {
    return switch (this.achievementType) {
      case TYPE_OPENER -> "开篇者";
      case TYPE_KEY_PUZZLE -> "关键拼图";
      case TYPE_LONG_CREATOR -> "长篇缔造者";
      case TYPE_STORY_KING -> "故事之王";
      default -> "未知成就";
    };
  }

  /** 获取成就描述 */
  public String getAchievementDesc() {
    return switch (this.achievementType) {
      case TYPE_OPENER -> "创建第一个故事";
      case TYPE_KEY_PUZZLE -> "段落成为关键转折点";
      case TYPE_LONG_CREATOR -> "单故事贡献10段以上";
      case TYPE_STORY_KING -> "总贡献度排名第一";
      default -> "";
    };
  }

  /** 获取成就图标 */
  public String getAchievementIcon() {
    return switch (this.achievementType) {
      case TYPE_OPENER -> "🖋️";
      case TYPE_KEY_PUZZLE -> "🧩";
      case TYPE_LONG_CREATOR -> "📚";
      case TYPE_STORY_KING -> "👑";
      default -> "🏆";
    };
  }
}
