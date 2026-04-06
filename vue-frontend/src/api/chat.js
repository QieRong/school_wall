// File: vue-frontend/src/api/chat.js
import request from '@/api/request'

// 获取联系人列表
export function getContacts(userId) {
  return request.get('/chat/contacts', { params: { userId } })
}

// 获取聊天记录
export function getHistory(userId, targetId) {
  return request.get('/chat/history', { params: { userId, targetId } })
}

// 发送消息
export function sendMessage(data) {
  return request.post('/chat/send', data)
}

// 撤回消息
export function withdrawMessage(id, userId) {
  return request.post('/chat/withdraw', null, { params: { id, userId } })
}