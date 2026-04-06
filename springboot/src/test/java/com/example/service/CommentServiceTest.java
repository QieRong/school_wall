package com.example.service;

import cn.hutool.crypto.digest.BCrypt;
import com.example.base.BaseTest;
import com.example.entity.Comment;
import com.example.entity.Post;
import com.example.entity.User;
import com.example.mapper.CommentMapper;
import com.example.mapper.PostMapper;
import com.example.mapper.UserMapper;
import com.example.service.impl.PostServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 评论功能单元测试
 * _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_
 */
@DisplayName("评论功能测试")
class CommentServiceTest extends BaseTest {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PostServiceImpl postService;

    private User testUser;
    private User otherUser;
    private Post testPost;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setAccount("2021666001");
        testUser.setPassword(BCrypt.hashpw("test123456"));
        testUser.setNickname("评论测试用户");
        testUser.setRole(0);
        testUser.setStatus(1);
        testUser.setCreditScore(100);
        testUser.setViolationCount(0);
        testUser.setAvatar("/default.png");
        userMapper.insert(testUser);

        otherUser = new User();
        otherUser.setAccount("2021666002");
        otherUser.setPassword(BCrypt.hashpw("test123456"));
        otherUser.setNickname("其他用户");
        otherUser.setRole(0);
        otherUser.setStatus(1);
        otherUser.setCreditScore(100);
        userMapper.insert(otherUser);

        testPost = new Post();
        testPost.setUserId(testUser.getId());
        testPost.setContent("测试帖子内容");
        testPost.setCategory(1);
        testPost.setIsAnonymous(0);
        testPost.setVisibility(0);
        testPost.setForceSubmit(true);
        postService.createPost(testPost);
    }

    @Nested
    @DisplayName("发表评论测试")
    class CreateCommentTests {

        @Test
        @DisplayName("发表文本评论 - 应该成功创建")
        void createTextComment_shouldSucceed() {
            Comment comment = new Comment();
            comment.setPostId(testPost.getId());
            comment.setUserId(otherUser.getId().longValue());
            comment.setContent("这是一条测试评论");
            comment.setParentId(0L);

            int result = commentMapper.insert(comment);
            assertEquals(1, result, "应该成功插入一条评论");

            List<Comment> comments = commentMapper.selectByPostId(testPost.getId());
            assertTrue(comments.stream().anyMatch(c -> "这是一条测试评论".equals(c.getContent())), "评论应该被保存");
        }

        @Test
        @DisplayName("发表图片评论 - 应该成功创建")
        void createImageComment_shouldSucceed() {
            Comment comment = new Comment();
            comment.setPostId(testPost.getId());
            comment.setUserId(otherUser.getId().longValue());
            comment.setContent("带图片的评论");
            comment.setImgUrl("/uploads/comment/test.jpg");
            comment.setParentId(0L);

            commentMapper.insert(comment);

            List<Comment> comments = commentMapper.selectByPostId(testPost.getId());
            assertTrue(comments.stream().anyMatch(c -> "/uploads/comment/test.jpg".equals(c.getImgUrl())), "图片评论应该被保存");
        }
    }

    @Nested
    @DisplayName("回复评论测试")
    class ReplyCommentTests {

        @Test
        @DisplayName("回复评论 - 应该正确设置父评论ID")
        void replyComment_shouldSetParentId() {
            // 创建父评论
            Comment parent = new Comment();
            parent.setPostId(testPost.getId());
            parent.setUserId(testUser.getId().longValue());
            parent.setContent("父评论");
            parent.setParentId(0L);
            commentMapper.insert(parent);

            List<Comment> comments = commentMapper.selectByPostId(testPost.getId());
            Long parentId = comments.get(0).getId();

            // 创建回复
            Comment reply = new Comment();
            reply.setPostId(testPost.getId());
            reply.setUserId(otherUser.getId().longValue());
            reply.setContent("回复评论");
            reply.setParentId(parentId);
            reply.setReplyUserId(testUser.getId().longValue());
            commentMapper.insert(reply);

            List<Comment> allComments = commentMapper.selectByPostId(testPost.getId());
            assertEquals(2, allComments.size(), "应该有2条评论");
        }
    }

    @Nested
    @DisplayName("评论树结构测试")
    class CommentTreeTests {

        @Test
        @DisplayName("构建评论树 - 应该正确组织层级")
        void buildCommentTree_shouldOrganizeHierarchy() {
            Comment root1 = new Comment();
            root1.setPostId(testPost.getId());
            root1.setUserId(testUser.getId().longValue());
            root1.setContent("一级评论1");
            root1.setParentId(0L);
            commentMapper.insert(root1);

            Comment root2 = new Comment();
            root2.setPostId(testPost.getId());
            root2.setUserId(testUser.getId().longValue());
            root2.setContent("一级评论2");
            root2.setParentId(0L);
            commentMapper.insert(root2);

            List<Comment> allComments = commentMapper.selectByPostId(testPost.getId());
            assertEquals(2, allComments.size(), "应该有2条评论");

            long rootCount = allComments.stream()
                    .filter(c -> c.getParentId() == null || c.getParentId() == 0)
                    .count();
            assertEquals(2, rootCount, "应该有2条一级评论");
        }

        @Test
        @DisplayName("评论计数 - 应该正确统计")
        void commentCount_shouldBeCorrect() {
            for (int i = 0; i < 5; i++) {
                Comment comment = new Comment();
                comment.setPostId(testPost.getId());
                comment.setUserId(otherUser.getId().longValue());
                comment.setContent("评论 " + i);
                comment.setParentId(0L);
                commentMapper.insert(comment);
                postMapper.incrementComment(testPost.getId());
            }

            Post post = postMapper.selectById(testPost.getId(), testUser.getId());
            assertEquals(5, post.getCommentCount(), "评论数应该为5");
        }
    }

    @Nested
    @DisplayName("评论点赞测试")
    class CommentLikeTests {

        @Test
        @DisplayName("评论点赞 - 应该增加点赞数")
        void likeComment_shouldIncrementLikeCount() {
            Comment comment = new Comment();
            comment.setPostId(testPost.getId());
            comment.setUserId(otherUser.getId().longValue());
            comment.setContent("待点赞的评论");
            comment.setParentId(0L);
            commentMapper.insert(comment);

            List<Comment> comments = commentMapper.selectByPostId(testPost.getId());
            Comment saved = comments.get(0);
            int initialLikes = saved.getLikeCount() != null ? saved.getLikeCount() : 0;

            commentMapper.incrementLike(saved.getId());

            List<Comment> updated = commentMapper.selectByPostId(testPost.getId());
            Comment updatedComment = updated.get(0);
            assertEquals(initialLikes + 1, updatedComment.getLikeCount(), "点赞数应该增加1");
        }
    }
}
