package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.common.BizException;
import com.example.dto.*;
import com.example.entity.*;
import com.example.mapper.*;
import com.example.server.WebSocketServer;
import com.example.service.StoryService;
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
 * 故事链服务实现类
 */
@Service
public class StoryServiceImpl implements StoryService {

    @Resource
    private StoryMapper storyMapper;
    @Resource
    private StoryParagraphMapper paragraphMapper;
    @Resource
    private ParagraphLikeMapper likeMapper;
    @Resource
    private StoryContributionMapper contributionMapper;
    @Resource
    private StoryAchievementMapper achievementMapper;
    @Resource
    private StoryFavoriteMapper favoriteMapper;
    @Resource
    private StoryFinishVoteMapper finishVoteMapper;
    @Resource
    private StoryFinishVoteRecordMapper voteRecordMapper;
    @Resource
    private StoryReadProgressMapper readProgressMapper;
    @Resource
    private SensitiveFilter sensitiveFilter;
    @Resource
    private UserMapper userMapper;

    private static final int MAX_PARAGRAPHS = 100; // 自动完结段落数
    private static final int COOLDOWN_HOURS = 24; // 续写冷却时间
    private static final int MIN_READ_PARAGRAPHS = 3; // 最少阅读段落数
    private static final int HOT_THRESHOLD = 10; // 热门阈值
    private static final int KEY_POINT_THRESHOLD = 50; // 关键转折点阈值

    @Override
    @Transactional
    public Story createStory(StoryCreateDTO dto) {
        // 敏感词过滤
        if (sensitiveFilter.hasSensitiveWord(dto.getTitle()) ||
                sensitiveFilter.hasSensitiveWord(dto.getContent()) ||
                (dto.getWorldSetting() != null && sensitiveFilter.hasSensitiveWord(dto.getWorldSetting()))) {
            throw new BizException("STORY_005", "系统检测到违规词汇，系统已拦截");
        }

        // 创建故事
        Story story = new Story();
        story.setTitle(dto.getTitle());
        story.setCategory(dto.getCategory());
        story.setWorldSetting(dto.getWorldSetting());
        story.setFirstParagraph(dto.getContent());
        story.setCreatorId(dto.getUserId());
        story.setStatus(Story.STATUS_ONGOING);
        story.setParagraphCount(1);
        story.setParticipantCount(1);
        story.setTotalLikes(0);
        story.setIsRecommended(0);
        storyMapper.insert(story);

        // 创建开篇段落
        StoryParagraph paragraph = new StoryParagraph();
        paragraph.setStoryId(story.getId());
        paragraph.setContent(dto.getContent());
        paragraph.setAuthorId(dto.getUserId());
        paragraph.setSequence(1);
        paragraph.setLikeCount(0);
        paragraph.setIsHot(0);
        paragraph.setIsKeyPoint(0);
        paragraph.setIsAuthorMarked(0);
        paragraph.setIsAiAssisted(dto.getIsAiAssisted() != null ? dto.getIsAiAssisted() : 0);
        paragraphMapper.insert(paragraph);

        // 初始化创建者贡献度
        StoryContribution contribution = new StoryContribution();
        contribution.setStoryId(story.getId());
        contribution.setUserId(dto.getUserId());
        contribution.setPoints(StoryContribution.POINTS_WRITE_PARAGRAPH);
        contribution.setParagraphCount(1);
        contribution.setLikeReceived(0);
        contribution.setKeyPointCount(0);
        contributionMapper.insert(contribution);

        // 检查并授予"开篇者"成就
        checkAndGrantOpenerAchievement(dto.getUserId(), story.getId());

        return story;
    }

    @Override
    public PageInfo<StoryVO> getStoryList(Integer category, Integer status, int page, int size) {
        PageHelper.startPage(page, size);
        List<StoryVO> list = storyMapper.selectStoryList(category, status, null);
        return new PageInfo<>(list);
    }

    @Override
    public StoryDetailVO getStoryDetail(Long storyId, Long userId) {
        Story story = storyMapper.selectById(storyId);
        if (story == null) {
            throw new BizException("STORY_006", "故事不存在");
        }

        StoryDetailVO vo = new StoryDetailVO();
        vo.setId(story.getId());
        vo.setTitle(story.getTitle());
        vo.setCategory(story.getCategory());
        vo.setCategoryName(story.getCategoryName());
        vo.setWorldSetting(story.getWorldSetting());
        vo.setFirstParagraph(story.getFirstParagraph());
        vo.setStatus(story.getStatus());
        vo.setStatusName(story.getStatusName());
        vo.setParagraphCount(story.getParagraphCount());
        vo.setParticipantCount(story.getParticipantCount());
        vo.setTotalLikes(story.getTotalLikes());
        vo.setIsRecommended(story.getIsRecommended() == 1);
        vo.setCreatorId(story.getCreatorId());
        vo.setCreateTime(story.getCreateTime());
        vo.setFinishTime(story.getFinishTime());

        // 获取创建者信息
        User creator = userMapper.selectById(story.getCreatorId());
        if (creator != null) {
            vo.setCreatorNickname(creator.getNickname());
            vo.setCreatorAvatar(creator.getAvatar());
        }

        // 设置当前锁状态
        if (story.getEditingUserId() != null && story.getEditingExpireTime() != null
                && story.getEditingExpireTime().isAfter(LocalDateTime.now())) {
            vo.setLockedByOther(!story.getEditingUserId().equals(userId));
            User locker = userMapper.selectById(story.getEditingUserId());
            vo.setLockingUserNickname(locker != null ? locker.getNickname() : "其他旅人");
            vo.setLockExpireSeconds((int) ChronoUnit.SECONDS.between(LocalDateTime.now(), story.getEditingExpireTime()));
        } else {
            vo.setLockedByOther(false);
        }

        // 获取段落列表
        List<ParagraphVO> paragraphs = paragraphMapper.selectParagraphList(storyId, userId);
        vo.setParagraphs(paragraphs);

        // 获取贡献度排行(前10)
        List<ContributionRankVO> ranks = storyMapper.selectContributionRank(storyId, 10);
        int rankNum = 1;
        for (ContributionRankVO rank : ranks) {
            rank.setRank(rankNum++);
        }
        vo.setContributionRank(ranks);

        // 检查是否收藏
        if (userId != null) {
            LambdaQueryWrapper<StoryFavorite> favQuery = new LambdaQueryWrapper<>();
            favQuery.eq(StoryFavorite::getStoryId, storyId).eq(StoryFavorite::getUserId, userId);
            vo.setIsFavorited(favoriteMapper.selectCount(favQuery) > 0);
        }

        // 获取当前完结投票
        if (story.getFinishVoteId() != null) {
            StoryFinishVote vote = finishVoteMapper.selectById(story.getFinishVoteId());
            if (vote != null && vote.getStatus() == StoryFinishVote.STATUS_ONGOING) {
                FinishVoteVO voteVO = convertToFinishVoteVO(vote, userId);
                vo.setCurrentVote(voteVO);
            }
        }

        return vo;
    }

    @Override
    public PageInfo<StoryVO> searchStory(String keyword, Integer category, int page, int size) {
        PageHelper.startPage(page, size);
        List<StoryVO> list = storyMapper.selectStoryList(category, null, keyword);
        return new PageInfo<>(list);
    }

    @Override
    public ContinueCheckResult checkCanContinue(Long storyId, Long userId) {
        Story story = storyMapper.selectById(storyId);
        if (story == null) {
            return ContinueCheckResult.fail("故事不存在");
        }
        if (story.getStatus() != Story.STATUS_ONGOING) {
            return ContinueCheckResult.fail("故事已完结，无法续写");
        }

        // 检查是否连续续写(最近一段不能是自己)
        Long lastAuthorId = paragraphMapper.selectLastAuthorId(storyId);
        if (lastAuthorId != null && lastAuthorId.equals(userId)) {
            return ContinueCheckResult.fail("不能连续续写，请等待其他人续写后再来");
        }

        // 检查24小时冷却
        LocalDateTime lastWriteTime = paragraphMapper.selectLastWriteTime(storyId, userId);
        if (lastWriteTime != null) {
            long hoursPassed = ChronoUnit.HOURS.between(lastWriteTime, LocalDateTime.now());
            if (hoursPassed < COOLDOWN_HOURS) {
                int remainingSeconds = (int) ((COOLDOWN_HOURS - hoursPassed) * 3600);
                return ContinueCheckResult.failWithCooldown(
                        "24小时内已续写过该故事，请稍后再来",
                        remainingSeconds);
            }
        }

        // 检查编辑锁
        if (story.getEditingUserId() != null && !story.getEditingUserId().equals(userId)) {
            if (story.getEditingExpireTime() != null && story.getEditingExpireTime().isAfter(LocalDateTime.now())) {
                User lockingUser = userMapper.selectById(story.getEditingUserId());
                String nickname = lockingUser != null ? lockingUser.getNickname() : "其他旅人";
                int expireSeconds = (int) ChronoUnit.SECONDS.between(LocalDateTime.now(), story.getEditingExpireTime());
                return ContinueCheckResult.failWithLock("当前有人正在编写", nickname, expireSeconds);
            }
        }

        // 检查是否阅读了最近3段
        LambdaQueryWrapper<StoryReadProgress> progressQuery = new LambdaQueryWrapper<>();
        progressQuery.eq(StoryReadProgress::getStoryId, storyId)
                .eq(StoryReadProgress::getUserId, userId);
        StoryReadProgress progress = readProgressMapper.selectOne(progressQuery);

        int currentMaxSequence = story.getParagraphCount();
        int requiredReadSequence = Math.max(1, currentMaxSequence - MIN_READ_PARAGRAPHS + 1);

        if (progress == null || progress.getLastReadSequence() < requiredReadSequence) {
            int unread = progress == null ? MIN_READ_PARAGRAPHS
                    : Math.min(MIN_READ_PARAGRAPHS, currentMaxSequence - progress.getLastReadSequence());
            return ContinueCheckResult.failWithUnread(
                    "请先阅读最近" + MIN_READ_PARAGRAPHS + "段内容",
                    unread);
        }

        return ContinueCheckResult.success();
    }

    @Override
    @Transactional
    public StoryParagraph continueStory(Long storyId, ParagraphCreateDTO dto) {
        // 验证续写资格
        ContinueCheckResult checkResult = checkCanContinue(storyId, dto.getUserId());
        if (!checkResult.getCanContinue()) {
            throw new BizException("STORY_008", checkResult.getReason());
        }

        // 敏感词过滤
        if (sensitiveFilter.hasSensitiveWord(dto.getContent())) {
            throw new BizException("STORY_005", "系统检测到违规词汇，系统已拦截");
        }

        // 使用悲观锁锁定故事记录，防止并发冲突
        Story story = storyMapper.selectByIdForUpdate(storyId);
        if (story == null) {
            throw new BizException("STORY_006", "故事不存在");
        }

        // 再次检查编辑锁，确保没有被别人抢占
        if (story.getEditingUserId() != null && !story.getEditingUserId().equals(dto.getUserId())) {
            if (story.getEditingExpireTime() != null && story.getEditingExpireTime().isAfter(LocalDateTime.now())) {
                throw new BizException("STORY_009", "当前有其他旅人正在编写，请稍后再试");
            }
        }

        // 加锁后再次检查故事状态，防止并发完结
        if (story.getStatus() != Story.STATUS_ONGOING) {
            throw new BizException("STORY_007", "故事已完结，无法续写");
        }

        // 在锁保护下再次检查连续续写（防止并发绕过）
        Long lastAuthorId = paragraphMapper.selectLastAuthorId(storyId);
        if (lastAuthorId != null && lastAuthorId.equals(dto.getUserId())) {
            throw new BizException("STORY_008", "不能连续续写，请等待其他人续写后再来");
        }

        // 直接使用story表中的段落计数作为新序号（在锁保护下，避免重复）
        int newSequence = story.getParagraphCount() + 1;

        // 创建段落
        StoryParagraph paragraph = new StoryParagraph();
        paragraph.setStoryId(storyId);
        paragraph.setContent(dto.getContent());
        paragraph.setImageUrl(dto.getImageUrl());
        paragraph.setAuthorId(dto.getUserId());
        paragraph.setPenName(dto.getPenName());
        paragraph.setSequence(newSequence);
        paragraph.setLikeCount(0);
        paragraph.setIsHot(0);
        paragraph.setIsKeyPoint(0);
        paragraph.setIsAuthorMarked(dto.getIsKeyPoint() ? 1 : 0);
        paragraph.setIsAiAssisted(dto.getIsAiAssisted() != null ? dto.getIsAiAssisted() : 0);
        paragraphMapper.insert(paragraph);

        // 更新故事统计
        story.setParagraphCount(newSequence);

        // 检查是否新参与者
        LambdaQueryWrapper<StoryContribution> contribQuery = new LambdaQueryWrapper<>();
        contribQuery.eq(StoryContribution::getStoryId, storyId)
                .eq(StoryContribution::getUserId, dto.getUserId());
        StoryContribution contribution = contributionMapper.selectOne(contribQuery);

        if (contribution == null) {
            // 新参与者
            contribution = new StoryContribution();
            contribution.setStoryId(storyId);
            contribution.setUserId(dto.getUserId());
            contribution.setPoints(StoryContribution.POINTS_WRITE_PARAGRAPH);
            contribution.setParagraphCount(1);
            contribution.setLikeReceived(0);
            contribution.setKeyPointCount(0);
            contributionMapper.insert(contribution);
            story.setParticipantCount(story.getParticipantCount() + 1);
        } else {
            // 已有参与者，更新贡献度
            contribution.setPoints(contribution.getPoints() + StoryContribution.POINTS_WRITE_PARAGRAPH);
            contribution.setParagraphCount(contribution.getParagraphCount() + 1);
            contributionMapper.updateById(contribution);
        }

        storyMapper.updateById(story);

        // 检查是否触发自动完结
        if (newSequence >= MAX_PARAGRAPHS) {
            finishStory(story);
        }

        // 检查长篇缔造者成就
        if (contribution.getParagraphCount() >= 10) {
            checkAndGrantLongCreatorAchievement(dto.getUserId(), storyId);
        }

        // WebSocket通知
        notifyNewParagraph(storyId, paragraph);

        // 清除编辑锁
        story.setEditingUserId(null);
        story.setEditingExpireTime(null);
        storyMapper.updateById(story);

        // 通知所有人锁已释放
        notifyLockReleased(storyId);

        return paragraph;
    }

    @Override
    @Transactional
    public void acquireLock(Long storyId, Long userId) {
        // 使用悲观锁保护锁定操作
        Story story = storyMapper.selectByIdForUpdate(storyId);
        if (story == null) {
            throw new BizException("STORY_006", "故事不存在");
        }
        if (story.getStatus() != Story.STATUS_ONGOING) {
            throw new BizException("STORY_007", "故事已完结，无法续写");
        }

        // 检查是否已被别人锁定并未过期
        if (story.getEditingUserId() != null && !story.getEditingUserId().equals(userId)) {
            if (story.getEditingExpireTime() != null && story.getEditingExpireTime().isAfter(LocalDateTime.now())) {
                throw new BizException("STORY_009", "已被其他人锁定");
            }
        }

        // 锁定成功，设置有效期为2分钟
        story.setEditingUserId(userId);
        story.setEditingExpireTime(LocalDateTime.now().plusMinutes(2));
        storyMapper.updateById(story);

        // 这里可以通过WS广播“已被锁定”以供其他查看者UI更新，但业务上可选
    }

    @Override
    @Transactional
    public void releaseLock(Long storyId, Long userId) {
        Story story = storyMapper.selectByIdForUpdate(storyId);
        if (story != null && userId.equals(story.getEditingUserId())) {
            story.setEditingUserId(null);
            story.setEditingExpireTime(null);
            storyMapper.updateById(story);

            notifyLockReleased(storyId);
        }
    }

    @Override
    @Transactional
    public void heartbeatLock(Long storyId, Long userId) {
        Story story = storyMapper.selectById(storyId);
        // 只有持有锁的人才能续期（宽限期可略宽松一点以防刚过期又被续）
        if (story != null && userId.equals(story.getEditingUserId())) {
            story.setEditingExpireTime(LocalDateTime.now().plusMinutes(2));
            storyMapper.updateById(story);
        }
    }

    @Override
    @Transactional
    public LikeResult toggleLike(Long paragraphId, Long userId) {
        StoryParagraph paragraph = paragraphMapper.selectById(paragraphId);
        if (paragraph == null) {
            throw new BizException("STORY_006", "段落不存在");
        }

        LambdaQueryWrapper<ParagraphLike> query = new LambdaQueryWrapper<>();
        query.eq(ParagraphLike::getParagraphId, paragraphId)
                .eq(ParagraphLike::getUserId, userId);
        ParagraphLike existingLike = likeMapper.selectOne(query);

        boolean liked;
        int likeDelta;
        if (existingLike != null) {
            // 取消点赞
            likeMapper.deleteById(existingLike.getId());
            likeDelta = -1;
            liked = false;

            // 更新作者贡献度
            updateAuthorContribution(paragraph.getStoryId(), paragraph.getAuthorId(), -1, false);
        } else {
            // 点赞
            ParagraphLike like = new ParagraphLike();
            like.setParagraphId(paragraphId);
            like.setUserId(userId);
            likeMapper.insert(like);
            likeDelta = 1;
            liked = true;

            // 更新作者贡献度
            updateAuthorContribution(paragraph.getStoryId(), paragraph.getAuthorId(), 1, false);
        }

        // 使用SQL原子操作更新点赞数，避免并发问题
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<StoryParagraph> updateWrapper = new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        updateWrapper.eq(StoryParagraph::getId, paragraphId)
                .setSql("like_count = like_count + " + likeDelta);
        paragraphMapper.update(null, updateWrapper);

        // 重新查询获取最新点赞数
        paragraph = paragraphMapper.selectById(paragraphId);

        // 更新热度状态
        updateParagraphHotStatus(paragraph);
        paragraphMapper.updateById(paragraph);

        // 使用SQL原子操作更新故事总点赞数
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Story> storyUpdateWrapper = new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        storyUpdateWrapper.eq(Story::getId, paragraph.getStoryId())
                .setSql("total_likes = total_likes + " + likeDelta);
        storyMapper.update(null, storyUpdateWrapper);

        // WebSocket通知
        notifyLikeUpdate(paragraph.getStoryId(), paragraphId, paragraph.getLikeCount());

        return new LikeResult(liked, paragraph.getLikeCount());
    }

    private void updateParagraphHotStatus(StoryParagraph paragraph) {
        int likeCount = paragraph.getLikeCount();

        // 更新热门状态
        paragraph.setIsHot(likeCount >= HOT_THRESHOLD ? 1 : 0);

        // 更新关键转折点状态
        if (likeCount >= KEY_POINT_THRESHOLD && paragraph.getIsKeyPoint() == 0) {
            paragraph.setIsKeyPoint(1);

            // 更新作者贡献度(关键转折点奖励)
            updateAuthorContribution(paragraph.getStoryId(), paragraph.getAuthorId(),
                    StoryContribution.POINTS_KEY_POINT, true);

            // 授予关键拼图成就
            checkAndGrantKeyPuzzleAchievement(paragraph.getAuthorId(), paragraph.getStoryId());
        }
    }

    private void updateAuthorContribution(Long storyId, Long authorId, int likeDelta, boolean isKeyPoint) {
        LambdaQueryWrapper<StoryContribution> query = new LambdaQueryWrapper<>();
        query.eq(StoryContribution::getStoryId, storyId)
                .eq(StoryContribution::getUserId, authorId);
        StoryContribution contribution = contributionMapper.selectOne(query);

        if (contribution != null) {
            contribution.setLikeReceived(contribution.getLikeReceived() + likeDelta);
            contribution.setPoints(contribution.getPoints() + likeDelta);
            if (isKeyPoint) {
                contribution.setKeyPointCount(contribution.getKeyPointCount() + 1);
            }
            contributionMapper.updateById(contribution);
        }
    }

    @Override
    @Transactional
    public boolean toggleFavorite(Long storyId, Long userId) {
        LambdaQueryWrapper<StoryFavorite> query = new LambdaQueryWrapper<>();
        query.eq(StoryFavorite::getStoryId, storyId)
                .eq(StoryFavorite::getUserId, userId);
        StoryFavorite existing = favoriteMapper.selectOne(query);

        if (existing != null) {
            favoriteMapper.deleteById(existing.getId());
            return false;
        } else {
            StoryFavorite favorite = new StoryFavorite();
            favorite.setStoryId(storyId);
            favorite.setUserId(userId);
            favoriteMapper.insert(favorite);
            return true;
        }
    }

    @Override
    @Transactional
    public void initiateFinishVote(Long storyId, Long userId) {
        Story story = storyMapper.selectById(storyId);
        if (story == null) {
            throw new BizException("STORY_006", "故事不存在");
        }
        if (story.getStatus() != Story.STATUS_ONGOING) {
            throw new BizException("STORY_007", "故事已完结");
        }
        if (story.getFinishVoteId() != null) {
            StoryFinishVote existingVote = finishVoteMapper.selectById(story.getFinishVoteId());
            if (existingVote != null && existingVote.getStatus() == StoryFinishVote.STATUS_ONGOING) {
                throw new BizException("STORY_015", "已有进行中的完结投票");
            }
        }

        // 检查是否在贡献度前10（排除创建者，创建者用直接完结接口）
        Integer userRank = contributionMapper.selectUserRank(storyId, userId);
        if (userRank == null || userRank > 10) {
            throw new BizException("STORY_014", "只有贡献度前10的用户可以发起完结投票");
        }

        // 创建者不能发起投票，应该使用直接完结接口
        if (story.getCreatorId().equals(userId)) {
            throw new BizException("STORY_019", "创建者请使用直接完结功能");
        }

        // 创建投票
        StoryFinishVote vote = new StoryFinishVote();
        vote.setStoryId(storyId);
        vote.setInitiatorId(userId);
        vote.setAgreeCount(1); // 发起者自动投同意票
        vote.setDisagreeCount(0);
        vote.setTotalVoters(story.getParticipantCount());
        vote.setStatus(StoryFinishVote.STATUS_ONGOING);
        vote.setExpireTime(LocalDateTime.now().plusHours(24));
        finishVoteMapper.insert(vote);

        // 记录发起者的投票
        StoryFinishVoteRecord initiatorRecord = new StoryFinishVoteRecord();
        initiatorRecord.setVoteId(vote.getId());
        initiatorRecord.setUserId(userId);
        initiatorRecord.setAgree(1);
        voteRecordMapper.insert(initiatorRecord);

        // 更新故事的当前投票ID
        story.setFinishVoteId(vote.getId());
        storyMapper.updateById(story);

        // 检查是否立即达到通过条件（如果只有1个参与者）
        if (vote.getAgreeCount() > vote.getTotalVoters() / 2) {
            vote.setStatus(StoryFinishVote.STATUS_PASSED);
            finishVoteMapper.updateById(vote);
            finishStory(story);
        }
    }

    @Override
    @Transactional
    public void finishStoryDirectly(Long storyId, Long userId) {
        Story story = storyMapper.selectById(storyId);
        if (story == null) {
            throw new BizException("STORY_006", "故事不存在");
        }
        if (story.getStatus() != Story.STATUS_ONGOING) {
            throw new BizException("STORY_007", "故事已完结");
        }

        // 只有创建者可以直接完结
        if (!story.getCreatorId().equals(userId)) {
            throw new BizException("STORY_020", "只有故事创建者可以直接完结故事");
        }

        // 直接完结，不需要投票
        finishStory(story);
    }

    @Override
    @Transactional
    public void voteFinish(Long storyId, Long userId, boolean agree) {
        Story story = storyMapper.selectById(storyId);
        if (story == null || story.getFinishVoteId() == null) {
            throw new BizException("STORY_006", "投票不存在");
        }

        StoryFinishVote vote = finishVoteMapper.selectById(story.getFinishVoteId());
        if (vote == null || vote.getStatus() != StoryFinishVote.STATUS_ONGOING) {
            throw new BizException("STORY_006", "投票已结束");
        }

        // 检查是否已投票（发起者自动算同意票，无需再投）
        LambdaQueryWrapper<StoryFinishVoteRecord> recordQuery = new LambdaQueryWrapper<>();
        recordQuery.eq(StoryFinishVoteRecord::getVoteId, vote.getId())
                .eq(StoryFinishVoteRecord::getUserId, userId);
        if (voteRecordMapper.selectCount(recordQuery) > 0) {
            throw new BizException("STORY_016", "您已投过票");
        }

        // 记录投票
        StoryFinishVoteRecord record = new StoryFinishVoteRecord();
        record.setVoteId(vote.getId());
        record.setUserId(userId);
        record.setAgree(agree ? 1 : 0);
        voteRecordMapper.insert(record);

        // 使用SQL原子操作更新投票统计
        if (agree) {
            finishVoteMapper.incrementAgreeCount(vote.getId());
        } else {
            finishVoteMapper.incrementDisagreeCount(vote.getId());
        }

        // 重新从数据库读取最新投票数据（避免使用过时的内存值）
        vote = finishVoteMapper.selectById(vote.getId());

        // 检查是否达到通过条件(>50%同意)
        if (vote.getAgreeCount() > vote.getTotalVoters() / 2) {
            vote.setStatus(StoryFinishVote.STATUS_PASSED);
            finishVoteMapper.updateById(vote);
            finishStory(story);
        }
    }

    private void finishStory(Story story) {
        story.setStatus(Story.STATUS_FINISHED);
        story.setFinishTime(LocalDateTime.now());
        storyMapper.updateById(story);

        // 通知所有参与者
        notifyStoryFinished(story.getId());

        // 检查故事之王成就
        checkAndGrantStoryKingAchievement();
    }

    @Override
    public PageInfo<StoryVO> getMyCreatedStories(Long userId, int page, int size) {
        PageHelper.startPage(page, size);
        List<StoryVO> list = storyMapper.selectMyCreatedStories(userId);
        return new PageInfo<>(list);
    }

    @Override
    public PageInfo<StoryVO> getMyParticipatedStories(Long userId, int page, int size) {
        PageHelper.startPage(page, size);
        List<StoryVO> list = storyMapper.selectMyParticipatedStories(userId);
        return new PageInfo<>(list);
    }

    @Override
    public List<ParagraphVO> getMyParagraphs(Long userId) {
        return paragraphMapper.selectUserParagraphs(userId);
    }

    @Override
    public List<StoryAchievement> getMyAchievements(Long userId) {
        LambdaQueryWrapper<StoryAchievement> query = new LambdaQueryWrapper<>();
        query.eq(StoryAchievement::getUserId, userId)
                .orderByDesc(StoryAchievement::getCreateTime);
        return achievementMapper.selectList(query);
    }

    @Override
    public PageInfo<StoryVO> getMyFavorites(Long userId, int page, int size) {
        PageHelper.startPage(page, size);
        List<StoryVO> list = storyMapper.selectMyFavoriteStories(userId);
        return new PageInfo<>(list);
    }

    @Override
    public List<ContributionRankVO> getContributionRank(Long storyId) {
        List<ContributionRankVO> ranks = storyMapper.selectContributionRank(storyId, null);
        int rankNum = 1;
        for (ContributionRankVO rank : ranks) {
            rank.setRank(rankNum++);
        }
        return ranks;
    }

    @Override
    @Transactional
    public void deleteStory(Long storyId, Long userId) {
        Story story = storyMapper.selectById(storyId);
        if (story == null) {
            throw new BizException("STORY_006", "故事不存在");
        }
        if (!story.getCreatorId().equals(userId)) {
            throw new BizException("STORY_017", "只能删除自己创建的故事");
        }
        if (story.getStatus() == Story.STATUS_ONGOING && story.getParagraphCount() > 1) {
            throw new BizException("STORY_018", "进行中且有他人参与的故事无法删除");
        }

        // 级联删除(数据库外键已设置CASCADE)
        storyMapper.deleteById(storyId);
    }

    @Override
    public PageInfo<StoryVO> getArchiveList(int page, int size) {
        PageHelper.startPage(page, size);
        List<StoryVO> list = storyMapper.selectStoryList(null, Story.STATUS_FINISHED, null);
        return new PageInfo<>(list);
    }

    @Override
    public void updateReadProgress(Long storyId, Long userId, Integer sequence) {
        LambdaQueryWrapper<StoryReadProgress> query = new LambdaQueryWrapper<>();
        query.eq(StoryReadProgress::getStoryId, storyId)
                .eq(StoryReadProgress::getUserId, userId);
        StoryReadProgress progress = readProgressMapper.selectOne(query);

        if (progress == null) {
            progress = new StoryReadProgress();
            progress.setStoryId(storyId);
            progress.setUserId(userId);
            progress.setLastReadSequence(sequence);
            readProgressMapper.insert(progress);
        } else if (sequence > progress.getLastReadSequence()) {
            progress.setLastReadSequence(sequence);
            readProgressMapper.updateById(progress);
        }
    }

    // ========== 管理员功能 ==========

    @Override
    public PageInfo<StoryVO> getAdminStoryList(Integer status, Integer category, int page, int size) {
        PageHelper.startPage(page, size);
        List<StoryVO> list = storyMapper.selectStoryList(category, status, null);
        return new PageInfo<>(list);
    }

    @Override
    @Transactional
    public void adminDeleteStory(Long storyId) {
        storyMapper.deleteById(storyId);
    }

    @Override
    @Transactional
    public void adminDeleteParagraph(Long paragraphId) {
        StoryParagraph paragraph = paragraphMapper.selectById(paragraphId);
        if (paragraph == null) {
            return;
        }

        Long storyId = paragraph.getStoryId();
        paragraphMapper.deleteById(paragraphId);

        // 重新计算故事统计
        Story story = storyMapper.selectById(storyId);
        if (story != null) {
            Integer maxSeq = paragraphMapper.selectMaxSequence(storyId);
            story.setParagraphCount(maxSeq == null ? 0 : maxSeq);

            // 重新计算总点赞数
            LambdaQueryWrapper<StoryParagraph> pQuery = new LambdaQueryWrapper<>();
            pQuery.eq(StoryParagraph::getStoryId, storyId);
            List<StoryParagraph> paragraphs = paragraphMapper.selectList(pQuery);
            int totalLikes = paragraphs.stream().mapToInt(p -> p.getLikeCount() != null ? p.getLikeCount() : 0).sum();
            story.setTotalLikes(totalLikes);

            storyMapper.updateById(story);
        }
    }

    @Override
    public void setRecommend(Long storyId, boolean recommend) {
        Story story = storyMapper.selectById(storyId);
        if (story != null) {
            story.setIsRecommended(recommend ? 1 : 0);
            storyMapper.updateById(story);
        }
    }

    @Override
    public StoryStatsVO getStats() {
        StoryStatsVO stats = new StoryStatsVO();
        stats.setTotalStories(storyMapper.selectCount(null));
        stats.setTodayNewStories(storyMapper.countTodayNewStories());
        stats.setTotalParagraphs(paragraphMapper.selectCount(null));
        stats.setTodayNewParagraphs(paragraphMapper.countTodayNewParagraphs());
        stats.setActiveUsers(paragraphMapper.countActiveUsers());
        stats.setOngoingStories(storyMapper.countByStatus(Story.STATUS_ONGOING));
        stats.setFinishedStories(storyMapper.countByStatus(Story.STATUS_FINISHED));
        stats.setArchivedStories(storyMapper.countByStatus(Story.STATUS_ARCHIVED));
        return stats;
    }

    // ========== 成就相关 ==========

    private void checkAndGrantOpenerAchievement(Long userId, Long storyId) {
        LambdaQueryWrapper<StoryAchievement> query = new LambdaQueryWrapper<>();
        query.eq(StoryAchievement::getUserId, userId)
                .eq(StoryAchievement::getAchievementType, StoryAchievement.TYPE_OPENER);
        if (achievementMapper.selectCount(query) == 0) {
            StoryAchievement achievement = new StoryAchievement();
            achievement.setUserId(userId);
            achievement.setAchievementType(StoryAchievement.TYPE_OPENER);
            achievement.setStoryId(storyId);
            achievementMapper.insert(achievement);
            notifyAchievement(userId, StoryAchievement.TYPE_OPENER);
        }
    }

    private void checkAndGrantKeyPuzzleAchievement(Long userId, Long storyId) {
        LambdaQueryWrapper<StoryAchievement> query = new LambdaQueryWrapper<>();
        query.eq(StoryAchievement::getUserId, userId)
                .eq(StoryAchievement::getAchievementType, StoryAchievement.TYPE_KEY_PUZZLE);
        if (achievementMapper.selectCount(query) == 0) {
            StoryAchievement achievement = new StoryAchievement();
            achievement.setUserId(userId);
            achievement.setAchievementType(StoryAchievement.TYPE_KEY_PUZZLE);
            achievement.setStoryId(storyId);
            achievementMapper.insert(achievement);
            notifyAchievement(userId, StoryAchievement.TYPE_KEY_PUZZLE);
        }
    }

    private void checkAndGrantLongCreatorAchievement(Long userId, Long storyId) {
        LambdaQueryWrapper<StoryAchievement> query = new LambdaQueryWrapper<>();
        query.eq(StoryAchievement::getUserId, userId)
                .eq(StoryAchievement::getAchievementType, StoryAchievement.TYPE_LONG_CREATOR);
        if (achievementMapper.selectCount(query) == 0) {
            StoryAchievement achievement = new StoryAchievement();
            achievement.setUserId(userId);
            achievement.setAchievementType(StoryAchievement.TYPE_LONG_CREATOR);
            achievement.setStoryId(storyId);
            achievementMapper.insert(achievement);
            notifyAchievement(userId, StoryAchievement.TYPE_LONG_CREATOR);
        }
    }

    private void checkAndGrantStoryKingAchievement() {
        Long topUserId = contributionMapper.selectTopContributor();
        if (topUserId == null)
            return;

        LambdaQueryWrapper<StoryAchievement> query = new LambdaQueryWrapper<>();
        query.eq(StoryAchievement::getUserId, topUserId)
                .eq(StoryAchievement::getAchievementType, StoryAchievement.TYPE_STORY_KING);
        if (achievementMapper.selectCount(query) == 0) {
            // 先删除其他人的故事之王成就
            LambdaQueryWrapper<StoryAchievement> deleteQuery = new LambdaQueryWrapper<>();
            deleteQuery.eq(StoryAchievement::getAchievementType, StoryAchievement.TYPE_STORY_KING);
            achievementMapper.delete(deleteQuery);

            StoryAchievement achievement = new StoryAchievement();
            achievement.setUserId(topUserId);
            achievement.setAchievementType(StoryAchievement.TYPE_STORY_KING);
            achievementMapper.insert(achievement);
            notifyAchievement(topUserId, StoryAchievement.TYPE_STORY_KING);
        }
    }

    // ========== WebSocket通知 ==========

    private void notifyNewParagraph(Long storyId, StoryParagraph paragraph) {
        try {
            String message = String.format(
                    "{\"type\":\"STORY_NEW_PARAGRAPH\",\"storyId\":%d,\"paragraphId\":%d,\"sequence\":%d}",
                    storyId, paragraph.getId(), paragraph.getSequence());
            WebSocketServer.broadcast(message);
        } catch (Exception e) {
            // 忽略WebSocket错误
        }
    }

    private void notifyLockReleased(Long storyId) {
        try {
            String message = String.format("{\"type\":\"STORY_LOCK_RELEASED\",\"storyId\":%d}", storyId);
            WebSocketServer.broadcast(message);
        } catch (Exception e) {
            // 忽略WebSocket错误
        }
    }

    private void notifyLikeUpdate(Long storyId, Long paragraphId, int likeCount) {
        try {
            String message = String.format(
                    "{\"type\":\"STORY_LIKE_UPDATE\",\"storyId\":%d,\"paragraphId\":%d,\"likeCount\":%d}",
                    storyId, paragraphId, likeCount);
            WebSocketServer.broadcast(message);
        } catch (Exception e) {
            // 忽略WebSocket错误
        }
    }

    private void notifyStoryFinished(Long storyId) {
        try {
            String message = String.format("{\"type\":\"STORY_FINISHED\",\"storyId\":%d}", storyId);
            WebSocketServer.broadcast(message);
        } catch (Exception e) {
            // 忽略WebSocket错误
        }
    }

    private void notifyAchievement(Long userId, String achievementType) {
        try {
            // 使用现有的通知方法发送成就通知
            String content = String.format("恭喜获得成就：%s", achievementType);
            WebSocketServer.sendNotification(userId, content, 5);
        } catch (Exception e) {
            // 忽略WebSocket错误
        }
    }

    // ========== 辅助方法 ==========

    private FinishVoteVO convertToFinishVoteVO(StoryFinishVote vote, Long userId) {
        FinishVoteVO vo = new FinishVoteVO();
        vo.setId(vote.getId());
        vo.setStoryId(vote.getStoryId());
        vo.setInitiatorId(vote.getInitiatorId());
        vo.setAgreeCount(vote.getAgreeCount());
        vo.setDisagreeCount(vote.getDisagreeCount());
        vo.setTotalVoters(vote.getTotalVoters());
        vo.setStatus(vote.getStatus());
        vo.setStatusName(vote.getStatusName());
        vo.setCreateTime(vote.getCreateTime());
        vo.setExpireTime(vote.getExpireTime());

        // 获取发起者昵称
        User initiator = userMapper.selectById(vote.getInitiatorId());
        if (initiator != null) {
            vo.setInitiatorNickname(initiator.getNickname());
        }

        // 检查当前用户投票状态
        if (userId != null) {
            LambdaQueryWrapper<StoryFinishVoteRecord> recordQuery = new LambdaQueryWrapper<>();
            recordQuery.eq(StoryFinishVoteRecord::getVoteId, vote.getId())
                    .eq(StoryFinishVoteRecord::getUserId, userId);
            StoryFinishVoteRecord record = voteRecordMapper.selectOne(recordQuery);
            vo.setHasVoted(record != null);
            vo.setMyVote(record != null ? record.getAgree() == 1 : null);
        }

        return vo;
    }
}
