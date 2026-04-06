// File: springboot/src/main/java/com/example/service/AdminService.java
package com.example.service;

import com.example.entity.Announcement;
import com.example.entity.Banner;
import com.example.entity.Category;
import com.example.entity.User;
import com.github.pagehelper.PageInfo;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;
import java.util.Map;

public interface AdminService {

    // --- 1. 仪表盘数据 ---
    Map<String, Object> getDashboardStats();

    // --- 2. 内容监管 ---
    PageInfo<Map<String, Object>> getPostList(Integer pageNum, Integer pageSize, String keyword, Integer status,
            Integer category);

    List<Map<String, Object>> getPostComments(Long postId);

    void deleteComment(Long commentId, String reason);

    void deletePost(Long postId, String reason);

    // --- 3. 用户管理 ---
    PageInfo<User> getUserList(Integer pageNum, Integer pageSize, String keyword, Integer role, Integer status, String orderBy, String orderDir);

    void banUser(Long userId, Integer duration, String reason);

    void unbanUser(Long userId);

    // --- 4. 敏感词管理 ---
    void addSensitiveWord(String word);

    void deleteSensitiveWord(String word);

    List<Map<String, Object>> getSensitiveWords();

    // --- 5. 数据导出 ---
    void exportData(String startDate, String endDate, HttpServletResponse response);

    // 导出新注册用户统计
    void exportNewUsers(String startDate, String endDate, HttpServletResponse response);

    // 导出用户活跃度数据
    void exportUserActivity(String startDate, String endDate, HttpServletResponse response);

    // 导出热词投票数据
    void exportHotwordData(String startDate, String endDate, HttpServletResponse response);

    // 导出漂流瓶数据
    void exportBottleData(String startDate, String endDate, HttpServletResponse response);

    // 导出故事接龙数据
    void exportStoryData(String startDate, String endDate, HttpServletResponse response);

    // 导出综合报表（包含所有数据）
    void exportComprehensiveReport(String startDate, String endDate, HttpServletResponse response);

    // --- 6. 公告管理 ---
    List<Announcement> getAnnouncementList();

    void addAnnouncement(Announcement announcement);

    void deleteAnnouncement(Long id);

    void toggleAnnouncementTop(Long id);

    // --- 7. 可视化分类管理 (您关心的功能在这里！) ---
    List<Category> getCategoryList();

    void addCategory(Category category);

    void updateCategory(Category category);

    void deleteCategory(Long id);

    // --- 8. 举报处理 (本次新增) ---
    PageInfo<Map<String, Object>> getReportList(Integer pageNum, Integer pageSize, Integer status);

    void handleReport(Long reportId, Integer action);

    // --- 9. 轮播图管理 ---
    List<Banner> getBannerList();

    void addBanner(Banner banner);

    void updateBanner(Banner banner);

    void deleteBanner(Long id);
}