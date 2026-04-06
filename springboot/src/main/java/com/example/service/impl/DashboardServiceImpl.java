package com.example.service.impl;

import com.example.dto.*;
import com.example.mapper.DashboardMapper;
import com.example.service.DashboardService;
import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据大屏服务实现类
 */
@Service
public class DashboardServiceImpl implements DashboardService {

  @Resource
  private DashboardMapper dashboardMapper;

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  @Override
  public HealthScoreDTO calculateHealthScore() {
    HealthScoreDTO dto = new HealthScoreDTO();

    // 获取基础数据
    Integer totalUsers = dashboardMapper.countTotalUsers();
    Integer todayActive = dashboardMapper.countTodayActiveUsers();
    Integer todayAudited = dashboardMapper.countTodayAudited();
    Integer todayPassed = dashboardMapper.countTodayPassed();
    Integer todayPosts = dashboardMapper.countTodayPosts();
    Integer yesterdayPosts = dashboardMapper.countYesterdayPosts();
    Integer todayInteractions = dashboardMapper.countTodayInteractions();
    Integer yesterdayInteractions = dashboardMapper.countYesterdayInteractions();

    // 计算各项指标 (0-100)
    // 日活率：今日活跃用户/总用户 * 100，上限100
    double dauRate = totalUsers > 0 ? Math.min(100, (todayActive * 100.0 / totalUsers)) : 0;

    // 发帖率：今日发帖量相对昨日的表现，基准50分，增长则加分，下降则减分
    double postRate = calculateGrowthRate(todayPosts, yesterdayPosts);

    // 互动率：今日互动量相对昨日的表现
    double interactionRate = calculateGrowthRate(todayInteractions, yesterdayInteractions);

    // 审核通过率
    double auditPassRate = todayAudited > 0 ? (todayPassed * 100.0 / todayAudited) : 100;

    dto.setDauRate(dauRate);
    dto.setPostRate(postRate);
    dto.setInteractionRate(interactionRate);
    dto.setAuditPassRate(auditPassRate);

    // 加权计算健康度评分: 日活率30% + 发帖率25% + 互动率25% + 审核通过率20%
    int score = calculateWeightedScore(dauRate, postRate, interactionRate, auditPassRate);
    dto.setScore(score);

    // 设置状态
    dto.setStatus(getHealthStatus(score));

    return dto;
  }

  /**
   * 计算增长率得分（0-100）
   * 基准50分，增长则加分（最高100），下降则减分（最低0）
   */
  private double calculateGrowthRate(Integer today, Integer yesterday) {
    if (yesterday == null || yesterday == 0) {
      return today != null && today > 0 ? 75 : 50; // 无历史数据时给基准分或稍高
    }
    double growthPercent = ((double) (today - yesterday) / yesterday) * 100;
    // 增长50%以上得100分，下降50%以上得0分，线性映射
    double score = 50 + growthPercent;
    return Math.max(0, Math.min(100, score));
  }

  /**
   * 计算加权健康度评分
   * 权重: 日活率30% + 发帖率25% + 互动率25% + 审核通过率20%
   */
  public int calculateWeightedScore(double dauRate, double postRate, double interactionRate, double auditPassRate) {
    double score = dauRate * 0.30 + postRate * 0.25 + interactionRate * 0.25 + auditPassRate * 0.20;
    return Math.max(0, Math.min(100, (int) Math.round(score)));
  }

  /**
   * 根据评分返回健康状态
   */
  public String getHealthStatus(int score) {
    if (score < 60) {
      return "red";
    } else if (score <= 80) {
      return "yellow";
    } else {
      return "green";
    }
  }

  @Override
  public UserStatsDTO getUserStats() {
    UserStatsDTO dto = new UserStatsDTO();

    dto.setTotal(dashboardMapper.countTotalUsers());
    dto.setTodayNew(dashboardMapper.countTodayNewUsers());
    dto.setTodayActive(dashboardMapper.countTodayActiveUsers());

    // 7天趋势
    List<Map<String, Object>> trendData = dashboardMapper.selectUserTrend(7);
    List<TrendPoint> trend = trendData.stream()
        .map(m -> new TrendPoint(
            m.get("date").toString(),
            ((Number) m.get("value")).intValue()))
        .collect(Collectors.toList());
    dto.setTrend(trend);

    // 信誉分分布
    List<Map<String, Object>> creditData = dashboardMapper.selectCreditDistribution();
    Map<String, Integer> creditDistribution = new LinkedHashMap<>();
    for (Map<String, Object> item : creditData) {
      String segment = (String) item.get("segment");
      Integer count = ((Number) item.get("count")).intValue();
      creditDistribution.put(segment, count);
    }
    dto.setCreditDistribution(creditDistribution);

    return dto;
  }

  @Override
  public UserSegmentsDTO getUserSegments() {
    UserSegmentsDTO dto = new UserSegmentsDTO();

    Integer totalUsers = dashboardMapper.countTotalUsers();
    Integer creators = dashboardMapper.countCreators(5); // 发帖>=5
    Integer interactors = dashboardMapper.countInteractors(10); // 评论>=10

    dto.setCreators(creators);
    dto.setInteractors(interactors);
    dto.setLurkers(Math.max(0, totalUsers - creators - interactors));

    return dto;
  }

  @Override
  public List<UserRankingDTO> getUserRankings(String type) {
    List<Map<String, Object>> data;
    switch (type) {
      case "likes":
        data = dashboardMapper.selectTopLikedUsers(10);
        break;
      case "reports":
        data = dashboardMapper.selectTopReporters(10);
        break;
      case "comments":
        data = dashboardMapper.selectTopCommenters(10);
        break;
      default:
        data = dashboardMapper.selectTopLikedUsers(10);
    }

    return data.stream().map(m -> {
      UserRankingDTO dto = new UserRankingDTO();
      dto.setUserId(((Number) m.get("userId")).longValue());
      dto.setNickname((String) m.get("nickname"));
      dto.setAvatar((String) m.get("avatar"));
      dto.setValue(((Number) m.get("value")).intValue());
      return dto;
    }).collect(Collectors.toList());
  }

  @Override
  public ContentStatsDTO getContentStats() {
    ContentStatsDTO dto = new ContentStatsDTO();

    dto.setPendingAudit(dashboardMapper.countPendingAudit());
    dto.setTodayAudited(dashboardMapper.countTodayAudited());

    Integer todayPassed = dashboardMapper.countTodayPassed();
    Integer todayAudited = dto.getTodayAudited();
    dto.setPassRate(todayAudited > 0 ? (todayPassed * 100.0 / todayAudited) : 100.0);

    // 敏感词统计 - 如果表不存在则返回空列表
    try {
      List<Map<String, Object>> sensitiveData = dashboardMapper.selectSensitiveWordStats(7, 10);
      List<SensitiveWordStat> sensitiveWords = sensitiveData.stream()
          .map(m -> new SensitiveWordStat(
              (String) m.get("word"),
              ((Number) m.get("count")).intValue()))
          .collect(Collectors.toList());
      dto.setSensitiveWords(sensitiveWords);
    } catch (Exception e) {
      dto.setSensitiveWords(new ArrayList<>());
    }

    return dto;
  }

  // 停用词列表
  private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
      "的", "是", "了", "在", "和", "有", "我", "你", "他", "她", "它", "们",
      "这", "那", "就", "也", "都", "要", "会", "可以", "不", "吗", "呢", "啊",
      "吧", "呀", "哦", "哈", "嗯", "好", "很", "太", "真", "还", "又", "再",
      "把", "被", "让", "给", "从", "到", "为", "与", "及", "或", "但", "而",
      "如果", "因为", "所以", "虽然", "但是", "然后", "之后", "之前", "一个",
      "什么", "怎么", "为什么", "哪里", "谁", "多少", "几", "个", "些", "点"));

  @Override
  public List<WordCloudDTO> getWordCloud(int days) {
    try {
      // 获取最近N天的帖子内容
      List<Map<String, Object>> posts = dashboardMapper.selectPostContents(days);

      // 词频统计
      Map<String, Integer> wordCount = new HashMap<>();

      for (Map<String, Object> post : posts) {
        String content = (String) post.get("content");
        if (content == null || content.isEmpty())
          continue;

        // 使用 HanLP 分词
        List<Term> terms = HanLP.segment(content);
        for (Term term : terms) {
          String word = term.word;
          // 过滤：长度>=2，不是停用词，不是纯数字/标点
          if (word.length() >= 2 && !STOP_WORDS.contains(word) && isValidWord(word)) {
            wordCount.merge(word, 1, (a, b) -> a + b);
          }
        }
      }

      // 按词频降序排序，取前100个
      return wordCount.entrySet().stream()
          .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
          .limit(100)
          .map(e -> new WordCloudDTO(e.getKey(), e.getValue()))
          .collect(Collectors.toList());

    } catch (Exception e) {
      return new ArrayList<>();
    }
  }

  /**
   * 判断是否为有效词汇（非纯数字/标点）
   */
  private boolean isValidWord(String word) {
    return word.matches(".*[\\u4e00-\\u9fa5a-zA-Z]+.*");
  }

  @Override
  public ReportStatsDTO getReportStats() {
    ReportStatsDTO dto = new ReportStatsDTO();

    dto.setPending(dashboardMapper.countPendingReports());
    dto.setTodayNew(dashboardMapper.countTodayReports());

    Integer handled = dashboardMapper.countHandledReports();
    Integer total = dto.getPending() + handled;
    dto.setHandleRate(total > 0 ? (handled * 100.0 / total) : 100.0);

    // 举报类型分布
    List<Map<String, Object>> distData = dashboardMapper.selectReportDistribution();
    Map<String, Integer> distribution = new LinkedHashMap<>();
    for (Map<String, Object> item : distData) {
      String type = (String) item.get("type");
      Integer count = ((Number) item.get("count")).intValue();
      distribution.put(type != null ? type : "其他", count);
    }
    dto.setDistribution(distribution);

    return dto;
  }

  @Override
  public List<AlertDTO> getAlerts() {
    List<AlertDTO> alerts = new ArrayList<>();

    // 检测高举报帖子 (5分钟内超过3条举报)
    List<Map<String, Object>> highReportPosts = dashboardMapper.selectHighReportPosts(5, 3);
    for (Map<String, Object> item : highReportPosts) {
      Long postId = ((Number) item.get("postId")).longValue();
      Integer reportCount = ((Number) item.get("reportCount")).intValue();
      alerts.add(new AlertDTO(
          "HIGH_REPORT",
          "red",
          "帖子ID:" + postId + " 在5分钟内收到" + reportCount + "条举报",
          postId));
    }

    // 检测刷屏用户 (10分钟内超过5条帖子)
    List<Map<String, Object>> spamUsers = dashboardMapper.selectSpamUsers(10, 5);
    for (Map<String, Object> item : spamUsers) {
      Long userId = ((Number) item.get("userId")).longValue();
      Integer postCount = ((Number) item.get("postCount")).intValue();
      alerts.add(new AlertDTO(
          "SPAM",
          "yellow",
          "用户ID:" + userId + " 在10分钟内发布了" + postCount + "条帖子",
          userId));
    }

    // 检测审核积压 (待审核超过50条)
    Integer pendingAudit = dashboardMapper.countPendingAudit();
    if (pendingAudit > 50) {
      alerts.add(new AlertDTO(
          "QUEUE_OVERFLOW",
          "orange",
          "待审核队列积压" + pendingAudit + "条内容",
          null));
    }

    return alerts;
  }

  @Override
  public HourlyStatsDTO getHourlyStats() {
    HourlyStatsDTO dto = new HourlyStatsDTO();

    // 初始化24小时数组
    List<Integer> posts = new ArrayList<>(Collections.nCopies(24, 0));
    List<Integer> comments = new ArrayList<>(Collections.nCopies(24, 0));

    // 填充发帖数据
    List<Map<String, Object>> hourlyPosts = dashboardMapper.selectHourlyPosts();
    for (Map<String, Object> item : hourlyPosts) {
      Integer hour = ((Number) item.get("hour")).intValue();
      Integer count = ((Number) item.get("count")).intValue();
      if (hour >= 0 && hour < 24) {
        posts.set(hour, count);
      }
    }

    // 填充评论数据
    List<Map<String, Object>> hourlyComments = dashboardMapper.selectHourlyComments();
    for (Map<String, Object> item : hourlyComments) {
      Integer hour = ((Number) item.get("hour")).intValue();
      Integer count = ((Number) item.get("count")).intValue();
      if (hour >= 0 && hour < 24) {
        comments.set(hour, count);
      }
    }

    dto.setPosts(posts);
    dto.setComments(comments);

    // 计算高峰时段
    dto.setPostPeakHour(findPeakHour(posts));
    dto.setCommentPeakHour(findPeakHour(comments));

    return dto;
  }

  /**
   * 找出数组中最大值对应的索引（高峰时段）
   */
  private Integer findPeakHour(List<Integer> data) {
    int maxIndex = 0;
    int maxValue = 0;
    for (int i = 0; i < data.size(); i++) {
      if (data.get(i) > maxValue) {
        maxValue = data.get(i);
        maxIndex = i;
      }
    }
    return maxIndex;
  }

  @Override
  public WeeklyComparisonDTO getWeeklyComparison() {
    WeeklyComparisonDTO dto = new WeeklyComparisonDTO();

    LocalDate today = LocalDate.now();

    // 本周起止日期 (周一到今天)
    LocalDate thisWeekStart = today.with(DayOfWeek.MONDAY);
    String thisStart = thisWeekStart.format(DATE_FORMATTER);
    String thisEnd = today.format(DATE_FORMATTER);

    // 上周起止日期
    LocalDate lastWeekStart = thisWeekStart.minusWeeks(1);
    LocalDate lastWeekEnd = thisWeekStart.minusDays(1);
    String lastStart = lastWeekStart.format(DATE_FORMATTER);
    String lastEnd = lastWeekEnd.format(DATE_FORMATTER);

    // 发帖量对比
    Integer currentPosts = dashboardMapper.countPostsBetween(thisStart, thisEnd);
    Integer previousPosts = dashboardMapper.countPostsBetween(lastStart, lastEnd);
    dto.setPosts(new ComparisonItem(currentPosts, previousPosts));

    // 日活对比
    Integer currentDau = dashboardMapper.countActiveUsersBetween(thisStart, thisEnd);
    Integer previousDau = dashboardMapper.countActiveUsersBetween(lastStart, lastEnd);
    dto.setDau(new ComparisonItem(currentDau, previousDau));

    // 互动量对比
    Integer currentInteractions = dashboardMapper.countInteractionsBetween(thisStart, thisEnd);
    Integer previousInteractions = dashboardMapper.countInteractionsBetween(lastStart, lastEnd);
    dto.setInteractions(new ComparisonItem(currentInteractions, previousInteractions));

    return dto;
  }
}
