<script setup>
import { ref, onMounted, reactive, computed } from 'vue'
import request from '@/api/request'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { Megaphone, Plus, Trash2, ArrowUp } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Switch } from '@/components/ui/switch'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
import AdminTable from '@/components/admin/AdminTable.vue'

const list = ref([])
const loading = ref(false)
const showDialog = ref(false)

const form = reactive({
  title: '',
  content: '',
  isTop: false
})

// 表格列配置
const columns = [
  { key: 'status', label: '状态', slot: 'status' },
  { key: 'title', label: '标题' },
  { key: 'content', label: '内容摘要', slot: 'content' },
  { key: 'createTime', label: '发布时间', slot: 'createTime' },
  { key: 'actions', label: '操作', slot: 'actions' }
]

// 分页配置（公告通常不多，暂不分页）
const pagination = computed(() => ({
  current: 1,
  pageSize: list.value.length,
  total: list.value.length
}))

const fetchList = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/announcement/list')
    if (res.code === '200') list.value = res.data
  } finally {
    loading.value = false
  }
}

const handleAdd = async () => {
  if (!form.title || !form.content) return ElMessage.warning('标题和内容不能为空')
  try {
    await request.post('/admin/announcement/add', {
      title: form.title,
      content: form.content,
      isTop: form.isTop ? 1 : 0
    })
    ElMessage.success('发布成功')
    showDialog.value = false
    form.title = ''; form.content = ''; form.isTop = false;
    fetchList()
  } catch (e) { ElMessage.error('发布失败') }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该公告？删除后无法恢复。', '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await request.post('/admin/announcement/delete', { id })
    ElMessage.success('删除成功')
    fetchList()
  } catch (e) {
    // 用户取消操作
  }
}

const handleTop = async (id) => {
  await request.post('/admin/announcement/top', { id })
  fetchList()
}

onMounted(fetchList)
</script>

<template>
  <div class="p-6 space-y-6">
    <!-- 页面标题 -->
    <div class="flex justify-between items-center">
      <div>
        <h2 class="text-2xl font-bold text-gray-800 flex items-center gap-2">
          <Megaphone class="w-6 h-6 text-emerald-600" /> 公告管理
        </h2>
        <p class="text-sm text-gray-500 mt-1">发布和管理系统公告</p>
      </div>
      <Button @click="showDialog = true" class="bg-emerald-600 hover:bg-emerald-700 text-white">
        <Plus class="w-4 h-4 mr-2" /> 发布公告
      </Button>
    </div>

    <!-- 表格 -->
    <AdminTable :columns="columns" :data="list" :loading="loading" :pagination="pagination">
      <!-- 状态列 -->
      <template #status="{ row }">
        <Badge v-if="row.isTop === 1" class="bg-red-100 text-red-700 border-none">
          <ArrowUp class="w-3 h-3 mr-1" /> 置顶
        </Badge>
        <Badge v-else variant="outline" class="text-gray-500">普通</Badge>
      </template>

      <!-- 内容列 -->
      <template #content="{ row }">
        <p class="max-w-xs truncate text-gray-600">{{ row.content }}</p>
      </template>

      <!-- 时间列 -->
      <template #createTime="{ row }">
        <span class="text-sm text-gray-500">{{ new Date(row.createTime).toLocaleString() }}</span>
      </template>

      <!-- 操作列 -->
      <template #actions="{ row }">
        <div class="flex items-center gap-2 flex-wrap">
          <Button size="sm" variant="ghost" :class="row.isTop === 1 ? 'text-gray-400' : 'text-orange-500'"
            class="whitespace-nowrap" @click="handleTop(row.id)">
            {{ row.isTop === 1 ? '取消置顶' : '置顶' }}
          </Button>
          <Button size="sm" variant="ghost" class="text-red-500 hover:bg-red-50 whitespace-nowrap"
            @click="handleDelete(row.id)">
            <Trash2 class="w-4 h-4" />
          </Button>
        </div>
      </template>
    </AdminTable>

    <!-- 发布弹窗 -->
    <Dialog :open="showDialog" @update:open="showDialog = $event">
      <DialogContent class="sm:max-w-[500px] bg-white">
        <DialogHeader>
          <DialogTitle>发布新公告</DialogTitle>
        </DialogHeader>
        <div class="space-y-4 py-4">
          <div class="space-y-2">
            <Label>公告标题</Label>
            <Input v-model="form.title" placeholder="请输入标题..." />
          </div>
          <div class="space-y-2">
            <Label>公告内容</Label>
            <Textarea v-model="form.content" placeholder="请输入内容..." class="min-h-[150px]" />
          </div>
          <div class="flex items-center space-x-2">
            <Switch id="top" :checked="form.isTop" @update:checked="form.isTop = $event" />
            <Label htmlFor="top">设为置顶</Label>
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" @click="showDialog = false">取消</Button>
          <Button class="bg-emerald-600 text-white" @click="handleAdd">发布</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>