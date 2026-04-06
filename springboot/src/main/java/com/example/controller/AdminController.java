// File: springboot/src/main/java/com/example/controller/AdminController.java
package com.example.controller;

import com.example.common.Result;
import com.example.entity.AdminAuditLog;
import com.example.entity.Announcement;
import com.example.entity.Banner;
import com.example.entity.Category;
import com.example.entity.User;
import com.example.mapper.AdminAuditLogMapper;
import com.example.service.AdminService;
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * 管理员控制器
 * 所有接口通过AdminInterceptor拦截器验证管理员权限
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Resource
    private AdminService adminService;

    @Resource
    private AdminAuditLogMapper auditLogMapper;

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getDashboardData() {
        return Result.success(adminService.getDashboardStats());
    }

    @GetMapping("/post/list")
    public Result<PageInfo<Map<String, Object>>> getPostList(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize, @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status, @RequestParam(required = false) Integer category) {
        return Result.success(adminService.getPostList(pageNum, pageSize, keyword, status, category));
    }

    @GetMapping("/post/comments")
    public Result<List<Map<String, Object>>> getPostComments(@RequestParam Long postId) {
        return Result.success(adminService.getPostComments(postId));
    }

    @PostMapping("/comment/delete")
    public Result<?> deleteComment(@RequestBody Map<String, Object> params) {
        adminService.deleteComment(Long.valueOf(params.get("id").toString()), (String) params.get("reason"));
        return Result.success(null);
    }

    @PostMapping("/post/delete")
    public Result<?> deletePost(@RequestBody Map<String, Object> params) {
        adminService.deletePost(Long.valueOf(params.get("id").toString()), (String) params.get("reason"));
        return Result.success(null);
    }

    @GetMapping("/user/list")
    public Result<PageInfo<User>> getUserList(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize, @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer role, @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String orderBy, @RequestParam(required = false) String orderDir) {
        return Result.success(adminService.getUserList(pageNum, pageSize, keyword, role, status, orderBy, orderDir));
    }

    @PostMapping("/user/ban")
    public Result<?> banUser(@RequestBody Map<String, Object> params) {
        adminService.banUser(Long.valueOf(params.get("userId").toString()), (Integer) params.get("duration"),
                (String) params.get("reason"));
        return Result.success(null);
    }

    @PostMapping("/user/unban")
    public Result<?> unbanUser(@RequestBody Map<String, Object> params) {
        adminService.unbanUser(Long.valueOf(params.get("userId").toString()));
        return Result.success(null);
    }

    @PostMapping("/sensitive/add")
    public Result<?> addSensitiveWord(@RequestBody Map<String, String> params) {
        adminService.addSensitiveWord(params.get("word"));
        return Result.success(null);
    }

    @PostMapping("/sensitive/delete")
    public Result<?> deleteSensitiveWord(@RequestBody Map<String, String> params) {
        adminService.deleteSensitiveWord(params.get("word"));
        return Result.success(null);
    }

    @GetMapping("/sensitive/list")
    public Result<List<Map<String, Object>>> getSensitiveWords() {
        return Result.success(adminService.getSensitiveWords());
    }

    @GetMapping("/export")
    public void exportData(@RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate, HttpServletResponse response) {
        adminService.exportData(startDate, endDate, response);
    }

    @GetMapping("/export/new-users")
    public void exportNewUsers(@RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate, HttpServletResponse response) {
        adminService.exportNewUsers(startDate, endDate, response);
    }

    @GetMapping("/export/user-activity")
    public void exportUserActivity(@RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate, HttpServletResponse response) {
        adminService.exportUserActivity(startDate, endDate, response);
    }

    @GetMapping("/export/hotword")
    public void exportHotwordData(@RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate, HttpServletResponse response) {
        adminService.exportHotwordData(startDate, endDate, response);
    }

    @GetMapping("/export/bottle")
    public void exportBottleData(@RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate, HttpServletResponse response) {
        adminService.exportBottleData(startDate, endDate, response);
    }

    @GetMapping("/export/story")
    public void exportStoryData(@RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate, HttpServletResponse response) {
        adminService.exportStoryData(startDate, endDate, response);
    }

    @GetMapping("/export/comprehensive")
    public void exportComprehensiveReport(@RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate, HttpServletResponse response) {
        adminService.exportComprehensiveReport(startDate, endDate, response);
    }

    @GetMapping("/announcement/list")
    public Result<List<Announcement>> getAnnouncements() {
        return Result.success(adminService.getAnnouncementList());
    }

    @PostMapping("/announcement/add")
    public Result<?> addAnnouncement(@RequestBody Announcement announcement) {
        adminService.addAnnouncement(announcement);
        return Result.success(null);
    }

    @PostMapping("/announcement/delete")
    public Result<?> deleteAnnouncement(@RequestBody Map<String, Long> params) {
        adminService.deleteAnnouncement(params.get("id"));
        return Result.success(null);
    }

    @PostMapping("/announcement/top")
    public Result<?> toggleTop(@RequestBody Map<String, Long> params) {
        adminService.toggleAnnouncementTop(params.get("id"));
        return Result.success(null);
    }

    // --- 分类管理 ---
    @GetMapping("/category/list")
    public Result<List<Category>> getCategoryList() {
        return Result.success(adminService.getCategoryList());
    }

    @PostMapping("/category/add")
    public Result<?> addCategory(@RequestBody Category category) {
        adminService.addCategory(category);
        return Result.success(null);
    }

    @PostMapping("/category/update")
    public Result<?> updateCategory(@RequestBody Category category) {
        adminService.updateCategory(category);
        return Result.success(null);
    }

    @PostMapping("/category/delete")
    public Result<?> deleteCategory(@RequestBody Map<String, Long> params) {
        try {
            adminService.deleteCategory(params.get("id"));
            return Result.success(null);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // --- 举报管理 (新增) ---
    @GetMapping("/report/list")
    public Result<PageInfo<Map<String, Object>>> getReportList(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize, @RequestParam(required = false) Integer status) {
        return Result.success(adminService.getReportList(pageNum, pageSize, status));
    }

    @PostMapping("/report/handle")
    public Result<?> handleReport(@RequestBody Map<String, Object> params) {
        Long reportId = Long.valueOf(params.get("id").toString());
        Integer action = (Integer) params.get("action");
        adminService.handleReport(reportId, action);
        return Result.success(null);
    }

    // --- 轮播图管理 ---
    @GetMapping("/banner/list")
    public Result<List<Banner>> getBannerList() {
        return Result.success(adminService.getBannerList());
    }

    @PostMapping("/banner/add")
    public Result<?> addBanner(@RequestBody Banner banner) {
        adminService.addBanner(banner);
        return Result.success(null);
    }

    @PostMapping("/banner/update")
    public Result<?> updateBanner(@RequestBody Banner banner) {
        adminService.updateBanner(banner);
        return Result.success(null);
    }

    @PostMapping("/banner/delete")
    public Result<?> deleteBanner(@RequestBody Map<String, Long> params) {
        adminService.deleteBanner(params.get("id"));
        return Result.success(null);
    }

    /**
     * 查询审计日志
     */
    @GetMapping("/audit-logs")
    public Result<List<AdminAuditLog>> getAuditLogs(
            @RequestParam(required = false) Long adminId,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "100") Integer limit) {

        List<AdminAuditLog> logs;
        if (adminId != null) {
            logs = auditLogMapper.selectByAdminId(adminId);
        } else if (action != null) {
            logs = auditLogMapper.selectByAction(action, limit);
        } else {
            logs = auditLogMapper.selectRecentLogs(limit);
        }

        return Result.success(logs);
    }
}