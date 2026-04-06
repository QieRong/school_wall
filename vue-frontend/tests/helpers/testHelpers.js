/**
 * 测试辅助函数
 * 为属性测试和单元测试提供通用的工具函数
 */

import { vi } from 'vitest'

/**
 * Mock 工具
 */
export const mockUtils = {
  /**
   * 创建 mock 文件对象
   */
  createMockFile: (options = {}) => {
    const {
      name = 'test.jpg',
      type = 'image/jpeg',
      size = 1024 * 1024, // 1MB
      lastModified = Date.now()
    } = options

    return new File([''], name, {
      type,
      size,
      lastModified
    })
  },

  /**
   * 创建 mock FileList
   */
  createMockFileList: (files) => {
    const fileList = {
      length: files.length,
      item: (index) => files[index] || null,
      [Symbol.iterator]: function* () {
        for (let i = 0; i < files.length; i++) {
          yield files[i]
        }
      }
    }
    
    // 添加数组索引访问
    files.forEach((file, index) => {
      fileList[index] = file
    })

    return fileList
  },

  /**
   * 创建 mock Event 对象
   */
  createMockEvent: (type, options = {}) => {
    return {
      type,
      target: options.target || {},
      preventDefault: vi.fn(),
      stopPropagation: vi.fn(),
      ...options
    }
  },

  /**
   * 创建 mock WebSocket
   */
  createMockWebSocket: () => {
    const ws = {
      readyState: WebSocket.CONNECTING,
      send: vi.fn(),
      close: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      onopen: null,
      onclose: null,
      onerror: null,
      onmessage: null
    }

    // 模拟连接成功
    ws.connect = () => {
      ws.readyState = WebSocket.OPEN
      if (ws.onopen) ws.onopen({ type: 'open' })
    }

    // 模拟连接关闭
    ws.disconnect = () => {
      ws.readyState = WebSocket.CLOSED
      if (ws.onclose) ws.onclose({ type: 'close' })
    }

    // 模拟接收消息
    ws.receiveMessage = (data) => {
      if (ws.onmessage) {
        ws.onmessage({ type: 'message', data: JSON.stringify(data) })
      }
    }

    return ws
  },

  /**
   * 创建 mock 地理位置 API
   */
  createMockGeolocation: () => {
    return {
      getCurrentPosition: vi.fn(),
      watchPosition: vi.fn(),
      clearWatch: vi.fn()
    }
  },

  /**
   * 创建 mock Toast Store
   */
  createMockToastStore: () => {
    return {
      showToast: vi.fn(),
      hideToast: vi.fn(),
      toasts: []
    }
  },

  /**
   * 创建 mock Router
   */
  createMockRouter: () => {
    return {
      push: vi.fn(),
      replace: vi.fn(),
      go: vi.fn(),
      back: vi.fn(),
      forward: vi.fn(),
      currentRoute: { value: { path: '/', params: {}, query: {} } }
    }
  }
}

/**
 * 断言工具
 */
export const assertUtils = {
  /**
   * 断言是否为有效的验证结果对象
   */
  isValidationResult: (result) => {
    return (
      result !== null &&
      typeof result === 'object' &&
      'valid' in result &&
      typeof result.valid === 'boolean'
    )
  },

  /**
   * 断言是否为失败的验证结果
   */
  isFailedValidation: (result) => {
    return (
      assertUtils.isValidationResult(result) &&
      result.valid === false &&
      'message' in result &&
      typeof result.message === 'string'
    )
  },

  /**
   * 断言是否为成功的验证结果
   */
  isSuccessValidation: (result) => {
    return (
      assertUtils.isValidationResult(result) &&
      result.valid === true
    )
  },

  /**
   * 断言错误消息包含中文
   */
  hasChineseMessage: (result) => {
    return (
      assertUtils.isFailedValidation(result) &&
      /[\u4e00-\u9fa5]/.test(result.message)
    )
  },

  /**
   * 断言对象包含所有指定的键
   */
  hasAllKeys: (obj, keys) => {
    return keys.every(key => key in obj)
  },

  /**
   * 断言函数是否被调用指定次数
   */
  wasCalledTimes: (mockFn, times) => {
    return mockFn.mock.calls.length === times
  },

  /**
   * 断言函数是否被调用且参数匹配
   */
  wasCalledWith: (mockFn, ...args) => {
    return mockFn.mock.calls.some(call => 
      call.length === args.length &&
      call.every((arg, index) => arg === args[index])
    )
  }
}

/**
 * 异步测试工具
 */
export const asyncUtils = {
  /**
   * 等待指定时间
   */
  wait: (ms) => new Promise(resolve => setTimeout(resolve, ms)),

  /**
   * 等待条件满足
   */
  waitFor: async (condition, options = {}) => {
    const {
      timeout = 5000,
      interval = 50
    } = options

    const startTime = Date.now()

    while (Date.now() - startTime < timeout) {
      if (await condition()) {
        return true
      }
      await asyncUtils.wait(interval)
    }

    throw new Error('Timeout waiting for condition')
  },

  /**
   * 等待 Promise 完成（成功或失败）
   */
  waitForPromise: async (promise) => {
    try {
      const result = await promise
      return { success: true, result }
    } catch (error) {
      return { success: false, error }
    }
  },

  /**
   * 模拟网络延迟
   */
  simulateNetworkDelay: (min = 100, max = 500) => {
    const delay = Math.random() * (max - min) + min
    return asyncUtils.wait(delay)
  }
}

/**
 * DOM 测试工具
 */
export const domUtils = {
  /**
   * 查找元素
   */
  findElement: (selector) => {
    return document.querySelector(selector)
  },

  /**
   * 查找所有元素
   */
  findAllElements: (selector) => {
    return Array.from(document.querySelectorAll(selector))
  },

  /**
   * 触发事件
   */
  triggerEvent: (element, eventName, options = {}) => {
    const event = new Event(eventName, { bubbles: true, ...options })
    element.dispatchEvent(event)
  },

  /**
   * 设置输入值
   */
  setInputValue: (element, value) => {
    element.value = value
    domUtils.triggerEvent(element, 'input')
    domUtils.triggerEvent(element, 'change')
  },

  /**
   * 点击元素
   */
  clickElement: (element) => {
    domUtils.triggerEvent(element, 'click')
  },

  /**
   * 检查元素是否可见
   */
  isVisible: (element) => {
    return !!(
      element &&
      element.offsetWidth > 0 &&
      element.offsetHeight > 0 &&
      window.getComputedStyle(element).visibility !== 'hidden'
    )
  },

  /**
   * 检查元素是否禁用
   */
  isDisabled: (element) => {
    return element.disabled || element.hasAttribute('disabled')
  }
}

/**
 * 数据生成工具
 */
export const dataUtils = {
  /**
   * 生成随机字符串
   */
  randomString: (length = 10) => {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
    return Array.from({ length }, () => chars[Math.floor(Math.random() * chars.length)]).join('')
  },

  /**
   * 生成随机整数
   */
  randomInt: (min = 0, max = 100) => {
    return Math.floor(Math.random() * (max - min + 1)) + min
  },

  /**
   * 生成随机布尔值
   */
  randomBoolean: () => {
    return Math.random() < 0.5
  },

  /**
   * 从数组中随机选择一个元素
   */
  randomChoice: (array) => {
    return array[Math.floor(Math.random() * array.length)]
  },

  /**
   * 生成随机日期
   */
  randomDate: (start = new Date(2020, 0, 1), end = new Date()) => {
    return new Date(start.getTime() + Math.random() * (end.getTime() - start.getTime()))
  },

  /**
   * 深拷贝对象
   */
  deepClone: (obj) => {
    return JSON.parse(JSON.stringify(obj))
  }
}

/**
 * 验证工具
 */
export const validationUtils = {
  /**
   * 检查是否为空字符串或纯空白
   */
  isEmpty: (str) => {
    return !str || str.trim().length === 0
  },

  /**
   * 检查是否为有效的 MIME 类型
   */
  isValidMimeType: (type, allowedTypes) => {
    return allowedTypes.includes(type)
  },

  /**
   * 检查文件大小是否在限制内
   */
  isValidFileSize: (size, maxSizeBytes) => {
    return size > 0 && size <= maxSizeBytes
  },

  /**
   * 检查是否为有效的手机号
   */
  isValidPhone: (phone) => {
    return /^1[3-9]\d{9}$/.test(phone.trim())
  },

  /**
   * 检查是否为有效的学号
   */
  isValidStudentId: (id) => {
    return id && id.trim().length >= 6
  },

  /**
   * 字节转 MB
   */
  bytesToMB: (bytes) => {
    return bytes / (1024 * 1024)
  },

  /**
   * MB 转字节
   */
  mbToBytes: (mb) => {
    return mb * 1024 * 1024
  }
}

/**
 * 测试数据清理工具
 */
export const cleanupUtils = {
  /**
   * 清理所有 mock
   */
  clearAllMocks: () => {
    vi.clearAllMocks()
  },

  /**
   * 重置所有 mock
   */
  resetAllMocks: () => {
    vi.resetAllMocks()
  },

  /**
   * 恢复所有 mock
   */
  restoreAllMocks: () => {
    vi.restoreAllMocks()
  },

  /**
   * 清理 localStorage
   */
  clearLocalStorage: () => {
    if (global.localStorage) {
      global.localStorage.clear()
    }
  },

  /**
   * 清理 sessionStorage
   */
  clearSessionStorage: () => {
    if (global.sessionStorage) {
      global.sessionStorage.clear()
    }
  },

  /**
   * 清理所有存储
   */
  clearAllStorage: () => {
    cleanupUtils.clearLocalStorage()
    cleanupUtils.clearSessionStorage()
  }
}

/**
 * 性能测试工具
 */
export const performanceUtils = {
  /**
   * 测量函数执行时间
   */
  measureTime: async (fn) => {
    const start = performance.now()
    await fn()
    const end = performance.now()
    return end - start
  },

  /**
   * 测量平均执行时间
   */
  measureAverageTime: async (fn, iterations = 10) => {
    const times = []
    for (let i = 0; i < iterations; i++) {
      times.push(await performanceUtils.measureTime(fn))
    }
    return times.reduce((a, b) => a + b, 0) / times.length
  }
}

/**
 * 导出所有工具
 */
export default {
  mockUtils,
  assertUtils,
  asyncUtils,
  domUtils,
  dataUtils,
  validationUtils,
  cleanupUtils,
  performanceUtils
}
