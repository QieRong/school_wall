// File: vue-frontend/src/api/bottle.js
import request from '@/api/request'

/**
 * 漂流瓶相关API
 */

// 投放漂流瓶
export function throwBottle(data) {
  return request.post('/bottle/throw', data)
}

// 打捞漂流瓶
export function fishBottle(userId) {
  return request.post('/bottle/fish', null, { params: { userId } })
}

// 回复漂流瓶
export function replyBottle(data) {
  return request.post('/bottle/reply', data)
}

// 放回漂流瓶
export function releaseBottle(bottleId) {
  // 确保bottleId是数字类型
  const id = typeof bottleId === 'string' ? parseInt(bottleId) : bottleId
  return request.post(`/bottle/release/${id}`)
}

// 珍藏漂流瓶
export function collectBottle(bottleId, userId) {
  return request.post(`/bottle/collect/${bottleId}`, null, { params: { userId } })
}

// 获取我投放的瓶子
export function getMySentBottles(userId, pageNum = 1, pageSize = 10) {
  return request.get('/bottle/my/sent', { params: { userId, pageNum, pageSize } })
}

// 获取我珍藏的瓶子
export function getMyCollectedBottles(userId, pageNum = 1, pageSize = 10) {
  return request.get('/bottle/my/collected', { params: { userId, pageNum, pageSize } })
}

// 获取瓶子详情
export function getBottleDetail(bottleId) {
  return request.get(`/bottle/detail/${bottleId}`)
}

// 删除我的漂流瓶
export function deleteBottle(bottleId, userId) {
  return request.delete(`/bottle/${bottleId}`, { params: { userId } })
}

// 获取我的成就
export function getAchievements(userId) {
  return request.get('/bottle/achievements', { params: { userId } })
}

// ========== 管理员接口 ==========

// 获取统计数据
export function getBottleStats() {
  return request.get('/bottle/admin/stats')
}

// 获取瓶子列表（管理员）
export function getAdminBottleList(params) {
  return request.get('/bottle/admin/list', { params })
}

// 删除瓶子（管理员）
export function adminDeleteBottle(bottleId) {
  return request.delete(`/bottle/admin/${bottleId}`)
}
