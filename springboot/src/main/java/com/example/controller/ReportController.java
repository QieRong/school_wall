package com.example.controller;

import com.example.common.Result;
import com.example.entity.Report;
import com.example.mapper.ReportMapper;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

@RestController
@RequestMapping("/report")
public class ReportController {

    @Resource
    private ReportMapper reportMapper;

    @PostMapping("/create")
    public Result<?> create(@RequestBody Report report) {
        if (report.getPostId() == null || report.getReason() == null) {
            return Result.error("参数不完整");
        }
        reportMapper.insert(report);
        return Result.success(null);
    }
}