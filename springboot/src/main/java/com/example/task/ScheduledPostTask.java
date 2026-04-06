package com.example.task;

import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 定时发布任务
 * 扫描 scheduled_time <= 当前时间 且 status = 2(待发布) 的帖子，将其状态改为 1(已发布)
 * 
 * ⚠️ 此任务已禁用，由 PostScheduleTask 统一处理，避免重复执行
 */
// @Component // 【禁用】与 PostScheduleTask 功能重复
public class ScheduledPostTask {

  @Resource
  private JdbcTemplate jdbcTemplate;

  // 每分钟执行一次
  @Scheduled(cron = "0 * * * * ?")
  public void publishScheduledPosts() {
    LocalDateTime now = LocalDateTime.now();
    String nowStr = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    // 查询需要发布的定时帖子 (scheduled_time <= now 且 status = 2)
    // status: 0=待审核, 1=已发布, 2=定时待发布
    // 【修复】将 create_time 更新为 scheduled_time，让帖子显示定时发布时间而非提交时间
    int count = jdbcTemplate.update(
        "UPDATE post SET status = 1, create_time = scheduled_time, scheduled_time = NULL WHERE scheduled_time IS NOT NULL AND scheduled_time <= ? AND status = 2",
        nowStr);

    if (count > 0) {
      System.out.println(">>> [定时任务] " + nowStr + " 发布了 " + count + " 条定时帖子");
    }
  }
}
