// File: springboot/src/main/java/com/example/service/impl/PostServiceImpl.java
package com.example.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.example.entity.Post;
import com.example.entity.User;
import com.example.mapper.FileRecordMapper;
import com.example.mapper.PostMapper;
import com.example.mapper.UserMapper;
import com.example.service.PostService;
import com.example.utils.SensitiveFilter;
import com.example.utils.XssUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class PostServiceImpl implements PostService {

    @Resource
    private PostMapper postMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private FileRecordMapper fileRecordMapper;
    @Resource
    private SensitiveFilter sensitiveFilter;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createPost(Post post) {
        // 1. 信誉分检查
        if (post.getUserId() != null && post.getUserId() != 0) {
            User user = userMapper.selectById(post.getUserId());
            if (user != null) {
                int score = user.getCreditScore() == null ? 100 : user.getCreditScore();
                if (score < 60) {
                    throw new RuntimeException("您的信誉分低于60，已被永久禁止发帖！");
                }
            }
        }

        // 2. XSS 防护 + 敏感词过滤
        if (StrUtil.isNotBlank(post.getContent())) {
            // 先进行 XSS 清理
            String safeContent = XssUtils.stripHtml(post.getContent());
            // 敏感词直接替换为 ***，不拦截发布，帖子正常存库
            // 用户发出后看到的内容已是净化版，无需感知
            post.setContent(sensitiveFilter.filter(safeContent));
        }

        // 清理位置字符串
        if (StrUtil.isNotBlank(post.getLocation())) {
            post.setLocation(XssUtils.stripHtml(post.getLocation()));
        }

        // 3. 初始化
        // 如果设置了定时发布，status设为2(定时待发布)，否则设为1(已发布)
        if (post.getScheduledTime() != null) {
            post.setStatus(2); // 定时待发布
        } else {
            post.setStatus(1); // 直接发布
        }
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setShareCount(0);

        // 4. 插入帖子
        postMapper.insert(post);

        // 5. 扣除积分（在同一事务中）
        if (post.getUserId() != null && post.getUserId() != 0) {
            User user = userMapper.selectById(post.getUserId());
            if (user != null) {
                int currentScore = user.getCreditScore() != null ? user.getCreditScore() : 100;
                int newScore = Math.max(0, currentScore - 1);
                user.setCreditScore(newScore);
                userMapper.updateById(user);
            }
        }

        // 6. 文件转正（支持JSON和逗号分隔两种格式）
        if (StrUtil.isNotBlank(post.getImages())) {
            List<String> urls = parseImageUrls(post.getImages());
            for (String url : urls) {
                fileRecordMapper.markAsUsed(url.trim());
            }
        }
        if (StrUtil.isNotBlank(post.getVideo())) {
            fileRecordMapper.markAsUsed(post.getVideo().trim());
        }
    }

    @Override
    public PageInfo<Post> getPostList(Integer pageNum, Integer pageSize, Long currentUserId, Long userId, Integer type,
            Integer category, String keyword, boolean isAdmin) {
        PageHelper.startPage(pageNum, pageSize);
        // 调用 Mapper 的全参数查询
        List<Post> list = postMapper.selectAll(currentUserId, userId, type, category, keyword, isAdmin);
        list.forEach(post -> processPostData(post, currentUserId, isAdmin));
        return new PageInfo<>(list);
    }

    @Override
    public Post getPostDetail(Long postId, Long currentUserId, boolean isAdmin) {
        // 【核心修复】这里传入两个参数，匹配 Mapper 接口
        Post post = postMapper.selectById(postId, currentUserId);

        if (post == null) {
            throw new RuntimeException("帖子不存在");
        }

        // 增加阅读数
        postMapper.incrementView(postId);

        processPostData(post, currentUserId, isAdmin);
        return post;
    }

    @Override
    public List<Post> getHotPosts() {
        return postMapper.selectHotPosts();
    }

    @Override
    public List<Post> getScheduledPosts(Long userId) {
        return postMapper.selectScheduled(userId);
    }

    @Override
    public List<Post> getMyPolls(Long userId) {
        return postMapper.selectPolls(userId);
    }

    /**
     * 点赞切换逻辑（并发安全优化）
     * 先检查状态，再执行操作，使用事务保证原子性
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleLike(Long postId, Long userId) {
        // 检查是否已点赞
        int isLiked = postMapper.checkIsLiked(postId, userId);

        if (isLiked > 0) {
            // 已点赞，执行取消点赞
            postMapper.deletePostLike(postId, userId);
            // 使用SQL原子操作减少点赞数
            postMapper.updateLikeCount(postId, -1);
            return false;
        } else {
            // 未点赞，执行点赞
            postMapper.insertPostLike(postId, userId);
            // 使用SQL原子操作增加点赞数
            postMapper.updateLikeCount(postId, 1);
            return true;
        }
    }

    // 趣味匿名昵称词库
    private static final String[] ANON_PREFIX = {
            "迷茫的", "甚至", "暴躁的", "甚至", "可爱的", "甚至", "快乐的",
            "正在内卷的", "刚下课的", "没抢到课的", "正在补作业的", "想脱单的",
            "熬夜的", "早起的", "吃土的", "甚至", "甚至", "甚至"
    };
    private static final String[] ANON_SUFFIX = {
            "大一新生", "考研党", "干饭人", "路人甲", "甚至", "甚至",
            "甚至", "甚至", "甚至", "甚至", "甚至", "甚至",
            "甚至", "甚至", "甚至", "甚至", "甚至", "甚至"
    };

    /**
     * 根据帖子ID生成固定的随机昵称
     * 保证同一个帖子的匿名显示永远一致
     */
    private String generateAnonymousName(Long postId) {
        if (postId == null)
            return "匿名同学";
        long seed = postId;
        int prefixIndex = (int) (seed % ANON_PREFIX.length);
        int suffixIndex = (int) ((seed * 7 + 3) % ANON_SUFFIX.length);
        return ANON_PREFIX[prefixIndex] + ANON_SUFFIX[suffixIndex];
    }

    private void processPostData(Post post, Long currentUserId, boolean isAdmin) {
        // 支持JSON和逗号分隔两种格式解析图片
        if (StrUtil.isNotEmpty(post.getImages())) {
            post.setImgList(parseImageUrls(post.getImages()));
        } else {
            post.setImgList(Collections.emptyList());
        }

        // 后端彻底清除匿名帖子的敏感信息
        boolean isSelf = (currentUserId != null && currentUserId.equals(post.getUserId()));
        if (post.getIsAnonymous() != null && post.getIsAnonymous() == 1) {
            String anonName = generateAnonymousName(post.getId());

            if (isAdmin) {
                // 管理员特权：显示真实信息，但标注匿名
                String realInfo = post.getNickname();
                if (post.getUserAccount() != null) {
                    realInfo += " @" + post.getUserAccount();
                }
                post.setNickname(anonName + " (" + realInfo + ")");
            } else {
                post.setNickname(isSelf ? anonName + " (我)" : anonName);
                post.setAvatar(null); // 清空头像，前端显示默认头像

                if (!isSelf) {
                    post.setUserId(null); // 对其他人隐藏用户ID
                }
            }
        }
    }

    /**
     * 【优化】解析图片URL列表，兼容JSON数组和逗号分隔两种格式
     */
    private List<String> parseImageUrls(String images) {
        if (StrUtil.isBlank(images)) {
            return Collections.emptyList();
        }

        // 尝试JSON格式解析
        String trimmed = images.trim();
        if (trimmed.startsWith("[")) {
            try {
                JSONArray arr = JSONUtil.parseArray(trimmed);
                List<String> result = new ArrayList<>();
                for (Object obj : arr) {
                    if (obj != null) {
                        result.add(obj.toString());
                    }
                }
                return result;
            } catch (Exception e) {
                // JSON解析失败，降级为逗号分隔
            }
        }

        // 逗号分隔格式（兼容旧数据）
        String[] urls = images.split(",");
        List<String> result = new ArrayList<>();
        for (String url : urls) {
            if (StrUtil.isNotBlank(url)) {
                result.add(url.trim());
            }
        }
        return result;
    }
}