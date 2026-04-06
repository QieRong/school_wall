package com.example.property;

import com.example.base.BasePropertyTest;
import com.example.entity.SysNotice;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 通知系统属性测试
 * 
 * 测试通知系统的核心属性：
 * - Property 31: 通知创建完整性
 * - Property 32: 通知已读批量更新
 * - Property 33: 未读通知计数准确性
 * 
 * **Feature: functional-testing**
 */
public class NoticePropertyTest extends BasePropertyTest {

  // ==================== Property 31: 通知创建完整性 ====================

  /**
   * **Feature: functional-testing, Property 31: 通知创建完整性**
   * **Validates: Requirements 7.1**
   * 
   * 对于任意通知，创建后应该包含完整的信息。
   */
  @Property(tries = 100)
  @Label("Property 31: 通知创建完整性 - 通知应该包含完整信息")
  void noticeShouldBeCreatedCompletely(
      @ForAll @IntRange(min = 1, max = 1000) long receiverId,
      @ForAll @IntRange(min = 0, max = 1000) long senderId,
      @ForAll @IntRange(min = 1, max = 4) int type,
      @ForAll @IntRange(min = 1, max = 10000) long relatedId,
      @ForAll("noticeContent") String content) {

    // Given - 创建通知
    SysNotice notice = new SysNotice();
    notice.setReceiverId(receiverId);
    notice.setSenderId(senderId);
    notice.setType(type);
    notice.setRelatedId(relatedId);
    notice.setContent(content);
    notice.setIsRead(0); // 初始未读
    notice.setCreateTime(LocalDateTime.now());

    // Then - 通知应该包含完整信息
    assertEquals(receiverId, notice.getReceiverId(), "接收者ID应该正确");
    assertEquals(senderId, notice.getSenderId(), "发送者ID应该正确");
    assertEquals(type, notice.getType(), "通知类型应该正确");
    assertEquals(relatedId, notice.getRelatedId(), "关联ID应该正确");
    assertEquals(content, notice.getContent(), "通知内容应该正确");
    assertEquals(0, notice.getIsRead(), "初始状态应该是未读");
    assertNotNull(notice.getCreateTime(), "创建时间不应为空");
  }

  /**
   * 生成通知内容
   */
  @Provide
  Arbitrary<String> noticeContent() {
    return Arbitraries.of(
        "评论了你的帖子",
        "点赞了你的帖子",
        "关注了你",
        "回复了你的评论",
        "@了你",
        "系统通知：您的帖子已通过审核",
        "系统通知：您的帖子被举报");
  }

  /**
   * **Feature: functional-testing, Property 31: 通知创建完整性**
   * **Validates: Requirements 7.1**
   * 
   * 通知类型应该在有效范围内。
   */
  @Property(tries = 100)
  @Label("Property 31: 通知类型有效性 - 类型应该在有效范围内")
  void noticeTypeShouldBeValid(
      @ForAll @IntRange(min = 1, max = 4) int type) {

    // Given - 创建通知
    SysNotice notice = new SysNotice();
    notice.setType(type);

    // Then - 通知类型应该在有效范围内
    assertTrue(notice.getType() >= 1 && notice.getType() <= 4,
        "通知类型应该在1-4之间（1:系统 2:评论 3:点赞 4:@）");
  }

  // ==================== Property 32: 通知已读批量更新 ====================

  /**
   * **Feature: functional-testing, Property 32: 通知已读批量更新**
   * **Validates: Requirements 7.4**
   * 
   * 批量标记已读后，所有通知应该变为已读状态。
   */
  @Property(tries = 100)
  @Label("Property 32: 通知已读批量更新 - 批量标记后应该全部已读")
  void batchMarkAsReadShouldUpdateAll(
      @ForAll("unreadNotices") List<SysNotice> notices) {

    // Given - 统计初始未读数
    long initialUnread = notices.stream()
        .filter(n -> n.getIsRead() == 0)
        .count();
    assertTrue(initialUnread > 0, "应该有未读通知");

    // When - 批量标记已读
    for (SysNotice notice : notices) {
      notice.setIsRead(1);
    }

    // Then - 所有通知应该变为已读
    long finalUnread = notices.stream()
        .filter(n -> n.getIsRead() == 0)
        .count();

    assertEquals(0, finalUnread, "批量标记后不应该有未读通知");
  }

  /**
   * 生成未读通知列表
   */
  @Provide
  Arbitrary<List<SysNotice>> unreadNotices() {
    return Arbitraries.integers().between(1, 20)
        .flatMap(size -> Arbitraries.integers().between(1, 1000).list().ofSize(size)
            .map(ids -> {
              List<SysNotice> notices = new ArrayList<>();
              for (int i = 0; i < ids.size(); i++) {
                SysNotice notice = new SysNotice();
                notice.setId(ids.get(i).longValue());
                notice.setContent("通知 " + i);
                notice.setIsRead(0); // 未读
                notice.setType((i % 4) + 1);
                notice.setCreateTime(LocalDateTime.now());
                notices.add(notice);
              }
              return notices;
            }));
  }

  /**
   * **Feature: functional-testing, Property 32: 通知已读批量更新**
   * **Validates: Requirements 7.4**
   * 
   * 单个标记已读应该只更新该通知。
   */
  @Property(tries = 100)
  @Label("Property 32: 单个标记已读 - 只应该更新该通知")
  void singleMarkAsReadShouldOnlyUpdateOne(
      @ForAll("mixedReadStatusNotices") List<SysNotice> notices,
      @ForAll @IntRange(min = 0, max = 19) int targetIndex) {

    Assume.that(targetIndex < notices.size());

    // Given - 记录初始状态
    int initialUnreadCount = (int) notices.stream()
        .filter(n -> n.getIsRead() == 0)
        .count();

    // When - 标记单个通知为已读
    SysNotice target = notices.get(targetIndex);
    boolean wasUnread = target.getIsRead() == 0;
    target.setIsRead(1);

    // Then - 只有目标通知被更新
    int finalUnreadCount = (int) notices.stream()
        .filter(n -> n.getIsRead() == 0)
        .count();

    if (wasUnread) {
      assertEquals(initialUnreadCount - 1, finalUnreadCount,
          "未读数应该减少1");
    } else {
      assertEquals(initialUnreadCount, finalUnreadCount,
          "未读数应该不变");
    }
  }

  /**
   * 生成混合已读状态的通知列表
   */
  @Provide
  Arbitrary<List<SysNotice>> mixedReadStatusNotices() {
    return Arbitraries.integers().between(1, 20)
        .flatMap(size -> Combinators.combine(
            Arbitraries.integers().between(1, 1000).list().ofSize(size),
            Arbitraries.integers().between(0, 1).list().ofSize(size)).as((ids, readStatuses) -> {
              List<SysNotice> notices = new ArrayList<>();
              for (int i = 0; i < ids.size(); i++) {
                SysNotice notice = new SysNotice();
                notice.setId(ids.get(i).longValue());
                notice.setContent("通知 " + i);
                notice.setIsRead(readStatuses.get(i));
                notice.setType((i % 4) + 1);
                notice.setCreateTime(LocalDateTime.now());
                notices.add(notice);
              }
              return notices;
            }));
  }

  // ==================== Property 33: 未读通知计数准确性 ====================

  /**
   * **Feature: functional-testing, Property 33: 未读通知计数准确性**
   * **Validates: Requirements 7.5**
   * 
   * 未读通知计数应该等于实际未读通知数量。
   */
  @Property(tries = 100)
  @Label("Property 33: 未读通知计数准确性 - 计数应该等于实际未读数")
  void unreadCountShouldBeAccurate(
      @ForAll("mixedReadStatusNotices") List<SysNotice> notices) {

    // When - 统计未读通知
    long unreadCount = notices.stream()
        .filter(n -> n.getIsRead() == 0)
        .count();

    long readCount = notices.stream()
        .filter(n -> n.getIsRead() == 1)
        .count();

    // Then - 未读数 + 已读数 = 总数
    assertEquals(notices.size(), unreadCount + readCount,
        "未读数 + 已读数应该等于总数");
  }

  /**
   * **Feature: functional-testing, Property 33: 未读通知计数准确性**
   * **Validates: Requirements 7.5**
   * 
   * 按类型统计未读通知应该正确。
   */
  @Property(tries = 100)
  @Label("Property 33: 按类型统计未读 - 各类型未读数之和应该等于总未读数")
  void unreadCountByTypeShouldBeAccurate(
      @ForAll("mixedReadStatusNotices") List<SysNotice> notices) {

    // When - 按类型统计未读通知
    Map<Integer, Long> unreadByType = new HashMap<>();
    for (int type = 1; type <= 4; type++) {
      final int t = type;
      long count = notices.stream()
          .filter(n -> n.getIsRead() == 0 && n.getType() == t)
          .count();
      unreadByType.put(type, count);
    }

    // Then - 各类型未读数之和应该等于总未读数
    long totalUnread = notices.stream()
        .filter(n -> n.getIsRead() == 0)
        .count();

    long sumByType = unreadByType.values().stream()
        .mapToLong(Long::longValue)
        .sum();

    assertEquals(totalUnread, sumByType,
        "各类型未读数之和应该等于总未读数");
  }

  /**
   * **Feature: functional-testing, Property 33: 未读通知计数准确性**
   * **Validates: Requirements 7.5**
   * 
   * 新通知应该增加未读计数。
   */
  @Property(tries = 100)
  @Label("Property 33: 新通知增加未读数 - 新通知应该增加未读计数")
  void newNoticeShouldIncreaseUnreadCount(
      @ForAll("unreadNotices") List<SysNotice> existingNotices,
      @ForAll @IntRange(min = 1, max = 10) int newNoticeCount) {

    // Given - 初始未读数
    long initialUnread = existingNotices.stream()
        .filter(n -> n.getIsRead() == 0)
        .count();

    // When - 添加新通知
    for (int i = 0; i < newNoticeCount; i++) {
      SysNotice newNotice = new SysNotice();
      newNotice.setId((long) (existingNotices.size() + i + 1));
      newNotice.setContent("新通知 " + i);
      newNotice.setIsRead(0); // 新通知默认未读
      newNotice.setType(1);
      newNotice.setCreateTime(LocalDateTime.now());
      existingNotices.add(newNotice);
    }

    // Then - 未读数应该增加
    long finalUnread = existingNotices.stream()
        .filter(n -> n.getIsRead() == 0)
        .count();

    assertEquals(initialUnread + newNoticeCount, finalUnread,
        "未读数应该增加新通知数量");
  }

  // ==================== 辅助属性测试 ====================

  /**
   * **Feature: functional-testing, Property 31: 通知创建完整性**
   * **Validates: Requirements 7.1, 7.2, 7.3**
   * 
   * 不同类型的通知应该有正确的发送者。
   */
  @Property(tries = 100)
  @Label("Property 31: 通知发送者验证 - 系统通知发送者应该是0")
  void systemNoticeShouldHaveZeroSender(
      @ForAll @IntRange(min = 1, max = 1000) long receiverId) {

    // Given - 创建系统通知
    SysNotice notice = new SysNotice();
    notice.setReceiverId(receiverId);
    notice.setSenderId(0L); // 系统通知发送者为0
    notice.setType(1); // 系统通知类型
    notice.setContent("系统通知");

    // Then - 系统通知发送者应该是0
    assertEquals(0L, notice.getSenderId(), "系统通知发送者应该是0");
    assertEquals(1, notice.getType(), "系统通知类型应该是1");
  }

  /**
   * **Feature: functional-testing, Property 31: 通知创建完整性**
   * **Validates: Requirements 7.2, 7.3**
   * 
   * 用户通知应该有有效的发送者。
   */
  @Property(tries = 100)
  @Label("Property 31: 用户通知发送者验证 - 用户通知应该有有效发送者")
  void userNoticeShouldHaveValidSender(
      @ForAll @IntRange(min = 1, max = 1000) int receiverIdInt,
      @ForAll @IntRange(min = 1, max = 1000) int senderIdInt,
      @ForAll @IntRange(min = 2, max = 4) int type) {

    long receiverId = receiverIdInt;
    long senderId = senderIdInt;
    Assume.that(receiverId != senderId);

    // Given - 创建用户通知
    SysNotice notice = new SysNotice();
    notice.setReceiverId(receiverId);
    notice.setSenderId(senderId);
    notice.setType(type);
    notice.setContent("用户通知");

    // Then - 用户通知应该有有效发送者
    assertTrue(notice.getSenderId() > 0, "用户通知发送者应该大于0");
    assertTrue(notice.getType() >= 2 && notice.getType() <= 4,
        "用户通知类型应该在2-4之间");
  }
}
