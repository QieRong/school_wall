<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useDashboardStore } from '@/stores/dashboard'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { RefreshCw, Activity, Users, FileText, AlertTriangle, Clock, BarChart2, Cloud, TrendingUp, Download, Calendar } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'

// 导入大屏组件
import HealthGauge from '@/components/dashboard/HealthGauge.vue'
import RealtimeFeed from '@/components/dashboard/RealtimeFeed.vue'
import UserStatsPanel from '@/components/dashboard/UserStatsPanel.vue'
import UserSegments from '@/components/dashboard/UserSegments.vue'
import UserRankings from '@/components/dashboard/UserRankings.vue'
import ContentMonitor from '@/components/dashboard/ContentMonitor.vue'
import ReportStats from '@/components/dashboard/ReportStats.vue'
import AlertPanel from '@/components/dashboard/AlertPanel.vue'
import HourlyHeatmap from '@/components/dashboard/HourlyHeatmap.vue'
import WordCloud from '@/components/dashboard/WordCloud.vue'
import ComparisonCards from '@/components/dashboard/ComparisonCards.vue'

const dashboardStore = useDashboardStore()
let refreshTimer = null

const startDate = ref('')
const endDate = ref('')
const showDatePicker = ref(false)

const refreshData = () => dashboardStore.fetchAllData()

const startAutoRefresh = () => {
  refreshTimer = setInterval(refreshData, 30000)
}

const stopAutoRefresh = () => {
  if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null }
}

// 导出数据函数
const exportData = (type) => {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:19090'
  let url = `${baseUrl}/admin/export`

  if (type === 'new-users') url += '/new-users'
  else if (type === 'user-activity') url += '/user-activity'
  else if (type === 'hotword') url += '/hotword'
  else if (type === 'bottle') url += '/bottle'
  else if (type === 'story') url += '/story'
  else if (type === 'comprehensive') url += '/comprehensive'

  // 添加日期参数
  const params = new URLSearchParams()
  if (startDate.value) {
    params.append('startDate', startDate.value.replace('T', ' ') + ':00')
  }
  if (endDate.value) {
    params.append('endDate', endDate.value.replace('T', ' ') + ':00')
  }

  const queryString = params.toString()
  if (queryString) url += '?' + queryString

  // 获取 token
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.error('请先登录')
    return
  }

  // 使用 fetch 下载文件
  ElMessage.info('正在导出数据，请稍候...')

  fetch(url, {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`
    }
  })
    .then(response => {
      if (!response.ok) {
        throw new Error('导出失败')
      }

      // 从响应头获取文件名
      const contentDisposition = response.headers.get('content-disposition')
      let fileName = '数据报表.xlsx'
      if (contentDisposition) {
        const fileNameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/)
        if (fileNameMatch && fileNameMatch[1]) {
          fileName = decodeURIComponent(fileNameMatch[1].replace(/['"]/g, ''))
        }
      }

      return response.blob().then(blob => ({ blob, fileName }))
    })
    .then(({ blob, fileName }) => {
      // 创建下载链接
      const downloadUrl = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = downloadUrl
      a.download = fileName
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      window.URL.revokeObjectURL(downloadUrl)

      ElMessage.success('导出成功！')
    })
    .catch(error => {
      console.error('导出失败:', error)
      ElMessage.error('导出失败，请重试')
    })
}

// 清除日期范围
const clearDateRange = () => {
  startDate.value = ''
  endDate.value = ''
}

onMounted(() => {
  dashboardStore.fetchAllData()
  startAutoRefresh()
})

onUnmounted(() => stopAutoRefresh())
</script>

<template>
  <div class="space-y-3 h-full overflow-y-auto pr-2 custom-scrollbar pb-6">
    <!-- 顶部标题栏 -->
    <div
      class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3 sticky top-0 bg-white z-10 pb-3 pt-1">
      <div>
        <h2 class="text-xl sm:text-2xl font-bold text-gray-800">📊 运营数据大屏</h2>
        <p class="text-xs sm:text-sm text-gray-500 mt-1">实时监控社区运营状态 · 每30秒自动刷新</p>
      </div>
      <Button class="bg-[rgb(33,111,85)] hover:bg-[rgb(28,95,72)] text-white text-sm" @click="refreshData">
        <RefreshCw class="w-4 h-4 mr-2" /> 刷新数据
      </Button>
    </div>

    <!-- 周对比卡片 -->
    <ComparisonCards />

    <!-- 第一行：健康度 + 实时动态 + 警报 -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-3">
      <Card class="flex flex-col">
        <CardHeader class="pb-2">
          <CardTitle class="text-sm sm:text-base flex items-center gap-2">
            <Activity class="w-4 h-4 text-emerald-600" /> 平台健康度
          </CardTitle>
        </CardHeader>
        <CardContent class="flex-1 h-[280px]">
          <HealthGauge />
        </CardContent>
      </Card>

      <Card class="flex flex-col">
        <CardHeader class="pb-2">
          <CardTitle class="text-sm sm:text-base flex items-center gap-2">
            <Clock class="w-4 h-4 text-blue-600" /> 实时动态
          </CardTitle>
        </CardHeader>
        <CardContent class="flex-1 h-[280px] overflow-hidden">
          <RealtimeFeed />
        </CardContent>
      </Card>

      <Card class="flex flex-col">
        <CardHeader class="pb-2">
          <CardTitle class="text-sm sm:text-base flex items-center gap-2">
            <AlertTriangle class="w-4 h-4 text-red-600" /> 安全警报
          </CardTitle>
        </CardHeader>
        <CardContent class="flex-1 h-[280px] overflow-hidden">
          <AlertPanel />
        </CardContent>
      </Card>
    </div>

    <!-- 第二行：用户统计 + 用户分层 + 用户排行 -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-3">
      <Card class="flex flex-col">
        <CardHeader class="pb-2">
          <CardTitle class="text-sm sm:text-base flex items-center gap-2">
            <Users class="w-4 h-4 text-emerald-600" /> 用户统计
          </CardTitle>
        </CardHeader>
        <CardContent class="h-[280px]">
          <UserStatsPanel />
        </CardContent>
      </Card>

      <Card class="flex flex-col">
        <CardHeader class="pb-2">
          <CardTitle class="text-sm sm:text-base flex items-center gap-2">
            <Users class="w-4 h-4 text-purple-600" /> 用户分层
          </CardTitle>
        </CardHeader>
        <CardContent class="h-[280px]">
          <UserSegments />
        </CardContent>
      </Card>

      <Card class="flex flex-col">
        <CardHeader class="pb-2">
          <CardTitle class="text-sm sm:text-base flex items-center gap-2">
            <TrendingUp class="w-4 h-4 text-yellow-600" /> 用户排行榜
          </CardTitle>
        </CardHeader>
        <CardContent class="h-[280px]">
          <UserRankings />
        </CardContent>
      </Card>
    </div>

    <!-- 第三行：内容监控 + 举报统计 -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-3">
      <Card class="flex flex-col">
        <CardHeader class="pb-2">
          <CardTitle class="text-sm sm:text-base flex items-center gap-2">
            <FileText class="w-4 h-4 text-blue-600" /> 内容审核监控
          </CardTitle>
        </CardHeader>
        <CardContent class="flex-1 h-[280px]">
          <ContentMonitor />
        </CardContent>
      </Card>

      <Card class="flex flex-col">
        <CardHeader class="pb-2">
          <CardTitle class="text-sm sm:text-base flex items-center gap-2">
            <AlertTriangle class="w-4 h-4 text-orange-600" /> 举报统计
          </CardTitle>
        </CardHeader>
        <CardContent class="flex-1 h-[280px]">
          <ReportStats />
        </CardContent>
      </Card>
    </div>

    <!-- 第四行：24小时热力曲线 -->
    <Card class="flex flex-col">
      <CardHeader class="pb-2">
        <CardTitle class="text-sm sm:text-base flex items-center gap-2">
          <BarChart2 class="w-4 h-4 text-indigo-600" /> 24小时互动热力曲线
        </CardTitle>
      </CardHeader>
      <CardContent class="flex-1 h-[280px]">
        <HourlyHeatmap />
      </CardContent>
    </Card>

    <!-- 第五行：词云 -->
    <Card class="flex flex-col">
      <CardHeader class="pb-2">
        <CardTitle class="text-sm sm:text-base flex items-center gap-2">
          <Cloud class="w-4 h-4 text-pink-600" /> 热门话题词云
        </CardTitle>
      </CardHeader>
      <CardContent class="flex-1 h-[280px]">
        <WordCloud />
      </CardContent>
    </Card>

    <!-- 第六行：数据导出 -->
    <Card class="flex flex-col">
      <CardHeader class="pb-2">
        <CardTitle class="text-sm sm:text-base flex items-center gap-2">
          <Download class="w-4 h-4 text-emerald-600" /> 数据导出
        </CardTitle>
      </CardHeader>
      <CardContent class="space-y-4">
        <!-- 日期范围选择器 -->
        <div class="flex flex-col sm:flex-row items-start sm:items-center gap-3 p-3 bg-gray-50 rounded-lg">
          <div class="flex items-center gap-2">
            <Calendar class="w-4 h-4 text-gray-500" />
            <span class="text-sm font-medium text-gray-700">时间范围：</span>
          </div>
          <div class="flex flex-col sm:flex-row items-start sm:items-center gap-2 flex-1">
            <input type="datetime-local" v-model="startDate"
              class="px-3 py-1.5 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-emerald-500"
              placeholder="开始时间" />
            <span class="text-gray-500 text-sm">至</span>
            <input type="datetime-local" v-model="endDate"
              class="px-3 py-1.5 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-emerald-500"
              placeholder="结束时间" />
            <Button variant="outline" size="sm" @click="clearDateRange" class="text-xs">
              清除
            </Button>
          </div>
          <p class="text-xs text-gray-500 w-full sm:w-auto">
            不选择时间则导出全部数据
          </p>
        </div>

        <!-- 导出按钮组 -->
        <div class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-2">
          <Button @click="exportData('posts')" variant="outline"
            class="flex items-center justify-center gap-2 text-sm hover:bg-blue-50 hover:border-blue-300">
            <FileText class="w-4 h-4" />
            导出帖子
          </Button>

          <Button @click="exportData('new-users')" variant="outline"
            class="flex items-center justify-center gap-2 text-sm hover:bg-green-50 hover:border-green-300">
            <Users class="w-4 h-4" />
            新注册用户
          </Button>

          <Button @click="exportData('user-activity')" variant="outline"
            class="flex items-center justify-center gap-2 text-sm hover:bg-purple-50 hover:border-purple-300">
            <Activity class="w-4 h-4" />
            用户活跃度
          </Button>

          <Button @click="exportData('hotword')" variant="outline"
            class="flex items-center justify-center gap-2 text-sm hover:bg-pink-50 hover:border-pink-300">
            <Cloud class="w-4 h-4" />
            热词数据
          </Button>

          <Button @click="exportData('bottle')" variant="outline"
            class="flex items-center justify-center gap-2 text-sm hover:bg-cyan-50 hover:border-cyan-300">
            <Download class="w-4 h-4" />
            漂流瓶
          </Button>

          <Button @click="exportData('story')" variant="outline"
            class="flex items-center justify-center gap-2 text-sm hover:bg-yellow-50 hover:border-yellow-300">
            <FileText class="w-4 h-4" />
            故事接龙
          </Button>

          <Button @click="exportData('comprehensive')"
            class="col-span-2 bg-emerald-600 hover:bg-emerald-700 text-white flex items-center justify-center gap-2 text-sm">
            <Download class="w-4 h-4" />
            导出综合报表（推荐）
          </Button>
        </div>

        <!-- 说明文字 -->
        <div class="text-xs text-gray-500 space-y-1 p-3 bg-blue-50 rounded-lg border border-blue-100">
          <p class="font-medium text-blue-700">💡 导出说明：</p>
          <ul class="list-disc list-inside space-y-0.5 text-blue-600">
            <li>综合报表包含所有数据类型，推荐使用</li>
            <li>可选择时间范围导出特定时段的数据</li>
            <li>导出的 Excel 文件包含中文列标题</li>
            <li>大数据量导出可能需要等待几秒钟</li>
          </ul>
        </div>
      </CardContent>
    </Card>
  </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}

.custom-scrollbar::-webkit-scrollbar-track {
  background: #f9fafb;
  border-radius: 3px;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 3px;
}

.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: #9ca3af;
}

/* 确保所有卡片高度一致 */
:deep(.card) {
  transition: all 0.2s ease;
}

/* 响应式优化 */
@media (max-width: 1024px) {
  .space-y-3 {
    gap: 0.5rem;
  }
}
</style>
