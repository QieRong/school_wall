package com.example.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VoteRecordVO {

  private Long id;

  private Long hotwordId;

  private String hotwordName;

  /** 投票数：1或2 */
  private Integer voteCount;

  private LocalDateTime createTime;
}
