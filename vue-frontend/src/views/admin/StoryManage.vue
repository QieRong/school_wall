<script setup>
import { ref, onMounted, computed } from 'vue'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { BookOpen, Trash2, AlertTriangle, Search } from 'lucide-vue-next'
import { useAppStore } from '@/stores/app'
import request from '@/api/request'
import AdminTable from '@/components/admin/AdminTable.vue'

const appStore = useAppStore()

// 数据状态
const loading = ref(false)
const stories = ref([])
const stats = ref(null)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 筛选条件
const filters = ref({
  status: null,
  category: null,
  keyword: ''
})

// 删除确认弹窗
const showDeleteDialog = ref(false)
const deleteTarget = ref(null)
const deleteLoading = ref(false)

// 状态选项
const statusOptions = [
  { value: null, label: '全部状态' },
  { value: 1, label: '进行中' },
  { value: 2, label: '已完结' },
  { value: 3, label: '已归档' }
]

// 分类选项
const categoryOptions = [
  { value: null, label: '全部分类' },
  { value: 1, label: '奇幻校园' },
  { value: 2, label: '悬疑推理' },
  { value: 3, label: '浪漫物语' },
  { value: 4, label: '搞笑日常' },
  { value: 5, label: '科幻未来' }
]

// 状态映射
const statusMap = {
  1: { label: '进行中', color: 'bg-green-100 text-green-700' },
  2: { label: '已完结', color: 'bg-blue-100 text-blue-700' },
  3: { label: '已归档', color: 'bg-gray-100 text-gray-700' }
}

// 表格列配置
const columns = [
  { key: 'id', label: 'ID' },
  { key: 'title', label: '标题', slot: 'title' },
  { key: 'categoryName', label: '分类' },
  { key: 'paragraphCount', label: '段落数', sortable: true },
  { key: 'participantCount', label: '参与人数', sortable: true },
  { key: 'status', label: '状态', slot: 'status' },
  { key: 'createTime', label: '创建时间', sortable: true },
  { key: 'actions', label: '操作', slot: 'actions' }
]

// 分页配置
const pagination = computed(() => ({
  current: currentPage.value,
  pageSize: pageSize.value,
  total: total.value
}))

// 加载故事列表
const loadStories = async () => {
  loading.value = true
  try {
    // 有关键词时用搜索接口，否则用列表接口
    const url = filters.value.keyword ? '/story/search' : '/story/list'
    const res = await request.get(url, {
      params: {
        keyword: filters.value.keyword || undefined,
        category: filters.value.category || undefined,
        status: filters.value.status !== null ? filters.value.status : undefined,
        page: currentPage.value,
        size: pageSize.value
      }
    })
    if (res.code === '200') {
      // PageHelper 返回格式：{ list: [], total: N }
      stories.value = res.data.list || []
      total.value = res.data.total || 0
    }
  } catch (e) {
    appStore.showToast('加载失败', 'error')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  loadStories()
}

// 切换筛选
const handleFilterChange = () => {
  currentPage.value = 1
  loadStories()
}

// 分页
const handlePageChange = (page) => {
  currentPage.value = page
  loadStories()
}

// 打开删除确认
const openDeleteDialog = (story) => {
  deleteTarget.value = story
  showDeleteDialog.value = true
}

// 确认删除（使用管理员token调用，通过 userId=0 标记管理员）
const confirmDelete = async () => {
  if (!deleteTarget.value) return

  deleteLoading.value = true
  try {
    const res = await request.delete(`/story/${deleteTarget.value.id}`, {
      params: { userId: deleteTarget.value.creatorId || 0 }
    })
    if (res.code === '200') {
      appStore.showToast('删除成功', 'success')
      showDeleteDialog.value = false
      loadStories()
    } else {
      appStore.showToast(res.msg || '删除失败', 'error')
    }
  } catch (e) {
    appStore.showToast('删除失败', 'error')
  } finally {
    deleteLoading.value = false
  }
}

// 格式化时间
const formatTime = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

onMounted(() => {
  loadStories()
})
</script>

<template>
  <div class="p-6 space-y-6">
    <!-- 页面标题 -->
    <div>
      <h2 class="text-2xl font-bold text-gray-800 flex items-center gap-2">
        <BookOpen class="w-6 h-6 text-amber-600" /> 故事接龙管理
      </h2>
      <p class="text-sm text-gray-500 mt-1">管理故事和段落内容</p>
    </div>

    <!-- 共 N 条提示 -->
    <div class="text-sm text-gray-500">共 {{ total }} 个故事</div>

    <!-- 筛选栏 -->
    <div class="bg-white rounded-lg border border-gray-200 p-4">
      <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div>
          <label class="text-sm font-medium text-gray-700 mb-1 block">状态</label>
          <select v-model="filters.status" @change="handleFilterChange"
            class="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary">
            <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
          </select>
        </div>
        <div>
          <label class="text-sm font-medium text-gray-700 mb-1 block">分类</label>
          <select v-model="filters.category" @change="handleFilterChange"
            class="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary">
            <option v-for="opt in categoryOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
          </select>
        </div>
        <div class="md:col-span-2">
          <label class="text-sm font-medium text-gray-700 mb-1 block">搜索标题</label>
          <div class="flex gap-2 items-center">
            <Input v-model="filters.keyword" placeholder="搜索标题..." @keyup.enter="handleSearch" class="flex-1 h-9" />
            <Button @click="handleSearch" class="h-9 px-4 bg-[rgb(33,111,85)] hover:bg-[rgb(28,95,72)] whitespace-nowrap shrink-0">
              <Search class="w-4 h-4 mr-1 shrink-0" /> 搜索
            </Button>
          </div>
        </div>
      </div>
    </div>

    <!-- 表格 -->
    <AdminTable :columns="columns" :data="stories" :loading="loading" :pagination="pagination"
      @page-change="handlePageChange">
      <!-- 标题列 -->
      <template #title="{ row }">
        <div class="flex items-center gap-2 min-w-0">
          <span class="font-medium text-gray-800 max-w-[200px] truncate break-words overflow-hidden">{{ row.title }}</span>
        </div>
      </template>

      <!-- 状态列 -->
      <template #status="{ row }">
        <Badge :class="statusMap[row.status]?.color">
          {{ statusMap[row.status]?.label }}
        </Badge>
      </template>

      <!-- 操作列 -->
      <template #actions="{ row }">
        <div class="flex items-center gap-1 flex-wrap justify-end">
          <Button size="sm" variant="ghost" class="text-red-500 hover:text-red-600 hover:bg-red-50 whitespace-nowrap"
            @click="openDeleteDialog(row)" title="删除">
            <Trash2 class="w-4 h-4" />
          </Button>
        </div>
      </template>
    </AdminTable>

    <!-- 删除确认弹窗 -->
    <Dialog :open="showDeleteDialog" @update:open="showDeleteDialog = $event">
      <DialogContent class="sm:max-w-md">
        <DialogHeader>
          <DialogTitle class="flex items-center gap-2 text-red-600">
            <AlertTriangle class="w-5 h-5" /> 确认删除
          </DialogTitle>
        </DialogHeader>
        <div class="py-4">
          <p class="text-gray-600">
            确定要删除故事「{{ deleteTarget?.title }}」吗？
            <br><span class="text-red-500 text-sm">此操作将删除故事及所有段落，无法恢复！</span>
          </p>
        </div>
        <DialogFooter>
          <Button variant="outline" @click="showDeleteDialog = false" :disabled="deleteLoading">
            取消
          </Button>
          <Button class="bg-red-600 hover:bg-red-700 text-white" @click="confirmDelete" :disabled="deleteLoading">
            {{ deleteLoading ? '删除中...' : '确认删除' }}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>
