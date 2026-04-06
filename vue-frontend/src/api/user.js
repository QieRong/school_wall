// File: vue-frontend/src/api/user.js
import request from '@/api/request'

// 获取用户资料 (含统计数据 + 自动记录访客)
export function getUserProfile(targetId, currentId) {
  return request.get('/user/profile', { 
    params: { targetId, currentId } 
  })
}

// 更新资料 (昵称、头像、封面)
export function updateUserProfile(data) {
  return request.post('/user/update', data)
}

// 获取访客列表
export function getVisitors(userId) {
  return request.get('/user/visitors', { 
    params: { userId } 
  })
}

// 获取指定用户的帖子列表
// 注意：复用 PostController 的 list 接口，后端需支持按 userId 筛选
export function getUserPosts(targetUserId, currentUserId = 0, pageNum = 1, pageSize = 10) {
  return request.get('/post/list', {
    params: { userId: targetUserId, currentUserId, pageNum, pageSize }
  })
}