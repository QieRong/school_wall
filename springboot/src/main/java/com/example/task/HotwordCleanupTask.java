package com.example.task;

import com.example.service.HotwordService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class HotwordCleanupTask {

  private static final Logger log = LoggerFactory.getLogger(HotwordCleanupTask.class);

  @Resource
  private HotwordService hotwordService;

  /**
   * 每日凌晨1点执行归档检查
   * 将30天无投票的热词归档
   */
  @Scheduled(cron = "0 0 1 * * ?")
  public void archiveExpiredHotwords() {
    long startTime = System.currentTimeMillis();
    log.info("========== 热词归档任务开始 ==========");
    log.info("执行时间: {}", LocalDateTime.now());

    try {
      int count = hotwordService.archiveExpiredHotwords();
      long duration = System.currentTimeMillis() - startTime;

      if (count > 0) {
        log.info("✓ 归档任务成功完成");
        log.info("  - 归档数量: {} 个热词", count);
        log.info("  - 执行耗时: {} ms", duration);
      } else {
        log.info("✓ 归档任务完成，暂无需归档的热词");
        log.info("  - 执行耗时: {} ms", duration);
      }
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      log.error("✗ 热词归档任务执行失败");
      log.error("  - 失败原因: {}", e.getMessage());
      log.error("  - 执行耗时: {} ms", duration);
      log.error("  - 异常堆栈:", e);
    } finally {
      log.info("========== 热词归档任务结束 ==========\n");
    }
  }

  /**
   * 每日凌晨0点重置投票配额
   * 清理过期的配额记录
   */
  @Scheduled(cron = "0 0 0 * * ?")
  public void resetDailyQuota() {
    long startTime = System.currentTimeMillis();
    log.info("========== 投票配额重置任务开始 ==========");
    log.info("执行时间: {}", LocalDateTime.now());

    try {
      hotwordService.resetDailyQuota();
      long duration = System.currentTimeMillis() - startTime;

      log.info("✓ 配额重置任务成功完成");
      log.info("  - 清理了7天前的历史配额记录");
      log.info("  - 执行耗时: {} ms", duration);
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      log.error("✗ 配额重置任务执行失败");
      log.error("  - 失败原因: {}", e.getMessage());
      log.error("  - 执行耗时: {} ms", duration);
      log.error("  - 异常堆栈:", e);
    } finally {
      log.info("========== 投票配额重置任务结束 ==========\n");
    }
  }
}
