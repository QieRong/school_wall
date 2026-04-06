// File: springboot/src/main/java/com/example/service/SocialService.java
package com.example.service;

import com.example.entity.Follow;
import java.util.List;

public interface SocialService {

    // --- 关注相关 ---

    /**
     * 关注用户
     */
    void followUser(Long currentUserId, Long targetId);

    /**
     * 取消关注
     */
    void unfollowUser(Long currentUserId, Long targetId);

    /**
     * 获取我的关注列表
     */
    List<Follow> getFollowings(Long userId);

    /**
     * 获取我的粉丝列表
     */
    List<Follow> getFollowers(Long userId);

    /**
     * 查询关注状态
     * 
     * @return true:已关注, false:未关注
     */
    boolean getFollowStatus(Long currentUserId, Long targetId);

    // --- 收藏相关 ---

    /**
     * 切换收藏状态 (已收藏则取消，未收藏则添加)
     */
    void toggleCollection(Long userId, Long postId);

    /**
     * 查询是否已收藏
     */
    boolean isCollected(Long userId, Long postId);

    /**
     * 获取我的收藏列表（支持分页）
     */
    Object getMyCollections(Long userId, Integer pageNum, Integer pageSize);

    // --- 黑名单相关 ---

    /**
     * 拉黑用户
     */
    void blockUser(Long userId, Long targetId);

    /**
     * 解除拉黑
     */
    void unblockUser(Long userId, Long targetId);

    /**
     * 获取黑名单列表
     */
    java.util.List<java.util.Map<String, Object>> getBlacklist(Long userId);
}