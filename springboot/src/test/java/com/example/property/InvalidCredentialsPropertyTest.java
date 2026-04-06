package com.example.property;

import cn.hutool.crypto.digest.BCrypt;
import com.example.utils.JwtUtils;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 无效凭证拒绝属性测试
 * 
 * **Feature: functional-testing, Property 2: 无效凭证拒绝**
 * **Validates: Requirements 1.2**
 * 
 * 对于任何无效凭证（错误密码或不存在的账号），系统应该拒绝登录尝试并返回错误。
 */
class InvalidCredentialsPropertyTest {

  private final JwtUtils jwtUtils;

  InvalidCredentialsPropertyTest() {
    jwtUtils = new JwtUtils();
    // 手动设置JWT配置，因为不使用Spring上下文
    Object target = Objects.requireNonNull(jwtUtils);
    ReflectionTestUtils.setField(target, "secret", "test-secret-key-for-unit-tests-only");
    ReflectionTestUtils.setField(target, "expiration", 86400000L);
  }

  /**
   * Property 2: 无效凭证拒绝 - 错误密码验证
   * 
   * 对于任何正确的密码和任何不同的密码，BCrypt 验证应该失败
   */
  @Property(tries = 100)
  @Label("Property 2: 错误密码应该验证失败")
  void wrongPasswordShouldFailVerification(
      @ForAll @StringLength(min = 6, max = 20) @AlphaChars String correctPassword,
      @ForAll @StringLength(min = 6, max = 20) @AlphaChars String wrongPassword) {
    // 跳过相同密码的情况
    Assume.that(!correctPassword.equals(wrongPassword));

    // Given: 使用正确密码生成哈希
    String hashedPassword = BCrypt.hashpw(correctPassword);

    // When: 使用错误密码验证
    boolean result = BCrypt.checkpw(wrongPassword, hashedPassword);

    // Then: 验证应该失败
    assertFalse(result, "错误密码不应该通过验证");
  }

  /**
   * Property 2 补充: 空密码验证
   * 
   * 空密码不应该通过任何哈希的验证
   */
  @Property(tries = 50)
  @Label("Property 2 补充: 空密码应该验证失败")
  void emptyPasswordShouldFailVerification(
      @ForAll @StringLength(min = 6, max = 20) @AlphaChars String correctPassword) {
    // Given: 使用正确密码生成哈希
    String hashedPassword = BCrypt.hashpw(correctPassword);

    // When & Then: 空密码验证应该失败
    assertFalse(BCrypt.checkpw("", hashedPassword), "空密码不应该通过验证");
  }

  /**
   * Property 2 补充: 密码大小写敏感
   * 
   * 密码验证应该区分大小写
   */
  @Property(tries = 50)
  @Label("Property 2 补充: 密码应该区分大小写")
  void passwordShouldBeCaseSensitive(
      @ForAll @StringLength(min = 6, max = 20) @AlphaChars String password) {
    // 跳过全大写或全小写的密码
    String upperCase = password.toUpperCase();
    String lowerCase = password.toLowerCase();
    Assume.that(!password.equals(upperCase) || !password.equals(lowerCase));

    // Given: 使用原始密码生成哈希
    String hashedPassword = BCrypt.hashpw(password);

    // When: 使用不同大小写的密码验证
    boolean upperResult = BCrypt.checkpw(upperCase, hashedPassword);
    boolean lowerResult = BCrypt.checkpw(lowerCase, hashedPassword);

    // Then: 如果原始密码不是全大写/全小写，则大小写变换后应该验证失败
    if (!password.equals(upperCase)) {
      assertFalse(upperResult, "大写密码不应该通过验证");
    }
    if (!password.equals(lowerCase)) {
      assertFalse(lowerResult, "小写密码不应该通过验证");
    }
  }

  /**
   * Property 2 补充: 密码前后空格敏感
   * 
   * 密码验证应该区分前后空格
   */
  @Property(tries = 50)
  @Label("Property 2 补充: 密码应该区分前后空格")
  void passwordShouldBeSpaceSensitive(
      @ForAll @StringLength(min = 6, max = 18) @AlphaChars String password) {
    // Given: 使用原始密码生成哈希
    String hashedPassword = BCrypt.hashpw(password);

    // When: 使用带空格的密码验证
    boolean leadingSpaceResult = BCrypt.checkpw(" " + password, hashedPassword);
    boolean trailingSpaceResult = BCrypt.checkpw(password + " ", hashedPassword);

    // Then: 带空格的密码应该验证失败
    assertFalse(leadingSpaceResult, "前导空格密码不应该通过验证");
    assertFalse(trailingSpaceResult, "尾随空格密码不应该通过验证");
  }

  /**
   * Property 2 补充: 无效 Token 验证
   * 
   * 任何无效格式的 Token 都应该验证失败
   */
  @Property(tries = 100)
  @Label("Property 2 补充: 无效 Token 应该验证失败")
  void invalidTokenShouldFailVerification(
      @ForAll("invalidTokens") String invalidToken) {
    // When: 验证无效 Token
    Long result = jwtUtils.verifyToken(invalidToken);

    // Then: 应该返回 null
    assertNull(result, "无效 Token 验证应该返回 null");
  }

  @Provide
  Arbitrary<String> invalidTokens() {
    return Arbitraries.oneOf(
        // 空字符串
        Arbitraries.just(""),
        // 随机字符串
        Arbitraries.strings().alpha().ofMinLength(10).ofMaxLength(50),
        // 格式错误的 JWT（缺少部分）
        Arbitraries.strings().alpha().ofLength(20).map(s -> s + "."),
        // 格式错误的 JWT（只有两部分）
        Arbitraries.strings().alpha().ofLength(20).map(s -> s + "." + s),
        // 伪造的 JWT 格式
        Arbitraries.just("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.fake.payload"));
  }

  /**
   * Property 2 补充: 密码长度边界测试
   * 
   * 测试各种长度的密码
   */
  @Property(tries = 30)
  @Label("Property 2 补充: 各种长度密码的验证")
  void variousLengthPasswordsShouldWork(
      @ForAll("passwordLengths") int length) {
    // Given: 生成指定长度的密码
    String password = "a".repeat(length);
    String hashedPassword = BCrypt.hashpw(password);

    // When: 验证正确密码
    boolean correctResult = BCrypt.checkpw(password, hashedPassword);
    // 验证错误密码（多一个字符）
    boolean wrongResult = BCrypt.checkpw(password + "x", hashedPassword);

    // Then
    assertTrue(correctResult, "正确密码应该通过验证");
    assertFalse(wrongResult, "错误密码不应该通过验证");
  }

  @Provide
  Arbitrary<Integer> passwordLengths() {
    return Arbitraries.of(6, 8, 10, 12, 16, 20);
  }
}
