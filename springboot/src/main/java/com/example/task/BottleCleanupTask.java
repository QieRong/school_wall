// File: springboot/src/main/java/com/example/task/BottleCleanupTask.java
package com.example.task;

import com.example.service.BottleService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 漂流瓶清理定时任务
 * 每日凌晨2点执行，将过期的漂流瓶状态更新为"已沉没"
 */
@Component
public class BottleCleanupTask {

  private static final Logger log = LoggerFactory.getLogger(BottleCleanupTask.class);

  @Resource
  private BottleService bottleService;

  /**
   * 每日凌晨2点执行清理任务
   */
  @Scheduled(cron = "0 0 2 * * ?")
  public void cleanupExpiredBottles() {
    long startTime = System.currentTimeMillis();
    log.info("========== 漂流瓶清理任务开始 ==========");
    log.info("执行时间: {}", java.time.LocalDateTime.now());

    try {
      int count = bottleService.cleanupExpiredBottles();
      long duration = System.currentTimeMillis() - startTime;

      if (count > 0) {
        log.info("✓ 清理任务成功完成");
        log.info("  - 清理数量: {} 个过期瓶子", count);
        log.info("  - 执行耗时: {} ms", duration);
      } else {
        log.info("✓ 清理任务完成，暂无过期瓶子需要清理");
        log.info("  - 执行耗时: {} ms", duration);
      }
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      log.error("✗ 漂流瓶清理任务执行失败");
      log.error("  - 失败原因: {}", e.getMessage());
      log.error("  - 执行耗时: {} ms", duration);
      log.error("  - 异常堆栈:", e);
    } finally {
      log.info("========== 漂流瓶清理任务结束 ==========\n");
    }
  }
}
