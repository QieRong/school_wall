package com.example.common;

/**
 * 系统常量类
 * 统一管理项目中的魔法数字和常量值
 */
public class Constants {

  private Constants() {
    // 私有构造函数，防止实例化
  }

  // ==================== 用户角色 ====================

  /** 普通用户 */
  public static final int ROLE_USER = 0;
  /** 管理员 */
  public static final int ROLE_ADMIN = 1;

  // ==================== 用户状态 ====================

  /** 正常 */
  public static final int USER_STATUS_NORMAL = 1;
  /** 封禁 */
  public static final int USER_STATUS_BANNED = 0;

  // ==================== 帖子状态 ====================

  /** 待审核 */
  public static final int POST_STATUS_PENDING = 0;
  /** 已发布 */
  public static final int POST_STATUS_PUBLISHED = 1;
  /** 定时待发布 */
  public static final int POST_STATUS_SCHEDULED = 2;
  /** 已驳回 */
  public static final int POST_STATUS_REJECTED = 3;

  // ==================== 帖子分类 ====================

  /** 表白 */
  public static final int CATEGORY_CONFESSION = 1;
  /** 寻物 */
  public static final int CATEGORY_LOST_FOUND = 2;
  /** 闲置 */
  public static final int CATEGORY_SECOND_HAND = 3;
  /** 吐槽 */
  public static final int CATEGORY_COMPLAINT = 4;
  /** 活动 */
  public static final int CATEGORY_ACTIVITY = 5;
  /** 求助 */
  public static final int CATEGORY_HELP = 6;
  /** 其他 */
  public static final int CATEGORY_OTHER = 9;

  // ==================== 通知类型 ====================

  /** 系统通知 */
  public static final int NOTICE_TYPE_SYSTEM = 1;
  /** 评论通知 */
  public static final int NOTICE_TYPE_COMMENT = 2;
  /** 点赞通知 */
  public static final int NOTICE_TYPE_LIKE = 3;
  /** 关注通知 */
  public static final int NOTICE_TYPE_FOLLOW = 4;

  // ==================== 举报状态 ====================

  /** 待处理 */
  public static final int REPORT_STATUS_PENDING = 0;
  /** 已处理（有效举报） */
  public static final int REPORT_STATUS_VALID = 1;
  /** 已驳回（无效举报） */
  public static final int REPORT_STATUS_INVALID = 2;

  // ==================== 处罚类型 ====================

  /** 扣分 */
  public static final int PUNISHMENT_TYPE_DEDUCT = 1;
  /** 封号 */
  public static final int PUNISHMENT_TYPE_BAN = 2;
  /** 解封 */
  public static final int PUNISHMENT_TYPE_UNBAN = 3;

  // ==================== 私信类型 ====================

  /** 文本消息 */
  public static final int CHAT_TYPE_TEXT = 0;
  /** 图片消息 */
  public static final int CHAT_TYPE_IMAGE = 1;

  // ==================== 可见性 ====================

  /** 公开可见 */
  public static final int VISIBILITY_PUBLIC = 0;
  /** 仅互关可见 */
  public static final int VISIBILITY_MUTUAL = 1;
  /** 仅自己可见 */
  public static final int VISIBILITY_PRIVATE = 2;

  // ==================== 信誉分相关 ====================

  /** 初始信誉分 */
  public static final int CREDIT_SCORE_INIT = 100;
  /** 发帖最低信誉分 */
  public static final int CREDIT_SCORE_MIN_POST = 60;

  // ==================== 文件大小限制 (字节) ====================

  /** 图片最大 10MB */
  public static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024L;
  /** 视频最大 50MB */
  public static final long MAX_VIDEO_SIZE = 50 * 1024 * 1024L;

  // ==================== 时间限制 ====================

  /** 改名冷却时间 (天) */
  public static final int NICKNAME_COOLDOWN_DAYS = 30;
  /** 消息撤回时限 (分钟) */
  public static final int MESSAGE_WITHDRAW_MINUTES = 2;
  /** 私信防骚扰阈值 (条) */
  public static final int CHAT_SPAM_THRESHOLD = 3;
}
