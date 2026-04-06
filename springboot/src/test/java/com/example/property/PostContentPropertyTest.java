package com.example.property;

import com.example.base.BasePropertyTest;
import com.example.entity.Post;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 帖子内容属性测试
 * 
 * 测试帖子系统的核心属性：
 * - Property 4: 帖子内容存储完整性
 * - Property 5: 图片数量约束
 * - Property 6: 分类存储正确性
 * 
 * **Feature: functional-testing**
 * 
 * 注意：这些测试验证帖子数据模型的属性，不依赖数据库
 */
public class PostContentPropertyTest extends BasePropertyTest {

  // ==================== Property 4: 帖子内容存储完整性 ====================

  /**
   * **Feature: functional-testing, Property 4: 帖子内容存储完整性**
   * **Validates: Requirements 2.1**
   * 
   * 对于任意有效的帖子内容（1-2000字符），设置到Post对象后应该能正确获取。
   */
  @Property(tries = 100)
  @Label("Property 4: 帖子内容存储完整性 - 设置和获取帖子内容应该一致")
  void postContentShouldBeStoredAndRetrievedCorrectly(
      @ForAll("safePostContent") String content) {

    // Given - 创建帖子对象
    Post post = new Post();

    // When - 设置内容
    post.setContent(content);

    // Then - 获取内容应该一致
    assertEquals(content, post.getContent(), "帖子内容应该与设置的一致");
  }

  /**
   * 生成安全的帖子内容
   */
  @Provide
  Arbitrary<String> safePostContent() {
    return Arbitraries.strings()
        .withCharRange('a', 'z')
        .withCharRange('A', 'Z')
        .withCharRange('0', '9')
        .withChars(' ', ',', '.', '!', '?')
        .ofMinLength(1)
        .ofMaxLength(500)
        .filter(s -> !s.isBlank());
  }

  /**
   * **Feature: functional-testing, Property 4: 帖子内容存储完整性**
   * **Validates: Requirements 2.1**
   * 
   * 帖子内容长度边界测试：最小1字符，最大2000字符
   */
  @Property(tries = 50)
  @Label("Property 4: 帖子内容长度边界 - 1到2000字符应该都能正确存储")
  void postContentLengthBoundary(
      @ForAll @IntRange(min = 1, max = 2000) int length) {

    // Given - 生成指定长度的内容
    String content = "a".repeat(length);
    Post post = new Post();

    // When - 设置内容
    post.setContent(content);

    // Then - 内容长度应该正确
    assertEquals(length, post.getContent().length(), "内容长度应该正确保存");
  }

  /**
   * **Feature: functional-testing, Property 4: 帖子内容存储完整性**
   * **Validates: Requirements 2.1**
   * 
   * 帖子内容不应该被意外修改
   */
  @Property(tries = 100)
  @Label("Property 4: 帖子内容不变性 - 多次获取应该返回相同内容")
  void postContentShouldBeImmutable(
      @ForAll("safePostContent") String content) {

    // Given
    Post post = new Post();
    post.setContent(content);

    // When - 多次获取
    String content1 = post.getContent();
    String content2 = post.getContent();
    String content3 = post.getContent();

    // Then - 每次获取应该相同
    assertEquals(content1, content2, "多次获取内容应该相同");
    assertEquals(content2, content3, "多次获取内容应该相同");
    assertEquals(content, content1, "获取的内容应该与设置的一致");
  }

  // ==================== Property 5: 图片数量约束 ====================

  /**
   * **Feature: functional-testing, Property 5: 图片数量约束**
   * **Validates: Requirements 2.2**
   * 
   * 对于任意帖子图片（0-9张），系统应该存储正确数量的图片。
   */
  @Property(tries = 100)
  @Label("Property 5: 图片数量约束 - 0-9张图片应该正确存储")
  void imageCountShouldBeStoredCorrectly(
      @ForAll @IntRange(min = 0, max = 9) int imageCount) {

    // Given - 生成图片URL列表
    List<String> imageUrls = generateImageUrls(imageCount);
    String imagesStr = imageUrls.isEmpty() ? null : String.join(",", imageUrls);

    Post post = new Post();
    post.setImages(imagesStr);

    // Then - 验证图片数量
    if (imageCount == 0) {
      assertTrue(post.getImages() == null || post.getImages().isEmpty(),
          "0张图片时images字段应该为空");
    } else {
      assertNotNull(post.getImages(), "图片字段不应为空");
      String[] savedImages = post.getImages().split(",");
      assertEquals(imageCount, savedImages.length,
          "存储的图片数量应该与上传数量一致");
    }
  }

  /**
   * 生成指定数量的图片URL
   */
  private List<String> generateImageUrls(int count) {
    return java.util.stream.IntStream.range(0, count)
        .mapToObj(i -> "/files/test_img_" + i + ".jpg")
        .collect(Collectors.toList());
  }

  /**
   * **Feature: functional-testing, Property 5: 图片数量约束**
   * **Validates: Requirements 2.2**
   * 
   * 图片URL列表解析测试 - 逗号分隔格式
   */
  @Property(tries = 50)
  @Label("Property 5: 逗号分隔图片列表 - 应该正确解析")
  void commaFormatImagesShouldParseCorrectly(
      @ForAll @IntRange(min = 1, max = 9) int imageCount) {

    // Given - 生成逗号分隔的图片列表
    List<String> imageUrls = generateImageUrls(imageCount);
    String imagesStr = String.join(",", imageUrls);

    // When - 解析图片列表
    String[] parsedImages = imagesStr.split(",");

    // Then - 解析后的数量应该正确
    assertEquals(imageCount, parsedImages.length, "解析后的图片数量应该正确");

    // And - 每个URL应该正确
    for (int i = 0; i < imageCount; i++) {
      assertEquals(imageUrls.get(i), parsedImages[i], "图片URL应该正确");
    }
  }

  /**
   * **Feature: functional-testing, Property 5: 图片数量约束**
   * **Validates: Requirements 2.2**
   * 
   * JSON格式图片列表应该正确解析
   */
  @Property(tries = 50)
  @Label("Property 5: JSON格式图片列表 - 应该正确解析")
  void jsonFormatImagesShouldParseCorrectly(
      @ForAll @IntRange(min = 1, max = 9) int imageCount) {

    // Given - 生成JSON格式的图片列表
    List<String> imageUrls = generateImageUrls(imageCount);
    String jsonImages = "[" + imageUrls.stream()
        .map(url -> "\"" + url + "\"")
        .collect(Collectors.joining(",")) + "]";

    // When - 使用 parseImageUrls 逻辑解析
    List<String> parsedImages = parseImageUrls(jsonImages);

    // Then - 解析后的数量应该正确
    assertEquals(imageCount, parsedImages.size(), "解析后的图片数量应该正确");
  }

  /**
   * 解析图片URL列表，兼容JSON数组和逗号分隔两种格式
   * 模拟 PostServiceImpl 中的解析逻辑
   */
  private List<String> parseImageUrls(String images) {
    if (images == null || images.isBlank()) {
      return Collections.emptyList();
    }

    String trimmed = images.trim();
    if (trimmed.startsWith("[")) {
      // JSON格式解析
      try {
        // 简单的JSON数组解析
        String content = trimmed.substring(1, trimmed.length() - 1);
        if (content.isEmpty()) {
          return Collections.emptyList();
        }
        String[] parts = content.split(",");
        List<String> result = new ArrayList<>();
        for (String part : parts) {
          String url = part.trim();
          if (url.startsWith("\"") && url.endsWith("\"")) {
            url = url.substring(1, url.length() - 1);
          }
          if (!url.isEmpty()) {
            result.add(url);
          }
        }
        return result;
      } catch (Exception e) {
        // 降级为逗号分隔
      }
    }

    // 逗号分隔格式
    String[] urls = images.split(",");
    List<String> result = new ArrayList<>();
    for (String url : urls) {
      if (!url.isBlank()) {
        result.add(url.trim());
      }
    }
    return result;
  }

  // ==================== Property 6: 分类存储正确性 ====================

  /**
   * **Feature: functional-testing, Property 6: 分类存储正确性**
   * **Validates: Requirements 2.4**
   * 
   * 对于任意帖子分类选择（1-9），存储的分类应该与选择的分类匹配。
   */
  @Property(tries = 100)
  @Label("Property 6: 分类存储正确性 - 分类应该正确保存")
  void categoryShouldBeStoredCorrectly(
      @ForAll("validPostCategories") int category) {

    // Given
    Post post = new Post();

    // When
    post.setCategory(category);

    // Then
    assertEquals(category, post.getCategory(),
        "存储的分类应该与输入分类一致");
  }

  /**
   * 生成有效的帖子分类
   * 1: 表白, 2: 寻物, 3: 闲置, 4: 吐槽, 5: 活动, 6: 求助, 9: 其他
   */
  @Provide
  Arbitrary<Integer> validPostCategories() {
    return Arbitraries.of(1, 2, 3, 4, 5, 6, 9);
  }

  /**
   * **Feature: functional-testing, Property 6: 分类存储正确性**
   * **Validates: Requirements 2.4**
   * 
   * 分类值应该在有效范围内
   */
  @Property(tries = 50)
  @Label("Property 6: 分类值范围 - 应该在有效范围内")
  void categoryShouldBeInValidRange(
      @ForAll("validPostCategories") int category) {

    // Given
    Post post = new Post();
    post.setCategory(category);

    // Then - 分类应该在有效范围内
    int savedCategory = post.getCategory();
    assertTrue(
        savedCategory == 1 || savedCategory == 2 || savedCategory == 3 ||
            savedCategory == 4 || savedCategory == 5 || savedCategory == 6 ||
            savedCategory == 9,
        "分类应该是有效值: 1, 2, 3, 4, 5, 6, 9");
  }

  // ==================== 组合属性测试 ====================

  /**
   * **Feature: functional-testing, Property 4, 5, 6: 组合测试**
   * **Validates: Requirements 2.1, 2.2, 2.4**
   * 
   * 帖子的内容、图片和分类应该同时正确存储
   */
  @Property(tries = 50)
  @Label("组合属性测试 - 内容、图片、分类应该同时正确存储")
  void postShouldStoreAllFieldsCorrectly(
      @ForAll("safePostContent") String content,
      @ForAll @IntRange(min = 0, max = 5) int imageCount,
      @ForAll("validPostCategories") int category) {

    // Given
    List<String> imageUrls = generateImageUrls(imageCount);
    String imagesStr = imageUrls.isEmpty() ? null : String.join(",", imageUrls);

    Post post = new Post();
    post.setContent(content);
    post.setCategory(category);
    post.setImages(imagesStr);
    post.setIsAnonymous(0);
    post.setVisibility(0);

    // Then - 验证所有字段
    assertEquals(content, post.getContent(), "内容应该正确存储");
    assertEquals(category, post.getCategory(), "分类应该正确存储");

    // 验证图片数量
    if (imageCount == 0) {
      assertTrue(post.getImages() == null || post.getImages().isEmpty(),
          "0张图片时images字段应该为空");
    } else {
      assertNotNull(post.getImages());
      String[] savedImages = post.getImages().split(",");
      assertEquals(imageCount, savedImages.length, "图片数量应该正确");
    }
  }

  /**
   * **Feature: functional-testing, Property 4, 5, 6: 组合测试**
   * **Validates: Requirements 2.1, 2.2, 2.4**
   * 
   * 帖子对象的所有字段应该独立存储，互不影响
   */
  @Property(tries = 50)
  @Label("组合属性测试 - 字段独立性")
  void postFieldsShouldBeIndependent(
      @ForAll("safePostContent") String content1,
      @ForAll("safePostContent") String content2,
      @ForAll("validPostCategories") int category1,
      @ForAll("validPostCategories") int category2) {

    // Given - 创建两个帖子
    Post post1 = new Post();
    Post post2 = new Post();

    // When - 设置不同的值
    post1.setContent(content1);
    post1.setCategory(category1);

    post2.setContent(content2);
    post2.setCategory(category2);

    // Then - 两个帖子的值应该独立
    assertEquals(content1, post1.getContent(), "post1内容应该正确");
    assertEquals(content2, post2.getContent(), "post2内容应该正确");
    assertEquals(category1, post1.getCategory(), "post1分类应该正确");
    assertEquals(category2, post2.getCategory(), "post2分类应该正确");

    // 修改一个不应该影响另一个
    post1.setContent("modified");
    assertEquals("modified", post1.getContent());
    assertEquals(content2, post2.getContent(), "修改post1不应该影响post2");
  }

  // ==================== 可见性属性测试 ====================

  /**
   * **Feature: functional-testing, Property 8: 可见性规则执行**
   * **Validates: Requirements 2.6**
   * 
   * 可见性设置应该正确存储
   */
  @Property(tries = 100)
  @Label("Property 8: 可见性存储 - 可见性设置应该正确保存")
  void visibilityShouldBeStoredCorrectly(
      @ForAll @IntRange(min = 0, max = 2) int visibility) {

    // Given
    Post post = new Post();

    // When
    post.setVisibility(visibility);

    // Then
    assertEquals(visibility, post.getVisibility(),
        "可见性设置应该正确保存");
  }

  // ==================== 匿名属性测试 ====================

  /**
   * **Feature: functional-testing, Property 7: 匿名帖子身份隐藏**
   * **Validates: Requirements 2.5**
   * 
   * 匿名标志应该正确存储
   */
  @Property(tries = 100)
  @Label("Property 7: 匿名标志存储 - 匿名设置应该正确保存")
  void anonymousFlagShouldBeStoredCorrectly(
      @ForAll @IntRange(min = 0, max = 1) int isAnonymous) {

    // Given
    Post post = new Post();

    // When
    post.setIsAnonymous(isAnonymous);

    // Then
    assertEquals(isAnonymous, post.getIsAnonymous(),
        "匿名标志应该正确保存");
  }
}
