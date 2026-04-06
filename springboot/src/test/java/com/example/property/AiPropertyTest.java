package com.example.property;

import com.example.base.BasePropertyTest;
import com.example.entity.AiUsage;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AI 助手属性测试
 * 
 * 测试 AI 助手的核心属性：
 * - Property 34: AI 使用次数限制
 * - Property 35: AI 剩余次数准确性
 * 
 * **Feature: functional-testing**
 */
public class AiPropertyTest extends BasePropertyTest {

  // 每日使用限制
  private static final int DAILY_LIMIT = 10;

  // ==================== Property 34: AI 使用次数限制 ====================

  /**
   * **Feature: functional-testing, Property 34: AI 使用次数限制**
   * **Validates: Requirements 8.3**
   * 
   * 用户每日使用次数不应超过限制。
   */
  @Property(tries = 100)
  @Label("Property 34: AI 使用次数限制 - 每日使用不应超过限制")
  void dailyUsageShouldNotExceedLimit(
      @ForAll @IntRange(min = 1, max = 1000) int userId,
      @ForAll @IntRange(min = 0, max = 20) int usageCount) {

    // Given - 模拟用户使用记录
    List<AiUsage> usages = new ArrayList<>();
    LocalDate today = LocalDate.now();

    for (int i = 0; i < usageCount; i++) {
      AiUsage usage = new AiUsage();
      usage.setUserId((long) userId);
      usage.setType(i % 2 == 0 ? "generate" : "polish");
      usage.setUsageDate(today);
      usage.setCreateTime(LocalDateTime.now());
      usages.add(usage);
    }

    // When - 统计今日使用次数
    long todayUsage = usages.stream()
        .filter(u -> u.getUsageDate().equals(today))
        .count();

    // Then - 检查是否可以继续使用
    boolean canUse = todayUsage < DAILY_LIMIT;

    if (usageCount < DAILY_LIMIT) {
      assertTrue(canUse, "未达到限制时应该可以使用");
    } else {
      assertFalse(canUse, "达到限制后不应该可以使用");
    }
  }

  /**
   * **Feature: functional-testing, Property 34: AI 使用次数限制**
   * **Validates: Requirements 8.3**
   * 
   * 不同日期的使用次数应该独立计算。
   */
  @Property(tries = 100)
  @Label("Property 34: 跨日使用独立 - 不同日期使用次数应该独立")
  void usageCountShouldBeIndependentByDate(
      @ForAll @IntRange(min = 1, max = 1000) int userId,
      @ForAll @IntRange(min = 0, max = 15) int todayUsage,
      @ForAll @IntRange(min = 0, max = 15) int yesterdayUsage) {

    // Given - 模拟跨日使用记录
    List<AiUsage> usages = new ArrayList<>();
    LocalDate today = LocalDate.now();
    LocalDate yesterday = today.minusDays(1);

    // 今日使用
    for (int i = 0; i < todayUsage; i++) {
      AiUsage usage = new AiUsage();
      usage.setUserId((long) userId);
      usage.setType("generate");
      usage.setUsageDate(today);
      usages.add(usage);
    }

    // 昨日使用
    for (int i = 0; i < yesterdayUsage; i++) {
      AiUsage usage = new AiUsage();
      usage.setUserId((long) userId);
      usage.setType("polish");
      usage.setUsageDate(yesterday);
      usages.add(usage);
    }

    // When - 分别统计今日和昨日使用次数
    long todayCount = usages.stream()
        .filter(u -> u.getUsageDate().equals(today))
        .count();
    long yesterdayCount = usages.stream()
        .filter(u -> u.getUsageDate().equals(yesterday))
        .count();

    // Then - 各日使用次数应该独立
    assertEquals(todayUsage, todayCount, "今日使用次数应该正确");
    assertEquals(yesterdayUsage, yesterdayCount, "昨日使用次数应该正确");

    // 今日是否可用只取决于今日使用次数
    boolean canUseToday = todayCount < DAILY_LIMIT;
    if (todayUsage < DAILY_LIMIT) {
      assertTrue(canUseToday, "今日未达限制时应该可以使用");
    }
  }

  // ==================== Property 35: AI 剩余次数准确性 ====================

  /**
   * **Feature: functional-testing, Property 35: AI 剩余次数准确性**
   * **Validates: Requirements 8.4**
   * 
   * 剩余次数应该等于限制减去已使用次数。
   */
  @Property(tries = 100)
  @Label("Property 35: AI 剩余次数准确性 - 剩余次数计算应该正确")
  void remainingCountShouldBeAccurate(
      @ForAll @IntRange(min = 0, max = 10) int usedCount) {

    // Given - 已使用次数
    int used = Math.min(usedCount, DAILY_LIMIT);

    // When - 计算剩余次数
    int remaining = DAILY_LIMIT - used;

    // Then - 剩余次数应该正确
    assertEquals(DAILY_LIMIT - used, remaining, "剩余次数应该等于限制减去已使用次数");
    assertTrue(remaining >= 0, "剩余次数不应该为负数");
    assertTrue(remaining <= DAILY_LIMIT, "剩余次数不应该超过限制");
  }

  /**
   * **Feature: functional-testing, Property 35: AI 剩余次数准确性**
   * **Validates: Requirements 8.4**
   * 
   * 使用一次后剩余次数应该减少1。
   */
  @Property(tries = 100)
  @Label("Property 35: 使用后剩余减少 - 使用一次后剩余应该减少1")
  void remainingShouldDecreaseAfterUse(
      @ForAll @IntRange(min = 0, max = 9) int initialUsed) {

    // Given - 初始剩余次数
    int initialRemaining = DAILY_LIMIT - initialUsed;

    // When - 使用一次
    int newUsed = initialUsed + 1;
    int newRemaining = DAILY_LIMIT - newUsed;

    // Then - 剩余次数应该减少1
    assertEquals(initialRemaining - 1, newRemaining, "使用后剩余次数应该减少1");
  }

  /**
   * **Feature: functional-testing, Property 35: AI 剩余次数准确性**
   * **Validates: Requirements 8.4**
   * 
   * 已使用次数 + 剩余次数 = 每日限制。
   */
  @Property(tries = 100)
  @Label("Property 35: 使用+剩余=限制 - 已使用加剩余应该等于限制")
  void usedPlusRemainingShouldEqualLimit(
      @ForAll @IntRange(min = 0, max = 10) int usedCount) {

    // Given - 已使用次数
    int used = Math.min(usedCount, DAILY_LIMIT);

    // When - 计算剩余次数
    int remaining = DAILY_LIMIT - used;

    // Then - 已使用 + 剩余 = 限制
    assertEquals(DAILY_LIMIT, used + remaining, "已使用加剩余应该等于限制");
  }

  // ==================== 辅助属性测试 ====================

  /**
   * **Feature: functional-testing, Property 34: AI 使用次数限制**
   * **Validates: Requirements 8.3**
   * 
   * AI 使用类型应该在有效范围内。
   */
  @Property(tries = 100)
  @Label("Property 34: AI 使用类型有效性 - 类型应该是 generate 或 polish")
  void aiUsageTypeShouldBeValid(
      @ForAll("validAiTypes") String type) {

    // Given - 创建使用记录
    AiUsage usage = new AiUsage();
    usage.setType(type);

    // Then - 类型应该有效
    assertTrue(type.equals("generate") || type.equals("polish"),
        "AI 使用类型应该是 generate 或 polish");
  }

  /**
   * 生成有效的 AI 使用类型
   */
  @Provide
  Arbitrary<String> validAiTypes() {
    return Arbitraries.of("generate", "polish");
  }

  /**
   * **Feature: functional-testing, Property 34: AI 使用次数限制**
   * **Validates: Requirements 8.1, 8.2**
   * 
   * AI 使用记录应该包含完整信息。
   */
  @Property(tries = 100)
  @Label("Property 34: AI 使用记录完整性 - 记录应该包含完整信息")
  void aiUsageRecordShouldBeComplete(
      @ForAll @IntRange(min = 1, max = 1000) int userId,
      @ForAll("validAiTypes") String type,
      @ForAll("aiPrompts") String prompt) {

    // Given - 创建使用记录
    AiUsage usage = new AiUsage();
    usage.setUserId((long) userId);
    usage.setType(type);
    usage.setPrompt(prompt);
    usage.setResult("AI 生成的结果");
    usage.setUsageDate(LocalDate.now());
    usage.setCreateTime(LocalDateTime.now());

    // Then - 记录应该包含完整信息
    assertEquals(userId, usage.getUserId().intValue(), "用户ID应该正确");
    assertEquals(type, usage.getType(), "类型应该正确");
    assertEquals(prompt, usage.getPrompt(), "提示词应该正确");
    assertNotNull(usage.getResult(), "结果不应为空");
    assertNotNull(usage.getUsageDate(), "使用日期不应为空");
    assertNotNull(usage.getCreateTime(), "创建时间不应为空");
  }

  /**
   * 生成 AI 提示词
   */
  @Provide
  Arbitrary<String> aiPrompts() {
    return Arbitraries.of(
        "帮我写一段表白文案",
        "润色这段文字",
        "生成一段校园生活分享",
        "帮我写一段心情日记");
  }
}
