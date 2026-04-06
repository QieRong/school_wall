package com.example.mapper;

import com.example.entity.Follow;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface FollowMapper {

        // 关注
        @Insert("INSERT IGNORE INTO sys_follow (follower_id, target_id, create_time) VALUES (#{followerId}, #{targetId}, NOW())")
        void insert(Long followerId, Long targetId);

        // 取消关注
        @Delete("DELETE FROM sys_follow WHERE follower_id = #{followerId} AND target_id = #{targetId}")
        void delete(Long followerId, Long targetId);

        // 使用SQL原子操作增加关注数
        @Update("UPDATE sys_user SET following_count = following_count + 1 WHERE id = #{userId}")
        void incrementFollowingCount(Long userId);

        // 使用SQL原子操作减少关注数
        @Update("UPDATE sys_user SET following_count = following_count - 1 WHERE id = #{userId} AND following_count > 0")
        void decrementFollowingCount(Long userId);

        // 使用SQL原子操作增加粉丝数
        @Update("UPDATE sys_user SET followers_count = followers_count + 1 WHERE id = #{userId}")
        void incrementFollowerCount(Long userId);

        // 使用SQL原子操作减少粉丝数
        @Update("UPDATE sys_user SET followers_count = followers_count - 1 WHERE id = #{userId} AND followers_count > 0")
        void decrementFollowerCount(Long userId);

        // 检查是否已关注
        @Select("SELECT COUNT(*) FROM sys_follow WHERE follower_id = #{followerId} AND target_id = #{targetId}")
        int checkFollow(Long followerId, Long targetId);

        // 获取我的关注列表 (isMutual=1 表示互关)
        // 逻辑：查出我关注的人，并关联查询这些人里是否有人也关注了我
        @Select("SELECT f.*, u.nickname, u.avatar, COALESCE(u.signature, '') as signature, " +
                        "(SELECT COUNT(*) FROM sys_follow f2 WHERE f2.follower_id = f.target_id AND f2.target_id = #{userId}) AS isMutual "
                        +
                        "FROM sys_follow f " +
                        "LEFT JOIN sys_user u ON f.target_id = u.id " +
                        "WHERE f.follower_id = #{userId} ORDER BY f.create_time DESC")
        List<Follow> selectFollowings(Long userId);

        // 获取我的粉丝列表
        // 逻辑：查出关注我的人，并关联查询我是否也关注了他们
        @Select("SELECT f.*, u.nickname, u.avatar, COALESCE(u.signature, '') as signature, " +
                        "(SELECT COUNT(*) FROM sys_follow f2 WHERE f2.follower_id = #{userId} AND f2.target_id = f.follower_id) AS isMutual "
                        +
                        "FROM sys_follow f " +
                        "LEFT JOIN sys_user u ON f.follower_id = u.id " +
                        "WHERE f.target_id = #{userId} ORDER BY f.create_time DESC")
        List<Follow> selectFollowers(Long userId);

        // 统计关注数
        @Select("SELECT COUNT(*) FROM sys_follow WHERE follower_id = #{userId}")
        int countFollowings(Long userId);

        // 统计粉丝数
        @Select("SELECT COUNT(*) FROM sys_follow WHERE target_id = #{userId}")
        int countFollowers(Long userId);
}