package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.dto.VoteRecordVO;
import com.example.dto.VoteTrendVO;
import com.example.entity.HotwordVote;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface HotwordVoteMapper extends BaseMapper<HotwordVote> {

  /**
   * 获取用户的投票记录
   */
  List<VoteRecordVO> selectUserVotes(@Param("userId") Long userId);

  /**
   * 获取热词的投票趋势（24小时或7天）
   * 
   * @param hours 小时数，24或168(7天)
   */
  List<VoteTrendVO> selectVoteTrend(@Param("hotwordId") Long hotwordId, @Param("hours") Integer hours);

  /**
   * 获取热词的学院分布
   */
  List<Map<String, Object>> selectCollegeDistribution(@Param("hotwordId") Long hotwordId);

  /**
   * 统计今日投票用户数
   */
  Long countTodayActiveUsers();

  /**
   * 统计总投票数
   */
  Long countTotalVotes();

  /**
   * 获取异常投票用户列表（短时间内投票次数过多）
   * 
   * @param minVotes 最小投票次数阈值
   * @param hours    时间范围（小时）
   */
  List<Map<String, Object>> selectAbnormalVoters(@Param("minVotes") Integer minVotes, @Param("hours") Integer hours);
}
