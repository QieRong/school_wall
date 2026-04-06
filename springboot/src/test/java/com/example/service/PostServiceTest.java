package com.example.service;

import cn.hutool.crypto.digest.BCrypt;
import com.example.base.BaseTest;
import com.example.entity.Post;
import com.example.entity.User;
import com.example.mapper.PostMapper;
import com.example.mapper.UserMapper;
import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 帖子服务单元测试
 * 
 * 测试范围：
 * - 文本帖子创建
 * - 图片上传
 * - 视频上传
 * - 分类选择
 * 
 * _Requirements: 2.1, 2.2, 2.3, 2.4_
 */
@DisplayName("帖子服务测试")
class PostServiceTest extends BaseTest {

  @Autowired
  private PostService postService;

  @Autowired
  private PostMapper postMapper;

  @Autowired
  private UserMapper userMapper;

  private User testUser;
  private final String TEST_ACCOUNT = "2021999101";

  @BeforeEach
  void setUp() {
    // 创建测试用户
    testUser = new User();
    testUser.setAccount(TEST_ACCOUNT);
    testUser.setPassword(BCrypt.hashpw("test123456"));
    testUser.setNickname("帖子测试用户");
    testUser.setRole(0);
    testUser.setStatus(1);
    testUser.setCreditScore(100);
    testUser.setViolationCount(0);
    testUser.setAvatar("/default.png");
    userMapper.insert(testUser);
  }

  @Nested
  @DisplayName("帖子创建测试")
  class CreatePostTests {

    @Test
    @DisplayName("创建文本帖子 - 应该成功创建并保存")
    void createTextPost_shouldSucceed() {
      // Given
      Post post = new Post();
      post.setUserId(testUser.getId());
      post.setContent("这是一条测试帖子内容");
      post.setCategory(1); // 表白分类
      post.setIsAnonymous(0);
      post.setVisibility(0); // 公开
      post.setStatus(1); // 已审核

      // When
      postService.createPost(post);

      // Then
      assertNotNull(post.getId(), "帖子ID不应为空");

      // 验证数据库中的数据
      Post savedPost = postMapper.selectById(post.getId(), testUser.getId());
      assertNotNull(savedPost, "帖子应该被保存到数据库");
      assertEquals("这是一条测试帖子内容", savedPost.getContent(), "内容应该匹配");
      assertEquals(testUser.getId(), savedPost.getUserId(), "用户ID应该匹配");
      assertEquals(1, savedPost.getCategory(), "分类应该匹配");
    }

    @Test
    @DisplayName("创建带图片的帖子 - 应该正确保存图片URL")
    void createPostWithImages_shouldSaveImageUrls() {
      // Given
      Post post = new Post();
      post.setUserId(testUser.getId());
      post.setContent("带图片的帖子");
      post.setCategory(2); // 寻物分类
      post.setImages("/files/img1.jpg,/files/img2.jpg,/files/img3.jpg");
      post.setIsAnonymous(0);
      post.setVisibility(0);
      post.setStatus(1);

      // When
      postService.createPost(post);

      // Then
      Post savedPost = postMapper.selectById(post.getId(), testUser.getId());
      assertNotNull(savedPost.getImages(), "图片字段不应为空");
      assertTrue(savedPost.getImages().contains("img1.jpg"), "应该包含第一张图片");
      assertTrue(savedPost.getImages().contains("img2.jpg"), "应该包含第二张图片");
      assertTrue(savedPost.getImages().contains("img3.jpg"), "应该包含第三张图片");
    }

    @Test
    @DisplayName("创建带视频的帖子 - 应该正确保存视频URL")
    void createPostWithVideo_shouldSaveVideoUrl() {
      // Given
      Post post = new Post();
      post.setUserId(testUser.getId());
      post.setContent("带视频的帖子");
      post.setCategory(3); // 闲置分类
      post.setVideo("/files/video1.mp4");
      post.setIsAnonymous(0);
      post.setVisibility(0);
      post.setStatus(1);

      // When
      postService.createPost(post);

      // Then
      Post savedPost = postMapper.selectById(post.getId(), testUser.getId());
      assertNotNull(savedPost.getVideo(), "视频字段不应为空");
      assertEquals("/files/video1.mp4", savedPost.getVideo(), "视频URL应该匹配");
    }

    @Test
    @DisplayName("创建不同分类的帖子 - 分类应该正确保存")
    void createPostWithDifferentCategories_shouldSaveCorrectCategory() {
      // 测试所有分类
      int[] categories = { 1, 2, 3, 4, 5, 6, 9 };
      String[] categoryNames = { "表白", "寻物", "闲置", "吐槽", "活动", "求助", "其他" };

      for (int i = 0; i < categories.length; i++) {
        // Given
        Post post = new Post();
        post.setUserId(testUser.getId());
        post.setContent("分类测试帖子 - " + categoryNames[i]);
        post.setCategory(categories[i]);
        post.setIsAnonymous(0);
        post.setVisibility(0);
        post.setStatus(1);

        // When
        postService.createPost(post);

        // Then
        Post savedPost = postMapper.selectById(post.getId(), testUser.getId());
        assertEquals(categories[i], savedPost.getCategory(),
            "分类 " + categoryNames[i] + " 应该正确保存");
      }
    }

    @Test
    @DisplayName("创建匿名帖子 - 匿名标志应该正确保存")
    void createAnonymousPost_shouldSaveAnonymousFlag() {
      // Given
      Post post = new Post();
      post.setUserId(testUser.getId());
      post.setContent("匿名帖子内容");
      post.setCategory(1);
      post.setIsAnonymous(1); // 匿名
      post.setVisibility(0);
      post.setStatus(1);

      // When
      postService.createPost(post);

      // Then
      Post savedPost = postMapper.selectById(post.getId(), testUser.getId());
      assertEquals(1, savedPost.getIsAnonymous(), "匿名标志应该为1");
    }

    @Test
    @DisplayName("创建带位置的帖子 - 位置信息应该正确保存")
    void createPostWithLocation_shouldSaveLocation() {
      // Given
      Post post = new Post();
      post.setUserId(testUser.getId());
      post.setContent("带位置的帖子");
      post.setCategory(5);
      post.setLocation("张家界学院图书馆");
      post.setIsAnonymous(0);
      post.setVisibility(0);
      post.setStatus(1);

      // When
      postService.createPost(post);

      // Then
      Post savedPost = postMapper.selectById(post.getId(), testUser.getId());
      assertEquals("张家界学院图书馆", savedPost.getLocation(), "位置应该正确保存");
    }

    @Test
    @DisplayName("创建带标签的帖子 - 标签应该正确保存")
    void createPostWithTags_shouldSaveTags() {
      // Given
      Post post = new Post();
      post.setUserId(testUser.getId());
      post.setContent("带标签的帖子 #校园生活 #表白");
      post.setCategory(1);
      post.setTags("校园生活,表白");
      post.setIsAnonymous(0);
      post.setVisibility(0);
      post.setStatus(1);

      // When
      postService.createPost(post);

      // Then
      Post savedPost = postMapper.selectById(post.getId(), testUser.getId());
      assertNotNull(savedPost.getTags(), "标签不应为空");
      assertTrue(savedPost.getTags().contains("校园生活"), "应该包含标签");
    }

    @Test
    @DisplayName("创建带投票的帖子 - 投票选项应该正确保存")
    void createPostWithPoll_shouldSavePollOptions() {
      // Given
      Post post = new Post();
      post.setUserId(testUser.getId());
      post.setContent("投票帖子：你喜欢什么颜色？");
      post.setCategory(4);
      post.setPollOptions("[\"红色\",\"蓝色\",\"绿色\"]");
      post.setPollEndTime(LocalDateTime.now().plusDays(7));
      post.setIsAnonymous(0);
      post.setVisibility(0);
      post.setStatus(1);

      // When
      postService.createPost(post);

      // Then
      Post savedPost = postMapper.selectById(post.getId(), testUser.getId());
      assertNotNull(savedPost.getPollOptions(), "投票选项不应为空");
      assertTrue(savedPost.getPollOptions().contains("红色"), "应该包含选项");
    }
  }

  @Nested
  @DisplayName("帖子可见性测试")
  class VisibilityTests {

    @Test
    @DisplayName("公开帖子 - 可见性应该为0")
    void publicPost_visibilityShouldBeZero() {
      // Given
      Post post = new Post();
      post.setUserId(testUser.getId());
      post.setContent("公开帖子");
      post.setCategory(1);
      post.setIsAnonymous(0);
      post.setVisibility(0); // 公开
      post.setStatus(1);

      // When
      postService.createPost(post);

      // Then
      Post savedPost = postMapper.selectById(post.getId(), testUser.getId());
      assertEquals(0, savedPost.getVisibility(), "公开帖子可见性应该为0");
    }

    @Test
    @DisplayName("仅互关可见帖子 - 可见性应该为1")
    void mutualFollowersOnlyPost_visibilityShouldBeOne() {
      // Given
      Post post = new Post();
      post.setUserId(testUser.getId());
      post.setContent("仅互关可见帖子");
      post.setCategory(1);
      post.setIsAnonymous(0);
      post.setVisibility(1); // 仅互关
      post.setStatus(1);

      // When
      postService.createPost(post);

      // Then
      Post savedPost = postMapper.selectById(post.getId(), testUser.getId());
      assertEquals(1, savedPost.getVisibility(), "仅互关可见帖子可见性应该为1");
    }

    @Test
    @DisplayName("仅自己可见帖子 - 可见性应该为2")
    void selfOnlyPost_visibilityShouldBeTwo() {
      // Given
      Post post = new Post();
      post.setUserId(testUser.getId());
      post.setContent("仅自己可见帖子");
      post.setCategory(1);
      post.setIsAnonymous(0);
      post.setVisibility(2); // 仅自己
      post.setStatus(1);

      // When
      postService.createPost(post);

      // Then
      Post savedPost = postMapper.selectById(post.getId(), testUser.getId());
      assertEquals(2, savedPost.getVisibility(), "仅自己可见帖子可见性应该为2");
    }
  }

  @Nested
  @DisplayName("帖子列表测试")
  class PostListTests {

    @BeforeEach
    void createTestPosts() {
      // 创建多个测试帖子
      for (int i = 1; i <= 10; i++) {
        Post post = new Post();
        post.setUserId(testUser.getId());
        post.setContent("测试帖子内容 " + i);
        post.setCategory((i % 5) + 1);
        post.setIsAnonymous(0);
        post.setVisibility(0);
        post.setStatus(1);
        post.setCreateTime(LocalDateTime.now().minusHours(i));
        postMapper.insert(post);
      }
    }

    @Test
    @DisplayName("获取帖子列表 - 应该返回分页数据")
    void getPostList_shouldReturnPaginatedData() {
      // When
      PageInfo<Post> pageInfo = postService.getPostList(1, 5, testUser.getId(), 0L, 0, 0, null, false);

      // Then
      assertNotNull(pageInfo, "分页信息不应为空");
      assertTrue(pageInfo.getList().size() <= 5, "每页数量不应超过5");
      assertTrue(pageInfo.getTotal() >= 10, "总数应该至少为10");
    }

    @Test
    @DisplayName("按分类筛选帖子 - 应该只返回指定分类")
    void getPostListByCategory_shouldReturnFilteredPosts() {
      // When
      PageInfo<Post> pageInfo = postService.getPostList(1, 10, testUser.getId(), 0L, 0, 1, null, false);

      // Then
      assertNotNull(pageInfo);
      for (Post post : pageInfo.getList()) {
        assertEquals(1, post.getCategory(), "所有帖子分类应该为1");
      }
    }

    @Test
    @DisplayName("搜索帖子 - 应该返回匹配的帖子")
    void searchPosts_shouldReturnMatchingPosts() {
      // When
      PageInfo<Post> pageInfo = postService.getPostList(1, 10, testUser.getId(), 0L, 0, 0, "测试帖子", false);

      // Then
      assertNotNull(pageInfo);
      assertTrue(pageInfo.getTotal() > 0, "应该找到匹配的帖子");
    }
  }

  @Nested
  @DisplayName("热帖测试")
  class HotPostsTests {

    @Test
    @DisplayName("获取热帖 - 应该返回热门帖子列表")
    void getHotPosts_shouldReturnHotPostsList() {
      // 先创建一些帖子
      for (int i = 0; i < 5; i++) {
        Post post = new Post();
        post.setUserId(testUser.getId());
        post.setContent("热帖测试 " + i);
        post.setCategory(1);
        post.setIsAnonymous(0);
        post.setVisibility(0);
        post.setStatus(1);
        post.setLikeCount(100 - i * 10);
        post.setViewCount(1000 - i * 100);
        postMapper.insert(post);
      }

      // When
      List<Post> hotPosts = postService.getHotPosts();

      // Then
      assertNotNull(hotPosts, "热帖列表不应为空");
    }
  }

  @Nested
  @DisplayName("定时发布测试")
  class ScheduledPostTests {

    @Test
    @DisplayName("创建定时发布帖子 - 状态应该为待发布")
    void createScheduledPost_statusShouldBePending() {
      // Given
      Post post = new Post();
      post.setUserId(testUser.getId());
      post.setContent("定时发布的帖子");
      post.setCategory(1);
      post.setIsAnonymous(0);
      post.setVisibility(0);
      post.setScheduledTime(LocalDateTime.now().plusDays(1)); // 明天发布

      // When
      postService.createPost(post);

      // Then
      Post savedPost = postMapper.selectById(post.getId(), testUser.getId());
      assertEquals(2, savedPost.getStatus(), "定时发布帖子状态应该为2(待发布)");
      assertNotNull(savedPost.getScheduledTime(), "定时发布时间不应为空");
    }

    @Test
    @DisplayName("获取用户定时发布帖子列表")
    void getScheduledPosts_shouldReturnUserScheduledPosts() {
      // Given - 创建定时发布帖子
      Post post = new Post();
      post.setUserId(testUser.getId());
      post.setContent("定时帖子");
      post.setCategory(1);
      post.setIsAnonymous(0);
      post.setVisibility(0);
      post.setScheduledTime(LocalDateTime.now().plusDays(1));
      postService.createPost(post);

      // When
      List<Post> scheduledPosts = postService.getScheduledPosts(testUser.getId());

      // Then
      assertNotNull(scheduledPosts, "定时帖子列表不应为空");
    }
  }

  @Nested
  @DisplayName("投票帖子测试")
  class PollPostTests {

    @Test
    @DisplayName("创建投票帖子 - 投票选项和截止时间应该正确保存")
    void createPollPost_shouldSavePollOptionsAndEndTime() {
      // Given
      Post post = new Post();
      post.setUserId(testUser.getId());
      post.setContent("投票：你最喜欢的编程语言？");
      post.setCategory(4);
      post.setPollOptions("[\"Java\",\"Python\",\"JavaScript\",\"Go\"]");
      post.setPollEndTime(LocalDateTime.now().plusDays(3));
      post.setIsAnonymous(0);
      post.setVisibility(0);

      // When
      postService.createPost(post);

      // Then
      Post savedPost = postMapper.selectById(post.getId(), testUser.getId());
      assertNotNull(savedPost.getPollOptions(), "投票选项不应为空");
      assertTrue(savedPost.getPollOptions().contains("Java"), "应该包含Java选项");
      assertTrue(savedPost.getPollOptions().contains("Python"), "应该包含Python选项");
      assertNotNull(savedPost.getPollEndTime(), "投票截止时间不应为空");
    }

    @Test
    @DisplayName("获取用户投票帖子列表")
    void getMyPolls_shouldReturnUserPollPosts() {
      // Given
      Post post = new Post();
      post.setUserId(testUser.getId());
      post.setContent("投票帖子");
      post.setCategory(4);
      post.setPollOptions("[\"选项A\",\"选项B\"]");
      post.setIsAnonymous(0);
      post.setVisibility(0);
      postService.createPost(post);

      // When
      List<Post> polls = postService.getMyPolls(testUser.getId());

      // Then
      assertNotNull(polls, "投票帖子列表不应为空");
      assertTrue(polls.size() > 0, "应该至少有一个投票帖子");
    }
  }

  @Nested
  @DisplayName("信誉分限制测试")
  class CreditScoreTests {

    @Test
    @DisplayName("低信誉分用户发帖 - 应该被拒绝")
    void lowCreditScoreUser_shouldBeRejected() {
      // Given - 创建低信誉分用户
      User lowCreditUser = new User();
      lowCreditUser.setAccount("2021999102");
      lowCreditUser.setPassword("test123");
      lowCreditUser.setNickname("低信誉用户");
      lowCreditUser.setRole(0);
      lowCreditUser.setStatus(1);
      lowCreditUser.setCreditScore(50); // 低于60分
      lowCreditUser.setViolationCount(5);
      userMapper.insert(lowCreditUser);

      Post post = new Post();
      post.setUserId(lowCreditUser.getId());
      post.setContent("低信誉用户的帖子");
      post.setCategory(1);
      post.setIsAnonymous(0);
      post.setVisibility(0);

      // When & Then
      RuntimeException exception = assertThrows(RuntimeException.class, () -> {
        postService.createPost(post);
      });
      assertTrue(exception.getMessage().contains("信誉分"), "应该提示信誉分不足");
    }

    @Test
    @DisplayName("正常信誉分用户发帖 - 应该成功")
    void normalCreditScoreUser_shouldSucceed() {
      // Given - testUser 信誉分为100
      Post post = new Post();
      post.setUserId(testUser.getId());
      post.setContent("正常用户的帖子");
      post.setCategory(1);
      post.setIsAnonymous(0);
      post.setVisibility(0);

      // When
      postService.createPost(post);

      // Then
      assertNotNull(post.getId(), "帖子应该创建成功");
    }
  }

  @Nested
  @DisplayName("帖子初始化测试")
  class PostInitializationTests {

    @Test
    @DisplayName("新帖子计数器应该初始化为0")
    void newPost_countersShouldBeZero() {
      // Given
      Post post = new Post();
      post.setUserId(testUser.getId());
      post.setContent("测试计数器初始化");
      post.setCategory(1);
      post.setIsAnonymous(0);
      post.setVisibility(0);

      // When
      postService.createPost(post);

      // Then
      Post savedPost = postMapper.selectById(post.getId(), testUser.getId());
      assertEquals(0, savedPost.getViewCount(), "浏览数应该为0");
      assertEquals(0, savedPost.getLikeCount(), "点赞数应该为0");
      assertEquals(0, savedPost.getCommentCount(), "评论数应该为0");
      assertEquals(0, savedPost.getShareCount(), "分享数应该为0");
    }

    @Test
    @DisplayName("非定时帖子状态应该为已发布")
    void nonScheduledPost_statusShouldBePublished() {
      // Given
      Post post = new Post();
      post.setUserId(testUser.getId());
      post.setContent("立即发布的帖子");
      post.setCategory(1);
      post.setIsAnonymous(0);
      post.setVisibility(0);
      // 不设置 scheduledTime

      // When
      postService.createPost(post);

      // Then
      Post savedPost = postMapper.selectById(post.getId(), testUser.getId());
      assertEquals(1, savedPost.getStatus(), "非定时帖子状态应该为1(已发布)");
    }
  }

  @Nested
  @DisplayName("图片格式解析测试")
  class ImageParsingTests {

    @Test
    @DisplayName("JSON格式图片列表 - 应该正确解析")
    void jsonFormatImages_shouldParseCorrectly() {
      // Given
      Post post = new Post();
      post.setUserId(testUser.getId());
      post.setContent("JSON格式图片帖子");
      post.setCategory(1);
      post.setImages("[\"/files/a.jpg\",\"/files/b.jpg\"]");
      post.setIsAnonymous(0);
      post.setVisibility(0);

      // When
      postService.createPost(post);
      Post detail = postService.getPostDetail(post.getId(), testUser.getId(), false);

      // Then
      assertNotNull(detail.getImgList(), "图片列表不应为空");
      assertEquals(2, detail.getImgList().size(), "应该有2张图片");
    }

    @Test
    @DisplayName("逗号分隔格式图片列表 - 应该正确解析")
    void commaFormatImages_shouldParseCorrectly() {
      // Given
      Post post = new Post();
      post.setUserId(testUser.getId());
      post.setContent("逗号分隔图片帖子");
      post.setCategory(1);
      post.setImages("/files/x.jpg,/files/y.jpg,/files/z.jpg");
      post.setIsAnonymous(0);
      post.setVisibility(0);

      // When
      postService.createPost(post);
      Post detail = postService.getPostDetail(post.getId(), testUser.getId(), false);

      // Then
      assertNotNull(detail.getImgList(), "图片列表不应为空");
      assertEquals(3, detail.getImgList().size(), "应该有3张图片");
    }
  }

  @Nested
  @DisplayName("匿名帖子处理测试")
  class AnonymousPostProcessingTests {

    @Test
    @DisplayName("匿名帖子 - 其他用户查看时应该隐藏身份")
    void anonymousPost_otherUserShouldSeeHiddenIdentity() {
      // Given - 创建另一个用户
      User otherUser = new User();
      otherUser.setAccount("2021999103");
      otherUser.setPassword("test123");
      otherUser.setNickname("其他用户");
      otherUser.setRole(0);
      otherUser.setStatus(1);
      otherUser.setCreditScore(100);
      userMapper.insert(otherUser);

      // 创建匿名帖子
      Post post = new Post();
      post.setUserId(testUser.getId());
      post.setContent("匿名帖子内容");
      post.setCategory(1);
      post.setIsAnonymous(1);
      post.setVisibility(0);
      postService.createPost(post);

      // When - 其他用户查看
      Post detail = postService.getPostDetail(post.getId(), otherUser.getId(), false);

      // Then
      assertEquals("匿名同学", detail.getNickname(), "其他用户看到的昵称应该是匿名同学");
      assertEquals("/default.png", detail.getAvatar(), "其他用户看到的头像应该是默认头像");
    }

    @Test
    @DisplayName("匿名帖子 - 作者自己查看时应该显示真实身份")
    void anonymousPost_authorShouldSeeRealIdentity() {
      // Given
      Post post = new Post();
      post.setUserId(testUser.getId());
      post.setContent("匿名帖子内容");
      post.setCategory(1);
      post.setIsAnonymous(1);
      post.setVisibility(0);
      postService.createPost(post);

      // When - 作者自己查看
      Post detail = postService.getPostDetail(post.getId(), testUser.getId(), false);

      // Then - 作者应该能看到自己的真实信息
      assertNotEquals("匿名同学", detail.getNickname(), "作者应该能看到真实昵称");
    }
  }
}
