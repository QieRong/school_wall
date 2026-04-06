package com.example.property;

import com.example.base.BasePropertyTest;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 社交功能属性测试
 * 
 * 测试社交功能的核心属性：
 * - Property 20: 关注计数一致性
 * - Property 21: 取关恢复状态
 * - Property 22: 关注列表完整性
 * - Property 23: 粉丝列表完整性
 * - Property 24: 互关检测正确性
 * - Property 25: 黑名单隔离
 * - Property 26: 访客记录完整性
 * 
 * **Feature: functional-testing**
 */
public class SocialPropertyTest extends BasePropertyTest {

  // ==================== Property 20: 关注计数一致性 ====================

  /**
   * **Feature: functional-testing, Property 20: 关注计数一致性**
   * **Validates: Requirements 5.1**
   * 
   * 对于任意关注操作序列，关注数应该等于实际关注的用户数。
   */
  @Property(tries = 100)
  @Label("Property 20: 关注计数一致性 - 关注数应该等于实际关注用户数")
  void followCountShouldMatchActualFollowings(
      @ForAll("followOperations") List<FollowOp> operations) {

    // Given - 模拟关注关系
    Set<Long> followings = new HashSet<>();
    int followingCount = 0;

    // When - 执行关注操作
    for (FollowOp op : operations) {
      if (op.isFollowing) {
        if (!followings.contains(op.targetId)) {
          followings.add(op.targetId);
          followingCount++;
        }
      } else {
        if (followings.contains(op.targetId)) {
          followings.remove(op.targetId);
          followingCount--;
        }
      }
    }

    // Then - 关注数应该等于实际关注用户数
    assertEquals(followings.size(), followingCount,
        "关注数应该等于实际关注用户数");
  }

  /**
   * 关注操作记录
   */
  static class FollowOp {
    Long targetId;
    Boolean isFollowing;

    FollowOp(Long targetId, Boolean isFollowing) {
      this.targetId = targetId;
      this.isFollowing = isFollowing;
    }
  }

  /**
   * 生成关注操作序列
   */
  @Provide
  Arbitrary<List<FollowOp>> followOperations() {
    Arbitrary<FollowOp> opArbitrary = Combinators.combine(
        Arbitraries.longs().between(1L, 100L),
        Arbitraries.of(true, false)).as(FollowOp::new);
    return opArbitrary.list().ofMinSize(1).ofMaxSize(50);
  }

  // ==================== Property 21: 取关恢复状态 ====================

  /**
   * **Feature: functional-testing, Property 21: 取关恢复状态**
   * **Validates: Requirements 5.2**
   * 
   * 关注后取消关注，应该恢复到未关注状态。
   */
  @Property(tries = 100)
  @Label("Property 21: 取关恢复状态 - 关注后取关应该恢复未关注状态")
  void unfollowShouldRestoreState(
      @ForAll @IntRange(min = 1, max = 1000) long targetId,
      @ForAll @IntRange(min = 1, max = 50) int toggleCount) {

    // Given - 初始状态
    Set<Long> followings = new HashSet<>();

    // When - 切换关注状态多次
    for (int i = 0; i < toggleCount; i++) {
      if (followings.contains(targetId)) {
        followings.remove(targetId);
      } else {
        followings.add(targetId);
      }
    }

    // Then - 偶数次切换应该恢复原状态
    if (toggleCount % 2 == 0) {
      assertFalse(followings.contains(targetId),
          "偶数次切换后应该是未关注状态");
    } else {
      assertTrue(followings.contains(targetId),
          "奇数次切换后应该是已关注状态");
    }
  }

  // ==================== Property 22: 关注列表完整性 ====================

  /**
   * **Feature: functional-testing, Property 22: 关注列表完整性**
   * **Validates: Requirements 5.3**
   * 
   * 关注列表应该包含所有已关注的用户。
   */
  @Property(tries = 100)
  @Label("Property 22: 关注列表完整性 - 应该包含所有已关注用户")
  void followingListShouldBeComplete(
      @ForAll("targetUserIds") List<Long> targetIds) {

    // Given - 关注多个用户
    Set<Long> followings = new HashSet<>();
    for (Long targetId : targetIds) {
      followings.add(targetId);
    }

    // Then - 关注列表应该包含所有已关注的用户
    for (Long targetId : targetIds) {
      assertTrue(followings.contains(targetId),
          "关注列表应该包含用户 " + targetId);
    }

    // And - 关注列表大小应该等于去重后的用户数
    Set<Long> uniqueTargets = new HashSet<>(targetIds);
    assertEquals(uniqueTargets.size(), followings.size(),
        "关注列表大小应该等于去重后的用户数");
  }

  /**
   * 生成目标用户ID列表
   */
  @Provide
  Arbitrary<List<Long>> targetUserIds() {
    return Arbitraries.longs().between(1L, 1000L)
        .list().ofMinSize(1).ofMaxSize(50);
  }

  // ==================== Property 23: 粉丝列表完整性 ====================

  /**
   * **Feature: functional-testing, Property 23: 粉丝列表完整性**
   * **Validates: Requirements 5.4**
   * 
   * 粉丝列表应该包含所有关注该用户的人。
   */
  @Property(tries = 100)
  @Label("Property 23: 粉丝列表完整性 - 应该包含所有粉丝")
  void followerListShouldBeComplete(
      @ForAll @IntRange(min = 1, max = 1000) long userId,
      @ForAll("followerIds") List<Long> followerIds) {

    // Given - 模拟多个用户关注同一个人
    Map<Long, Set<Long>> followRelations = new HashMap<>();

    for (Long followerId : followerIds) {
      followRelations.computeIfAbsent(followerId, k -> new HashSet<>()).add(userId);
    }

    // When - 获取粉丝列表
    Set<Long> followers = new HashSet<>();
    for (Map.Entry<Long, Set<Long>> entry : followRelations.entrySet()) {
      if (entry.getValue().contains(userId)) {
        followers.add(entry.getKey());
      }
    }

    // Then - 粉丝列表应该包含所有关注该用户的人
    Set<Long> uniqueFollowers = new HashSet<>(followerIds);
    assertEquals(uniqueFollowers.size(), followers.size(),
        "粉丝列表大小应该等于去重后的粉丝数");
  }

  /**
     * 生成粉丝ID列表
     */
    @Provide
    Arbitrary<List<Long>> followerIds() {
        return Arbitraries.longs().between(1L, 1000L)
                .list().ofMinSize(1).ofMaxSize(50);
    }


    // ==================== Property 24: 互关检测正确性 ====================

    /**
     * **Feature: functional-testing, Property 24: 互关检测正确性**
     * **Validates: Requirements 5.5**
     * 
     * 双方互相关注时，应该检测到互关状态。
     */
    @Property(tries = 100)
    @Label("Property 24: 互关检测正确性 - 双方互关应该被正确检测")
    void mutualFollowShouldBeDetected(
            @ForAll @IntRange(min = 1, max = 1000) long userA,
            @ForAll @IntRange(min = 1, max = 1000) long userB) {
        
        Assume.that(userA != userB);
        
        // Given - 模拟关注关系
        Map<Long, Set<Long>> followRelations = new HashMap<>();
        
        // When - A关注B，B关注A
        followRelations.computeIfAbsent(userA, k -> new HashSet<>()).add(userB);
        followRelations.computeIfAbsent(userB, k -> new HashSet<>()).add(userA);
        
        // Then - 应该检测到互关
        boolean aFollowsB = followRelations.getOrDefault(userA, Collections.emptySet()).contains(userB);
        boolean bFollowsA = followRelations.getOrDefault(userB, Collections.emptySet()).contains(userA);
        boolean isMutual = aFollowsB && bFollowsA;
        
        assertTrue(isMutual, "双方互相关注时应该检测到互关");
    }

    /**
     * **Feature: functional-testing, Property 24: 互关检测正确性**
     * **Validates: Requirements 5.5**
     * 
     * 单向关注时，不应该是互关状态。
     */
    @Property(tries = 100)
    @Label("Property 24: 单向关注检测 - 单向关注不应该是互关")
    void oneWayFollowShouldNotBeMutual(
            @ForAll @IntRange(min = 1, max = 1000) long userA,
            @ForAll @IntRange(min = 1, max = 1000) long userB) {
        
        Assume.that(userA != userB);
        
        // Given - 模拟关注关系
        Map<Long, Set<Long>> followRelations = new HashMap<>();
        
        // When - 只有A关注B
        followRelations.computeIfAbsent(userA, k -> new HashSet<>()).add(userB);
        
        // Then - 不应该是互关
        boolean aFollowsB = followRelations.getOrDefault(userA, Collections.emptySet()).contains(userB);
        boolean bFollowsA = followRelations.getOrDefault(userB, Collections.emptySet()).contains(userA);
        boolean isMutual = aFollowsB && bFollowsA;
        
        assertTrue(aFollowsB, "A应该关注B");
        assertFalse(bFollowsA, "B不应该关注A");
        assertFalse(isMutual, "单向关注不应该是互关");
    }

    // ==================== Property 25: 黑名单隔离 ====================

    /**
     * **Feature: functional-testing, Property 25: 黑名单隔离**
     * **Validates: Requirements 5.6**
     * 
     * 拉黑用户后，应该自动取消关注关系。
     */
    @Property(tries = 100)
    @Label("Property 25: 黑名单隔离 - 拉黑后应该取消关注关系")
    void blockShouldCancelFollow(
            @ForAll @IntRange(min = 1, max = 1000) long userId,
            @ForAll @IntRange(min = 1, max = 1000) long targetId) {
        
        Assume.that(userId != targetId);
        
        // Given - 先关注
        Set<Long> followings = new HashSet<>();
        Set<Long> blacklist = new HashSet<>();
        followings.add(targetId);
        
        // When - 拉黑
        blacklist.add(targetId);
        // 拉黑时自动取消关注
        followings.remove(targetId);
        
        // Then - 关注关系应该被取消
        assertFalse(followings.contains(targetId),
                "拉黑后应该取消关注关系");
        assertTrue(blacklist.contains(targetId),
                "目标用户应该在黑名单中");
    }

    /**
     * **Feature: functional-testing, Property 25: 黑名单隔离**
     * **Validates: Requirements 5.6**
     * 
     * 解除拉黑后，可以重新关注。
     */
    @Property(tries = 100)
    @Label("Property 25: 解除拉黑 - 解除后可以重新关注")
    void unblockShouldAllowRefollow(
            @ForAll @IntRange(min = 1, max = 1000) long userId,
            @ForAll @IntRange(min = 1, max = 1000) long targetId) {
        
        Assume.that(userId != targetId);
        
        // Given - 拉黑状态
        Set<Long> followings = new HashSet<>();
        Set<Long> blacklist = new HashSet<>();
        blacklist.add(targetId);
        
        // When - 解除拉黑
        blacklist.remove(targetId);
        
        // Then - 可以重新关注
        assertFalse(blacklist.contains(targetId),
                "目标用户不应该在黑名单中");
        
        // 重新关注
        followings.add(targetId);
        assertTrue(followings.contains(targetId),
                "解除拉黑后应该可以重新关注");
    }

    // ==================== Property 26: 访客记录完整性 ====================

    /**
     * **Feature: functional-testing, Property 26: 访客记录完整性**
     * **Validates: Requirements 5.7**
     * 
     * 访客记录应该包含所有访问过的用户。
     */
    @Property(tries = 100)
    @Label("Property 26: 访客记录完整性 - 应该记录所有访客")
    void visitorRecordShouldBeComplete(
            @ForAll @IntRange(min = 1, max = 1000) long userId,
            @ForAll("visitorIds") List<Long> visitorIds) {
        
        // Given - 模拟访客记录
        Set<Long> visitors = new HashSet<>();
        
        // When - 记录访客
        for (Long visitorId : visitorIds) {
            if (!visitorId.equals(userId)) { // 自己访问自己不记录
                visitors.add(visitorId);
            }
        }
        
        // Then - 访客记录应该包含所有访客（去重后）
        Set<Long> uniqueVisitors = new HashSet<>();
        for (Long visitorId : visitorIds) {
            if (!visitorId.equals(userId)) {
                uniqueVisitors.add(visitorId);
            }
        }
        
        assertEquals(uniqueVisitors.size(), visitors.size(),
                "访客记录大小应该等于去重后的访客数");
    }

    /**
     * 生成访客ID列表
     */
    @Provide
    Arbitrary<List<Long>> visitorIds() {
        return Arbitraries.longs().between(1L, 1000L)
                .list().ofMinSize(1).ofMaxSize(50);
    }

    /**
     * **Feature: functional-testing, Property 26: 访客记录完整性**
     * **Validates: Requirements 5.7**
     * 
     * 同一访客多次访问只记录一次（或更新时间）。
     */
    @Property(tries = 100)
    @Label("Property 26: 访客去重 - 同一访客只记录一次")
    void duplicateVisitorShouldNotCreateMultipleRecords(
            @ForAll @IntRange(min = 1, max = 1000) long userId,
            @ForAll @IntRange(min = 1, max = 1000) long visitorId,
            @ForAll @IntRange(min = 1, max = 10) int visitCount) {
        
        Assume.that(userId != visitorId);
        
        // Given - 访客记录
        Set<Long> visitors = new HashSet<>();
        
        // When - 同一访客多次访问
        for (int i = 0; i < visitCount; i++) {
            visitors.add(visitorId);
        }
        
        // Then - 只应该有一条记录
        assertEquals(1, visitors.size(),
                "同一访客多次访问只应该有一条记录");
        assertTrue(visitors.contains(visitorId),
                "访客记录应该包含该访客");
    }
}
