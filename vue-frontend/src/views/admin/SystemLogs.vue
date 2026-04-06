<script setup>
import { ref, onMounted, reactive } from 'vue'
import request from '@/api/request'
import AdminTable from '@/components/admin/AdminTable.vue'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { FileText, Search, Calendar } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'

const logs = ref([])
const loading = ref(false)
const adminList = ref([])

const query = reactive({
  adminId: null,
  action: null,
  startDate: '',
  endDate: '',
  limit: 100
})

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0
})

const columns = [
  {
    key: 'id',
    label: 'ID',
    sortable: true
  },
  {
    key: 'admin',
    label: '操作管理员',
    slot: 'admin'
  },
  {
    key: 'action',
    label: '操作类型',
    slot: 'action'
  },
  {
    key: 'details',
    label: '操作详情',
    slot: 'details'
  },
  {
    key: 'ip',
    label: 'IP地址',
    slot: 'ip'
  },
  {
    key: 'createTime',
    label: '操作时间',
    slot: 'createTime',
    sortable: true
  }
]

const actionTypes = [
  { value: null, label: '全部操作' },
  { value: '删除帖子', label: '删除帖子' },
  { value: '删除评论', label: '删除评论' },
  { value: '封禁用户', label: '封禁用户' },
  { value: '解封用户', label: '解封用户' },
  { value: '添加敏感词', label: '添加敏感词' },
  { value: '删除敏感词', label: '删除敏感词' },
  { value: '处理举报', label: '处理举报' },
  { value: '发布公告', label: '发布公告' },
  { value: '删除公告', label: '删除公告' }
]

const fetchLogs = async () => {
  loading.value = true
  try {
    const params = {
      limit: query.limit
    }

    if (query.adminId) {
      params.adminId = query.adminId
    }

    if (query.action) {
      params.action = query.action
    }

    const res = await request.get('/admin/audit-logs', { params })
    if (res.code === '200') {
      let filteredLogs = res.data || []

      if (query.startDate) {
        const startTime = new Date(query.startDate).getTime()
        filteredLogs = filteredLogs.filter(log =>
          new Date(log.createTime).getTime() >= startTime
        )
      }

      if (query.endDate) {
        const endTime = new Date(query.endDate).getTime() + 86400000
        filteredLogs = filteredLogs.filter(log =>
          new Date(log.createTime).getTime() < endTime
        )
      }

      logs.value = filteredLogs
      pagination.total = filteredLogs.length
    }
  } catch (e) {
    ElMessage.error('获取日志失败')
  } finally {
    loading.value = false
  }
}

const fetchAdminList = async () => {
  try {
    const res = await request.get('/admin/user/list', {
      params: {
        pageNum: 1,
        pageSize: 100
      }
    })
    if (res.code === '200') {
      adminList.value = (res.data.list || []).filter(u => u.role === 1)
    }
  } catch (e) {
    console.error('获取管理员列表失败', e)
  }
}

const handleFilterChange = () => {
  pagination.current = 1
  fetchLogs()
}

const handlePageChange = (page) => {
  pagination.current = page
}

const paginatedLogs = ref([])
const updatePaginatedLogs = () => {
  const start = (pagination.current - 1) * pagination.pageSize
  const end = start + pagination.pageSize
  paginatedLogs.value = logs.value.slice(start, end)
}

const getActionColor = (action) => {
  if (action.includes('删除') || action.includes('封禁')) {
    return 'bg-red-100 text-red-700'
  }
  if (action.includes('解封') || action.includes('通过')) {
    return 'bg-green-100 text-green-700'
  }
  if (action.includes('添加') || action.includes('发布')) {
    return 'bg-blue-100 text-blue-700'
  }
  return 'bg-gray-100 text-gray-700'
}

onMounted(() => {
  fetchLogs()
  fetchAdminList()
})

// 监听日志和分页变化
import { watch } from 'vue'
watch([logs, () => pagination.current], () => {
  updatePaginatedLogs()
}, { immediate: true })
</script>

<template>
  <div class="space-y-4">
    <div class="bg-white rounded-lg shadow-sm p-4 border border-gray-200">
      <div class="flex items-center justify-between mb-4">
        <h2 class="font-bold text-lg flex items-center gap-2">
          <FileText class="w-5 h-5 text-primary" />
          操作日志
        </h2>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-3">
        <select v-model="query.adminId" @change="handleFilterChange"
          class="px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary">
          <option :value="null">全部管理员</option>
          <option v-for="admin in adminList" :key="admin.id" :value="admin.id">
            {{ admin.nickname }}
          </option>
        </select>

        <select v-model="query.action" @change="handleFilterChange"
          class="px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary">
          <option v-for="type in actionTypes" :key="type.value" :value="type.value">
            {{ type.label }}
          </option>
        </select>

        <div class="relative">
          <Calendar class="absolute left-3 top-2.5 w-4 h-4 text-gray-400" />
          <input type="date" v-model="query.startDate" @change="handleFilterChange"
            class="w-full pl-9 pr-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary"
            placeholder="开始日期" />
        </div>

        <div class="relative">
          <Calendar class="absolute left-3 top-2.5 w-4 h-4 text-gray-400" />
          <input type="date" v-model="query.endDate" @change="handleFilterChange"
            class="w-full pl-9 pr-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary"
            placeholder="结束日期" />
        </div>
      </div>
    </div>

    <div class="bg-white rounded-lg shadow-sm p-4 border border-gray-200">
      <AdminTable :columns="columns" :data="paginatedLogs" :loading="loading" :pagination="pagination"
        @page-change="handlePageChange">
        <template #admin="{ row }">
          <div class="text-sm">
            <div class="font-medium text-gray-900">{{ row.adminName || '未知' }}</div>
            <div class="text-xs text-gray-500">ID: {{ row.adminId }}</div>
          </div>
        </template>

        <template #action="{ row }">
          <Badge :class="getActionColor(row.action)" class="border-none">
            {{ row.action }}
          </Badge>
        </template>

        <template #details="{ row }">
          <p class="text-sm text-gray-700 line-clamp-2">{{ row.details || '-' }}</p>
        </template>

        <template #ip="{ row }">
          <span class="text-sm text-gray-600 font-mono">{{ row.ip || '-' }}</span>
        </template>

        <template #createTime="{ row }">
          <span class="text-sm text-gray-600">
            {{ new Date(row.createTime).toLocaleString('zh-CN') }}
          </span>
        </template>
      </AdminTable>
    </div>
  </div>
</template>

<style scoped>
svg {
  flex-shrink: 0;
}
</style>
