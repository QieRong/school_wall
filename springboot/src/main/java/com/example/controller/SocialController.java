// File: springboot/src/main/java/com/example/controller/SocialController.java
package com.example.controller;

import com.example.common.Result;
import com.example.entity.Follow;
import com.example.service.SocialService; // 引入接口
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/social")
public class SocialController {

    @Resource
    private SocialService socialService; // 【核心修改】依赖接口，而非实现类

    // 关注
    @PostMapping("/follow")
    public Result<?> follow(@RequestBody Map<String, Long> params) {
        socialService.followUser(params.get("userId"), params.get("targetId"));
        return Result.success(null);
    }

    // 取关
    @PostMapping("/unfollow")
    public Result<?> unfollow(@RequestBody Map<String, Long> params) {
        socialService.unfollowUser(params.get("userId"), params.get("targetId"));
        return Result.success(null);
    }

    // 关注列表
    @GetMapping("/followings")
    public Result<List<Follow>> getFollowings(@RequestParam Long userId) {
        return Result.success(socialService.getFollowings(userId));
    }

    // 粉丝列表
    @GetMapping("/followers")
    public Result<List<Follow>> getFollowers(@RequestParam Long userId) {
        return Result.success(socialService.getFollowers(userId));
    }

    // 检查关注状态
    @GetMapping("/check_follow")
    public Result<Boolean> checkFollow(@RequestParam Long userId, @RequestParam Long targetId) {
        return Result.success(socialService.getFollowStatus(userId, targetId));
    }

    // 收藏/取消收藏
    @PostMapping("/collect")
    public Result<?> collect(@RequestBody Map<String, Long> params) {
        socialService.toggleCollection(params.get("userId"), params.get("postId"));
        return Result.success(null);
    }

    // 检查收藏状态
    @GetMapping("/check_collect")
    public Result<Boolean> checkCollect(@RequestParam Long userId, @RequestParam Long postId) {
        return Result.success(socialService.isCollected(userId, postId));
    }

    // 我的收藏（支持分页）
    @GetMapping("/collections")
    public Result<?> getCollections(@RequestParam Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(socialService.getMyCollections(userId, pageNum, pageSize));
    }

    // 拉黑
    @PostMapping("/block")
    public Result<?> block(@RequestBody Map<String, Long> params) {
        socialService.blockUser(params.get("userId"), params.get("targetId"));
        return Result.success(null);
    }

    // 解除拉黑
    @PostMapping("/unblock")
    public Result<?> unblock(@RequestBody Map<String, Long> params) {
        socialService.unblockUser(params.get("userId"), params.get("targetId"));
        return Result.success(null);
    }

    // 获取黑名单列表
    @GetMapping("/blacklist")
    public Result<?> getBlacklist(@RequestParam Long userId) {
        return Result.success(socialService.getBlacklist(userId));
    }
}