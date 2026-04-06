package com.example.controller;

import com.example.common.Result;
import com.example.mapper.PostMapper;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.*;

/**
 * 统计数据接口（热力图等）
 */
@RestController
@RequestMapping("/stats")
public class StatsController {

    @Resource
    private PostMapper postMapper;

    /**
     * 获取表白热力图数据
     * 基于帖子的地理位置信息聚合统计
     */
    @GetMapping("/location-heatmap")
    public Result<Map<String, Object>> getLocationHeatmap(
            @RequestParam(defaultValue = "1") Integer category,
            @RequestParam(defaultValue = "30") Integer days) {
        
        List<Map<String, Object>> rawData = postMapper.selectLocationStats(category, days);
        
        // 转换为热力图格式
        List<Map<String, Object>> points = new ArrayList<>();
        int totalPosts = 0;
        int maxCount = 0;
        
        for (Map<String, Object> item : rawData) {
            String location = (String) item.get("location");
            Number countNum = (Number) item.get("count");
            Number likesNum = (Number) item.get("likes");
            
            if (location == null || location.isEmpty()) continue;
            
            int count = countNum != null ? countNum.intValue() : 0;
            int likes = likesNum != null ? likesNum.intValue() : 0;
            
            // 热度 = 帖子数 + 点赞数/10
            int heat = count + likes / 10;
            
            totalPosts += count;
            if (heat > maxCount) maxCount = heat;
            
            Map<String, Object> point = new HashMap<>();
            point.put("location", location);
            point.put("count", count);
            point.put("likes", likes);
            point.put("heat", heat);
            points.add(point);
        }
        
        // 按热度排序
        points.sort((a, b) -> ((Integer) b.get("heat")).compareTo((Integer) a.get("heat")));
        
        Map<String, Object> result = new HashMap<>();
        result.put("points", points);
        result.put("totalPosts", totalPosts);
        result.put("maxHeat", maxCount);
        result.put("locationCount", points.size());
        
        return Result.success(result);
    }

    /**
     * 获取热门地点排行
     */
    @GetMapping("/hot-locations")
    public Result<List<Map<String, Object>>> getHotLocations(
            @RequestParam(defaultValue = "10") Integer limit) {
        
        List<Map<String, Object>> locations = postMapper.selectHotLocations(limit);
        return Result.success(locations);
    }
}
