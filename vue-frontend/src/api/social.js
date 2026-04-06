// File: vue-frontend/src/api/social.js
import request from '@/api/request'

// --- 关注 ---
export function followUser(data) {
  return request.post('/social/follow', data)
}

export function unfollowUser(data) {
  return request.post('/social/unfollow', data)
}

export function getFollowings(userId) {
  return request.get('/social/followings', { params: { userId } })
}

export function getFollowers(userId) {
  return request.get('/social/followers', { params: { userId } })
}

export function checkFollowStatus(userId, targetId) {
  return request.get('/social/check_follow', { params: { userId, targetId } })
}

// --- 收藏 ---
export function toggleCollection(data) {
  return request.post('/social/collect', data)
}

export function getMyCollections(userId) {
  return request.get('/social/collections', { params: { userId } })
}

export function checkCollectStatus(userId, postId) {
  return request.get('/social/check_collect', { params: { userId, postId } })
}

// --- 黑名单 ---
export function blockUser(data) {
  return request.post('/social/block', data)
}

export function unblockUser(data) {
  return request.post('/social/unblock', data)
}

export function getBlacklist(userId) {
  return request.get('/social/blacklist', { params: { userId } })
}