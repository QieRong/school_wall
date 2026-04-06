package com.example.dto;

/**
 * 健康度评分DTO
 */
public class HealthScoreDTO {
  private Integer score; // 0-100
  private String status; // green/yellow/red
  private Double dauRate; // 日活率
  private Double postRate; // 发帖率
  private Double interactionRate; // 互动率
  private Double auditPassRate; // 审核通过率

  public Integer getScore() {
    return score;
  }

  public void setScore(Integer score) {
    this.score = score;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Double getDauRate() {
    return dauRate;
  }

  public void setDauRate(Double dauRate) {
    this.dauRate = dauRate;
  }

  public Double getPostRate() {
    return postRate;
  }

  public void setPostRate(Double postRate) {
    this.postRate = postRate;
  }

  public Double getInteractionRate() {
    return interactionRate;
  }

  public void setInteractionRate(Double interactionRate) {
    this.interactionRate = interactionRate;
  }

  public Double getAuditPassRate() {
    return auditPassRate;
  }

  public void setAuditPassRate(Double auditPassRate) {
    this.auditPassRate = auditPassRate;
  }
}
