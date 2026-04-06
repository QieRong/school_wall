// File: springboot/src/main/java/com/example/mapper/ChatMapper.java
package com.example.mapper;

import com.example.entity.Chat;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Mapper
public interface ChatMapper {

        @Insert("INSERT INTO sys_chat (sender_id, receiver_id, content, type, is_read, is_withdrawn, create_time) " +
                        "VALUES (#{senderId}, #{receiverId}, #{content}, #{type}, 0, 0, NOW())")
        void insert(Chat chat);

        // 查询历史记录
        @Select("SELECT * FROM sys_chat " +
                        "WHERE (sender_id = #{userId} AND receiver_id = #{targetId}) " +
                        "   OR (sender_id = #{targetId} AND receiver_id = #{userId}) " +
                        "ORDER BY create_time ASC")
        List<Chat> selectHistory(Long userId, Long targetId);

        // 撤回消息
        @Update("UPDATE sys_chat SET is_withdrawn = 1 WHERE id = #{id}")
        void withdraw(Long id);

        // 标记已读 - 使用事务保护
        @Transactional
        @Update("UPDATE sys_chat SET is_read = 1 WHERE sender_id = #{targetId} AND receiver_id = #{userId} AND is_read = 0")
        void readAll(Long userId, Long targetId);

        // 【核心】查询最近联系人列表
        // 包含：对方信息、最后一条消息内容、最后消息时间、未读数
        @Select("SELECT u.id, u.nickname, u.avatar, u.last_active_time, " +
                        " (SELECT COUNT(*) FROM sys_chat WHERE sender_id = u.id AND receiver_id = #{userId} AND is_read = 0) as unreadCount, "
                        +
                        " (SELECT content FROM sys_chat WHERE (sender_id = u.id AND receiver_id = #{userId}) OR (sender_id = #{userId} AND receiver_id = u.id) ORDER BY create_time DESC LIMIT 1) as latestMsg, "
                        +
                        " (SELECT create_time FROM sys_chat WHERE (sender_id = u.id AND receiver_id = #{userId}) OR (sender_id = #{userId} AND receiver_id = u.id) ORDER BY create_time DESC LIMIT 1) as latestTime "
                        +
                        "FROM sys_user u " +
                        "WHERE u.id IN (SELECT sender_id FROM sys_chat WHERE receiver_id = #{userId} UNION SELECT receiver_id FROM sys_chat WHERE sender_id = #{userId}) "
                        +
                        "ORDER BY latestTime DESC")
        List<Map<String, Object>> selectContactList(Long userId);

        // 统计双方消息数 (用于陌生人限聊)
        @Select("SELECT COUNT(*) FROM sys_chat WHERE sender_id = #{senderId} AND receiver_id = #{receiverId}")
        int countBySenderAndReceiver(Long senderId, Long receiverId);

        @Select("SELECT * FROM sys_chat WHERE id = #{id}")
        Chat selectById(Long id);
}