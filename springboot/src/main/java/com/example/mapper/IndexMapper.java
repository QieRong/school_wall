// File: springboot/src/main/java/com/example/mapper/IndexMapper.java
package com.example.mapper;

import com.example.entity.Banner;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface IndexMapper {

    /**
     * 获取轮播图列表
     * 对应数据库表: sys_banner
     */
    @Select("SELECT * FROM sys_banner ORDER BY sort ASC")
    List<Banner> selectBanner();
}