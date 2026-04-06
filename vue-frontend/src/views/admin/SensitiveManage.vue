<script setup>
import { ref, onMounted, computed } from 'vue'
import request from '@/api/request'
import AdminTable from '@/components/admin/AdminTable.vue'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { Textarea } from '@/components/ui/textarea'
import { Trash2, Plus, Search, AlertTriangle, Upload } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'

const words = ref([])
const newWord = ref('')
const searchQuery = ref('')
const loading = ref(false)
const showBatchImportDialog = ref(false)
const batchImportText = ref('')
const selectedWords = ref([])

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0
})

const columns = [
  {
    key: 'word',
    label: '敏感词',
    slot: 'word'
  },
  {
    key: 'category',
    label: '分类',
    slot: 'category'
  },
  {
    key: 'createTime',
    label: '添加时间',
    slot: 'createTime'
  },
  {
    key: 'actions',
    label: '操作',
    slot: 'actions'
  }
]

const fetchWords = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/sensitive/list')
    const rawWords = (res && res.data) ? res.data : (Array.isArray(res) ? res : [])

    words.value = rawWords.map((item, index) => {
      // 后端现在返回 Map 对象包含 word 和 createTime
      const word = typeof item === 'string' ? item : (item.word || item)
      const createTime = typeof item === 'object' && item.createTime
        ? item.createTime
        : null
      return {
        id: index + 1,
        word,
        category: getCategory(word),
        createTime
      }
    })

    pagination.total = words.value.length
  } catch (e) {
    ElMessage.error('加载敏感词失败')
  } finally {
    loading.value = false
  }
}

const getCategory = (word) => {
  const w = typeof word === 'string' ? word : word.word || word
  if (/政治|党|国家/.test(w)) return '政治敏感'
  if (/色情|黄色|裸/.test(w)) return '色情低俗'
  if (/暴力|杀|死/.test(w)) return '暴力血腥'
  if (/广告|微信|QQ/.test(w)) return '广告推广'
  return '其他违规'
}

const filteredWords = computed(() => {
  if (!searchQuery.value.trim()) return words.value
  const query = searchQuery.value.toLowerCase()
  return words.value.filter(w => w.word.toLowerCase().includes(query))
})

const paginatedWords = computed(() => {
  const start = (pagination.current - 1) * pagination.pageSize
  const end = start + pagination.pageSize
  return filteredWords.value.slice(start, end)
})

const addWord = async () => {
  if (!newWord.value.trim()) {
    return ElMessage.warning('请输入敏感词')
  }

  try {
    await request.post('/admin/sensitive/add', { word: newWord.value.trim() })
    ElMessage.success('添加成功')
    newWord.value = ''
    fetchWords()
  } catch (e) {
    ElMessage.error(e.msg || '添加失败')
  }
}

const deleteWord = async (word) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除敏感词 "${word}" 吗？`,
      '删除确认',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )

    await request.post('/admin/sensitive/delete', { word })
    ElMessage.success('删除成功')
    fetchWords()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.msg || '删除失败')
    }
  }
}

const handleSelectionChange = (selection) => {
  selectedWords.value = selection
}

const batchDelete = async () => {
  if (selectedWords.value.length === 0) {
    return ElMessage.warning('请先选择要删除的敏感词')
  }

  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedWords.value.length} 个敏感词吗？`,
      '批量删除确认',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )

    const deletePromises = selectedWords.value.map(item =>
      request.post('/admin/sensitive/delete', { word: item.word })
    )

    await Promise.all(deletePromises)
    ElMessage.success(`成功删除 ${selectedWords.value.length} 个敏感词`)
    selectedWords.value = []
    fetchWords()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量删除失败')
    }
  }
}

const openBatchImportDialog = () => {
  batchImportText.value = ''
  showBatchImportDialog.value = true
}

const confirmBatchImport = async () => {
  if (!batchImportText.value.trim()) {
    return ElMessage.warning('请输入要导入的敏感词')
  }

  const wordsToImport = batchImportText.value
    .split('\n')
    .map(w => w.trim())
    .filter(w => w.length > 0)

  if (wordsToImport.length === 0) {
    return ElMessage.warning('没有有效的敏感词')
  }

  try {
    const importPromises = wordsToImport.map(word =>
      request.post('/admin/sensitive/add', { word })
    )

    await Promise.all(importPromises)
    ElMessage.success(`成功导入 ${wordsToImport.length} 个敏感词`)
    showBatchImportDialog.value = false
    fetchWords()
  } catch (e) {
    ElMessage.error('批量导入失败')
  }
}

const handlePageChange = (page) => {
  pagination.current = page
}

import { reactive, watch } from 'vue'

watch(searchQuery, () => {
  pagination.current = 1
  pagination.total = filteredWords.value.length
})

onMounted(fetchWords)
</script>

<template>
  <div class="space-y-4">
    <div class="bg-white rounded-lg shadow-sm p-4 border border-gray-200">
      <div class="flex items-center justify-between mb-4">
        <div class="flex items-center gap-2">
          <h2 class="font-bold text-lg flex items-center gap-2">
            <AlertTriangle class="w-5 h-5 text-orange-600" />
            敏感词库管理
          </h2>
          <span class="text-xs text-gray-500 bg-gray-100 px-2 py-1 rounded-full">
            共 {{ words.length }} 条
          </span>
        </div>
      </div>

      <div class="flex flex-wrap gap-3">
        <div class="flex-1 min-w-[200px]">
          <Input v-model="searchQuery" placeholder="搜索敏感词..." class="w-full">
            <template #prefix>
              <Search class="w-4 h-4 text-gray-400" />
            </template>
          </Input>
        </div>

        <Input v-model="newWord" placeholder="输入新敏感词..." class="w-48" @keyup.enter="addWord" />

        <Button @click="addWord" class="bg-primary hover:opacity-90 text-white">
          <Plus class="w-4 h-4 mr-1" /> 添加
        </Button>

        <Button variant="outline" @click="openBatchImportDialog">
          <Upload class="w-4 h-4 mr-1" /> 批量导入
        </Button>

        <Button v-if="selectedWords.length > 0" variant="destructive" @click="batchDelete">
          <Trash2 class="w-4 h-4 mr-1" /> 批量删除 ({{ selectedWords.length }})
        </Button>
      </div>
    </div>

    <div class="bg-white rounded-lg shadow-sm p-4 border border-gray-200">
      <AdminTable :columns="columns" :data="paginatedWords" :loading="loading" :pagination="pagination"
        :selectable="true" @page-change="handlePageChange" @selection-change="handleSelectionChange">
        <template #word="{ row }">
          <span class="font-medium text-gray-900">{{ row.word }}</span>
        </template>

        <template #category="{ row }">
          <span class="text-sm px-2 py-1 rounded" :class="{
            'bg-red-100 text-red-700': row.category === '政治敏感',
            'bg-purple-100 text-purple-700': row.category === '色情低俗',
            'bg-orange-100 text-orange-700': row.category === '暴力血腥',
            'bg-blue-100 text-blue-700': row.category === '广告推广',
            'bg-gray-100 text-gray-700': row.category === '其他违规'
          }">
            {{ row.category }}
          </span>
        </template>

        <template #createTime="{ row }">
          <span class="text-sm text-gray-600">
            {{ row.createTime ? new Date(row.createTime).toLocaleString('zh-CN', { year:'numeric', month:'2-digit', day:'2-digit', hour:'2-digit', minute:'2-digit', second:'2-digit' }) : '—' }}
          </span>
        </template>

        <template #actions="{ row }">
          <div class="flex items-center justify-end gap-2 flex-wrap">
            <Button size="sm" variant="destructive" @click="deleteWord(row.word)" class="whitespace-nowrap">
              <Trash2 class="w-3 h-3 mr-1" /> 删除
            </Button>
          </div>
        </template>
      </AdminTable>
    </div>

    <Dialog :open="showBatchImportDialog" @update:open="showBatchImportDialog = $event">
      <DialogContent class="max-w-2xl">
        <DialogHeader>
          <DialogTitle class="flex items-center gap-2">
            <Upload class="w-5 h-5 text-primary" />
            批量导入敏感词
          </DialogTitle>
        </DialogHeader>
        <div class="space-y-4 py-4">
          <div class="bg-blue-50 border border-blue-200 rounded-md p-3">
            <p class="text-sm text-blue-800">
              请每行输入一个敏感词，系统将自动去除空行和重复项。
            </p>
          </div>
          <div class="space-y-2">
            <label class="text-sm font-medium text-gray-700">
              敏感词列表 <span class="text-red-500">*</span>
            </label>
            <Textarea v-model="batchImportText" placeholder="每行一个敏感词，例如：&#10;违规词1&#10;违规词2&#10;违规词3"
              class="min-h-[300px] font-mono text-sm" />
            <p class="text-xs text-gray-500">
              当前输入：{{batchImportText.split('\n').filter(w => w.trim()).length}} 个敏感词
            </p>
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" @click="showBatchImportDialog = false">取消</Button>
          <Button class="bg-primary hover:opacity-90 text-white" @click="confirmBatchImport">
            确认导入
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
