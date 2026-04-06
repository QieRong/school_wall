package com.example.controller;

import com.example.common.Result;
import com.example.dto.*;
import com.example.entity.Hotword;
import com.example.service.HotwordService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/hotword")
public class HotwordController {

  @Resource
  private HotwordService hotwordService;

  /**
   * 投稿热词
   */
  @PostMapping("/create")
  public Result<Hotword> create(@Valid @RequestBody HotwordCreateDTO dto,
      @RequestParam Long userId) {
    Hotword hotword = hotwordService.createHotword(dto, userId);
    return Result.success(hotword);
  }

  /**
   * 投票
   * 
   * @param count 投票数：1-普通投票，2-强烈认同
   */
  @PostMapping("/vote/{id}")
  public Result<VoteResultVO> vote(@PathVariable Long id,
      @RequestParam Long userId,
      @RequestParam(defaultValue = "1") Integer count) {
    VoteResultVO result = hotwordService.vote(id, userId, count);
    return Result.success(result);
  }

  /**
   * 获取热词列表
   * 
   * @param status 状态：1-活跃，2-归档，不传则返回活跃
   */
  @GetMapping("/list")
  public Result<List<HotwordVO>> list(@RequestParam(defaultValue = "1") Integer status) {
    return Result.success(hotwordService.getHotwordList(status));
  }

  /**
   * 获取榜单
   * 
   * @param type day/week/month/all
   */
  @GetMapping("/ranking")
  public Result<List<RankingVO>> ranking(@RequestParam(defaultValue = "day") String type) {
    return Result.success(hotwordService.getRanking(type));
  }

  /**
   * 获取热词详情
   * 
   * @param trendHours 趋势时间：24或168(7天)
   */
  @GetMapping("/{id}")
  public Result<HotwordDetailVO> detail(@PathVariable Long id,
      @RequestParam(defaultValue = "24") Integer trendHours) {
    return Result.success(hotwordService.getDetail(id, trendHours));
  }

  /**
   * 获取我的投稿
   */
  @GetMapping("/my/posts")
  public Result<PageInfo<HotwordVO>> myPosts(@RequestParam Long userId,
      @RequestParam(defaultValue = "1") Integer pageNum,
      @RequestParam(defaultValue = "10") Integer pageSize) {
    return Result.success(hotwordService.getMyPosts(userId, pageNum, pageSize));
  }

  /**
   * 获取我的投票记录
   */
  @GetMapping("/my/votes")
  public Result<PageInfo<VoteRecordVO>> myVotes(@RequestParam Long userId,
      @RequestParam(defaultValue = "1") Integer pageNum,
      @RequestParam(defaultValue = "10") Integer pageSize) {
    return Result.success(hotwordService.getMyVotes(userId, pageNum, pageSize));
  }

  /**
   * 获取剩余票数
   */
  @GetMapping("/my/quota")
  public Result<Integer> quota(@RequestParam Long userId) {
    return Result.success(hotwordService.getRemainingQuota(userId));
  }

  /**
   * 删除我的热词
   */
  @DeleteMapping("/{id}")
  public Result<Void> delete(@PathVariable Long id, @RequestParam Long userId) {
    hotwordService.deleteHotword(id, userId);
    return Result.success(null);
  }

  /**
   * 搜索热词
   */
  @GetMapping("/search")
  public Result<List<HotwordVO>> search(@RequestParam String keyword) {
    return Result.success(hotwordService.search(keyword));
  }
}
