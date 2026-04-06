// File: springboot/src/main/java/com/example/controller/NoticeController.java
package com.example.controller;

import com.example.common.Result;
import com.example.entity.SysNotice;
import com.example.mapper.NoticeMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/notice")
public class NoticeController {

    @Resource
    private NoticeMapper noticeMapper;

    /**
     * 获取通知列表
     * 
     * @param type 0:全部 1:系统 2:评论 3:赞
     */
    @GetMapping("/list")
    public Result<PageInfo<SysNotice>> list(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") Integer type) {
        PageHelper.startPage(pageNum, pageSize);
        List<SysNotice> list = noticeMapper.selectByUserId(userId, type);
        return Result.success(new PageInfo<>(list));
    }

    /**
     * 获取未读数量 (用于红点)
     */
    @GetMapping("/unread")
    public Result<Integer> unread(@RequestParam Long userId) {
        return Result.success(noticeMapper.countUnread(userId));
    }

    /**
     * 一键已读
     */
    @PostMapping("/readAll")
    public Result<?> readAll(@RequestParam Long userId) {
        noticeMapper.readAll(userId);
        // 传入 null
        return Result.success(null);
    }

    /**
     * 删除已读
     */
    @PostMapping("/clear")
    public Result<?> clear(@RequestParam Long userId) {
        noticeMapper.deleteRead(userId);
        // 传入 null
        return Result.success(null);
    }

    /**
     * 单个已读
     */
    @PostMapping("/read")
    public Result<?> read(@RequestParam Integer id) {
        noticeMapper.read(id);
        return Result.success(null);
    }
}