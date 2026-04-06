package com.example.dto;

import java.util.List;

/**
 * 内容统计DTO
 */
public class ContentStatsDTO {
  private Integer pendingAudit;
  private Integer todayAudited;
  private Double passRate;
  private List<SensitiveWordStat> sensitiveWords;

  public Integer getPendingAudit() {
    return pendingAudit;
  }

  public void setPendingAudit(Integer pendingAudit) {
    this.pendingAudit = pendingAudit;
  }

  public Integer getTodayAudited() {
    return todayAudited;
  }

  public void setTodayAudited(Integer todayAudited) {
    this.todayAudited = todayAudited;
  }

  public Double getPassRate() {
    return passRate;
  }

  public void setPassRate(Double passRate) {
    this.passRate = passRate;
  }

  public List<SensitiveWordStat> getSensitiveWords() {
    return sensitiveWords;
  }

  public void setSensitiveWords(List<SensitiveWordStat> sensitiveWords) {
    this.sensitiveWords = sensitiveWords;
  }
}
