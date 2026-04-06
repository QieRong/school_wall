package com.example.dto;

import lombok.Data;

@Data
public class HotwordStatsVO {

  /** 总热词数 */
  private Long totalHotwords;

  /** 今日新增 */
  private Long todayNew;

  /** 总投票数 */
  private Long totalVotes;

  /** 活跃用户数 */
  private Long activeUsers;

  /** 活跃热词数 */
  private Long activeHotwords;

  /** 归档热词数 */
  private Long archivedHotwords;
}
