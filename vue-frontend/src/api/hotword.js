import request from './request'

// 投稿热词
export function createHotword(data, userId) {
  return request.post(`/hotword/create?userId=${userId}`, data)
}

// 投票
export function voteHotword(id, userId, count = 1) {
  return request.post(`/hotword/vote/${id}?userId=${userId}&count=${count}`)
}

// 获取热词列表
export function getHotwordList(status = 1) {
  return request.get('/hotword/list', { params: { status } })
}

// 获取榜单
export function getRanking(type = 'day') {
  return request.get('/hotword/ranking', { params: { type } })
}

// 获取热词详情
export function getHotwordDetail(id, trendHours = 24) {
  return request.get(`/hotword/${id}`, { params: { trendHours } })
}

// 获取我的投稿
export function getMyPosts(userId, pageNum = 1, pageSize = 10) {
  return request.get('/hotword/my/posts', { params: { userId, pageNum, pageSize } })
}

// 获取我的投票记录
export function getMyVotes(userId, pageNum = 1, pageSize = 10) {
  return request.get('/hotword/my/votes', { params: { userId, pageNum, pageSize } })
}

// 获取剩余票数
export function getRemainingQuota(userId) {
  return request.get('/hotword/my/quota', { params: { userId } })
}

// 删除我的热词
export function deleteHotword(id, userId) {
  return request.delete(`/hotword/${id}?userId=${userId}`)
}

// 搜索热词
export function searchHotword(keyword) {
  return request.get('/hotword/search', { params: { keyword } })
}

// ========== 管理员接口 ==========

// 管理员获取热词列表
export function getAdminHotwordList(params) {
  return request.get('/admin/hotword/list', { params })
}

// 管理员删除热词
export function adminDeleteHotword(id) {
  return request.delete(`/admin/hotword/${id}`)
}

// 设置官方推荐
export function setRecommend(id, recommend) {
  return request.post(`/admin/hotword/${id}/recommend?recommend=${recommend}`)
}

// 获取统计数据
export function getHotwordStats() {
  return request.get('/admin/hotword/stats')
}

// 获取异常投票预警列表
export function getAbnormalVoters(minVotes = 10, hours = 24) {
  return request.get('/admin/hotword/abnormal-voters', { params: { minVotes, hours } })
}
