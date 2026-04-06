package com.example.mapper;

import com.example.entity.Post;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface SocialMapper {

    // --- 收藏 ---
    @Insert("INSERT IGNORE INTO sys_collection (user_id, post_id, create_time) VALUES (#{userId}, #{postId}, NOW())")
    void insertCollection(Long userId, Long postId);

    @Delete("DELETE FROM sys_collection WHERE user_id = #{userId} AND post_id = #{postId}")
    void deleteCollection(Long userId, Long postId);

    @Select("SELECT COUNT(*) FROM sys_collection WHERE user_id = #{userId} AND post_id = #{postId}")
    int checkCollection(Long userId, Long postId);

    // 使用SQL原子操作增加收藏计数
    @Update("UPDATE post SET collection_count = collection_count + 1 WHERE id = #{postId}")
    void incrementCollectionCount(Long postId);

    // 使用SQL原子操作减少收藏计数
    @Update("UPDATE post SET collection_count = collection_count - 1 WHERE id = #{postId} AND collection_count > 0")
    void decrementCollectionCount(Long postId);

    // 查询我的收藏列表 (关联帖子表)
    @Select("SELECT p.*, u.nickname, u.avatar " +
            "FROM sys_collection c " +
            "JOIN post p ON c.post_id = p.id " +
            "LEFT JOIN sys_user u ON p.user_id = u.id " +
            "WHERE c.user_id = #{userId} " +
            "ORDER BY c.create_time DESC")
    List<Post> selectMyCollections(Long userId);

    // --- 黑名单 ---
    @Insert("INSERT IGNORE INTO sys_blacklist (user_id, target_id, create_time) VALUES (#{userId}, #{targetId}, NOW())")
    void insertBlacklist(Long userId, Long targetId);

    @Delete("DELETE FROM sys_blacklist WHERE user_id = #{userId} AND target_id = #{targetId}")
    void deleteBlacklist(Long userId, Long targetId);

    // 检查是否有拉黑关系 (双向屏蔽：我拉黑他 或 他拉黑我)
    @Select("SELECT COUNT(*) FROM sys_blacklist WHERE (user_id = #{userId} AND target_id = #{targetId}) OR (user_id = #{targetId} AND target_id = #{userId})")
    int checkBlockStatus(Long userId, Long targetId);

    // 获取用户的黑名单列表
    @Select("SELECT b.id, b.target_id, u.nickname, u.avatar, b.create_time " +
            "FROM sys_blacklist b " +
            "LEFT JOIN sys_user u ON b.target_id = u.id " +
            "WHERE b.user_id = #{userId} " +
            "ORDER BY b.create_time DESC")
    List<java.util.Map<String, Object>> selectBlacklist(Long userId);
}