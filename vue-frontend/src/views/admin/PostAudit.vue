<script setup>
import { ref, onMounted, reactive, watch, computed } from 'vue'
import request from '@/api/request'
import AdminTable from '@/components/admin/AdminTable.vue'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { Search, Trash2, MessageSquare, FileText, Eye } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { highlightSensitiveWords, containsSensitiveWords } from '@/utils/sensitiveHighlight'

const posts = ref([])
const loading = ref(false)
const activeTab = ref('all')
const sensitiveWords = ref([])
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: null,
  category: null
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0
})

const showDeleteDialog = ref(false)
const showCommentsDialog = ref(false)
const showDetailDialog = ref(false)
const currentPost = ref(null)
const currentComments = ref([])
const deleteReason = ref('')

const reasons = [
  "包含不当词汇或辱骂内容",
  "涉及暴力、恐怖主义言论",
  "发布垃圾广告或推广信息",
  "侵犯他人隐私或权益",
  "散布不实信息或谣言",
  "涉及色情低俗内容",
  "其他违规行为"
]

const categories = ref([
  { id: null, name: '全部分类' }
])

const fetchCategories = async () => {
  try {
    const res = await request.get('/index/categories')
    if (res.code === '200') {
      categories.value = [
        { id: null, name: '全部分类' },
        ...(res.data || [])
      ]
    }
  } catch (e) {
    console.error('获取分类失败', e)
  }
}

const tabs = [
  { key: 'all', label: '全部', status: null },
  { key: 'published', label: '已发布', status: 1 },
  { key: 'scheduled', label: '定时待发布', status: 2 }
]

const columns = [
  {
    key: 'post',
    label: '帖子内容',
    slot: 'post'
  },
  {
    key: 'user',
    label: '发布者',
    slot: 'user'
  },
  {
    key: 'category',
    label: '分类',
    slot: 'category'
  },
  {
    key: 'likeCount',
    label: '点赞数',
    slot: 'likeCount',
    sortable: true
  },
  {
    key: 'status',
    label: '状态',
    slot: 'status'
  },
  {
    key: 'createTime',
    label: '发布时间',
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

const selectedPosts = ref([])
const showBatchDeleteDialog = ref(false)
const batchDeleteReason = ref('')

const fetchList = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/post/list', {
      params: {
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
        keyword: query.keyword,
        status: query.status,
        category: query.category
      }
    })
    if (res.code === '200') {
      posts.value = res.data.list || []
      pagination.total = res.data.total || 0
    }
  } catch (e) {
    ElMessage.error('获取帖子列表失败')
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

const handleCategoryChange = () => {
  pagination.current = 1
  fetchList()
}

const handlePageChange = (page) => {
  pagination.current = page
  fetchList()
}

const openDetailDialog = (post) => {
  currentPost.value = post
  showDetailDialog.value = true
}

const openCommentsDialog = async (post) => {
  currentPost.value = post
  showCommentsDialog.value = true
  try {
    const res = await request.get('/admin/post/comments', {
      params: { postId: post.id }
    })
    if (res.code === '200') {
      currentComments.value = res.data || []
    }
  } catch (e) {
    ElMessage.error('获取评论失败')
  }
}

const openDeleteDialog = (post) => {
  currentPost.value = post
  deleteReason.value = reasons[0]
  showDeleteDialog.value = true
}

const confirmDelete = async () => {
  if (!deleteReason.value.trim()) {
    return ElMessage.warning('请选择或填写删除理由')
  }

  try {
    await request.post('/admin/post/delete', {
      id: currentPost.value.id,
      reason: deleteReason.value
    })
    ElMessage.success('删除成功')
    showDeleteDialog.value = false
    fetchList()
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || '操作失败')
  }
}

const deleteComment = async (commentId) => {
  try {
    await request.post('/admin/comment/delete', {
      id: commentId,
      reason: '违规评论'
    })
    ElMessage.success('删除成功')
    openCommentsDialog(currentPost.value)
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const getCategoryName = (categoryId) => {
  const cat = categories.value.find(c => c.id === categoryId)
  return cat ? cat.name : '未分类'
}

const fetchSensitiveWords = async () => {
  try {
    const res = await request.get('/admin/sensitive/list')
    if (res.code === '200') {
      sensitiveWords.value = (res.data || []).map(item => typeof item === 'string' ? item : item.word).filter(Boolean)
    }
  } catch (e) {
    console.error('获取敏感词列表失败', e)
  }
}

const highlightContent = (content) => {
  return highlightSensitiveWords(content, sensitiveWords.value)
}

const hasSensitiveWords = (content) => {
  return containsSensitiveWords(content, sensitiveWords.value)
}

const handleSelectionChange = (selection) => {
  selectedPosts.value = selection
}

const openBatchDeleteDialog = () => {
  if (selectedPosts.value.length === 0) {
    return ElMessage.warning('请先选择要删除的帖子')
  }
  batchDeleteReason.value = reasons[0]
  showBatchDeleteDialog.value = true
}

const confirmBatchDelete = async () => {
  if (!batchDeleteReason.value.trim()) {
    return ElMessage.warning('请选择或填写删除理由')
  }

  try {
    const deletePromises = selectedPosts.value.map(post =>
      request.post('/admin/post/delete', {
        id: post.id,
        reason: batchDeleteReason.value
      })
    )

    await Promise.all(deletePromises)
    ElMessage.success(`成功删除 ${selectedPosts.value.length} 条帖子`)
    showBatchDeleteDialog.value = false
    selectedPosts.value = []
    fetchList()
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || '批量删除失败')
  }
}

watch(() => query.keyword, () => {
  pagination.current = 1
  fetchList()
})

onMounted(() => {
  fetchList()
  fetchSensitiveWords()
  fetchCategories()
})
</script>

<template>
  <div class="space-y-4">
    <div class="bg-white rounded-lg shadow-sm p-4 border border-gray-200">
      <div class="flex items-center justify-between mb-4">
        <h2 class="font-bold text-lg flex items-center gap-2">
          <FileText class="w-5 h-5 text-primary" />
          内容管理
        </h2>
      </div>

      <div class="flex items-center gap-3 mb-4 pb-3 border-b border-gray-200">
        <button v-for="tab in tabs" :key="tab.key" @click="handleTabChange(tab)" :class="[
          'px-4 py-2 rounded-lg text-sm font-medium transition-all',
          activeTab === tab.key
            ? 'bg-primary text-white shadow-sm'
            : 'text-gray-600 hover:bg-gray-100'
        ]">
          {{ tab.label }}
        </button>
      </div>

      <div class="flex flex-wrap gap-3">
        <div class="flex-1 min-w-[200px]">
          <Input v-model="query.keyword" placeholder="搜索内容或用户..." class="w-full">
            <template #prefix>
              <Search class="w-4 h-4 text-gray-400" />
            </template>
          </Input>
        </div>

        <select v-model="query.category" @change="handleCategoryChange"
          class="px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary">
          <option v-for="cat in categories" :key="cat.id" :value="cat.id">
            {{ cat.name }}
          </option>
        </select>

        <Button v-if="selectedPosts.length > 0" variant="destructive" @click="openBatchDeleteDialog">
          <Trash2 class="w-4 h-4 mr-1" /> 批量删除 ({{ selectedPosts.length }})
        </Button>
      </div>
    </div>

    <div class="bg-white rounded-lg shadow-sm p-4 border border-gray-200">
      <AdminTable :columns="columns" :data="posts" :loading="loading" :pagination="pagination" :selectable="true"
        @page-change="handlePageChange" @selection-change="handleSelectionChange" @sort-change="handleSortChange">
        <template #post="{ row }">
          <div class="max-w-md">
            <div class="flex items-start gap-2">
              <p class="text-sm text-gray-900 line-clamp-2 flex-1 break-words overflow-hidden"
                v-html="highlightContent(row.content)"></p>
              <Badge v-if="hasSensitiveWords(row.content)"
                class="bg-red-100 text-red-700 border-none text-xs flex-shrink-0">
                敏感
              </Badge>
            </div>
            <div v-if="row.imgList && row.imgList.length" class="flex gap-1 mt-2 flex-wrap">
              <img v-for="(img, i) in row.imgList.slice(0, 3)" :key="i"
                :src="img.startsWith('http') ? img : `http://localhost:19090${img}`" alt="帖子图片"
                class="w-12 h-12 object-cover rounded border" @error="(e) => e.target.src = '/图片加载失败.png'" />
              <span v-if="row.imgList.length > 3" class="text-xs text-gray-500 self-center ml-1">
                +{{ row.imgList.length - 3 }}
              </span>
            </div>
          </div>
        </template>

        <template #user="{ row }">
          <div class="flex items-center gap-2 min-w-0">
            <img :src="row.avatar" alt="头像" class="w-8 h-8 rounded-full border flex-shrink-0"
              @error="(e) => e.target.src = '/default.png'" />
            <div class="min-w-0 flex-1">
              <div class="text-sm font-medium text-gray-900 truncate">{{ row.nickname }}</div>
              <div class="text-xs text-gray-500 truncate">ID: {{ row.user_id }}</div>
            </div>
          </div>
        </template>

        <template #category="{ row }">
          <Badge class="bg-blue-100 text-blue-700 border-none">
            {{ getCategoryName(row.category) }}
          </Badge>
        </template>

        <template #status="{ row }">
          <Badge v-if="row.status === 1" class="bg-green-100 text-green-700 border-none">
            已发布
          </Badge>
          <Badge v-else-if="row.status === 2" class="bg-blue-100 text-blue-700 border-none">
            定时待发布
          </Badge>
          <Badge v-else-if="row.status === -1" class="bg-red-100 text-red-700 border-none">
            已删除
          </Badge>
          <Badge v-else class="bg-gray-100 text-gray-700 border-none">
            未知
          </Badge>
        </template>

        <template #createTime="{ row }">
          <span class="text-sm text-gray-600">
            {{ new Date(row.create_time).toLocaleString('zh-CN') }}
          </span>
        </template>

        <template #actions="{ row }">
          <div class="flex items-center justify-end gap-2 flex-wrap">
            <Button size="sm" variant="outline" @click="openDetailDialog(row)" class="whitespace-nowrap">
              <Eye class="w-3 h-3 mr-1" /> 详情
            </Button>
            <Button size="sm" variant="outline" @click="openCommentsDialog(row)" class="whitespace-nowrap">
              <MessageSquare class="w-3 h-3 mr-1" /> 评论({{ row.comment_count || 0 }})
            </Button>
            <Button size="sm" variant="destructive" @click="openDeleteDialog(row)" class="whitespace-nowrap">
              <Trash2 class="w-3 h-3 mr-1" /> 删除
            </Button>
          </div>
        </template>
      </AdminTable>
    </div>

    <Dialog :open="showDetailDialog" @update:open="showDetailDialog = $event">
      <DialogContent class="max-w-2xl max-h-[85vh] flex flex-col">
        <DialogHeader class="flex-shrink-0">
          <DialogTitle class="flex items-center gap-2">
            <Eye class="w-5 h-5 text-primary" />
            帖子详情
          </DialogTitle>
        </DialogHeader>
        <div v-if="currentPost" class="flex-1 overflow-y-auto min-h-0 space-y-4 py-4">
          <div class="flex items-start gap-3">
            <img :src="currentPost.avatar" alt="头像" class="w-12 h-12 rounded-full border flex-shrink-0"
              @error="(e) => e.target.src = '/default.png'" />
            <div class="flex-1 min-w-0">
              <div class="font-medium text-gray-900 truncate">{{ currentPost.nickname }}</div>
              <div class="text-sm text-gray-500">{{ new Date(currentPost.create_time).toLocaleString('zh-CN') }}</div>
            </div>
            <Badge v-if="currentPost.status === 1" class="bg-green-100 text-green-700 border-none flex-shrink-0">
              已发布
            </Badge>
            <Badge v-else-if="currentPost.status === 2" class="bg-blue-100 text-blue-700 border-none flex-shrink-0">
              定时待发布
            </Badge>
            <Badge v-else-if="currentPost.status === -1" class="bg-red-100 text-red-700 border-none flex-shrink-0">
              已删除
            </Badge>
            <Badge v-else class="bg-gray-100 text-gray-700 border-none flex-shrink-0">
              未知
            </Badge>
          </div>

          <div class="bg-gray-50 rounded-lg p-4 border border-gray-200">
            <div class="text-gray-900 whitespace-pre-wrap break-words overflow-wrap-anywhere"
              v-html="highlightContent(currentPost.content)"></div>
          </div>

          <div v-if="currentPost.imgList && currentPost.imgList.length" class="grid grid-cols-3 gap-2">
            <img v-for="(img, i) in currentPost.imgList" :key="i"
              :src="img.startsWith('http') ? img : `http://localhost:19090${img}`" alt="帖子图片"
              class="w-full h-32 object-cover rounded-lg border" @error="(e) => e.target.src = '/图片加载失败.png'" />
          </div>

          <div class="grid grid-cols-2 gap-4 text-sm">
            <div>
              <span class="text-gray-500">分类：</span>
              <span class="text-gray-900">{{ getCategoryName(currentPost.category) }}</span>
            </div>
            <div>
              <span class="text-gray-500">评论数：</span>
              <span class="text-gray-900">{{ currentPost.comment_count || 0 }}</span>
            </div>
          </div>
        </div>
        <DialogFooter class="flex-shrink-0">
          <Button variant="outline" @click="showDetailDialog = false">关闭</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <Dialog :open="showCommentsDialog" @update:open="showCommentsDialog = $event">
      <DialogContent class="max-w-2xl max-h-[85vh] flex flex-col">
        <DialogHeader class="flex-shrink-0">
          <DialogTitle class="flex items-center gap-2">
            <MessageSquare class="w-5 h-5 text-primary" />
            评论管理
          </DialogTitle>
        </DialogHeader>
        <div class="flex-1 overflow-y-auto min-h-0 space-y-3 py-4">
          <div v-if="currentComments.length === 0" class="text-center py-8 text-gray-500">
            暂无评论
          </div>
          <div v-else v-for="comment in currentComments" :key="comment.id"
            class="flex items-start gap-3 p-3 bg-gray-50 rounded-lg border border-gray-200 hover:border-primary transition-colors">
            <img :src="comment.avatar" alt="头像" class="w-8 h-8 rounded-full border flex-shrink-0"
              @error="(e) => e.target.src = '/default.png'" />
            <div class="flex-1 min-w-0">
              <div class="flex items-center justify-between mb-1 gap-2">
                <span class="text-sm font-medium text-gray-900 truncate">{{ comment.nickname }}</span>
                <span class="text-xs text-gray-500 whitespace-nowrap flex-shrink-0">{{ new
                  Date(comment.create_time).toLocaleString('zh-CN') }}</span>
              </div>
              <p class="text-sm text-gray-700 break-words">{{ comment.content }}</p>
            </div>
            <Button size="sm" variant="destructive" @click="deleteComment(comment.id)" class="flex-shrink-0">
              <Trash2 class="w-3 h-3" />
            </Button>
          </div>
        </div>
        <DialogFooter class="flex-shrink-0">
          <Button variant="outline" @click="showCommentsDialog = false">关闭</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <Dialog :open="showDeleteDialog" @update:open="showDeleteDialog = $event">
      <DialogContent>
        <DialogHeader>
          <DialogTitle class="flex items-center gap-2 text-red-600">
            <Trash2 class="w-5 h-5" />
            删除帖子
          </DialogTitle>
        </DialogHeader>
        <div class="space-y-4 py-4">
          <div class="bg-red-50 border border-red-200 rounded-md p-3">
            <p class="text-sm text-red-800">
              删除后将无法恢复，请谨慎操作。
            </p>
          </div>
          <div class="space-y-2">
            <label class="text-sm font-medium text-gray-700">
              删除理由 <span class="text-red-500">*</span>
            </label>
            <select v-model="deleteReason"
              class="w-full p-2.5 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary">
              <option v-for="reason in reasons" :key="reason" :value="reason">
                {{ reason }}
              </option>
            </select>
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" @click="showDeleteDialog = false">取消</Button>
          <Button variant="destructive" @click="confirmDelete">确认删除</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <Dialog :open="showBatchDeleteDialog" @update:open="showBatchDeleteDialog = $event">
      <DialogContent>
        <DialogHeader>
          <DialogTitle class="flex items-center gap-2 text-red-600">
            <Trash2 class="w-5 h-5" />
            批量删除帖子
          </DialogTitle>
        </DialogHeader>
        <div class="space-y-4 py-4">
          <div class="bg-red-50 border border-red-200 rounded-md p-3">
            <p class="text-sm text-red-800">
              即将删除 <span class="font-bold">{{ selectedPosts.length }}</span> 条帖子，删除后将无法恢复。
            </p>
          </div>
          <div class="space-y-2">
            <label class="text-sm font-medium text-gray-700">
              删除理由 <span class="text-red-500">*</span>
            </label>
            <select v-model="batchDeleteReason"
              class="w-full p-2.5 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary">
              <option v-for="reason in reasons" :key="reason" :value="reason">
                {{ reason }}
              </option>
            </select>
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" @click="showBatchDeleteDialog = false">取消</Button>
          <Button variant="destructive" @click="confirmBatchDelete">确认批量删除</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>

<style scoped>
svg {
  flex-shrink: 0;
}

:deep(.sensitive-word) {
  background-color: #fee2e2;
  color: #991b1b;
  padding: 2px 4px;
  border-radius: 3px;
  font-weight: 600;
  border: 1px solid #fca5a5;
}
</style>
