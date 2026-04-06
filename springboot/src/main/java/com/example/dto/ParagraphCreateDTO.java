package com.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 段落续写请求DTO
 */
public class ParagraphCreateDTO {

  /** 用户ID */
  @NotNull(message = "用户ID不能为空")
  private Long userId;

  /** 续写内容 */
  @NotBlank(message = "续写内容不能为空")
  @Size(min = 100, max = 500, message = "续写内容需要100-500字符")
  private String content;

  /** 笔名 */
  @Size(max = 20, message = "笔名最多20字符")
  private String penName;

  /** 插画URL */
  private String imageUrl;

  /** 是否标记为关键剧情点 */
  private Boolean isKeyPoint = false;

  private Integer isAiAssisted;

  // ========== Getter/Setter ==========

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getPenName() {
    return penName;
  }

  public void setPenName(String penName) {
    this.penName = penName;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }

  public Boolean getIsKeyPoint() {
    return isKeyPoint;
  }

  public void setIsKeyPoint(Boolean isKeyPoint) {
    this.isKeyPoint = isKeyPoint;
  }

  public Integer getIsAiAssisted() {
    return isAiAssisted;
  }

  public void setIsAiAssisted(Integer isAiAssisted) {
    this.isAiAssisted = isAiAssisted;
  }
}
