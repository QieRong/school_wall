// File: springboot/src/main/java/com/example/service/impl/SocialServiceImpl.java
package com.example.service.impl;

import com.example.entity.Follow;
import com.example.entity.Post;
import com.example.mapper.FollowMapper;
import com.example.mapper.SocialMapper;
import com.example.server.WebSocketServer;
import com.example.service.SocialService; // 引入接口
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

@Service
public class SocialServiceImpl implements SocialService { // 【核心修改】实现接口

    @Resource
    private FollowMapper followMapper;
    @Resource
    private SocialMapper socialMapper;

    // --- 关注 ---
    @Override
    public void followUser(Long currentUserId, Long targetId) {
        if (currentUserId.equals(targetId))
            throw new RuntimeException("不能关注自己");
        // 插入关注
        followMapper.insert(currentUserId, targetId);
        // 使用SQL原子操作更新关注计数
        followMapper.incrementFollowingCount(currentUserId);
        followMapper.incrementFollowerCount(targetId);
        // 推送通知 (类型4: 关注)
        WebSocketServer.sendNotification(targetId, "有人关注了你", 4);
    }

    @Override
    public void unfollowUser(Long currentUserId, Long targetId) {
        followMapper.delete(currentUserId, targetId);
        // 使用SQL原子操作更新关注计数
        followMapper.decrementFollowingCount(currentUserId);
        followMapper.decrementFollowerCount(targetId);
    }

    @Override
    public List<Follow> getFollowings(Long userId) {
        return followMapper.selectFollowings(userId);
    }

    @Override
    public List<Follow> getFollowers(Long userId) {
        return followMapper.selectFollowers(userId);
    }

    @Override
    public boolean getFollowStatus(Long currentUserId, Long targetId) {
        if (currentUserId == null)
            return false;
        return followMapper.checkFollow(currentUserId, targetId) > 0;
    }

    // --- 收藏 ---
    @Override
    public void toggleCollection(Long userId, Long postId) {
        int count = socialMapper.checkCollection(userId, postId);
        if (count > 0) {
            socialMapper.deleteCollection(userId, postId);
            // 使用SQL原子操作减少收藏计数
            socialMapper.decrementCollectionCount(postId);
        } else {
            socialMapper.insertCollection(userId, postId);
            // 使用SQL原子操作增加收藏计数
            socialMapper.incrementCollectionCount(postId);
        }
    }

    @Override
    public boolean isCollected(Long userId, Long postId) {
        if (userId == null)
            return false;
        return socialMapper.checkCollection(userId, postId) > 0;
    }

    @Override
    public Object getMyCollections(Long userId, Integer pageNum, Integer pageSize) {
        com.github.pagehelper.PageHelper.startPage(pageNum, pageSize);
        List<Post> list = socialMapper.selectMyCollections(userId);
        return new com.github.pagehelper.PageInfo<>(list);
    }

    // --- 黑名单 ---
    @Override
    public void blockUser(Long userId, Long targetId) {
        if (userId.equals(targetId))
            throw new RuntimeException("不能拉黑自己");
        socialMapper.insertBlacklist(userId, targetId);
        // 拉黑后自动取消关注
        followMapper.delete(userId, targetId); // 我取关他
        // followMapper.delete(targetId, userId); // (可选)他也取关我
    }

    @Override
    public void unblockUser(Long userId, Long targetId) {
        socialMapper.deleteBlacklist(userId, targetId);
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> getBlacklist(Long userId) {
        return socialMapper.selectBlacklist(userId);
    }
}