package com.example.mapper;

import com.example.entity.Category;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CategoryMapper {

    // 查询所有可用分类 (给前台用)
    @Select("SELECT * FROM sys_category WHERE status = 1 ORDER BY sort ASC")
    List<Category> selectActive();

    // 查询所有分类 (给后台管理用)
    @Select("SELECT * FROM sys_category ORDER BY sort ASC")
    List<Category> selectAll();

    @Insert("INSERT INTO sys_category (name, icon, color, bg_color, sort, status, create_time) " +
            "VALUES (#{name}, #{icon}, #{color}, #{bgColor}, #{sort}, 1, NOW())")
    void insert(Category category);

    @Update("UPDATE sys_category SET name=#{name}, icon=#{icon}, color=#{color}, bg_color=#{bgColor}, sort=#{sort}, status=#{status} WHERE id=#{id}")
    void update(Category category);

    @Delete("DELETE FROM sys_category WHERE id = #{id}")
    void delete(Long id);
}