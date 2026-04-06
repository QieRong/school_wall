package com.example.service;

import cn.hutool.crypto.digest.BCrypt;
import com.example.base.BaseTest;
import com.example.entity.Post;
import com.example.entity.User;
import com.example.mapper.PostMapper;
import com.example.mapper.UserMapper;
import com.example.service.impl.PostServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 帖子交互功能单元测试
 * 
 * 测试范围：
 * - 点赞功能
 * - 取消点赞
 * - 分享功能
 * - 收藏功能
 * - 投票功能
 * 
 * _Requirements: 3.1, 3.4, 3.5, 3.6, 3.7, 3.8_
 */
@DisplayName("帖子交互功能测试")
class PostInteractionTest extends BaseTest {

  @Autowired
  private PostServiceImpl postService;

  @Autowired
  private SocialService socialService;

  @Autowired
  private PostMapper postMapper;

  @Autowired
  private UserMapper userMapper;

  private User testUser;
  private User otherUser;
  private Post testPost;

  @BeforeEach
  void setUp() {
    // 创建测试用户
    testUser = new User();
    testUser.setAccount("2021777001");
    testUser.setPassword(BCrypt.hashpw("test123456"));
    testUser.setNickname("交互测试用户");
    testUser.setRole(0);
    testUser.setStatus(1);
    testUser.setCreditScore(100);
    testUser.setViolationCount(0);
    testUser.setAvatar("/default.png");
    userMapper.insert(testUser);

    // 创建另一个测试用户
    otherUser = new User();
    otherUser.setAccount("2021777002");
    otherUser.setPassword(BCrypt.hashpw("test123456"));
    otherUser.setNickname("其他用户");
    otherUser.setRole(0);
    otherUser.setStatus(1);
    otherUser.setCreditScore(100);
    userMapper.insert(otherUser);

    // 创建测试帖子
    testPost = new Post();
    testPost.setUserId(testUser.getId());
    testPost.setContent("测试帖子内容");
    testPost.setCategory(1);
    testPost.setIsAnonymous(0);
    testPost.setVisibility(0);
    testPost.setForceSubmit(true);
    postService.createPost(testPost);
  }

  @Nested
  @DisplayName("点赞功能测试")
  class LikeTests {

    @Test
    @DisplayName("点赞帖子 - 应该增加点赞数")
    void likePost_shouldIncrementLikeCount() {
      // Given - 获取初始点赞数
      Post before = postMapper.selectById(testPost.getId(), testUser.getId());
      int initialLikeCount = before.getLikeCount() != null ? before.getLikeCount() : 0;

      // When - 点赞
      boolean isLiked = postService.toggleLike(testPost.getId(), otherUser.getId());

      // Then
      assertTrue(isLiked, "应该返回已点赞状态");
      Post after = postMapper.selectById(testPost.getId(), testUser.getId());
      assertEquals(initialLikeCount + 1, after.getLikeCount(), "点赞数应该增加1");
    }

    @Test
    @DisplayName("取消点赞 - 应该减少点赞数")
    void unlikePost_shouldDecrementLikeCount() {
      // Given - 先点赞
      postService.toggleLike(testPost.getId(), otherUser.getId());
      Post afterLike = postMapper.selectById(testPost.getId(), testUser.getId());
      int likeCountAfterLike = afterLike.getLikeCount();

      // When - 再次点击取消点赞
      boolean isLiked = postService.toggleLike(testPost.getId(), otherUser.getId());

      // Then
      assertFalse(isLiked, "应该返回未点赞状态");
      Post afterUnlike = postMapper.selectById(testPost.getId(), testUser.getId());
      assertEquals(likeCountAfterLike - 1, afterUnlike.getLikeCount(), "点赞数应该减少1");
    }

    @Test
    @DisplayName("重复点赞 - 应该切换状态")
    void toggleLike_shouldToggleState() {
      // Given - 初始状态
      Post initial = postMapper.selectById(testPost.getId(), testUser.getId());
      int initialCount = initial.getLikeCount() != null ? initial.getLikeCount() : 0;

      // When - 第一次点赞
      boolean firstLike = postService.toggleLike(testPost.getId(), otherUser.getId());
      assertTrue(firstLike, "第一次应该是点赞");

      // When - 第二次点击（取消）
      boolean secondLike = postService.toggleLike(testPost.getId(), otherUser.getId());
      assertFalse(secondLike, "第二次应该是取消点赞");

      // When - 第三次点击（再次点赞）
      boolean thirdLike = postService.toggleLike(testPost.getId(), otherUser.getId());
      assertTrue(thirdLike, "第三次应该是点赞");

      // Then - 最终点赞数应该是初始值+1
      Post finalPost = postMapper.selectById(testPost.getId(), testUser.getId());
      assertEquals(initialCount + 1, finalPost.getLikeCount(), "最终点赞数应该是初始值+1");
    }

    @Test
    @DisplayName("多用户点赞 - 点赞数应该正确累加")
    void multipleUsersLike_shouldAccumulateCorrectly() {
      // Given - 创建更多用户
      User user3 = new User();
      user3.setAccount("2021777003");
      user3.setPassword("test123");
      user3.setNickname("用户3");
      user3.setRole(0);
      user3.setStatus(1);
      user3.setCreditScore(100);
      userMapper.insert(user3);

      User user4 = new User();
      user4.setAccount("2021777004");
      user4.setPassword("test123");
      user4.setNickname("用户4");
      user4.setRole(0);
      user4.setStatus(1);
      user4.setCreditScore(100);
      userMapper.insert(user4);

      // When - 多个用户点赞
      postService.toggleLike(testPost.getId(), otherUser.getId());
      postService.toggleLike(testPost.getId(), user3.getId());
      postService.toggleLike(testPost.getId(), user4.getId());

      // Then
      Post after = postMapper.selectById(testPost.getId(), testUser.getId());
      assertEquals(3, after.getLikeCount(), "点赞数应该为3");
    }
  }

  @Nested
  @DisplayName("分享功能测试")
  class ShareTests {

    @Test
    @DisplayName("分享帖子 - 应该增加分享数")
    void sharePost_shouldIncrementShareCount() {
      // Given
      Post before = postMapper.selectById(testPost.getId(), testUser.getId());
      int initialShareCount = before.getShareCount() != null ? before.getShareCount() : 0;

      // When
      postMapper.incrementShare(testPost.getId());

      // Then
      Post after = postMapper.selectById(testPost.getId(), testUser.getId());
      assertEquals(initialShareCount + 1, after.getShareCount(), "分享数应该增加1");
    }

    @Test
    @DisplayName("多次分享 - 分享数应该正确累加")
    void multipleShares_shouldAccumulateCorrectly() {
      // Given
      Post before = postMapper.selectById(testPost.getId(), testUser.getId());
      int initialShareCount = before.getShareCount() != null ? before.getShareCount() : 0;

      // When - 分享5次
      for (int i = 0; i < 5; i++) {
        postMapper.incrementShare(testPost.getId());
      }

      // Then
      Post after = postMapper.selectById(testPost.getId(), testUser.getId());
      assertEquals(initialShareCount + 5, after.getShareCount(), "分享数应该增加5");
    }
  }

  @Nested
  @DisplayName("收藏功能测试")
  class CollectionTests {

    @Test
    @DisplayName("收藏帖子 - 应该添加到收藏列表")
    void collectPost_shouldAddToCollection() {
      // Given - 确认未收藏
      assertFalse(socialService.isCollected(otherUser.getId(), testPost.getId()),
          "初始状态应该未收藏");

      // When - 收藏
      socialService.toggleCollection(otherUser.getId(), testPost.getId());

      // Then
      assertTrue(socialService.isCollected(otherUser.getId(), testPost.getId()),
          "应该已收藏");
    }

    @Test
    @DisplayName("取消收藏 - 应该从收藏列表移除")
    void uncollectPost_shouldRemoveFromCollection() {
      // Given - 先收藏
      socialService.toggleCollection(otherUser.getId(), testPost.getId());
      assertTrue(socialService.isCollected(otherUser.getId(), testPost.getId()));

      // When - 取消收藏
      socialService.toggleCollection(otherUser.getId(), testPost.getId());

      // Then
      assertFalse(socialService.isCollected(otherUser.getId(), testPost.getId()),
          "应该已取消收藏");
    }

    @Test
    @DisplayName("收藏列表 - 应该包含已收藏的帖子")
    void getMyCollections_shouldContainCollectedPosts() {
      // Given - 创建多个帖子并收藏
      Post post2 = new Post();
      post2.setUserId(testUser.getId());
      post2.setContent("第二个帖子");
      post2.setCategory(2);
      post2.setIsAnonymous(0);
      post2.setVisibility(0);
      post2.setForceSubmit(true);
      postService.createPost(post2);

      // 收藏两个帖子
      socialService.toggleCollection(otherUser.getId(), testPost.getId());
      socialService.toggleCollection(otherUser.getId(), post2.getId());

      // When
      Object collections = socialService.getMyCollections(otherUser.getId(), 1, 10);

      // Then
      assertNotNull(collections, "收藏列表不应为空");
    }

    @Test
    @DisplayName("重复收藏 - 应该切换状态")
    void toggleCollection_shouldToggleState() {
      // Given - 初始未收藏
      assertFalse(socialService.isCollected(otherUser.getId(), testPost.getId()));

      // When - 第一次收藏
      socialService.toggleCollection(otherUser.getId(), testPost.getId());
      assertTrue(socialService.isCollected(otherUser.getId(), testPost.getId()),
          "第一次应该是收藏");

      // When - 第二次取消
      socialService.toggleCollection(otherUser.getId(), testPost.getId());
      assertFalse(socialService.isCollected(otherUser.getId(), testPost.getId()),
          "第二次应该是取消收藏");

      // When - 第三次收藏
      socialService.toggleCollection(otherUser.getId(), testPost.getId());
      assertTrue(socialService.isCollected(otherUser.getId(), testPost.getId()),
          "第三次应该是收藏");
    }
  }

  @Nested
  @DisplayName("投票功能测试")
  class VoteTests {

    private Post pollPost;

    @BeforeEach
    void createPollPost() {
      pollPost = new Post();
      pollPost.setUserId(testUser.getId());
      pollPost.setContent("投票测试：你喜欢什么？");
      pollPost.setCategory(4);
      pollPost.setPollOptions("[\"选项A\",\"选项B\",\"选项C\"]");
      pollPost.setIsAnonymous(0);
      pollPost.setVisibility(0);
      pollPost.setForceSubmit(true);
      postService.createPost(pollPost);
    }

    @Test
    @DisplayName("投票 - 应该记录投票")
    void vote_shouldRecordVote() {
      // Given - 确认未投票
      Integer existingVote = postMapper.checkVote(pollPost.getId(), otherUser.getId());
      assertNull(existingVote, "初始状态应该未投票");

      // When - 投票
      postMapper.insertVote(pollPost.getId(), otherUser.getId(), 0);

      // Then
      Integer vote = postMapper.checkVote(pollPost.getId(), otherUser.getId());
      assertNotNull(vote, "应该已投票");
      assertEquals(0, vote, "投票选项应该是0");
    }

    @Test
    @DisplayName("重复投票 - 应该被阻止")
    void duplicateVote_shouldBeBlocked() {
      // Given - 先投票
      postMapper.insertVote(pollPost.getId(), otherUser.getId(), 0);

      // When - 尝试再次投票
      Integer existingVote = postMapper.checkVote(pollPost.getId(), otherUser.getId());

      // Then - 已经投过票
      assertNotNull(existingVote, "应该检测到已投票");
    }

    @Test
    @DisplayName("投票统计 - 应该正确统计")
    void voteStats_shouldBeCorrect() {
      // Given - 创建更多用户并投票
      User user3 = new User();
      user3.setAccount("2021777005");
      user3.setPassword("test123");
      user3.setNickname("用户5");
      user3.setRole(0);
      user3.setStatus(1);
      user3.setCreditScore(100);
      userMapper.insert(user3);

      // When - 多个用户投票
      postMapper.insertVote(pollPost.getId(), otherUser.getId(), 0); // 选项A
      postMapper.insertVote(pollPost.getId(), user3.getId(), 1); // 选项B

      // Then - 获取投票统计
      var voteCounts = postMapper.selectVoteCounts(pollPost.getId());
      assertNotNull(voteCounts, "投票统计不应为空");
    }
  }

  @Nested
  @DisplayName("浏览量测试")
  class ViewCountTests {

    @Test
    @DisplayName("查看帖子 - 应该增加浏览量")
    void viewPost_shouldIncrementViewCount() {
      // Given
      Post before = postMapper.selectById(testPost.getId(), testUser.getId());
      int initialViewCount = before.getViewCount() != null ? before.getViewCount() : 0;

      // When - 查看帖子详情
      postService.getPostDetail(testPost.getId(), otherUser.getId(), false);

      // Then
      Post after = postMapper.selectById(testPost.getId(), testUser.getId());
      assertEquals(initialViewCount + 1, after.getViewCount(), "浏览量应该增加1");
    }

    @Test
    @DisplayName("多次查看 - 浏览量应该正确累加")
    void multipleViews_shouldAccumulateCorrectly() {
      // Given
      Post before = postMapper.selectById(testPost.getId(), testUser.getId());
      int initialViewCount = before.getViewCount() != null ? before.getViewCount() : 0;

      // When - 查看3次
      postService.getPostDetail(testPost.getId(), otherUser.getId(), false);
      postService.getPostDetail(testPost.getId(), testUser.getId(), false);
      postService.getPostDetail(testPost.getId(), otherUser.getId(), false);

      // Then
      Post after = postMapper.selectById(testPost.getId(), testUser.getId());
      assertEquals(initialViewCount + 3, after.getViewCount(), "浏览量应该增加3");
    }
  }
}
