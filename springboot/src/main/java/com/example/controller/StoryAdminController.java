package com.example.controller;

import com.example.common.Result;
import com.example.dto.StoryStatsVO;
import com.example.dto.StoryVO;
import com.example.service.StoryService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * 故事链管理员控制器
 */
@RestController
@RequestMapping("/admin/story")
public class StoryAdminController {

  @Resource
  private StoryService storyService;

  /**
   * 管理员获取故事列表
   */
  @GetMapping("/list")
  public Result<PageInfo<StoryVO>> getAdminStoryList(
      @RequestParam(required = false) Integer status,
      @RequestParam(required = false) Integer category,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "10") Integer size) {
    PageInfo<StoryVO> result = storyService.getAdminStoryList(status, category, page, size);
    return Result.success(result);
  }

  /**
   * 管理员删除故事
   */
  @DeleteMapping("/{id}")
  public Result<String> adminDeleteStory(@PathVariable Long id) {
    storyService.adminDeleteStory(id);
    return Result.success("删除成功");
  }

  /**
   * 管理员删除段落
   */
  @DeleteMapping("/paragraph/{paragraphId}")
  public Result<String> adminDeleteParagraph(@PathVariable Long paragraphId) {
    storyService.adminDeleteParagraph(paragraphId);
    return Result.success("删除成功");
  }

  /**
   * 设置/取消官方推荐
   */
  @PostMapping("/{id}/recommend")
  public Result<String> setRecommend(
      @PathVariable Long id,
      @RequestParam Boolean recommend) {
    storyService.setRecommend(id, recommend);
    return Result.success(recommend ? "已设为官方推荐" : "已取消推荐");
  }

  /**
   * 获取统计数据
   */
  @GetMapping("/stats")
  public Result<StoryStatsVO> getStats() {
    StoryStatsVO stats = storyService.getStats();
    return Result.success(stats);
  }
}
