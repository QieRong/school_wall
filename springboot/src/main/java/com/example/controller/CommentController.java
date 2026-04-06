// File: springboot/src/main/java/com/example/controller/CommentController.java
package com.example.controller;

import com.example.common.Result;
import com.example.entity.Comment;
import com.example.entity.SysNotice;
import com.example.entity.User;
import com.example.mapper.CommentMapper;
import com.example.mapper.NoticeMapper;
import com.example.mapper.PostMapper;
import com.example.mapper.UserMapper;
import com.example.server.WebSocketServer;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/comment")
public class CommentController {

    @Resource
    private CommentMapper commentMapper;
    @Resource
    private PostMapper postMapper;
    @Resource
    private NoticeMapper noticeMapper;
    @Resource
    private UserMapper userMapper;

    // 【优化】统一的管理员检查方法
    private boolean isAdmin(Long userId) {
        if (userId == null)
            return false;
        User user = userMapper.selectById(userId);
        return user != null && user.getRole() != null && user.getRole() == com.example.common.Constants.ROLE_ADMIN;
    }

    @PostMapping("/create")
    @Transactional(rollbackFor = Exception.class) // 【修复】添加事务，保证评论和计数一致性
    public Result<?> create(@RequestBody Comment comment) {
        // 1. 权限检查（统一使用isAdmin方法）
        if (isAdmin(comment.getUserId())) {
            return Result.error("管理员禁止发布评论");
        }

        // 允许纯图片评论：内容和图片至少有一个
        boolean hasContent = comment.getContent() != null && !comment.getContent().trim().isEmpty();
        boolean hasImage = comment.getImgUrl() != null && !comment.getImgUrl().trim().isEmpty();
        if (!hasContent && !hasImage) {
            return Result.error("请输入内容或添加图片");
        }

        // 【安全】XSS 防护
        if (hasContent) {
            comment.setContent(com.example.utils.XssUtils.stripHtml(comment.getContent()));
        }

        // 2. 插入评论
        commentMapper.insert(comment);

        // 【核心修复】发布评论后，立即更新 post 表的 comment_count
        postMapper.incrementComment(comment.getPostId());

        // 3. 发送通知
        Long receiverId = comment.getReplyUserId();
        if (receiverId != null && !receiverId.equals(comment.getUserId())) {
            SysNotice notice = new SysNotice();
            notice.setReceiverId(receiverId);
            notice.setSenderId(comment.getUserId());
            notice.setType(2); // 2=评论
            notice.setRelatedId(comment.getPostId());
            notice.setContent(comment.getContent());
            noticeMapper.insert(notice);
            WebSocketServer.sendNotification(receiverId, "收到一条新评论: " + comment.getContent(), 2);
        }

        return Result.success(null);
    }

    @GetMapping("/list")
    public Result<List<Comment>> list(@RequestParam Long postId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "50") Integer pageSize) {
        // 【优化】添加分页，默认每页50条，避免一次性加载过多评论
        com.github.pagehelper.PageHelper.startPage(pageNum, pageSize);
        List<Comment> comments = commentMapper.selectByPostId(postId);
        return Result.success(comments);
    }

    /**
     * 【优化】获取树形结构的评论列表
     * 后端构建树形结构，减轻前端负担
     */
    @GetMapping("/tree")
    public Result<List<Comment>> tree(@RequestParam Long postId) {
        List<Comment> allComments = commentMapper.selectByPostId(postId);
        return Result.success(buildCommentTree(allComments));
    }

    /**
     * 构建评论树形结构
     */
    private List<Comment> buildCommentTree(List<Comment> allComments) {
        List<Comment> roots = new ArrayList<>();
        Map<Long, Comment> commentMap = new HashMap<>();

        // 第一遍：建立ID映射
        for (Comment c : allComments) {
            c.setChildren(new ArrayList<>());
            commentMap.put(c.getId(), c);
        }

        // 第二遍：构建树
        for (Comment c : allComments) {
            Long parentId = c.getParentId();
            if (parentId == null || parentId == 0) {
                // 一级评论
                roots.add(c);
            } else {
                // 二级评论，挂到父评论下
                Comment parent = commentMap.get(parentId);
                if (parent != null) {
                    parent.getChildren().add(c);
                } else {
                    // 父评论不存在，作为一级评论处理
                    roots.add(c);
                }
            }
        }

        return roots;
    }
}