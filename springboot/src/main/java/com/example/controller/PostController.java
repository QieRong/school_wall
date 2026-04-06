// File: springboot/src/main/java/com/example/controller/PostController.java
package com.example.controller;

import com.example.common.Result;
import com.example.entity.Post;
import com.example.entity.User;
import com.example.mapper.PostMapper;
import com.example.mapper.UserMapper;
import com.example.service.impl.PostServiceImpl; // 注入实现类以使用 toggleLike
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/post")
public class PostController {

    @Resource
    private PostServiceImpl postService; // 使用实现类
    @Resource
    private PostMapper postMapper;
    @Resource
    private UserMapper userMapper;

    private boolean isAdmin(Long userId) {
        if (userId == null)
            return false;
        User user = userMapper.selectById(userId);
        return user != null && user.getRole() != null && user.getRole() == com.example.common.Constants.ROLE_ADMIN;
    }

    @PostMapping("/create")
    public Result<?> create(@RequestBody Post post) {
        if (isAdmin(post.getUserId()))
            return Result.error("管理员禁止参与发帖互动");
        if (post.getContent() == null && post.getImages() == null && post.getVideo() == null) {
            return Result.error("内容不能为空");
        }
        postService.createPost(post);
        return Result.success("发布成功");
    }

    @GetMapping("/list")
    public Result<PageInfo<Post>> list(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long currentUserId,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "0") Integer type,
            @RequestParam(defaultValue = "0") Integer category,
            @RequestParam(required = false) String keyword) {
        // 校验 pageNum，防止恶意请求
        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageNum > 10000) {
            pageNum = 10000;
        }

        // 限制最大每页数量，防止恶意请求大量数据
        if (pageSize > 50) {
            pageSize = 50;
        }
        if (pageSize < 1) {
            pageSize = 1;
        }
        if (currentUserId == null)
            currentUserId = 0L;
        if (userId == null)
            userId = 0L;
        PageInfo<Post> page = postService.getPostList(pageNum, pageSize, currentUserId, userId, type, category,
                keyword, isAdmin(currentUserId));
        return Result.success(page);
    }

    @GetMapping("/detail")
    public Result<Post> detail(@RequestParam Long id, @RequestParam(required = false) Long userId) {
        try {
            if (userId == null)
                userId = 0L;
            // 确保这里也是传两个参数
            Post post = postService.getPostDetail(id, userId, isAdmin(userId));
            return Result.success(post);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/hot")
    public Result<List<Post>> hot() {
        return Result.success(postService.getHotPosts());
    }

    @GetMapping("/scheduled")
    public Result<List<Post>> scheduled(@RequestParam Long userId) {
        return Result.success(postService.getScheduledPosts(userId));
    }

    @GetMapping("/polls")
    public Result<List<Post>> polls(@RequestParam Long userId) {
        return Result.success(postService.getMyPolls(userId));
    }

    @PostMapping("/like/{postId}")
    public Result<Boolean> like(@PathVariable Long postId, @RequestParam Long userId) {
        if (isAdmin(userId))
            return Result.error("管理员禁止点赞");
        boolean isLiked = postService.toggleLike(postId, userId);
        return Result.success(isLiked);
    }

    @PostMapping("/share/{postId}")
    public Result<?> share(@PathVariable Long postId) {
        postMapper.incrementShare(postId);
        return Result.success(null);
    }

    @PostMapping("/vote")
    public Result<?> vote(@RequestBody Map<String, Object> params) {
        Long postId = Long.valueOf(params.get("postId").toString());
        Long userId = Long.valueOf(params.get("userId").toString());
        Integer index = (Integer) params.get("index");

        if (isAdmin(userId))
            return Result.error("管理员禁止投票");

        try {
            // 检查投票是否已截止
            java.time.LocalDateTime pollEndTime = postMapper.getPollEndTime(postId);
            if (pollEndTime != null && java.time.LocalDateTime.now().isAfter(pollEndTime)) {
                return Result.error("投票已截止");
            }

            // 检查是否已投票
            if (postMapper.checkVote(postId, userId) != null) {
                return Result.error("您已经投过票了");
            }

            // 插入投票记录（依赖数据库唯一索引防止并发重复）
            postMapper.insertVote(postId, userId, index);

            // 返回最新的投票统计
            return Result.success(postMapper.selectVoteCounts(postId));
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 捕获唯一索引冲突异常
            return Result.error("您已经投过票了");
        } catch (Exception e) {
            return Result.error("投票失败");
        }
    }

    // 获取投票结果
    @GetMapping("/vote/result")
    public Result<?> getVoteResult(@RequestParam Long postId, @RequestParam(required = false) Long userId) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> counts = postMapper.selectVoteCounts(postId);
        result.put("counts", counts);
        result.put("myVote", userId != null ? postMapper.checkVote(postId, userId) : null);
        // 计算总投票人数
        int totalVotes = counts.stream().mapToInt(m -> ((Number) m.get("count")).intValue()).sum();
        result.put("totalVotes", totalVotes);
        // 获取投票截止时间
        result.put("pollEndTime", postMapper.getPollEndTime(postId));
        return Result.success(result);
    }

    /**
     * 删除帖子
     * 权限验证：只有作者或管理员可以删除
     */
    @DeleteMapping("/{id}")
    public Result<?> deletePost(@PathVariable Long id, @RequestParam Long userId) {
        Post post = postMapper.selectById(id, userId);
        if (post == null) {
            return Result.error("帖子不存在");
        }

        // 权限验证：只有作者或管理员可删除
        if (!post.getUserId().equals(userId) && !isAdmin(userId)) {
            return Result.error(403, "无权删除此帖子");
        }

        int rows = postMapper.deleteById(id);
        return rows > 0 ? Result.success("删除成功") : Result.error("删除失败");
    }
}