/**
 * Property-Based Testing Utilities
 * 测试工具函数集合
 */

/**
 * 创建模拟的 File 对象
 */
export function createMockFile(fileData) {
  const { name, type, size } = fileData
  
  // 创建一个模拟的 Blob
  const blob = new Blob(['x'.repeat(size)], { type })
  
  // 添加 File 特有的属性
  Object.defineProperty(blob, 'name', {
    value: name,
    writable: false
  })
  
  Object.defineProperty(blob, 'lastModified', {
    value: Date.now(),
    writable: false
  })
  
  return blob
}

/**
 * 创建模拟的 FileList
 */
export function createMockFileList(files) {
  const fileList = files.map(createMockFile)
  
  Object.defineProperty(fileList, 'length', {
    value: files.length,
    writable: false
  })
  
  fileList.item = function(index) {
    return this[index] || null
  }
  
  return fileList
}

/**
 * 验证文件 MIME 类型
 */
export function isValidImageType(mimeType) {
  const validTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp']
  return validTypes.includes(mimeType)
}

/**
 * 验证视频 MIME 类型
 */
export function isValidVideoType(mimeType) {
  const validTypes = ['video/mp4', 'video/webm', 'video/ogg', 'video/quicktime']
  return validTypes.includes(mimeType)
}

/**
 * 验证文件大小
 */
export function isValidFileSize(size, maxSize) {
  return size > 0 && size <= maxSize
}

/**
 * 检查内容是否为空或仅包含空白字符
 */
export function isEmptyContent(content) {
  return !content || content.trim().length === 0
}

/**
 * 模拟 WebSocket 连接
 */
export function createMockWebSocket() {
  const listeners = {}
  
  return {
    readyState: WebSocket.OPEN,
    send: vi.fn(),
    close: vi.fn(),
    addEventListener: vi.fn((event, handler) => {
      if (!listeners[event]) listeners[event] = []
      listeners[event].push(handler)
    }),
    removeEventListener: vi.fn((event, handler) => {
      if (listeners[event]) {
        listeners[event] = listeners[event].filter(h => h !== handler)
      }
    }),
    trigger: (event, data) => {
      if (listeners[event]) {
        listeners[event].forEach(handler => handler(data))
      }
    }
  }
}

/**
 * 模拟地理位置 API
 */
export function createMockGeolocation() {
  return {
    getCurrentPosition: vi.fn((success, error) => {
      // 默认成功
      success({
        coords: {
          latitude: 29.1167,
          longitude: 110.4833,
          accuracy: 100
        }
      })
    }),
    watchPosition: vi.fn(),
    clearWatch: vi.fn()
  }
}

/**
 * 等待异步操作
 */
export async function waitFor(callback, options = {}) {
  const { timeout = 1000, interval = 50 } = options
  const startTime = Date.now()
  
  while (Date.now() - startTime < timeout) {
    try {
      const result = await callback()
      if (result) return result
    } catch (e) {
      // 继续等待
    }
    await new Promise(resolve => setTimeout(resolve, interval))
  }
  
  throw new Error('Timeout waiting for condition')
}

/**
 * 模拟 localStorage
 */
export function createMockLocalStorage() {
  let store = {}
  
  return {
    getItem: vi.fn(key => store[key] || null),
    setItem: vi.fn((key, value) => {
      store[key] = String(value)
    }),
    removeItem: vi.fn(key => {
      delete store[key]
    }),
    clear: vi.fn(() => {
      store = {}
    }),
    get length() {
      return Object.keys(store).length
    },
    key: vi.fn(index => {
      const keys = Object.keys(store)
      return keys[index] || null
    })
  }
}

/**
 * 计算冷却剩余时间
 */
export function calculateCooldownRemaining(endTime) {
  if (!endTime) return 0
  const remaining = Math.ceil((endTime - Date.now()) / 1000)
  return Math.max(0, remaining)
}

/**
 * 验证 Dashboard 数据结构
 */
export function validateDashboardData(data) {
  if (!data) return false
  
  // 检查必要的字段
  const hasValidStructure = 
    (data.healthScore === null || typeof data.healthScore === 'object') &&
    (data.userStats === null || typeof data.userStats === 'object') &&
    (data.hourlyStats === null || typeof data.hourlyStats === 'object')
  
  return hasValidStructure
}

/**
 * 检查数组是否全为零
 */
export function isAllZeros(arr) {
  return Array.isArray(arr) && arr.every(v => v === 0)
}

/**
 * 检查对象是否为空
 */
export function isEmptyObject(obj) {
  return obj && typeof obj === 'object' && Object.keys(obj).length === 0
}
