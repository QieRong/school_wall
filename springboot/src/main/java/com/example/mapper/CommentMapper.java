package com.example.mapper;

import com.example.entity.Comment;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CommentMapper {

    @Insert("INSERT INTO comment (post_id, user_id, content, parent_id, reply_user_id, like_count, img_url, create_time) " +
            "VALUES (#{postId}, #{userId}, #{content}, #{parentId}, #{replyUserId}, 0, #{imgUrl}, NOW())")
    int insert(Comment comment);

    // 查询某帖子的所有评论 (带用户信息)
    // 按热度(点赞)和时间排序
    @Select("SELECT c.*, u.nickname, u.avatar, ru.nickname AS replyNickname " +
            "FROM comment c " +
            "LEFT JOIN sys_user u ON c.user_id = u.id " +
            "LEFT JOIN sys_user ru ON c.reply_user_id = ru.id " +
            "WHERE c.post_id = #{postId} " +
            "ORDER BY c.like_count DESC, c.create_time ASC")
    List<Comment> selectByPostId(Long postId);

    // 评论点赞 +1
    @Update("UPDATE comment SET like_count = like_count + 1 WHERE id = #{id}")
    void incrementLike(Long id);
}