package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.StoryFinishVote;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

/**
 * 完结投票Mapper接口
 */
@Mapper
public interface StoryFinishVoteMapper extends BaseMapper<StoryFinishVote> {

  /**
   * 原子操作：同意票数+1
   */
  @Update("UPDATE story_finish_vote SET agree_count = agree_count + 1 WHERE id = #{voteId}")
  void incrementAgreeCount(Long voteId);

  /**
   * 原子操作：反对票数+1
   */
  @Update("UPDATE story_finish_vote SET disagree_count = disagree_count + 1 WHERE id = #{voteId}")
  void incrementDisagreeCount(Long voteId);
}
