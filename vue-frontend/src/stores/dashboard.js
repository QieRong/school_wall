import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as dashboardApi from '@/api/dashboard'

export const useDashboardStore = defineStore('dashboard', () => {
  // 健康度评分
  const healthScore = ref(null)
  // 用户统计
  const userStats = ref(null)
  // 用户分层
  const userSegments = ref(null)
  // 用户排行榜
  const userRankings = ref([])
  // 内容统计
  const contentStats = ref(null)
  // 举报统计
  const reportStats = ref(null)
  // 安全警报
  const alerts = ref([])
  // 24小时统计
  const hourlyStats = ref(null)
  // 词云数据
  const wordCloud = ref([])
  // 周对比数据
  const weeklyComparison = ref(null)
  // 实时动态流
  const realtimeFeed = ref([])
  // 加载状态
  const loading = ref(false)

  // 获取健康度评分
  async function fetchHealthScore() {
    try {
      const res = await dashboardApi.getHealthScore()
      if (res.code === '200') healthScore.value = res.data
    } catch (e) { console.error('获取健康度评分失败', e) }
  }

  // 获取用户统计
  async function fetchUserStats() {
    try {
      const res = await dashboardApi.getUserStats()
      if (res.code === '200') userStats.value = res.data
    } catch (e) { console.error('获取用户统计失败', e) }
  }

  // 获取用户分层
  async function fetchUserSegments() {
    try {
      const res = await dashboardApi.getUserSegments()
      if (res.code === '200') userSegments.value = res.data
    } catch (e) { console.error('获取用户分层失败', e) }
  }

  // 获取用户排行榜
  async function fetchUserRankings(type = 'likes') {
    try {
      const res = await dashboardApi.getUserRankings(type)
      if (res.code === '200') userRankings.value = res.data
    } catch (e) { console.error('获取用户排行榜失败', e) }
  }


  // 获取内容统计
  async function fetchContentStats() {
    try {
      const res = await dashboardApi.getContentStats()
      if (res.code === '200') contentStats.value = res.data
    } catch (e) { console.error('获取内容统计失败', e) }
  }

  // 获取举报统计
  async function fetchReportStats() {
    try {
      const res = await dashboardApi.getReportStats()
      if (res.code === '200') reportStats.value = res.data
    } catch (e) { console.error('获取举报统计失败', e) }
  }

  // 获取安全警报
  async function fetchAlerts() {
    try {
      const res = await dashboardApi.getAlerts()
      if (res.code === '200') alerts.value = res.data
    } catch (e) { console.error('获取安全警报失败', e) }
  }

  // 获取24小时统计
  async function fetchHourlyStats() {
    try {
      const res = await dashboardApi.getHourlyStats()
      if (res.code === '200') hourlyStats.value = res.data
    } catch (e) { console.error('获取24小时统计失败', e) }
  }

  // 获取词云数据
  async function fetchWordCloud(days = 7) {
    try {
      const res = await dashboardApi.getWordCloud(days)
      if (res.code === '200') wordCloud.value = res.data
    } catch (e) { console.error('获取词云数据失败', e) }
  }

  // 获取周对比数据
  async function fetchWeeklyComparison() {
    try {
      const res = await dashboardApi.getWeeklyComparison()
      if (res.code === '200') weeklyComparison.value = res.data
    } catch (e) { console.error('获取周对比数据失败', e) }
  }

  // 添加实时动态
  function addRealtimeEvent(event) {
    realtimeFeed.value.unshift(event)
    // 保持列表长度不超过50
    if (realtimeFeed.value.length > 50) {
      realtimeFeed.value.pop()
    }
  }

  // 加载所有数据
  async function fetchAllData() {
    loading.value = true
    await Promise.all([
      fetchHealthScore(),
      fetchUserStats(),
      fetchUserSegments(),
      fetchUserRankings(),
      fetchContentStats(),
      fetchReportStats(),
      fetchAlerts(),
      fetchHourlyStats(),
      fetchWordCloud(),
      fetchWeeklyComparison()
    ])
    loading.value = false
  }

  return {
    healthScore, userStats, userSegments, userRankings,
    contentStats, reportStats, alerts, hourlyStats,
    wordCloud, weeklyComparison, realtimeFeed, loading,
    fetchHealthScore, fetchUserStats, fetchUserSegments, fetchUserRankings,
    fetchContentStats, fetchReportStats, fetchAlerts, fetchHourlyStats,
    fetchWordCloud, fetchWeeklyComparison, addRealtimeEvent, fetchAllData
  }
})
