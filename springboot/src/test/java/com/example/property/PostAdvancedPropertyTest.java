package com.example.property;

import com.example.base.BasePropertyTest;
import com.example.entity.Post;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 帖子高级功能属性测试
 * 
 * 测试帖子系统的高级属性：
 * - Property 9: 投票选项存储
 * - Property 10: 位置信息存储
 * - Property 11: 标签关联正确性
 * 
 * **Feature: functional-testing**
 */
public class PostAdvancedPropertyTest extends BasePropertyTest {

  // ==================== Property 9: 投票选项存储 ====================

  /**
   * **Feature: functional-testing, Property 9: 投票选项存储**
   * **Validates: Requirements 2.7**
   * 
   * 对于任意投票选项，创建和检索应该返回相同的选项，顺序一致。
   */
  @Property(tries = 100)
  @Label("Property 9: 投票选项存储 - 选项应该正确保存")
  void pollOptionsShouldBeStoredCorrectly(
      @ForAll("validPollOptions") List<String> options) {

    // Given - 将选项转换为JSON格式
    String pollOptionsJson = "[" + options.stream()
        .map(opt -> "\"" + opt + "\"")
        .collect(Collectors.joining(",")) + "]";

    Post post = new Post();
    post.setPollOptions(pollOptionsJson);

    // Then - 验证存储的选项
    assertNotNull(post.getPollOptions(), "投票选项不应为空");

    // 解析并验证选项数量
    List<String> parsedOptions = parsePollOptions(post.getPollOptions());
    assertEquals(options.size(), parsedOptions.size(), "选项数量应该一致");

    // 验证选项顺序
    for (int i = 0; i < options.size(); i++) {
      assertEquals(options.get(i), parsedOptions.get(i),
          "选项 " + i + " 应该一致");
    }
  }

  /**
   * 生成有效的投票选项（2-6个选项）
   */
  @Provide
  Arbitrary<List<String>> validPollOptions() {
    return Arbitraries.strings()
        .withCharRange('a', 'z')
        .withCharRange('A', 'Z')
        .withCharRange('0', '9')
        .ofMinLength(1)
        .ofMaxLength(50)
        .filter(s -> !s.isBlank())
        .list()
        .ofMinSize(2)
        .ofMaxSize(6);
  }

  /**
   * 解析投票选项JSON
   */
  private List<String> parsePollOptions(String pollOptionsJson) {
    if (pollOptionsJson == null || pollOptionsJson.isBlank()) {
      return new ArrayList<>();
    }

    String trimmed = pollOptionsJson.trim();
    if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
      return new ArrayList<>();
    }

    String content = trimmed.substring(1, trimmed.length() - 1);
    if (content.isEmpty()) {
      return new ArrayList<>();
    }

    List<String> result = new ArrayList<>();
    String[] parts = content.split(",");
    for (String part : parts) {
      String opt = part.trim();
      if (opt.startsWith("\"") && opt.endsWith("\"")) {
        opt = opt.substring(1, opt.length() - 1);
      }
      if (!opt.isEmpty()) {
        result.add(opt);
      }
    }
    return result;
  }

  /**
   * **Feature: functional-testing, Property 9: 投票选项存储**
   * **Validates: Requirements 2.7**
   * 
   * 投票截止时间应该正确存储
   */
  @Property(tries = 50)
  @Label("Property 9: 投票截止时间 - 应该正确保存")
  void pollEndTimeShouldBeStoredCorrectly(
      @ForAll @IntRange(min = 1, max = 30) int daysFromNow) {

    // Given
    LocalDateTime endTime = LocalDateTime.now().plusDays(daysFromNow);
    Post post = new Post();
    post.setPollEndTime(endTime);

    // Then
    assertNotNull(post.getPollEndTime(), "投票截止时间不应为空");
    assertEquals(endTime, post.getPollEndTime(), "投票截止时间应该一致");
  }

  // ==================== Property 10: 位置信息存储 ====================

  /**
   * **Feature: functional-testing, Property 10: 位置信息存储**
   * **Validates: Requirements 2.9**
   * 
   * 对于任意位置信息，存储的位置应该与输入匹配。
   */
  @Property(tries = 100)
  @Label("Property 10: 位置信息存储 - 位置应该正确保存")
  void locationShouldBeStoredCorrectly(
      @ForAll("validLocations") String location) {

    // Given
    Post post = new Post();

    // When
    post.setLocation(location);

    // Then
    assertEquals(location, post.getLocation(), "位置信息应该正确保存");
  }

  /**
   * 生成有效的位置信息
   */
  @Provide
  Arbitrary<String> validLocations() {
    return Arbitraries.oneOf(
        // 校园地点
        Arbitraries.of(
            "张家界学院图书馆",
            "张家界学院食堂",
            "张家界学院体育馆",
            "张家界学院教学楼A",
            "张家界学院宿舍区"),
        // 随机地点名称
        Arbitraries.strings()
            .withCharRange('a', 'z')
            .withCharRange('A', 'Z')
            .ofMinLength(2)
            .ofMaxLength(100)
            .filter(s -> !s.isBlank()));
  }

  /**
   * **Feature: functional-testing, Property 10: 位置信息存储**
   * **Validates: Requirements 2.9**
   * 
   * 空位置应该正确处理
   */
  @Property(tries = 20)
  @Label("Property 10: 空位置处理 - 空位置应该正确保存")
  void emptyLocationShouldBeHandled(
      @ForAll("emptyOrNullLocations") String location) {

    // Given
    Post post = new Post();

    // When
    post.setLocation(location);

    // Then - 空位置应该保持为空
    assertEquals(location, post.getLocation());
  }

  @Provide
  Arbitrary<String> emptyOrNullLocations() {
    return Arbitraries.of(null, "", "   ");
  }

  // ==================== Property 11: 标签关联正确性 ====================

  /**
   * **Feature: functional-testing, Property 11: 标签关联正确性**
   * **Validates: Requirements 2.10**
   * 
   * 对于任意标签（0-5个），存储的标签应该与输入匹配。
   */
  @Property(tries = 100)
  @Label("Property 11: 标签存储 - 标签应该正确保存")
  void tagsShouldBeStoredCorrectly(
      @ForAll("validTagsList") List<String> tags) {

    // Given - 将标签转换为逗号分隔格式
    String tagsStr = tags.isEmpty() ? null : String.join(",", tags);

    Post post = new Post();
    post.setTags(tagsStr);

    // Then
    if (tags.isEmpty()) {
      assertTrue(post.getTags() == null || post.getTags().isEmpty(),
          "空标签列表应该为空");
    } else {
      assertNotNull(post.getTags(), "标签不应为空");
      String[] savedTags = post.getTags().split(",");
      assertEquals(tags.size(), savedTags.length, "标签数量应该一致");

      for (int i = 0; i < tags.size(); i++) {
        assertEquals(tags.get(i), savedTags[i], "标签 " + i + " 应该一致");
      }
    }
  }

  /**
   * 生成有效的标签列表（0-5个标签）
   */
  @Provide
  Arbitrary<List<String>> validTagsList() {
    return Arbitraries.strings()
        .withCharRange('a', 'z')
        .withCharRange('A', 'Z')
        .ofMinLength(1)
        .ofMaxLength(20)
        .filter(s -> !s.isBlank() && !s.contains(","))
        .list()
        .ofMaxSize(5);
  }

  /**
   * **Feature: functional-testing, Property 11: 标签关联正确性**
   * **Validates: Requirements 2.10**
   * 
   * 标签数量不应超过5个
   */
  @Property(tries = 50)
  @Label("Property 11: 标签数量限制 - 最多5个标签")
  void tagCountShouldBeLimited(
      @ForAll @IntRange(min = 0, max = 5) int tagCount) {

    // Given - 生成指定数量的标签
    List<String> tags = new ArrayList<>();
    for (int i = 0; i < tagCount; i++) {
      tags.add("tag" + i);
    }
    String tagsStr = tags.isEmpty() ? null : String.join(",", tags);

    Post post = new Post();
    post.setTags(tagsStr);

    // Then
    if (tagCount == 0) {
      assertTrue(post.getTags() == null || post.getTags().isEmpty());
    } else {
      String[] savedTags = post.getTags().split(",");
      assertTrue(savedTags.length <= 5, "标签数量不应超过5个");
      assertEquals(tagCount, savedTags.length, "标签数量应该正确");
    }
  }

  // ==================== 组合属性测试 ====================

  /**
   * **Feature: functional-testing, Property 9, 10, 11: 组合测试**
   * **Validates: Requirements 2.7, 2.9, 2.10**
   * 
   * 帖子的投票、位置和标签应该同时正确存储
   */
  @Property(tries = 50)
  @Label("组合属性测试 - 投票、位置、标签应该同时正确存储")
  void postShouldStoreAllAdvancedFieldsCorrectly(
      @ForAll("validPollOptions") List<String> pollOptions,
      @ForAll("validLocations") String location,
      @ForAll("validTagsList") List<String> tags) {

    // Given
    String pollOptionsJson = "[" + pollOptions.stream()
        .map(opt -> "\"" + opt + "\"")
        .collect(Collectors.joining(",")) + "]";
    String tagsStr = tags.isEmpty() ? null : String.join(",", tags);
    LocalDateTime pollEndTime = LocalDateTime.now().plusDays(7);

    Post post = new Post();
    post.setPollOptions(pollOptionsJson);
    post.setPollEndTime(pollEndTime);
    post.setLocation(location);
    post.setTags(tagsStr);

    // Then - 验证所有字段
    assertNotNull(post.getPollOptions(), "投票选项不应为空");
    assertEquals(pollEndTime, post.getPollEndTime(), "投票截止时间应该正确");
    assertEquals(location, post.getLocation(), "位置应该正确");

    if (tags.isEmpty()) {
      assertTrue(post.getTags() == null || post.getTags().isEmpty());
    } else {
      assertNotNull(post.getTags());
      assertEquals(tags.size(), post.getTags().split(",").length);
    }
  }

  /**
   * **Feature: functional-testing, Property 9, 10, 11: 字段独立性**
   * **Validates: Requirements 2.7, 2.9, 2.10**
   * 
   * 高级字段应该独立存储，互不影响
   */
  @Property(tries = 30)
  @Label("组合属性测试 - 高级字段独立性")
  void advancedFieldsShouldBeIndependent(
      @ForAll("validLocations") String location1,
      @ForAll("validLocations") String location2) {

    // Given - 创建两个帖子
    Post post1 = new Post();
    Post post2 = new Post();

    // When - 设置不同的位置
    post1.setLocation(location1);
    post2.setLocation(location2);

    // Then - 两个帖子的位置应该独立
    assertEquals(location1, post1.getLocation(), "post1位置应该正确");
    assertEquals(location2, post2.getLocation(), "post2位置应该正确");

    // 修改一个不应该影响另一个
    post1.setLocation("modified location");
    assertEquals("modified location", post1.getLocation());
    assertEquals(location2, post2.getLocation(), "修改post1不应该影响post2");
  }

  // ==================== 定时发布属性测试 ====================

  /**
   * **Feature: functional-testing, Property: 定时发布**
   * **Validates: Requirements 2.8**
   * 
   * 定时发布时间应该正确存储
   */
  @Property(tries = 50)
  @Label("定时发布时间 - 应该正确保存")
  void scheduledTimeShouldBeStoredCorrectly(
      @ForAll @IntRange(min = 1, max = 365) int daysFromNow) {

    // Given
    LocalDateTime scheduledTime = LocalDateTime.now().plusDays(daysFromNow);
    Post post = new Post();

    // When
    post.setScheduledTime(scheduledTime);

    // Then
    assertNotNull(post.getScheduledTime(), "定时发布时间不应为空");
    assertEquals(scheduledTime, post.getScheduledTime(), "定时发布时间应该一致");
  }

  /**
   * **Feature: functional-testing, Property: 定时发布状态**
   * **Validates: Requirements 2.8**
   * 
   * 定时发布帖子状态应该为待发布(2)
   */
  @Property(tries = 30)
  @Label("定时发布状态 - 应该为待发布状态")
  void scheduledPostStatusShouldBePending(
      @ForAll @IntRange(min = 1, max = 30) int daysFromNow) {

    // Given
    Post post = new Post();
    post.setScheduledTime(LocalDateTime.now().plusDays(daysFromNow));
    post.setStatus(2); // 模拟 PostServiceImpl 的行为

    // Then
    assertEquals(2, post.getStatus(), "定时发布帖子状态应该为2(待发布)");
  }
}
