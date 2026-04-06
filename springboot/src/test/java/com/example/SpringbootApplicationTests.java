package com.example;

import com.example.base.BaseTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 应用启动测试
 * 验证测试环境配置是否正确
 */
class SpringbootApplicationTests extends BaseTest {

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  void contextLoads() {
    // 验证 Spring 上下文加载成功
    assertNotNull(jdbcTemplate);
  }

  @Test
  void testDatabaseConnection() {
    // 验证 H2 数据库连接正常
    Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_user", Integer.class);
    assertNotNull(count);
    assertTrue(count >= 0, "应该能查询到用户数据");
  }

  @Test
  void testInitialDataLoaded() {
    // 验证初始测试数据已加载
    Integer userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_user", Integer.class);
    Integer categoryCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_category", Integer.class);

    assertNotNull(userCount, "用户数量不应为null");
    assertNotNull(categoryCount, "分类数量不应为null");
    assertTrue(userCount > 0, "应该有测试用户数据");
    assertTrue(categoryCount > 0, "应该有分类数据");
  }
}
