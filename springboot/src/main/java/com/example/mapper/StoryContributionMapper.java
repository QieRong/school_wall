package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.StoryContribution;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 故事贡献度Mapper接口
 */
@Mapper
public interface StoryContributionMapper extends BaseMapper<StoryContribution> {

  /**
   * 获取用户在故事中的排名
   */
  Integer selectUserRank(@Param("storyId") Long storyId, @Param("userId") Long userId);

  /**
   * 获取用户总贡献度
   */
  Integer selectUserTotalPoints(@Param("userId") Long userId);

  /**
   * 获取总贡献度排名第一的用户ID
   */
  Long selectTopContributor();
}
