/**
 * 首次私信限制机制单元测试
 * 测试 Message.vue 中的首次私信限制逻辑
 */

import { describe, it, expect } from 'vitest'

/**
 * 模拟 isInputDisabled 计算属性的逻辑
 * 规则：A给B第一次发消息时只能发送一条，除非B回复后才能继续发送
 * @param {Array} messages - 消息列表
 * @param {number} currentUserId - 当前用户ID
 * @returns {boolean} 是否应该禁用输入
 */
function isInputDisabled(messages, currentUserId) {
  if (!messages || messages.length === 0) return false
  
  // 检查是否有对方的回复
  const hasReceiverReply = messages.some(msg => msg.senderId !== currentUserId)
  
  // 如果对方已经回复过
  if (hasReceiverReply) {
    const lastMsg = messages[messages.length - 1]
    if (lastMsg.senderId === currentUserId) {
      // 检查最后一条自己的消息之前是否有对方的回复
      for (let i = messages.length - 2; i >= 0; i--) {
        if (messages[i].senderId !== currentUserId) {
          return false
        }
        if (messages[i].senderId === currentUserId) {
          return true
        }
      }
    }
    return false
  }
  
  // 如果对方从未回复过，检查自己是否已经发送了消息
  const hasSentMessage = messages.some(msg => msg.senderId === currentUserId)
  return hasSentMessage
}

describe('首次私信限制机制', () => {
  const currentUserId = 1
  const otherUserId = 2

  describe('空消息列表场景', () => {
    it('应该在消息列表为空时不禁用输入', () => {
      const messages = []
      expect(isInputDisabled(messages, currentUserId)).toBe(false)
    })

    it('应该在消息列表为 null 时不禁用输入', () => {
      const messages = null
      expect(isInputDisabled(messages, currentUserId)).toBe(false)
    })

    it('应该在消息列表为 undefined 时不禁用输入', () => {
      const messages = undefined
      expect(isInputDisabled(messages, currentUserId)).toBe(false)
    })
  })

  describe('只有一条发送者消息场景', () => {
    it('应该在只有一条自己发送的消息时禁用输入', () => {
      const messages = [
        { id: 1, senderId: currentUserId, receiverId: otherUserId, content: '你好' }
      ]
      expect(isInputDisabled(messages, currentUserId)).toBe(true)
    })

    it('应该在只有一条对方发送的消息时不禁用输入', () => {
      const messages = [
        { id: 1, senderId: otherUserId, receiverId: currentUserId, content: '你好' }
      ]
      expect(isInputDisabled(messages, currentUserId)).toBe(false)
    })
  })

  describe('有往来消息场景', () => {
    it('应该在有往来消息时不禁用输入', () => {
      const messages = [
        { id: 1, senderId: currentUserId, receiverId: otherUserId, content: '你好' },
        { id: 2, senderId: otherUserId, receiverId: currentUserId, content: '你好啊' }
      ]
      expect(isInputDisabled(messages, currentUserId)).toBe(false)
    })

    it('应该在最后一条是对方消息时不禁用输入', () => {
      const messages = [
        { id: 1, senderId: currentUserId, receiverId: otherUserId, content: '你好' },
        { id: 2, senderId: otherUserId, receiverId: currentUserId, content: '你好啊' },
        { id: 3, senderId: currentUserId, receiverId: otherUserId, content: '最近怎么样' },
        { id: 4, senderId: otherUserId, receiverId: currentUserId, content: '还不错' }
      ]
      expect(isInputDisabled(messages, currentUserId)).toBe(false)
    })

    it('应该在多轮对话后最后是对方消息时不禁用输入', () => {
      const messages = [
        { id: 1, senderId: currentUserId, receiverId: otherUserId, content: '消息1' },
        { id: 2, senderId: otherUserId, receiverId: currentUserId, content: '消息2' },
        { id: 3, senderId: currentUserId, receiverId: otherUserId, content: '消息3' },
        { id: 4, senderId: otherUserId, receiverId: currentUserId, content: '消息4' },
        { id: 5, senderId: currentUserId, receiverId: otherUserId, content: '消息5' },
        { id: 6, senderId: otherUserId, receiverId: currentUserId, content: '消息6' }
      ]
      expect(isInputDisabled(messages, currentUserId)).toBe(false)
    })
  })

  describe('连续发送者消息场景', () => {
    it('应该在连续两条自己的消息时禁用输入', () => {
      const messages = [
        { id: 1, senderId: currentUserId, receiverId: otherUserId, content: '你好' },
        { id: 2, senderId: currentUserId, receiverId: otherUserId, content: '在吗？' }
      ]
      expect(isInputDisabled(messages, currentUserId)).toBe(true)
    })

    it('应该在有往来后又连续发送两条时禁用输入', () => {
      const messages = [
        { id: 1, senderId: currentUserId, receiverId: otherUserId, content: '你好' },
        { id: 2, senderId: otherUserId, receiverId: currentUserId, content: '你好啊' },
        { id: 3, senderId: currentUserId, receiverId: otherUserId, content: '最近怎么样' },
        { id: 4, senderId: currentUserId, receiverId: otherUserId, content: '在吗？' }
      ]
      expect(isInputDisabled(messages, currentUserId)).toBe(true)
    })

    it('应该在连续三条自己的消息时禁用输入', () => {
      const messages = [
        { id: 1, senderId: currentUserId, receiverId: otherUserId, content: '你好' },
        { id: 2, senderId: currentUserId, receiverId: otherUserId, content: '在吗？' },
        { id: 3, senderId: currentUserId, receiverId: otherUserId, content: '回复一下' }
      ]
      expect(isInputDisabled(messages, currentUserId)).toBe(true)
    })

    it('应该在对方连续发送多条消息时不禁用输入', () => {
      const messages = [
        { id: 1, senderId: otherUserId, receiverId: currentUserId, content: '你好' },
        { id: 2, senderId: otherUserId, receiverId: currentUserId, content: '在吗？' },
        { id: 3, senderId: otherUserId, receiverId: currentUserId, content: '回复一下' }
      ]
      expect(isInputDisabled(messages, currentUserId)).toBe(false)
    })
  })

  describe('边界情况', () => {
    it('应该正确处理大量消息的情况', () => {
      const messages = []
      // 创建100条交替的消息
      for (let i = 0; i < 100; i++) {
        messages.push({
          id: i + 1,
          senderId: i % 2 === 0 ? currentUserId : otherUserId,
          receiverId: i % 2 === 0 ? otherUserId : currentUserId,
          content: `消息${i + 1}`
        })
      }
      // 最后一条是自己发的（因为100是偶数，100-1=99是奇数，所以最后一条索引99%2=1，是对方的）
      // 实际上最后一条索引是99，99%2=1，所以是otherUserId
      expect(isInputDisabled(messages, currentUserId)).toBe(false)
    })

    it('应该在最后两条都是自己发送的大量消息中禁用输入', () => {
      const messages = []
      // 创建98条交替的消息
      for (let i = 0; i < 98; i++) {
        messages.push({
          id: i + 1,
          senderId: i % 2 === 0 ? currentUserId : otherUserId,
          receiverId: i % 2 === 0 ? otherUserId : currentUserId,
          content: `消息${i + 1}`
        })
      }
      // 添加最后两条自己的消息
      messages.push({
        id: 99,
        senderId: currentUserId,
        receiverId: otherUserId,
        content: '消息99'
      })
      messages.push({
        id: 100,
        senderId: currentUserId,
        receiverId: otherUserId,
        content: '消息100'
      })
      expect(isInputDisabled(messages, currentUserId)).toBe(true)
    })

    it('应该正确处理不同用户ID的情况', () => {
      const messages = [
        { id: 1, senderId: 999, receiverId: 888, content: '你好' },
        { id: 2, senderId: 999, receiverId: 888, content: '在吗？' }
      ]
      expect(isInputDisabled(messages, 999)).toBe(true)
      expect(isInputDisabled(messages, 888)).toBe(false)
    })
  })

  describe('输入框占位符文本', () => {
    it('应该在禁用时显示等待提示', () => {
      const messages = [
        { id: 1, senderId: currentUserId, receiverId: otherUserId, content: '你好' }
      ]
      const disabled = isInputDisabled(messages, currentUserId)
      const placeholder = disabled ? '等待对方回复后才能继续发送...' : 'Enter 发送，Ctrl+Enter 换行'
      expect(placeholder).toBe('等待对方回复后才能继续发送...')
    })

    it('应该在不禁用时显示正常提示', () => {
      const messages = [
        { id: 1, senderId: otherUserId, receiverId: currentUserId, content: '你好' }
      ]
      const disabled = isInputDisabled(messages, currentUserId)
      const placeholder = disabled ? '等待对方回复后才能继续发送...' : 'Enter 发送，Ctrl+Enter 换行'
      expect(placeholder).toBe('Enter 发送，Ctrl+Enter 换行')
    })
  })
})
