package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.HotwordDailyQuota;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

@Mapper
public interface HotwordDailyQuotaMapper extends BaseMapper<HotwordDailyQuota> {

  /**
   * 获取用户今日剩余票数
   */
  Integer selectRemainingVotes(@Param("userId") Long userId, @Param("voteDate") LocalDate voteDate);

  /**
   * 使用行锁获取用户今日剩余票数（用于并发安全）
   */
  Integer selectRemainingVotesForUpdate(@Param("userId") Long userId, @Param("voteDate") LocalDate voteDate);

  /**
   * 扣减用户票数
   */
  int decreaseVotes(@Param("userId") Long userId, @Param("voteDate") LocalDate voteDate, @Param("count") Integer count);

  /**
   * 初始化用户今日配额
   */
  int insertOrUpdate(@Param("userId") Long userId, @Param("voteDate") LocalDate voteDate,
      @Param("remainingVotes") Integer remainingVotes);

  /**
   * 删除过期的配额记录（清理历史数据）
   */
  int deleteExpiredQuotas(@Param("beforeDate") LocalDate beforeDate);
}
