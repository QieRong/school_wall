// File: vue-frontend/src/api/comment.js
import request from '@/api/request'

// 获取评论列表（平铺结构，前端构建树）
export function getComments(postId) {
  return request.get('/comment/list', { params: { postId } })
}

// 【优化】获取评论树形结构（后端构建树）
export function getCommentTree(postId) {
  return request.get('/comment/tree', { params: { postId } })
}

export function sendComment(data) {
  return request.post('/comment/create', data)
}