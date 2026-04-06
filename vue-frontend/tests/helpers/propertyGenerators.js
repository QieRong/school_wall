/**
 * 属性测试生成器工具
 * 为 pre-defense-quality-fixes 规范提供通用的测试数据生成器
 */

import fc from 'fast-check'

/**
 * 文件生成器
 */
export const fileGenerators = {
  /**
   * 生成有效的图片 MIME 类型
   */
  validImageMimeType: () => fc.constantFrom(
    'image/jpeg',
    'image/jpg',
    'image/png',
    'image/gif',
    'image/webp'
  ),

  /**
   * 生成有效的视频 MIME 类型
   */
  validVideoMimeType: () => fc.constantFrom(
    'video/mp4',
    'video/webm',
    'video/ogg',
    'video/quicktime'
  ),

  /**
   * 生成无效的 MIME 类型（非图片/视频）
   */
  invalidMimeType: () => fc.constantFrom(
    'text/plain',
    'application/pdf',
    'application/msword',
    'application/zip',
    'audio/mp3',
    'audio/wav'
  ),

  /**
   * 生成文件对象
   * @param {Object} options - 配置选项
   * @param {fc.Arbitrary} options.type - MIME 类型生成器
   * @param {fc.Arbitrary} options.size - 文件大小生成器（字节）
   * @param {fc.Arbitrary} options.name - 文件名生成器
   */
  file: (options = {}) => {
    const {
      type = fc.string(),
      size = fc.integer({ min: 0, max: 100 * 1024 * 1024 }),
      name = fc.string({ minLength: 1, maxLength: 50 })
    } = options

    return fc.record({
      name,
      type,
      size,
      lastModified: fc.date().map(d => d.getTime())
    })
  },

  /**
   * 生成图片文件
   */
  imageFile: (maxSizeMB = 10) => fileGenerators.file({
    type: fileGenerators.validImageMimeType(),
    size: fc.integer({ min: 1024, max: maxSizeMB * 1024 * 1024 }),
    name: fc.string({ minLength: 1, maxLength: 50 }).map(s => `${s}.jpg`)
  }),

  /**
   * 生成视频文件
   */
  videoFile: (maxSizeMB = 50) => fileGenerators.file({
    type: fileGenerators.validVideoMimeType(),
    size: fc.integer({ min: 1024, max: maxSizeMB * 1024 * 1024 }),
    name: fc.string({ minLength: 1, maxLength: 50 }).map(s => `${s}.mp4`)
  }),

  /**
   * 生成文件数组
   */
  fileArray: (options = {}) => {
    const {
      minLength = 1,
      maxLength = 10,
      fileGen = fileGenerators.imageFile()
    } = options

    return fc.array(fileGen, { minLength, maxLength })
  }
}

/**
 * WebSocket 生成器
 */
export const websocketGenerators = {
  /**
   * 生成 WebSocket 连接状态
   */
  connectionState: () => fc.constantFrom('connected', 'disconnected', 'connecting', 'error'),

  /**
   * 生成 WebSocket 消息类型
   */
  messageType: () => fc.constantFrom(
    'NOTICE',
    'CHAT',
    'STORY_NEW_PARAGRAPH',
    'STORY_LIKE_UPDATE'
  ),

  /**
   * 生成 WebSocket 消息
   */
  message: () => fc.record({
    type: websocketGenerators.messageType(),
    content: fc.option(fc.string({ maxLength: 500 }), { nil: undefined }),
    data: fc.option(fc.anything(), { nil: undefined }),
    timestamp: fc.date().map(d => d.getTime())
  }),

  /**
   * 生成重连计数
   */
  reconnectCount: () => fc.integer({ min: 0, max: 10 })
}

/**
 * 内容验证生成器
 */
export const contentGenerators = {
  /**
   * 生成空内容（空字符串或纯空白）
   */
  emptyContent: () => fc.oneof(
    fc.constant(''),
    fc.string({ minLength: 1, maxLength: 20 }).map(s => ' '.repeat(s.length)),
    fc.constant('   '),
    fc.constant('\n\n\n'),
    fc.constant('\t\t\t')
  ),

  /**
   * 生成有效内容（非空且包含非空白字符）
   */
  validContent: () => fc.string({ minLength: 1, maxLength: 500 })
    .filter(s => s.trim().length > 0),

  /**
   * 生成任意内容（可能为空）
   */
  anyContent: () => fc.string({ maxLength: 500 }),

  /**
   * 生成帖子表单数据
   */
  postForm: () => fc.record({
    content: fc.string({ maxLength: 500 }),
    category: fc.option(fc.string({ minLength: 1, maxLength: 20 }), { nil: undefined }),
    images: fc.array(fc.string(), { maxLength: 9 }),
    video: fc.option(fc.string(), { nil: undefined }),
    location: fc.option(fc.string(), { nil: undefined })
  })
}

/**
 * 地图定位生成器
 */
export const locationGenerators = {
  /**
   * 生成经纬度坐标
   */
  coordinates: () => fc.record({
    lng: fc.double({ min: -180, max: 180 }),
    lat: fc.double({ min: -90, max: 90 })
  }),

  /**
   * 生成地理位置错误类型
   */
  errorType: () => fc.constantFrom(
    'PERMISSION_DENIED',
    'POSITION_UNAVAILABLE',
    'TIMEOUT',
    'UNKNOWN_ERROR'
  ),

  /**
   * 生成地理位置错误消息
   */
  errorMessage: () => fc.constantFrom(
    '定位权限被拒绝',
    '无法获取位置信息',
    '定位请求超时',
    '未知错误'
  ),

  /**
   * 生成地理位置状态
   */
  locationState: () => fc.record({
    isLoading: fc.boolean(),
    hasError: fc.boolean(),
    errorMessage: fc.option(locationGenerators.errorMessage(), { nil: undefined }),
    position: fc.option(locationGenerators.coordinates(), { nil: null }),
    displayName: fc.option(fc.string({ minLength: 1, maxLength: 100 }), { nil: undefined })
  })
}

/**
 * AI 服务生成器
 */
export const aiServiceGenerators = {
  /**
   * 生成 AI 请求超时时间（毫秒）
   */
  timeout: () => fc.integer({ min: 1000, max: 60000 }),

  /**
   * 生成 AI 响应
   */
  response: () => fc.record({
    code: fc.constantFrom('200', '400', '500', '503'),
    msg: fc.option(fc.string({ maxLength: 100 }), { nil: undefined }),
    data: fc.option(fc.string({ maxLength: 1000 }), { nil: undefined })
  }),

  /**
   * 生成 AI 服务错误
   */
  error: () => fc.record({
    message: fc.string({ maxLength: 200 }),
    code: fc.option(fc.string(), { nil: undefined }),
    stack: fc.option(fc.string(), { nil: undefined })
  })
}

/**
 * 按钮状态生成器
 */
export const buttonStateGenerators = {
  /**
   * 生成按钮状态
   */
  state: () => fc.constantFrom('idle', 'loading', 'disabled', 'success', 'error'),

  /**
   * 生成加载状态
   */
  loading: () => fc.boolean(),

  /**
   * 生成禁用状态
   */
  disabled: () => fc.boolean()
}

/**
 * 时间生成器
 */
export const timeGenerators = {
  /**
   * 生成时间戳（毫秒）
   */
  timestamp: () => fc.date().map(d => d.getTime()),

  /**
   * 生成冷却时间（秒）
   */
  cooldownSeconds: () => fc.integer({ min: 0, max: 300 }),

  /**
   * 生成超时时间（毫秒）
   */
  timeoutMs: () => fc.integer({ min: 1000, max: 60000 })
}

/**
 * 数据大屏生成器
 */
export const dashboardGenerators = {
  /**
   * 生成图表数据点
   */
  dataPoint: () => fc.record({
    name: fc.string({ minLength: 1, maxLength: 20 }),
    value: fc.integer({ min: 0, max: 10000 })
  }),

  /**
   * 生成图表数据数组（可能为空）
   */
  chartData: () => fc.array(
    dashboardGenerators.dataPoint(),
    { minLength: 0, maxLength: 50 }
  ),

  /**
   * 生成空数据（null、undefined 或空数组）
   */
  emptyData: () => fc.constantFrom(null, undefined, []),

  /**
   * 生成有效数据（非空数组）
   */
  validData: () => fc.array(
    dashboardGenerators.dataPoint(),
    { minLength: 1, maxLength: 50 }
  )
}

/**
 * 通用工具函数
 */
export const testUtils = {
  /**
   * 创建延迟 Promise
   */
  delay: (ms) => new Promise(resolve => setTimeout(resolve, ms)),

  /**
   * 模拟异步操作
   */
  mockAsyncOperation: (result, delayMs = 100) => {
    return new Promise((resolve) => {
      setTimeout(() => resolve(result), delayMs)
    })
  },

  /**
   * 模拟失败的异步操作
   */
  mockFailedAsyncOperation: (error, delayMs = 100) => {
    return new Promise((_, reject) => {
      setTimeout(() => reject(error), delayMs)
    })
  },

  /**
   * 检查是否为中文字符串
   */
  isChineseString: (str) => {
    return typeof str === 'string' && /[\u4e00-\u9fa5]/.test(str)
  },

  /**
   * 检查对象是否有指定的属性
   */
  hasProperties: (obj, properties) => {
    return properties.every(prop => prop in obj)
  }
}

/**
 * 预定义的常量
 */
export const constants = {
  // 文件大小限制
  MAX_IMAGE_SIZE_MB: 10,
  MAX_VIDEO_SIZE_MB: 50,
  MAX_IMAGE_COUNT: 9,

  // MIME 类型
  ALLOWED_IMAGE_TYPES: [
    'image/jpeg',
    'image/jpg',
    'image/png',
    'image/gif',
    'image/webp'
  ],
  ALLOWED_VIDEO_TYPES: [
    'video/mp4',
    'video/webm',
    'video/ogg',
    'video/quicktime'
  ],

  // WebSocket
  MAX_RECONNECT_COUNT: 10,
  HEARTBEAT_INTERVAL: 30000,

  // AI 服务
  AI_SERVICE_TIMEOUT: 30000,

  // 冷却时间
  DEFAULT_COOLDOWN_SECONDS: 60
}
