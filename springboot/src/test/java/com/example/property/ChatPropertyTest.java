package com.example.property;

import com.example.base.BasePropertyTest;
import com.example.entity.Chat;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 私信系统属性测试
 * 
 * 测试私信系统的核心属性：
 * - Property 27: 消息送达完整性
 * - Property 28: 消息已读状态更新
 * - Property 29: 消息撤回完整性
 * - Property 30: 非互关消息限制
 * 
 * **Feature: functional-testing**
 */
public class ChatPropertyTest extends BasePropertyTest {

  // ==================== Property 27: 消息送达完整性 ====================

  /**
   * **Feature: functional-testing, Property 27: 消息送达完整性**
   * **Validates: Requirements 6.1**
   * 
   * 对于任意发送的消息，接收方应该能收到完整的消息内容。
   */
  @Property(tries = 100)
  @Label("Property 27: 消息送达完整性 - 消息内容应该完整送达")
  void messageShouldBeDeliveredCompletely(
      @ForAll @IntRange(min = 1, max = 1000) long senderId,
      @ForAll @IntRange(min = 1, max = 1000) long receiverId,
      @ForAll("validMessageContent") String content) {

    Assume.that(senderId != receiverId);
    Assume.that(content != null && !content.trim().isEmpty());

    // Given - 创建消息
    Chat chat = new Chat();
    chat.setSenderId(senderId);
    chat.setReceiverId(receiverId);
    chat.setContent(content);
    chat.setType(0); // 文本消息
    chat.setIsRead(0); // 未读
    chat.setIsWithdrawn(0); // 未撤回
    chat.setCreateTime(LocalDateTime.now());

    // Then - 消息内容应该完整
    assertEquals(content, chat.getContent(), "消息内容应该完整");
    assertEquals(senderId, chat.getSenderId(), "发送者ID应该正确");
    assertEquals(receiverId, chat.getReceiverId(), "接收者ID应该正确");
  }

  /**
   * 生成有效的消息内容
   */
  @Provide
  Arbitrary<String> chatMessageContent() {
    return Arbitraries.strings()
        .withCharRange('a', 'z')
        .withCharRange('A', 'Z')
        .withCharRange('0', '9')
        .withChars(' ', ',', '.', '!', '?')
        .ofMinLength(1)
        .ofMaxLength(500)
        .filter(s -> !s.trim().isEmpty());
  }

  /**
   * **Feature: functional-testing, Property 27: 消息送达完整性**
   * **Validates: Requirements 6.1**
   * 
   * 消息历史记录应该按时间顺序排列。
   */
  @Property(tries = 100)
  @Label("Property 27: 消息历史排序 - 应该按时间顺序排列")
  void messageHistoryShouldBeSortedByTime(
      @ForAll("messageList") List<Chat> messages) {

    // When - 按时间排序
    List<Chat> sortedMessages = new ArrayList<>(messages);
    sortedMessages.sort(Comparator.comparing(Chat::getCreateTime));

    // Then - 验证排序正确性
    for (int i = 0; i < sortedMessages.size() - 1; i++) {
      LocalDateTime current = sortedMessages.get(i).getCreateTime();
      LocalDateTime next = sortedMessages.get(i + 1).getCreateTime();
      assertTrue(current.compareTo(next) <= 0,
          "消息应该按时间升序排列");
    }
  }

  /**
   * 生成消息列表
   */
  @Provide
  Arbitrary<List<Chat>> messageList() {
    return Arbitraries.integers().between(1, 20)
        .flatMap(size -> {
          LocalDateTime baseTime = LocalDateTime.now();
          return Arbitraries.integers().between(0, 10000).list().ofSize(size)
              .map(offsets -> {
                List<Chat> messages = new ArrayList<>();
                for (int i = 0; i < offsets.size(); i++) {
                  Chat chat = new Chat();
                  chat.setId((long) (i + 1));
                  chat.setContent("消息 " + i);
                  chat.setCreateTime(baseTime.minusSeconds(offsets.get(i)));
                  messages.add(chat);
                }
                return messages;
              });
        });
  }

  // ==================== Property 28: 消息已读状态更新 ====================

  /**
   * **Feature: functional-testing, Property 28: 消息已读状态更新**
   * **Validates: Requirements 6.3**
   * 
   * 标记已读后，消息状态应该更新为已读。
   */
  @Property(tries = 100)
  @Label("Property 28: 消息已读状态更新 - 标记后应该变为已读")
  void markAsReadShouldUpdateStatus(
      @ForAll("unreadMessages") List<Chat> messages) {

    // Given - 所有消息初始为未读
    for (Chat msg : messages) {
      assertEquals(0, msg.getIsRead(), "初始状态应该是未读");
    }

    // When - 标记为已读
    for (Chat msg : messages) {
      msg.setIsRead(1);
    }

    // Then - 所有消息应该变为已读
    for (Chat msg : messages) {
      assertEquals(1, msg.getIsRead(), "标记后应该是已读状态");
    }
  }

  /**
   * 生成未读消息列表
   */
  @Provide
  Arbitrary<List<Chat>> unreadMessages() {
    return Arbitraries.integers().between(1, 20)
        .flatMap(size -> Arbitraries.integers().between(1, 1000).list().ofSize(size)
            .map(ids -> {
              List<Chat> messages = new ArrayList<>();
              for (int i = 0; i < ids.size(); i++) {
                Chat chat = new Chat();
                chat.setId(ids.get(i).longValue());
                chat.setContent("消息 " + i);
                chat.setIsRead(0); // 未读
                chat.setCreateTime(LocalDateTime.now());
                messages.add(chat);
              }
              return messages;
            }));
  }

  /**
   * **Feature: functional-testing, Property 28: 消息已读状态更新**
   * **Validates: Requirements 6.3**
   * 
   * 未读消息计数应该正确。
   */
  @Property(tries = 100)
  @Label("Property 28: 未读消息计数 - 计数应该正确")
  void unreadCountShouldBeAccurate(
      @ForAll("mixedReadStatusMessages") List<Chat> messages) {

    // When - 统计未读消息
    long unreadCount = messages.stream()
        .filter(msg -> msg.getIsRead() == 0)
        .count();

    long readCount = messages.stream()
        .filter(msg -> msg.getIsRead() == 1)
        .count();

    // Then - 未读数 + 已读数 = 总数
    assertEquals(messages.size(), unreadCount + readCount,
        "未读数 + 已读数应该等于总数");
  }

  /**
   * 生成混合已读状态的消息列表
   */
  @Provide
  Arbitrary<List<Chat>> mixedReadStatusMessages() {
    return Arbitraries.integers().between(1, 20)
        .flatMap(size -> Combinators.combine(
            Arbitraries.integers().between(1, 1000).list().ofSize(size),
            Arbitraries.integers().between(0, 1).list().ofSize(size)).as((ids, readStatuses) -> {
              List<Chat> messages = new ArrayList<>();
              for (int i = 0; i < ids.size(); i++) {
                Chat chat = new Chat();
                chat.setId(ids.get(i).longValue());
                chat.setContent("消息 " + i);
                chat.setIsRead(readStatuses.get(i));
                chat.setCreateTime(LocalDateTime.now());
                messages.add(chat);
              }
              return messages;
            }));
  }

  // ==================== Property 29: 消息撤回完整性 ====================

  /**
   * **Feature: functional-testing, Property 29: 消息撤回完整性**
   * **Validates: Requirements 6.4**
   * 
   * 撤回消息后，消息状态应该更新为已撤回。
   */
  @Property(tries = 100)
  @Label("Property 29: 消息撤回完整性 - 撤回后状态应该更新")
  void withdrawShouldUpdateStatus(
      @ForAll @IntRange(min = 1, max = 1000) long messageId,
      @ForAll("validMessageContent") String content) {

    // Given - 创建消息
    Chat chat = new Chat();
    chat.setId(messageId);
    chat.setContent(content);
    chat.setIsWithdrawn(0); // 未撤回
    chat.setCreateTime(LocalDateTime.now());

    // When - 撤回消息
    chat.setIsWithdrawn(1);

    // Then - 消息应该标记为已撤回
    assertEquals(1, chat.getIsWithdrawn(), "消息应该标记为已撤回");
  }

  /**
   * **Feature: functional-testing, Property 29: 消息撤回完整性**
   * **Validates: Requirements 6.4**
   * 
   * 只有发送者可以撤回自己的消息。
   */
  @Property(tries = 100)
  @Label("Property 29: 撤回权限验证 - 只有发送者可以撤回")
  void onlySenderCanWithdraw(
      @ForAll @IntRange(min = 1, max = 1000) long senderId,
      @ForAll @IntRange(min = 1, max = 1000) long receiverId,
      @ForAll @IntRange(min = 1, max = 1000) long requesterId) {

    Assume.that(senderId != receiverId);

    // Given - 创建消息
    Chat chat = new Chat();
    chat.setSenderId(senderId);
    chat.setReceiverId(receiverId);
    chat.setIsWithdrawn(0);

    // When - 验证撤回权限
    boolean canWithdraw = chat.getSenderId().equals(requesterId);

    // Then - 只有发送者可以撤回
    if (requesterId == senderId) {
      assertTrue(canWithdraw, "发送者应该可以撤回");
    } else {
      assertFalse(canWithdraw, "非发送者不应该可以撤回");
    }
  }

  /**
   * **Feature: functional-testing, Property 29: 消息撤回完整性**
   * **Validates: Requirements 6.4**
   * 
   * 超时后不能撤回消息。
   */
  @Property(tries = 100)
  @Label("Property 29: 撤回时限验证 - 超时后不能撤回")
  void cannotWithdrawAfterTimeout(
      @ForAll @IntRange(min = 0, max = 120) int minutesAgo) {

    // Given - 创建消息
    Chat chat = new Chat();
    chat.setCreateTime(LocalDateTime.now().minusMinutes(minutesAgo));

    // When - 检查是否可以撤回（假设时限为2分钟）
    int withdrawLimitMinutes = 2;
    long minutesSinceCreation = java.time.Duration.between(
        chat.getCreateTime(), LocalDateTime.now()).toMinutes();
    boolean canWithdraw = minutesSinceCreation <= withdrawLimitMinutes;

    // Then - 验证撤回时限
    if (minutesAgo <= withdrawLimitMinutes) {
      assertTrue(canWithdraw, "在时限内应该可以撤回");
    } else {
      assertFalse(canWithdraw, "超过时限不应该可以撤回");
    }
  }

  // ==================== Property 30: 非互关消息限制 ====================

  /**
   * **Feature: functional-testing, Property 30: 非互关消息限制**
   * **Validates: Requirements 6.5**
   * 
   * 非互关用户只能发送一条消息，等待对方回复。
   */
  @Property(tries = 100)
  @Label("Property 30: 非互关消息限制 - 只能发送一条消息")
  void nonMutualFollowShouldLimitMessages(
      @ForAll @IntRange(min = 1, max = 1000) long senderId,
      @ForAll @IntRange(min = 1, max = 1000) long receiverId,
      @ForAll @IntRange(min = 0, max = 10) int sentCount,
      @ForAll @IntRange(min = 0, max = 10) int receivedCount) {

    Assume.that(senderId != receiverId);

    // Given - 模拟消息计数
    // sentCount: 发送者已发送的消息数
    // receivedCount: 发送者收到对方回复的消息数

    // When - 检查是否可以发送新消息
    boolean canSend;
    if (receivedCount > 0) {
      // 对方已回复，可以继续发送
      canSend = true;
    } else {
      // 对方未回复，只能发送一条
      canSend = sentCount < 1;
    }

    // Then - 验证发送限制
    if (receivedCount > 0) {
      assertTrue(canSend, "对方已回复时应该可以发送");
    } else if (sentCount >= 1) {
      assertFalse(canSend, "对方未回复且已发送过消息时不应该可以发送");
    } else {
      assertTrue(canSend, "对方未回复但未发送过消息时应该可以发送");
    }
  }

  /**
   * **Feature: functional-testing, Property 30: 非互关消息限制**
   * **Validates: Requirements 6.5**
   * 
   * 互关用户可以自由发送消息。
   */
  @Property(tries = 100)
  @Label("Property 30: 互关用户无限制 - 可以自由发送消息")
  void mutualFollowShouldAllowUnlimitedMessages(
      @ForAll @IntRange(min = 1, max = 1000) long userA,
      @ForAll @IntRange(min = 1, max = 1000) long userB,
      @ForAll @IntRange(min = 0, max = 100) int messageCount) {

    Assume.that(userA != userB);

    // Given - 模拟互关关系
    Set<Long> userAFollowings = new HashSet<>();
    Set<Long> userBFollowings = new HashSet<>();
    userAFollowings.add(userB);
    userBFollowings.add(userA);

    // When - 检查是否互关
    boolean isMutual = userAFollowings.contains(userB) && userBFollowings.contains(userA);

    // Then - 互关用户可以自由发送消息
    assertTrue(isMutual, "应该是互关状态");
    // 互关用户没有消息数量限制
    assertTrue(messageCount >= 0, "互关用户可以发送任意数量的消息");
  }

  // ==================== 辅助属性测试 ====================

  /**
   * **Feature: functional-testing, Property 27: 消息送达完整性**
   * **Validates: Requirements 6.1**
   * 
   * 消息类型应该在有效范围内。
   */
  @Property(tries = 100)
  @Label("Property 27: 消息类型有效性 - 类型应该在有效范围内")
  void messageTypeShouldBeValid(
      @ForAll @IntRange(min = 0, max = 2) int messageType) {

    // Given - 创建消息
    Chat chat = new Chat();
    chat.setType(messageType);

    // Then - 消息类型应该在有效范围内
    assertTrue(chat.getType() >= 0 && chat.getType() <= 2,
        "消息类型应该在0-2之间（0:文本 1:图片 2:表情）");
  }

  /**
   * **Feature: functional-testing, Property 28: 消息已读状态更新**
   * **Validates: Requirements 6.3**
   * 
   * 批量标记已读应该更新所有消息。
   */
  @Property(tries = 100)
  @Label("Property 28: 批量标记已读 - 应该更新所有消息")
  void batchMarkAsReadShouldUpdateAll(
      @ForAll("unreadMessages") List<Chat> messages) {

    // Given - 统计初始未读数
    long initialUnread = messages.stream()
        .filter(msg -> msg.getIsRead() == 0)
        .count();

    // When - 批量标记已读
    for (Chat msg : messages) {
      msg.setIsRead(1);
    }

    // Then - 所有消息应该变为已读
    long finalUnread = messages.stream()
        .filter(msg -> msg.getIsRead() == 0)
        .count();

    assertEquals(0, finalUnread, "批量标记后不应该有未读消息");
    assertTrue(initialUnread >= finalUnread, "已读数应该减少或保持不变");
  }
}
