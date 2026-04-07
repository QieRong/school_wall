// File: springboot/src/main/java/com/example/controller/BottleController.java
package com.example.controller;

import com.example.common.Result;
import com.example.dto.BottleCreateDTO;
import com.example.dto.BottleReplyDTO;
import com.example.dto.BottleStatsDTO;
import com.example.dto.FishResult;
import com.example.entity.BottleAchievement;
import com.example.entity.BottleFishRecord;
import com.example.entity.DriftBottle;
import com.example.mapper.BottleFishRecordMapper;
import com.example.service.BottleService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 漂流瓶控制器
 */
@RestController
@RequestMapping("/bottle")
public class BottleController {

  @Resource
  private BottleService bottleService;

  @Resource
  private BottleFishRecordMapper fishRecordMapper;

  /**
   * 投放漂流瓶
   */
  @PostMapping("/throw")
  public Result<?> throwBottle(@Valid @RequestBody BottleCreateDTO dto) {
    bottleService.throwBottle(dto);
    return Result.success("投放成功，你的心意已随波远行");
  }

  /**
   * 打捞漂流瓶
   */
  @PostMapping("/fish")
  public Result<FishResult> fishBottle(@RequestParam Long userId) {
    FishResult result = bottleService.fishBottle(userId);
    return Result.success(result);
  }

  /**
   * 回复漂流瓶
   */
  @PostMapping("/reply")
  public Result<?> replyBottle(@Valid @RequestBody BottleReplyDTO dto) {
    bottleService.replyBottle(dto);
    return Result.success("回复成功，瓶子已放回大海");
  }

  /**
   * 放回漂流瓶（不回复）
   */
  @PostMapping("/release/{bottleId}")
  public Result<?> releaseBottle(@PathVariable Long bottleId) {
    bottleService.releaseBottle(bottleId);
    return Result.success("瓶子已放回大海");
  }

  /**
   * 珍藏漂流瓶
   */
  @PostMapping("/collect/{bottleId}")
  public Result<?> collectBottle(@PathVariable Long bottleId, @RequestParam Long userId) {
    bottleService.collectBottle(bottleId, userId);
    return Result.success("珍藏成功");
  }

  /**
   * 获取我投放的瓶子列表
   */
  @GetMapping("/my/sent")
  public Result<PageInfo<DriftBottle>> getMySentBottles(
      @RequestParam Long userId,
      @RequestParam(defaultValue = "1") Integer pageNum,
      @RequestParam(defaultValue = "10") Integer pageSize) {
    PageInfo<DriftBottle> page = bottleService.getMySentBottles(userId, pageNum, pageSize);
    return Result.success(page);
  }

  /**
   * 获取我珍藏的瓶子列表
   */
  @GetMapping("/my/collected")
  public Result<PageInfo<DriftBottle>> getMyCollectedBottles(
      @RequestParam Long userId,
      @RequestParam(defaultValue = "1") Integer pageNum,
      @RequestParam(defaultValue = "10") Integer pageSize) {
    PageInfo<DriftBottle> page = bottleService.getMyCollectedBottles(userId, pageNum, pageSize);
    return Result.success(page);
  }

  /**
   * 获取瓶子详情
   */
  @GetMapping("/detail/{bottleId}")
  public Result<DriftBottle> getBottleDetail(@PathVariable Long bottleId,
      @RequestParam(required = false) Long userId) {
    DriftBottle bottle = bottleService.getBottleDetail(bottleId, userId);
    return Result.success(bottle);
  }

  /**
   * 删除我的漂流瓶
   */
  @DeleteMapping("/{bottleId}")
  public Result<?> deleteBottle(@PathVariable Long bottleId, @RequestParam Long userId) {
    // [安全加固] 防止前端伪造userId垂直越权删除他人的瓶子
    Long currentUserId = com.example.utils.AuthUtil.getCurrentUserId();
    if (currentUserId != null && !currentUserId.equals(userId)) {
        return Result.error(403, "系统拦截：拦截到非法的越权操作请求。");
    }

    bottleService.deleteBottle(bottleId, userId);
    return Result.success("删除成功");
  }

  /**
   * 获取我的成就列表
   */
  @GetMapping("/achievements")
  public Result<List<BottleAchievement>> getAchievements(@RequestParam Long userId) {
    List<BottleAchievement> list = bottleService.getAchievements(userId);
    return Result.success(list);
  }

  /**
   * 获取用户打捞冷却状态
   */
  @GetMapping("/cooldown-status")
  public Result<Map<String, Object>> getCooldownStatus(@RequestParam Long userId) {
    BottleFishRecord lastRecord = fishRecordMapper.getLastRecord(userId);

    Map<String, Object> result = new HashMap<>();
    if (lastRecord != null) {
      long secondsSinceLastFish = ChronoUnit.SECONDS.between(
          lastRecord.getCreateTime(),
          LocalDateTime.now());
      int remainingSeconds = Math.max(0, 60 - (int) secondsSinceLastFish);
      result.put("remainingSeconds", remainingSeconds);
      result.put("canFish", remainingSeconds == 0);
    } else {
      result.put("remainingSeconds", 0);
      result.put("canFish", true);
    }

    return Result.success(result);
  }

  // ========== 管理员接口 ==========

  /**
   * 管理员获取统计数据
   */
  @GetMapping("/admin/stats")
  public Result<BottleStatsDTO> getStats() {
    BottleStatsDTO stats = bottleService.getStats();
    return Result.success(stats);
  }

  /**
   * 管理员获取瓶子列表
   */
  @GetMapping("/admin/list")
  public Result<PageInfo<DriftBottle>> getAdminBottleList(
      @RequestParam(required = false) Integer status,
      @RequestParam(required = false) Integer direction,
      @RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate,
      @RequestParam(defaultValue = "1") Integer pageNum,
      @RequestParam(defaultValue = "10") Integer pageSize) {
    // 将字符串日期转换为LocalDateTime
    LocalDateTime startDateTime = null;
    LocalDateTime endDateTime = null;
    if (startDate != null && !startDate.isEmpty()) {
      startDateTime = LocalDate.parse(startDate, DateTimeFormatter.ISO_DATE).atStartOfDay();
    }
    if (endDate != null && !endDate.isEmpty()) {
      endDateTime = LocalDate.parse(endDate, DateTimeFormatter.ISO_DATE).atTime(23, 59, 59);
    }
    PageInfo<DriftBottle> page = bottleService.getAdminBottleList(
        status, direction, startDateTime, endDateTime, pageNum, pageSize);
    return Result.success(page);
  }

  /**
   * 管理员删除瓶子
   */
  @DeleteMapping("/admin/{bottleId}")
  public Result<?> adminDeleteBottle(@PathVariable Long bottleId) {
    bottleService.adminDeleteBottle(bottleId);
    return Result.success("删除成功");
  }
}
