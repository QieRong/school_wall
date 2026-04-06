package com.example.property;

import com.example.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户密码序列化属性测试
 * 
 * Feature: code-quality-and-architecture-improvements, Property 1: 密码字段永不序列化
 * Validates: Requirements 1.2
 * 
 * 对于任何User对象，当其被序列化为JSON时，生成的JSON字符串中不应包含"password"字段或其值。
 */
@DisplayName("用户密码序列化属性测试")
@Tag("code-quality-and-architecture-improvements")
@Tag("property-test")
@Tag("password-serialization")
class UserPasswordPropertyTest {

  private ObjectMapper objectMapper;
  private Random random;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    random = new Random();
  }

  /**
   * Property 1: 密码字段永不序列化
   * 
   * 对于任何随机生成的User对象，序列化后的JSON不应包含password字段
   */
  @RepeatedTest(100)
  @DisplayName("Property 1: 随机User对象序列化时密码字段永不出现")
  void propertyPasswordNeverSerializedInJson() throws Exception {
    // Given: 生成随机User对象
    User user = generateRandomUser();
    String originalPassword = user.getPassword();

    // When: 序列化为JSON
    String json = objectMapper.writeValueAsString(user);

    // Then: JSON中不应包含password字段名
    assertFalse(json.contains("password"),
        "JSON序列化结果不应包含'password'字段名");

    // And: JSON中不应包含密码值（如果密码不为null）
    if (originalPassword != null && !originalPassword.isEmpty()) {
      assertFalse(json.contains(originalPassword),
          "JSON序列化结果不应包含密码值: " + originalPassword);
    }

    // And: 验证User对象的getter方法仍然可以访问密码
    assertEquals(originalPassword, user.getPassword(),
        "@JsonIgnore注解不应影响getter方法的正常使用");
  }

  /**
   * Property 1 补充: 包含BCrypt哈希密码的User对象序列化测试
   * 
   * 测试包含真实BCrypt哈希格式密码的User对象
   */
  @RepeatedTest(100)
  @DisplayName("Property 1 补充: BCrypt哈希密码不应被序列化")
  void propertyBCryptPasswordNeverSerialized() throws Exception {
    // Given: 生成包含BCrypt哈希密码的User对象
    User user = generateRandomUser();
    String bcryptPassword = generateBCryptLikePassword();
    user.setPassword(bcryptPassword);

    // When: 序列化为JSON
    String json = objectMapper.writeValueAsString(user);

    // Then: JSON中不应包含password字段
    assertFalse(json.contains("password"),
        "JSON不应包含password字段");

    // And: JSON中不应包含BCrypt哈希值
    assertFalse(json.contains(bcryptPassword),
        "JSON不应包含BCrypt哈希值");

    // And: JSON中不应包含BCrypt特征字符串
    assertFalse(json.contains("$2a$"),
        "JSON不应包含BCrypt特征字符串 $2a$");
  }

  /**
   * Property 1 补充: 密码为null时的序列化测试
   * 
   * 即使密码为null，password字段也不应出现在JSON中
   */
  @RepeatedTest(100)
  @DisplayName("Property 1 补充: 密码为null时password字段不应出现")
  void propertyNullPasswordNotSerialized() throws Exception {
    // Given: 生成密码为null的User对象
    User user = generateRandomUser();
    user.setPassword(null);

    // When: 序列化为JSON
    String json = objectMapper.writeValueAsString(user);

    // Then: JSON中不应包含password字段
    assertFalse(json.contains("password"),
        "即使密码为null，JSON也不应包含password字段");
  }

  /**
   * Property 1 补充: 密码为空字符串时的序列化测试
   * 
   * 即使密码为空字符串，password字段也不应出现在JSON中
   */
  @RepeatedTest(100)
  @DisplayName("Property 1 补充: 密码为空字符串时password字段不应出现")
  void propertyEmptyPasswordNotSerialized() throws Exception {
    // Given: 生成密码为空字符串的User对象
    User user = generateRandomUser();
    user.setPassword("");

    // When: 序列化为JSON
    String json = objectMapper.writeValueAsString(user);

    // Then: JSON中不应包含password字段
    assertFalse(json.contains("password"),
        "即使密码为空字符串，JSON也不应包含password字段");
  }

  /**
   * Property 1 补充: 包含特殊字符密码的序列化测试
   * 
   * 测试包含特殊字符的密码不会被序列化
   */
  @RepeatedTest(100)
  @DisplayName("Property 1 补充: 特殊字符密码不应被序列化")
  void propertySpecialCharPasswordNotSerialized() throws Exception {
    // Given: 生成包含特殊字符密码的User对象
    User user = generateRandomUser();
    String specialPassword = generateSpecialCharPassword();
    user.setPassword(specialPassword);

    // When: 序列化为JSON
    String json = objectMapper.writeValueAsString(user);

    // Then: JSON中不应包含password字段
    assertFalse(json.contains("password"),
        "JSON不应包含password字段");

    // And: JSON中不应包含特殊字符密码值
    // 注意：某些特殊字符可能在JSON中被转义，所以我们主要检查password字段不存在
    assertFalse(json.matches(".*[\"']password[\"']\\s*:\\s*.*"),
        "JSON不应包含password字段的键值对");
  }

  // ==================== 随机数据生成器 ====================

  /**
   * 生成随机User对象
   */
  private User generateRandomUser() {
    User user = new User();

    // 基础字段
    user.setId(random.nextLong(1, 100000));
    user.setAccount(generateRandomAccount());
    user.setPassword(generateRandomPassword());
    user.setNickname(generateRandomNickname());
    user.setAvatar(generateRandomAvatar());

    // 角色和状态
    user.setRole(random.nextInt(2)); // 0:用户 1:管理员
    user.setStatus(random.nextInt(2)); // 0:封禁 1:正常

    // 信用和违规
    user.setCreditScore(random.nextInt(0, 101)); // 0-100
    user.setViolationCount(random.nextInt(0, 11)); // 0-10

    // 时间字段
    user.setCreateTime(generateRandomDateTime());
    user.setLastActiveTime(generateRandomDateTime());

    // 个人中心字段（随机决定是否设置）
    if (random.nextBoolean()) {
      user.setCoverImage(generateRandomCoverImage());
    }
    if (random.nextBoolean()) {
      user.setLastNicknameUpdate(generateRandomDateTime());
    }

    // 封禁时间（如果状态为封禁）
    if (user.getStatus() == 0 && random.nextBoolean()) {
      user.setBanEndTime(generateRandomFutureDateTime());
    }

    return user;
  }

  /**
   * 生成随机账号（学号格式：10位数字）
   */
  private String generateRandomAccount() {
    return String.format("%010d", random.nextLong(1000000000L, 10000000000L));
  }

  /**
   * 生成随机密码（6-20位字符）
   */
  private String generateRandomPassword() {
    int length = random.nextInt(6, 21);
    StringBuilder sb = new StringBuilder();
    String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    for (int i = 0; i < length; i++) {
      sb.append(chars.charAt(random.nextInt(chars.length())));
    }
    return sb.toString();
  }

  /**
   * 生成BCrypt格式的密码（模拟）
   * BCrypt哈希格式：$2a$10$ + 53个字符
   */
  private String generateBCryptLikePassword() {
    // UUID去掉连字符后是32个字符，需要生成53个字符
    String uuid1 = UUID.randomUUID().toString().replace("-", "");
    String uuid2 = UUID.randomUUID().toString().replace("-", "");
    // 拼接两个UUID并截取前53个字符
    return "$2a$10$" + (uuid1 + uuid2).substring(0, 53);
  }

  /**
   * 生成包含特殊字符的密码
   */
  private String generateSpecialCharPassword() {
    String[] specialPasswords = {
        "Pass@123!",
        "Test#456$",
        "User&789%",
        "Admin*012^",
        "密码123456",
        "测试@#$%",
        "pass word",
        "test\ttab",
        "new\nline",
        "quote\"test",
        "slash\\test"
    };
    return specialPasswords[random.nextInt(specialPasswords.length)];
  }

  /**
   * 生成随机昵称
   */
  private String generateRandomNickname() {
    String[] prefixes = { "用户", "学生", "同学", "小", "大", "老", "新" };
    String[] suffixes = { "A", "B", "C", "123", "456", "789", "abc", "xyz" };
    return prefixes[random.nextInt(prefixes.length)] +
        suffixes[random.nextInt(suffixes.length)];
  }

  /**
   * 生成随机头像URL
   */
  private String generateRandomAvatar() {
    if (random.nextBoolean()) {
      return "https://example.com/avatar/" + UUID.randomUUID() + ".jpg";
    } else {
      return "/files/avatar_" + random.nextInt(1000) + ".png";
    }
  }

  /**
   * 生成随机封面图URL
   */
  private String generateRandomCoverImage() {
    return "https://example.com/cover/" + UUID.randomUUID() + ".jpg";
  }

  /**
   * 生成随机日期时间（过去一年内）
   */
  private LocalDateTime generateRandomDateTime() {
    LocalDateTime now = LocalDateTime.now();
    int daysAgo = random.nextInt(365);
    int hoursAgo = random.nextInt(24);
    int minutesAgo = random.nextInt(60);
    return now.minusDays(daysAgo).minusHours(hoursAgo).minusMinutes(minutesAgo);
  }

  /**
   * 生成随机未来日期时间（未来30天内）
   */
  private LocalDateTime generateRandomFutureDateTime() {
    LocalDateTime now = LocalDateTime.now();
    int daysLater = random.nextInt(1, 31);
    int hoursLater = random.nextInt(24);
    return now.plusDays(daysLater).plusHours(hoursLater);
  }
}
