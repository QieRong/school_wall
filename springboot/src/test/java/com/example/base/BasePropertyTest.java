package com.example.base;

import net.jqwik.api.*;

import java.util.List;

/**
 * 属性测试基类
 * 提供通用的数据生成器和测试工具方法
 * 
 * 所有属性测试类都应该继承此类以获取通用的生成器
 */
public abstract class BasePropertyTest {

    // ==================== 用户相关生成器 ====================

    /**
     * 生成有效的用户账号（学号格式）
     */
    @Provide
    protected Arbitrary<String> validAccounts() {
        return Arbitraries.strings()
                .numeric()
                .ofLength(10);
    }

    /**
     * 生成有效的密码（6-20位）
     */
    @Provide
    protected Arbitrary<String> validPasswords() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(6)
                .ofMaxLength(20);
    }

    /**
     * 生成有效的昵称（1-20位）
     */
    @Provide
    protected Arbitrary<String> validNicknames() {
        return Arbitraries.strings()
                .ofMinLength(1)
                .ofMaxLength(20)
                .filter(s -> !s.isBlank());
    }

    // ==================== 帖子相关生成器 ====================

    /**
     * 生成有效的帖子内容（1-2000字符）
     */
    @Provide
    protected Arbitrary<String> validPostContent() {
        return Arbitraries.strings()
                .ofMinLength(1)
                .ofMaxLength(2000)
                .filter(s -> !s.isBlank());
    }

    /**
     * 生成有效的帖子分类（1-5）
     */
    @Provide
    protected Arbitrary<Integer> validCategories() {
        return Arbitraries.integers().between(1, 5);
    }

    /**
     * 生成有效的可见性设置（0-2）
     * 0: 公开, 1: 仅互关, 2: 仅自己
     */
    @Provide
    protected Arbitrary<Integer> validVisibility() {
        return Arbitraries.integers().between(0, 2);
    }

    /**
     * 生成有效的标签列表（0-5个标签）
     */
    @Provide
    protected Arbitrary<List<String>> validTags() {
        return Arbitraries.strings()
                .ofMinLength(1)
                .ofMaxLength(20)
                .filter(s -> !s.isBlank())
                .list()
                .ofMaxSize(5);
    }

    /**
     * 生成有效的图片URL列表（0-9张）
     */
    @Provide
    protected Arbitrary<List<String>> validImageUrls() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(5)
                .ofMaxLength(50)
                .map(s -> "/files/" + s + ".jpg")
                .list()
                .ofMaxSize(9);
    }

    // ==================== 评论相关生成器 ====================

    /**
     * 生成有效的评论内容（1-500字符）
     */
    @Provide
    protected Arbitrary<String> validCommentContent() {
        return Arbitraries.strings()
                .ofMinLength(1)
                .ofMaxLength(500)
                .filter(s -> !s.isBlank());
    }

    // ==================== 私信相关生成器 ====================

    /**
     * 生成有效的消息内容（1-1000字符）
     */
    @Provide
    protected Arbitrary<String> validMessageContent() {
        return Arbitraries.strings()
                .ofMinLength(1)
                .ofMaxLength(1000)
                .filter(s -> !s.isBlank());
    }

    // ==================== ID 生成器 ====================

    /**
     * 生成有效的用户ID
     */
    @Provide
    protected Arbitrary<Long> validUserIds() {
        return Arbitraries.longs().between(1, 100000);
    }

    /**
     * 生成有效的帖子ID
     */
    @Provide
    protected Arbitrary<Long> validPostIds() {
        return Arbitraries.longs().between(1, 100000);
    }

    // ==================== 工具方法 ====================

    /**
     * 断言列表按降序排列
     */
    protected <T extends Comparable<T>> boolean isSortedDescending(List<T> list) {
        for (int i = 0; i < list.size() - 1; i++) {
            if (list.get(i).compareTo(list.get(i + 1)) < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * 断言列表按升序排列
     */
    protected <T extends Comparable<T>> boolean isSortedAscending(List<T> list) {
        for (int i = 0; i < list.size() - 1; i++) {
            if (list.get(i).compareTo(list.get(i + 1)) > 0) {
                return false;
            }
        }
        return true;
    }
}
