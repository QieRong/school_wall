<script setup>
import { ref, onMounted } from 'vue'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { Plus, Trash2, Edit, Upload, AlertTriangle, Loader2, LayoutGrid, Image as ImageIcon, Crop } from 'lucide-vue-next'
import AdminTable from '@/components/admin/AdminTable.vue'
import request from '@/api/request'
import { useAppStore } from '@/stores/app'
import { validateImageFile } from '@/utils/fileValidator'
import { Cropper } from 'vue-advanced-cropper'
import 'vue-advanced-cropper/dist/style.css'

const appStore = useAppStore()
const banners = ref([])
const loading = ref(false)
const showDialog = ref(false)
const isEdit = ref(false)
const currentBanner = ref({ id: null, title: '', imageUrl: '', sort: 0 })
const fileInput = ref(null)
const uploading = ref(false)

// 裁剪相关状态
const cropperRef = ref(null)
const cropperSrc = ref('')     // 待裁剪原图的 dataURL
const showCropper = ref(false) // 是否显示裁剪器

// 表格列配置
const columns = [
  { key: 'id', label: 'ID', width: '20', sortable: true },
  { key: 'imageUrl', label: '图片预览', slot: 'image' },
  { key: 'title', label: '标题', sortable: true },
  { key: 'sort', label: '排序', width: '24', sortable: true },
  { key: 'actions', label: '操作', slot: 'actions', width: '32' }
]

const fetchBanners = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/banner/list')
    if (res.code === '200') {
      banners.value = res.data.sort((a, b) => a.sort - b.sort)
    }
  } catch (e) {
    appStore.showToast('获取轮播图失败', 'error')
  } finally {
    loading.value = false
  }
}

const reorderBanners = async () => {
  if (banners.value.length === 0) return
  loading.value = true
  try {
    const sorted = banners.value
      .sort((a, b) => a.sort - b.sort)
      .map((banner, index) => ({ ...banner, sort: index + 1 }))
    for (const banner of sorted) {
      await request.post('/admin/banner/update', banner)
    }
    appStore.showToast('✅ 排序已重新整理', 'success')
    await fetchBanners()
  } catch (e) {
    appStore.showToast('重排序失败', 'error')
  } finally {
    loading.value = false
  }
}

const openAddDialog = () => {
  isEdit.value = false
  const maxSort = banners.value.length > 0
    ? Math.max(...banners.value.map(b => b.sort || 0))
    : 0
  currentBanner.value = { id: null, title: '', imageUrl: '', sort: maxSort + 1 }
  cropperSrc.value = ''
  showCropper.value = false
  showDialog.value = true
}

const openEditDialog = (banner) => {
  isEdit.value = true
  currentBanner.value = { ...banner }
  cropperSrc.value = ''
  showCropper.value = false
  showDialog.value = true
}

// 选择文件后进入裁剪模式
const handleFileChange = (e) => {
  const file = e.target.files[0]
  if (!file) return

  const validation = validateImageFile(file)
  if (!validation.valid) {
    appStore.showToast(validation.error, 'error')
    e.target.value = ''
    return
  }

  // 读取文件为 dataURL，显示裁剪器
  const reader = new FileReader()
  reader.onload = (ev) => {
    cropperSrc.value = ev.target.result
    showCropper.value = true
  }
  reader.readAsDataURL(file)
  e.target.value = ''
}

// 确认裁剪并上传
const confirmCrop = async () => {
  if (!cropperRef.value) return

  uploading.value = true
  try {
    const { canvas } = cropperRef.value.getResult()
    if (!canvas) {
      appStore.showToast('裁剪失败', 'error')
      return
    }

    // 新建 canvas 先填白色背景，避免透明区域转 JPEG 后变灰
    const finalCanvas = document.createElement('canvas')
    finalCanvas.width = canvas.width
    finalCanvas.height = canvas.height
    const ctx = finalCanvas.getContext('2d')
    ctx.fillStyle = '#ffffff'
    ctx.fillRect(0, 0, finalCanvas.width, finalCanvas.height)
    ctx.drawImage(canvas, 0, 0)

    const blob = await new Promise(resolve => {
      finalCanvas.toBlob(resolve, 'image/jpeg', 0.85)
    })

    const formData = new FormData()
    formData.append('file', blob, 'banner_cropped.jpg')
    formData.append('userId', 1)

    const res = await request.post('/file/upload', formData)
    if (res.code === '200') {
      currentBanner.value.imageUrl = res.data
      showCropper.value = false
      cropperSrc.value = ''
      appStore.showToast('裁剪并上传成功', 'success')
    }
  } catch (err) {
    console.error('裁剪上传失败:', err)
    appStore.showToast('上传失败', 'error')
  } finally {
    uploading.value = false
  }
}

// 取消裁剪
const cancelCrop = () => {
  showCropper.value = false
  cropperSrc.value = ''
}

const saveBanner = async () => {
  if (!currentBanner.value.title || !currentBanner.value.imageUrl) {
    appStore.showToast('请填写标题并上传图片', 'error')
    return
  }
  if (currentBanner.value.sort < 1) currentBanner.value.sort = 1
  currentBanner.value.sort = Math.floor(currentBanner.value.sort)

  try {
    const url = isEdit.value ? '/admin/banner/update' : '/admin/banner/add'
    const res = await request.post(url, currentBanner.value)
    if (res.code === '200') {
      appStore.showToast(isEdit.value ? '修改成功' : '添加成功', 'success')
      showDialog.value = false
      fetchBanners()
    }
  } catch (e) {
    appStore.showToast('操作失败', 'error')
  }
}

const showDeleteDialog = ref(false)
const deleteTargetId = ref(null)
const deleting = ref(false)

const openDeleteDialog = (id) => {
  deleteTargetId.value = id
  showDeleteDialog.value = true
}

const confirmDelete = async () => {
  if (!deleteTargetId.value || deleting.value) return
  deleting.value = true
  try {
    const res = await request.post('/admin/banner/delete', { id: deleteTargetId.value })
    if (res.code === '200') {
      appStore.showToast('删除成功', 'success')
      showDeleteDialog.value = false
      deleteTargetId.value = null
      fetchBanners()
    }
  } catch (e) {
    appStore.showToast('删除失败', 'error')
  } finally {
    deleting.value = false
  }
}

// 阻止外部区域点击关闭弹窗
const preventOutsideClose = (e) => {
  e.preventDefault()
}

onMounted(fetchBanners)
</script>

<template>
  <div class="p-6">
    <div class="flex justify-between items-center mb-6">
      <h1 class="text-2xl font-bold text-gray-800">轮播图管理</h1>
      <div class="flex gap-2">
        <Button @click="reorderBanners" variant="outline" :disabled="loading || banners.length === 0">
          <LayoutGrid class="w-4 h-4 mr-2" /> 重新排序
        </Button>
        <Button @click="openAddDialog" class="bg-[rgb(33,111,85)] hover:bg-[rgb(28,95,72)]">
          <Plus class="w-4 h-4 mr-2" /> 添加轮播图
        </Button>
      </div>
    </div>

    <AdminTable :columns="columns" :data="banners" :loading="loading">
      <template #image="{ row }">
        <div class="flex items-center gap-3">
          <div class="w-16 h-16 rounded-lg overflow-hidden bg-gray-100 flex-shrink-0">
            <img v-if="row.imageUrl" :src="row.imageUrl" :alt="row.title" class="w-full h-full object-cover" />
            <div v-else class="w-full h-full flex items-center justify-center">
              <ImageIcon class="w-6 h-6 text-gray-400" />
            </div>
          </div>
        </div>
      </template>

      <template #actions="{ row }">
        <div class="flex items-center gap-2 flex-wrap">
          <Button size="sm" variant="outline" @click="openEditDialog(row)" class="whitespace-nowrap">
            <Edit class="w-4 h-4 mr-1" /> 编辑
          </Button>
          <Button size="sm" variant="destructive" @click="openDeleteDialog(row.id)" class="whitespace-nowrap">
            <Trash2 class="w-4 h-4 mr-1" /> 删除
          </Button>
        </div>
      </template>
    </AdminTable>

    <!-- 添加/编辑弹窗 -->
    <Dialog :open="showDialog" @update:open="val => { if (val === false) return; showDialog = val }">
      <DialogContent class="sm:max-w-[600px] max-h-[90vh] flex flex-col"
        @interactOutside="preventOutsideClose"
        @pointerDownOutside="preventOutsideClose">
        <DialogHeader class="flex-shrink-0">
          <DialogTitle>{{ isEdit ? '编辑轮播图' : '添加轮播图' }}</DialogTitle>
        </DialogHeader>

        <!-- 可滚动内容区 -->
        <div class="flex-1 overflow-y-auto min-h-0 space-y-4 py-4 pr-1">
          <div class="space-y-2">
            <Label>标题</Label>
            <Input v-model="currentBanner.title" placeholder="请输入轮播图标题" />
          </div>

          <div class="space-y-2">
            <Label>图片</Label>

            <!-- 裁剪器 -->
            <div v-if="showCropper" class="space-y-3">
              <div class="bg-gray-50 border border-gray-200 rounded-lg p-2">
                <p class="text-xs text-gray-600 mb-2 flex items-center gap-1">
                  <Crop class="w-3.5 h-3.5" />
                  拖动选择框裁剪轮播图区域（推荐 8:3 比例）
                </p>
                <div class="h-64 bg-black/5 rounded-lg overflow-hidden">
                  <Cropper
                    ref="cropperRef"
                    :src="cropperSrc"
                    :stencil-props="{
                      aspectRatio: 8 / 3,
                    }"
                    class="h-full"
                  />
                </div>
              </div>

              <div class="flex gap-2">
                <Button variant="outline" @click="cancelCrop" class="flex-1" :disabled="uploading">
                  取消
                </Button>
                <Button @click="confirmCrop" class="flex-1 bg-[rgb(33,111,85)] hover:bg-[rgb(28,95,72)]"
                  :disabled="uploading">
                  <Loader2 v-if="uploading" class="w-4 h-4 mr-2 animate-spin" />
                  <Crop v-else class="w-4 h-4 mr-2" />
                  {{ uploading ? '上传中...' : '确认裁剪并上传' }}
                </Button>
              </div>
            </div>

            <!-- 当前图片预览 -->
            <div v-if="currentBanner.imageUrl && !showCropper" class="relative rounded-lg overflow-hidden h-40 mb-2 bg-gray-100">
              <img :src="currentBanner.imageUrl" class="w-full h-full object-cover" />
              <button @click="currentBanner.imageUrl = ''"
                class="absolute top-2 right-2 bg-red-500 text-white rounded-full p-1 hover:bg-red-600 transition-colors">
                <Trash2 class="w-4 h-4" />
              </button>
            </div>

            <!-- 选择图片按钮 -->
            <div v-if="!showCropper">
              <div class="bg-blue-50 border border-blue-200 rounded-lg p-3 mb-2 flex items-start gap-2">
                <ImageIcon class="w-5 h-5 text-blue-600 flex-shrink-0 mt-0.5" />
                <div class="text-xs text-blue-800">
                  <p class="font-medium">📐 推荐尺寸：1200 × 450px（8:3 比例）</p>
                  <p class="text-blue-600 mt-1">💡 选择图片后可自由裁剪为轮播图合适的区域</p>
                </div>
              </div>
              <Button variant="outline" @click="fileInput.click()" :disabled="uploading" class="w-full">
                <Upload class="w-4 h-4 mr-2" />
                {{ uploading ? '上传中...' : '选择图片' }}
              </Button>
              <input type="file" ref="fileInput" accept="image/*" class="hidden" @change="handleFileChange" />
            </div>
          </div>

          <div class="space-y-2">
            <Label>排序 (数字越小越靠前)</Label>
            <Input v-model.number="currentBanner.sort" type="number" placeholder="1" min="1" />
          </div>
        </div>

        <DialogFooter class="flex-shrink-0">
          <Button variant="outline" @click="showDialog = false">取消</Button>
          <Button @click="saveBanner" class="bg-[rgb(33,111,85)] hover:bg-[rgb(28,95,72)]"
            :disabled="showCropper">保存</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- 删除确认弹窗 -->
    <Dialog :open="showDeleteDialog" @update:open="showDeleteDialog = $event">
      <DialogContent class="sm:max-w-[380px]">
        <DialogHeader>
          <DialogTitle class="text-red-600 flex items-center gap-2">
            <AlertTriangle class="w-5 h-5" /> 删除确认
          </DialogTitle>
        </DialogHeader>
        <div class="py-4">
          <p class="text-gray-600">确定要删除这个轮播图吗？</p>
          <p class="text-sm text-gray-400 mt-2">删除后将无法恢复。</p>
        </div>
        <DialogFooter class="gap-2">
          <Button variant="outline" @click="showDeleteDialog = false" :disabled="deleting">取消</Button>
          <Button variant="destructive" @click="confirmDelete" :disabled="deleting">
            <Loader2 v-if="deleting" class="w-4 h-4 mr-2 animate-spin" />
            {{ deleting ? '删除中...' : '确认删除' }}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>

<style scoped>
/* 确保裁剪器在弹窗内正常显示 */
:deep(.vue-advanced-cropper) {
  max-height: 100%;
}
</style>
