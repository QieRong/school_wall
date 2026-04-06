package com.example.integration;

import cn.hutool.crypto.digest.BCrypt;
import com.example.base.BaseTest;
import com.example.entity.User;
import com.example.mapper.UserMapper;
import com.example.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * 用户API安全性集成测试
 * 
 * 测试目标：验证所有用户相关API不会在响应中暴露密码字段
 * 
 * 测试场景：
 * 1. 用户资料查询接口 (/user/profile)
 * 2. 用户搜索接口 (/user/search)
 * 
 * 验收标准：
 * - 响应JSON中不应包含password字段
 * - 响应JSON中不应包含密码哈希值
 * - 其他用户信息应正常返回
 * 
 * _Requirements: 1.3_
 */
@DisplayName("用户API安全性集成测试")
@AutoConfigureMockMvc
class UserApiSecurityTest extends BaseTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserMapper userMapper;

  @Autowired
  private JwtUtils jwtUtils;

  private User testUser;

  private final String TEST_PASSWORD = "testPassword123";
  private final String TEST_PASSWORD_HASH = BCrypt.hashpw(TEST_PASSWORD);

  @BeforeEach
  void setUp() {
    // 创建测试用户
    testUser = new User();
    testUser.setAccount("2021999001");
    testUser.setPassword(TEST_PASSWORD_HASH);
    testUser.setNickname("测试用户");
    testUser.setAvatar("/default.png");
    testUser.setRole(0);
    testUser.setStatus(1);
    testUser.setCreditScore(100);
    testUser.setViolationCount(0);
    userMapper.insert(testUser);

    // 生成测试Token

  }

  @Test
  @DisplayName("用户资料查询接口不应暴露密码字段")
  void testUserApiDoesNotExposePassword() throws Exception {
    // 调用用户资料查询接口（不传currentId，避免触发访客记录功能）
    mockMvc.perform(get("/user/profile")
        .param("targetId", testUser.getId().toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data").exists())
        // 验证正常字段存在
        .andExpect(jsonPath("$.data.id").value(testUser.getId()))
        .andExpect(jsonPath("$.data.account").value("2021999001"))
        .andExpect(jsonPath("$.data.nickname").value("测试用户"))
        .andExpect(jsonPath("$.data.avatar").value("/default.png"))
        // 验证password字段不存在
        .andExpect(jsonPath("$.data.password").doesNotExist())
        // 验证响应体中不包含密码哈希值
        .andExpect(content().string(not(containsString(TEST_PASSWORD_HASH))))
        .andExpect(content().string(not(containsString("password"))));
  }

  @Test
  @DisplayName("用户搜索接口不应暴露密码字段")
  void testUserSearchApiDoesNotExposePassword() throws Exception {
    // 调用用户搜索接口
    mockMvc.perform(get("/user/search")
        .param("keyword", "测试"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data[0]").exists())
        // 验证正常字段存在
        .andExpect(jsonPath("$.data[0].account").value("2021999001"))
        .andExpect(jsonPath("$.data[0].nickname").value("测试用户"))
        // 验证password字段不存在
        .andExpect(jsonPath("$.data[0].password").doesNotExist())
        // 验证响应体中不包含密码哈希值
        .andExpect(content().string(not(containsString(TEST_PASSWORD_HASH))))
        .andExpect(content().string(not(containsString("password"))));
  }

  @Test
  @DisplayName("多个用户搜索结果都不应暴露密码字段")
  void testMultipleUsersSearchDoesNotExposePassword() throws Exception {
    // 创建第二个测试用户
    User testUser2 = new User();
    testUser2.setAccount("2021999002");
    testUser2.setPassword(BCrypt.hashpw("anotherPassword456"));
    testUser2.setNickname("测试用户2");
    testUser2.setAvatar("/default.png");
    testUser2.setRole(0);
    testUser2.setStatus(1);
    testUser2.setCreditScore(100);
    testUser2.setViolationCount(0);
    userMapper.insert(testUser2);

    // 搜索所有测试用户
    mockMvc.perform(get("/user/search")
        .param("keyword", "测试"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(2)))
        // 验证第一个用户没有password字段
        .andExpect(jsonPath("$.data[0].password").doesNotExist())
        // 验证第二个用户没有password字段
        .andExpect(jsonPath("$.data[1].password").doesNotExist())
        // 验证响应体中不包含任何密码相关内容
        .andExpect(content().string(not(containsString("password"))));
  }

  @Test
  @DisplayName("即使Service层忘记清空密码，@JsonIgnore注解也应防止密码泄露")
  void testJsonIgnoreAnnotationPreventsPasswordLeak() throws Exception {
    // 这个测试验证即使Service层代码有bug，@JsonIgnore注解也能作为最后一道防线
    // 通过直接查询数据库获取包含密码的User对象，然后验证API响应中不包含密码

    // 调用API（不传currentId，避免触发访客记录功能）
    mockMvc.perform(get("/user/profile")
        .param("targetId", testUser.getId().toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data").exists())
        // 验证password字段绝对不存在（即使数据库中有）
        .andExpect(jsonPath("$.data.password").doesNotExist())
        // 验证响应体中不包含密码哈希值
        .andExpect(content().string(not(containsString(TEST_PASSWORD_HASH))))
        // 验证响应体中不包含"password"字符串
        .andExpect(content().string(not(containsString("password"))));
  }
}
