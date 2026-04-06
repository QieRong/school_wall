package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.dto.ContributionRankVO;
import com.example.dto.StoryVO;
import com.example.entity.Story;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 故事Mapper接口
 */
@Mapper
public interface StoryMapper extends BaseMapper<Story> {

  /**
   * 查询故事列表(带创建者信息)
   */
  List<StoryVO> selectStoryList(@Param("category") Integer category,
      @Param("status") Integer status,
      @Param("keyword") String keyword);

  /**
   * 查询用户创建的故事
   */
  List<StoryVO> selectMyCreatedStories(@Param("userId") Long userId);

  /**
   * 查询用户参与的故事
   */
  List<StoryVO> selectMyParticipatedStories(@Param("userId") Long userId);

  /**
   * 查询用户收藏的故事
   */
  List<StoryVO> selectMyFavoriteStories(@Param("userId") Long userId);

  /**
   * 查询故事贡献度排行
   */
  List<ContributionRankVO> selectContributionRank(@Param("storyId") Long storyId,
      @Param("limit") Integer limit);

  /**
   * 统计今日新增故事数
   */
  Long countTodayNewStories();

  /**
   * 统计各状态故事数
   */
  Long countByStatus(@Param("status") Integer status);

  /**
   * 悲观锁查询故事（用于并发控制）
   */
  @org.apache.ibatis.annotations.Select("SELECT * FROM story WHERE id = #{id} FOR UPDATE")
  Story selectByIdForUpdate(@Param("id") Long id);
}
