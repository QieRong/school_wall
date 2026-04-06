package com.example.property;

import cn.hutool.crypto.digest.BCrypt;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 密码加密一致性属性测试
 * 
 * **Feature: functional-testing, Property 3: 密码加密一致性**
 * **Validates: Requirements 1.4**
 * 
 * 对于任何用户注册，存储的密码应该是 BCrypt 加密的，并且可以与原始密码验证。
 */
class PasswordEncryptionPropertyTest {

  /**
   * Property 3: 密码加密一致性 - 加密后可验证
   * 
   * 对于任何密码，BCrypt 加密后应该能够正确验证原始密码
   */
  @Property(tries = 30)
  @Label("Property 3: 密码加密后应该能正确验证原始密码")
  void encryptedPasswordShouldBeVerifiable(
      @ForAll @StringLength(min = 6, max = 20) String password) {
    // Given: 原始密码
    // When: 使用 BCrypt 加密
    String hashedPassword = BCrypt.hashpw(password);

    // Then: 加密后的密码不应该等于原始密码
    assertNotEquals(password, hashedPassword, "加密后的密码不应该等于原始密码");

    // And: 加密后的密码应该能验证原始密码
    assertTrue(BCrypt.checkpw(password, hashedPassword), "加密后的密码应该能验证原始密码");
  }

  /**
   * Property 3 补充: 加密结果唯一性
   * 
   * 对于相同的密码，每次加密应该产生不同的哈希（因为使用随机盐）
   */
  @Property(tries = 15)
  @Label("Property 3 补充: 相同密码每次加密应该产生不同的哈希")
  void samePasswordShouldProduceDifferentHashes(
      @ForAll @StringLength(min = 6, max = 20) String password) {
    // Given: 相同的密码
    // When: 两次加密
    String hash1 = BCrypt.hashpw(password);
    String hash2 = BCrypt.hashpw(password);

    // Then: 两次加密结果应该不同
    assertNotEquals(hash1, hash2, "相同密码的两次加密结果应该不同");

    // But: 两个哈希都应该能验证原始密码
    assertTrue(BCrypt.checkpw(password, hash1), "第一个哈希应该能验证原始密码");
    assertTrue(BCrypt.checkpw(password, hash2), "第二个哈希应该能验证原始密码");
  }

  /**
   * Property 3 补充: 哈希格式正确性
   * 
   * BCrypt 哈希应该符合标准格式
   */
  @Property(tries = 15)
  @Label("Property 3 补充: BCrypt 哈希应该符合标准格式")
  void bcryptHashShouldHaveCorrectFormat(
      @ForAll @StringLength(min = 6, max = 20) String password) {
    // Given: 原始密码
    // When: 使用 BCrypt 加密
    String hashedPassword = BCrypt.hashpw(password);

    // Then: 哈希应该以 $2a$ 或 $2b$ 开头（BCrypt 标识）
    assertTrue(
        hashedPassword.startsWith("$2a$") || hashedPassword.startsWith("$2b$"),
        "BCrypt 哈希应该以 $2a$ 或 $2b$ 开头");

    // And: 哈希长度应该是 60 字符
    assertEquals(60, hashedPassword.length(), "BCrypt 哈希长度应该是 60 字符");
  }

  /**
   * Property 3 补充: 特殊字符密码测试
   * 
   * 包含特殊字符的密码也应该能正确加密和验证
   */
  @Property(tries = 10)
  @Label("Property 3 补充: 特殊字符密码应该能正确加密和验证")
  void specialCharacterPasswordsShouldWork(
      @ForAll("specialPasswords") String password) {
    // Given: 包含特殊字符的密码
    // When: 使用 BCrypt 加密
    String hashedPassword = BCrypt.hashpw(password);

    // Then: 应该能正确验证
    assertTrue(BCrypt.checkpw(password, hashedPassword), "特殊字符密码应该能正确验证");
  }

  @Provide
  Arbitrary<String> specialPasswords() {
    return Arbitraries.oneOf(
        // 包含数字
        Arbitraries.strings().withCharRange('a', 'z').ofLength(4)
            .map(s -> s + "123"),
        // 包含特殊字符
        Arbitraries.strings().withCharRange('a', 'z').ofLength(4)
            .map(s -> s + "!@#"),
        // 包含空格
        Arbitraries.strings().withCharRange('a', 'z').ofLength(3)
            .map(s -> s + " " + s),
        // 中文密码
        Arbitraries.of("密码123456", "测试密码abc", "中文test"),
        // 混合字符
        Arbitraries.of("Aa1!Bb2@", "Test_123", "pass-word"));
  }

  /**
   * Property 3 补充: 密码不可逆性
   * 
   * 从哈希值不应该能推导出原始密码（通过验证其他密码来间接测试）
   */
  @Property(tries = 15)
  @Label("Property 3 补充: 哈希值不应该泄露密码信息")
  void hashShouldNotLeakPasswordInfo(
      @ForAll @StringLength(min = 6, max = 20) @AlphaChars String password1,
      @ForAll @StringLength(min = 6, max = 20) @AlphaChars String password2) {
    // 跳过相同密码
    Assume.that(!password1.equals(password2));

    // Given: 两个不同的密码
    String hash1 = BCrypt.hashpw(password1);
    String hash2 = BCrypt.hashpw(password2);

    // Then: 每个哈希只能验证对应的密码
    assertTrue(BCrypt.checkpw(password1, hash1), "hash1 应该能验证 password1");
    assertTrue(BCrypt.checkpw(password2, hash2), "hash2 应该能验证 password2");
    assertFalse(BCrypt.checkpw(password1, hash2), "hash2 不应该能验证 password1");
    assertFalse(BCrypt.checkpw(password2, hash1), "hash1 不应该能验证 password2");
  }

  /**
   * Property 3 补充: 边界长度密码测试
   * 
   * 测试最短和较长密码的加密
   */
  @Property(tries = 8)
  @Label("Property 3 补充: 边界长度密码应该能正确加密")
  void boundaryLengthPasswordsShouldWork(
      @ForAll("boundaryPasswords") String password) {
    // Given: 边界长度的密码
    // When: 使用 BCrypt 加密
    String hashedPassword = BCrypt.hashpw(password);

    // Then: 应该能正确验证
    assertTrue(BCrypt.checkpw(password, hashedPassword), "边界长度密码应该能正确验证");
  }

  @Provide
  Arbitrary<String> boundaryPasswords() {
    return Arbitraries.oneOf(
        // 最短密码 (6位)
        Arbitraries.strings().alpha().ofLength(6),
        // 常见长度 (8位)
        Arbitraries.strings().alpha().ofLength(8),
        // 较长密码 (20位)
        Arbitraries.strings().alpha().ofLength(20),
        // 很长密码 (50位)
        Arbitraries.strings().alpha().ofLength(50));
  }
}
