package com.example.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class HotwordDetailVO {

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

  private String authorNickname;

  private String authorAvatar;

  private Long userId;

  private LocalDateTime createTime;

  /** 投票趋势数据 */
  private List<VoteTrendVO> voteTrend;

  /** 学院分布 */
  private Map<String, Integer> collegeDistribution;
}
