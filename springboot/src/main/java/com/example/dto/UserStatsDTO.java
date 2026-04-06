package com.example.dto;

import java.util.List;
import java.util.Map;

/**
 * 用户统计DTO
 */
public class UserStatsDTO {
  private Integer total;
  private Integer todayNew;
  private Integer todayActive;
  private List<TrendPoint> trend; // 7天趋势
  private Map<String, Integer> creditDistribution;

  public Integer getTotal() {
    return total;
  }

  public void setTotal(Integer total) {
    this.total = total;
  }

  public Integer getTodayNew() {
    return todayNew;
  }

  public void setTodayNew(Integer todayNew) {
    this.todayNew = todayNew;
  }

  public Integer getTodayActive() {
    return todayActive;
  }

  public void setTodayActive(Integer todayActive) {
    this.todayActive = todayActive;
  }

  public List<TrendPoint> getTrend() {
    return trend;
  }

  public void setTrend(List<TrendPoint> trend) {
    this.trend = trend;
  }

  public Map<String, Integer> getCreditDistribution() {
    return creditDistribution;
  }

  public void setCreditDistribution(Map<String, Integer> creditDistribution) {
    this.creditDistribution = creditDistribution;
  }
}
