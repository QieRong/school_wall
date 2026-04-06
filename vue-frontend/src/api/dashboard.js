import request from './request'

// 获取平台健康度评分
export function getHealthScore() {
  return request.get('/dashboard/health-score')
}

// 获取用户统计数据
export function getUserStats() {
  return request.get('/dashboard/user-stats')
}

// 获取用户分层数据
export function getUserSegments() {
  return request.get('/dashboard/user-segments')
}

// 获取用户排行榜
export function getUserRankings(type = 'likes') {
  return request.get('/dashboard/user-rankings', { params: { type } })
}

// 获取内容统计数据
export function getContentStats() {
  return request.get('/dashboard/content-stats')
}

// 获取举报统计数据
export function getReportStats() {
  return request.get('/dashboard/report-stats')
}

// 获取安全警报列表
export function getAlerts() {
  return request.get('/dashboard/alerts')
}

// 获取24小时统计数据
export function getHourlyStats() {
  return request.get('/dashboard/hourly-stats')
}

// 获取词云数据
export function getWordCloud(days = 7) {
  return request.get('/dashboard/word-cloud', { params: { days } })
}

// 获取周对比数据
export function getWeeklyComparison() {
  return request.get('/dashboard/weekly-comparison')
}
