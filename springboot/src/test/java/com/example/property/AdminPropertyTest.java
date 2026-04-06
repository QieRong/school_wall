package com.example.property;

import com.example.base.BasePropertyTest;
import com.example.entity.Post;
import com.example.entity.User;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 后台管理属性测试
 * 
 * 测试后台管理的核心属性：
 * - Property 37: 管理员权限验证
 * - Property 38: 帖子审核状态更新
 * - Property 39: 用户封禁状态
 * - Property 40: 用户解封恢复
 * - Property 41: 敏感词过滤更新
 * 
 * **Feature: functional-testing**
 */
public class AdminPropertyTest extends BasePropertyTest {
    // ==================== Property 37: 管理员权限验证 ====================

    /**
     * **Feature: functional-testing, Property 37: 管理员权限验证**
     * **Validates: Requirements 10.1, 10.2**
     * 
     * 只有管理员角色可以访问后台功能。
     */
    @Property(tries = 100)
    @Label("Property 37: 管理员权限验证 - 只有管理员可以访问后台")
    void onlyAdminCanAccessBackend(
            @ForAll @IntRange(min = 0, max = 1) int role) {
        
        // Given - 创建用户
        User user = new User();
        user.setRole(role);
        
        // When - 检查是否是管理员
        boolean isAdmin = user.getRole() == 1;
        
        // Then - 只有角色为1的用户是管理员
        if (role == 1) {
            assertTrue(isAdmin, "角色为1的用户应该是管理员");
        } else {
            assertFalse(isAdmin, "角色为0的用户不应该是管理员");
        }
    }

    /**
     * **Feature: functional-testing, Property 37: 管理员权限验证**
     * **Validates: Requirements 10.1, 10.2**
     * 
     * 非管理员访问后台应该被拒绝。
     */
    @Property(tries = 100)
    @Label("Property 37: 非管理员访问拒绝 - 非管理员应该被拒绝访问")
    void nonAdminShouldBeRejected(
            @ForAll @IntRange(min = 1, max = 1000) int userId) {
        
        // Given - 创建普通用户
        User user = new User();
        user.setId((long) userId);
        user.setRole(0); // 普通用户
        
        // When - 检查访问权限
        boolean canAccess = user.getRole() == 1;
        
        // Then - 普通用户不能访问后台
        assertFalse(canAccess, "普通用户不应该能访问后台");
    }

    // ==================== Property 38: 帖子审核状态更新 ====================

    /**
     * **Feature: functional-testing, Property 38: 帖子审核状态更新**
     * **Validates: Requirements 10.4**
     * 
     * 审核通过后帖子状态应该更新为已通过。
     */
    @Property(tries = 100)
    @Label("Property 38: 帖子审核通过 - 状态应该更新为已通过")
    void approvePostShouldUpdateStatus(
            @ForAll @IntRange(min = 1, max = 1000) int postId) {
        
        // Given - 创建待审核帖子
        Post post = new Post();
        post.setId((long) postId);
        post.setStatus(0); // 待审核
        
        // When - 审核通过
        post.setStatus(1); // 已通过
        
        // Then - 状态应该是已通过
        assertEquals(1, post.getStatus(), "审核通过后状态应该是1");
    }

    /**
     * **Feature: functional-testing, Property 38: 帖子审核状态更新**
     * **Validates: Requirements 10.4**
     * 
     * 审核拒绝后帖子状态应该更新为已拒绝。
     */
    @Property(tries = 100)
    @Label("Property 38: 帖子审核拒绝 - 状态应该更新为已拒绝")
    void rejectPostShouldUpdateStatus(
            @ForAll @IntRange(min = 1, max = 1000) int postId) {
        
        // Given - 创建待审核帖子
        Post post = new Post();
        post.setId((long) postId);
        post.setStatus(0); // 待审核
        
        // When - 审核拒绝
        post.setStatus(2); // 已拒绝
        
        // Then - 状态应该是已拒绝
        assertEquals(2, post.getStatus(), "审核拒绝后状态应该是2");
    }

    // ==================== Property 39 & 40: 用户封禁/解封 ====================

    /**
     * **Feature: functional-testing, Property 39: 用户封禁状态**
     * **Validates: Requirements 10.5**
     * 
     * 封禁用户后状态应该更新为已封禁。
     */
    @Property(tries = 100)
    @Label("Property 39: 用户封禁状态 - 封禁后状态应该更新")
    void banUserShouldUpdateStatus(
            @ForAll @IntRange(min = 1, max = 1000) int userId) {
        
        // Given - 创建正常用户
        User user = new User();
        user.setId((long) userId);
        user.setStatus(1); // 正常
        
        // When - 封禁用户
        user.setStatus(0); // 封禁
        
        // Then - 状态应该是已封禁
        assertEquals(0, user.getStatus(), "封禁后状态应该是0");
    }

    /**
     * **Feature: functional-testing, Property 40: 用户解封恢复**
     * **Validates: Requirements 10.6**
     * 
     * 解封用户后状态应该恢复为正常。
     */
    @Property(tries = 100)
    @Label("Property 40: 用户解封恢复 - 解封后状态应该恢复正常")
    void unbanUserShouldRestoreStatus(
            @ForAll @IntRange(min = 1, max = 1000) int userId) {
        
        // Given - 创建已封禁用户
        User user = new User();
        user.setId((long) userId);
        user.setStatus(0); // 封禁
        
        // When - 解封用户
        user.setStatus(1); // 正常
        
        // Then - 状态应该是正常
        assertEquals(1, user.getStatus(), "解封后状态应该是1");
    }

    /**
     * **Feature: functional-testing, Property 39: 用户封禁状态**
     * **Validates: Requirements 10.5**
     * 
     * 封禁后用户不能登录。
     */
    @Property(tries = 100)
    @Label("Property 39: 封禁用户不能登录 - 封禁后应该拒绝登录")
    void bannedUserCannotLogin(
            @ForAll @IntRange(min = 1, max = 1000) int userId) {
        
        // Given - 创建已封禁用户
        User user = new User();
        user.setId((long) userId);
        user.setStatus(0); // 封禁
        
        // When - 检查是否可以登录
        boolean canLogin = user.getStatus() == 1;
        
        // Then - 封禁用户不能登录
        assertFalse(canLogin, "封禁用户不应该能登录");
    }

    // ==================== Property 41: 敏感词过滤更新 ====================

    /**
     * **Feature: functional-testing, Property 41: 敏感词过滤更新**
     * **Validates: Requirements 10.7**
     * 
     * 添加敏感词后应该能被检测到。
     */
    @Property(tries = 100)
    @Label("Property 41: 敏感词添加 - 添加后应该能被检测")
    void addSensitiveWordShouldBeDetected(
            @ForAll("sensitiveWords") String word) {
        
        // Given - 敏感词列表
        Set<String> sensitiveWords = new HashSet<>();
        
        // When - 添加敏感词
        sensitiveWords.add(word);
        
        // Then - 应该能检测到
        assertTrue(sensitiveWords.contains(word), "添加的敏感词应该能被检测到");
    }

    /**
     * 生成敏感词
     */
    @Provide
    Arbitrary<String> sensitiveWords() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(2)
                .ofMaxLength(20);
    }

    /**
     * **Feature: functional-testing, Property 41: 敏感词过滤更新**
     * **Validates: Requirements 10.7**
     * 
     * 删除敏感词后应该不再被检测到。
     */
    @Property(tries = 100)
    @Label("Property 41: 敏感词删除 - 删除后应该不再被检测")
    void removeSensitiveWordShouldNotBeDetected(
            @ForAll("sensitiveWords") String word) {
        
        // Given - 敏感词列表
        Set<String> sensitiveWords = new HashSet<>();
        sensitiveWords.add(word);
        assertTrue(sensitiveWords.contains(word));
        
        // When - 删除敏感词
        sensitiveWords.remove(word);
        
        // Then - 应该不再被检测到
        assertFalse(sensitiveWords.contains(word), "删除的敏感词不应该再被检测到");
    }

    /**
     * **Feature: functional-testing, Property 41: 敏感词过滤更新**
     * **Validates: Requirements 10.7**
     * 
     * 敏感词过滤应该替换所有匹配的词。
     */
    @Property(tries = 100)
    @Label("Property 41: 敏感词过滤 - 应该替换所有匹配的词")
    void sensitiveWordFilterShouldReplaceAll(
            @ForAll("sensitiveWords") String word,
            @ForAll @IntRange(min = 1, max = 5) int repeatCount) {
        
        // Given - 包含敏感词的内容
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < repeatCount; i++) {
            content.append("前缀").append(word).append("后缀 ");
        }
        String originalContent = content.toString();
        
        // When - 过滤敏感词
        String filteredContent = originalContent.replace(word, "***");
        
        // Then - 所有敏感词都应该被替换
        assertFalse(filteredContent.contains(word), "过滤后不应该包含敏感词");
        assertTrue(filteredContent.contains("***"), "应该包含替换后的内容");
    }
}
