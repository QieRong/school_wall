// File: springboot/src/main/java/com/example/dto/BottleCreateDTO.java
package com.example.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 创建漂流瓶请求DTO
 */
public class BottleCreateDTO {

  @NotNull(message = "用户ID不能为空")
  private Long userId;

  @NotBlank(message = "内容不能为空")
  @Size(min = 1, max = 200, message = "内容长度必须在1-200字之间")
  private String content;

  @NotNull(message = "漂流方向不能为空")
  @Min(value = 1, message = "无效的漂流方向")
  @Max(value = 3, message = "无效的漂流方向")
  private Integer direction;

  /**
   * 是否匿名(0否 1是)
   */
  private Integer isAnonymous;

  // Getter/Setter
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

  public Integer getDirection() {
    return direction;
  }

  public void setDirection(Integer direction) {
    this.direction = direction;
  }

  public Integer getIsAnonymous() {
    return isAnonymous;
  }

  public void setIsAnonymous(Integer isAnonymous) {
    this.isAnonymous = isAnonymous;
  }
}
