// File: springboot/src/main/java/com/example/controller/IndexController.java
package com.example.controller;

import com.example.common.Result;
import com.example.entity.Announcement;
import com.example.entity.Banner;
import com.example.mapper.AnnouncementMapper;
import com.example.mapper.IndexMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.entity.Category;
import com.example.mapper.CategoryMapper;

import jakarta.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/index")
public class IndexController {

    @Resource
    private IndexMapper indexMapper;
    
    @Resource
    private CategoryMapper categoryMapper; // 注入


    @Resource
    private AnnouncementMapper announcementMapper; // 注入

    @GetMapping("/banner")
    public Result<List<Banner>> getBanner() {
        return Result.success(indexMapper.selectBanner());
    }

    /**
     * 获取公告列表 (真实数据库数据)
     */
    @GetMapping("/announcements")
    public Result<List<Announcement>> getAnnouncements() {
        // 调用 Mapper 直接查库，按置顶和时间排序
        return Result.success(announcementMapper.selectAll());
    }


    // 获取前台可用分类
    @GetMapping("/categories")
    public Result<List<Category>> getCategories() {
        return Result.success(categoryMapper.selectActive());
    }
}