// File: springboot/src/main/java/com/example/service/impl/AdminServiceImpl.java
package com.example.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.example.entity.AdminAuditLog;
import com.example.entity.Announcement;
import com.example.entity.Banner;
import com.example.entity.Category;
import com.example.entity.Report;
import com.example.entity.SysNotice;
import com.example.entity.User;
import com.example.mapper.AdminAuditLogMapper;
import com.example.mapper.AnnouncementMapper;
import com.example.mapper.CategoryMapper;
import com.example.mapper.NoticeMapper;
import com.example.mapper.ReportMapper;
import com.example.mapper.UserMapper;
import com.example.server.WebSocketServer;
import com.example.service.AdminService;
import com.example.utils.JwtUtils;
import com.example.utils.SensitiveFilter;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.*;

@Service
@SuppressWarnings("null")
public class AdminServiceImpl implements AdminService {

    // 【优化】添加日志记录
    private static final Logger log = LoggerFactory.getLogger(AdminServiceImpl.class);

    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    private SensitiveFilter sensitiveFilter;
    @Resource
    private NoticeMapper noticeMapper;
    @Resource
    private AnnouncementMapper announcementMapper;
    @Resource
    private CategoryMapper categoryMapper;
    @Resource
    private ReportMapper reportMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private AdminAuditLogMapper auditLogMapper;
    @Resource
    private JwtUtils jwtUtils;
    @Resource
    private HttpServletRequest request;

    /**
     * 记录审计日志
     */
    private void recordAuditLog(String action, String targetType, Long targetId, String reason) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("无法记录审计日志：未找到有效的Authorization header");
                return;
            }

            String token = authHeader.substring(7);
            Long adminId = jwtUtils.verifyToken(token);

            if (adminId == null) {
                log.warn("无法记录审计日志：token验证失败");
                return;
            }

            User admin = userMapper.selectById(adminId);

            AdminAuditLog auditLog = new AdminAuditLog();
            auditLog.setAdminId(adminId);
            auditLog.setAdminName(admin != null ? admin.getNickname() : "未知管理员");
            auditLog.setAction(action);
            auditLog.setTargetType(targetType);
            auditLog.setTargetId(targetId);
            auditLog.setReason(reason);
            auditLog.setIpAddress(getClientIp());
            auditLog.setCreateTime(LocalDateTime.now());

            auditLogMapper.insert(auditLog);
            log.info("【审计日志】管理员={}, 操作={}, 目标={}#{}, 原因={}",
                    auditLog.getAdminName(), action, targetType, targetId, reason);
        } catch (Exception e) {
            log.error("记录审计日志失败", e);
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp() {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    // --- 1. 数据大屏 ---
    @Override
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", jdbcTemplate.queryForObject("SELECT count(*) FROM sys_user", Integer.class));
        stats.put("todayNewUsers", jdbcTemplate
                .queryForObject("SELECT count(*) FROM sys_user WHERE DATE(create_time) = CURDATE()", Integer.class));
        stats.put("todayActiveUsers", jdbcTemplate.queryForObject(
                "SELECT count(*) FROM sys_user WHERE DATE(last_active_time) = CURDATE()", Integer.class));
        stats.put("totalPosts", jdbcTemplate.queryForObject("SELECT count(*) FROM post", Integer.class));
        stats.put("todayNewPosts", jdbcTemplate
                .queryForObject("SELECT count(*) FROM post WHERE DATE(create_time) = CURDATE()", Integer.class));
        stats.put("pendingAudit",
                jdbcTemplate.queryForObject("SELECT count(*) FROM post WHERE status = 0", Integer.class));
        return stats;
    }

    // --- 2. 内容监管 ---
    @Override
    public PageInfo<Map<String, Object>> getPostList(Integer pageNum, Integer pageSize, String keyword, Integer status,
            Integer category) {
        
        // 【物理分页修复】由于使用了 JdbcTemplate，PageHelper 无法拦截，需手写 LIMIT 分页
        StringBuilder countSql = new StringBuilder(
                "SELECT count(*) FROM post p LEFT JOIN sys_user u ON p.user_id = u.id WHERE 1=1 ");
        StringBuilder sql = new StringBuilder(
                "SELECT p.*, u.nickname, u.account, u.avatar FROM post p LEFT JOIN sys_user u ON p.user_id = u.id WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (StrUtil.isNotBlank(keyword)) {
            String condition = "AND (p.content LIKE ? OR u.nickname LIKE ?) ";
            countSql.append(condition);
            sql.append(condition);
            params.add("%" + keyword + "%");
            params.add("%" + keyword + "%");
        }
        if (status != null) {
            String condition = "AND p.status = ? ";
            countSql.append(condition);
            sql.append(condition);
            params.add(status);
        }
        if (category != null && category != 0) {
            String condition = "AND p.category = ? ";
            countSql.append(condition);
            sql.append(condition);
            params.add(category);
        }
        
        // 查询总记录数
        Long total = jdbcTemplate.queryForObject(countSql.toString(), Long.class, params.toArray());

        // 默认按照发布时间最新(倒序)排列
        sql.append("ORDER BY p.create_time DESC ");
        
        // 增加分页参数 LIMIT pageSize OFFSET offset
        sql.append("LIMIT ? OFFSET ?");
        params.add(pageSize);
        params.add((pageNum - 1) * pageSize);

        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql.toString(), params.toArray());

        for (Map<String, Object> map : list) {
            String imgs = (String) map.get("images");
            if (StrUtil.isNotEmpty(imgs)) {
                // 兼容JSON数组和逗号分隔两种格式
                List<String> imgList = parseImageUrls(imgs);
                map.put("imgList", imgList);
            }
        }
        
        // 构造 PageInfo 并通过反射或直接设值欺骗前端（前端取 res.data.list 和 res.data.total）
        PageInfo<Map<String, Object>> pageInfo = new PageInfo<>(list);
        pageInfo.setTotal(total == null ? 0 : total);
        return pageInfo;
    }

    /**
     * 解析图片URL列表，兼容JSON数组和逗号分隔两种格式
     */
    private List<String> parseImageUrls(String images) {
        if (StrUtil.isBlank(images)) {
            return Collections.emptyList();
        }

        // 尝试JSON格式解析
        String trimmed = images.trim();
        if (trimmed.startsWith("[")) {
            try {
                JSONArray arr = JSONUtil.parseArray(trimmed);
                List<String> result = new ArrayList<>();
                for (Object obj : arr) {
                    if (obj != null) {
                        result.add(obj.toString());
                    }
                }
                return result;
            } catch (Exception e) {
                // JSON解析失败，降级为逗号分隔
            }
        }

        // 逗号分隔格式（兼容旧数据）
        String[] urls = images.split(",");
        List<String> result = new ArrayList<>();
        for (String url : urls) {
            if (StrUtil.isNotBlank(url)) {
                result.add(url.trim());
            }
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getPostComments(Long postId) {
        return jdbcTemplate.queryForList(
                "SELECT c.*, u.nickname, u.avatar FROM comment c LEFT JOIN sys_user u ON c.user_id = u.id WHERE c.post_id = ? ORDER BY c.create_time ASC",
                postId);
    }

    @Override
    public void deleteComment(Long commentId, String reason) {
        Map<String, Object> comment = jdbcTemplate.queryForMap("SELECT * FROM comment WHERE id = ?", commentId);
        Long commentUserId = (Long) comment.get("user_id");
        Long postId = (Long) comment.get("post_id");
        String content = (String) comment.get("content");
        if (content.length() > 20)
            content = content.substring(0, 20) + "...";

        Long postUserId = jdbcTemplate.queryForObject("SELECT user_id FROM post WHERE id = ?", Long.class, postId);

        jdbcTemplate.update("DELETE FROM comment WHERE id = ?", commentId);
        jdbcTemplate.update(
                "INSERT INTO sys_punishment (user_id, type, score_deducted, reason, create_time) VALUES (?, 1, 0, ?, NOW())",
                commentUserId, "评论违规删除: " + reason);

        sendSysNotice(commentUserId, "您的评论「" + content + "」因「" + reason + "」已被管理员删除，请遵守社区规范。");

        if (!postUserId.equals(commentUserId)) {
            sendSysNotice(postUserId, "您帖子中的评论「" + content + "」因「" + reason + "」已被删除，感谢您维护社区环境。");
        }

        recordAuditLog("DELETE_COMMENT", "COMMENT", commentId, reason);
    }

    @Override
    public void deletePost(Long postId, String reason) {
        Long userId = jdbcTemplate.queryForObject("SELECT user_id FROM post WHERE id = ?", Long.class, postId);

        log.info("【删帖操作】postId={}, userId={}, reason={}", postId, userId, reason);

        jdbcTemplate.update("DELETE FROM post WHERE id = ?", postId);
        jdbcTemplate.update("DELETE FROM comment WHERE post_id = ?", postId);
        jdbcTemplate.update("DELETE FROM post_vote WHERE post_id = ?", postId);
        jdbcTemplate.update("DELETE FROM sys_collection WHERE post_id = ?", postId);
        jdbcTemplate.update("DELETE FROM sys_post_like WHERE post_id = ?", postId);

        if (userId != null) {
            jdbcTemplate.update(
                    "INSERT INTO sys_punishment (user_id, type, score_deducted, reason, create_time) VALUES (?, 1, 0, ?, NOW())",
                    userId, "帖子违规删除: " + reason);
            sendSysNotice(userId, "您的帖子因「" + reason + "」已被管理员删除，请遵守社区规范。");
        }

        recordAuditLog("DELETE_POST", "POST", postId, reason);
    }

    // --- 3. 用户管理 ---
    @Override
    public PageInfo<User> getUserList(Integer pageNum, Integer pageSize, String keyword,
            Integer role, Integer status, String orderBy, String orderDir) {
        // 使用手写分页 LIMIT/OFFSET，避免与 jdbcTemplate 子查询冲突
        StringBuilder baseSql = new StringBuilder("FROM sys_user u WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        
        if (StrUtil.isNotBlank(keyword)) {
            baseSql.append("AND (u.nickname LIKE ? OR u.account LIKE ?) ");
            params.add("%" + keyword + "%");
            params.add("%" + keyword + "%");
        }
        if (role != null) {
            baseSql.append("AND u.role = ? ");
            params.add(role);
        }
        if (status != null) {
            baseSql.append("AND u.status = ? ");
            params.add(status);
        }

        // 白名单校验排序字段，防止 SQL 注入
        java.util.Set<String> allowedOrderBy = java.util.Set.of("creditScore", "violationCount", "createTime");
        // camelCase 转 snake_case 映射
        java.util.Map<String, String> colMap = java.util.Map.of(
            "creditScore", "u.credit_score",
            "violationCount", "u.violation_count",
            "createTime", "u.create_time"
        );
        String sortCol = (orderBy != null && allowedOrderBy.contains(orderBy))
                ? colMap.get(orderBy) : "u.create_time";
        String sortDir = "asc".equalsIgnoreCase(orderDir) ? "ASC" : "DESC";

        // 查询总记录数
        String countSql = "SELECT count(*) " + baseSql.toString();
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());

        // 查询分页数据明细
        String sql = "SELECT u.*, " +
                "(SELECT COUNT(*) FROM post WHERE user_id = u.id AND status = 1) AS postCount, " +
                "(SELECT COUNT(*) FROM comment WHERE user_id = u.id) AS commentCount " +
                baseSql.toString() +
                "ORDER BY " + sortCol + " " + sortDir + " LIMIT ? OFFSET ?";
                
        params.add(pageSize);
        params.add((pageNum - 1) * pageSize);

        List<User> list = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(User.class), params.toArray());
        
        PageInfo<User> pageInfo = new PageInfo<>(list);
        pageInfo.setTotal(total == null ? 0 : total);
        return pageInfo;
    }

    @Override
    public void banUser(Long userId, Integer duration, String reason) {
        log.info("【封号操作】userId={}, duration={}, reason={}", userId, duration, reason);

        LocalDateTime banUntil = (duration == -1) ? LocalDateTime.now().plusYears(99)
                : LocalDateTime.now().plusDays(duration);
        jdbcTemplate.update("UPDATE sys_user SET status = 0, ban_end_time = ? WHERE id = ?", banUntil, userId);
        jdbcTemplate.update(
                "INSERT INTO sys_punishment (user_id, type, duration_days, reason, create_time) VALUES (?, 2, ?, ?, NOW())",
                userId, duration, reason);
        sendSysNotice(userId, "您已被管理员封禁，原因：" + reason);

        recordAuditLog("BAN_USER", "USER", userId, reason + " (封禁" + duration + "天)");
    }

    @Override
    public void unbanUser(Long userId) {
        log.info("【解封操作】userId={}", userId);

        jdbcTemplate.update("UPDATE sys_user SET status = 1, ban_end_time = NULL WHERE id = ?", userId);
        jdbcTemplate.update(
                "INSERT INTO sys_punishment (user_id, type, score_deducted, reason, create_time) VALUES (?, 3, 0, '管理员手动解封', NOW())",
                userId);

        recordAuditLog("UNBAN_USER", "USER", userId, "解除封禁");
    }

    // --- 4. 敏感词 ---
    @Override
    public void addSensitiveWord(String word) {
        jdbcTemplate.update("INSERT IGNORE INTO sys_sensitive (word) VALUES (?)", word);
        reloadFilter();
    }

    @Override
    public void deleteSensitiveWord(String word) {
        jdbcTemplate.update("DELETE FROM sys_sensitive WHERE word = ?", word);
        reloadFilter();
    }

    @Override
    public List<Map<String, Object>> getSensitiveWords() {
        return jdbcTemplate.queryForList("SELECT word, create_time AS createTime FROM sys_sensitive ORDER BY id DESC");
    }

    private void reloadFilter() {
        List<String> words = jdbcTemplate.queryForList("SELECT word FROM sys_sensitive", String.class);
        sensitiveFilter.refresh(words);
    }

    // --- 5. 数据导出 ---
    @Override
    public void exportData(String startDate, String endDate, HttpServletResponse response) {
        try {
            String sql = "SELECT p.id, u.account, u.nickname, p.content, p.category, p.create_time, p.view_count, p.like_count FROM post p LEFT JOIN sys_user u ON p.user_id = u.id WHERE p.create_time BETWEEN ? AND ? ORDER BY p.create_time DESC";
            if (StrUtil.isBlank(startDate))
                startDate = "2020-01-01 00:00:00";
            if (StrUtil.isBlank(endDate))
                endDate = DateUtil.now();

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, startDate, endDate);
            ExcelWriter writer = ExcelUtil.getWriter(true);
            writer.addHeaderAlias("id", "帖子ID");
            writer.addHeaderAlias("account", "用户账号");
            writer.addHeaderAlias("nickname", "用户昵称");
            writer.addHeaderAlias("content", "帖子内容");
            writer.addHeaderAlias("category", "分类ID");
            writer.addHeaderAlias("create_time", "发布时间");
            writer.addHeaderAlias("view_count", "浏览量");
            writer.addHeaderAlias("like_count", "点赞数");

            writer.write(rows, true);
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
            String fileName = URLEncoder.encode("表白墙运营数据报表", "UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");
            ServletOutputStream out = response.getOutputStream();
            writer.flush(out, true);
            writer.close();
            IoUtil.close(out);
        } catch (Exception e) {
            throw new RuntimeException("导出失败: " + e.getMessage());
        }
    }

    @Override
    public void exportNewUsers(String startDate, String endDate, HttpServletResponse response) {
        try {
            if (StrUtil.isBlank(startDate))
                startDate = "2020-01-01 00:00:00";
            if (StrUtil.isBlank(endDate))
                endDate = DateUtil.now();

            String sql = "SELECT id, account, nickname, email, phone, gender, create_time, credit_score, level " +
                    "FROM sys_user WHERE create_time BETWEEN ? AND ? ORDER BY create_time DESC";

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, startDate, endDate);

            ExcelWriter writer = ExcelUtil.getWriter(true);
            writer.addHeaderAlias("id", "用户ID");
            writer.addHeaderAlias("account", "账号");
            writer.addHeaderAlias("nickname", "昵称");
            writer.addHeaderAlias("email", "邮箱");
            writer.addHeaderAlias("phone", "手机号");
            writer.addHeaderAlias("gender", "性别");
            writer.addHeaderAlias("create_time", "注册时间");
            writer.addHeaderAlias("credit_score", "信誉分");
            writer.addHeaderAlias("level", "等级");

            writer.write(rows, true);
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
            String fileName = URLEncoder.encode("新注册用户统计", "UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");
            ServletOutputStream out = response.getOutputStream();
            writer.flush(out, true);
            writer.close();
            IoUtil.close(out);
        } catch (Exception e) {
            throw new RuntimeException("导出失败: " + e.getMessage());
        }
    }

    @Override
    public void exportUserActivity(String startDate, String endDate, HttpServletResponse response) {
        try {
            if (StrUtil.isBlank(startDate))
                startDate = "2020-01-01 00:00:00";
            if (StrUtil.isBlank(endDate))
                endDate = DateUtil.now();

            String sql = "SELECT u.id, u.account, u.nickname, " +
                    "(SELECT COUNT(*) FROM post WHERE user_id = u.id AND create_time BETWEEN ? AND ?) as post_count, " +
                    "(SELECT COUNT(*) FROM comment WHERE user_id = u.id AND create_time BETWEEN ? AND ?) as comment_count, "
                    +
                    "(SELECT COUNT(*) FROM sys_post_like WHERE user_id = u.id AND create_time BETWEEN ? AND ?) as like_count, "
                    +
                    "u.last_login_time, u.credit_score " +
                    "FROM sys_user u " +
                    "WHERE u.create_time <= ? " +
                    "ORDER BY post_count DESC, comment_count DESC";

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql,
                    startDate, endDate, startDate, endDate, startDate, endDate, endDate);

            ExcelWriter writer = ExcelUtil.getWriter(true);
            writer.addHeaderAlias("id", "用户ID");
            writer.addHeaderAlias("account", "账号");
            writer.addHeaderAlias("nickname", "昵称");
            writer.addHeaderAlias("post_count", "发帖数");
            writer.addHeaderAlias("comment_count", "评论数");
            writer.addHeaderAlias("like_count", "点赞数");
            writer.addHeaderAlias("last_login_time", "最后登录时间");
            writer.addHeaderAlias("credit_score", "信誉分");

            writer.write(rows, true);
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
            String fileName = URLEncoder.encode("用户活跃度统计", "UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");
            ServletOutputStream out = response.getOutputStream();
            writer.flush(out, true);
            writer.close();
            IoUtil.close(out);
        } catch (Exception e) {
            throw new RuntimeException("导出失败: " + e.getMessage());
        }
    }

    @Override
    public void exportHotwordData(String startDate, String endDate, HttpServletResponse response) {
        try {
            if (StrUtil.isBlank(startDate))
                startDate = "2020-01-01 00:00:00";
            if (StrUtil.isBlank(endDate))
                endDate = DateUtil.now();

            String sql = "SELECT h.id, h.word, h.heat, h.status, h.create_time, " +
                    "(SELECT COUNT(*) FROM hotword_vote WHERE hotword_id = h.id AND create_time BETWEEN ? AND ?) as vote_count, "
                    +
                    "(SELECT COUNT(DISTINCT user_id) FROM hotword_vote WHERE hotword_id = h.id AND create_time BETWEEN ? AND ?) as voter_count "
                    +
                    "FROM hotword h " +
                    "WHERE h.create_time <= ? " +
                    "ORDER BY h.heat DESC";

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql,
                    startDate, endDate, startDate, endDate, endDate);

            ExcelWriter writer = ExcelUtil.getWriter(true);
            writer.addHeaderAlias("id", "热词ID");
            writer.addHeaderAlias("word", "热词内容");
            writer.addHeaderAlias("heat", "热度值");
            writer.addHeaderAlias("status", "状态");
            writer.addHeaderAlias("create_time", "创建时间");
            writer.addHeaderAlias("vote_count", "投票次数");
            writer.addHeaderAlias("voter_count", "投票人数");

            writer.write(rows, true);
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
            String fileName = URLEncoder.encode("热词投票统计", "UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");
            ServletOutputStream out = response.getOutputStream();
            writer.flush(out, true);
            writer.close();
            IoUtil.close(out);
        } catch (Exception e) {
            throw new RuntimeException("导出失败: " + e.getMessage());
        }
    }

    @Override
    public void exportBottleData(String startDate, String endDate, HttpServletResponse response) {
        try {
            if (StrUtil.isBlank(startDate))
                startDate = "2020-01-01 00:00:00";
            if (StrUtil.isBlank(endDate))
                endDate = DateUtil.now();

            String sql = "SELECT b.id, u.nickname as creator_nickname, b.content, b.type, b.direction, b.status, " +
                    "b.create_time, b.picked_time, " +
                    "(SELECT nickname FROM sys_user WHERE id = b.picked_by) as picker_nickname, " +
                    "(SELECT COUNT(*) FROM bottle_collection WHERE bottle_id = b.id) as collection_count " +
                    "FROM drift_bottle b " +
                    "LEFT JOIN sys_user u ON b.user_id = u.id " +
                    "WHERE b.create_time BETWEEN ? AND ? " +
                    "ORDER BY b.create_time DESC";

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, startDate, endDate);

            ExcelWriter writer = ExcelUtil.getWriter(true);
            writer.addHeaderAlias("id", "漂流瓶ID");
            writer.addHeaderAlias("creator_nickname", "投放者");
            writer.addHeaderAlias("content", "内容");
            writer.addHeaderAlias("type", "类型");
            writer.addHeaderAlias("direction", "方向");
            writer.addHeaderAlias("status", "状态");
            writer.addHeaderAlias("create_time", "投放时间");
            writer.addHeaderAlias("picked_time", "打捞时间");
            writer.addHeaderAlias("picker_nickname", "打捞者");
            writer.addHeaderAlias("collection_count", "珍藏次数");

            writer.write(rows, true);
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
            String fileName = URLEncoder.encode("漂流瓶统计", "UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");
            ServletOutputStream out = response.getOutputStream();
            writer.flush(out, true);
            writer.close();
            IoUtil.close(out);
        } catch (Exception e) {
            throw new RuntimeException("导出失败: " + e.getMessage());
        }
    }

    @Override
    public void exportStoryData(String startDate, String endDate, HttpServletResponse response) {
        try {
            if (StrUtil.isBlank(startDate))
                startDate = "2020-01-01 00:00:00";
            if (StrUtil.isBlank(endDate))
                endDate = DateUtil.now();

            String sql = "SELECT s.id, s.title, s.category, u.nickname as creator_nickname, s.status, " +
                    "s.create_time, s.finish_time, " +
                    "(SELECT COUNT(*) FROM story_paragraph WHERE story_id = s.id) as paragraph_count, " +
                    "(SELECT COUNT(DISTINCT user_id) FROM story_paragraph WHERE story_id = s.id) as participant_count, "
                    +
                    "(SELECT SUM(like_count) FROM story_paragraph WHERE story_id = s.id) as total_likes " +
                    "FROM story s " +
                    "LEFT JOIN sys_user u ON s.creator_id = u.id " +
                    "WHERE s.create_time BETWEEN ? AND ? " +
                    "ORDER BY s.create_time DESC";

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, startDate, endDate);

            ExcelWriter writer = ExcelUtil.getWriter(true);
            writer.addHeaderAlias("id", "故事ID");
            writer.addHeaderAlias("title", "故事标题");
            writer.addHeaderAlias("category", "分类");
            writer.addHeaderAlias("creator_nickname", "创建者");
            writer.addHeaderAlias("status", "状态");
            writer.addHeaderAlias("create_time", "创建时间");
            writer.addHeaderAlias("finish_time", "完结时间");
            writer.addHeaderAlias("paragraph_count", "段落数");
            writer.addHeaderAlias("participant_count", "参与人数");
            writer.addHeaderAlias("total_likes", "总点赞数");

            writer.write(rows, true);
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
            String fileName = URLEncoder.encode("故事接龙统计", "UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");
            ServletOutputStream out = response.getOutputStream();
            writer.flush(out, true);
            writer.close();
            IoUtil.close(out);
        } catch (Exception e) {
            throw new RuntimeException("导出失败: " + e.getMessage());
        }
    }

    @Override
    public void exportComprehensiveReport(String startDate, String endDate, HttpServletResponse response) {
        try {
            if (StrUtil.isBlank(startDate))
                startDate = "2020-01-01 00:00:00";
            if (StrUtil.isBlank(endDate))
                endDate = DateUtil.now();

            ExcelWriter writer = ExcelUtil.getWriter(true);

            // Sheet 1: 帖子数据
            String postSql = "SELECT p.id, u.nickname, p.content, p.category, p.create_time, p.view_count, p.like_count "
                    +
                    "FROM post p LEFT JOIN sys_user u ON p.user_id = u.id " +
                    "WHERE p.create_time BETWEEN ? AND ? ORDER BY p.create_time DESC";
            List<Map<String, Object>> posts = jdbcTemplate.queryForList(postSql, startDate, endDate);
            writer.renameSheet(0, "帖子数据");
            writer.addHeaderAlias("id", "ID");
            writer.addHeaderAlias("nickname", "用户");
            writer.addHeaderAlias("content", "内容");
            writer.addHeaderAlias("category", "分类");
            writer.addHeaderAlias("create_time", "时间");
            writer.addHeaderAlias("view_count", "浏览");
            writer.addHeaderAlias("like_count", "点赞");
            writer.write(posts, true);

            // Sheet 2: 新注册用户
            writer.setSheet("新注册用户");
            String userSql = "SELECT id, account, nickname, create_time, credit_score FROM sys_user " +
                    "WHERE create_time BETWEEN ? AND ? ORDER BY create_time DESC";
            List<Map<String, Object>> users = jdbcTemplate.queryForList(userSql, startDate, endDate);
            writer.clearHeaderAlias();
            writer.addHeaderAlias("id", "ID");
            writer.addHeaderAlias("account", "账号");
            writer.addHeaderAlias("nickname", "昵称");
            writer.addHeaderAlias("create_time", "注册时间");
            writer.addHeaderAlias("credit_score", "信誉分");
            writer.write(users, true);

            // Sheet 3: 热词数据
            writer.setSheet("热词数据");
            String hotwordSql = "SELECT h.id, h.word, h.heat, h.create_time, " +
                    "(SELECT COUNT(*) FROM hotword_vote WHERE hotword_id = h.id AND create_time BETWEEN ? AND ?) as votes "
                    +
                    "FROM hotword h WHERE h.create_time <= ? ORDER BY h.heat DESC";
            List<Map<String, Object>> hotwords = jdbcTemplate.queryForList(hotwordSql, startDate, endDate, endDate);
            writer.clearHeaderAlias();
            writer.addHeaderAlias("id", "ID");
            writer.addHeaderAlias("word", "热词");
            writer.addHeaderAlias("heat", "热度");
            writer.addHeaderAlias("create_time", "创建时间");
            writer.addHeaderAlias("votes", "投票数");
            writer.write(hotwords, true);

            // Sheet 4: 漂流瓶数据
            writer.setSheet("漂流瓶数据");
            String bottleSql = "SELECT b.id, u.nickname as creator, b.content, b.status, b.create_time " +
                    "FROM drift_bottle b LEFT JOIN sys_user u ON b.user_id = u.id " +
                    "WHERE b.create_time BETWEEN ? AND ? ORDER BY b.create_time DESC";
            List<Map<String, Object>> bottles = jdbcTemplate.queryForList(bottleSql, startDate, endDate);
            writer.clearHeaderAlias();
            writer.addHeaderAlias("id", "ID");
            writer.addHeaderAlias("creator", "投放者");
            writer.addHeaderAlias("content", "内容");
            writer.addHeaderAlias("status", "状态");
            writer.addHeaderAlias("create_time", "投放时间");
            writer.write(bottles, true);

            // Sheet 5: 故事接龙数据
            writer.setSheet("故事接龙数据");
            String storySql = "SELECT s.id, s.title, u.nickname as creator, s.status, s.create_time, " +
                    "(SELECT COUNT(*) FROM story_paragraph WHERE story_id = s.id) as paragraphs " +
                    "FROM story s LEFT JOIN sys_user u ON s.creator_id = u.id " +
                    "WHERE s.create_time BETWEEN ? AND ? ORDER BY s.create_time DESC";
            List<Map<String, Object>> stories = jdbcTemplate.queryForList(storySql, startDate, endDate);
            writer.clearHeaderAlias();
            writer.addHeaderAlias("id", "ID");
            writer.addHeaderAlias("title", "标题");
            writer.addHeaderAlias("creator", "创建者");
            writer.addHeaderAlias("status", "状态");
            writer.addHeaderAlias("create_time", "创建时间");
            writer.addHeaderAlias("paragraphs", "段落数");
            writer.write(stories, true);

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
            String fileName = URLEncoder.encode("综合运营报表", "UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");
            ServletOutputStream out = response.getOutputStream();
            writer.flush(out, true);
            writer.close();
            IoUtil.close(out);
        } catch (Exception e) {
            throw new RuntimeException("导出失败: " + e.getMessage());
        }
    }

    // --- 6. 公告管理 ---
    @Override
    public List<Announcement> getAnnouncementList() {
        return announcementMapper.selectAll();
    }

    @Override
    public void addAnnouncement(Announcement announcement) {
        if (announcement.getIsTop() == null) {
            announcement.setIsTop(0);
        } else if (announcement.getIsTop() == 1) {
            Integer topCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_announcement WHERE is_top = 1", Integer.class);
            if (topCount != null && topCount >= 3) {
                throw new com.example.common.BizException("最多只能置顶3条公告");
            }
        }
        announcementMapper.insert(announcement);

        // 通过 WebSocket 广播新公告通知给所有在线用户
        try {
            String message = String.format(
                    "{\"type\":\"ANNOUNCEMENT\",\"title\":\"%s\",\"content\":\"%s\"}",
                    announcement.getTitle(),
                    announcement.getContent().length() > 50
                            ? announcement.getContent().substring(0, 50) + "..."
                            : announcement.getContent());
            WebSocketServer.broadcast(message);
        } catch (Exception e) {
            // 忽略 WebSocket 广播失败
        }
    }

    @Override
    public void deleteAnnouncement(Long id) {
        announcementMapper.delete(id);
    }

    @Override
    public void toggleAnnouncementTop(Long id) {
        Integer isTop = jdbcTemplate.queryForObject("SELECT is_top FROM sys_announcement WHERE id = ?", Integer.class, id);
        if (isTop != null && isTop == 0) {
            // 即将变为置顶，校验数量
            Integer topCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_announcement WHERE is_top = 1", Integer.class);
            if (topCount != null && topCount >= 3) {
                throw new com.example.common.BizException("最多只能置顶3条公告");
            }
        }
        jdbcTemplate.update("UPDATE sys_announcement SET is_top = 1 - is_top WHERE id = ?", id);
    }

    // --- 7. 分类管理 (确保存在！) ---
    @Override
    public List<Category> getCategoryList() {
        return categoryMapper.selectAll();
    }

    @Override
    public void addCategory(Category category) {
        categoryMapper.insert(category);
    }

    @Override
    public void updateCategory(Category category) {
        // 保护基础8个板块：不可修改
        if (category.getId() != null && java.util.Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 9L).contains(category.getId())) {
            throw new RuntimeException("系统基础板块不可修改！");
        }
        categoryMapper.update(category);
    }

    @Override
    public void deleteCategory(Long id) {
        // 保护基础8个板块：不可删除
        if (id != null && java.util.Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 9L).contains(id)) {
            throw new RuntimeException("系统基础板块不可删除！");
        }
        Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM post WHERE category = ?", Integer.class, id);
        if (count != null && count > 0) {
            throw new RuntimeException("该分类下有 " + count + " 条帖子，无法删除！请先转移或删除帖子。");
        }
        categoryMapper.delete(id);
    }

    // --- 8. 举报处理 (本次合并) ---
    @Override
    public PageInfo<Map<String, Object>> getReportList(Integer pageNum, Integer pageSize, Integer status) {
        PageHelper.startPage(pageNum, pageSize);
        List<Map<String, Object>> list = reportMapper.selectReportList(status);
        return new PageInfo<>(list);
    }

    @Override
    public void handleReport(Long reportId, Integer action) {
        Report report = reportMapper.selectById(reportId);
        if (report == null)
            return;

        if (action == 1) {
            try {
                deletePost(report.getPostId(), "被举报核实： " + report.getReason());
            } catch (Exception e) {
            }

            reportMapper.updateStatus(reportId, 1);
            sendSysNotice(report.getUserId(), "您对帖子 #" + report.getPostId() + " 的举报已核实处理，感谢您维护社区环境！信誉分 +2");

            recordAuditLog("APPROVE_REPORT", "REPORT", reportId, "举报核实：" + report.getReason());

        } else if (action == 2) {
            reportMapper.updateStatus(reportId, 2);
            sendSysNotice(report.getUserId(), "您对帖子 #" + report.getPostId() + " 的举报经核查不成立，已驳回。");

            recordAuditLog("REJECT_REPORT", "REPORT", reportId, "举报驳回");
        }
    }

    // 通用通知方法
    private void sendSysNotice(Long userId, String content) {
        SysNotice notice = new SysNotice();
        notice.setReceiverId(userId);
        notice.setSenderId(0L);
        notice.setType(1);
        notice.setContent(content);
        noticeMapper.insert(notice);
        WebSocketServer.sendNotification(userId, content, 1);
    }

    // --- 9. 轮播图管理 ---
    @Override
    public List<Banner> getBannerList() {
        return jdbcTemplate.query("SELECT * FROM sys_banner ORDER BY sort ASC",
                new BeanPropertyRowMapper<>(Banner.class));
    }

    @Override
    public void addBanner(Banner banner) {
        jdbcTemplate.update("INSERT INTO sys_banner (image_url, title, sort) VALUES (?, ?, ?)",
                banner.getImageUrl(), banner.getTitle(), banner.getSort());
    }

    @Override
    public void updateBanner(Banner banner) {
        jdbcTemplate.update("UPDATE sys_banner SET image_url = ?, title = ?, sort = ? WHERE id = ?",
                banner.getImageUrl(), banner.getTitle(), banner.getSort(), banner.getId());
    }

    @Override
    public void deleteBanner(Long id) {
        jdbcTemplate.update("DELETE FROM sys_banner WHERE id = ?", id);
    }
}