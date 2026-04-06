/**
 * Property-Based Testing Generators
 * 用于生成测试数据的生成器集合
 */

import * as fc from 'fast-check'

/**
 * 文件生成器
 */
export const fileArbitrary = () => fc.record({
  name: fc.string({ minLength: 1, maxLength: 50 }),
  type: fc.oneof(
    fc.constant('image/jpeg'),
    fc.constant('image/png'),
    fc.constant('image/gif'),
    fc.constant('image/webp'),
    fc.constant('video/mp4'),
    fc.constant('video/webm'),
    fc.constant('text/plain'),
    fc.constant('application/pdf'),
    fc.string()
  ),
  size: fc.nat({ max: 100 * 1024 * 1024 }) // 最大 100MB
})

/**
 * 图片文件生成器
 */
export const imageFileArbitrary = () => fc.record({
  name: fc.string({ minLength: 1, maxLength: 50 }).map(s => `${s}.jpg`),
  type: fc.oneof(
    fc.constant('image/jpeg'),
    fc.constant('image/png'),
    fc.constant('image/gif'),
    fc.constant('image/webp')
  ),
  size: fc.nat({ max: 20 * 1024 * 1024 }) // 最大 20MB
})

/**
 * 视频文件生成器
 */
export const videoFileArbitrary = () => fc.record({
  name: fc.string({ minLength: 1, maxLength: 50 }).map(s => `${s}.mp4`),
  type: fc.oneof(
    fc.constant('video/mp4'),
    fc.constant('video/webm'),
    fc.constant('video/ogg'),
    fc.constant('video/quicktime')
  ),
  size: fc.nat({ max: 100 * 1024 * 1024 }) // 最大 100MB
})

/**
 * 无效文件生成器（非图片/视频）
 */
export const invalidFileArbitrary = () => fc.record({
  name: fc.string({ minLength: 1, maxLength: 50 }),
  type: fc.oneof(
    fc.constant('text/plain'),
    fc.constant('application/pdf'),
    fc.constant('application/json'),
    fc.constant('text/html')
  ),
  size: fc.nat({ max: 10 * 1024 * 1024 })
})

/**
 * 文件数组生成器
 */
export const fileArrayArbitrary = (minLength = 0, maxLength = 10) => 
  fc.array(fileArbitrary(), { minLength, maxLength })

/**
 * 内容字符串生成器
 */
export const contentArbitrary = () => fc.oneof(
  fc.constant(''),
  fc.constant('   '),
  fc.constant('\n\n\n'),
  fc.constant('\t\t'),
  fc.string({ minLength: 1, maxLength: 500 })
)

/**
 * 非空内容生成器
 */
export const nonEmptyContentArbitrary = () => 
  fc.string({ minLength: 1, maxLength: 500 })
    .filter(s => s.trim().length > 0)

/**
 * 空白内容生成器
 */
export const whitespaceContentArbitrary = () => fc.oneof(
  fc.constant(''),
  fc.constant('   '),
  fc.constant('\n'),
  fc.constant('\t'),
  fc.constant('  \n  \t  ')
)

/**
 * WebSocket 状态生成器
 */
export const wsStateArbitrary = () => fc.record({
  isConnected: fc.boolean(),
  reconnectCount: fc.nat({ max: 10 }),
  lastToken: fc.option(fc.string(), { nil: null })
})

/**
 * 位置坐标生成器
 */
export const coordinatesArbitrary = () => fc.record({
  lng: fc.double({ min: -180, max: 180 }),
  lat: fc.double({ min: -90, max: 90 })
})

/**
 * 时间戳生成器
 */
export const timestampArbitrary = () => fc.nat({ max: Date.now() + 1000000 })

/**
 * 冷却时间生成器
 */
export const cooldownArbitrary = () => fc.record({
  remainingSeconds: fc.nat({ max: 300 }),
  endTime: fc.option(timestampArbitrary(), { nil: null })
})

/**
 * 用户 ID 生成器
 */
export const userIdArbitrary = () => fc.nat({ min: 1, max: 100000 })

/**
 * 消息生成器
 */
export const messageArbitrary = () => fc.record({
  id: fc.nat(),
  senderId: userIdArbitrary(),
  receiverId: userIdArbitrary(),
  content: nonEmptyContentArbitrary(),
  timestamp: timestampArbitrary()
})

/**
 * 消息列表生成器
 */
export const messageListArbitrary = (minLength = 0, maxLength = 20) =>
  fc.array(messageArbitrary(), { minLength, maxLength })

/**
 * Dashboard 数据生成器
 */
export const dashboardDataArbitrary = () => fc.record({
  healthScore: fc.option(fc.record({
    score: fc.nat({ max: 100 }),
    status: fc.oneof(fc.constant('red'), fc.constant('yellow'), fc.constant('green')),
    dauRate: fc.double({ min: 0, max: 100 }),
    postRate: fc.double({ min: 0, max: 100 }),
    interactionRate: fc.double({ min: 0, max: 100 }),
    auditPassRate: fc.double({ min: 0, max: 100 })
  }), { nil: null }),
  
  userStats: fc.option(fc.record({
    total: fc.nat({ max: 100000 }),
    todayNew: fc.nat({ max: 1000 }),
    todayActive: fc.nat({ max: 10000 }),
    trend: fc.array(fc.record({
      date: fc.date().map(d => d.toISOString().slice(0, 10)),
      value: fc.nat({ max: 1000 })
    }), { maxLength: 7 }),
    creditDistribution: fc.option(fc.record({
      '优秀': fc.nat({ max: 1000 }),
      '良好': fc.nat({ max: 1000 }),
      '一般': fc.nat({ max: 1000 }),
      '较差': fc.nat({ max: 1000 }),
      '极差': fc.nat({ max: 1000 })
    }), { nil: null })
  }), { nil: null }),
  
  hourlyStats: fc.option(fc.record({
    posts: fc.array(fc.nat({ max: 100 }), { minLength: 24, maxLength: 24 }),
    comments: fc.array(fc.nat({ max: 100 }), { minLength: 24, maxLength: 24 }),
    postPeakHour: fc.nat({ max: 23 }),
    commentPeakHour: fc.nat({ max: 23 })
  }), { nil: null })
})
