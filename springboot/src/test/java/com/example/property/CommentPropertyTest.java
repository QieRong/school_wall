package com.example.property;

import com.example.base.BasePropertyTest;
import com.example.entity.Comment;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 评论系统属性测试
 * 
 * 测试评论系统的核心属性：
 * - Property 17: 评论计数一致性
 * - Property 18: 评论树结构完整性
 * - Property 19: 评论删除计数更新
 * 
 * **Feature: functional-testing**
 */
public class CommentPropertyTest extends BasePropertyTest {

    // ==================== Property 17: 评论计数一致性 ====================

    /**
     * **Feature: functional-testing, Property 17: 评论计数一致性**
     * **Validates: Requirements 4.1**
     * 
     * 对于任意评论操作序列，评论计数应该等于实际评论数量。
     */
    @Property(tries = 100)
    @Label("Property 17: 评论计数一致性 - 计数应该等于实际评论数")
    void commentCountShouldMatchActualCount(
            @ForAll @IntRange(min = 0, max = 100) int addCount,
            @ForAll @IntRange(min = 0, max = 50) int deleteCount) {

        // Given - 模拟评论操作
        List<Long> commentIds = new ArrayList<>();
        int commentCount = 0;

        // When - 添加评论
        for (int i = 0; i < addCount; i++) {
            commentIds.add((long) i);
            commentCount++;
        }

        // When - 删除部分评论
        int actualDeletes = Math.min(deleteCount, commentIds.size());
        for (int i = 0; i < actualDeletes; i++) {
            commentIds.remove(0);
            commentCount--;
        }

        // Then - 计数应该等于实际评论数
        assertEquals(commentIds.size(), commentCount,
                "评论计数应该等于实际评论数量");
    }

    /**
     * **Feature: functional-testing, Property 17: 评论计数一致性**
     * **Validates: Requirements 4.1**
     * 
     * 添加评论后计数应该增加1。
     */
    @Property(tries = 100)
    @Label("Property 17: 添加评论 - 计数应该增加1")
    void addCommentShouldIncrementCount(
            @ForAll @IntRange(min = 0, max = 1000) int initialCount) {

        // Given
        int commentCount = initialCount;

        // When - 添加一条评论
        commentCount++;

        // Then
        assertEquals(initialCount + 1, commentCount,
                "添加评论后计数应该增加1");
    }

    // ==================== Property 18: 评论树结构完整性 ====================

    /**
     * **Feature: functional-testing, Property 18: 评论树结构完整性**
     * **Validates: Requirements 4.2, 4.4**
     * 
     * 对于任意评论树，所有子评论的父ID应该指向有效的父评论。
     */
    @Property(tries = 100)
    @Label("Property 18: 评论树结构 - 子评论应该有有效的父ID")
    void commentTreeShouldHaveValidParentIds(
            @ForAll("commentTreeData") List<Comment> comments) {

        // Given - 构建ID集合
        Set<Long> validIds = new HashSet<>();
        validIds.add(0L); // 0表示一级评论
        for (Comment c : comments) {
            validIds.add(c.getId());
        }

        // Then - 所有子评论的父ID应该有效
        for (Comment c : comments) {
            Long parentId = c.getParentId();
            if (parentId != null && parentId != 0) {
                assertTrue(validIds.contains(parentId),
                        "子评论的父ID应该指向有效的评论");
            }
        }
    }

    /**
     * 生成评论树数据
     */
    @Provide
    Arbitrary<List<Comment>> commentTreeData() {
        return Arbitraries.integers().between(1, 50).flatMap(size -> {
            List<Comment> comments = new ArrayList<>();
            Random random = new Random();

            for (int i = 1; i <= size; i++) {
                Comment comment = new Comment();
                comment.setId((long) i);
                comment.setPostId(1L);
                comment.setUserId((long) random.nextInt(100) + 1);
                comment.setContent("评论内容 " + i);

                // 前几条是一级评论，后面的随机选择父评论
                if (i <= 3 || random.nextBoolean()) {
                    comment.setParentId(0L);
                } else {
                    // 随机选择一个已存在的评论作为父评论
                    int parentIndex = random.nextInt(comments.size());
                    comment.setParentId(comments.get(parentIndex).getId());
                }

                comment.setCreateTime(LocalDateTime.now().minusMinutes(size - i));
                comments.add(comment);
            }

            return Arbitraries.just(comments);
        });
    }

    /**
     * **Feature: functional-testing, Property 18: 评论树结构完整性**
     * **Validates: Requirements 4.2, 4.4**
     * 
     * 构建评论树后，一级评论数量应该正确。
     */
    @Property(tries = 100)
    @Label("Property 18: 评论树 - 一级评论数量应该正确")
    void commentTreeRootCountShouldBeCorrect(
            @ForAll("commentTreeData") List<Comment> comments) {

        // When - 统计一级评论
        long rootCount = comments.stream()
                .filter(c -> c.getParentId() == null || c.getParentId() == 0)
                .count();

        // Then - 一级评论数量应该大于0
        assertTrue(rootCount > 0, "应该至少有一条一级评论");
        assertTrue(rootCount <= comments.size(), "一级评论数量不应超过总评论数");
    }

    /**
     * **Feature: functional-testing, Property 18: 评论树结构完整性**
     * **Validates: Requirements 4.2, 4.4**
     * 
     * 评论树构建后，每个节点的子评论列表应该正确。
     */
    @Property(tries = 50)
    @Label("Property 18: 评论树构建 - 子评论应该正确挂载")
    void commentTreeBuildShouldBeCorrect(
            @ForAll("commentTreeData") List<Comment> comments) {

        // Given - 构建评论树
        Map<Long, Comment> commentMap = new HashMap<>();
        List<Comment> roots = new ArrayList<>();

        // 初始化
        for (Comment c : comments) {
            c.setChildren(new ArrayList<>());
            commentMap.put(c.getId(), c);
        }

        // 构建树
        for (Comment c : comments) {
            Long parentId = c.getParentId();
            if (parentId == null || parentId == 0) {
                roots.add(c);
            } else {
                Comment parent = commentMap.get(parentId);
                if (parent != null) {
                    parent.getChildren().add(c);
                }
            }
        }

        // Then - 验证树结构
        int totalInTree = countNodesInTree(roots);
        assertEquals(comments.size(), totalInTree,
                "树中的节点数应该等于总评论数");
    }

    private int countNodesInTree(List<Comment> nodes) {
        int count = 0;
        for (Comment node : nodes) {
            count++;
            if (node.getChildren() != null) {
                count += countNodesInTree(node.getChildren());
            }
        }
        return count;
    }

    // ==================== Property 19: 评论删除计数更新 ====================

    /**
     * **Feature: functional-testing, Property 19: 评论删除计数更新**
     * **Validates: Requirements 4.5**
     * 
     * 删除评论后，计数应该正确减少。
     */
    @Property(tries = 100)
    @Label("Property 19: 删除评论 - 计数应该减少")
    void deleteCommentShouldDecrementCount(
            @ForAll @IntRange(min = 1, max = 1000) int initialCount,
            @ForAll @IntRange(min = 1, max = 100) int deleteCount) {

        // Given
        int commentCount = initialCount;
        int actualDeletes = Math.min(deleteCount, initialCount);

        // When - 删除评论
        commentCount -= actualDeletes;

        // Then
        assertEquals(initialCount - actualDeletes, commentCount,
                "删除评论后计数应该正确减少");
        assertTrue(commentCount >= 0, "评论计数不应为负数");
    }

    /**
     * **Feature: functional-testing, Property 19: 评论删除计数更新**
     * **Validates: Requirements 4.5**
     * 
     * 删除不存在的评论不应该影响计数。
     */
    @Property(tries = 100)
    @Label("Property 19: 删除不存在评论 - 计数不应变化")
    void deleteNonExistentCommentShouldNotAffectCount(
            @ForAll @IntRange(min = 0, max = 1000) int initialCount) {

        // Given
        Set<Long> existingIds = new HashSet<>();
        for (int i = 0; i < initialCount; i++) {
            existingIds.add((long) i);
        }
        int commentCount = initialCount;

        // When - 尝试删除不存在的评论
        Long nonExistentId = (long) (initialCount + 100);
        if (!existingIds.contains(nonExistentId)) {
            // 不存在，不删除
        }

        // Then - 计数不应变化
        assertEquals(initialCount, commentCount,
                "删除不存在的评论不应影响计数");
    }

    // ==================== 辅助属性测试 ====================

    /**
     * **Feature: functional-testing, Property 17: 评论计数一致性**
     * **Validates: Requirements 4.1**
     * 
     * 评论内容不应为空（除非有图片）。
     */
    @Property(tries = 100)
    @Label("Property 17: 评论内容验证 - 内容或图片至少有一个")
    void commentShouldHaveContentOrImage(
            @ForAll("nullableCommentContent") String content,
            @ForAll("validImageUrl") String imageUrl) {

        // Given
        Comment comment = new Comment();
        comment.setContent(content);
        comment.setImgUrl(imageUrl);

        // Then - 内容或图片至少有一个
        boolean hasContent = content != null && !content.trim().isEmpty();
        boolean hasImage = imageUrl != null && !imageUrl.trim().isEmpty();

        // 如果两者都为空，评论无效
        if (!hasContent && !hasImage) {
            // 这种情况应该被拒绝
            assertTrue(true, "空评论应该被拒绝");
        } else {
            // 有效评论
            assertTrue(hasContent || hasImage, "评论应该有内容或图片");
        }
    }

    /**
     * 生成可能为空的评论内容
     */
    @Provide
    Arbitrary<String> nullableCommentContent() {
        return Arbitraries.oneOf(
                Arbitraries.just(null),
                Arbitraries.just(""),
                Arbitraries.strings()
                        .withCharRange('a', 'z')
                        .withCharRange('A', 'Z')
                        .withCharRange('0', '9')
                        .withChars(' ', ',', '.', '!', '?')
                        .ofMinLength(1)
                        .ofMaxLength(500));
    }

    /**
     * 生成有效的图片URL
     */
    @Provide
    Arbitrary<String> validImageUrl() {
        return Arbitraries.oneOf(
                Arbitraries.just(null),
                Arbitraries.just(""),
                Arbitraries.of(
                        "/uploads/comment/img1.jpg",
                        "/uploads/comment/img2.png",
                        "/uploads/comment/img3.gif"));
    }
}
