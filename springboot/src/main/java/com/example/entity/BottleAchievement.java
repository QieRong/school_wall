// File: springboot/src/main/java/com/example/entity/BottleAchievement.java
package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 漂流瓶成就实体类
 * 对应数据库表 bottle_achievement
 */
@TableName("bottle_achievement")
public class BottleAchievement implements Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * 成就类型常量
   */
  public static final String TYPE_FIRST_FISH = "FIRST_FISH"; // 初访者(首次打捞)
  public static final String TYPE_FISH_50 = "FISH_50"; // 捕瓶达人(打捞50次)
  public static final String TYPE_REPLY_10 = "REPLY_10"; // 海洋之心(收到10次回复)
  public static final String TYPE_COLLECT_30 = "COLLECT_30"; // 收藏家(珍藏30个瓶子)

  @TableId(type = IdType.AUTO)
  private Long id;

  /** 用户ID */
  private Long userId;

  /** 成就类型 */
  private String achievementType;

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

  public LocalDateTime getCreateTime() {
    return createTime;
  }

  public void setCreateTime(LocalDateTime createTime) {
    this.createTime = createTime;
  }

  /**
   * 获取成就的中文名称
   */
  public String getAchievementName() {
    return switch (this.achievementType) {
      case TYPE_FIRST_FISH -> "初访者";
      case TYPE_FISH_50 -> "捕瓶达人";
      case TYPE_REPLY_10 -> "海洋之心";
      case TYPE_COLLECT_30 -> "收藏家";
      default -> "未知成就";
    };
  }

  /**
   * 获取成就的描述
   */
  public String getAchievementDescription() {
    return switch (this.achievementType) {
      case TYPE_FIRST_FISH -> "完成首次打捞漂流瓶";
      case TYPE_FISH_50 -> "累计打捞50个漂流瓶";
      case TYPE_REPLY_10 -> "投放的漂流瓶收到10次回复";
      case TYPE_COLLECT_30 -> "珍藏30个漂流瓶";
      default -> "";
    };
  }

  /**
   * 获取成就的图标
   */
  public String getAchievementIcon() {
    return switch (this.achievementType) {
      case TYPE_FIRST_FISH -> "🐚";
      case TYPE_FISH_50 -> "🎣";
      case TYPE_REPLY_10 -> "🌊";
      case TYPE_COLLECT_30 -> "🏺";
      default -> "⭐";
    };
  }
}
