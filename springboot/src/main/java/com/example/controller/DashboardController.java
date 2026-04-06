package com.example.controller;

import com.example.common.Result;
import com.example.dto.*;
import com.example.service.DashboardService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 数据大屏控制器
 */
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

  @Resource
  private DashboardService dashboardService;

  /**
   * 获取平台健康度评分
   */
  @GetMapping("/health-score")
  public Result<HealthScoreDTO> getHealthScore() {
    return Result.success(dashboardService.calculateHealthScore());
  }

  /**
   * 获取用户统计数据
   */
  @GetMapping("/user-stats")
  public Result<UserStatsDTO> getUserStats() {
    return Result.success(dashboardService.getUserStats());
  }

  /**
   * 获取用户分层数据
   */
  @GetMapping("/user-segments")
  public Result<UserSegmentsDTO> getUserSegments() {
    return Result.success(dashboardService.getUserSegments());
  }

  /**
   * 获取用户排行榜
   * 
   * @param type 排行类型: likes/reports/comments
   */
  @GetMapping("/user-rankings")
  public Result<List<UserRankingDTO>> getUserRankings(
      @RequestParam(defaultValue = "likes") String type) {
    return Result.success(dashboardService.getUserRankings(type));
  }

  /**
   * 获取内容统计数据
   */
  @GetMapping("/content-stats")
  public Result<ContentStatsDTO> getContentStats() {
    return Result.success(dashboardService.getContentStats());
  }

  /**
   * 获取举报统计数据
   */
  @GetMapping("/report-stats")
  public Result<ReportStatsDTO> getReportStats() {
    return Result.success(dashboardService.getReportStats());
  }

  /**
   * 获取安全警报列表
   */
  @GetMapping("/alerts")
  public Result<List<AlertDTO>> getAlerts() {
    return Result.success(dashboardService.getAlerts());
  }

  /**
   * 获取24小时统计数据
   */
  @GetMapping("/hourly-stats")
  public Result<HourlyStatsDTO> getHourlyStats() {
    return Result.success(dashboardService.getHourlyStats());
  }

  /**
   * 获取词云数据
   * 
   * @param days 统计天数，默认7天
   */
  @GetMapping("/word-cloud")
  public Result<List<WordCloudDTO>> getWordCloud(
      @RequestParam(defaultValue = "7") Integer days) {
    return Result.success(dashboardService.getWordCloud(days));
  }

  /**
   * 获取周对比数据
   */
  @GetMapping("/weekly-comparison")
  public Result<WeeklyComparisonDTO> getWeeklyComparison() {
    return Result.success(dashboardService.getWeeklyComparison());
  }
}
