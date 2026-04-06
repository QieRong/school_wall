<template>
  <div class="p-6 space-y-6">
    <!-- 页面标题 -->
    <div>
      <h2 class="text-2xl font-bold text-gray-800 flex items-center gap-2">
        <Flame class="w-6 h-6 text-orange-600" /> 热词墙管理
      </h2>
      <p class="text-sm text-gray-500 mt-1">管理热词投票和推荐</p>
    </div>

    <!-- 统计卡片 -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
      <AdminCard title="总热词数" :value="stats.totalHotwords || 0" :icon="Hash" icon-bg="bg-purple-100"
        icon-color="text-purple-600" />
      <AdminCard title="今日新增" :value="stats.todayNew || 0" :icon="TrendingUp" icon-bg="bg-green-100"
        icon-color="text-green-600" />
      <AdminCard title="总投票数" :value="stats.totalVotes || 0" :icon="ThumbsUp" icon-bg="bg-blue-100"
        icon-color="text-blue-600" />
      <AdminCard title="今日活跃" :value="stats.activeUsers || 0" :icon="Users" icon-bg="bg-amber-100"
        icon-color="text-amber-600" />
    </div>

    <!-- 异常投票预警 -->
    <div v-if="abnormalVoters.length" class="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
      <h3 class="text-yellow-800 font-semibold mb-3 flex items-center gap-2">
        <AlertTriangle class="w-5 h-5" /> 异常投票预警
      </h3>
      <div class="space-y-2">
        <div v-for="voter in abnormalVoters" :key="voter.userId"
          class="flex items-center gap-3 bg-white rounded-lg p-3 border border-yellow-100">
          <img :src="voter.avatar || '/default.png'" class="w-8 h-8 rounded-full border" />
          <span class="font-medium min-w-[100px]">{{ voter.nickname || '用户' + voter.userId }}</span>
          <span class="text-red-600 font-bold">{{ voter.voteCount }}次投票 / {{ voter.totalVotes }}票</span>
          <span class="text-xs text-gray-500 ml-auto">{{ formatTime(voter.firstVoteTime) }} - {{
            formatTime(voter.lastVoteTime) }}</span>
        </div>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="bg-white rounded-lg border border-gray-200 p-4 flex items-center gap-4">
      <div class="flex-1">
        <label class="text-sm font-medium text-gray-700 mb-1 block">状态筛选</label>
        <select v-model="filter.status" @change="loadData(1)"
          class="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary">
          <option :value="null">全部状态</option>
          <option :value="1">活跃</option>
          <option :value="2">已归档</option>
        </select>
      </div>
      <div class="flex gap-2 items-end">
        <Button variant="outline" @click="loadData(pageNum)">
          <RefreshCw class="w-4 h-4 mr-2" /> 刷新
        </Button>
        <Button variant="outline" @click="loadAbnormalVoters"
          class="border-yellow-500 text-yellow-700 hover:bg-yellow-50">
          <AlertTriangle class="w-4 h-4 mr-2" /> 检测异常
        </Button>
      </div>
    </div>

    <!-- 表格 -->
    <AdminTable :columns="columns" :data="hotwords" :loading="loading" :pagination="pagination"
      @page-change="handlePageChange">
      <!-- 热词列 -->
      <template #name="{ row }">
        <div class="flex items-center gap-2 min-w-0">
          <span class="font-semibold text-gray-800 truncate">{{ row.name }}</span>
          <Badge v-if="row.isRecommended"
            class="bg-gradient-to-r from-yellow-400 to-orange-500 text-white border-none flex-shrink-0">
            推荐
          </Badge>
        </div>
      </template>

      <!-- 释义列 -->
      <template #definition="{ row }">
        <p class="max-w-xs truncate text-gray-600 break-words overflow-hidden">{{ row.definition }}</p>
      </template>

      <!-- 票数列 -->
      <template #votes="{ row }">
        <div class="flex items-center gap-2">
          <span class="font-bold text-gray-800">{{ row.totalVotes }}</span>
          <Badge :class="getHeatClass(row.heatLevel)">{{ row.heatLevel }}</Badge>
        </div>
      </template>

      <!-- 状态列 -->
      <template #status="{ row }">
        <Badge :class="row.status === 1 ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'">
          {{ row.status === 1 ? '活跃' : '归档' }}
        </Badge>
      </template>

      <!-- 操作列 -->
      <template #actions="{ row }">
        <div class="flex items-center gap-1 flex-wrap justify-end">
          <Button v-if="!row.isRecommended" size="sm" variant="ghost" @click="handleRecommend(row, true)" title="设为推荐"
            class="whitespace-nowrap">
            <Star class="w-4 h-4" />
          </Button>
          <Button v-else size="sm" variant="ghost" class="text-yellow-600 whitespace-nowrap"
            @click="handleRecommend(row, false)" title="取消推荐">
            <StarOff class="w-4 h-4" />
          </Button>
          <Button size="sm" variant="ghost" class="text-red-500 hover:text-red-600 hover:bg-red-50 whitespace-nowrap"
            @click="handleDelete(row)" title="删除">
            <Trash2 class="w-4 h-4" />
          </Button>
        </div>
      </template>
    </AdminTable>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Flame, Hash, TrendingUp, ThumbsUp, Users, AlertTriangle, RefreshCw, Star, StarOff, Trash2 } from 'lucide-vue-next'
import { ElMessageBox } from 'element-plus'
import { getAdminHotwordList, adminDeleteHotword, setRecommend, getHotwordStats, getAbnormalVoters } from '@/api/hotword'
import { useAppStore } from '@/stores/app'
import AdminTable from '@/components/admin/AdminTable.vue'
import AdminCard from '@/components/admin/AdminCard.vue'

const appStore = useAppStore()
const hotwords = ref([])
const stats = ref({})
const abnormalVoters = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const filter = reactive({ status: null })

// 表格列配置
const columns = [
  { key: 'id', label: 'ID' },
  { key: 'name', label: '热词', slot: 'name' },
  { key: 'definition', label: '释义', slot: 'definition' },
  { key: 'votes', label: '票数', slot: 'votes', sortable: true },
  { key: 'status', label: '状态', slot: 'status' },
  { key: 'authorNickname', label: '投稿者' },
  { key: 'createTime', label: '创建时间', sortable: true },
  { key: 'actions', label: '操作', slot: 'actions' }
]

// 分页配置
const pagination = computed(() => ({
  current: pageNum.value,
  pageSize: pageSize.value,
  total: total.value
}))

const getHeatClass = (level) => {
  const map = {
    '新芽': 'bg-green-100 text-green-700',
    '升温': 'bg-orange-100 text-orange-700',
    '火爆': 'bg-red-100 text-red-700',
    '现象级': 'bg-pink-100 text-pink-700'
  }
  return map[level] || 'bg-gray-100 text-gray-500'
}

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

const loadData = async (page = 1) => {
  loading.value = true
  pageNum.value = page
  try {
    const res = await getAdminHotwordList({
      status: filter.status,
      pageNum: page,
      pageSize: pageSize.value
    })
    hotwords.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

const loadStats = async () => {
  const res = await getHotwordStats()
  stats.value = res.data || {}
}

const handleRecommend = async (item, recommend) => {
  try {
    await setRecommend(item.id, recommend)
    item.isRecommended = recommend
    appStore.showToast('操作成功', 'success')
  } catch (e) {
    appStore.showToast('操作失败', 'error')
  }
}

const handleDelete = async (item) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除热词"${item.name}"吗？此操作将同时删除所有投票记录。`,
      '删除确认',
      { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
    )
    await adminDeleteHotword(item.id)
    loadData(pageNum.value)
    loadStats()
    appStore.showToast('删除成功', 'success')
  } catch (e) {
    // 用户取消
  }
}

const loadAbnormalVoters = async () => {
  try {
    const res = await getAbnormalVoters(10, 24)
    abnormalVoters.value = res.data || []
    if (abnormalVoters.value.length === 0) {
      appStore.showToast('未检测到异常投票行为', 'info')
    }
  } catch (e) {
    appStore.showToast('检测失败', 'error')
  }
}

const handlePageChange = (page) => {
  loadData(page)
}

onMounted(() => {
  loadData()
  loadStats()
  loadAbnormalVoters()
})
</script>
