/**
 * 测试工具函数
 * 提供通用的测试辅助方法
 */

import { mount, shallowMount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'

/**
 * 创建带有 Pinia 的组件挂载器
 * @param {Object} component - Vue 组件
 * @param {Object} options - 挂载选项
 * @returns {Object} 挂载的组件包装器
 */
export function mountWithPinia(component, options = {}) {
  const pinia = createPinia()
  setActivePinia(pinia)
  
  return mount(component, {
    global: {
      plugins: [pinia],
      ...options.global
    },
    ...options
  })
}

/**
 * 创建带有 Pinia 的浅挂载器
 * @param {Object} component - Vue 组件
 * @param {Object} options - 挂载选项
 * @returns {Object} 挂载的组件包装器
 */
export function shallowMountWithPinia(component, options = {}) {
  const pinia = createPinia()
  setActivePinia(pinia)
  
  return shallowMount(component, {
    global: {
      plugins: [pinia],
      ...options.global
    },
    ...options
  })
}

/**
 * 等待 DOM 更新
 * @param {number} ms - 等待毫秒数
 * @returns {Promise}
 */
export function wait(ms = 0) {
  return new Promise(resolve => setTimeout(resolve, ms))
}

/**
 * 等待组件更新完成
 * @param {Object} wrapper - Vue Test Utils 包装器
 * @returns {Promise}
 */
export async function flushPromises() {
  await new Promise(resolve => setTimeout(resolve, 0))
}

/**
 * 创建模拟的 API 响应
 * @param {Object} data - 响应数据
 * @param {number} code - 响应码
 * @param {string} msg - 响应消息
 * @returns {Object} 模拟响应
 */
export function mockApiResponse(data, code = 200, msg = 'success') {
  return {
    data: {
      code,
      msg,
      data
    }
  }
}

/**
 * 创建模拟的错误响应
 * @param {string} msg - 错误消息
 * @param {number} code - 错误码
 * @returns {Object} 模拟错误响应
 */
export function mockApiError(msg = 'error', code = 500) {
  return {
    data: {
      code,
      msg,
      data: null
    }
  }
}

/**
 * 创建测试用户数据
 * @param {Object} overrides - 覆盖的字段
 * @returns {Object} 用户数据
 */
export function createTestUser(overrides = {}) {
  return {
    id: 1,
    account: '2021001001',
    nickname: '测试用户',
    avatar: '/default.png',
    role: 0,
    status: 1,
    creditScore: 50,
    followerCount: 0,
    followingCount: 0,
    ...overrides
  }
}

/**
 * 创建测试帖子数据
 * @param {Object} overrides - 覆盖的字段
 * @returns {Object} 帖子数据
 */
export function createTestPost(overrides = {}) {
  return {
    id: 1,
    userId: 1,
    content: '这是一条测试帖子',
    images: [],
    category: 1,
    isAnonymous: 0,
    visibility: 0,
    likeCount: 0,
    commentCount: 0,
    shareCount: 0,
    viewCount: 0,
    status: 1,
    createTime: new Date().toISOString(),
    ...overrides
  }
}

/**
 * 创建测试评论数据
 * @param {Object} overrides - 覆盖的字段
 * @returns {Object} 评论数据
 */
export function createTestComment(overrides = {}) {
  return {
    id: 1,
    postId: 1,
    userId: 1,
    content: '这是一条测试评论',
    parentId: null,
    likeCount: 0,
    createTime: new Date().toISOString(),
    ...overrides
  }
}

/**
 * 触发输入事件
 * @param {Object} wrapper - 组件包装器
 * @param {string} selector - 选择器
 * @param {string} value - 输入值
 */
export async function triggerInput(wrapper, selector, value) {
  const input = wrapper.find(selector)
  await input.setValue(value)
  await input.trigger('input')
}

/**
 * 触发点击事件
 * @param {Object} wrapper - 组件包装器
 * @param {string} selector - 选择器
 */
export async function triggerClick(wrapper, selector) {
  const element = wrapper.find(selector)
  await element.trigger('click')
}
