// File: springboot/src/main/java/com/example/service/BottleService.java
package com.example.service;

import com.example.dto.BottleCreateDTO;
import com.example.dto.BottleReplyDTO;
import com.example.dto.BottleStatsDTO;
import com.example.dto.FishResult;
import com.example.entity.BottleAchievement;
import com.example.entity.DriftBottle;
import com.github.pagehelper.PageInfo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 漂流瓶服务接口
 */
public interface BottleService {

  /**
   * 投放漂流瓶
   */
  void throwBottle(BottleCreateDTO dto);

  /**
   * 打捞漂流瓶
   */
  FishResult fishBottle(Long userId);

  /**
   * 回复漂流瓶
   */
  void replyBottle(BottleReplyDTO dto);

  /**
   * 放回漂流瓶(不回复)
   */
  void releaseBottle(Long bottleId);

  /**
   * 珍藏漂流瓶
   */
  void collectBottle(Long bottleId, Long userId);

  /**
   * 获取用户投放的瓶子列表
   */
  PageInfo<DriftBottle> getMySentBottles(Long userId, Integer pageNum, Integer pageSize);

  /**
   * 获取用户珍藏的瓶子列表
   */
  PageInfo<DriftBottle> getMyCollectedBottles(Long userId, Integer pageNum, Integer pageSize);

  /**
   * 获取瓶子详情(带回复)
   */
  DriftBottle getBottleDetail(Long bottleId, Long userId);

  /**
   * 删除漂流瓶
   */
  void deleteBottle(Long bottleId, Long userId);

  /**
   * 获取用户成就列表
   */
  List<BottleAchievement> getAchievements(Long userId);

  /**
   * 清理过期漂流瓶(定时任务调用)
   * 
   * @return 清理的瓶子数量
   */
  int cleanupExpiredBottles();

  /**
   * 获取统计数据(管理员)
   */
  BottleStatsDTO getStats();

  /**
   * 管理员获取瓶子列表
   */
  PageInfo<DriftBottle> getAdminBottleList(Integer status, Integer direction,
      LocalDateTime startDate, LocalDateTime endDate,
      Integer pageNum, Integer pageSize);

  /**
   * 管理员删除瓶子
   */
  void adminDeleteBottle(Long bottleId);
}
