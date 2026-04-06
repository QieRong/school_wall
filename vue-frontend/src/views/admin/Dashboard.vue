<script setup>
import { ref, onMounted, onUnmounted, reactive } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart, BarChart, GaugeChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent } from 'echarts/components'
import axios from 'axios'
import request from '@/api/request'
import { useDashboardStore } from '@/stores/dashboard'
import { useAppStore } from '@/stores/app'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription } from '@/components/ui/dialog'
import {
  Download, FileText, MessageCircle, TrendingUp, AlertTriangle, BarChart2, RefreshCw
} from 'lucide-vue-next'

// 导入数据大屏组件
import UserRankings from '@/components/dashboard/UserRankings.vue'
import ContentMonitor from '@/components/dashboard/ContentMonitor.vue'
import ReportStats from '@/components/dashboard/ReportStats.vue'
import HourlyHeatmap from '@/components/dashboard/HourlyHeatmap.vue'
import WordCloud from '@/components/dashboard/WordCloud.vue'
import ComparisonCards from '@/components/dashboard/ComparisonCards.vue'

use([CanvasRenderer, LineChart, PieChart, BarChart, GaugeChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent])

// 使用 stores
const dashboardStore = useDashboardStore()
const appStore = useAppStore()

// 自动刷新定时器
let refreshTimer = null

const stats = ref({
  totalUsers: 0,
  todayActiveUsers: 0,
  totalPosts: 0,
  todayNewPosts: 0,
  totalComments: 0,
  todayComments: 0,
  totalLikes: 0,
  pendingReports: 0,
  weeklyGrowth: 0
})
const recentPosts = ref([])
const showExport = ref(false)
const exportForm = reactive({ start: '', end: '' })
const loading = ref(true)
const lastRefreshTime = ref('')

// 初始化数据
const initData = async () => {
  loading.value = true
  try {
    await dashboardStore.fetchAllData()

    const res = await request.get('/admin/dashboard')
    if (res.code === '200') {
      stats.value = { ...stats.value, ...res.data }
    }

    // 只获取已通过的帖子（status=1）
    const postRes = await request.get('/admin/post/list', { params: { pageNum: 1, pageSize: 5, status: 1 } })
    if (postRes.code === '200') {
      recentPosts.value = postRes.data.list
      // 调试：打印实际返回的数据
      console.log('=== 数据大屏帖子数据 ===')
      console.log('返回的帖子数量:', postRes.data.list?.length)
      if (postRes.data.list?.length > 0) {
        console.log('第一条帖子的status:', postRes.data.list[0].status, '类型:', typeof postRes.data.list[0].status)
        console.log('完整数据:', JSON.stringify(postRes.data.list[0], null, 2))
      }
    }

    lastRefreshTime.value = new Date().toLocaleTimeString('zh-CN')
  } catch (e) {
    console.error('加载数据失败', e)
  } finally {
    loading.value = false
  }
}

// 刷新数据
const refreshData = async () => {
  loading.value = true
  try {
    await dashboardStore.fetchAllData()

    // 同时刷新基础统计数据
    const res = await request.get('/admin/dashboard')
    if (res.code === '200') {
      stats.value = { ...stats.value, ...res.data }
    }

    // 只获取已通过的帖子（status=1）
    const postRes = await request.get('/admin/post/list', { params: { pageNum: 1, pageSize: 5, status: 1 } })
    if (postRes.code === '200') recentPosts.value = postRes.data.list

    lastRefreshTime.value = new Date().toLocaleTimeString('zh-CN')
  } catch (e) {
    console.error('刷新数据失败', e)
  } finally {
    loading.value = false
  }
}

// 启动自动刷新（每30秒）
const startAutoRefresh = () => {
  refreshTimer = setInterval(() => {
    refreshData()
  }, 30000)
}

// 停止自动刷新
const stopAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

// 导出 Excel
const handleExport = async () => {
  try {
    console.log('=== 开始导出 ===')
    console.log('导出参数:', { start: exportForm.start, end: exportForm.end })

    // 直接使用axios而不是request,避免响应拦截器破坏blob数据
    const userStr = localStorage.getItem('user')
    const user = userStr ? JSON.parse(userStr) : null
    const token = user?.token

    if (!token) {
      appStore.showToast('请先登录', 'error')
      return
    }

    console.log('Token:', token.substring(0, 20) + '...')

    const response = await axios.get('/api/admin/export', {
      params: {
        startDate: exportForm.start,
        endDate: exportForm.end
      },
      headers: {
        'Authorization': `Bearer ${token}`
      },
      responseType: 'blob' // 接收二进制数据
    })

    console.log('响应状态:', response.status)
    console.log('响应头:', response.headers)
    console.log('响应数据类型:', response.data.constructor.name)
    console.log('响应数据大小:', response.data.size, 'bytes')
    console.log('响应数据类型(type):', response.data.type)

    // 检查是否是错误响应(JSON格式)
    if (response.data.type === 'application/json') {
      const text = await response.data.text()
      console.error('后端返回JSON错误:', text)
      appStore.showToast('导出失败: ' + text, 'error')
      return
    }

    // 获取文件名
    let filename = '表白墙运营数据报表.xlsx'
    const contentDisposition = response.headers?.['content-disposition']
    console.log('Content-Disposition:', contentDisposition)

    if (contentDisposition) {
      const filenameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/)
      if (filenameMatch && filenameMatch[1]) {
        filename = decodeURIComponent(filenameMatch[1].replace(/['"]/g, ''))
        console.log('解析出的文件名:', filename)
      }
    }

    // 创建Blob下载链接
    const blob = new Blob([response.data], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    console.log('创建的Blob大小:', blob.size, 'bytes')
    console.log('创建的Blob类型:', blob.type)

    const downloadUrl = window.URL.createObjectURL(blob)
    console.log('下载URL:', downloadUrl)

    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(downloadUrl)

    console.log('=== 导出完成 ===')
    appStore.showToast('导出成功', 'success')
    showExport.value = false
  } catch (error) {
    console.error('=== 导出失败 ===')
    console.error('错误对象:', error)
    console.error('错误响应:', error.response)
    if (error.response) {
      console.error('响应状态:', error.response.status)
      console.error('响应数据:', error.response.data)
    }
    appStore.showToast('导出失败，请重试', 'error')
  }
}

onMounted(() => {
  initData()
  startAutoRefresh()
})

onUnmounted(() => {
  stopAutoRefresh()
})
</script>

<template>
  <div class="space-y-6 h-full overflow-y-auto custom-scrollbar pb-6">
    <!-- 顶部标题栏 -->
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-2xl font-bold text-gray-800">📊 运营数据大屏</h2>
        <p class="text-sm text-gray-500 mt-1">
          实时监控社区运营状态
          <span v-if="lastRefreshTime" class="ml-2 text-gray-400">
            · 更新于 {{ lastRefreshTime }}
          </span>
        </p>
      </div>
      <div class="flex gap-3">
        <Button variant="outline" @click="refreshData" size="sm"
          class="border-primary text-primary hover:bg-primary/10">
          <RefreshCw class="w-4 h-4 mr-2" /> 刷新数据
        </Button>
        <Button size="sm" class="bg-primary hover:opacity-90 text-white" @click="showExport = true">
          <Download class="w-4 h-4 mr-2" /> 导出报表
        </Button>
      </div>
    </div>

    <!-- 第一行: 核心指标卡片 (4个关键数据) -->
    <ComparisonCards />

    <!-- 第二行: 趋势图表 (2列布局) -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <!-- 左侧: 24小时活跃趋势 -->
      <Card class="border-t-4 border-primary">
        <CardHeader class="pb-3">
          <CardTitle class="text-base flex items-center gap-2">
            <BarChart2 class="w-5 h-5 text-primary" /> 24小时活跃趋势
          </CardTitle>
        </CardHeader>
        <CardContent class="h-[280px]">
          <HourlyHeatmap />
        </CardContent>
      </Card>

      <!-- 右侧: 热门词云 -->
      <Card class="border-t-4 border-pink-500">
        <CardHeader class="pb-3">
          <CardTitle class="text-base flex items-center gap-2">
            <MessageCircle class="w-5 h-5 text-pink-500" /> 热门话题词云
          </CardTitle>
        </CardHeader>
        <CardContent class="h-[280px]">
          <WordCloud />
        </CardContent>
      </Card>
    </div>

    <!-- 第三行: 快捷入口区域 (3列布局) -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
      <!-- 紧急举报处理 -->
      <Card class="border-t-4 border-red-500 hover:shadow-lg transition-shadow cursor-pointer"
        @click="$router.push('/admin/report')">
        <CardHeader class="pb-3">
          <CardTitle class="text-base flex items-center justify-between">
            <div class="flex items-center gap-2">
              <AlertTriangle class="w-5 h-5 text-red-500" />
              <span>紧急举报</span>
            </div>
            <Badge class="bg-red-500 text-white">{{ stats.pendingReports || 0 }}</Badge>
          </CardTitle>
        </CardHeader>
        <CardContent>
          <ReportStats />
        </CardContent>
      </Card>

      <!-- 内容审核 -->
      <Card class="border-t-4 border-blue-500 hover:shadow-lg transition-shadow cursor-pointer"
        @click="$router.push('/admin/audit')">
        <CardHeader class="pb-3">
          <CardTitle class="text-base flex items-center justify-between">
            <div class="flex items-center gap-2">
              <FileText class="w-5 h-5 text-blue-500" />
              <span>待审核内容</span>
            </div>
            <Badge class="bg-blue-500 text-white">{{ stats.pendingAudit || 0 }}</Badge>
          </CardTitle>
        </CardHeader>
        <CardContent>
          <ContentMonitor />
        </CardContent>
      </Card>

      <!-- 用户排行榜 -->
      <Card class="border-t-4 border-amber-500 hover:shadow-lg transition-shadow">
        <CardHeader class="pb-3">
          <CardTitle class="text-base flex items-center gap-2">
            <TrendingUp class="w-5 h-5 text-amber-500" /> 用户排行榜
          </CardTitle>
        </CardHeader>
        <CardContent class="h-[480px]">
          <UserRankings />
        </CardContent>
      </Card>
    </div>

    <!-- 第四行: 最新动态 (精简版 - 只显示5条) -->
    <Card>
      <CardHeader class="pb-3">
        <div class="flex items-center justify-between">
          <CardTitle class="text-base flex items-center gap-2">
            📝 最新发布动态
          </CardTitle>
          <div class="flex items-center gap-3">
            <Badge variant="outline" class="text-xs">最近5条</Badge>
            <Button variant="ghost" size="sm" class="text-primary hover:bg-primary/10 text-sm h-8"
              @click="$router.push('/admin/audit')">
              查看全部 →
            </Button>
          </div>
        </div>
      </CardHeader>
      <CardContent class="p-0">
        <div class="divide-y">
          <div v-for="post in recentPosts" :key="post.id"
            class="flex items-center gap-4 p-4 hover:bg-gray-50 transition cursor-pointer"
            @click="$router.push('/admin/audit')">
            <img :src="post.avatar || '/default.png'" alt="User Avatar"
              class="w-10 h-10 rounded-full border-2 border-gray-200 flex-shrink-0" />
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 mb-1">
                <span class="text-sm font-medium text-gray-800">{{ post.nickname }}</span>
                <span class="text-xs text-gray-400">{{ post.createTime }}</span>
              </div>
              <div class="text-sm text-gray-600 truncate">{{ post.content }}</div>
            </div>
            <Badge :class="{
              'bg-green-100 text-green-700 border-none': post.status === 1 || post.status === true,
              'bg-yellow-100 text-yellow-700 border-none': post.status === 0 || post.status === false,
              'bg-red-100 text-red-700 border-none': post.status === 2
            }" class="text-xs flex-shrink-0 whitespace-nowrap">
              {{ (post.status === 1 || post.status === true) ? '已发布' : (post.status === 0 || post.status === false) ?
              '待审核' : '已拒绝' }}
            </Badge>
          </div>
          <div v-if="recentPosts.length === 0" class="p-8 text-center text-gray-400">
            <FileText class="w-12 h-12 mx-auto mb-2 opacity-30" />
            <div class="text-sm">暂无最新动态</div>
          </div>
        </div>
      </CardContent>
    </Card>

    <!-- 导出弹窗 -->
    <Dialog :open="showExport" @update:open="showExport = $event">
      <DialogContent aria-describedby="export-dialog-description">
        <DialogHeader>
          <DialogTitle>导出运营数据</DialogTitle>
          <DialogDescription id="export-dialog-description">
            选择日期范围导出Excel报表
          </DialogDescription>
        </DialogHeader>
        <div class="py-4 space-y-4">
          <div class="flex flex-col gap-2">
            <label class="text-sm font-medium text-gray-700">开始日期</label>
            <input type="datetime-local" v-model="exportForm.start"
              class="border p-2 rounded-md text-sm focus:border-primary focus:ring-primary focus:outline-none" />
          </div>
          <div class="flex flex-col gap-2">
            <label class="text-sm font-medium text-gray-700">结束日期</label>
            <input type="datetime-local" v-model="exportForm.end"
              class="border p-2 rounded-md text-sm focus:border-primary focus:ring-primary focus:outline-none" />
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" @click="showExport = false">取消</Button>
          <Button @click="handleExport" class="bg-primary hover:opacity-90 text-white">确认导出</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 3px;
}

.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: #9ca3af;
}
</style>
