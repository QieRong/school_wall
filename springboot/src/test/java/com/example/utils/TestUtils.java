package com.example.utils;

import com.example.entity.User;
import com.example.entity.Post;
import com.example.entity.Comment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 测试工具类
 * 提供测试数据创建、并发测试等通用工具方法
 */
public class TestUtils {

  private static final Random RANDOM = new Random();

  // ==================== 测试数据创建 ====================

  /**
   * 创建测试用户
   */
  public static User createTestUser(Long id, String account) {
    User user = new User();
    user.setId(id);
    user.setAccount(account);
    user.setPassword("$2a$10$encrypted_password"); // BCrypt 加密后的密码
    user.setNickname("测试用户" + id);
    user.setAvatar("/default.png");
    user.setRole(0); // 普通用户
    user.setStatus(1); // 正常状态
    user.setCreditScore(50);
    user.setViolationCount(0);
    user.setCreateTime(LocalDateTime.now());
    return user;
  }

  /**
   * 创建管理员用户
   */
  public static User createAdminUser(Long id, String account) {
    User user = createTestUser(id, account);
    user.setRole(1); // 管理员
    return user;
  }

  /**
   * 创建测试帖子
   */
  public static Post createTestPost(Long id, Long userId, String content) {
    Post post = new Post();
    post.setId(id);
    post.setUserId(userId);
    post.setContent(content);
    post.setCategory(1); // 表白
    post.setIsAnonymous(0);
    post.setVisibility(0); // 公开
    post.setLikeCount(0);
    post.setCommentCount(0);
    post.setShareCount(0);
    post.setViewCount(0);
    post.setStatus(1); // 已审核
    post.setCreateTime(LocalDateTime.now());
    return post;
  }

  /**
   * 创建匿名帖子
   */
  public static Post createAnonymousPost(Long id, Long userId, String content) {
    Post post = createTestPost(id, userId, content);
    post.setIsAnonymous(1);
    return post;
  }

  /**
   * 创建测试评论
   */
  public static Comment createTestComment(Long id, Long postId, Long userId, String content) {
    Comment comment = new Comment();
    comment.setId(id);
    comment.setPostId(postId);
    comment.setUserId(userId);
    comment.setContent(content);
    comment.setParentId(null);
    comment.setCreateTime(LocalDateTime.now());
    return comment;
  }

  /**
   * 创建回复评论
   */
  public static Comment createReplyComment(Long id, Long postId, Long userId,
      String content, Long parentId) {
    Comment comment = createTestComment(id, postId, userId, content);
    comment.setParentId(parentId);
    return comment;
  }

  // ==================== 并发测试工具 ====================

  /**
   * 执行并发测试
   * 
   * @param threadCount 并发线程数
   * @param task        要执行的任务
   * @return 所有任务是否成功完成
   */
  public static boolean runConcurrently(int threadCount, Runnable task) {
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);

    for (int i = 0; i < threadCount; i++) {
      executor.submit(() -> {
        try {
          task.run();
        } finally {
          latch.countDown();
        }
      });
    }

    try {
      boolean completed = latch.await(30, TimeUnit.SECONDS);
      executor.shutdown();
      return completed;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  /**
   * 执行并发测试并收集结果
   * 
   * @param threadCount 并发线程数
   * @param task        要执行的任务（返回结果）
   * @return 所有任务的结果列表
   */
  public static <T> List<T> runConcurrentlyWithResults(int threadCount, Supplier<T> task) {
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    List<T> results = new ArrayList<>();
    CountDownLatch latch = new CountDownLatch(threadCount);

    for (int i = 0; i < threadCount; i++) {
      executor.submit(() -> {
        try {
          T result = task.get();
          synchronized (results) {
            results.add(result);
          }
        } finally {
          latch.countDown();
        }
      });
    }

    try {
      latch.await(30, TimeUnit.SECONDS);
      executor.shutdown();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    return results;
  }

  // ==================== 随机数据生成 ====================

  /**
   * 生成随机字符串
   */
  public static String randomString(int length) {
    String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < length; i++) {
      sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
    }
    return sb.toString();
  }

  /**
   * 生成随机学号（10位数字）
   */
  public static String randomAccount() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 10; i++) {
      sb.append(RANDOM.nextInt(10));
    }
    return sb.toString();
  }

  /**
   * 生成随机帖子内容
   */
  public static String randomPostContent() {
    String[] templates = {
        "今天在图书馆看到一个很可爱的人",
        "食堂的新菜品真的很好吃",
        "有人一起去操场跑步吗",
        "求助：有人知道这道题怎么做吗",
        "闲置出售：全新的教材，有需要的联系我"
    };
    return templates[RANDOM.nextInt(templates.length)] + " " + randomString(10);
  }

  // ==================== 断言工具 ====================

  /**
   * 断言两个时间相差在指定秒数内
   */
  public static boolean isWithinSeconds(LocalDateTime time1, LocalDateTime time2, long seconds) {
    return Math.abs(java.time.Duration.between(time1, time2).getSeconds()) <= seconds;
  }
}
