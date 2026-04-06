package com.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class HotwordCreateDTO {

  @NotBlank(message = "热词名称不能为空")
  @Size(max = 10, message = "热词名称不能超过10个字符")
  private String name;

  @NotBlank(message = "释义不能为空")
  @Size(max = 200, message = "释义不能超过200个字符")
  private String definition;

  @Size(max = 100, message = "例句不能超过100个字符")
  private String example;

  /** 标签列表，1-3个 */
  private List<String> tags;

  private String imageUrl;
}
