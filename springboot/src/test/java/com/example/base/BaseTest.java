package com.example.base;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * 测试基类
 * 所有需要 Spring 上下文的测试类都应该继承此类
 * 
 * 特性：
 * - 使用 test profile 加载测试配置
 * - 默认开启事务回滚，测试数据不会污染数据库
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class BaseTest {
  // 基类提供通用的测试配置
}
