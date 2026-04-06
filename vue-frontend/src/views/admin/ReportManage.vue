<script setup>
import { ref, onMounted, reactive } from 'vue'
import request from '@/api/request'
import AdminTable from '@/components/admin/AdminTable.vue'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { ShieldAlert, CheckCircle, XCircle, Eye } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'

const list = ref([])
const loading = ref(false)
const activeTab = ref('pending')

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  status: 0
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0
})

const showDetailDialog = ref(false)
const showHandleDialog = ref(false)
const currentReport = ref(null)
const handleAction = ref(1)
const handleReason = ref('')

const tabs = [
  { key: 'pending', label: '待处理', status: 0 },
  { key: 'approved', label: '已采纳', status: 1 },
  { key: 'rejected', label: '已驳回', status: 2 }
]

const columns = [
  {
    key: 'reporter',
    label: '举报人',
    slot: 'reporter'
  },
  {
    key: 'reason',
    label: '举报理由',
    slot: 'reason'
  },
  {
    key: 'content',
    label: '被举报内容',
    slot: 'content'
  },
  {
    key: 'status',
    label: '状态',
    slot: 'status',
    sortable: true
  },
  {
    key: 'createTime',
    label: '举报时间',
    slot: 'createTime',
    sortable: true
  },
  {
    key: 'actions',
    label: '操作',
    slot: 'actions'
  }
]

// 前端本地排序状态
const localSort = ref({ column: null, order: 'desc' })
const handleSortChange = ({ column, order }) => {
  localSort.value = { column, order }
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/report/list', {
      params: {
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
        status: query.status
      }
    })
    if (res.code === '200') {
      list.value = res.data.list || []
      pagination.total = res.data.total || 0
    }
  } catch (e) {
    ElMessage.error('获取举报列表失败')
  } finally {
    loading.value = false
  }
}

const handleTabChange = (tab) => {
  activeTab.value = tab.key
  query.status = tab.status
  pagination.current = 1
  fetchList()
}

const handlePageChange = (page) => {
  pagination.current = page
  fetchList()
}

const openDetailDialog = (report) => {
  currentReport.value = report
  showDetailDialog.value = true
}

const openHandleDialog = (report, action) => {
  currentReport.value = report
  handleAction.value = action
  handleReason.value = ''
  showHandleDialog.value = true
}

const confirmHandle = async () => {
  try {
    await request.post('/admin/report/handle', {
      id: currentReport.value.id,
      action: handleAction.value
    })
    ElMessage.success('处理成功')
    showHandleDialog.value = false
    fetchList()
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || '操作失败')
  }
}

onMounted(fetchList)
</script>

<template>
  <div class="space-y-4">
    <div class="bg-white rounded-lg shadow-sm p-4 border border-gray-200">
      <div class="flex items-center justify-between mb-4">
        <h2 class="font-bold text-lg flex items-center gap-2">
          <ShieldAlert class="w-5 h-5 text-red-600" />
          举报处理中心
        </h2>
      </div>

      <div class="flex items-center gap-3 pb-3 border-b border-gray-200">
        <button v-for="tab in tabs" :key="tab.key" @click="handleTabChange(tab)" :class="[
          'px-4 py-2 rounded-lg text-sm font-medium transition-all',
          activeTab === tab.key
            ? 'bg-primary text-white shadow-sm'
            : 'text-gray-600 hover:bg-gray-100'
        ]">
          {{ tab.label }}
        </button>
      </div>
    </div>

    <div class="bg-white rounded-lg shadow-sm p-4 border border-gray-200">
      <AdminTable :columns="columns" :data="list" :loading="loading" :pagination="pagination"
        @page-change="handlePageChange" @sort-change="handleSortChange">
        <template #reporter="{ row }">
          <div class="flex items-center gap-2 min-w-0">
            <img :src="row.reporterAvatar" alt="头像" class="w-8 h-8 rounded-full border flex-shrink-0"
              @error="(e) => e.target.src = '/default.png'" />
            <div class="min-w-0 flex-1">
              <div class="text-sm font-medium text-gray-900 truncate">{{ row.reporterName }}</div>
              <div class="text-xs text-gray-500 truncate">ID: {{ row.reporterId }}</div>
            </div>
          </div>
        </template>

        <template #reason="{ row }">
          <Badge class="bg-red-100 text-red-700 border-none">
            {{ row.reason }}
          </Badge>
        </template>

        <template #content="{ row }">
          <div class="max-w-md">
            <p v-if="row.postContent" class="text-sm text-gray-700 line-clamp-2 break-words overflow-hidden">
              {{ row.postContent }}
            </p>
            <p v-else class="text-sm text-gray-400 italic">
              [内容已删除]
            </p>
          </div>
        </template>

        <template #status="{ row }">
          <Badge v-if="Number(row.status) === 0" class="bg-orange-100 text-orange-700 border-none">
            待处理
          </Badge>
          <Badge v-else-if="Number(row.status) === 1" class="bg-green-100 text-green-700 border-none">
            已采纳
          </Badge>
          <Badge v-else class="bg-gray-100 text-gray-700 border-none">
            已驳回
          </Badge>
        </template>

        <template #createTime="{ row }">
          <span class="text-sm text-gray-600">
            {{ row.createTime ? new Date(row.createTime).toLocaleString('zh-CN') : '-' }}
          </span>
        </template>

        <template #actions="{ row }">
          <div class="flex items-center justify-end gap-2 flex-wrap">
            <Button size="sm" variant="outline" @click="openDetailDialog(row)" class="whitespace-nowrap">
              <Eye class="w-3 h-3 mr-1" /> 详情
            </Button>
            <template v-if="Number(row.status) === 0">
              <Button size="sm" class="bg-green-600 hover:bg-green-700 text-white whitespace-nowrap"
                @click="openHandleDialog(row, 1)">
                <CheckCircle class="w-3 h-3 mr-1" /> 通过
              </Button>
              <Button size="sm" variant="destructive" @click="openHandleDialog(row, 2)" class="whitespace-nowrap">
                <XCircle class="w-3 h-3 mr-1" /> 驳回
              </Button>
            </template>
            <span v-else class="text-xs text-gray-400 whitespace-nowrap">已归档</span>
          </div>
        </template>
      </AdminTable>
    </div>

    <Dialog :open="showDetailDialog" @update:open="showDetailDialog = $event">
      <DialogContent class="max-w-2xl max-h-[85vh] flex flex-col">
        <DialogHeader class="flex-shrink-0">
          <DialogTitle class="flex items-center gap-2">
            <ShieldAlert class="w-5 h-5 text-red-600" />
            举报详情
          </DialogTitle>
        </DialogHeader>
        <div v-if="currentReport" class="flex-1 overflow-y-auto min-h-0 space-y-4 py-4">
          <div class="bg-gray-50 rounded-lg p-4 border border-gray-200">
            <h3 class="font-semibold text-gray-900 mb-3 flex items-center gap-2">
              <div class="w-1 h-4 bg-red-600 rounded"></div>
              举报信息
            </h3>
            <div class="grid grid-cols-2 gap-4">
              <div>
                <span class="text-sm text-gray-500">举报人：</span>
                <div class="flex items-center gap-2 mt-1">
                  <img :src="currentReport.reporterAvatar" alt="头像" class="w-8 h-8 rounded-full border flex-shrink-0"
                    @error="(e) => e.target.src = '/default.png'" />
                  <span class="text-sm font-medium text-gray-900 truncate">{{ currentReport.reporterName }}</span>
                </div>
              </div>
              <div>
                <span class="text-sm text-gray-500">举报时间：</span>
                <div class="text-sm text-gray-900 mt-1 break-words">
                  {{ new Date(currentReport.createTime).toLocaleString('zh-CN') }}
                </div>
              </div>
              <div class="col-span-2">
                <span class="text-sm text-gray-500">举报理由：</span>
                <Badge class="bg-red-100 text-red-700 border-none mt-1">
                  {{ currentReport.reason }}
                </Badge>
              </div>
            </div>
          </div>

          <div class="bg-gray-50 rounded-lg p-4 border border-gray-200">
            <h3 class="font-semibold text-gray-900 mb-3 flex items-center gap-2">
              <div class="w-1 h-4 bg-primary rounded"></div>
              被举报内容
            </h3>
            <div v-if="currentReport.postContent" class="bg-white rounded-lg p-3 border border-gray-200">
              <p class="text-sm text-gray-900 whitespace-pre-wrap break-words">{{ currentReport.postContent }}</p>
            </div>
            <div v-else class="text-center py-4 text-gray-400 text-sm">
              内容已被删除
            </div>
          </div>

          <div class="bg-gray-50 rounded-lg p-4 border border-gray-200">
            <h3 class="font-semibold text-gray-900 mb-3 flex items-center gap-2">
              <div class="w-1 h-4 bg-primary rounded"></div>
              处理状态
            </h3>
            <div class="flex items-center gap-2">
              <Badge v-if="currentReport.status === 0" class="bg-orange-100 text-orange-700 border-none">
                待处理
              </Badge>
              <Badge v-else-if="currentReport.status === 1" class="bg-green-100 text-green-700 border-none">
                已采纳
              </Badge>
              <Badge v-else class="bg-gray-100 text-gray-700 border-none">
                已驳回
              </Badge>
            </div>
          </div>
        </div>
        <DialogFooter class="flex-shrink-0">
          <Button variant="outline" @click="showDetailDialog = false">关闭</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <Dialog :open="showHandleDialog" @update:open="showHandleDialog = $event">
      <DialogContent>
        <DialogHeader>
          <DialogTitle class="flex items-center gap-2" :class="handleAction === 1 ? 'text-green-600' : 'text-red-600'">
            <component :is="handleAction === 1 ? CheckCircle : XCircle" class="w-5 h-5" />
            {{ handleAction === 1 ? '通过举报' : '驳回举报' }}
          </DialogTitle>
        </DialogHeader>
        <div class="space-y-4 py-4">
          <div :class="[
            'border rounded-md p-3',
            handleAction === 1 ? 'bg-green-50 border-green-200' : 'bg-red-50 border-red-200'
          ]">
            <p class="text-sm" :class="handleAction === 1 ? 'text-green-800' : 'text-red-800'">
              <template v-if="handleAction === 1">
                通过举报后，被举报的内容将被删除，发布者将收到通知。
              </template>
              <template v-else>
                驳回举报后，被举报的内容将保留，举报人将收到通知。
              </template>
            </p>
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" @click="showHandleDialog = false">取消</Button>
          <Button :class="handleAction === 1 ? 'bg-green-600 hover:bg-green-700 text-white' : ''"
            :variant="handleAction === 2 ? 'destructive' : 'default'" @click="confirmHandle">
            确认{{ handleAction === 1 ? '通过' : '驳回' }}
          </Button>
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
