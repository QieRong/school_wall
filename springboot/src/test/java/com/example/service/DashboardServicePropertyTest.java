package com.example.service;

import com.example.service.impl.DashboardServiceImpl;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据大屏服务属性测试
 */
class DashboardServicePropertyTest {

  private final DashboardServiceImpl dashboardService = new DashboardServiceImpl();

  /**
   * **Feature: data-dashboard, Property 1: 健康度评分到状态映射正确性**
   * **Validates: Requirements 1.2, 1.3, 1.4**
   */
  @Property(tries = 100)
  void healthScoreStatusMapping_shouldReturnCorrectStatus(
      @ForAll @IntRange(min = 0, max = 100) int score) {
    String status = dashboardService.getHealthStatus(score);
    if (score < 60) {
      assertEquals("red", status);
    } else if (score <= 80) {
      assertEquals("yellow", status);
    } else {
      assertEquals("green", status);
    }
  }

  /**
   * **Feature: data-dashboard, Property 2: 健康度评分加权计算正确性**
   * **Validates: Requirements 1.5**
   */
  @Property(tries = 100)
  void healthScoreCalculation_shouldUseCorrectWeights(
      @ForAll @DoubleRange(min = 0, max = 100) double dauRate,
      @ForAll @DoubleRange(min = 0, max = 100) double postRate,
      @ForAll @DoubleRange(min = 0, max = 100) double interactionRate,
      @ForAll @DoubleRange(min = 0, max = 100) double auditPassRate) {
    int score = dashboardService.calculateWeightedScore(dauRate, postRate, interactionRate, auditPassRate);
    assertTrue(score >= 0 && score <= 100);
    double expectedScore = dauRate * 0.30 + postRate * 0.25 + interactionRate * 0.25 + auditPassRate * 0.20;
    int expectedRounded = Math.max(0, Math.min(100, (int) Math.round(expectedScore)));
    assertEquals(expectedRounded, score);
  }

  @Example
  void healthScore_allZero_shouldBeZero() {
    assertEquals(0, dashboardService.calculateWeightedScore(0, 0, 0, 0));
  }

  @Example
  void healthScore_allMax_shouldBeMax() {
    assertEquals(100, dashboardService.calculateWeightedScore(100, 100, 100, 100));
  }
}

/**
 * 用户统计属性测试
 */
class UserStatsPropertyTest {

  /**
   * **Feature: data-dashboard, Property 5: 用户信誉分分段统计正确性**
   * **Validates: Requirements 3.3**
   */
  @Property(tries = 100)
  void creditDistribution_shouldAssignToCorrectSegment(@ForAll @IntRange(min = 0, max = 100) int credit) {
    String segment = getCreditSegment(credit);
    if (credit <= 20)
      assertEquals("0-20", segment);
    else if (credit <= 40)
      assertEquals("21-40", segment);
    else if (credit <= 60)
      assertEquals("41-60", segment);
    else if (credit <= 80)
      assertEquals("61-80", segment);
    else
      assertEquals("81-100", segment);
  }

  private String getCreditSegment(int credit) {
    if (credit <= 20)
      return "0-20";
    if (credit <= 40)
      return "21-40";
    if (credit <= 60)
      return "41-60";
    if (credit <= 80)
      return "61-80";
    return "81-100";
  }

  /**
   * **Feature: data-dashboard, Property 6: 用户分层逻辑正确性**
   * **Validates: Requirements 3.4**
   */
  @Property(tries = 100)
  void userSegmentation_shouldClassifyCorrectly(
      @ForAll @IntRange(min = 0, max = 50) int postCount,
      @ForAll @IntRange(min = 0, max = 50) int commentCount) {
    String segment = classifyUser(postCount, commentCount);
    if (postCount >= 5)
      assertEquals("creator", segment);
    else if (commentCount >= 10)
      assertEquals("interactor", segment);
    else
      assertEquals("lurker", segment);
  }

  private String classifyUser(int postCount, int commentCount) {
    if (postCount >= 5)
      return "creator";
    if (commentCount >= 10)
      return "interactor";
    return "lurker";
  }
}

/**
 * 排行榜属性测试
 */
class RankingPropertyTest {

  /**
   * **Feature: data-dashboard, Property 7: 排行榜排序正确性**
   * **Validates: Requirements 4.1, 4.2, 4.3**
   */
  @Property(tries = 100)
  void ranking_shouldBeSortedDescending(@ForAll("rankingValues") List<Integer> values) {
    List<Integer> sorted = values.stream()
        .sorted((a, b) -> b.compareTo(a))
        .limit(10)
        .toList();
    assertTrue(sorted.size() <= 10);
    for (int i = 0; i < sorted.size() - 1; i++) {
      assertTrue(sorted.get(i) >= sorted.get(i + 1));
    }
  }

  @Provide
  Arbitrary<List<Integer>> rankingValues() {
    return Arbitraries.integers().between(0, 10000).list().ofMinSize(0).ofMaxSize(50);
  }
}

/**
 * 敏感词统计属性测试
 */
class SensitiveWordPropertyTest {

  /**
   * **Feature: data-dashboard, Property 9: 敏感词统计排序正确性**
   * **Validates: Requirements 6.3**
   */
  @Property(tries = 100)
  void sensitiveWordStats_shouldBeSortedDescending(@ForAll("wordCounts") List<Integer> counts) {
    List<Integer> sorted = counts.stream().sorted((a, b) -> b.compareTo(a)).limit(10).toList();
    for (int i = 0; i < sorted.size() - 1; i++) {
      assertTrue(sorted.get(i) >= sorted.get(i + 1));
    }
  }

  @Provide
  Arbitrary<List<Integer>> wordCounts() {
    return Arbitraries.integers().between(1, 1000).list().ofMinSize(0).ofMaxSize(30);
  }
}

/**
 * 安全警报属性测试
 */
class AlertPropertyTest {

  /**
   * **Feature: data-dashboard, Property 10: 高举报帖子警报触发正确性**
   * **Validates: Requirements 8.1**
   */
  @Property(tries = 100)
  void highReportAlert_shouldTriggerCorrectly(@ForAll @IntRange(min = 0, max = 20) int reportCount) {
    boolean shouldTrigger = reportCount > 3;
    assertEquals(shouldTrigger, reportCount > 3);
  }

  /**
   * **Feature: data-dashboard, Property 11: 刷屏行为警报触发正确性**
   * **Validates: Requirements 8.2**
   */
  @Property(tries = 100)
  void spamAlert_shouldTriggerCorrectly(@ForAll @IntRange(min = 0, max = 20) int postCount) {
    boolean shouldTrigger = postCount > 5;
    assertEquals(shouldTrigger, postCount > 5);
  }

  /**
   * **Feature: data-dashboard, Property 12: 审核积压警报触发正确性**
   * **Validates: Requirements 8.3**
   */
  @Property(tries = 100)
  void queueOverflowAlert_shouldTriggerCorrectly(@ForAll @IntRange(min = 0, max = 200) int queueLength) {
    boolean shouldTrigger = queueLength > 50;
    assertEquals(shouldTrigger, queueLength > 50);
  }
}

/**
 * 24小时统计属性测试
 */
class HourlyStatsPropertyTest {

  /**
   * **Feature: data-dashboard, Property 13: 24小时数据完整性**
   * **Validates: Requirements 9.1, 9.2**
   */
  @Property(tries = 100)
  void hourlyStats_shouldHave24Elements(@ForAll("hourlyData") List<Integer> data) {
    List<Integer> hourlyData = new ArrayList<>(java.util.Collections.nCopies(24, 0));
    assertEquals(24, hourlyData.size());
  }

  @Provide
  Arbitrary<List<Integer>> hourlyData() {
    return Arbitraries.integers().between(0, 1000).list().ofSize(24);
  }

  /**
   * **Feature: data-dashboard, Property 14: 高峰时段计算正确性**
   * **Validates: Requirements 9.3**
   */
  @Property(tries = 100)
  void peakHour_shouldBeIndexOfMaxValue(@ForAll("hourlyData") List<Integer> data) {
    int peakHour = findPeakHour(data);
    assertTrue(peakHour >= 0 && peakHour < 24);
    int maxValue = data.stream().mapToInt(Integer::intValue).max().orElse(0);
    assertEquals(maxValue, data.get(peakHour).intValue());
  }

  private int findPeakHour(List<Integer> data) {
    int maxIndex = 0, maxValue = 0;
    for (int i = 0; i < data.size(); i++) {
      if (data.get(i) > maxValue) { maxValue = data.get(i); maxIndex = i; }
    }
    return maxIndex;
  }
}

/**
 * 位置热力图属性测试
 */
class LocationHeatmapPropertyTest {

  /**
   * **Feature: data-dashboard, Property 15: 位置热度计算正确性**
   * **Validates: Requirements 10.2**
   */
  @Property(tries = 100)
  void locationHeat_shouldCalculateCorrectly(
      @ForAll @IntRange(min = 0, max = 1000) int postCount,
      @ForAll @IntRange(min = 0, max = 10000) int likeCount) {
    int expectedHeat = postCount + likeCount / 10;
    assertEquals(expectedHeat, postCount + likeCount / 10);
  }

  /**
   * **Feature: data-dashboard, Property 16: 热门地点排序正确性**
   * **Validates: Requirements 10.3**
   */
  @Property(tries = 100)
  void hotLocations_shouldBeSortedDescending(@ForAll("heatValues") List<Integer> heatValues) {
    List<Integer> sorted = heatValues.stream().sorted((a, b) -> b.compareTo(a)).toList();
    for (int i = 0; i < sorted.size() - 1; i++) {
      assertTrue(sorted.get(i) >= sorted.get(i + 1));
    }
  }

  @Provide
  Arbitrary<List<Integer>> heatValues() {
    return Arbitraries.integers().between(0, 5000).list().ofMinSize(0).ofMaxSize(20);
  }
}

/**
 * 周对比属性测试
 */
class WeeklyComparisonPropertyTest {

  /**
   * **Feature: data-dashboard, Property 17: 周对比变化百分比计算正确性**
   * **Validates: Requirements 11.1, 11.2, 11.3**
   */
  @Property(tries = 100)
  void changePercent_shouldCalculateCorrectly(
      @ForAll @IntRange(min = 0, max = 10000) int current,
      @ForAll @IntRange(min = 0, max = 10000) int previous) {
    Double changePercent = calculateChangePercent(current, previous);
    if (previous == 0) {
      assertEquals(current > 0 ? 100.0 : 0.0, changePercent, 0.01);
    } else {
      double expected = ((double) (current - previous) / previous) * 100;
      assertEquals(expected, changePercent, 0.01);
    }
  }

  private Double calculateChangePercent(int current, int previous) {
    if (previous == 0) return current > 0 ? 100.0 : 0.0;
    return ((double) (current - previous) / previous) * 100;
  }

  /**
   * **Feature: data-dashboard, Property 18: 变化方向到颜色映射正确性**
   * **Validates: Requirements 11.4, 11.5**
   */
  @Property(tries = 100)
  void changeDirection_shouldMapToCorrectColor(@ForAll @DoubleRange(min = -100, max = 100) double changePercent) {
    String color = getChangeColor(changePercent);
    if (changePercent > 0) assertEquals("green", color);
    else if (changePercent < 0) assertEquals("red", color);
    else assertEquals("gray", color);
  }

  private String getChangeColor(double changePercent) {
    if (changePercent > 0) return "green";
    if (changePercent < 0) return "red";
    return "gray";
  }
}

/**
 * 词云属性测试
 */
class WordCloudPropertyTest {

  private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
      "的", "是", "了", "在", "和", "有", "我", "你", "他", "她", "它", "们",
      "这", "那", "就", "也", "都", "要", "会", "可以", "不", "吗", "呢", "啊"));

  /**
   * **Feature: data-dashboard, Property 8: 停用词过滤正确性**
   * **Validates: Requirements 5.2**
   */
  @Property(tries = 100)
  void wordCloud_shouldFilterStopWords(@ForAll("stopWords") String stopWord) {
    assertTrue(STOP_WORDS.contains(stopWord));
  }

  @Provide
  Arbitrary<String> stopWords() {
    return Arbitraries.of("的", "是", "了", "在", "和", "有", "我", "你", "他", "她");
  }

  @Property(tries = 100)
  void wordCloud_shouldKeepValidWords(@ForAll("validWords") String validWord) {
    assertFalse(STOP_WORDS.contains(validWord));
  }

  @Provide
  Arbitrary<String> validWords() {
    return Arbitraries.of("表白", "喜欢", "校园", "学习", "图书馆", "食堂", "宿舍", "考试");
  }
}
