<script setup>
import { ref, onMounted, reactive, computed } from 'vue'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { Waves, Eye, Trash2, Users, TrendingUp, Package } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBottleStats, getAdminBottleList, adminDeleteBottle } from '@/api/bottle'
import AdminTable from '@/components/admin/AdminTable.vue'
import AdminCard from '@/components/admin/AdminCard.vue'

const bottles = ref([])
const stats = ref({ total: 0, active: 0, todayNew: 0, totalFish: 0 })
const loading = ref(false)

// 安全的趋势值计算
const todayNewTrend = computed(() => {
  const value = stats.value.todayNew
  return value > 0 ? `+${value}` : null
})

const query = reactive({
  status: null,
  direction: null,
  startDate: '',
  endDate: '',
  pageNum: 1,
  pageSize: 10
})

const showDetailDialog = ref(false)
const currentBottle = ref(null)

// 状态选项
const statusOptions = [
  { value: null, label: '全部状态' },
  { value: 0, label: '漂流中' },
  { value: 1, label: '已被捞起' },
  { value: 2, label: '已被珍藏' },
  { value: 3, label: '已过期' }
]

const directionOptions = [
  { value: null, label: '全部方向' },
  { value: 1, label: '樱花海岸' },
  { value: 2, label: '深海秘境' },
  { value: 3, label: '星辰大海' }
]

// 表格列配置
const columns = [
  { key: 'user', label: '用户', slot: 'user' },
  { key: 'content', label: '内容', slot: 'content' },
  { key: 'direction', label: '方向', slot: 'direction' },
  { key: 'status', label: '状态', slot: 'status' },
  { key: 'viewCount', label: '查看数', sortable: true },
  { key: 'createTime', label: '创建时间', sortable: true },
  { key: 'actions', label: '操作', slot: 'actions' }
]

// 分页配置
const pagination = computed(() => ({
  current: query.pageNum,
  pageSize: query.pageSize,
  total: stats.value.total || 0
}))

// 获取统计数据
const fetchStats = async () => {
  try {
    const res = await getBottleStats()
    if (res.code === '200') {
      stats.value = {
        total: res.data.total || 0,
        active: res.data.active || 0,
        todayNew: res.data.todayNew || 0,
        totalFish: res.data.totalFish || 0
      }
    }
  } catch (e) {
    console.error(e)
    stats.value = { total: 0, active: 0, todayNew: 0, totalFish: 0 }
  }
}

// 获取瓶子列表
const fetchList = async () => {
  loading.value = true
  try {
    const res = await getAdminBottleList(query)
    if (res.code === '200') {
      bottles.value = res.data.list || []
    }
  } catch (e) {
    ElMessage.error('获取列表失败')
  } finally {
    loading.value = false
  }
}

// 删除瓶子
const handleDelete = async (bottle) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除这个漂流瓶吗？此操作将同时删除所有相关回复。`,
      '删除确认',
      { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
    )
    await adminDeleteBottle(bottle.id)
    ElMessage.success('删除成功')
    fetchList()
    fetchStats()
  } catch (e) {
    // 用户取消
  }
}

// 查看详情
const viewDetail = (bottle) => {
  currentBottle.value = bottle
  showDetailDialog.value = true
}

// 筛选变更
const handleFilterChange = () => {
  query.pageNum = 1
  fetchList()
}

// 状态文本
const getStatusText = (status) => {
  const map = { 0: '漂流中', 1: '已被捞起', 2: '已被珍藏', 3: '已过期' }
  return map[status] || '未知'
}

const getStatusClass = (status) => {
  const map = {
    0: 'bg-blue-100 text-blue-700',
    1: 'bg-yellow-100 text-yellow-700',
    2: 'bg-pink-100 text-pink-700',
    3: 'bg-gray-100 text-gray-500'
  }
  return map[status] || 'bg-gray-100 text-gray-500'
}

const getDirectionText = (dir) => {
  const map = { 1: '樱花海岸', 2: '深海秘境', 3: '星辰大海' }
  return map[dir] || '未知'
}

// 分页
const handlePageChange = (page) => {
  query.pageNum = page
  fetchList()
}

onMounted(() => {
  fetchStats()
  fetchList()
})
</script>

<template>
  <div class="p-6 space-y-6">
    <!-- 页面标题 -->
    <div>
      <h2 class="text-2xl font-bold text-gray-800 flex items-center gap-2">
        <Waves class="w-6 h-6 text-sky-600" /> 漂流瓶管理
      </h2>
      <p class="text-sm text-gray-500 mt-1">管理所有漂流瓶内容</p>
    </div>

    <!-- 统计卡片 -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
      <AdminCard title="总漂流瓶" :value="stats.total" :icon="Package" icon-bg="bg-sky-100" icon-color="text-sky-600" />
      <AdminCard title="漂流中" :value="stats.active" :icon="Waves" icon-bg="bg-blue-100" icon-color="text-blue-600" />
      <AdminCard title="今日新增" :value="stats.todayNew || 0" :icon="TrendingUp" icon-bg="bg-green-100"
        icon-color="text-green-600" :trend="todayNewTrend ? 'up' : undefined" :trend-value="todayNewTrend" />
      <AdminCard title="打捞次数" :value="stats.totalFish" :icon="Users" icon-bg="bg-purple-100"
        icon-color="text-purple-600" />
    </div>

    <!-- 筛选栏 -->
    <div class="bg-white rounded-lg border border-gray-200 p-4">
      <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div>
          <label class="text-sm font-medium text-gray-700 mb-1 block">状态</label>
          <select v-model="query.status" @change="handleFilterChange"
            class="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary">
            <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
          </select>
        </div>
        <div>
          <label class="text-sm font-medium text-gray-700 mb-1 block">漂流方向</label>
          <select v-model="query.direction" @change="handleFilterChange"
            class="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary">
            <option v-for="opt in directionOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
          </select>
        </div>
        <div>
          <label class="text-sm font-medium text-gray-700 mb-1 block">开始日期</label>
          <Input type="date" v-model="query.startDate" @change="handleFilterChange" />
        </div>
        <div>
          <label class="text-sm font-medium text-gray-700 mb-1 block">结束日期</label>
          <Input type="date" v-model="query.endDate" @change="handleFilterChange" />
        </div>
      </div>
    </div>

    <!-- 表格 -->
    <AdminTable :columns="columns" :data="bottles" :loading="loading" :pagination="pagination"
      @page-change="handlePageChange">
      <!-- 用户列 -->
      <template #user="{ row }">
        <div class="flex items-center gap-2 min-w-0">
          <img :src="row.avatar || '/default.png'" alt="头像" class="w-8 h-8 rounded-full border flex-shrink-0"
            @error="(e) => e.target.src = '/default.png'" />
          <span class="font-medium text-gray-700 truncate">{{ row.nickname || '匿名' }}</span>
        </div>
      </template>

      <!-- 内容列 -->
      <template #content="{ row }">
        <p class="max-w-xs truncate text-gray-600 break-words overflow-hidden">{{ row.content }}</p>
      </template>

      <!-- 方向列 -->
      <template #direction="{ row }">
        <Badge variant="outline">{{ getDirectionText(row.direction) }}</Badge>
      </template>

      <!-- 状态列 -->
      <template #status="{ row }">
        <Badge :class="getStatusClass(row.status)">{{ getStatusText(row.status) }}</Badge>
      </template>

      <!-- 操作列 -->
      <template #actions="{ row }">
        <div class="flex items-center gap-1 flex-wrap justify-end">
          <Button size="sm" variant="ghost" @click="viewDetail(row)" title="查看详情" class="whitespace-nowrap">
            <Eye class="w-4 h-4" />
          </Button>
          <Button size="sm" variant="ghost" class="text-red-500 hover:text-red-600 hover:bg-red-50 whitespace-nowrap"
            @click="handleDelete(row)" title="删除">
            <Trash2 class="w-4 h-4" />
          </Button>
        </div>
      </template>
    </AdminTable>

    <!-- 详情弹窗 -->
    <Dialog v-model:open="showDetailDialog">
      <DialogContent class="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>漂流瓶详情</DialogTitle>
        </DialogHeader>
        <div v-if="currentBottle" class="py-4 space-y-4">
          <div class="flex items-center gap-3">
            <img :src="currentBottle.avatar || '/default.png'" alt="头像"
              class="w-12 h-12 rounded-full border flex-shrink-0" @error="(e) => e.target.src = '/default.png'" />
            <div class="min-w-0 flex-1">
              <div class="font-medium truncate">{{ currentBottle.nickname || '匿名' }}</div>
              <div class="text-xs text-gray-400 truncate">ID: {{ currentBottle.userId }}</div>
            </div>
          </div>
          <div class="bg-gray-50 rounded-lg p-4">
            <p class="text-gray-700 whitespace-pre-wrap break-words">{{ currentBottle.content }}</p>
          </div>
          <div class="grid grid-cols-2 gap-4 text-sm">
            <div><span class="text-gray-500">状态：</span>
              <Badge :class="getStatusClass(currentBottle.status)">{{ getStatusText(currentBottle.status) }}</Badge>
            </div>
            <div><span class="text-gray-500">方向：</span>{{ getDirectionText(currentBottle.direction) }}</div>
            <div><span class="text-gray-500">查看次数：</span>{{ currentBottle.viewCount || 0 }}</div>
            <div><span class="text-gray-500">创建时间：</span><span class="break-words">{{ currentBottle.createTime }}</span>
            </div>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  </div>
</template>
