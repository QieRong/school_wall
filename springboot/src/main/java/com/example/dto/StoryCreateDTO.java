package com.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.Range;

/**
 * 故事创建请求DTO
 */
public class StoryCreateDTO {

  /** 用户ID */
  @NotNull(message = "用户ID不能为空")
  private Long userId;

  /** 故事标题 */
  @NotBlank(message = "标题不能为空")
  @Size(max = 30, message = "标题最多30个字符")
  private String title;

  /** 开篇内容 */
  @NotBlank(message = "开篇内容不能为空")
  @Size(min = 100, max = 500, message = "开篇内容需要100-500字符")
  private String content;

  /** 故事分类 */
  @NotNull(message = "请选择故事分类")
  @Range(min = 1, max = 5, message = "无效的分类")
  private Integer category;

  /** 世界观设定 */
  @Size(max = 200, message = "世界观设定最多200字符")
  private String worldSetting;

  private Integer isAiAssisted;

  // ========== Getter/Setter ==========

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public Integer getCategory() {
    return category;
  }

  public void setCategory(Integer category) {
    this.category = category;
  }

  public String getWorldSetting() {
    return worldSetting;
  }

  public void setWorldSetting(String worldSetting) {
    this.worldSetting = worldSetting;
  }

  public Integer getIsAiAssisted() {
    return isAiAssisted;
  }

  public void setIsAiAssisted(Integer isAiAssisted) {
    this.isAiAssisted = isAiAssisted;
  }
}
