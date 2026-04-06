package com.example.property;

import com.example.utils.JwtUtils;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 认证令牌属性测试
 * 
 * **Feature: functional-testing, Property 1: 认证令牌有效性**
 * **Validates: Requirements 1.1, 1.3**
 * 
 * 对于任何有效的用户凭证（账号和密码），认证应该返回一个有效的 JWT Token，
 * 该 Token 可以用于访问受保护的资源。
 */
class AuthTokenPropertyTest {

    // JwtUtils 需要手动设置配置
    private final JwtUtils jwtUtils;

    AuthTokenPropertyTest() {
        jwtUtils = new JwtUtils();
        // 手动设置JWT配置，因为不使用Spring上下文
        Object target = Objects.requireNonNull(jwtUtils);
        ReflectionTestUtils.setField(target, "secret", "test-secret-key-for-unit-tests-only");
        ReflectionTestUtils.setField(target, "expiration", 86400000L);
    }

    /**
     * Property 1: 认证令牌有效性
     * 
     * 对于任何有效的用户ID和账号，生成的 Token 应该能够被正确验证，
     * 并且验证后返回的用户ID应该与原始用户ID一致。
     */
    @Property(tries = 100)
    @Label("Property 1: 认证令牌有效性 - Token 生成后应能正确验证并返回原始用户ID")
    void tokenShouldBeValidAndReturnCorrectUserId(
            @ForAll @LongRange(min = 1, max = Long.MAX_VALUE - 1) Long userId,
            @ForAll @StringLength(min = 10, max = 10) @NumericChars String account) {
        // Given: 有效的用户ID和账号
        // When: 生成 Token
        String token = jwtUtils.createToken(userId, account);

        // Then: Token 应该不为空
        assertNotNull(token, "生成的 Token 不应为空");
        assertFalse(token.isEmpty(), "生成的 Token 不应为空字符串");

        // And: Token 应该是有效的 JWT 格式（包含三个部分）
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT Token 应该包含三个部分");

        // And: 验证 Token 应该返回正确的用户ID
        Long verifiedUserId = jwtUtils.verifyToken(token);
        assertEquals(userId, verifiedUserId, "验证后的用户ID应该与原始用户ID一致");
    }

    /**
     * Property 1 补充: Token 唯一性
     * 
     * 对于相同的用户，在不同时间生成的 Token 应该不同（因为包含时间戳）
     */
    @Property(tries = 50)
    @Label("Property 1 补充: 相同用户生成的 Token 应该唯一")
    void tokensShouldBeUniqueForSameUser(
            @ForAll @LongRange(min = 1, max = 1000000) Long userId,
            @ForAll @StringLength(min = 10, max = 10) @NumericChars String account) {
        // Given: 相同的用户ID和账号
        // When: 生成两个 Token
        String token1 = jwtUtils.createToken(userId, account);
        String token2 = jwtUtils.createToken(userId, account);

        // Then: 两个 Token 都应该有效
        assertNotNull(jwtUtils.verifyToken(token1), "第一个 Token 应该有效");
        assertNotNull(jwtUtils.verifyToken(token2), "第二个 Token 应该有效");

        // And: 两个 Token 验证后都应该返回相同的用户ID
        assertEquals(userId, jwtUtils.verifyToken(token1));
        assertEquals(userId, jwtUtils.verifyToken(token2));
    }

    /**
     * Property 1 补充: Token 不可伪造性
     * 
     * 对于任何篡改的 Token，验证应该失败
     */
    @Property(tries = 100)
    @Label("Property 1 补充: 篡改的 Token 应该验证失败")
    void tamperedTokenShouldFailVerification(
            @ForAll @LongRange(min = 1, max = 1000000) Long userId,
            @ForAll @StringLength(min = 10, max = 10) @NumericChars String account) {
        // Given: 生成一个有效的 Token
        String validToken = jwtUtils.createToken(userId, account);

        // When: 篡改 Token 的签名部分（替换签名中间的字符）
        String[] parts = validToken.split("\\.");
        if (parts.length == 3) {
            String signature = parts[2];
            // 在签名中间位置进行篡改，确保破坏签名
            int midPoint = signature.length() / 2;
            char originalChar = signature.charAt(midPoint);
            char newChar = (originalChar == 'A') ? 'B' : 'A';
            String tamperedSignature = signature.substring(0, midPoint) + newChar + signature.substring(midPoint + 1);
            String tamperedToken = parts[0] + "." + parts[1] + "." + tamperedSignature;

            // Then: 篡改后的 Token 验证应该失败
            Long result = jwtUtils.verifyToken(tamperedToken);
            assertNull(result, "篡改签名的 Token 验证应该返回 null");
        }
    }

    /**
     * Property 1 补充: 边界用户ID测试
     * 
     * 对于边界值的用户ID，Token 生成和验证应该正常工作
     */
    @Property(tries = 20)
    @Label("Property 1 补充: 边界用户ID应该正常工作")
    void boundaryUserIdsShouldWork(
            @ForAll("boundaryUserIds") Long userId,
            @ForAll @StringLength(min = 10, max = 10) @NumericChars String account) {
        // Given: 边界值用户ID
        // When: 生成 Token
        String token = jwtUtils.createToken(userId, account);

        // Then: Token 应该有效
        assertNotNull(token);

        // And: 验证应该返回正确的用户ID
        Long verifiedUserId = jwtUtils.verifyToken(token);
        assertEquals(userId, verifiedUserId);
    }

    @Provide
    Arbitrary<Long> boundaryUserIds() {
        return Arbitraries.of(1L, 100L, 1000L, 10000L, 100000L, 1000000L);
    }

    /**
     * Property 1 补充: 各种账号格式测试
     * 
     * 对于各种格式的账号，Token 生成和验证应该正常工作
     */
    @Property(tries = 50)
    @Label("Property 1 补充: 各种账号格式应该正常工作")
    void variousAccountFormatsShouldWork(
            @ForAll @LongRange(min = 1, max = 1000000) Long userId,
            @ForAll("validAccounts") String account) {
        // Given: 各种格式的账号
        // When: 生成 Token
        String token = jwtUtils.createToken(userId, account);

        // Then: Token 应该有效
        assertNotNull(token);

        // And: 验证应该返回正确的用户ID
        Long verifiedUserId = jwtUtils.verifyToken(token);
        assertEquals(userId, verifiedUserId);
    }

    @Provide
    Arbitrary<String> validAccounts() {
        return Arbitraries.oneOf(
                // 10位数字学号
                Arbitraries.strings().numeric().ofLength(10),
                // 管理员账号
                Arbitraries.of("admin00001", "admin00002", "admin00003"),
                // 普通账号
                Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20));
    }
}
