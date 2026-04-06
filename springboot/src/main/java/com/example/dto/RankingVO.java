package com.example.dto;

import lombok.Data;

@Data
public class RankingVO {

  /** 当前排名 */
  private Integer rank;

  private Long id;

  private String name;

  private Integer totalVotes;

  /** 热度等级：新芽/升温/火爆/现象级 */
  private String heatLevel;

  /** 排名变化：正数上升，负数下降，0不变 */
  private Integer rankChange;

  private Boolean isRecommended;
}
