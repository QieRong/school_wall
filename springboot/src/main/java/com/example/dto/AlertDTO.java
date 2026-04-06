package com.example.dto;

import java.time.LocalDateTime;

/**
 * 安全警报DTO
 */
public class AlertDTO {
  private String type; // HIGH_REPORT/SPAM/QUEUE_OVERFLOW
  private String level; // red/orange/yellow
  private String message;
  private Long targetId;
  private LocalDateTime timestamp;

  public AlertDTO() {
  }

  public AlertDTO(String type, String level, String message, Long targetId) {
    this.type = type;
    this.level = level;
    this.message = message;
    this.targetId = targetId;
    this.timestamp = LocalDateTime.now();
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getLevel() {
    return level;
  }

  public void setLevel(String level) {
    this.level = level;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public Long getTargetId() {
    return targetId;
  }

  public void setTargetId(Long targetId) {
    this.targetId = targetId;
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(LocalDateTime timestamp) {
    this.timestamp = timestamp;
  }
}
