package com.example.service;

import cn.hutool.crypto.digest.BCrypt;
import com.example.base.BaseTest;
import com.example.entity.User;
import com.example.mapper.UserMapper;
import com.example.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户认证服务单元测试
 * 
 * 测试范围：
 * - 正常登录流程
 * - 错误密码登录
 * - 不存在用户登录
 * - JWT Token 生成和验证
 * 
 * _Requirements: 1.1, 1.2, 1.3_
 */
@DisplayName("用户认证服务测试")
class UserServiceTest extends BaseTest {

  @Autowired
  private UserService userService;

  @Autowired
  private UserMapper userMapper;

  @Autowired
  private JwtUtils jwtUtils;

  private User testUser;
  private final String TEST_ACCOUNT = "2021999001";
  private final String TEST_PASSWORD = "test123456";

  @BeforeEach
  void setUp() {
    // 创建测试用户
    testUser = new User();
    testUser.setAccount(TEST_ACCOUNT);
    testUser.setPassword(BCrypt.hashpw(TEST_PASSWORD));
    testUser.setNickname("测试用户");
    testUser.setRole(0);
    testUser.setStatus(1);
    testUser.setCreditScore(100);
    testUser.setViolationCount(0);
    testUser.setAvatar("/default.png");
    userMapper.insert(testUser);
  }

  @Nested
  @DisplayName("登录功能测试")
  class LoginTests {

    @Test
    @DisplayName("正常登录 - 使用正确的账号密码应该成功登录")
    void login_withValidCredentials_shouldSucceed() {
      // When
      User result = userService.login(TEST_ACCOUNT, TEST_PASSWORD);

      // Then
      assertNotNull(result, "登录结果不应为空");
      assertEquals(TEST_ACCOUNT, result.getAccount(), "账号应该匹配");
      assertEquals("测试用户", result.getNickname(), "昵称应该匹配");
    }

    @Test
    @DisplayName("错误密码登录 - 使用错误密码应该抛出异常")
    void login_withWrongPassword_shouldThrowException() {
      // When & Then
      RuntimeException exception = assertThrows(
          RuntimeException.class,
          () -> userService.login(TEST_ACCOUNT, "wrongpassword"),
          "错误密码应该抛出异常");
      assertTrue(
          exception.getMessage().contains("密码错误") ||
              exception.getMessage().contains("账号或密码错误"),
          "异常消息应该包含密码错误提示");
    }

    @Test
    @DisplayName("不存在用户登录 - 使用不存在的账号应该抛出异常")
    void login_withNonExistentAccount_shouldThrowException() {
      // When & Then
      RuntimeException exception = assertThrows(
          RuntimeException.class,
          () -> userService.login("9999999999", TEST_PASSWORD),
          "不存在的账号应该抛出异常");
      assertTrue(
          exception.getMessage().contains("不存在"),
          "异常消息应该包含账号不存在提示");
    }

    // 注意：封禁用户测试需要在集成测试中进行，因为涉及复杂的数据库状态更新
    // 这里仅测试核心登录逻辑

    @Test
    @DisplayName("空密码登录 - 空密码应该抛出异常")
    void login_withEmptyPassword_shouldThrowException() {
      // When & Then
      assertThrows(
          Exception.class,
          () -> userService.login(TEST_ACCOUNT, ""),
          "空密码应该抛出异常");
    }
  }

  @Nested
  @DisplayName("注册功能测试")
  class RegisterTests {

    @Test
    @DisplayName("正常注册 - 使用有效信息应该成功注册")
    void register_withValidInfo_shouldSucceed() {
      // Given
      User newUser = new User();
      newUser.setAccount("2021999002");
      newUser.setPassword("newpass123");

      // When
      userService.register(newUser);

      // Then
      User savedUser = userMapper.selectByAccount("2021999002");
      assertNotNull(savedUser, "用户应该被保存");
      assertNotEquals("newpass123", savedUser.getPassword(), "密码应该被加密");
      assertTrue(BCrypt.checkpw("newpass123", savedUser.getPassword()), "加密后的密码应该可以验证");
    }

    @Test
    @DisplayName("重复账号注册 - 使用已存在的账号应该抛出异常")
    void register_withExistingAccount_shouldThrowException() {
      // Given
      User duplicateUser = new User();
      duplicateUser.setAccount(TEST_ACCOUNT);
      duplicateUser.setPassword("somepassword");

      // When & Then
      RuntimeException exception = assertThrows(
          RuntimeException.class,
          () -> userService.register(duplicateUser),
          "重复账号应该抛出异常");
      assertTrue(
          exception.getMessage().contains("已存在"),
          "异常消息应该包含已存在提示");
    }

    @Test
    @DisplayName("无效账号格式注册 - 非10位学号应该抛出异常")
    void register_withInvalidAccountFormat_shouldThrowException() {
      // Given
      User invalidUser = new User();
      invalidUser.setAccount("12345"); // 不是10位
      invalidUser.setPassword("validpass123");

      // When & Then
      RuntimeException exception = assertThrows(
          RuntimeException.class,
          () -> userService.register(invalidUser),
          "无效账号格式应该抛出异常");
      assertTrue(
          exception.getMessage().contains("格式错误"),
          "异常消息应该包含格式错误提示");
    }

    @Test
    @DisplayName("短密码注册 - 密码少于6位应该抛出异常")
    void register_withShortPassword_shouldThrowException() {
      // Given
      User shortPwdUser = new User();
      shortPwdUser.setAccount("2021999003");
      shortPwdUser.setPassword("12345"); // 少于6位

      // When & Then
      RuntimeException exception = assertThrows(
          RuntimeException.class,
          () -> userService.register(shortPwdUser),
          "短密码应该抛出异常");
      assertTrue(
          exception.getMessage().contains("6位"),
          "异常消息应该包含密码长度提示");
    }
  }

  @Nested
  @DisplayName("JWT Token 测试")
  class JwtTokenTests {

    @Test
    @DisplayName("Token 生成 - 应该成功生成有效的 Token")
    void createToken_shouldGenerateValidToken() {
      // When
      String token = jwtUtils.createToken(1L, TEST_ACCOUNT);

      // Then
      assertNotNull(token, "Token 不应为空");
      assertTrue(token.length() > 0, "Token 应该有内容");
      assertTrue(token.contains("."), "Token 应该是 JWT 格式");
    }

    @Test
    @DisplayName("Token 验证 - 有效 Token 应该返回正确的用户ID")
    void verifyToken_withValidToken_shouldReturnUserId() {
      // Given
      Long userId = 123L;
      String token = jwtUtils.createToken(userId, TEST_ACCOUNT);

      // When
      Long result = jwtUtils.verifyToken(token);

      // Then
      assertEquals(userId, result, "验证结果应该返回正确的用户ID");
    }

    @Test
    @DisplayName("Token 验证 - 无效 Token 应该返回 null")
    void verifyToken_withInvalidToken_shouldReturnNull() {
      // When
      Long result = jwtUtils.verifyToken("invalid.token.here");

      // Then
      assertNull(result, "无效 Token 应该返回 null");
    }

    @Test
    @DisplayName("Token 验证 - 篡改的 Token 应该返回 null")
    void verifyToken_withTamperedToken_shouldReturnNull() {
      // Given
      String validToken = jwtUtils.createToken(1L, TEST_ACCOUNT);
      String tamperedToken = validToken.substring(0, validToken.length() - 5) + "xxxxx";

      // When
      Long result = jwtUtils.verifyToken(tamperedToken);

      // Then
      assertNull(result, "篡改的 Token 应该返回 null");
    }

    @Test
    @DisplayName("Token 验证 - 空 Token 应该返回 null")
    void verifyToken_withEmptyToken_shouldReturnNull() {
      // When
      Long result = jwtUtils.verifyToken("");

      // Then
      assertNull(result, "空 Token 应该返回 null");
    }

    @Test
    @DisplayName("Token 验证 - null Token 应该返回 null")
    void verifyToken_withNullToken_shouldReturnNull() {
      // When
      Long result = jwtUtils.verifyToken(null);

      // Then
      assertNull(result, "null Token 应该返回 null");
    }
  }
}
