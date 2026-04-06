package com.example.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * User实体类单元测试
 * 主要测试密码字段的序列化防护机制
 */
@DisplayName("User实体类测试")
class UserTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    // 配置ObjectMapper支持Java 8日期时间类型
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
  }

  @Test
  @DisplayName("密码字段不应被序列化到JSON")
  void testPasswordNotSerializedInJson() throws Exception {
    // 创建测试用户对象
    User user = new User();
    user.setId(1L);
    user.setAccount("testuser");
    user.setPassword("secret123456");
    user.setNickname("测试用户");
    user.setAvatar("https://example.com/avatar.jpg");
    user.setRole(0);
    user.setStatus(1);
    user.setCreditScore(100);
    user.setViolationCount(0);
    user.setCreateTime(LocalDateTime.now());
    user.setLastActiveTime(LocalDateTime.now());

    // 序列化为JSON字符串
    String json = objectMapper.writeValueAsString(user);

    // 验证JSON中不包含password字段名
    assertFalse(json.contains("password"),
        "JSON序列化结果不应包含'password'字段名");

    // 验证JSON中不包含密码值
    assertFalse(json.contains("secret123456"),
        "JSON序列化结果不应包含密码值");

    // 验证JSON中包含其他正常字段
    assertTrue(json.contains("testuser"),
        "JSON序列化结果应包含account字段值");
    assertTrue(json.contains("测试用户"),
        "JSON序列化结果应包含nickname字段值");

    // 打印JSON结果用于调试（可选）
    System.out.println("序列化后的JSON: " + json);
  }

  @Test
  @DisplayName("密码字段为null时序列化不应出现password字段")
  void testPasswordNullNotSerializedInJson() throws Exception {
    // 创建密码为null的用户对象
    User user = new User();
    user.setId(2L);
    user.setAccount("testuser2");
    user.setPassword(null);
    user.setNickname("测试用户2");

    // 序列化为JSON字符串
    String json = objectMapper.writeValueAsString(user);

    // 验证JSON中不包含password字段（即使值为null）
    assertFalse(json.contains("password"),
        "即使密码为null，JSON序列化结果也不应包含'password'字段");

    System.out.println("密码为null时的JSON: " + json);
  }

  @Test
  @DisplayName("密码字段为空字符串时序列化不应出现password字段")
  void testPasswordEmptyNotSerializedInJson() throws Exception {
    // 创建密码为空字符串的用户对象
    User user = new User();
    user.setId(3L);
    user.setAccount("testuser3");
    user.setPassword("");
    user.setNickname("测试用户3");

    // 序列化为JSON字符串
    String json = objectMapper.writeValueAsString(user);

    // 验证JSON中不包含password字段
    assertFalse(json.contains("password"),
        "即使密码为空字符串，JSON序列化结果也不应包含'password'字段");

    System.out.println("密码为空字符串时的JSON: " + json);
  }

  @Test
  @DisplayName("验证@JsonIgnore注解对getter方法的影响")
  void testPasswordGetterStillWorks() {
    // 创建用户对象
    User user = new User();
    String testPassword = "mySecretPassword";
    user.setPassword(testPassword);

    // 验证getter方法仍然可以正常工作
    assertEquals(testPassword, user.getPassword(),
        "@JsonIgnore注解不应影响getter方法的正常使用");
  }

  @Test
  @DisplayName("验证包含敏感信息的完整用户对象序列化")
  void testCompleteUserSerializationWithSensitiveData() throws Exception {
    // 创建包含所有字段的用户对象
    User user = new User();
    user.setId(100L);
    user.setAccount("admin");
    user.setPassword("$2a$10$hashedPasswordValue");
    user.setNickname("管理员");
    user.setAvatar("https://example.com/admin.jpg");
    user.setRole(1);
    user.setStatus(1);
    user.setCreditScore(100);
    user.setViolationCount(0);
    user.setCoverImage("https://example.com/cover.jpg");
    user.setCreateTime(LocalDateTime.now());
    user.setLastActiveTime(LocalDateTime.now());
    user.setLastNicknameUpdate(LocalDateTime.now());

    // 序列化
    String json = objectMapper.writeValueAsString(user);

    // 验证密码相关内容不在JSON中
    assertFalse(json.contains("password"),
        "完整用户对象序列化时不应包含password字段");
    assertFalse(json.contains("$2a$10$hashedPasswordValue"),
        "完整用户对象序列化时不应包含密码哈希值");

    // 验证其他字段正常序列化
    assertTrue(json.contains("admin"), "应包含account字段");
    assertTrue(json.contains("管理员"), "应包含nickname字段");
    assertTrue(json.contains("\"role\":1"), "应包含role字段");

    System.out.println("完整用户对象的JSON: " + json);
  }
}
