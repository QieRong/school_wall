package com.example.service;

import com.example.dto.*;

import java.util.List;

/**
 * 数据大屏服务接口
 */
public interface DashboardService {

  /**
   * 计算平台健康度评分
   */
  HealthScoreDTO calculateHealthScore();

  /**
   * 获取用户统计数据
   */
  UserStatsDTO getUserStats();

  /**
   * 获取用户分层数据
   */
  UserSegmentsDTO getUserSegments();

  /**
   * 获取用户排行榜
   * 
   * @param type 排行类型: likes/reports/comments
   */
  List<UserRankingDTO> getUserRankings(String type);

  /**
   * 获取内容统计数据
   */
  ContentStatsDTO getContentStats();

  /**
   * 获取词云数据
   * 
   * @param days 统计天数
   */
  List<WordCloudDTO> getWordCloud(int days);

  /**
   * 获取举报统计数据
   */
  ReportStatsDTO getReportStats();

  /**
   * 获取安全警报列表
   */
  List<AlertDTO> getAlerts();

  /**
   * 获取24小时统计数据
   */
  HourlyStatsDTO getHourlyStats();

  /**
   * 获取周对比数据
   */
  WeeklyComparisonDTO getWeeklyComparison();
}
