package com.example.service;

import com.example.dto.*;
import com.example.entity.Story;
import com.example.entity.StoryAchievement;
import com.example.entity.StoryParagraph;
import com.github.pagehelper.PageInfo;

import java.util.List;

/**
 * 故事链服务接口
 */
public interface StoryService {

  /**
   * 创建故事
   */
  Story createStory(StoryCreateDTO dto);

  /**
   * 获取故事列表
   */
  PageInfo<StoryVO> getStoryList(Integer category, Integer status, int page, int size);

  /**
   * 获取故事详情
   */
  StoryDetailVO getStoryDetail(Long storyId, Long userId);

  /**
   * 搜索故事
   */
  PageInfo<StoryVO> searchStory(String keyword, Integer category, int page, int size);

  /**
   * 检查续写资格
   */
  ContinueCheckResult checkCanContinue(Long storyId, Long userId);

  /**
   * 获取续写锁
   */
  void acquireLock(Long storyId, Long userId);

  /**
   * 释放续写锁
   */
  void releaseLock(Long storyId, Long userId);

  /**
   * 续写锁心跳续期
   */
  void heartbeatLock(Long storyId, Long userId);

  /**
   * 续写故事
   */
  StoryParagraph continueStory(Long storyId, ParagraphCreateDTO dto);

  /**
   * 段落点赞/取消点赞
   */
  LikeResult toggleLike(Long paragraphId, Long userId);

  /**
   * 收藏/取消收藏故事
   */
  boolean toggleFavorite(Long storyId, Long userId);

  /**
   * 发起完结投票
   */
  void initiateFinishVote(Long storyId, Long userId);

  /**
   * 直接完结故事（发起者权限）
   */
  void finishStoryDirectly(Long storyId, Long userId);

  /**
   * 投票完结
   */
  void voteFinish(Long storyId, Long userId, boolean agree);

  /**
   * 获取我创建的故事
   */
  PageInfo<StoryVO> getMyCreatedStories(Long userId, int page, int size);

  /**
   * 获取我参与的故事
   */
  PageInfo<StoryVO> getMyParticipatedStories(Long userId, int page, int size);

  /**
   * 获取我的段落
   */
  List<ParagraphVO> getMyParagraphs(Long userId);

  /**
   * 获取我的成就
   */
  List<StoryAchievement> getMyAchievements(Long userId);

  /**
   * 获取我的收藏
   */
  PageInfo<StoryVO> getMyFavorites(Long userId, int page, int size);

  /**
   * 获取贡献度排行
   */
  List<ContributionRankVO> getContributionRank(Long storyId);

  /**
   * 删除故事
   */
  void deleteStory(Long storyId, Long userId);

  /**
   * 获取档案馆故事列表
   */
  PageInfo<StoryVO> getArchiveList(int page, int size);

  /**
   * 更新阅读进度
   */
  void updateReadProgress(Long storyId, Long userId, Integer sequence);

  // ========== 管理员功能 ==========

  /**
   * 管理员获取故事列表
   */
  PageInfo<StoryVO> getAdminStoryList(Integer status, Integer category, int page, int size);

  /**
   * 管理员删除故事
   */
  void adminDeleteStory(Long storyId);

  /**
   * 管理员删除段落
   */
  void adminDeleteParagraph(Long paragraphId);

  /**
   * 设置/取消官方推荐
   */
  void setRecommend(Long storyId, boolean recommend);

  /**
   * 获取统计数据
   */
  StoryStatsVO getStats();
}
