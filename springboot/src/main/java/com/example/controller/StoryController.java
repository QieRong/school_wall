package com.example.controller;

import com.example.common.Result;
import com.example.dto.*;
import com.example.entity.Story;
import com.example.entity.StoryAchievement;
import com.example.entity.StoryParagraph;
import com.example.service.StoryService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 故事链控制器
 */
@RestController
@RequestMapping("/story")
public class StoryController {

  @Resource
  private StoryService storyService;

  /**
   * 获取故事列表
   */
  @GetMapping("/list")
  public Result<PageInfo<StoryVO>> getStoryList(
      @RequestParam(required = false) Integer category,
      @RequestParam(required = false) Integer status,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "10") Integer size) {
    PageInfo<StoryVO> result = storyService.getStoryList(category, status, page, size);
    return Result.success(result);
  }

  /**
   * 创建故事
   */
  @PostMapping("/create")
  public Result<Story> createStory(@Valid @RequestBody StoryCreateDTO dto) {
    Story story = storyService.createStory(dto);
    return Result.success(story);
  }

  /**
   * 获取故事详情
   */
  @GetMapping("/{id}")
  public Result<StoryDetailVO> getStoryDetail(
      @PathVariable Long id,
      @RequestParam(required = false) Long userId) {
    StoryDetailVO detail = storyService.getStoryDetail(id, userId);
    return Result.success(detail);
  }

  /**
   * 检查续写资格
   */
  @GetMapping("/{id}/can-continue")
  public Result<ContinueCheckResult> checkCanContinue(
      @PathVariable Long id,
      @RequestParam Long userId) {
    ContinueCheckResult result = storyService.checkCanContinue(id, userId);
    return Result.success(result);
  }

  /**
   * 续写故事
   */
  @PostMapping("/{id}/continue")
  public Result<StoryParagraph> continueStory(
      @PathVariable Long id,
      @Valid @RequestBody ParagraphCreateDTO dto) {
    StoryParagraph paragraph = storyService.continueStory(id, dto);
    return Result.success(paragraph);
  }

  /**
   * 获取续写锁（打开弹窗时调用）
   */
  @PostMapping("/{id}/lock")
  public Result<String> acquireLock(
      @PathVariable Long id,
      @RequestParam Long userId) {
    storyService.acquireLock(id, userId);
    return Result.success("获取锁成功");
  }

  /**
   * 释放续写锁（关闭弹窗时调用）
   */
  @PostMapping("/{id}/unlock")
  public Result<String> releaseLock(
      @PathVariable Long id,
      @RequestParam Long userId) {
    storyService.releaseLock(id, userId);
    return Result.success("释放锁成功");
  }

  /**
   * 续写锁心跳续期
   */
  @PostMapping("/{id}/heartbeat")
  public Result<String> heartbeatLock(
      @PathVariable Long id,
      @RequestParam Long userId) {
    storyService.heartbeatLock(id, userId);
    return Result.success("续期成功");
  }

  /**
   * 段落点赞/取消点赞
   */
  @PostMapping("/paragraph/{paragraphId}/like")
  public Result<LikeResult> toggleLike(
      @PathVariable Long paragraphId,
      @RequestParam Long userId) {
    LikeResult result = storyService.toggleLike(paragraphId, userId);
    return Result.success(result);
  }

  /**
   * 搜索故事
   */
  @GetMapping("/search")
  public Result<PageInfo<StoryVO>> searchStory(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Integer category,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "10") Integer size) {
    PageInfo<StoryVO> result = storyService.searchStory(keyword, category, page, size);
    return Result.success(result);
  }

  /**
   * 收藏/取消收藏故事
   */
  @PostMapping("/{id}/favorite")
  public Result<Boolean> toggleFavorite(
      @PathVariable Long id,
      @RequestParam Long userId) {
    boolean favorited = storyService.toggleFavorite(id, userId);
    return Result.success(favorited);
  }

  /**
   * 直接完结故事（创建者权限）
   */
  @PostMapping("/{id}/finish-directly")
  public Result<String> finishDirectly(
      @PathVariable Long id,
      @RequestParam Long userId) {
    storyService.finishStoryDirectly(id, userId);
    return Result.success("故事已完结");
  }

  /**
   * 发起完结投票（贡献者权限）
   */
  @PostMapping("/{id}/finish-vote")
  public Result<String> initiateFinishVote(
      @PathVariable Long id,
      @RequestParam Long userId) {
    storyService.initiateFinishVote(id, userId);
    return Result.success("完结投票已发起，将持续24小时");
  }

  /**
   * 投票完结
   */
  @PostMapping("/{id}/vote-finish")
  public Result<String> voteFinish(
      @PathVariable Long id,
      @RequestParam Long userId,
      @RequestParam Boolean agree) {
    storyService.voteFinish(id, userId, agree);
    return Result.success("投票成功");
  }

  /**
   * 获取我创建的故事
   */
  @GetMapping("/my/created")
  public Result<PageInfo<StoryVO>> getMyCreatedStories(
      @RequestParam Long userId,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "10") Integer size) {
    PageInfo<StoryVO> result = storyService.getMyCreatedStories(userId, page, size);
    return Result.success(result);
  }

  /**
   * 获取我参与的故事
   */
  @GetMapping("/my/participated")
  public Result<PageInfo<StoryVO>> getMyParticipatedStories(
      @RequestParam Long userId,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "10") Integer size) {
    PageInfo<StoryVO> result = storyService.getMyParticipatedStories(userId, page, size);
    return Result.success(result);
  }

  /**
   * 获取我的段落
   */
  @GetMapping("/my/paragraphs")
  public Result<List<ParagraphVO>> getMyParagraphs(@RequestParam Long userId) {
    List<ParagraphVO> result = storyService.getMyParagraphs(userId);
    return Result.success(result);
  }

  /**
   * 获取我的成就
   */
  @GetMapping("/my/achievements")
  public Result<List<StoryAchievement>> getMyAchievements(@RequestParam Long userId) {
    List<StoryAchievement> result = storyService.getMyAchievements(userId);
    return Result.success(result);
  }

  /**
   * 获取我的收藏
   */
  @GetMapping("/my/favorites")
  public Result<PageInfo<StoryVO>> getMyFavorites(
      @RequestParam Long userId,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "10") Integer size) {
    PageInfo<StoryVO> result = storyService.getMyFavorites(userId, page, size);
    return Result.success(result);
  }

  /**
   * 获取贡献度排行
   */
  @GetMapping("/{id}/contribution-rank")
  public Result<List<ContributionRankVO>> getContributionRank(@PathVariable Long id) {
    List<ContributionRankVO> result = storyService.getContributionRank(id);
    return Result.success(result);
  }

  /**
   * 删除故事
   */
  @DeleteMapping("/{id}")
  public Result<String> deleteStory(
      @PathVariable Long id,
      @RequestParam Long userId) {
    storyService.deleteStory(id, userId);
    return Result.success("删除成功");
  }

  /**
   * 获取档案馆故事列表
   */
  @GetMapping("/archive")
  public Result<PageInfo<StoryVO>> getArchiveList(
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "10") Integer size) {
    PageInfo<StoryVO> result = storyService.getArchiveList(page, size);
    return Result.success(result);
  }

  /**
   * 更新阅读进度
   */
  @PostMapping("/{id}/read-progress")
  public Result<String> updateReadProgress(
      @PathVariable Long id,
      @RequestParam Long userId,
      @RequestParam Integer sequence) {
    storyService.updateReadProgress(id, userId, sequence);
    return Result.success("更新成功");
  }
}
