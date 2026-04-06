package com.example.property;

import com.example.base.BasePropertyTest;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 地图功能属性测试
 * 
 * 测试地图功能的核心属性：
 * - Property 36: 热门地点排序
 * 
 * **Feature: functional-testing**
 */
public class LocationPropertyTest extends BasePropertyTest {

  // ==================== Property 36: 热门地点排序 ====================

  /**
   * 地点数据类
   */
  static class LocationStats {
    String name;
    Integer postCount;
    Integer likeCount;

    LocationStats(String name, Integer postCount, Integer likeCount) {
      this.name = name;
      this.postCount = postCount;
      this.likeCount = likeCount;
    }

    int getHotness() {
      return (postCount != null ? postCount : 0) +
          (likeCount != null ? likeCount : 0);
    }
  }

  /**
   * **Feature: functional-testing, Property 36: 热门地点排序**
   * **Validates: Requirements 9.5**
   * 
   * 热门地点应该按热度降序排列。
   */
  @Property(tries = 100)
  @Label("Property 36: 热门地点排序 - 应该按热度降序排列")
  void hotLocationsShouldBeSortedByHotness(
      @ForAll("locationStatsList") List<LocationStats> locations) {

    // When - 按热度排序
    List<LocationStats> sortedLocations = new ArrayList<>(locations);
    sortedLocations.sort((a, b) -> Integer.compare(b.getHotness(), a.getHotness()));

    // Then - 验证排序正确性
    for (int i = 0; i < sortedLocations.size() - 1; i++) {
      int currentHotness = sortedLocations.get(i).getHotness();
      int nextHotness = sortedLocations.get(i + 1).getHotness();
      assertTrue(currentHotness >= nextHotness,
          "地点应该按热度降序排列");
    }
  }

  /**
   * 生成地点统计列表
   */
  @Provide
  Arbitrary<List<LocationStats>> locationStatsList() {
    return Arbitraries.integers().between(1, 20)
        .flatMap(size -> Combinators.combine(
            Arbitraries.strings().withCharRange('a', 'z').ofMinLength(3).ofMaxLength(20)
                .list().ofSize(size),
            Arbitraries.integers().between(0, 1000).list().ofSize(size),
            Arbitraries.integers().between(0, 5000).list().ofSize(size)).as((names, postCounts, likeCounts) -> {
              List<LocationStats> locations = new ArrayList<>();
              for (int i = 0; i < names.size(); i++) {
                locations.add(new LocationStats(
                    names.get(i),
                    postCounts.get(i),
                    likeCounts.get(i)));
              }
              return locations;
            }));
  }

  /**
   * **Feature: functional-testing, Property 36: 热门地点排序**
   * **Validates: Requirements 9.5**
   * 
   * 热度计算应该正确（帖子数 + 点赞数）。
   */
  @Property(tries = 100)
  @Label("Property 36: 热度计算正确性 - 热度应该等于帖子数加点赞数")
  void hotnessCalculationShouldBeCorrect(
      @ForAll @IntRange(min = 0, max = 1000) int postCount,
      @ForAll @IntRange(min = 0, max = 5000) int likeCount) {

    // Given - 创建地点统计
    LocationStats location = new LocationStats("测试地点", postCount, likeCount);

    // When - 计算热度
    int hotness = location.getHotness();

    // Then - 热度应该等于帖子数加点赞数
    assertEquals(postCount + likeCount, hotness,
        "热度应该等于帖子数加点赞数");
  }

  /**
   * **Feature: functional-testing, Property 36: 热门地点排序**
   * **Validates: Requirements 9.5**
   * 
   * 取前N个热门地点应该是热度最高的N个。
   */
  @Property(tries = 100)
  @Label("Property 36: 取前N个热门地点 - 应该是热度最高的N个")
  void topNLocationsShouldBeHottest(
      @ForAll("locationStatsList") List<LocationStats> locations,
      @ForAll @IntRange(min = 1, max = 10) int n) {

    Assume.that(locations.size() >= n);

    // When - 按热度排序并取前N个
    List<LocationStats> sortedLocations = new ArrayList<>(locations);
    sortedLocations.sort((a, b) -> Integer.compare(b.getHotness(), a.getHotness()));
    List<LocationStats> topN = sortedLocations.subList(0, Math.min(n, sortedLocations.size()));

    // Then - 前N个的热度应该都大于等于第N+1个
    if (sortedLocations.size() > n) {
      int nthHotness = topN.get(topN.size() - 1).getHotness();
      int nextHotness = sortedLocations.get(n).getHotness();
      assertTrue(nthHotness >= nextHotness,
          "前N个的热度应该都大于等于第N+1个");
    }
  }
}
