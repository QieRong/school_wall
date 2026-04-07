package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.common.BizException;
import com.example.dto.*;
import com.example.entity.Hotword;
import com.example.entity.HotwordVote;
import com.example.mapper.HotwordDailyQuotaMapper;
import com.example.mapper.HotwordMapper;
import com.example.mapper.HotwordVoteMapper;
import com.example.server.WebSocketServer;
import com.example.service.HotwordService;
import com.example.utils.SensitiveFilter;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class HotwordServiceImpl implements HotwordService {

  @Resource
  private HotwordMapper hotwordMapper;

  @Resource
  private HotwordVoteMapper hotwordVoteMapper;

  @Resource
  private HotwordDailyQuotaMapper quotaMapper;

  @Resource
  private SensitiveFilter sensitiveFilter;

  private static final int DAILY_VOTE_LIMIT = 5;
  private static final int ARCHIVE_DAYS = 30;

  @Override
  @Transactional
  public Hotword createHotword(HotwordCreateDTO dto, Long userId) {
    // 验证输入
    validateInput(dto);

    // 敏感词过滤
    if (sensitiveFilter.hasSensitiveWord(dto.getName()) ||
        sensitiveFilter.hasSensitiveWord(dto.getDefinition()) ||
        (dto.getExample() != null && sensitiveFilter.hasSensitiveWord(dto.getExample()))) {
      throw new BizException("系统检测到违规词汇，系统已拦截");
    }

    Hotword hotword = new Hotword();
    hotword.setName(dto.getName());
    hotword.setDefinition(dto.getDefinition());
    hotword.setExample(dto.getExample());
    hotword.setTags(dto.getTags() != null ? String.join(",", dto.getTags()) : null);
    hotword.setImageUrl(dto.getImageUrl());
    hotword.setUserId(userId);
    hotword.setTotalVotes(0);
    hotword.setStatus(1); // 活跃
    hotword.setIsRecommended(0);
    hotword.setCreateTime(LocalDateTime.now());

    hotwordMapper.insert(hotword);

    // WebSocket 通知新热词上墙
    broadcastNewHotword(hotword);

    return hotword;
  }

  private void validateInput(HotwordCreateDTO dto) {
    if (dto.getName() == null || dto.getName().trim().isEmpty()) {
      throw new BizException("热词名称不能为空");
    }
    if (dto.getName().length() > 10) {
      throw new BizException("热词名称不能超过10个字符");
    }
    if (dto.getDefinition() == null || dto.getDefinition().trim().isEmpty()) {
      throw new BizException("释义不能为空");
    }
    if (dto.getDefinition().length() > 200) {
      throw new BizException("释义不能超过200个字符");
    }
    if (dto.getExample() != null && dto.getExample().length() > 100) {
      throw new BizException("例句不能超过100个字符");
    }
    if (dto.getTags() != null && (dto.getTags().isEmpty() || dto.getTags().size() > 3)) {
      throw new BizException("请选择1-3个标签");
    }
  }

  @Override
  @Transactional
  public VoteResultVO vote(Long hotwordId, Long userId, Integer count) {
    VoteResultVO result = new VoteResultVO();

    // 验证投票数
    if (count != 1 && count != 2) {
      throw new BizException("投票数无效");
    }

    // 检查热词是否存在
    Hotword hotword = hotwordMapper.selectById(hotwordId);
    if (hotword == null) {
      throw new BizException("热词不存在或已被删除");
    }

    // 使用行锁查询配额，防止并发绕过限制
    LocalDate today = LocalDate.now();
    Integer remaining = quotaMapper.selectRemainingVotesForUpdate(userId, today);

    // 如果记录不存在，插入初始配额（仍在事务锁保护下）
    if (remaining == null) {
      quotaMapper.insertOrUpdate(userId, today, DAILY_VOTE_LIMIT);
      remaining = DAILY_VOTE_LIMIT;
    }

    // 在锁保护下检查配额是否足够
    if (remaining < count) {
      throw new BizException("今日投票已用完，明天再来吧~");
    }

    // 原子扣减配额（已在锁保护下）
    int updated = quotaMapper.decreaseVotes(userId, today, count);
    if (updated == 0) {
      throw new BizException("投票失败，请重试");
    }

    // 记录投票
    HotwordVote vote = new HotwordVote();
    vote.setHotwordId(hotwordId);
    vote.setUserId(userId);
    vote.setVoteCount(count);
    vote.setCreateTime(LocalDateTime.now());
    hotwordVoteMapper.insert(vote);

    // 使用原子SQL更新，避免竞态条件
    com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Hotword> updateWrapper = new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
    updateWrapper.eq(Hotword::getId, hotwordId)
        .setSql("total_votes = total_votes + " + count)
        .set(Hotword::getLastVoteTime, LocalDateTime.now());

    // 如果是归档状态，重新激活
    if (hotword.getStatus() == 2) {
      updateWrapper.set(Hotword::getStatus, 1);
    }

    hotwordMapper.update(null, updateWrapper);

    // 重新查询获取最新票数
    hotword = hotwordMapper.selectById(hotwordId);

    // 构建返回结果
    result.setSuccess(true);
    result.setMessage("投票成功");
    result.setTotalVotes(hotword.getTotalVotes());
    result.setRemainingQuota(remaining - count);
    result.setHeatLevel(calculateHeatLevel(hotword.getTotalVotes()));

    // WebSocket 广播投票更新
    broadcastVoteUpdate(hotword);

    return result;
  }

  @Override
  public List<HotwordVO> getHotwordList(Integer status) {
    List<HotwordVO> list = hotwordMapper.selectHotwordList(status);
    list.forEach(vo -> {
      vo.setHeatLevel(calculateHeatLevel(vo.getTotalVotes()));
      if (vo.getTags() != null) {
        vo.setTags(Arrays.asList(vo.getTags().toString().split(",")));
      }
    });
    return list;
  }

  @Override
  public List<RankingVO> getRanking(String type) {
    // 获取当前排名
    List<RankingVO> currentRanking = hotwordMapper.selectRanking(type, 20);

    // 获取上一周期排名用于计算变化
    List<RankingVO> previousRanking = hotwordMapper.selectPreviousRanking(type, 100);
    Map<Long, Integer> prevRankMap = new HashMap<>();
    for (int i = 0; i < previousRanking.size(); i++) {
      prevRankMap.put(previousRanking.get(i).getId(), i + 1);
    }

    // 设置排名和变化
    for (int i = 0; i < currentRanking.size(); i++) {
      RankingVO vo = currentRanking.get(i);
      vo.setRank(i + 1);
      vo.setHeatLevel(calculateHeatLevel(vo.getTotalVotes()));

      Integer prevRank = prevRankMap.get(vo.getId());
      if (prevRank == null) {
        vo.setRankChange(0); // 新上榜
      } else {
        vo.setRankChange(prevRank - (i + 1)); // 正数表示上升
      }
    }

    return currentRanking;
  }

  @Override
  public HotwordDetailVO getDetail(Long id, Integer trendHours) {
    HotwordVO vo = hotwordMapper.selectDetailById(id);
    if (vo == null) {
      throw new BizException("热词不存在");
    }

    HotwordDetailVO detail = new HotwordDetailVO();
    detail.setId(vo.getId());
    detail.setName(vo.getName());
    detail.setDefinition(vo.getDefinition());
    detail.setExample(vo.getExample());
    detail.setImageUrl(vo.getImageUrl());
    detail.setTotalVotes(vo.getTotalVotes());
    detail.setHeatLevel(calculateHeatLevel(vo.getTotalVotes()));
    detail.setIsRecommended(vo.getIsRecommended());
    detail.setAuthorNickname(vo.getAuthorNickname());
    detail.setAuthorAvatar(vo.getAuthorAvatar());
    detail.setUserId(vo.getUserId());
    detail.setCreateTime(vo.getCreateTime());

    // 解析标签
    if (vo.getTags() != null) {
      detail.setTags(Arrays.asList(vo.getTags().toString().split(",")));
    }

    // 获取投票趋势
    int hours = (trendHours != null && trendHours == 168) ? 168 : 24;
    detail.setVoteTrend(hotwordVoteMapper.selectVoteTrend(id, hours));

    // 获取学院分布
    List<Map<String, Object>> distribution = hotwordVoteMapper.selectCollegeDistribution(id);
    Map<String, Integer> collegeMap = new LinkedHashMap<>();
    for (Map<String, Object> item : distribution) {
      collegeMap.put((String) item.get("college"), ((Number) item.get("count")).intValue());
    }
    detail.setCollegeDistribution(collegeMap);

    return detail;
  }

  @Override
  public String calculateHeatLevel(Integer totalVotes) {
    if (totalVotes == null || totalVotes <= 0) {
      return "新芽";
    } else if (totalVotes <= 10) {
      return "新芽";
    } else if (totalVotes <= 50) {
      return "升温";
    } else if (totalVotes <= 200) {
      return "火爆";
    } else {
      return "现象级";
    }
  }

  @Override
  public Integer getRemainingQuota(Long userId) {
    LocalDate today = LocalDate.now();
    Integer remaining = quotaMapper.selectRemainingVotes(userId, today);
    return remaining != null ? remaining : DAILY_VOTE_LIMIT;
  }

  @Override
  public PageInfo<HotwordVO> getMyPosts(Long userId, Integer pageNum, Integer pageSize) {
    PageHelper.startPage(pageNum, pageSize);
    List<HotwordVO> list = hotwordMapper.selectByUserId(userId);
    list.forEach(vo -> vo.setHeatLevel(calculateHeatLevel(vo.getTotalVotes())));
    return new PageInfo<>(list);
  }

  @Override
  public PageInfo<VoteRecordVO> getMyVotes(Long userId, Integer pageNum, Integer pageSize) {
    PageHelper.startPage(pageNum, pageSize);
    List<VoteRecordVO> list = hotwordVoteMapper.selectUserVotes(userId);
    return new PageInfo<>(list);
  }

  @Override
  @Transactional
  public void deleteHotword(Long id, Long userId) {
    Hotword hotword = hotwordMapper.selectById(id);
    if (hotword == null) {
      throw new BizException("热词不存在");
    }
    if (!hotword.getUserId().equals(userId)) {
      throw new BizException("只能删除自己投稿的热词");
    }

    // 删除热词
    hotwordMapper.deleteById(id);

    // 删除相关投票记录
    hotwordVoteMapper.delete(new LambdaQueryWrapper<HotwordVote>()
        .eq(HotwordVote::getHotwordId, id));
  }

  @Override
  public List<HotwordVO> search(String keyword) {
    if (keyword == null || keyword.trim().isEmpty()) {
      return Collections.emptyList();
    }
    List<HotwordVO> list = hotwordMapper.searchHotwords(keyword.trim());
    list.forEach(vo -> vo.setHeatLevel(calculateHeatLevel(vo.getTotalVotes())));
    return list;
  }

  @Override
  @Transactional
  public int archiveExpiredHotwords() {
    LocalDateTime expireTime = LocalDateTime.now().minusDays(ARCHIVE_DAYS);
    List<Long> expiredIds = hotwordMapper.selectExpiredHotwordIds(expireTime);
    if (!expiredIds.isEmpty()) {
      hotwordMapper.batchArchive(expiredIds);
      return expiredIds.size();
    }
    return 0;
  }

  @Override
  public void resetDailyQuota() {
    // 清理7天前的配额记录
    quotaMapper.deleteExpiredQuotas(LocalDate.now().minusDays(7));
  }

  // ========== 管理员功能 ==========

  @Override
  public PageInfo<HotwordVO> getAdminList(Integer status, Integer pageNum, Integer pageSize) {
    PageHelper.startPage(pageNum, pageSize);
    List<HotwordVO> list = hotwordMapper.selectHotwordList(status);
    list.forEach(vo -> vo.setHeatLevel(calculateHeatLevel(vo.getTotalVotes())));
    return new PageInfo<>(list);
  }

  @Override
  @Transactional
  public void adminDelete(Long id) {
    Hotword hotword = hotwordMapper.selectById(id);
    if (hotword == null) {
      throw new BizException("热词不存在");
    }

    // 删除热词
    hotwordMapper.deleteById(id);

    // 删除相关投票记录
    hotwordVoteMapper.delete(new LambdaQueryWrapper<HotwordVote>()
        .eq(HotwordVote::getHotwordId, id));
  }

  @Override
  public void setRecommend(Long id, Boolean recommend) {
    Hotword hotword = hotwordMapper.selectById(id);
    if (hotword == null) {
      throw new BizException("热词不存在");
    }
    hotword.setIsRecommended(recommend ? 1 : 0);
    hotwordMapper.updateById(hotword);
  }

  @Override
  public HotwordStatsVO getStats() {
    HotwordStatsVO stats = new HotwordStatsVO();

    // 总热词数
    stats.setTotalHotwords(hotwordMapper.selectCount(null));

    // 今日新增
    stats.setTodayNew(hotwordMapper.selectCount(new LambdaQueryWrapper<Hotword>()
        .ge(Hotword::getCreateTime, LocalDate.now().atStartOfDay())));

    // 总投票数
    stats.setTotalVotes(hotwordVoteMapper.countTotalVotes());

    // 活跃用户数（今日投票）
    stats.setActiveUsers(hotwordVoteMapper.countTodayActiveUsers());

    // 活跃热词数
    stats.setActiveHotwords(hotwordMapper.selectCount(new LambdaQueryWrapper<Hotword>()
        .eq(Hotword::getStatus, 1)));

    // 归档热词数
    stats.setArchivedHotwords(hotwordMapper.selectCount(new LambdaQueryWrapper<Hotword>()
        .eq(Hotword::getStatus, 2)));

    return stats;
  }

  // ========== WebSocket 广播 ==========

  private void broadcastVoteUpdate(Hotword hotword) {
    try {
      Map<String, Object> data = new HashMap<>();
      data.put("type", "HOTWORD_VOTE_UPDATE");
      data.put("hotwordId", hotword.getId());
      data.put("totalVotes", hotword.getTotalVotes());
      data.put("heatLevel", calculateHeatLevel(hotword.getTotalVotes()));
      WebSocketServer.broadcast(cn.hutool.json.JSONUtil.toJsonStr(data));
    } catch (Exception e) {
      // 忽略广播失败
    }
  }

  private void broadcastNewHotword(Hotword hotword) {
    try {
      Map<String, Object> data = new HashMap<>();
      data.put("type", "HOTWORD_NEW");
      data.put("hotwordId", hotword.getId());
      data.put("name", hotword.getName());
      WebSocketServer.broadcast(cn.hutool.json.JSONUtil.toJsonStr(data));
    } catch (Exception e) {
      // 忽略广播失败
    }
  }

  @Override
  public List<Map<String, Object>> getAbnormalVoters(Integer minVotes, Integer hours) {
    // 默认值：24小时内投票超过10次的用户
    int threshold = (minVotes != null && minVotes > 0) ? minVotes : 10;
    int timeRange = (hours != null && hours > 0) ? hours : 24;
    return hotwordVoteMapper.selectAbnormalVoters(threshold, timeRange);
  }
}
