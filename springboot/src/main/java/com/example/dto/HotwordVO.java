package com.example.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class HotwordVO {

  private Long id;

  private String name;

  private String definition;

  private String example;

  private List<String> tags;

  private String imageUrl;

  private Integer totalVotes;

  /** 热度等级：新芽/升温/火爆/现象级 */
  private String heatLevel;

  private Boolean isRecommended;

  /** 状态：1-活跃，2-归档 */
  private Integer status;

  private String authorNickname;

  private String authorAvatar;

  private Long userId;

  private LocalDateTime createTime;
}
