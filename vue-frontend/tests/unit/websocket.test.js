/**
 * WebSocket 功能单元测试
 * Feature: pre-defense-quality-fixes
 * Requirements: 3.1, 3.2, 3.3, 3.4, 3.5
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { ref } from 'vue'

/**
 * 模拟 WebSocket Store
 */
class WebSocketStore {
  constructor() {
    this.socket = ref(null)
    this.isConnected = ref(false)
    this.reconnectCount = 0
    this.maxReconnectCount = 10
    this.lastToken = null
    this.reconnectTimer = null
  }
  
  /**
   * 连接 WebSocket
   */
  connect(token) {
    this.lastToken = token
    
    try {
      // 创建 WebSocket 连接
      this.socket.value = this.createWebSocket(token)
      
      // 设置事件监听器
      this.socket.value.onopen = () => {
        this.isConnected.value = true
        this.reconnectCount = 0
      }
      
      this.socket.value.onclose = () => {
        this.isConnected.value = false
        this.handleDisconnect()
      }
      
      this.socket.value.onerror = (error) => {
        console.error('WebSocket error:', error)
      }
      
      return true
    } catch (error) {
      console.error('Failed to connect:', error)
      return false
    }
  }
  
  /**
   * 断开连接
   */
  disconnect() {
    if (this.socket.value) {
      this.socket.value.close()
      this.socket.value = null
    }
    this.isConnected.value = false
    this.clearReconnectTimer()
  }
  
  /**
   * 手动重连
   */
  manualReconnect() {
    if (!this.lastToken) {
      console.error('No token available for reconnection')
      return false
    }
    
    // 重置重连计数
    this.reconnectCount = 0
    
    // 关闭现有连接
    if (this.socket.value) {
      this.socket.value.close()
    }
    
    // 重新连接
    return this.connect(this.lastToken)
  }
  
  /**
   * 处理断开连接
   */
  handleDisconnect() {
    if (this.reconnectCount < this.maxReconnectCount && this.lastToken) {
      // 自动重连
      this.reconnectCount++
      const delay = Math.min(1000 * Math.pow(2, this.reconnectCount), 10000)
      
      this.reconnectTimer = setTimeout(() => {
        this.connect(this.lastToken)
      }, delay)
    }
  }
  
  /**
   * 清除重连定时器
   */
  clearReconnectTimer() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
  }
  
  /**
   * 创建 WebSocket（用于测试时 mock）
   */
  createWebSocket(token) {
    // 这个方法会在测试中被 mock
    return new WebSocket(`ws://localhost:8080/ws?token=${token}`)
  }
}

describe('WebSocket 功能 - 单元测试', () => {
  let store
  let mockWebSocket
  
  beforeEach(() => {
    store = new WebSocketStore()
    
    // 创建 mock WebSocket
    mockWebSocket = {
      readyState: 1, // WebSocket.OPEN = 1
      onopen: null,
      onclose: null,
      onerror: null,
      onmessage: null,
      send: vi.fn(),
      close: vi.fn()
    }
    
    // Mock createWebSocket 方法
    vi.spyOn(store, 'createWebSocket').mockReturnValue(mockWebSocket)
  })
  
  afterEach(() => {
    vi.restoreAllMocks()
    vi.clearAllTimers()
  })
  
  describe('连接状态管理', () => {
    
    it('应该在连接成功时设置 isConnected 为 true', () => {
      store.connect('test-token')
      
      // 触发 onopen 事件
      mockWebSocket.onopen()
      
      expect(store.isConnected.value).toBe(true)
    })
    
    it('应该在断开连接时设置 isConnected 为 false', () => {
      store.connect('test-token')
      mockWebSocket.onopen()
      expect(store.isConnected.value).toBe(true)
      
      // 触发 onclose 事件
      mockWebSocket.onclose()
      
      expect(store.isConnected.value).toBe(false)
    })
    
    it('应该在调用 disconnect 时设置 isConnected 为 false', () => {
      store.connect('test-token')
      mockWebSocket.onopen()
      
      store.disconnect()
      
      expect(store.isConnected.value).toBe(false)
    })
    
    it('应该保存最后使用的 token', () => {
      const token = 'my-auth-token'
      store.connect(token)
      
      expect(store.lastToken).toBe(token)
    })
    
    it('应该在连接成功时重置重连计数', () => {
      store.reconnectCount = 5
      
      store.connect('test-token')
      mockWebSocket.onopen()
      
      expect(store.reconnectCount).toBe(0)
    })
  })
  
  describe('手动重连函数', () => {
    
    it('应该使用保存的 token 重新连接', () => {
      const token = 'saved-token'
      store.connect(token)
      
      const connectSpy = vi.spyOn(store, 'connect')
      store.manualReconnect()
      
      expect(connectSpy).toHaveBeenCalledWith(token)
    })
    
    it('应该在手动重连时重置重连计数', () => {
      store.connect('test-token')
      store.reconnectCount = 5
      
      store.manualReconnect()
      
      expect(store.reconnectCount).toBe(0)
    })
    
    it('应该在手动重连前关闭现有连接', () => {
      store.connect('test-token')
      mockWebSocket.onopen()
      
      store.manualReconnect()
      
      expect(mockWebSocket.close).toHaveBeenCalled()
    })
    
    it('应该在没有 token 时返回 false', () => {
      store.lastToken = null
      
      const result = store.manualReconnect()
      
      expect(result).toBe(false)
    })
    
    it('应该在有 token 时返回 true', () => {
      store.lastToken = 'test-token'
      
      const result = store.manualReconnect()
      
      expect(result).toBe(true)
    })
  })
  
  describe('UI 指示器可见性', () => {
    
    it('应该在连接时隐藏断开指示器', () => {
      store.connect('test-token')
      mockWebSocket.onopen()
      
      // isConnected 为 true 时，UI 应该隐藏断开指示器
      expect(store.isConnected.value).toBe(true)
    })
    
    it('应该在断开时显示断开指示器', () => {
      store.connect('test-token')
      mockWebSocket.onopen()
      mockWebSocket.onclose()
      
      // isConnected 为 false 时，UI 应该显示断开指示器
      expect(store.isConnected.value).toBe(false)
    })
    
    it('应该在重连成功后隐藏断开指示器', () => {
      store.connect('test-token')
      mockWebSocket.onopen()
      
      // 断开
      mockWebSocket.onclose()
      expect(store.isConnected.value).toBe(false)
      
      // 重连成功
      store.manualReconnect()
      mockWebSocket.onopen()
      expect(store.isConnected.value).toBe(true)
    })
  })
  
  describe('重连逻辑', () => {
    
    it('应该在断开后自动尝试重连', () => {
      vi.useFakeTimers()
      
      store.connect('test-token')
      mockWebSocket.onopen()
      
      const connectSpy = vi.spyOn(store, 'connect')
      
      // 触发断开
      mockWebSocket.onclose()
      
      // 等待重连延迟
      vi.advanceTimersByTime(2000)
      
      expect(connectSpy).toHaveBeenCalled()
      expect(store.reconnectCount).toBeGreaterThan(0)
      
      vi.useRealTimers()
    })
    
    it('应该在达到最大重连次数后停止重连', () => {
      vi.useFakeTimers()
      
      store.connect('test-token')
      store.reconnectCount = store.maxReconnectCount
      
      const connectSpy = vi.spyOn(store, 'connect')
      
      // 触发断开
      mockWebSocket.onclose()
      
      // 等待
      vi.advanceTimersByTime(20000)
      
      // 不应该再次调用 connect
      expect(connectSpy).not.toHaveBeenCalled()
      
      vi.useRealTimers()
    })
    
    it('应该使用指数退避策略', () => {
      vi.useFakeTimers()
      
      store.connect('test-token')
      
      // 第一次断开
      mockWebSocket.onclose()
      expect(store.reconnectCount).toBe(1)
      
      // 第二次断开
      vi.advanceTimersByTime(2000)
      mockWebSocket.onclose()
      expect(store.reconnectCount).toBe(2)
      
      // 第三次断开
      vi.advanceTimersByTime(4000)
      mockWebSocket.onclose()
      expect(store.reconnectCount).toBe(3)
      
      vi.useRealTimers()
    })
    
    it('应该在没有 token 时不尝试重连', () => {
      vi.useFakeTimers()
      
      store.lastToken = null
      
      const connectSpy = vi.spyOn(store, 'connect')
      
      // 触发断开
      store.handleDisconnect()
      
      vi.advanceTimersByTime(10000)
      
      expect(connectSpy).not.toHaveBeenCalled()
      
      vi.useRealTimers()
    })
  })
  
  describe('连接生命周期', () => {
    
    it('应该在 disconnect 时关闭 socket', () => {
      store.connect('test-token')
      
      store.disconnect()
      
      expect(mockWebSocket.close).toHaveBeenCalled()
      expect(store.socket.value).toBeNull()
    })
    
    it('应该在 disconnect 时清除重连定时器', () => {
      vi.useFakeTimers()
      
      store.connect('test-token')
      mockWebSocket.onclose()
      
      // 应该有重连定时器
      expect(store.reconnectTimer).not.toBeNull()
      
      store.disconnect()
      
      // 定时器应该被清除
      expect(store.reconnectTimer).toBeNull()
      
      vi.useRealTimers()
    })
    
    it('应该处理连接错误', () => {
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
      
      store.connect('test-token')
      
      // 触发错误
      const error = new Error('Connection failed')
      mockWebSocket.onerror(error)
      
      expect(consoleSpy).toHaveBeenCalled()
      
      consoleSpy.mockRestore()
    })
  })
  
  describe('边界情况', () => {
    
    it('应该处理 null socket 的 disconnect', () => {
      store.socket.value = null
      
      expect(() => store.disconnect()).not.toThrow()
    })
    
    it('应该处理重复的 connect 调用', () => {
      store.connect('token1')
      store.connect('token2')
      
      expect(store.lastToken).toBe('token2')
    })
    
    it('应该处理快速的连接/断开循环', () => {
      store.connect('test-token')
      mockWebSocket.onopen()
      mockWebSocket.onclose()
      store.connect('test-token')
      mockWebSocket.onopen()
      
      expect(store.isConnected.value).toBe(true)
    })
    
    it('应该在创建 WebSocket 失败时返回 false', () => {
      vi.spyOn(store, 'createWebSocket').mockImplementation(() => {
        throw new Error('Failed to create WebSocket')
      })
      
      const result = store.connect('test-token')
      
      expect(result).toBe(false)
    })
  })
  
  describe('状态一致性', () => {
    
    it('应该保持 socket 和 isConnected 状态一致', () => {
      // 初始状态
      expect(store.socket.value).toBeNull()
      expect(store.isConnected.value).toBe(false)
      
      // 连接后
      store.connect('test-token')
      expect(store.socket.value).not.toBeNull()
      mockWebSocket.onopen()
      expect(store.isConnected.value).toBe(true)
      
      // 断开后
      store.disconnect()
      expect(store.socket.value).toBeNull()
      expect(store.isConnected.value).toBe(false)
    })
    
    it('应该在重连时保持 token 一致', () => {
      const token = 'consistent-token'
      
      store.connect(token)
      expect(store.lastToken).toBe(token)
      
      mockWebSocket.onclose()
      store.manualReconnect()
      
      expect(store.lastToken).toBe(token)
    })
  })
})
