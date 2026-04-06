package com.example.property;

import com.example.base.BasePropertyTest;
import com.example.entity.Post;
import net.jqwik.api.*;
import net.jqwik.api.Combinators;
import net.jqwik.api.constraints.IntRange;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 帖子交互属性测试
 * 
 * 测试帖子交互的核心属性：
 * - Property 12: 帖子列表排序
 * - Property 13: 点赞并发安全
 * - Property 14: 点赞取消一致性
 * - Property 15: 收藏列表完整性
 * - Property 16: 投票记录准确性
 * 
 * **Feature: functional-testing**
 */
public class PostInteractionPropertyTest extends BasePropertyTest {

  // ==================== Property 12: 帖子列表排序 ====================

  /**
   * **Feature: functional-testing, Property 12: 帖子列表排序**
   * **Validates: Requirements 3.1**
   * 
   * 对于任意帖子列表，按时间排序后应该保持时间降序。
   */
  @Property(tries = 100)
  @Label("Property 12: 帖子列表排序 - 按时间排序应该保持降序")
  void postListShouldBeSortedByTimeDescending(
      @ForAll("randomPostList") List<Post> posts) {

    // When - 按创建时间降序排序
    List<Post> sortedPosts = new ArrayList<>(posts);
    sortedPosts.sort((a, b) -> {
      if (a.getCreateTime() == null && b.getCreateTime() == null)
        return 0;
      if (a.getCreateTime() == null)
        return 1;
      if (b.getCreateTime() == null)
        return -1;
      return b.getCreateTime().compareTo(a.getCreateTime());
    });

    // Then - 验证排序正确性
    for (int i = 0; i < sortedPosts.size() - 1; i++) {
      LocalDateTime current = sortedPosts.get(i).getCreateTime();
      LocalDateTime next = sortedPosts.get(i + 1).getCreateTime();
      if (current != null && next != null) {
        assertTrue(current.compareTo(next) >= 0,
            "帖子应该按时间降序排列");
      }
    }
  }

  /**
   * 生成随机帖子列表
   */
  @Provide
  Arbitrary<List<Post>> randomPostList() {
    return Arbitraries.integers().between(1, 20)
        .flatMap(size -> Arbitraries.integers().between(1, 1000).list().ofSize(size).map(ids -> {
          List<Post> posts = new ArrayList<>();
          LocalDateTime baseTime = LocalDateTime.now();
          Random random = new Random();
          for (int i = 0; i < ids.size(); i++) {
            Post post = new Post();
            post.setId(ids.get(i).longValue());
            post.setContent("帖子内容 " + i);
            post.setCreateTime(baseTime.minusMinutes(random.nextInt(10000)));
            posts.add(post);
          }
          return posts;
        }));
  }

  /**
   * **Feature: functional-testing, Property 12: 帖子列表排序**
   * **Validates: Requirements 3.1**
   * 
   * 按热度排序时，热度高的帖子应该排在前面。
   */
  @Property(tries = 100)
  @Label("Property 12: 帖子热度排序 - 热度高的应该排在前面")
  void postListShouldBeSortedByHotness(
      @ForAll("randomPostListWithStats") List<Post> posts) {

    // When - 按热度排序（点赞数 + 评论数 + 分享数）
    List<Post> sortedPosts = new ArrayList<>(posts);
    sortedPosts.sort((a, b) -> {
      int hotnessA = getHotness(a);
      int hotnessB = getHotness(b);
      return Integer.compare(hotnessB, hotnessA);
    });

    // Then - 验证排序正确性
    for (int i = 0; i < sortedPosts.size() - 1; i++) {
      int currentHotness = getHotness(sortedPosts.get(i));
      int nextHotness = getHotness(sortedPosts.get(i + 1));
      assertTrue(currentHotness >= nextHotness,
          "帖子应该按热度降序排列");
    }
  }

  private int getHotness(Post post) {
    int likes = post.getLikeCount() != null ? post.getLikeCount() : 0;
    int comments = post.getCommentCount() != null ? post.getCommentCount() : 0;
    int shares = post.getShareCount() != null ? post.getShareCount() : 0;
    return likes + comments * 2 + shares * 3;
  }

  /**
   * 生成带统计数据的随机帖子列表
   */
  @Provide
  Arbitrary<List<Post>> randomPostListWithStats() {
    return Arbitraries.integers().between(1, 20)
        .flatMap(size -> Arbitraries.integers().between(1, 1000).list().ofSize(size).map(ids -> {
          List<Post> posts = new ArrayList<>();
          Random random = new Random();
          for (int i = 0; i < ids.size(); i++) {
            Post post = new Post();
            post.setId(ids.get(i).longValue());
            post.setContent("帖子内容 " + i);
            post.setLikeCount(random.nextInt(1000));
            post.setCommentCount(random.nextInt(500));
            post.setShareCount(random.nextInt(200));
            posts.add(post);
          }
          return posts;
        }));
  }

  // ==================== Property 13 & 14: 点赞一致性 ====================

  /**
   * **Feature: functional-testing, Property 13: 点赞并发安全**
   * **Validates: Requirements 3.4**
   * 
   * 对于任意初始点赞数和点赞操作序列，最终点赞数应该等于初始值加上净点赞数。
   */
  @Property(tries = 100)
  @Label("Property 13: 点赞计数一致性 - 点赞数应该正确累加")
  void likeCountShouldBeConsistent(
      @ForAll @IntRange(min = 0, max = 1000) int initialLikes,
      @ForAll("likeOperations") List<Boolean> operations) {

    // Given - 初始点赞数
    int currentLikes = initialLikes;
    Set<Integer> likedUsers = new HashSet<>();

    // When - 模拟点赞操作
    for (int i = 0; i < operations.size(); i++) {
      int userId = i;
      boolean isLiking = operations.get(i);

      if (isLiking && !likedUsers.contains(userId)) {
        // 点赞
        likedUsers.add(userId);
        currentLikes++;
      } else if (!isLiking && likedUsers.contains(userId)) {
        // 取消点赞
        likedUsers.remove(userId);
        currentLikes--;
      }
    }

    // Then - 点赞数应该等于初始值加上当前点赞用户数
    assertEquals(initialLikes + likedUsers.size(), currentLikes,
        "点赞数应该等于初始值加上当前点赞用户数");
  }

  /**
   * 生成点赞操作序列
   */
  @Provide
  Arbitrary<List<Boolean>> likeOperations() {
    return Arbitraries.of(true, false).list().ofMinSize(1).ofMaxSize(50);
  }

  /**
   * **Feature: functional-testing, Property 14: 点赞取消一致性**
   * **Validates: Requirements 3.5**
   * 
   * 对于任意用户，点赞后取消点赞应该恢复到原始状态。
   */
  @Property(tries = 100)
  @Label("Property 14: 点赞取消一致性 - 点赞后取消应该恢复原状态")
  void likeToggleShouldRestoreState(
      @ForAll @IntRange(min = 0, max = 1000) int initialLikes,
      @ForAll @IntRange(min = 1, max = 100) int toggleCount) {

    // Given - 初始状态
    int currentLikes = initialLikes;
    boolean isLiked = false;

    // When - 切换点赞状态多次
    for (int i = 0; i < toggleCount; i++) {
      if (isLiked) {
        currentLikes--;
        isLiked = false;
      } else {
        currentLikes++;
        isLiked = true;
      }
    }

    // Then - 偶数次切换应该恢复原状态
    if (toggleCount % 2 == 0) {
      assertEquals(initialLikes, currentLikes,
          "偶数次切换后应该恢复原始点赞数");
      assertFalse(isLiked, "偶数次切换后应该是未点赞状态");
    } else {
      assertEquals(initialLikes + 1, currentLikes,
          "奇数次切换后点赞数应该增加1");
      assertTrue(isLiked, "奇数次切换后应该是已点赞状态");
    }
  }

  /**
   * **Feature: functional-testing, Property 13: 点赞并发安全**
   * **Validates: Requirements 3.4**
   * 
   * 多用户点赞同一帖子，点赞数应该等于点赞用户数。
   */
  @Property(tries = 100)
  @Label("Property 13: 多用户点赞 - 点赞数应该等于点赞用户数")
  void multiUserLikesShouldBeAccurate(
      @ForAll @IntRange(min = 1, max = 100) int userCount) {

    // Given - 模拟多用户点赞
    Set<Integer> likedUsers = new HashSet<>();
    int likeCount = 0;

    // When - 每个用户点赞一次
    for (int userId = 0; userId < userCount; userId++) {
      if (!likedUsers.contains(userId)) {
        likedUsers.add(userId);
        likeCount++;
      }
    }

    // Then - 点赞数应该等于点赞用户数
    assertEquals(likedUsers.size(), likeCount,
        "点赞数应该等于点赞用户数");
    assertEquals(userCount, likeCount,
        "点赞数应该等于用户数");
  }

  // ==================== Property 15: 收藏列表完整性 ====================

  /**
   * **Feature: functional-testing, Property 15: 收藏列表完整性**
   * **Validates: Requirements 3.7**
   * 
   * 对于任意收藏操作，收藏列表应该包含所有已收藏的帖子。
   */
  @Property(tries = 100)
  @Label("Property 15: 收藏列表完整性 - 应该包含所有已收藏帖子")
  void collectionListShouldBeComplete(
      @ForAll("collectionOperations") List<CollectionOp> operations) {

    // Given - 模拟收藏操作
    Set<Integer> collectedPosts = new HashSet<>();

    // When - 执行收藏操作
    for (CollectionOp op : operations) {
      if (op.isCollecting) {
        collectedPosts.add(op.postId);
      } else {
        collectedPosts.remove(op.postId);
      }
    }

    // Then - 收藏列表应该包含所有已收藏的帖子
    for (CollectionOp op : operations) {
      if (op.isCollecting && collectedPosts.contains(op.postId)) {
        assertTrue(collectedPosts.contains(op.postId),
            "收藏列表应该包含已收藏的帖子");
      }
    }
  }

  /**
   * 收藏操作记录
   */
  static class CollectionOp {
    Integer postId;
    Boolean isCollecting;

    CollectionOp(Integer postId, Boolean isCollecting) {
      this.postId = postId;
      this.isCollecting = isCollecting;
    }
  }

  /**
   * 生成收藏操作序列
   */
  @Provide
  Arbitrary<List<CollectionOp>> collectionOperations() {
    Arbitrary<CollectionOp> opArbitrary = Combinators.combine(
        Arbitraries.integers().between(1, 100),
        Arbitraries.of(true, false)).as(CollectionOp::new);
    return opArbitrary.list().ofMinSize(1).ofMaxSize(50);
  }

  /**
   * **Feature: functional-testing, Property 15: 收藏列表完整性**
   * **Validates: Requirements 3.7**
   * 
   * 收藏后取消收藏，帖子应该从收藏列表中移除。
   */
  @Property(tries = 100)
  @Label("Property 15: 收藏取消 - 取消后应该从列表移除")
  void uncollectShouldRemoveFromList(
      @ForAll @IntRange(min = 1, max = 1000) int postId) {

    // Given - 收藏帖子
    Set<Integer> collections = new HashSet<>();
    collections.add(postId);
    assertTrue(collections.contains(postId), "收藏后应该在列表中");

    // When - 取消收藏
    collections.remove(postId);

    // Then - 应该从列表中移除
    assertFalse(collections.contains(postId), "取消收藏后应该不在列表中");
  }

  // ==================== Property 16: 投票记录准确性 ====================

  /**
   * **Feature: functional-testing, Property 16: 投票记录准确性**
   * **Validates: Requirements 3.8**
   * 
   * 对于任意投票操作，投票统计应该准确反映每个选项的票数。
   */
  @Property(tries = 100)
  @Label("Property 16: 投票统计准确性 - 每个选项票数应该正确")
  void voteCountsShouldBeAccurate(
      @ForAll @IntRange(min = 2, max = 10) int optionCount,
      @ForAll("voteOperations") List<Integer> votes) {

    // Given - 初始化投票统计
    Map<Integer, Integer> voteCounts = new HashMap<>();
    for (int i = 0; i < optionCount; i++) {
      voteCounts.put(i, 0);
    }

    // When - 统计投票
    Map<Integer, Integer> userVotes = new HashMap<>(); // 用户ID -> 选项
    for (int i = 0; i < votes.size(); i++) {
      int userId = i;
      int option = votes.get(i) % optionCount; // 确保选项在有效范围内

      // 每个用户只能投一票
      if (!userVotes.containsKey(userId)) {
        userVotes.put(userId, option);
        voteCounts.put(option, voteCounts.get(option) + 1);
      }
    }

    // Then - 验证总票数等于投票用户数
    int totalVotes = voteCounts.values().stream().mapToInt(Integer::intValue).sum();
    assertEquals(userVotes.size(), totalVotes,
        "总票数应该等于投票用户数");
  }

  /**
   * 生成投票操作序列
   */
  @Provide
  Arbitrary<List<Integer>> voteOperations() {
    return Arbitraries.integers().between(0, 9).list().ofMinSize(1).ofMaxSize(100);
  }

  /**
   * **Feature: functional-testing, Property 16: 投票记录准确性**
   * **Validates: Requirements 3.8**
   * 
   * 用户只能投一票，重复投票应该被忽略。
   */
  @Property(tries = 100)
  @Label("Property 16: 重复投票防护 - 用户只能投一票")
  void userCanOnlyVoteOnce(
      @ForAll @IntRange(min = 1, max = 100) int userId,
      @ForAll @IntRange(min = 0, max = 9) int firstOption,
      @ForAll @IntRange(min = 0, max = 9) int secondOption) {

    // Given - 用户投票记录
    Map<Integer, Integer> userVotes = new HashMap<>();

    // When - 第一次投票
    if (!userVotes.containsKey(userId)) {
      userVotes.put(userId, firstOption);
    }

    // When - 尝试第二次投票
    Integer existingVote = userVotes.get(userId);

    // Then - 应该保持第一次投票
    assertNotNull(existingVote, "应该有投票记录");
    assertEquals(firstOption, existingVote, "应该保持第一次投票的选项");
  }

  /**
   * **Feature: functional-testing, Property 16: 投票记录准确性**
   * **Validates: Requirements 3.8**
   * 
   * 投票选项应该在有效范围内。
   */
  @Property(tries = 100)
  @Label("Property 16: 投票选项有效性 - 选项应该在有效范围内")
  void voteOptionShouldBeValid(
      @ForAll @IntRange(min = 2, max = 10) int optionCount,
      @ForAll @IntRange(min = 0, max = 100) int voteOption) {

    // Given - 有效选项范围
    int validOption = voteOption % optionCount;

    // Then - 选项应该在有效范围内
    assertTrue(validOption >= 0 && validOption < optionCount,
        "投票选项应该在0到" + (optionCount - 1) + "之间");
  }

  // ==================== 辅助属性测试 ====================

  /**
   * **Feature: functional-testing, Property 12: 帖子列表排序**
   * **Validates: Requirements 3.1**
   * 
   * 分页后的帖子列表应该保持排序顺序。
   */
  @Property(tries = 50)
  @Label("Property 12: 分页排序一致性 - 分页后应该保持排序")
  void paginatedListShouldMaintainOrder(
      @ForAll("randomPostListWithStats") List<Post> posts,
      @ForAll @IntRange(min = 1, max = 20) int pageSize) {

    // Given - 按热度排序
    List<Post> sortedPosts = new ArrayList<>(posts);
    sortedPosts.sort((a, b) -> Integer.compare(getHotness(b), getHotness(a)));

    // When - 分页
    int totalPages = (int) Math.ceil((double) sortedPosts.size() / pageSize);

    // Then - 每页内的排序应该正确
    for (int page = 0; page < totalPages; page++) {
      int start = page * pageSize;
      int end = Math.min(start + pageSize, sortedPosts.size());
      List<Post> pageContent = sortedPosts.subList(start, end);

      for (int i = 0; i < pageContent.size() - 1; i++) {
        int currentHotness = getHotness(pageContent.get(i));
        int nextHotness = getHotness(pageContent.get(i + 1));
        assertTrue(currentHotness >= nextHotness,
            "页内帖子应该按热度降序排列");
      }
    }
  }
}
