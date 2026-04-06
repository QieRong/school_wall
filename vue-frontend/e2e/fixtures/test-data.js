/**
 * 测试数据
 * 用于 E2E 测试的固定数据和生成器
 */

// 测试用户
export const testUsers = {
  admin: {
    account: 'admin',
    password: '123456',
    role: 'admin'
  },
  normalUser: {
    account: '2212040241',
    password: '123456',
    role: 'user'
  },
  normalUser2: {
    account: '2212040201',
    password: '123456',
    role: 'user'
  }
}

// 兼容旧的导出名称
export const TEST_USERS = {
  admin: {
    account: 'admin',
    password: '123456',
    role: 'admin'
  },
  normal: {
    account: '2212040241',
    password: '123456',
    role: 'user'
  },
  normal2: {
    account: '2212040201',
    password: '123456',
    role: 'user'
  }
}

// 生成唯一的测试用户
export function generateTestUser() {
  const timestamp = Date.now().toString().slice(-6) // 取后6位时间戳
  const randomDigits = Math.floor(Math.random() * 1000).toString().padStart(3, '0') // 3位随机数
  return {
    account: `22120${timestamp}${randomDigits}`.slice(0, 10), // 确保是10位学号
    nickname: `测试用户${timestamp}`,
    password: '123456'
  }
}

// 生成测试帖子内容
export function generatePostContent() {
  const timestamp = Date.now()
  return {
    content: `这是一条测试帖子 ${timestamp}\n测试内容包含多行文本\n用于验证发帖功能`,
    category: '表白',
    tags: ['测试', '自动化']
  }
}

// 生成漂流瓶内容
export function generateBottleContent() {
  const timestamp = Date.now()
  return {
    content: `这是一个测试漂流瓶 ${timestamp}\n希望有人能捞到它`,
    direction: 1 // 樱花海岸
  }
}

// 生成热词内容
export function generateHotwordContent() {
  const timestamp = Date.now()
  return {
    name: `测试热词${timestamp}`,
    definition: '这是一个测试热词的释义，用于验证热词墙功能',
    example: '这是一个使用测试热词的例句',
    tags: ['测试', '自动化']
  }
}

// 生成故事内容
export function generateStoryContent() {
  const timestamp = Date.now()
  return {
    title: `测试故事${timestamp}`,
    category: 1, // 奇幻校园
    worldSetting: '这是一个测试故事的世界观设定，描述了故事发生的背景和环境',
    content: '这是测试故事的开篇内容。从前有一个测试工程师，他每天都在写自动化测试...'
  }
}

// 生成随机文本
export function generateRandomText(length = 50) {
  const chars = '这是一段测试文本内容用于验证功能的正确性包含中文字符和标点符号'
  let result = ''
  for (let i = 0; i < length; i++) {
    result += chars[Math.floor(Math.random() * chars.length)]
  }
  return result
}

// 等待时间常量
export const WAIT_TIME = {
  SHORT: 1000,
  MEDIUM: 2000,
  LONG: 3000,
  ANIMATION: 500
}
