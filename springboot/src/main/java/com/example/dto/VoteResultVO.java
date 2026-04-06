package com.example.dto;

import lombok.Data;

@Data
public class VoteResultVO {

  /** 是否成功 */
  private Boolean success;

  /** 消息 */
  private String message;

  /** 热词当前总票数 */
  private Integer totalVotes;

  /** 用户剩余票数 */
  private Integer remainingQuota;

  /** 热度等级 */
  private String heatLevel;
}
