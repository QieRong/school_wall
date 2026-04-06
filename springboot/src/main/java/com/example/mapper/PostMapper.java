// File: springboot/src/main/java/com/example/mapper/PostMapper.java
package com.example.mapper;

import com.example.entity.Post;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface PostMapper {

        @Insert("INSERT INTO post (user_id, content, images, video, location, category, is_anonymous, visibility, end_time, reward, deadline, status, create_time, view_count, like_count, comment_count, share_count, poll_options, poll_end_time, scheduled_time, tags, is_ai_assisted) "
                        +
                        "VALUES (#{userId}, #{content}, #{images}, #{video}, #{location}, #{category}, #{isAnonymous}, #{visibility}, #{endTime}, #{reward}, #{deadline}, #{status}, NOW(), 0, 0, 0, 0, #{pollOptions}, #{pollEndTime}, #{scheduledTime}, #{tags}, #{isAiAssisted})")
        @Options(useGeneratedKeys = true, keyProperty = "id")
        void insert(Post post);

        /**
         * 首页信息流查询 (全能版)
         * 包含 isLiked 字段
         */
        @Select("<script>" +
                        "SELECT p.*, u.nickname, u.avatar, u.account as userAccount, " +
                        "(SELECT COUNT(*) FROM sys_post_like WHERE post_id = p.id AND user_id = #{currentUserId}) > 0 AS isLiked "
                        +
                        "FROM post p " +
                        "LEFT JOIN sys_user u ON p.user_id = u.id " +
                        "WHERE p.status = 1 " +

                        "<if test='userId != null and userId != 0'>" +
                        "  AND p.user_id = #{userId} " +
                        // 隐私保护：查看他人主页时，隐藏匿名贴（除非是管理员）
                        "  <if test='userId != currentUserId and !isAdmin'>" +
                        "     AND p.is_anonymous = 0 " +
                        "  </if>" +
                        "</if>" +

                        "<if test='type == 1 and currentUserId != 0'>" +
                        "  AND p.user_id IN (SELECT target_id FROM sys_follow WHERE follower_id = #{currentUserId}) " +
                        "</if>" +

                        "<if test='type == 0'>" +
                        "  AND p.visibility = 0 " +
                        "</if>" +

                        "<if test='category != null and category != 0'>" +
                        "  AND p.category = #{category} " +
                        "</if>" +

                        "<if test='keyword != null and keyword != \"\"'>" +
                        "  AND (p.content LIKE CONCAT('%', #{keyword}, '%') OR u.nickname LIKE CONCAT('%', #{keyword}, '%') OR p.tags LIKE CONCAT('%', #{keyword}, '%')) "
                        +
                        "</if>" +

                        "<if test='currentUserId != 0'>" +
                        "  AND p.user_id NOT IN (SELECT target_id FROM sys_blacklist WHERE user_id = #{currentUserId}) "
                        +
                        "  AND p.user_id NOT IN (SELECT user_id FROM sys_blacklist WHERE target_id = #{currentUserId}) "
                        +
                        "</if>" +

                        "ORDER BY p.create_time DESC" +
                        "</script>")
        List<Post> selectAll(@Param("currentUserId") Long currentUserId,
                        @Param("userId") Long userId,
                        @Param("type") Integer type,
                        @Param("category") Integer category,
                        @Param("keyword") String keyword,
                        @Param("isAdmin") boolean isAdmin);

        // 热帖榜
        @Select("SELECT p.*, u.nickname, u.avatar, u.account as userAccount FROM post p " +
                        "LEFT JOIN sys_user u ON p.user_id = u.id " +
                        "WHERE p.status = 1 AND p.visibility = 0 " +
                        "ORDER BY (IFNULL(p.like_count, 0) * 2 + IFNULL(p.comment_count, 0)) DESC, p.create_time DESC "
                        +
                        "LIMIT 20")
        List<Post> selectHotPosts();

        // 【核心修复】改为双参数，支持查询 isLiked
        @Select("SELECT p.*, u.nickname, u.avatar, u.account as userAccount, " +
                        "(SELECT COUNT(*) FROM sys_post_like WHERE post_id = p.id AND user_id = #{currentUserId}) > 0 AS isLiked "
                        +
                        "FROM post p LEFT JOIN sys_user u ON p.user_id = u.id WHERE p.id = #{id}")
        Post selectById(@Param("id") Long id, @Param("currentUserId") Long currentUserId);

        // 根据ID删除帖子
        @Delete("DELETE FROM post WHERE id = #{id}")
        int deleteById(@Param("id") Long id);

        @Select("SELECT p.*, u.nickname, u.avatar, u.account as userAccount FROM post p LEFT JOIN sys_user u ON p.user_id = u.id WHERE p.user_id = #{userId} ORDER BY p.create_time DESC")
        List<Post> selectByUserId(Long userId);

        @Select("SELECT IFNULL(SUM(like_count), 0) FROM post WHERE user_id = #{userId}")
        int sumLikesByUserId(Long userId);

        // 查询待发布的定时帖子（status=2 表示待发布）
        @Select("SELECT * FROM post WHERE user_id = #{userId} AND status = 2 AND scheduled_time IS NOT NULL ORDER BY scheduled_time ASC")
        List<Post> selectScheduled(Long userId);

        @Select("SELECT * FROM post WHERE user_id = #{userId} AND poll_options IS NOT NULL")
        List<Post> selectPolls(Long userId);

        // 计数更新
        @Update("UPDATE post SET like_count = IFNULL(like_count, 0) + 1 WHERE id = #{postId}")
        void incrementLike(Long postId);

        @Update("UPDATE post SET share_count = IFNULL(share_count, 0) + 1 WHERE id = #{postId}")
        void incrementShare(Long postId);

        @Update("UPDATE post SET comment_count = IFNULL(comment_count, 0) + 1 WHERE id = #{postId}")
        void incrementComment(Long postId);

        @Update("UPDATE post SET view_count = IFNULL(view_count, 0) + 1 WHERE id = #{postId}")
        void incrementView(Long postId);

        // --- 点赞切换相关 ---
        @Select("SELECT COUNT(*) FROM sys_post_like WHERE post_id = #{postId} AND user_id = #{userId}")
        int checkIsLiked(@Param("postId") Long postId, @Param("userId") Long userId);

        @Insert("INSERT INTO sys_post_like (post_id, user_id, create_time) VALUES (#{postId}, #{userId}, NOW())")
        void insertPostLike(@Param("postId") Long postId, @Param("userId") Long userId);

        @Delete("DELETE FROM sys_post_like WHERE post_id = #{postId} AND user_id = #{userId}")
        void deletePostLike(@Param("postId") Long postId, @Param("userId") Long userId);

        @Update("UPDATE post SET like_count = like_count + #{delta} WHERE id = #{postId}")
        void updateLikeCount(@Param("postId") Long postId, @Param("delta") int delta);

        // --- 投票相关 ---
        @Insert("INSERT INTO post_vote (post_id, user_id, option_index, create_time) VALUES (#{postId}, #{userId}, #{optionIndex}, NOW())")
        void insertVote(@Param("postId") Long postId, @Param("userId") Long userId,
                        @Param("optionIndex") Integer optionIndex);

        @Select("SELECT option_index FROM post_vote WHERE post_id = #{postId} AND user_id = #{userId}")
        Integer checkVote(@Param("postId") Long postId, @Param("userId") Long userId);

        @Select("SELECT option_index, COUNT(*) as count FROM post_vote WHERE post_id = #{postId} GROUP BY option_index")
        List<Map<String, Object>> selectVoteCounts(Long postId);

        @Select("SELECT poll_end_time FROM post WHERE id = #{postId}")
        java.time.LocalDateTime getPollEndTime(Long postId);

        /**
         * 发布到期的定时帖子
         * 将 status=2 且 scheduled_time <= NOW() 的帖子改为 status=1
         * 【重要】同时将 create_time 更新为 scheduled_time，让帖子显示预定发布时间
         */
        @Update("UPDATE post SET status = 1, create_time = scheduled_time WHERE status = 2 AND scheduled_time IS NOT NULL AND scheduled_time <= NOW()")
        int publishScheduledPosts();

        // --- 热力图统计相关 ---
        @Select("<script>" +
                        "SELECT location, COUNT(*) as count, SUM(IFNULL(like_count, 0)) as likes " +
                        "FROM post " +
                        "WHERE status = 1 AND location IS NOT NULL AND location != '' " +
                        "<if test='category != null and category != 0'>" +
                        "  AND category = #{category} " +
                        "</if>" +
                        "<if test='days != null and days > 0'>" +
                        "  AND create_time >= DATE_SUB(NOW(), INTERVAL #{days} DAY) " +
                        "</if>" +
                        "GROUP BY location " +
                        "ORDER BY count DESC " +
                        "</script>")
        List<Map<String, Object>> selectLocationStats(@Param("category") Integer category, @Param("days") Integer days);

        @Select("SELECT location, COUNT(*) as count, SUM(IFNULL(like_count, 0)) as likes " +
                        "FROM post " +
                        "WHERE status = 1 AND location IS NOT NULL AND location != '' " +
                        "GROUP BY location " +
                        "ORDER BY count DESC " +
                        "LIMIT #{limit}")
        List<Map<String, Object>> selectHotLocations(@Param("limit") Integer limit);
}