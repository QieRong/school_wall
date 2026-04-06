/**
 * fast-check 属性测试生成器
 * 提供通用的数据生成器用于属性测试
 */

import fc from 'fast-check'

// ==================== 用户相关生成器 ====================

/**
 * 生成有效的用户账号（学号格式，10位数字）
 */
export const validAccount = fc.stringOf(fc.constantFrom('0', '1', '2', '3', '4', '5', '6', '7', '8', '9'), {
  minLength: 10,
  maxLength: 10
})

/**
 * 生成有效的密码（6-20位字母数字）
 */
export const validPassword = fc.string({ minLength: 6, maxLength: 20 })
  .filter(s => /^[a-zA-Z0-9]+$/.test(s))

/**
 * 生成有效的昵称（1-20位非空字符）
 */
export const validNickname = fc.string({ minLength: 1, maxLength: 20 })
  .filter(s => s.trim().length > 0)

/**
 * 生成测试用户对象
 */
export const testUser = fc.record({
  id: fc.integer({ min: 1, max: 100000 }),
  account: validAccount,
  nickname: validNickname,
  avatar: fc.constant('/default.png'),
  role: fc.constantFrom(0, 1),
  status: fc.constantFrom(0, 1),
  creditScore: fc.integer({ min: 0, max: 100 }),
  followerCount: fc.integer({ min: 0, max: 10000 }),
  followingCount: fc.integer({ min: 0, max: 10000 })
})

// ==================== 帖子相关生成器 ====================

/**
 * 生成有效的帖子内容（1-2000字符）
 */
export const validPostContent = fc.string({ minLength: 1, maxLength: 2000 })
  .filter(s => s.trim().length > 0)

/**
 * 生成有效的帖子分类（1-5）
 */
export const validCategory = fc.integer({ min: 1, max: 5 })

/**
 * 生成有效的可见性设置（0-2）
 */
export const validVisibility = fc.integer({ min: 0, max: 2 })

/**
 * 生成有效的标签列表（0-5个标签）
 */
export const validTags = fc.array(
  fc.string({ minLength: 1, maxLength: 20 }).filter(s => s.trim().length > 0),
  { maxLength: 5 }
)

/**
 * 生成有效的图片URL列表（0-9张）
 */
export const validImageUrls = fc.array(
  fc.string({ minLength: 5, maxLength: 50 }).map(s => `/files/${s}.jpg`),
  { maxLength: 9 }
)

/**
 * 生成测试帖子对象
 */
export const testPost = fc.record({
  id: fc.integer({ min: 1, max: 100000 }),
  userId: fc.integer({ min: 1, max: 100000 }),
  content: validPostContent,
  images: validImageUrls,
  category: validCategory,
  isAnonymous: fc.constantFrom(0, 1),
  visibility: validVisibility,
  likeCount: fc.integer({ min: 0, max: 100000 }),
  commentCount: fc.integer({ min: 0, max: 10000 }),
  shareCount: fc.integer({ min: 0, max: 10000 }),
  viewCount: fc.integer({ min: 0, max: 1000000 }),
  status: fc.constantFrom(0, 1, 2),
  tags: validTags
})

// ==================== 评论相关生成器 ====================

/**
 * 生成有效的评论内容（1-500字符）
 */
export const validCommentContent = fc.string({ minLength: 1, maxLength: 500 })
  .filter(s => s.trim().length > 0)

/**
 * 生成测试评论对象
 */
export const testComment = fc.record({
  id: fc.integer({ min: 1, max: 100000 }),
  postId: fc.integer({ min: 1, max: 100000 }),
  userId: fc.integer({ min: 1, max: 100000 }),
  content: validCommentContent,
  parentId: fc.option(fc.integer({ min: 1, max: 100000 }), { nil: null }),
  likeCount: fc.integer({ min: 0, max: 10000 })
})

// ==================== 私信相关生成器 ====================

/**
 * 生成有效的消息内容（1-1000字符）
 */
export const validMessageContent = fc.string({ minLength: 1, maxLength: 1000 })
  .filter(s => s.trim().length > 0)

/**
 * 生成测试消息对象
 */
export const testMessage = fc.record({
  id: fc.integer({ min: 1, max: 100000 }),
  senderId: fc.integer({ min: 1, max: 100000 }),
  receiverId: fc.integer({ min: 1, max: 100000 }),
  content: validMessageContent,
  type: fc.constantFrom(0, 1, 2, 3),
  isRead: fc.constantFrom(0, 1),
  isWithdrawn: fc.constantFrom(0, 1)
})

// ==================== 安全相关生成器 ====================

/**
 * 生成 XSS 攻击字符串
 */
export const xssStrings = fc.constantFrom(
  '<script>alert("xss")</script>',
  '<img src="x" onerror="alert(1)">',
  '<svg onload="alert(1)">',
  'javascript:alert(1)',
  '<iframe src="javascript:alert(1)">',
  '<body onload="alert(1)">',
  '"><script>alert(1)</script>',
  "'-alert(1)-'",
  '<a href="javascript:alert(1)">click</a>'
)

/**
 * 生成 SQL 注入字符串
 */
export const sqlInjectionStrings = fc.constantFrom(
  "' OR '1'='1",
  "'; DROP TABLE users; --",
  "1; DELETE FROM posts",
  "' UNION SELECT * FROM users --",
  "admin'--",
  "1' AND '1'='1",
  "'; INSERT INTO users VALUES('hacker', 'password'); --"
)

// ==================== 工具函数 ====================

/**
 * 运行属性测试（默认100次迭代）
 * @param {Function} property - 属性函数
 * @param {Object} options - fast-check 选项
 */
export function runProperty(property, options = {}) {
  return fc.assert(property, {
    numRuns: 100,
    ...options
  })
}

/**
 * 检查列表是否按降序排列
 * @param {Array} list - 要检查的列表
 * @param {Function} keyFn - 获取比较键的函数
 * @returns {boolean}
 */
export function isSortedDescending(list, keyFn = x => x) {
  for (let i = 0; i < list.length - 1; i++) {
    if (keyFn(list[i]) < keyFn(list[i + 1])) {
      return false
    }
  }
  return true
}

/**
 * 检查列表是否按升序排列
 * @param {Array} list - 要检查的列表
 * @param {Function} keyFn - 获取比较键的函数
 * @returns {boolean}
 */
export function isSortedAscending(list, keyFn = x => x) {
  for (let i = 0; i < list.length - 1; i++) {
    if (keyFn(list[i]) > keyFn(list[i + 1])) {
      return false
    }
  }
  return true
}
