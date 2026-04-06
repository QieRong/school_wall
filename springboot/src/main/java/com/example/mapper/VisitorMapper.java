package com.example.mapper;

import com.example.entity.Visitor;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface VisitorMapper {

    @Insert("INSERT INTO sys_visitor (user_id, visitor_id, visit_time) VALUES (#{userId}, #{visitorId}, NOW())")
    void insert(Visitor visitor);

    @Select("SELECT v.*, u.nickname AS visitorName, u.avatar AS visitorAvatar " +
            "FROM sys_visitor v " +
            "LEFT JOIN sys_user u ON v.visitor_id = u.id " +
            "WHERE v.user_id = #{userId} " +
            "ORDER BY v.visit_time DESC LIMIT 50") // 文档要求最近50位
    List<Visitor> selectRecentVisitors(Long userId);

    @Select("SELECT COUNT(*) FROM sys_visitor WHERE user_id = #{userId}")
    int countByUserId(Long userId);

    // 【核心修正】检查24小时内是否访问过
    @Select("SELECT COUNT(*) FROM sys_visitor " +
            "WHERE user_id = #{userId} " +
            "AND visitor_id = #{visitorId} " +
            "AND visit_time > DATE_SUB(NOW(), INTERVAL 24 HOUR)")
    int checkTodayVisit(Long userId, Long visitorId);
}