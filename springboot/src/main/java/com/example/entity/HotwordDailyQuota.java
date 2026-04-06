package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("hotword_daily_quota")
public class HotwordDailyQuota {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long userId;

  private LocalDate voteDate;

  /** 剩余票数，默认5票 */
  private Integer remainingVotes;
}
