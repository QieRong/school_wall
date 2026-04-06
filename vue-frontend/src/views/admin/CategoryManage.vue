<script setup>
import { ref, onMounted, reactive, computed } from 'vue'
import request from '@/api/request'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { Badge } from '@/components/ui/badge'
import { Switch } from '@/components/ui/switch'
import { Label } from '@/components/ui/label'
import { LayoutGrid, Plus, Edit, Trash2 } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as LucideIcons from 'lucide-vue-next'
import AdminTable from '@/components/admin/AdminTable.vue'

const list = ref([])
const loading = ref(false)
const showDialog = ref(false)
const isEdit = ref(false)

// 表单数据
const form = reactive({
  id: null,
  name: '',
  icon: 'Hash',
  color: 'text-gray-500',
  bgColor: 'bg-gray-100',
  sort: 0,
  status: true
})

// 预设颜色方案
const colorPresets = [
  { name: '粉色(表白)', text: 'text-pink-500', bg: 'bg-pink-100' },
  { name: '蓝色(寻物)', text: 'text-blue-500', bg: 'bg-blue-100' },
  { name: '橙色(闲置)', text: 'text-orange-500', bg: 'bg-orange-100' },
  { name: '紫色(吐槽)', text: 'text-purple-500', bg: 'bg-purple-100' },
  { name: '绿色(活动)', text: 'text-green-500', bg: 'bg-green-100' },
  { name: '青色(求助)', text: 'text-cyan-500', bg: 'bg-cyan-100' },
  { name: '灰色(其他)', text: 'text-gray-500', bg: 'bg-gray-100' },
]

// 表格列配置
const columns = [
  { key: 'sort', label: '排序' },
  { key: 'preview', label: '预览效果', slot: 'preview' },
  { key: 'name', label: '分类名称' },
  { key: 'icon', label: '图标代码' },
  { key: 'status', label: '状态', slot: 'status' },
  { key: 'actions', label: '操作', slot: 'actions' }
]

// 分页配置
const pagination = computed(() => ({
  current: 1,
  pageSize: list.value.length,
  total: list.value.length
}))

const fetchList = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/category/list')
    if (res.code === '200') list.value = res.data
  } finally {
    loading.value = false
  }
}

const openAdd = () => {
  isEdit.value = false
  Object.assign(form, { id: null, name: '', icon: 'Hash', color: 'text-gray-500', bgColor: 'bg-gray-100', sort: list.value.length + 1, status: true })
  showDialog.value = true
}

const openEdit = (item) => {
  isEdit.value = true
  Object.assign(form, { ...item, status: item.status === 1 })
  showDialog.value = true
}

const submitForm = async () => {
  if (!form.name) return ElMessage.warning('分类名称不能为空')

  const url = isEdit.value ? '/admin/category/update' : '/admin/category/add'
  const data = { ...form, status: form.status ? 1 : 0 }

  try {
    await request.post(url, data)
    ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
    showDialog.value = false
    fetchList()
  } catch (e) { ElMessage.error('操作失败') }
}

const handleDelete = (id) => {
  ElMessageBox.confirm('确定删除该分类吗？如果该分类下有帖子将无法删除。', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await request.post('/admin/category/delete', { id })
      ElMessage.success('删除成功')
      fetchList()
    } catch (e) { ElMessage.error(e.msg || '删除失败') }
  })
}

// 快速切换状态
const toggleStatus = async (item) => {
  // 基础8个板块不允许修改状态
  if ([1, 2, 3, 4, 5, 6, 7, 9].includes(item.id)) {
    ElMessage.warning('系统基础板块不可停用')
    return
  }
  item.status = item.status === 1 ? 0 : 1
  await request.post('/admin/category/update', item)
  ElMessage.success('状态已更新')
}

// 图标名称转换（支持横杠命名：graduation-cap -> GraduationCap）
const getLucideIcon = (name) => {
  if (!name) return LucideIcons.Hash
  const pascalName = name.split('-').map(w => w.charAt(0).toUpperCase() + w.slice(1).toLowerCase()).join('')
  return LucideIcons[pascalName] || LucideIcons[name] || LucideIcons.Hash
}

onMounted(fetchList)
</script>

<template>
  <div class="p-6 space-y-6">
    <!-- 页面标题 -->
    <div class="flex justify-between items-center">
      <div>
        <h2 class="text-2xl font-bold text-gray-800 flex items-center gap-2">
          <LayoutGrid class="w-6 h-6 text-emerald-600" /> 板块分类管理
        </h2>
        <p class="text-sm text-gray-500 mt-1">管理帖子分类和样式</p>
      </div>
      <Button @click="openAdd" class="bg-emerald-600 hover:bg-emerald-700 text-white">
        <Plus class="w-4 h-4 mr-2" /> 新增分类
      </Button>
    </div>

    <!-- 表格 -->
    <AdminTable :columns="columns" :data="list" :loading="loading" :pagination="pagination">
      <!-- 预览列 -->
      <template #preview="{ row }">
        <Badge :class="`${row.bgColor} ${row.color}`"
          class="border-none px-2 py-1 rounded-md flex items-center gap-1 w-fit">
          <component :is="getLucideIcon(row.icon)" class="w-3 h-3" />
          {{ row.name }}
        </Badge>
      </template>

      <!-- 状态列 -->
      <template #status="{ row }">
        <Switch :checked="row.status === 1" @update:checked="toggleStatus(row)" />
      </template>

      <!-- 操作列 -->
      <template #actions="{ row }">
        <div class="flex items-center gap-2 flex-wrap">
          <!-- 基础8个板块：仅显示锁定标识，不允许编辑/删除 -->
          <template v-if="[1, 2, 3, 4, 5, 6, 7, 9].includes(row.id)">
            <span class="inline-flex items-center gap-1 px-2 py-1 rounded-md text-xs font-medium bg-gray-100 text-gray-500 border border-gray-200">
              <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/></svg>
              系统板块
            </span>
          </template>
          <!-- 自定义板块：显示编辑和删除按钮 -->
          <template v-else>
            <Button size="sm" variant="outline" @click="openEdit(row)" class="whitespace-nowrap">
              <Edit class="w-3 h-3 mr-1" /> 编辑
            </Button>
            <Button size="sm" variant="ghost" class="text-red-500 hover:bg-red-50 whitespace-nowrap"
              @click="handleDelete(row.id)">
              <Trash2 class="w-4 h-4" />
            </Button>
          </template>
        </div>
      </template>
    </AdminTable>

    <!-- 编辑弹窗 -->
    <Dialog :open="showDialog" @update:open="showDialog = $event">
      <DialogContent class="sm:max-w-[500px] bg-white">
        <DialogHeader>
          <DialogTitle>{{ isEdit ? '编辑分类' : '新增分类' }}</DialogTitle>
        </DialogHeader>
        <div class="space-y-4 py-4">
          <div class="grid grid-cols-2 gap-4">
            <div class="space-y-2">
              <Label>分类名称</Label>
              <Input v-model="form.name" placeholder="例如：考研" />
            </div>
            <div class="space-y-2">
              <Label>排序 (越小越前)</Label>
              <Input v-model="form.sort" type="number" />
            </div>
          </div>

          <div class="space-y-2">
            <Label>图标 (Lucide Icon Name)</Label>
            <div class="flex gap-2">
              <Input v-model="form.icon" placeholder="例如：BookOpen" />
              <div class="w-10 h-10 border rounded flex items-center justify-center bg-gray-50">
                <component :is="LucideIcons[form.icon] || LucideIcons.HelpCircle" class="w-5 h-5 text-gray-500" />
              </div>
            </div>
            <p class="text-[10px] text-gray-400">去 lucide.dev 找图标名字</p>
          </div>

          <div class="space-y-2">
            <Label>颜色方案</Label>
            <div class="flex flex-wrap gap-2">
              <div v-for="p in colorPresets" :key="p.name"
                class="cursor-pointer px-2 py-1 rounded text-xs border transition-all"
                :class="[p.bg, p.text, form.color === p.text ? 'ring-2 ring-black' : '']"
                @click="form.color = p.text; form.bgColor = p.bg">
                {{ p.name }}
              </div>
            </div>
            <div class="grid grid-cols-2 gap-2 mt-2">
              <Input v-model="form.color" placeholder="文字颜色类名" class="text-xs" />
              <Input v-model="form.bgColor" placeholder="背景颜色类名" class="text-xs" />
            </div>
          </div>

          <div class="flex items-center gap-2 pt-2">
            <Switch id="c-status" :checked="form.status" @update:checked="form.status = $event" />
            <Label htmlFor="c-status">立即启用</Label>
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" @click="showDialog = false">取消</Button>
          <Button class="bg-emerald-600 text-white" @click="submitForm">保存</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>