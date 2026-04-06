/**
 * 首次私信限制机制属性测试
 * Property 1: 首次私信限制正确性
 * Validates: Requirements 1.1, 1.2
 */

import { describe, it, expect } from 'vitest'
import fc from 'fast-check'

/**
 * 模拟 isInputDisabled 计算属性的逻辑
 * 规则：A给B第一次发消息时只能发送一条，除非B回复后才能继续发送
 */
function isInputDisabled(messages, currentUserId) {
  if (!messages || messages.length === 0) return false
  
  const hasReceiverReply = messages.some(msg => msg.senderId !== currentUserId)
  
  if (hasReceiverReply) {
    const lastMsg = messages[messages.length - 1]
    if (lastMsg.senderId === currentUserId) {
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
  
  const hasSentMessage = messages.some(msg => msg.senderId === currentUserId)
  return hasSentMessage
}

/**
 * 生成随机消息对象
 */
const messageArbitrary = (senderId, receiverId) => fc.record({
  id: fc.integer({ min: 1, max: 1000000 }),
  senderId: fc.constant(senderId),
  receiverId: fc.constant(receiverId),
  content: fc.string({ minLength: 1, maxLength: 100 }),
  type: fc.constant(0),
  isWithdrawn: fc.constant(0),
  createTime: fc.date()
})

describe('Property 1: 首次私信限制正确性', () => {
  const currentUserId = 1
  const otherUserId = 2

  it('属性：空消息列表永远不禁用输入', () => {
    fc.assert(
      fc.property(
        fc.constant([]),
        fc.integer({ min: 1, max: 1000 }),
        (messages, userId) => {
          return isInputDisabled(messages, userId) === false
        }
      ),
      { numRuns: 100 }
    )
  })

  it('属性：只有一条自己的消息时必须禁用输入', () => {
    fc.assert(
      fc.property(
        fc.integer({ min: 1, max: 1000 }),
        fc.integer({ min: 1, max: 1000 }),
        fc.string({ minLength: 1, maxLength: 100 }),
        (userId, otherId, content) => {
          fc.pre(userId !== otherId) // 确保是不同的用户
          const messages = [{
            id: 1,
            senderId: userId,
            receiverId: otherId,
            content: content
          }]
          return isInputDisabled(messages, userId) === true
        }
      ),
      { numRuns: 100 }
    )
  })

  it('属性：只有一条对方的消息时必须不禁用输入', () => {
    fc.assert(
      fc.property(
        fc.integer({ min: 1, max: 1000 }),
        fc.integer({ min: 1, max: 1000 }),
        fc.string({ minLength: 1, maxLength: 100 }),
        (userId, otherId, content) => {
          fc.pre(userId !== otherId)
          const messages = [{
            id: 1,
            senderId: otherId,
            receiverId: userId,
            content: content
          }]
          return isInputDisabled(messages, userId) === false
        }
      ),
      { numRuns: 100 }
    )
  })

  it('属性：最后一条是对方消息时必须不禁用输入', () => {
    fc.assert(
      fc.property(
        fc.array(fc.boolean(), { minLength: 1, maxLength: 20 }),
        fc.integer({ min: 1, max: 1000 }),
        fc.integer({ min: 1, max: 1000 }),
        (senderPattern, userId, otherId) => {
          fc.pre(userId !== otherId)
          
          // 根据模式生成消息，但确保最后一条是对方的
          const messages = senderPattern.map((isCurrentUser, index) => ({
            id: index + 1,
            senderId: isCurrentUser ? userId : otherId,
            receiverId: isCurrentUser ? otherId : userId,
            content: `消息${index + 1}`
          }))
          
          // 强制最后一条是对方的消息
          messages[messages.length - 1] = {
            id: messages.length,
            senderId: otherId,
            receiverId: userId,
            content: `最后的消息`
          }
          
          return isInputDisabled(messages, userId) === false
        }
      ),
      { numRuns: 100 }
    )
  })

  it('属性：连续两条自己的消息时必须禁用输入', () => {
    fc.assert(
      fc.property(
        fc.array(fc.boolean(), { minLength: 0, maxLength: 18 }),
        fc.integer({ min: 1, max: 1000 }),
        fc.integer({ min: 1, max: 1000 }),
        (senderPattern, userId, otherId) => {
          fc.pre(userId !== otherId)
          
          // 根据模式生成前面的消息
          const messages = senderPattern.map((isCurrentUser, index) => ({
            id: index + 1,
            senderId: isCurrentUser ? userId : otherId,
            receiverId: isCurrentUser ? otherId : userId,
            content: `消息${index + 1}`
          }))
          
          // 添加最后两条自己的消息
          messages.push({
            id: messages.length + 1,
            senderId: userId,
            receiverId: otherId,
            content: '倒数第二条'
          })
          messages.push({
            id: messages.length + 1,
            senderId: userId,
            receiverId: otherId,
            content: '最后一条'
          })
          
          return isInputDisabled(messages, userId) === true
        }
      ),
      { numRuns: 100 }
    )
  })

  it('属性：交替发送消息时，最后是对方消息则不禁用', () => {
    fc.assert(
      fc.property(
        fc.integer({ min: 1, max: 50 }),
        fc.integer({ min: 1, max: 1000 }),
        fc.integer({ min: 1, max: 1000 }),
        (messageCount, userId, otherId) => {
          fc.pre(userId !== otherId)
          fc.pre(messageCount > 0)
          
          // 生成交替的消息，偶数索引是当前用户，奇数索引是对方
          const messages = []
          for (let i = 0; i < messageCount; i++) {
            const isCurrentUser = i % 2 === 0
            messages.push({
              id: i + 1,
              senderId: isCurrentUser ? userId : otherId,
              receiverId: isCurrentUser ? otherId : userId,
              content: `消息${i + 1}`
            })
          }
          
          // 如果消息数量是奇数，最后一条是当前用户的，应该禁用
          // 如果消息数量是偶数，最后一条是对方的，应该不禁用
          const lastIsCurrentUser = (messageCount - 1) % 2 === 0
          const expectedDisabled = lastIsCurrentUser && (messageCount === 1 || (messageCount >= 2 && (messageCount - 2) % 2 === 0))
          
          return isInputDisabled(messages, userId) === expectedDisabled
        }
      ),
      { numRuns: 100 }
    )
  })

  it('属性：禁用状态的一致性 - 相同消息列表应该得到相同结果', () => {
    fc.assert(
      fc.property(
        fc.array(fc.boolean(), { minLength: 1, maxLength: 10 }),
        fc.integer({ min: 1, max: 1000 }),
        fc.integer({ min: 1, max: 1000 }),
        (senderPattern, userId, otherId) => {
          fc.pre(userId !== otherId)
          
          const messages = senderPattern.map((isCurrentUser, index) => ({
            id: index + 1,
            senderId: isCurrentUser ? userId : otherId,
            receiverId: isCurrentUser ? otherId : userId,
            content: `消息${index + 1}`
          }))
          
          // 多次调用应该得到相同的结果
          const result1 = isInputDisabled(messages, userId)
          const result2 = isInputDisabled(messages, userId)
          const result3 = isInputDisabled(messages, userId)
          
          return result1 === result2 && result2 === result3
        }
      ),
      { numRuns: 100 }
    )
  })

  it('属性：消息内容不影响禁用状态', () => {
    fc.assert(
      fc.property(
        fc.array(fc.boolean(), { minLength: 1, maxLength: 10 }),
        fc.integer({ min: 1, max: 1000 }),
        fc.integer({ min: 1, max: 1000 }),
        fc.array(fc.string({ minLength: 1, maxLength: 100 }), { minLength: 1, maxLength: 10 }),
        (senderPattern, userId, otherId, contents) => {
          fc.pre(userId !== otherId)
          fc.pre(senderPattern.length === contents.length)
          
          // 使用第一组内容
          const messages1 = senderPattern.map((isCurrentUser, index) => ({
            id: index + 1,
            senderId: isCurrentUser ? userId : otherId,
            receiverId: isCurrentUser ? otherId : userId,
            content: contents[index]
          }))
          
          // 使用不同的内容但相同的发送者模式
          const messages2 = senderPattern.map((isCurrentUser, index) => ({
            id: index + 1,
            senderId: isCurrentUser ? userId : otherId,
            receiverId: isCurrentUser ? otherId : userId,
            content: `不同的内容${index}`
          }))
          
          // 两种情况应该得到相同的禁用状态
          return isInputDisabled(messages1, userId) === isInputDisabled(messages2, userId)
        }
      ),
      { numRuns: 100 }
    )
  })

  it('属性：用户ID的对称性 - 从不同用户角度看应该得到正确结果', () => {
    fc.assert(
      fc.property(
        fc.array(fc.boolean(), { minLength: 1, maxLength: 10 }),
        fc.integer({ min: 1, max: 1000 }),
        fc.integer({ min: 1, max: 1000 }),
        (senderPattern, userId, otherId) => {
          fc.pre(userId !== otherId)
          
          const messages = senderPattern.map((isCurrentUser, index) => ({
            id: index + 1,
            senderId: isCurrentUser ? userId : otherId,
            receiverId: isCurrentUser ? otherId : userId,
            content: `消息${index + 1}`
          }))
          
          const disabledForUser = isInputDisabled(messages, userId)
          const disabledForOther = isInputDisabled(messages, otherId)
          
          // 如果最后一条消息是userId发的，那么userId应该被禁用，otherId不应该被禁用
          const lastMsg = messages[messages.length - 1]
          if (lastMsg.senderId === userId) {
            // 检查是否应该禁用userId
            if (messages.length === 1 || (messages.length >= 2 && messages[messages.length - 2].senderId === userId)) {
              return disabledForUser === true && disabledForOther === false
            }
          } else {
            // 最后一条是otherId发的
            if (messages.length === 1 || (messages.length >= 2 && messages[messages.length - 2].senderId === otherId)) {
              return disabledForUser === false && disabledForOther === true
            }
          }
          
          return true
        }
      ),
      { numRuns: 100 }
    )
  })
})
