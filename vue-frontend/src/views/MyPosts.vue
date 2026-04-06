<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Button } from '@/components/ui/button'
import { Avatar, AvatarImage, AvatarFallback } from '@/components/ui/avatar'
import { Badge } from '@/components/ui/badge'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { getUserPosts } from '@/api/user'
import { useAppStore } from '@/stores/app'
import {
  ArrowLeft, Heart, MessageCircle, Eye, Trash2, Edit,
  Plus, FileText, Clock, MoreHorizontal, AlertTriangle, Loader2
} from 'lucide-vue-next'
import request from '@/api/request'

const router = useRouter()
const appStore = useAppStore()

const loading = ref(true)
const posts = ref([])
const currentUser = computed(() => JSON.parse(localStorage.getItem('user') || '{}'))

//删除确认弹窗状态
const showDeleteDialog = ref(false)
const deleteTarget = ref(null)
const deleting = ref(false)

const allCategories = ref([])
const fetchCategories = async () => {
  try {
    const res = await request.get('/index/categories')
    if (res.code === '200') allCategories.value = res.data || []
  } catch (e) {
    console.error('加载分区失败', e)
  }
}

const categoryMap = computed(() => {
  const map = {}
  const styleMap = {
    1: 'bg-pink-100 text-pink-600',
    2: 'bg-blue-100 text-blue-600',
    3: 'bg-orange-100 text-orange-600',
    4: 'bg-purple-100 text-purple-600',
    5: 'bg-green-100 text-green-600',
    6: 'bg-cyan-100 text-cyan-600',
    7: 'bg-red-100 text-red-600',
    8: 'bg-gray-100 text-gray-600'
  }
  allCategories.value.forEach(c => {
    map[c.id] = {
      name: c.name,
      color: styleMap[c.id] || 'bg-emerald-100 text-emerald-600'
    }
  })
  return map
})
const loadPosts = async () => {
  if (!currentUser.value?.id) return
  loading.value = true
  try {
    const res = await getUserPosts(currentUser.value.id, currentUser.value.id)
    if (res.code === '200') {
      posts.value = res.data.list || res.data || []
      posts.value.forEach(p => {
        if (p.images) {
          try {
            p.imgList = p.images.startsWith('[') ? JSON.parse(p.images) : p.images.split(',')
          } catch { p.imgList = p.images.split(',') }
        } else {
          p.imgList = []
        }
      })
    }
  } catch (e) {
    appStore.showToast('加载失败', 'error')
  } finally {
    loading.value = false
  }
}

//打开删除确认弹窗
const openDeleteDialog = (post) => {
  deleteTarget.value = post
  showDeleteDialog.value = true
}

//确认删除
const confirmDelete = async () => {
  if (!deleteTarget.value || deleting.value) return

  deleting.value = true
  try {
    const res = await request.delete(`/post/${deleteTarget.value.id}`, { params: { userId: currentUser.value.id } })
    if (res.code === '200') {
      posts.value = posts.value.filter(p => p.id !== deleteTarget.value.id)
      appStore.showToast('✅ 删除成功', 'success')
      showDeleteDialog.value = false
      deleteTarget.value = null
    } else {
      appStore.showToast(res.msg || '删除失败', 'error')
    }
  } catch (e) {
    appStore.showToast('删除失败，请重试', 'error')
  } finally {
    deleting.value = false
  }
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  return date.toLocaleDateString()
}

onMounted(() => {
  loadPosts()
  fetchCategories()
})
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-gray-50 via-emerald-50/30 to-teal-50/20 pb-20">
    <!-- 装饰背景 -->
    <div class="fixed inset-0 pointer-events-none overflow-hidden">
      <div
        class="absolute top-32 -right-32 w-80 h-80 bg-gradient-to-br from-emerald-100/40 to-teal-100/40 rounded-full blur-3xl">
      </div>
      <div
        class="absolute bottom-32 -left-32 w-64 h-64 bg-gradient-to-br from-cyan-100/40 to-blue-100/40 rounded-full blur-3xl">
      </div>
    </div>

    <!-- 顶部导航 -->
    <header class="bg-white/80 backdrop-blur-md shadow-sm border-b border-white/20 sticky top-0 z-40">
      <div class="max-w-3xl mx-auto px-4 py-3 flex justify-between items-center">
        <div class="flex items-center gap-3">
          <Button variant="ghost" size="icon" @click="router.back()"
            class="hover:bg-emerald-50 hover:text-[rgb(33,111,85)] rounded-full">
            <ArrowLeft class="w-5 h-5" />
          </Button>
          <h1
            class="text-xl font-bold bg-gradient-to-r from-[rgb(33,111,85)] to-emerald-600 bg-clip-text text-transparent">
            我的帖子</h1>
        </div>
        <Button @click="router.push('/home')"
          class="bg-gradient-to-r from-[rgb(33,111,85)] to-emerald-600 hover:from-[rgb(28,95,72)] hover:to-emerald-700 text-white px-4 py-2 rounded-full font-medium shadow-lg shadow-emerald-500/25 transition-all duration-300 hover:scale-105">
          <Plus class="w-4 h-4 mr-1" />
          发帖
        </Button>
      </div>
    </header>

    <!-- 主内容 -->
    <main class="max-w-3xl mx-auto px-4 py-6 relative z-10">
      <!-- 统计卡片 -->
      <div class="bg-white/80 backdrop-blur-md rounded-2xl p-6 shadow-lg border border-white/20 mb-6">
        <div class="flex items-center gap-4">
          <div
            class="w-14 h-14 bg-gradient-to-br from-[rgb(33,111,85)] to-emerald-600 rounded-2xl flex items-center justify-center shadow-lg">
            <FileText class="w-7 h-7 text-white" />
          </div>
          <div>
            <h3 class="text-2xl font-bold text-gray-800">{{ posts.length }}</h3>
            <p class="text-sm text-gray-500">篇帖子</p>
          </div>
        </div>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="space-y-4">
        <div v-for="i in 3" :key="i"
          class="bg-white/80 backdrop-blur-md rounded-2xl p-6 shadow-lg border border-white/20 animate-pulse">
          <div class="flex items-center gap-3 mb-4">
            <div class="w-10 h-10 bg-gradient-to-br from-gray-200 to-gray-300 rounded-full"></div>
            <div class="flex-1">
              <div class="h-4 bg-gradient-to-r from-gray-200 to-gray-300 rounded-full w-24 mb-2"></div>
              <div class="h-3 bg-gradient-to-r from-gray-200 to-gray-300 rounded-full w-32"></div>
            </div>
          </div>
          <div class="space-y-2">
            <div class="h-4 bg-gradient-to-r from-gray-200 to-gray-300 rounded-full"></div>
            <div class="h-4 bg-gradient-to-r from-gray-200 to-gray-300 rounded-full w-3/4"></div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else-if="posts.length === 0"
        class="bg-white/80 backdrop-blur-md rounded-2xl p-12 text-center shadow-lg border border-white/20">
        <div
          class="w-20 h-20 mx-auto mb-6 bg-gradient-to-br from-gray-100 to-gray-200 rounded-full flex items-center justify-center">
          <FileText class="w-10 h-10 text-gray-400" />
        </div>
        <p class="text-xl font-bold text-gray-600 mb-2">还没有发布过帖子</p>
        <p class="text-sm text-gray-500 mb-6">快去分享你的故事吧！✨</p>
        <Button @click="router.push('/home')"
          class="bg-gradient-to-r from-[rgb(33,111,85)] to-emerald-600 hover:from-[rgb(28,95,72)] hover:to-emerald-700 text-white px-8 py-3 rounded-full font-bold shadow-lg shadow-emerald-500/25 transition-all duration-300 hover:scale-105">
          <Plus class="w-4 h-4 mr-2" />
          立即发帖
        </Button>
      </div>

      <!-- 帖子列表 -->
      <div v-else class="space-y-5">
        <div v-for="post in posts" :key="post.id"
          class="bg-white/80 backdrop-blur-md rounded-2xl p-6 shadow-lg border border-white/20 hover:shadow-xl transition-all duration-300 cursor-pointer group"
          @click="router.push(`/post/${post.id}`)">
          <!-- 帖子头部 -->
          <div class="flex items-center justify-between mb-4">
            <div class="flex items-center gap-3">
              <Badge :class="categoryMap[post.category]?.color || 'bg-gray-100 text-gray-600'"
                class="px-3 py-1 rounded-full text-xs font-medium">
                {{ categoryMap[post.category]?.name || '其他' }}
              </Badge>
              <span class="text-xs text-gray-400 flex items-center gap-1">
                <Clock class="w-3 h-3" />
                {{ formatTime(post.createTime) }}
              </span>
            </div>
            <div class="flex items-center gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
              <Button variant="ghost" size="sm" class="h-8 w-8 p-0 rounded-full hover:bg-red-50 hover:text-red-500"
                @click.stop="openDeleteDialog(post)">
                <Trash2 class="w-4 h-4" />
              </Button>
            </div>
          </div>

          <!-- 帖子内容 -->
          <p class="text-gray-800 leading-relaxed mb-4 line-clamp-3">{{ post.content }}</p>

          <!-- 图片预览 -->
          <div v-if="post.imgList && post.imgList.length > 0" class="mb-4">
            <div class="flex gap-2 overflow-hidden rounded-xl">
              <img v-for="(img, idx) in post.imgList.slice(0, 3)" :key="idx" :src="img" loading="lazy"
                class="w-20 h-20 object-cover rounded-lg bg-gray-100" @click.stop />
              <div v-if="post.imgList.length > 3"
                class="w-20 h-20 bg-gray-100 rounded-lg flex items-center justify-center text-gray-500 text-sm font-medium">
                +{{ post.imgList.length - 3 }}
              </div>
            </div>
          </div>

          <!-- 帖子底部 -->
          <div class="flex items-center gap-6 pt-4 border-t border-gray-100/50 text-sm text-gray-500">
            <span class="flex items-center gap-2">
              <Eye class="w-4 h-4" />
              {{ post.viewCount || 0 }}
            </span>
            <span class="flex items-center gap-2">
              <Heart class="w-4 h-4" />
              {{ post.likeCount || 0 }}
            </span>
            <span class="flex items-center gap-2">
              <MessageCircle class="w-4 h-4" />
              {{ post.commentCount || 0 }}
            </span>
          </div>
        </div>
      </div>
    </main>

    <!--删除确认弹窗 -->
    <Dialog :open="showDeleteDialog" @update:open="showDeleteDialog = $event">
      <DialogContent class="sm:max-w-[400px] bg-white rounded-xl">
        <DialogHeader>
          <DialogTitle class="text-lg font-bold text-red-600 flex items-center gap-2">
            <AlertTriangle class="w-5 h-5" /> 删除确认
          </DialogTitle>
        </DialogHeader>
        <div class="py-4">
          <p class="text-gray-600 mb-2">确定要删除这条帖子吗？</p>
          <p class="text-sm text-gray-400">删除后将无法恢复，相关的评论和点赞也会一并删除。</p>
          <div v-if="deleteTarget" class="mt-4 p-3 bg-gray-50 rounded-lg border border-gray-100">
            <p class="text-sm text-gray-700 line-clamp-2">{{ deleteTarget.content }}</p>
          </div>
        </div>
        <DialogFooter class="gap-2">
          <Button variant="outline" @click="showDeleteDialog = false" :disabled="deleting">取消</Button>
          <Button class="bg-red-500 hover:bg-red-600 text-white" @click="confirmDelete" :disabled="deleting">
            <Loader2 v-if="deleting" class="w-4 h-4 mr-2 animate-spin" />
            {{ deleting ? '删除中...' : '确认删除' }}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>
