package com.example.dto;

import lombok.Data;

@Data
public class VoteTrendVO {

  /** 时间点 */
  private String time;

  /** 投票数 */
  private Integer votes;
}
