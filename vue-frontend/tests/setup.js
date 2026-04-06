/**
 * Vitest 测试设置文件
 * 在每个测试文件运行前执行
 */

import { config } from '@vue/test-utils'

// 全局组件存根配置
config.global.stubs = {
  // 存根 router-link 和 router-view
  'router-link': true,
  'router-view': true,
  // 存根 Element Plus 组件（按需添加）
  'el-button': true,
  'el-input': true,
  'el-form': true,
  'el-form-item': true,
  'el-dialog': true,
  'el-message': true
}

// 全局 mock
config.global.mocks = {
  // Mock $router
  $router: {
    push: vi.fn(),
    replace: vi.fn(),
    go: vi.fn()
  },
  // Mock $route
  $route: {
    path: '/',
    params: {},
    query: {}
  }
}

// Mock localStorage
const localStorageMock = {
  getItem: vi.fn(),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn()
}
global.localStorage = localStorageMock

// Mock sessionStorage
const sessionStorageMock = {
  getItem: vi.fn(),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn()
}
global.sessionStorage = sessionStorageMock

// Mock window.matchMedia
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation(query => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn()
  }))
})

// Mock IntersectionObserver
global.IntersectionObserver = vi.fn().mockImplementation(() => ({
  observe: vi.fn(),
  unobserve: vi.fn(),
  disconnect: vi.fn()
}))

// Mock ResizeObserver
global.ResizeObserver = vi.fn().mockImplementation(() => ({
  observe: vi.fn(),
  unobserve: vi.fn(),
  disconnect: vi.fn()
}))

// 清理函数 - 在每个测试后重置 mock
afterEach(() => {
  vi.clearAllMocks()
  localStorageMock.getItem.mockReset()
  localStorageMock.setItem.mockReset()
  localStorageMock.removeItem.mockReset()
  localStorageMock.clear.mockReset()
})
