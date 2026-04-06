package com.example.task;

import com.example.mapper.PostMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * 帖子定时发布任务
 * 每分钟检查一次是否有到期的定时帖子需要发布
 */
@Component
public class PostScheduleTask {

  private static final Logger log = LoggerFactory.getLogger(PostScheduleTask.class);

  @Resource
  private PostMapper postMapper;

  /**
   * 每分钟执行一次，发布到期的定时帖子
   * 将 status=2 且 scheduled_time <= NOW() 的帖子改为 status=1
   */
  @Scheduled(cron = "0 * * * * ?")
  public void publishScheduledPosts() {
    try {
      int count = postMapper.publishScheduledPosts();
      if (count > 0) {
        log.info("定时发布任务执行完成，发布了 {} 条帖子", count);
      }
    } catch (Exception e) {
      log.error("定时发布任务执行失败", e);
    }
  }
}
