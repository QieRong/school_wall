package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.dto.ParagraphVO;
import com.example.entity.StoryParagraph;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 故事段落Mapper接口
 */
@Mapper
public interface StoryParagraphMapper extends BaseMapper<StoryParagraph> {

  /**
   * 查询故事段落列表(带作者信息)
   */
  List<ParagraphVO> selectParagraphList(@Param("storyId") Long storyId,
      @Param("userId") Long userId);

  /**
   * 查询用户的所有段落
   */
  List<ParagraphVO> selectUserParagraphs(@Param("userId") Long userId);

  /**
   * 获取故事最新段落的作者ID
   */
  Long selectLastAuthorId(@Param("storyId") Long storyId);

  /**
   * 获取用户在故事中最后续写的时间
   */
  LocalDateTime selectLastWriteTime(@Param("storyId") Long storyId,
      @Param("userId") Long userId);

  /**
   * 获取故事当前最大序号
   */
  Integer selectMaxSequence(@Param("storyId") Long storyId);

  /**
   * 统计今日新增段落数
   */
  Long countTodayNewParagraphs();

  /**
   * 统计7天内活跃用户数
   */
  Long countActiveUsers();
}
