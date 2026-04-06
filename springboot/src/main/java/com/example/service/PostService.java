// File: springboot/src/main/java/com/example/service/PostService.java
package com.example.service;

import com.example.entity.Post;
import com.github.pagehelper.PageInfo;
import java.util.List;

public interface PostService {

    /**
     * 创建帖子
     */
    void createPost(Post post);

    /**
     * 获取帖子列表 (核心查询接口)
     * 
     * @param currentUserId 当前登录用户ID (用于判断isLiked和黑名单)
     * @param userId        目标用户ID (用于筛选特定用户的帖子，0表示不筛选)
     * @param type          0:推荐/最新, 1:关注流
     * @param category      分类ID (0:全部)
     * @param keyword       搜索关键词
     */
    PageInfo<Post> getPostList(Integer pageNum, Integer pageSize, Long currentUserId, Long userId, Integer type,
            Integer category, String keyword, boolean isAdmin);

    /**
     * 获取帖子详情
     */
    Post getPostDetail(Long postId, Long currentUserId, boolean isAdmin);

    /**
     * 获取热帖榜
     */
    List<Post> getHotPosts();

    /**
     * 获取我的定时发布帖子
     */
    List<Post> getScheduledPosts(Long userId);

    /**
     * 获取我发起的投票贴
     */
    List<Post> getMyPolls(Long userId);
}