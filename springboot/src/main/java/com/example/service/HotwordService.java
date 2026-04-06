package com.example.service;

import com.example.dto.*;
import com.example.entity.Hotword;
import com.github.pagehelper.PageInfo;

import java.util.List;
import java.util.Map;

public interface HotwordService {

  /**
   * 创建热词
   */
  Hotword createHotword(HotwordCreateDTO dto, Long userId);

  /**
   * 投票
   * 
   * @param hotwordId 热词ID
   * @param userId    用户ID
   * @param count     投票数（1或2）
   */
  VoteResultVO vote(Long hotwordId, Long userId, Integer count);

  /**
   * 获取热词列表
   * 
   * @param status 状态：1-活跃，2-归档，null-全部
   */
  List<HotwordVO> getHotwordList(Integer status);

  /**
   * 获取榜单
   * 
   * @param type day/week/month/all
   */
  List<RankingVO> getRanking(String type);

  /**
   * 获取热词详情
   */
  HotwordDetailVO getDetail(Long id, Integer trendHours);

  /**
   * 计算热度等级
   */
  String calculateHeatLevel(Integer totalVotes);

  /**
   * 获取用户剩余票数
   */
  Integer getRemainingQuota(Long userId);

  /**
   * 获取用户投稿的热词
   */
  PageInfo<HotwordVO> getMyPosts(Long userId, Integer pageNum, Integer pageSize);

  /**
   * 获取用户投票记录
   */
  PageInfo<VoteRecordVO> getMyVotes(Long userId, Integer pageNum, Integer pageSize);

  /**
   * 删除热词（仅限作者）
   */
  void deleteHotword(Long id, Long userId);

  /**
   * 搜索热词
   */
  List<HotwordVO> search(String keyword);

  /**
   * 归档过期热词（30天无投票）
   * 
   * @return 归档的热词数量
   */
  int archiveExpiredHotwords();

  /**
   * 重置每日投票配额
   */
  void resetDailyQuota();

  // ========== 管理员功能 ==========

  /**
   * 管理员获取热词列表
   */
  PageInfo<HotwordVO> getAdminList(Integer status, Integer pageNum, Integer pageSize);

  /**
   * 管理员删除热词
   */
  void adminDelete(Long id);

  /**
   * 设置官方推荐
   */
  void setRecommend(Long id, Boolean recommend);

  /**
   * 获取统计数据
   */
  HotwordStatsVO getStats();

  /**
   * 获取异常投票预警列表
   * 
   * @param minVotes 最小投票次数阈值（默认10）
   * @param hours    时间范围小时数（默认24）
   */
  List<Map<String, Object>> getAbnormalVoters(Integer minVotes, Integer hours);
}
