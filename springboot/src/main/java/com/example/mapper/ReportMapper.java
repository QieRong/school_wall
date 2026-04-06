// File: springboot/src/main/java/com/example/mapper/ReportMapper.java
package com.example.mapper;

import com.example.entity.Report;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface ReportMapper {

    @Insert("INSERT INTO sys_report (user_id, post_id, reason, status, create_time) VALUES (#{userId}, #{postId}, #{reason}, 0, NOW())")
    void insert(Report report);

    // 【新增】查询举报列表 (关联用户表和帖子表)
    // status: 0待处理 1已处理 2已驳回
    @Select("<script>" +
            "SELECT r.id, r.user_id AS reporterId, r.post_id AS postId, r.reason, r.status, " +
            "r.create_time AS createTime, " +
            "u.nickname AS reporterName, u.avatar AS reporterAvatar, " +
            "p.content AS postContent, p.images AS postImages, p.user_id AS postUserId " +
            "FROM sys_report r " +
            "LEFT JOIN sys_user u ON r.user_id = u.id " +
            "LEFT JOIN post p ON r.post_id = p.id " +
            "WHERE 1=1 " +
            "<if test='status != null'> AND r.status = #{status} </if> " +
            "ORDER BY r.create_time DESC" +
            "</script>")
    List<Map<String, Object>> selectReportList(Integer status);

    // 【新增】更新处理状态
    @Update("UPDATE sys_report SET status = #{status} WHERE id = #{id}")
    void updateStatus(Long id, Integer status);

    @Select("SELECT * FROM sys_report WHERE id = #{id}")
    Report selectById(Long id);
}