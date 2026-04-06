<script setup>
import { ref, onMounted, reactive, watch } from 'vue'
import { debounce } from 'lodash-es'
import request from '@/api/request'
import AdminTable from '@/components/admin/AdminTable.vue'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { Search, Ban, CheckCircle, ShieldAlert, Filter, Eye } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'

const users = ref([])
const loading = ref(false)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  role: null,
  status: null
})

// 排序状态
const sort = reactive({
  orderBy: null,
  orderDir: null
})

const showBanDialog = ref(false)
const showUnbanDialog = ref(false)
const showDetailDialog = ref(false)
const currentUser = ref(null)


const banForm = reactive({
  duration: 1,
  reason: ''
})

const unbanForm = reactive({
  reason: ''
})

// 获取当前登录用户
const loginUser = JSON.parse(localStorage.getItem('user') || '{}')

// 表格列配置
const columns = [
  {
    key: 'user',
    label: '用户信息',
    slot: 'user'
  },
  {
    key: 'creditScore',
    label: '信誉分',
    slot: 'creditScore',
    sortable: true
  },
  {
    key: 'violationCount',
    label: '违规次数',
    sortable: true
  },
  {
    key: 'status',
    label: '状态',
    slot: 'status'
  },
  {
    key: 'banEndTime',
    label: '封禁截止',
    slot: 'banEndTime'
  },
  {
    key: 'actions',
    label: '操作',
    slot: 'actions'
  }
]

// 分页配置
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0
})

// 获取用户列表
const fetchList = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/user/list', {
      params: {
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
        keyword: query.keyword || undefined, // 空字符不传参数，避免后端 isNotBlank 判断失败
        role: query.role,
        status: query.status,
        orderBy: sort.orderBy || undefined,
        orderDir: sort.orderDir || undefined
      }
    })
    if (res.code === '200') {
      users.value = res.data.list || []
      pagination.total = res.data.total || 0
    }
  } catch (e) {
    ElMessage.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

// 处理表头排序点击（服务端排序）
const handleSortChange = ({ column, order }) => {
  sort.orderBy = column  // null 表示取消排序
  sort.orderDir = order
  pagination.current = 1
  fetchList()
}

// 防抖搜索 (300ms)
const debouncedSearch = debounce(() => {
  pagination.current = 1
  fetchList()
}, 300)

// 监听搜索关键词变化
watch(() => query.keyword, () => {
  debouncedSearch()
})

// 筛选变化
const handleFilterChange = () => {
  pagination.current = 1
  fetchList()
}

// 分页变化
const handlePageChange = (page) => {
  pagination.current = page
  fetchList()
}

// 打开封禁弹窗
const openBanDialog = (user) => {
  if (user.id === loginUser.id) {
    return ElMessage.warning('不能封禁自己')
  }
  if (user.role === 1) {
    return ElMessage.warning('不能封禁管理员账号')
  }
  currentUser.value = user
  banForm.duration = 1
  banForm.reason = ''
  showBanDialog.value = true
}

// 提交封禁
const submitBan = async () => {
  if (!banForm.reason.trim()) {
    return ElMessage.warning('请填写封禁理由')
  }

  try {
    await request.post('/admin/user/ban', {
      userId: currentUser.value.id,
      duration: parseInt(banForm.duration),
      reason: banForm.reason
    })
    ElMessage.success('封禁成功')
    showBanDialog.value = false
    fetchList()
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || '操作失败')
  }
}

// 打开解封弹窗
const openUnbanDialog = (user) => {
  currentUser.value = user
  unbanForm.reason = ''
  showUnbanDialog.value = true
}

// 提交解封
const submitUnban = async () => {
  if (!unbanForm.reason.trim()) {
    return ElMessage.warning('请填写解封理由')
  }

  try {
    await request.post('/admin/user/unban', {
      userId: currentUser.value.id,
      reason: unbanForm.reason
    })
    ElMessage.success('解封成功')
    showUnbanDialog.value = false
    fetchList()
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || '操作失败')
  }
}

// 数据脱敏工具函数
const maskPhone = (phone) => {
  if (!phone) return '-'
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
}

const maskEmail = (email) => {
  if (!email) return '-'
  const [name, domain] = email.split('@')
  if (name.length <= 3) {
    return `${name.charAt(0)}***@${domain}`
  }
  return `${name.substring(0, 3)}***@${domain}`
}

// 打开用户详情对话框
const openDetailDialog = (user) => {
  currentUser.value = user
  showDetailDialog.value = true
}

onMounted(fetchList)
</script>

<template>
  <div class="space-y-4">
    <!-- 页面标题和操作栏 -->
    <div class="bg-white rounded-lg shadow-sm p-4 border border-gray-200">
      <div class="flex items-center justify-between mb-4">
        <h2 class="font-bold text-lg flex items-center gap-2">
          <ShieldAlert class="w-5 h-5 text-primary" />
          用户管理
        </h2>
      </div>

      <!-- 搜索和筛选 -->
      <div class="flex flex-wrap gap-3">
        <!-- 搜索框 -->
        <div class="flex-1 min-w-[200px]">
          <Input v-model="query.keyword" placeholder="搜索昵称/账号..." class="w-full">
            <template #prefix>
              <Search class="w-4 h-4 text-gray-400" />
            </template>
          </Input>
        </div>

        <!-- 角色筛选 -->
        <select v-model="query.role" @change="handleFilterChange"
          class="px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary">
          <option :value="null">全部角色</option>
          <option :value="0">普通用户</option>
        </select>

        <!-- 状态筛选 -->
        <select v-model="query.status" @change="handleFilterChange"
          class="px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary">
          <option :value="null">全部状态</option>
          <option :value="1">正常</option>
          <option :value="0">封禁中</option>
        </select>
      </div>
    </div>

    <!-- 用户列表表格 -->
    <div class="bg-white rounded-lg shadow-sm p-4 border border-gray-200">
      <AdminTable :columns="columns" :data="users" :loading="loading" :pagination="pagination"
        @page-change="handlePageChange" @sort-change="handleSortChange">
        <!-- 用户信息列 -->
        <template #user="{ row }">
          <div class="flex items-center gap-2 min-w-0">
            <img :src="row.avatar" alt="头像" class="w-8 h-8 rounded-full border flex-shrink-0"
              @error="(e) => e.target.src = '/default.png'" />
            <div class="min-w-0 flex-1">
              <div class="font-medium text-gray-900 truncate">{{ row.nickname }}</div>
              <div class="text-xs text-gray-500 truncate">{{ row.account }}</div>
            </div>
          </div>
        </template>

        <!-- 信誉分列 -->
        <template #creditScore="{ row }">
          <span class="font-semibold" :class="row.creditScore < 80 ? 'text-red-500' : 'text-green-600'">
            {{ row.creditScore }}
          </span>
        </template>

        <!-- 状态列 -->
        <template #status="{ row }">
          <Badge v-if="row.status === 1" class="bg-green-100 text-green-700 border-none">
            正常
          </Badge>
          <Badge v-else class="bg-red-100 text-red-700 border-none">
            封禁中
          </Badge>
        </template>

        <!-- 封禁截止列 -->
        <template #banEndTime="{ row }">
          <span class="text-sm text-gray-600">
            {{ row.banEndTime ? new Date(row.banEndTime).toLocaleString('zh-CN') : '-' }}
          </span>
        </template>

        <!-- 操作列 -->
        <template #actions="{ row }">
          <div class="flex items-center justify-start gap-2 flex-wrap">
            <Button size="sm" variant="outline" @click="openDetailDialog(row)" class="whitespace-nowrap">
              <Eye class="w-3 h-3 mr-1" /> 查看
            </Button>
            <template v-if="row.role !== 1 && row.id !== loginUser.id">
              <Button v-if="row.status === 1" size="sm" variant="destructive" @click="openBanDialog(row)"
                class="whitespace-nowrap">
                <Ban class="w-3 h-3 mr-1" /> 封禁
              </Button>
              <Button v-else size="sm" class="bg-primary hover:opacity-90 text-white whitespace-nowrap"
                @click="openUnbanDialog(row)">
                <CheckCircle class="w-3 h-3 mr-1" /> 解封
              </Button>
            </template>
            <Badge v-else-if="row.role === 1" class="bg-yellow-100 text-yellow-700 border-none whitespace-nowrap">
              管理员
            </Badge>
          </div>
        </template>
      </AdminTable>
    </div>

    <!-- 封禁对话框 -->
    <Dialog :open="showBanDialog" @update:open="showBanDialog = $event">
      <DialogContent>
        <DialogHeader>
          <DialogTitle class="flex items-center gap-2">
            <Ban class="w-5 h-5 text-red-500" />
            封禁用户
          </DialogTitle>
        </DialogHeader>
        <div class="space-y-4 py-4">
          <div class="space-y-2">
            <label class="text-sm font-medium text-gray-700">封禁时长</label>
            <select v-model="banForm.duration"
              class="w-full p-2.5 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary">
              <option :value="1">1 天 (轻微违规)</option>
              <option :value="3">3 天 (一般违规)</option>
              <option :value="7">7 天 (严重违规)</option>
              <option :value="30">30 天 (恶劣违规)</option>
              <option :value="-1">永久封禁 (零容忍)</option>
            </select>
          </div>
          <div class="space-y-2">
            <label class="text-sm font-medium text-gray-700">
              违规原因 <span class="text-red-500">*</span>
            </label>
            <Input v-model="banForm.reason" placeholder="请输入封禁理由，如：发布广告、辱骂他人" />
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" @click="showBanDialog = false">取消</Button>
          <Button variant="destructive" @click="submitBan">确认封禁</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- 解封对话框 -->
    <Dialog :open="showUnbanDialog" @update:open="showUnbanDialog = $event">
      <DialogContent>
        <DialogHeader>
          <DialogTitle class="flex items-center gap-2">
            <CheckCircle class="w-5 h-5 text-green-500" />
            解封用户
          </DialogTitle>
        </DialogHeader>
        <div class="space-y-4 py-4">
          <div class="bg-blue-50 border border-blue-200 rounded-md p-3">
            <p class="text-sm text-blue-800">
              解封后，用户 <span class="font-semibold">{{ currentUser?.nickname }}</span> 将恢复正常使用权限。
            </p>
          </div>
          <div class="space-y-2">
            <label class="text-sm font-medium text-gray-700">
              解封理由 <span class="text-red-500">*</span>
            </label>
            <Input v-model="unbanForm.reason" placeholder="请输入解封理由，如：已认识到错误、申诉成功" />
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" @click="showUnbanDialog = false">取消</Button>
          <Button class="bg-primary hover:opacity-90 text-white" @click="submitUnban">
            确认解封
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- 用户详情对话框 -->
    <Dialog :open="showDetailDialog" @update:open="showDetailDialog = $event">
      <DialogContent class="max-w-3xl max-h-[85vh] flex flex-col">
        <DialogHeader class="flex-shrink-0">
          <DialogTitle class="flex items-center gap-2">
            <Eye class="w-5 h-5 text-primary" />
            用户详情
          </DialogTitle>
        </DialogHeader>

        <div v-if="currentUser" class="flex-1 overflow-y-auto min-h-0 space-y-6 py-4">
          <!-- 基本信息 -->
          <div class="bg-gray-50 rounded-lg p-4 border border-gray-200">
            <h3 class="font-semibold text-gray-900 mb-4 flex items-center gap-2">
              <div class="w-1 h-4 bg-primary rounded"></div>
              基本信息
            </h3>
            <div class="grid grid-cols-2 gap-4">
              <div class="flex items-center gap-3">
                <img :src="currentUser.avatar" alt="头像" class="w-16 h-16 rounded-full border-2 border-gray-200" />
                <div>
                  <div class="font-medium text-gray-900">{{ currentUser.nickname }}</div>
                  <div class="text-sm text-gray-500">ID: {{ currentUser.id }}</div>
                </div>
              </div>
              <div class="space-y-2">
                <div class="text-sm">
                  <span class="text-gray-500">账号：</span>
                  <span class="text-gray-900">{{ currentUser.account }}</span>
                </div>
                <div class="text-sm">
                  <span class="text-gray-500">角色：</span>
                  <Badge :class="currentUser.role === 1 ? 'bg-yellow-100 text-yellow-700' : 'bg-blue-100 text-blue-700'"
                    class="border-none">
                    {{ currentUser.role === 1 ? '管理员' : '普通用户' }}
                  </Badge>
                </div>
              </div>
              <div class="space-y-2">
                <div class="text-sm">
                  <span class="text-gray-500">手机号：</span>
                  <span class="text-gray-900 font-mono">{{ maskPhone(currentUser.phone) }}</span>
                </div>
                <div class="text-sm">
                  <span class="text-gray-500">邮箱：</span>
                  <span class="text-gray-900 font-mono">{{ maskEmail(currentUser.email) }}</span>
                </div>
              </div>
              <div class="space-y-2">
                <div class="text-sm">
                  <span class="text-gray-500">注册时间：</span>
                  <span class="text-gray-900">{{ new Date(currentUser.createTime).toLocaleString('zh-CN') }}</span>
                </div>
                <div class="text-sm">
                  <span class="text-gray-500">状态：</span>
                  <Badge :class="currentUser.status === 1 ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'"
                    class="border-none">
                    {{ currentUser.status === 1 ? '正常' : '封禁中' }}
                  </Badge>
                </div>
              </div>
            </div>
          </div>

          <!-- 统计数据 -->
          <div class="bg-gray-50 rounded-lg p-4 border border-gray-200">
            <h3 class="font-semibold text-gray-900 mb-4 flex items-center gap-2">
              <div class="w-1 h-4 bg-primary rounded"></div>
              统计数据
            </h3>
            <div class="grid grid-cols-4 gap-4">
              <div class="bg-white rounded-lg p-3 border border-gray-200 text-center">
                <div class="text-2xl font-bold text-primary">{{ currentUser.creditScore || 100 }}</div>
                <div class="text-xs text-gray-500 mt-1">信誉分</div>
              </div>
              <div class="bg-white rounded-lg p-3 border border-gray-200 text-center">
                <div class="text-2xl font-bold text-orange-500">{{ currentUser.violationCount || 0 }}</div>
                <div class="text-xs text-gray-500 mt-1">违规次数</div>
              </div>
              <div class="bg-white rounded-lg p-3 border border-gray-200 text-center">
                <div class="text-2xl font-bold text-blue-500">{{ currentUser.postCount || 0 }}</div>
                <div class="text-xs text-gray-500 mt-1">发帖数</div>
              </div>
              <div class="bg-white rounded-lg p-3 border border-gray-200 text-center">
                <div class="text-2xl font-bold text-green-500">{{ currentUser.commentCount || 0 }}</div>
                <div class="text-xs text-gray-500 mt-1">评论数</div>
              </div>
            </div>
          </div>

          <!-- 封禁信息 -->
          <div v-if="currentUser.status === 0" class="bg-red-50 rounded-lg p-4 border border-red-200">
            <h3 class="font-semibold text-red-900 mb-3 flex items-center gap-2">
              <Ban class="w-4 h-4" />
              封禁信息
            </h3>
            <div class="space-y-2 text-sm">
              <div>
                <span class="text-red-700">封禁截止：</span>
                <span class="text-red-900 font-medium">
                  {{ currentUser.banEndTime ? new Date(currentUser.banEndTime).toLocaleString('zh-CN') : '永久' }}
                </span>
              </div>
              <div v-if="currentUser.banReason">
                <span class="text-red-700">封禁理由：</span>
                <span class="text-red-900">{{ currentUser.banReason }}</span>
              </div>
            </div>
          </div>


        </div>

        <DialogFooter class="flex-shrink-0">
          <Button variant="outline" @click="showDetailDialog = false">关闭</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>

<style scoped>
svg {
  flex-shrink: 0;
}
</style>
