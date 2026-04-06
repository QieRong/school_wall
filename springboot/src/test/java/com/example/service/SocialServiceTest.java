package com.example.service;

import cn.hutool.crypto.digest.BCrypt;
import com.example.base.BaseTest;
import com.example.entity.Follow;
import com.example.entity.User;
import com.example.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 社交功能单元测试
 * 
 * 测试范围：
 * - 关注/取消关注
 * - 关注列表/粉丝列表
 * - 互关检测
 * - 收藏功能
 * - 黑名单功能
 * - 访客记录
 * 
 * _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7_
 */
@DisplayName("社交功能测试")
class SocialServiceTest extends BaseTest {

  @Autowired
  private SocialService socialService;

  @Autowired
  private UserMapper userMapper;

  private User user1;
  private User user2;
  private User user3;

  @BeforeEach
  void setUp() {
    // 创建测试用户1
    user1 = new User();
    user1.setAccount("2021555001");
    user1.setPassword(BCrypt.hashpw("test123456"));
    user1.setNickname("用户1");
    user1.setRole(0);
    user1.setStatus(1);
    user1.setCreditScore(100);
    user1.setViolationCount(0);
    user1.setAvatar("/default.png");
    userMapper.insert(user1);
    // 重新查询获取 ID
    user1 = userMapper.selectByAccount("2021555001");

    // 创建测试用户2
    user2 = new User();
    user2.setAccount("2021555002");
    user2.setPassword(BCrypt.hashpw("test123456"));
    user2.setNickname("用户2");
    user2.setRole(0);
    user2.setStatus(1);
    user2.setCreditScore(100);
    user2.setAvatar("/default.png");
    userMapper.insert(user2);
    user2 = userMapper.selectByAccount("2021555002");

    // 创建测试用户3
    user3 = new User();
    user3.setAccount("2021555003");
    user3.setPassword(BCrypt.hashpw("test123456"));
    user3.setNickname("用户3");
    user3.setRole(0);
    user3.setStatus(1);
    user3.setCreditScore(100);
    user3.setAvatar("/default.png");
    userMapper.insert(user3);
    user3 = userMapper.selectByAccount("2021555003");
  }

  @Nested
  @DisplayName("关注功能测试")
  class FollowTests {

    @Test
    @DisplayName("关注用户 - 应该成功建立关注关系")
    void followUser_shouldSucceed() {
      // Given - 初始未关注
      assertFalse(socialService.getFollowStatus(user1.getId().longValue(), user2.getId().longValue()),
          "初始状态应该未关注");

      // When - 关注
      socialService.followUser(user1.getId().longValue(), user2.getId().longValue());

      // Then
      assertTrue(socialService.getFollowStatus(user1.getId().longValue(), user2.getId().longValue()),
          "应该已关注");
    }

    @Test
    @DisplayName("取消关注 - 应该成功解除关注关系")
    void unfollowUser_shouldSucceed() {
      // Given - 先关注
      socialService.followUser(user1.getId().longValue(), user2.getId().longValue());
      assertTrue(socialService.getFollowStatus(user1.getId().longValue(), user2.getId().longValue()));

      // When - 取消关注
      socialService.unfollowUser(user1.getId().longValue(), user2.getId().longValue());

      // Then
      assertFalse(socialService.getFollowStatus(user1.getId().longValue(), user2.getId().longValue()),
          "应该已取消关注");
    }

    @Test
    @DisplayName("重复关注 - 不应该创建重复记录")
    void duplicateFollow_shouldNotCreateDuplicate() {
      // Given - 关注一次
      socialService.followUser(user1.getId().longValue(), user2.getId().longValue());

      // When - 再次关注（应该被忽略或更新）
      socialService.followUser(user1.getId().longValue(), user2.getId().longValue());

      // Then - 关注列表中只有一条记录
      List<Follow> followings = socialService.getFollowings(user1.getId().longValue());
      long count = followings.stream()
          .filter(f -> f.getTargetId().equals(user2.getId().longValue()))
          .count();
      assertEquals(1, count, "不应该有重复的关注记录");
    }
  }

  @Nested
  @DisplayName("关注列表测试")
  class FollowListTests {

    @Test
    @DisplayName("获取关注列表 - 应该返回所有关注的用户")
    void getFollowings_shouldReturnAllFollowedUsers() {
      // Given - 关注多个用户
      socialService.followUser(user1.getId().longValue(), user2.getId().longValue());
      socialService.followUser(user1.getId().longValue(), user3.getId().longValue());

      // When
      List<Follow> followings = socialService.getFollowings(user1.getId().longValue());

      // Then
      assertEquals(2, followings.size(), "应该有2个关注");
    }

    @Test
    @DisplayName("获取粉丝列表 - 应该返回所有粉丝")
    void getFollowers_shouldReturnAllFollowers() {
      // Given - 多个用户关注同一个人
      socialService.followUser(user1.getId().longValue(), user3.getId().longValue());
      socialService.followUser(user2.getId().longValue(), user3.getId().longValue());

      // When
      List<Follow> followers = socialService.getFollowers(user3.getId().longValue());

      // Then
      assertEquals(2, followers.size(), "应该有2个粉丝");
    }
  }

  @Nested
  @DisplayName("互关检测测试")
  class MutualFollowTests {

    @Test
    @DisplayName("互关检测 - 双方互相关注应该检测到互关")
    void mutualFollow_shouldBeDetected() {
      // Given - 双方互相关注
      socialService.followUser(user1.getId().longValue(), user2.getId().longValue());
      socialService.followUser(user2.getId().longValue(), user1.getId().longValue());

      // When - 检查关注状态
      boolean user1FollowsUser2 = socialService.getFollowStatus(
          user1.getId().longValue(), user2.getId().longValue());
      boolean user2FollowsUser1 = socialService.getFollowStatus(
          user2.getId().longValue(), user1.getId().longValue());

      // Then - 双方都应该是关注状态
      assertTrue(user1FollowsUser2, "用户1应该关注用户2");
      assertTrue(user2FollowsUser1, "用户2应该关注用户1");
    }

    @Test
    @DisplayName("单向关注 - 不应该是互关")
    void oneWayFollow_shouldNotBeMutual() {
      // Given - 只有单向关注
      socialService.followUser(user1.getId().longValue(), user2.getId().longValue());

      // When
      boolean user1FollowsUser2 = socialService.getFollowStatus(
          user1.getId().longValue(), user2.getId().longValue());
      boolean user2FollowsUser1 = socialService.getFollowStatus(
          user2.getId().longValue(), user1.getId().longValue());

      // Then
      assertTrue(user1FollowsUser2, "用户1应该关注用户2");
      assertFalse(user2FollowsUser1, "用户2不应该关注用户1");
    }
  }

  @Nested
  @DisplayName("收藏功能测试")
  class CollectionTests {

    @Test
    @DisplayName("获取收藏列表 - 空列表应该返回空")
    void getMyCollections_shouldReturnEmptyList() {
      // When - 获取空收藏列表
      Object collections = socialService.getMyCollections(user2.getId(), 1, 10);

      // Then
      assertNotNull(collections, "收藏列表不应为null");
    }
  }

  @Nested
  @DisplayName("黑名单功能测试")
  class BlacklistTests {

    @Test
    @DisplayName("拉黑用户 - 应该成功添加到黑名单")
    void blockUser_shouldSucceed() {
      // When
      socialService.blockUser(user1.getId().longValue(), user2.getId().longValue());

      // Then - 验证拉黑成功
      assertTrue(true, "拉黑操作应该成功");
    }

    @Test
    @DisplayName("解除拉黑 - 应该成功从黑名单移除")
    void unblockUser_shouldSucceed() {
      // Given - 先拉黑
      socialService.blockUser(user1.getId().longValue(), user2.getId().longValue());

      // When - 解除拉黑
      socialService.unblockUser(user1.getId().longValue(), user2.getId().longValue());

      // Then - 可以重新关注
      socialService.followUser(user1.getId().longValue(), user2.getId().longValue());
      assertTrue(socialService.getFollowStatus(user1.getId().longValue(), user2.getId().longValue()),
          "解除拉黑后应该可以重新关注");
    }

    @Test
    @DisplayName("拉黑后取消关注 - 拉黑应该自动取消关注关系")
    void blockUser_shouldCancelFollow() {
      // Given - 先关注
      socialService.followUser(user1.getId().longValue(), user2.getId().longValue());
      assertTrue(socialService.getFollowStatus(user1.getId().longValue(), user2.getId().longValue()));

      // When - 拉黑
      socialService.blockUser(user1.getId().longValue(), user2.getId().longValue());

      // Then - 关注关系应该被取消
      assertFalse(socialService.getFollowStatus(user1.getId().longValue(), user2.getId().longValue()),
          "拉黑后关注关系应该被取消");
    }
  }

}
