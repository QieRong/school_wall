package com.example.mapper;

import com.example.entity.SysNotice;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Mapper
public interface NoticeMapper {

        @Insert("INSERT INTO sys_notice (user_id, sender_id, type, related_id, content, is_read, create_time) " +
                        "VALUES (#{receiverId}, #{senderId}, #{type}, #{relatedId}, #{content}, 0, NOW())")
        void insert(SysNotice notice);

        // 查询用户的通知列表 (支持按类型筛选，type=0查全部)
        // 如果 type 不为null，则追加 AND type = #{type}
        @Select("<script>" +
                        "SELECT n.*, u.nickname AS senderName, u.avatar AS senderAvatar " +
                        "FROM sys_notice n " +
                        "LEFT JOIN sys_user u ON n.sender_id = u.id " +
                        "WHERE n.user_id = #{userId} " +
                        "<if test='type != 0'> AND n.type = #{type} </if> " +
                        "ORDER BY n.is_read ASC, n.create_time DESC" +
                        "</script>")
        List<SysNotice> selectByUserId(Long userId, Integer type);

        // 一键已读 - 使用事务保护
        @Transactional
        @Update("UPDATE sys_notice SET is_read = 1 WHERE user_id = #{userId} AND is_read = 0")
        void readAll(Long userId);

        // 删除已读
        @Update("DELETE FROM sys_notice WHERE user_id = #{userId} AND is_read = 1")
        void deleteRead(Long userId);

        // 统计未读数
        @Select("SELECT COUNT(*) FROM sys_notice WHERE user_id = #{userId} AND is_read = 0")
        int countUnread(Long userId);

        // 单个已读
        @Update("UPDATE sys_notice SET is_read = 1 WHERE id = #{id}")
        void read(Integer id);
}