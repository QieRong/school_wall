// File: springboot/src/main/java/com/example/dto/BottleReplyDTO.java
package com.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 回复漂流瓶请求DTO
 */
public class BottleReplyDTO {

  @NotNull(message = "漂流瓶ID不能为空")
  private Long bottleId;

  @NotNull(message = "用户ID不能为空")
  private Long userId;

  @NotBlank(message = "回复内容不能为空")
  @Size(min = 1, max = 50, message = "回复内容长度必须在1-50字之间")
  private String content;

  // Getter/Setter
  public Long getBottleId() {
    return bottleId;
  }

  public void setBottleId(Long bottleId) {
    this.bottleId = bottleId;
  }

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
}
