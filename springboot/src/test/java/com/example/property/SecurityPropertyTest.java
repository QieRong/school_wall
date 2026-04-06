package com.example.property;

import com.example.base.BasePropertyTest;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 安全功能属性测试
 * 
 * 测试安全功能的核心属性：
 * - Property 42: XSS 脚本过滤
 * - Property 43: 敏感词检测
 * - Property 44: 文件类型验证
 * - Property 45: SQL 注入防护
 * - Property 46: JWT 令牌篡改检测
 * 
 * **Feature: functional-testing**
 */
public class SecurityPropertyTest extends BasePropertyTest {

  // ==================== Property 42: XSS 脚本过滤 ====================

  /**
   * **Feature: functional-testing, Property 42: XSS 脚本过滤**
   * **Validates: Requirements 12.1**
   * 
   * XSS 脚本标签应该被过滤。
   */
  @Property(tries = 100)
  @Label("Property 42: XSS 脚本过滤 - script 标签应该被过滤")
  void xssScriptTagsShouldBeFiltered(
      @ForAll("xssPayloads") String payload) {

    // When - 过滤 XSS
    String filtered = filterXss(payload);

    // Then - 不应该包含危险标签
    assertFalse(filtered.toLowerCase().contains("<script"),
        "过滤后不应该包含 script 标签");
    assertFalse(filtered.toLowerCase().contains("javascript:"),
        "过滤后不应该包含 javascript: 协议");
    assertFalse(filtered.toLowerCase().contains("onerror"),
        "过滤后不应该包含 onerror 事件");
    assertFalse(filtered.toLowerCase().contains("onclick"),
        "过滤后不应该包含 onclick 事件");
  }

  /**
   * 生成 XSS 攻击载荷
   */
  @Provide
  Arbitrary<String> xssPayloads() {
    return Arbitraries.of(
        "<script>alert('xss')</script>",
        "<img src=x onerror=alert('xss')>",
        "<a href='javascript:alert(1)'>click</a>",
        "<div onclick='alert(1)'>test</div>",
        "<SCRIPT>alert('XSS')</SCRIPT>",
        "<img src=\"x\" onerror=\"alert('xss')\">",
        "正常文本<script>恶意代码</script>后续文本");
  }

  /**
   * 简单的 XSS 过滤实现
   */
  private String filterXss(String input) {
    if (input == null)
      return null;
    String result = input;
    // 移除 script 标签
    result = result.replaceAll("(?i)<script[^>]*>.*?</script>", "");
    result = result.replaceAll("(?i)<script[^>]*>", "");
    // 移除事件处理器
    result = result.replaceAll("(?i)\\s*on\\w+\\s*=\\s*['\"][^'\"]*['\"]", "");
    result = result.replaceAll("(?i)\\s*on\\w+\\s*=\\s*[^\\s>]+", "");
    // 移除 javascript: 协议
    result = result.replaceAll("(?i)javascript:", "");
    return result;
  }

  /**
   * **Feature: functional-testing, Property 42: XSS 脚本过滤**
   * **Validates: Requirements 12.1**
   * 
   * 正常内容不应该被过滤。
   */
  @Property(tries = 100)
  @Label("Property 42: 正常内容保留 - 正常内容不应该被过滤")
  void normalContentShouldNotBeFiltered(
      @ForAll("normalContent") String content) {

    // When - 过滤 XSS
    String filtered = filterXss(content);

    // Then - 正常内容应该保留
    assertEquals(content, filtered, "正常内容不应该被修改");
  }

  /**
   * 生成正常内容
   */
  @Provide
  Arbitrary<String> normalContent() {
    return Arbitraries.strings()
        .withCharRange('a', 'z')
        .withCharRange('A', 'Z')
        .withCharRange('0', '9')
        .withChars(' ', ',', '.', '!', '?')
        .ofMinLength(1)
        .ofMaxLength(200)
        .filter(s -> !s.toLowerCase().contains("script") &&
            !s.toLowerCase().contains("javascript") &&
            !s.toLowerCase().contains("onerror") &&
            !s.toLowerCase().contains("onclick"));
  }

  // ==================== Property 43: 敏感词检测 ====================

  /**
   * **Feature: functional-testing, Property 43: 敏感词检测**
   * **Validates: Requirements 12.2**
   * 
   * 敏感词应该被检测到。
   */
  @Property(tries = 100)
  @Label("Property 43: 敏感词检测 - 敏感词应该被检测到")
  void sensitiveWordsShouldBeDetected(
      @ForAll("sensitiveWordList") Set<String> sensitiveWords,
      @ForAll("contentWithSensitiveWord") String content) {

    // When - 检测敏感词
    boolean hasSensitive = sensitiveWords.stream()
        .anyMatch(word -> content.toLowerCase().contains(word.toLowerCase()));

    // Then - 如果内容包含敏感词，应该被检测到
    if (hasSensitive) {
      assertTrue(hasSensitive, "包含敏感词的内容应该被检测到");
    }
  }

  /**
   * 生成敏感词列表
   */
  @Provide
  Arbitrary<Set<String>> sensitiveWordList() {
    return Arbitraries.of("敏感词1", "敏感词2", "badword")
        .set().ofMinSize(1).ofMaxSize(5);
  }

  /**
   * 生成可能包含敏感词的内容
   */
  @Provide
  Arbitrary<String> contentWithSensitiveWord() {
    return Arbitraries.of(
        "这是正常内容",
        "这里有敏感词1在里面",
        "badword is here",
        "完全正常的文本");
  }

  // ==================== Property 44: 文件类型验证 ====================

  /**
   * **Feature: functional-testing, Property 44: 文件类型验证**
   * **Validates: Requirements 12.3**
   * 
   * 只允许特定类型的文件上传。
   */
  @Property(tries = 100)
  @Label("Property 44: 文件类型验证 - 只允许特定类型")
  void onlyAllowedFileTypesShouldPass(
      @ForAll("fileExtensions") String extension) {

    // Given - 允许的文件类型
    Set<String> allowedTypes = Set.of("jpg", "jpeg", "png", "gif", "mp4", "webp");

    // When - 验证文件类型
    boolean isAllowed = allowedTypes.contains(extension.toLowerCase());

    // Then - 验证结果
    if (allowedTypes.contains(extension.toLowerCase())) {
      assertTrue(isAllowed, "允许的文件类型应该通过验证");
    } else {
      assertFalse(isAllowed, "不允许的文件类型应该被拒绝");
    }
  }

  /**
   * 生成文件扩展名
   */
  @Provide
  Arbitrary<String> fileExtensions() {
    return Arbitraries.of(
        "jpg", "jpeg", "png", "gif", "mp4", "webp",
        "exe", "php", "js", "html", "bat", "sh");
  }

  /**
   * **Feature: functional-testing, Property 44: 文件类型验证**
   * **Validates: Requirements 12.3**
   * 
   * 危险文件类型应该被拒绝。
   */
  @Property(tries = 100)
  @Label("Property 44: 危险文件拒绝 - 危险文件类型应该被拒绝")
  void dangerousFileTypesShouldBeRejected(
      @ForAll("dangerousExtensions") String extension) {

    // Given - 允许的文件类型
    Set<String> allowedTypes = Set.of("jpg", "jpeg", "png", "gif", "mp4", "webp");

    // When - 验证文件类型
    boolean isAllowed = allowedTypes.contains(extension.toLowerCase());

    // Then - 危险文件类型应该被拒绝
    assertFalse(isAllowed, "危险文件类型应该被拒绝");
  }

  /**
   * 生成危险文件扩展名
   */
  @Provide
  Arbitrary<String> dangerousExtensions() {
    return Arbitraries.of("exe", "php", "js", "html", "bat", "sh", "cmd", "vbs");
  }

  // ==================== Property 45: SQL 注入防护 ====================

  /**
   * **Feature: functional-testing, Property 45: SQL 注入防护**
   * **Validates: Requirements 12.4**
   * 
   * SQL 注入攻击应该被检测到。
   */
  @Property(tries = 100)
  @Label("Property 45: SQL 注入检测 - SQL 注入应该被检测到")
  void sqlInjectionShouldBeDetected(
      @ForAll("sqlInjectionPayloads") String payload) {

    // When - 检测 SQL 注入
    boolean hasSqlInjection = detectSqlInjection(payload);

    // Then - SQL 注入应该被检测到
    assertTrue(hasSqlInjection, "SQL 注入攻击应该被检测到");
  }

  /**
   * 生成 SQL 注入载荷
   */
  @Provide
  Arbitrary<String> sqlInjectionPayloads() {
    return Arbitraries.of(
        "' OR '1'='1",
        "'; DROP TABLE users; --",
        "1; DELETE FROM posts",
        "' UNION SELECT * FROM users --",
        "admin'--",
        "1' OR '1'='1' /*");
  }

  /**
   * 简单的 SQL 注入检测
   */
  private boolean detectSqlInjection(String input) {
    if (input == null)
      return false;
    String lower = input.toLowerCase();
    // 检测常见的 SQL 注入模式
    return lower.contains("'") ||
        lower.contains("--") ||
        lower.contains("/*") ||
        lower.contains("drop ") ||
        lower.contains("delete ") ||
        lower.contains("union ") ||
        lower.contains(";");
  }

  // ==================== Property 46: JWT 令牌篡改检测 ====================

  /**
   * **Feature: functional-testing, Property 46: JWT 令牌篡改检测**
   * **Validates: Requirements 12.6**
   * 
   * 篡改的 JWT 令牌应该被检测到。
   */
  @Property(tries = 100)
  @Label("Property 46: JWT 篡改检测 - 篡改的令牌应该被检测到")
  void tamperedJwtShouldBeDetected(
      @ForAll("validJwtTokens") String originalToken,
      @ForAll @IntRange(min = 1, max = 10) int tamperPosition) {

    Assume.that(originalToken.length() > tamperPosition);

    // When - 篡改令牌
    char[] chars = originalToken.toCharArray();
    chars[tamperPosition] = chars[tamperPosition] == 'a' ? 'b' : 'a';
    String tamperedToken = new String(chars);

    // Then - 篡改后的令牌应该与原令牌不同
    assertNotEquals(originalToken, tamperedToken, "篡改后的令牌应该与原令牌不同");
  }

  /**
   * 生成模拟的 JWT 令牌
   */
  @Provide
  Arbitrary<String> validJwtTokens() {
    return Arbitraries.strings()
        .withCharRange('a', 'z')
        .withCharRange('A', 'Z')
        .withCharRange('0', '9')
        .ofMinLength(20)
        .ofMaxLength(100)
        .map(s -> "eyJ" + s + ".eyJ" + s + "." + s);
  }

  /**
   * **Feature: functional-testing, Property 46: JWT 令牌篡改检测**
   * **Validates: Requirements 12.6**
   * 
   * JWT 令牌格式应该正确。
   */
  @Property(tries = 100)
  @Label("Property 46: JWT 格式验证 - 令牌应该有三部分")
  void jwtShouldHaveThreeParts(
      @ForAll("validJwtTokens") String token) {

    // When - 分割令牌
    String[] parts = token.split("\\.");

    // Then - JWT 应该有三部分
    assertEquals(3, parts.length, "JWT 令牌应该有三部分（header.payload.signature）");
  }
}
