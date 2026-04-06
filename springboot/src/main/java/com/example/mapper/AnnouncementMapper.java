package com.example.mapper;

import com.example.entity.Announcement;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AnnouncementMapper {

    @Select("SELECT * FROM sys_announcement ORDER BY is_top DESC, create_time DESC")
    List<Announcement> selectAll();

    @Insert("INSERT INTO sys_announcement (title, content, is_top, create_time) VALUES (#{title}, #{content}, #{isTop}, NOW())")
    void insert(Announcement announcement);

    @Delete("DELETE FROM sys_announcement WHERE id = #{id}")
    void delete(Long id);

    // 切换置顶状态
    @Update("UPDATE sys_announcement SET is_top = #{isTop} WHERE id = #{id}")
    void updateTop(Long id, Integer isTop);
}