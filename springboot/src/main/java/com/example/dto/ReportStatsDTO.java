package com.example.dto;

import java.util.Map;

/**
 * 举报统计DTO
 */
public class ReportStatsDTO {
  private Integer pending;
  private Integer todayNew;
  private Double handleRate;
  private Map<String, Integer> distribution;

  public Integer getPending() {
    return pending;
  }

  public void setPending(Integer pending) {
    this.pending = pending;
  }

  public Integer getTodayNew() {
    return todayNew;
  }

  public void setTodayNew(Integer todayNew) {
    this.todayNew = todayNew;
  }

  public Double getHandleRate() {
    return handleRate;
  }

  public void setHandleRate(Double handleRate) {
    this.handleRate = handleRate;
  }

  public Map<String, Integer> getDistribution() {
    return distribution;
  }

  public void setDistribution(Map<String, Integer> distribution) {
    this.distribution = distribution;
  }
}
