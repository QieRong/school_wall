package com.example.task;

import com.example.server.WebSocketServer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * WebSocket连接池清理任务
 * 定期清理无效的Session，防止内存泄漏
 */
@Component
public class WebSocketCleanupTask {

  // 每5分钟执行一次清理
  @Scheduled(fixedRate = 5 * 60 * 1000)
  public void cleanupSessions() {
    WebSocketServer.cleanupInvalidSessions();
  }
}
