package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("hotword_vote")
public class HotwordVote {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long hotwordId;

  private Long userId;

  /** 投票数：1-普通投票，2-强烈认同 */
  private Integer voteCount;

  private LocalDateTime createTime;
}
