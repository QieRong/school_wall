import request from '@/api/request'

/**
 * 故事链API模块
 */

// 获取故事列表
export function getStoryList(category, status, page = 1, size = 10) {
  return request({
    url: '/story/list',
    method: 'get',
    params: { category, status, page, size }
  })
}

// 创建故事
export function createStory(data) {
  return request({
    url: '/story/create',
    method: 'post',
    data
  })
}

// 获取故事详情
export function getStoryDetail(id, userId) {
  return request({
    url: `/story/${id}`,
    method: 'get',
    params: { userId }
  })
}

// 检查续写资格
export function checkCanContinue(storyId, userId) {
  return request({
    url: `/story/${storyId}/can-continue`,
    method: 'get',
    params: { userId }
  })
}

// 续写故事
export function continueStory(storyId, data) {
  return request({
    url: `/story/${storyId}/continue`,
    method: 'post',
    data
  })
}

// 获取续写锁
export function acquireStoryLock(storyId, userId) {
  return request({
    url: `/story/${storyId}/lock`,
    method: 'post',
    params: { userId }
  })
}

// 释放续写锁
export function releaseStoryLock(storyId, userId) {
  return request({
    url: `/story/${storyId}/unlock`,
    method: 'post',
    params: { userId }
  })
}

// 续写锁心跳
export function heartbeatStoryLock(storyId, userId) {
  return request({
    url: `/story/${storyId}/heartbeat`,
    method: 'post',
    params: { userId }
  })
}

// 段落点赞/取消点赞
export function toggleLike(paragraphId, userId) {
  return request({
    url: `/story/paragraph/${paragraphId}/like`,
    method: 'post',
    params: { userId }
  })
}


// 搜索故事
export function searchStory(keyword, category, page = 1, size = 10) {
  return request({
    url: '/story/search',
    method: 'get',
    params: { keyword, category, page, size }
  })
}

// 收藏/取消收藏故事
export function toggleFavorite(storyId, userId) {
  return request({
    url: `/story/${storyId}/favorite`,
    method: 'post',
    params: { userId }
  })
}

// 发起完结投票
export function initiateFinishVote(storyId, userId) {
  return request({
    url: `/story/${storyId}/finish-vote`,
    method: 'post',
    params: { userId }
  })
}

// 投票完结
export function voteFinish(storyId, userId, agree) {
  return request({
    url: `/story/${storyId}/vote-finish`,
    method: 'post',
    params: { userId, agree }
  })
}

// 获取我创建的故事
export function getMyCreatedStories(userId, page = 1, size = 10) {
  return request({
    url: '/story/my/created',
    method: 'get',
    params: { userId, page, size }
  })
}

// 获取我参与的故事
export function getMyParticipatedStories(userId, page = 1, size = 10) {
  return request({
    url: '/story/my/participated',
    method: 'get',
    params: { userId, page, size }
  })
}

// 获取我的段落
export function getMyParagraphs(userId) {
  return request({
    url: '/story/my/paragraphs',
    method: 'get',
    params: { userId }
  })
}

// 获取我的成就
export function getMyAchievements(userId) {
  return request({
    url: '/story/my/achievements',
    method: 'get',
    params: { userId }
  })
}

// 获取我的收藏
export function getMyFavorites(userId, page = 1, size = 10) {
  return request({
    url: '/story/my/favorites',
    method: 'get',
    params: { userId, page, size }
  })
}

// 获取贡献度排行
export function getContributionRank(storyId) {
  return request({
    url: `/story/${storyId}/contribution-rank`,
    method: 'get'
  })
}

// 删除故事
export function deleteStory(id, userId) {
  return request({
    url: `/story/${id}`,
    method: 'delete',
    params: { userId }
  })
}

// 获取档案馆故事列表
export function getArchiveList(page = 1, size = 10) {
  return request({
    url: '/story/archive',
    method: 'get',
    params: { page, size }
  })
}

// 更新阅读进度
export function updateReadProgress(storyId, userId, sequence) {
  return request({
    url: `/story/${storyId}/read-progress`,
    method: 'post',
    params: { userId, sequence }
  })
}

// ========== 管理员API ==========

// 管理员获取故事列表
export function getAdminStoryList(status, category, page = 1, size = 10) {
  return request({
    url: '/admin/story/list',
    method: 'get',
    params: { status, category, page, size }
  })
}

// 管理员删除故事
export function adminDeleteStory(id) {
  return request({
    url: `/admin/story/${id}`,
    method: 'delete'
  })
}

// 管理员删除段落
export function adminDeleteParagraph(paragraphId) {
  return request({
    url: `/admin/story/paragraph/${paragraphId}`,
    method: 'delete'
  })
}

// 设置/取消官方推荐
export function setRecommend(id, recommend) {
  return request({
    url: `/admin/story/${id}/recommend`,
    method: 'post',
    params: { recommend }
  })
}

// 获取统计数据
export function getStoryStats() {
  return request({
    url: '/admin/story/stats',
    method: 'get'
  })
}
