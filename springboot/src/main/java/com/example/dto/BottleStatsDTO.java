// File: springboot/src/main/java/com/example/dto/BottleStatsDTO.java
package com.example.dto;

/**
 * 漂流瓶统计数据DTO(管理员用)
 */
public class BottleStatsDTO {

  /** 总瓶子数 */
  private Long totalCount;

  /** 漂流中的瓶子数 */
  private Long activeCount;

  /** 今日新增瓶子数 */
  private Long todayNewCount;

  /** 总打捞次数 */
  private Long totalFishCount;

  /** 已沉没瓶子数 */
  private Long sunkenCount;

  /** 被珍藏瓶子数 */
  private Long collectedCount;

  // Getter/Setter
  public Long getTotalCount() {
    return totalCount;
  }

  public void setTotalCount(Long totalCount) {
    this.totalCount = totalCount;
  }

  public Long getActiveCount() {
    return activeCount;
  }

  public void setActiveCount(Long activeCount) {
    this.activeCount = activeCount;
  }

  public Long getTodayNewCount() {
    return todayNewCount;
  }

  public void setTodayNewCount(Long todayNewCount) {
    this.todayNewCount = todayNewCount;
  }

  public Long getTotalFishCount() {
    return totalFishCount;
  }

  public void setTotalFishCount(Long totalFishCount) {
    this.totalFishCount = totalFishCount;
  }

  public Long getSunkenCount() {
    return sunkenCount;
  }

  public void setSunkenCount(Long sunkenCount) {
    this.sunkenCount = sunkenCount;
  }

  public Long getCollectedCount() {
    return collectedCount;
  }

  public void setCollectedCount(Long collectedCount) {
    this.collectedCount = collectedCount;
  }
}
