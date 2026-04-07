// File: springboot/src/main/java/com/example/service/impl/BottleServiceImpl.java
package com.example.service.impl;

import com.example.common.BizException;
import com.example.dto.BottleCreateDTO;
import com.example.dto.BottleReplyDTO;
import com.example.dto.BottleStatsDTO;
import com.example.dto.FishResult;
import com.example.entity.*;
import com.example.mapper.*;
import com.example.service.BottleService;
import com.example.utils.SensitiveFilter;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 漂流瓶服务实现类
 */
@Service
public class BottleServiceImpl implements BottleService {

  /** 打捞冷却时间(秒) */
  private static final int FISH_COOLDOWN_SECONDS = 60;

  /** 每日珍藏限制 */
  private static final int DAILY_COLLECT_LIMIT = 1;

  /** 瓶子有效期(天) */
  private static final int BOTTLE_LIFESPAN_DAYS = 7;

  @Resource
  private BottleMapper bottleMapper;

  @Resource
  private BottleReplyMapper replyMapper;

  @Resource
  private BottleCollectionMapper collectionMapper;

  @Resource
  private BottleFishRecordMapper fishRecordMapper;

  @Resource
  private BottleAchievementMapper achievementMapper;

  @Resource
  private NoticeMapper noticeMapper;

  @Resource
  private SensitiveFilter sensitiveFilter;

  @Override
  @Transactional
  public void throwBottle(BottleCreateDTO dto) {
    // 1. 检查投放冷却（5分钟）
    DriftBottle lastBottle = bottleMapper.getLastBottleByUser(dto.getUserId());
    if (lastBottle != null) {
      long secondsSinceLastThrow = ChronoUnit.SECONDS.between(lastBottle.getCreateTime(), LocalDateTime.now());
      if (secondsSinceLastThrow < 300) { // 5分钟 = 300秒
        int remainingSeconds = (int) (300 - secondsSinceLastThrow);
        throw new BizException("投放冷却中，请" + remainingSeconds + "秒后再试");
      }
    }

    // 2. 内容校验
    String content = dto.getContent();
    if (content == null || content.trim().isEmpty()) {
      throw new BizException("内容不能为空");
    }
    content = content.trim();
    if (content.length() > 200) {
      throw new BizException("内容不能超过200字");
    }

    // 3. 敏感词过滤
    String filteredContent = sensitiveFilter.filter(content);
    boolean isFlagged = !filteredContent.equals(content);
    if (isFlagged) {
      throw new BizException("系统检测到违规词汇，系统已拦截");
    }

    // 4. 方向校验
    Integer direction = dto.getDirection();
    if (direction == null || direction < 1 || direction > 3) {
      direction = DriftBottle.DIRECTION_SAKURA; // 默认樱花海岸
    }

    // 5. 创建漂流瓶
    DriftBottle bottle = new DriftBottle();
    bottle.setUserId(dto.getUserId());
    bottle.setContent(filteredContent);
    bottle.setDirection(direction);
    bottle.setStatus(DriftBottle.STATUS_DRIFTING);
    bottle.setViewCount(0);
    bottle.setIsFlagged(isFlagged ? 1 : 0);
    bottle.setIsAnonymous(dto.getIsAnonymous() != null ? dto.getIsAnonymous() : 0);
    bottle.setCreateTime(LocalDateTime.now());
    bottle.setExpireTime(LocalDateTime.now().plusDays(BOTTLE_LIFESPAN_DAYS));

    bottleMapper.insert(bottle);
  }

  @Override
  @Transactional
  public FishResult fishBottle(Long userId) {
    FishResult result = new FishResult();

    // 使用数据库行锁防止并发绕过冷却检查
    BottleFishRecord lastRecord = fishRecordMapper.getLastRecordForUpdate(userId);

    if (lastRecord != null) {
      long secondsSinceLastFish = ChronoUnit.SECONDS.between(lastRecord.getCreateTime(), LocalDateTime.now());
      if (secondsSinceLastFish < FISH_COOLDOWN_SECONDS) {
        result.setSuccess(false);
        result.setCooldownSeconds((int) (FISH_COOLDOWN_SECONDS - secondsSinceLastFish));
        result.setMessage("打捞冷却中，请" + result.getCooldownSeconds() + "秒后再试");
        return result;
      }
    }

    // 随机获取一个漂流中的瓶子（排除自己的、已被珍藏的）
    DriftBottle bottle = bottleMapper.getRandomBottleExcludeUser(userId);
    if (bottle == null) {
      result.setSuccess(false);
      result.setMessage("海洋中暂无漂流瓶，稍后再来吧");
      return result;
    }

    // 递增浏览次数，并将状态更新为"已被捞起"(STATUS_OPENED=1)
    // 注意：打捞筛选条件为 status NOT IN (2,3)，所以已被捞起的瓶子仍可再次被其他人打捞
    // 只有当用户主动"珍藏"时才锁定为 STATUS_COLLECTED(2)，不再参与漂流
    bottle.setViewCount((bottle.getViewCount() == null ? 0 : bottle.getViewCount()) + 1);
    bottle.setStatus(DriftBottle.STATUS_OPENED);
    bottleMapper.updateById(bottle);

    // 记录打捞轨迹
    BottleFishRecord record = new BottleFishRecord();
    record.setUserId(userId);
    record.setBottleId(bottle.getId());
    record.setCreateTime(LocalDateTime.now());
    fishRecordMapper.insert(record);

    // 检查成就
    checkFishAchievements(userId);

    result.setSuccess(true);
    result.setBottle(bottle);
    result.setCooldownSeconds(FISH_COOLDOWN_SECONDS); // 明确告诉前端冷却时间
    result.setMessage("成功打捞到一个漂流瓶！");
    return result;
  }

  @Override
  @Transactional
  public void replyBottle(BottleReplyDTO dto) {
    // 1. 内容校验
    String content = dto.getContent();
    if (content == null || content.trim().isEmpty()) {
      throw new BizException("回复内容不能为空");
    }
    content = content.trim();
    if (content.length() > 50) {
      throw new BizException("回复内容不能超过50字");
    }

    // 2. 检查瓶子是否存在
    DriftBottle bottle = bottleMapper.selectById(dto.getBottleId());
    if (bottle == null) {
      throw new BizException("漂流瓶不存在");
    }

    // 3. 敏感词过滤
    String filteredContent = sensitiveFilter.filter(content);
    if (!filteredContent.equals(content)) {
      throw new BizException("系统检测到违规词汇，系统已拦截");
    }

    // 4. 创建回复
    BottleReply reply = new BottleReply();
    reply.setBottleId(dto.getBottleId());
    reply.setUserId(dto.getUserId());
    reply.setContent(filteredContent);
    reply.setCreateTime(LocalDateTime.now());
    replyMapper.insert(reply);

    // 5. 将瓶子放回海洋
    bottle.setStatus(DriftBottle.STATUS_DRIFTING);
    bottleMapper.updateById(bottle);

    // 6. 通知原瓶主人
    if (!bottle.getUserId().equals(dto.getUserId())) {
      SysNotice notice = new SysNotice();
      notice.setReceiverId(bottle.getUserId());
      notice.setSenderId(dto.getUserId());
      notice.setContent("有人回复了你的漂流瓶");
      notice.setType(1); // 系统通知
      notice.setIsRead(0);
      notice.setRelatedId(bottle.getId());
      notice.setCreateTime(LocalDateTime.now());
      noticeMapper.insert(notice);

      // 检查瓶主的回复成就
      checkReplyAchievements(bottle.getUserId());
    }
  }

  @Override
  public void releaseBottle(Long bottleId) {
    // 确认瓶子存在即可（打捞后不改变 status，瓶子始终漂流中，"放回"是幂等操作）
    DriftBottle bottle = bottleMapper.selectById(bottleId);
    if (bottle == null) {
      throw new BizException("漂流瓶不存在");
    }
    // 不做任何状态变更，瓶子依然漂流中，可被其他人打捞
  }

  @Override
  @Transactional
  public void collectBottle(Long bottleId, Long userId) {
    // 1. 检查今日珍藏次数
    int todayCount = collectionMapper.countTodayCollections(userId);
    if (todayCount >= DAILY_COLLECT_LIMIT) {
      throw new BizException("今日珍藏次数已用完，明天再来吧");
    }

    // 2. 检查瓶子是否存在
    DriftBottle bottle = bottleMapper.selectById(bottleId);
    if (bottle == null) {
      throw new BizException("漂流瓶不存在");
    }

    // 3. 检查是否已珍藏
    if (collectionMapper.checkCollected(userId, bottleId) != null) {
      throw new BizException("你已经珍藏过这个瓶子了");
    }

    // 4. 创建珍藏记录
    BottleCollection collection = new BottleCollection();
    collection.setUserId(userId);
    collection.setBottleId(bottleId);
    collection.setCreateTime(LocalDateTime.now());
    collectionMapper.insert(collection);

    // 5. 更新瓶子状态
    bottle.setStatus(DriftBottle.STATUS_COLLECTED);
    bottleMapper.updateById(bottle);

    // 6. 检查珍藏成就
    checkCollectAchievements(userId);
  }

  @Override
  public PageInfo<DriftBottle> getMySentBottles(Long userId, Integer pageNum, Integer pageSize) {
    PageHelper.startPage(pageNum, pageSize);
    List<DriftBottle> list = bottleMapper.getMySentBottles(userId);
    return new PageInfo<>(list);
  }

  @Override
  public PageInfo<DriftBottle> getMyCollectedBottles(Long userId, Integer pageNum, Integer pageSize) {
    PageHelper.startPage(pageNum, pageSize);
    List<DriftBottle> list = bottleMapper.getMyCollectedBottles(userId);
    return new PageInfo<>(list);
  }

  @Override
  public DriftBottle getBottleDetail(Long bottleId, Long userId) {
    DriftBottle bottle = bottleMapper.getBottleWithReplies(bottleId);
    if (bottle == null) {
      throw new BizException("漂流瓶不存在");
    }
    // 补充查询回复列表（含回复者昵称和头像），填充非数据库字段 replies
    List<BottleReply> replies = replyMapper.selectByBottleIdWithUser(bottleId);
    bottle.setReplies(replies);
    return bottle;
  }

  @Override
  @Transactional
  public void deleteBottle(Long bottleId, Long userId) {
    DriftBottle bottle = bottleMapper.selectById(bottleId);
    if (bottle == null) {
      throw new BizException("漂流瓶不存在");
    }
    if (!bottle.getUserId().equals(userId)) {
      throw new BizException("只能删除自己的漂流瓶");
    }
    // 级联删除由数据库外键处理
    bottleMapper.deleteById(bottleId);
  }

  @Override
  public List<BottleAchievement> getAchievements(Long userId) {
    return achievementMapper.getByUserId(userId);
  }

  @Override
  @Transactional
  public int cleanupExpiredBottles() {
    // 只清理状态为"漂流中"且已过期的瓶子
    return bottleMapper.updateExpiredBottles();
  }

  @Override
  public BottleStatsDTO getStats() {
    BottleStatsDTO stats = new BottleStatsDTO();
    stats.setTotalCount(bottleMapper.countTotal());
    stats.setActiveCount(bottleMapper.countByStatus(DriftBottle.STATUS_DRIFTING));
    stats.setTodayNewCount(bottleMapper.countTodayNew());
    stats.setTotalFishCount(fishRecordMapper.countTotal());
    stats.setSunkenCount(bottleMapper.countByStatus(DriftBottle.STATUS_SUNKEN));
    return stats;
  }

  @Override
  public PageInfo<DriftBottle> getAdminBottleList(Integer status, Integer direction,
      LocalDateTime startDate, LocalDateTime endDate,
      Integer pageNum, Integer pageSize) {
    PageHelper.startPage(pageNum, pageSize);
    List<DriftBottle> list = bottleMapper.getAdminBottleList(status, direction, startDate, endDate);
    return new PageInfo<>(list);
  }

  @Override
  @Transactional
  public void adminDeleteBottle(Long bottleId) {
    DriftBottle bottle = bottleMapper.selectById(bottleId);
    if (bottle == null) {
      throw new BizException("漂流瓶不存在");
    }
    // 级联删除由数据库外键处理
    bottleMapper.deleteById(bottleId);
  }

  // ========== 私有方法：成就检查 ==========

  private void checkFishAchievements(Long userId) {
    // 检查首次打捞成就
    if (!hasAchievement(userId, BottleAchievement.TYPE_FIRST_FISH)) {
      awardAchievement(userId, BottleAchievement.TYPE_FIRST_FISH);
    }

    // 检查打捞50次成就
    if (!hasAchievement(userId, BottleAchievement.TYPE_FISH_50)) {
      long fishCount = fishRecordMapper.countByUserId(userId);
      if (fishCount >= 50) {
        awardAchievement(userId, BottleAchievement.TYPE_FISH_50);
      }
    }
  }

  private void checkReplyAchievements(Long userId) {
    // 检查收到10次回复成就
    if (!hasAchievement(userId, BottleAchievement.TYPE_REPLY_10)) {
      long replyCount = replyMapper.countRepliesReceived(userId);
      if (replyCount >= 10) {
        awardAchievement(userId, BottleAchievement.TYPE_REPLY_10);
      }
    }
  }

  private void checkCollectAchievements(Long userId) {
    // 检查珍藏30个成就
    if (!hasAchievement(userId, BottleAchievement.TYPE_COLLECT_30)) {
      long collectCount = collectionMapper.countByUserId(userId);
      if (collectCount >= 30) {
        awardAchievement(userId, BottleAchievement.TYPE_COLLECT_30);
      }
    }
  }

  private boolean hasAchievement(Long userId, String type) {
    return achievementMapper.checkAchievement(userId, type) != null;
  }

  private void awardAchievement(Long userId, String type) {
    BottleAchievement achievement = new BottleAchievement();
    achievement.setUserId(userId);
    achievement.setAchievementType(type);
    achievement.setCreateTime(LocalDateTime.now());
    achievementMapper.insert(achievement);

    // 发送成就通知
    SysNotice notice = new SysNotice();
    notice.setReceiverId(userId);
    notice.setSenderId(0L);
    notice.setContent("恭喜获得成就：" + achievement.getAchievementName());
    notice.setType(1);
    notice.setIsRead(0);
    notice.setCreateTime(LocalDateTime.now());
    noticeMapper.insert(notice);
  }
}
