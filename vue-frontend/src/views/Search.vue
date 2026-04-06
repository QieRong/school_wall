<script setup>
import { ref, reactive, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import request from '@/api/request'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Search, User, FileText, ChevronLeft, Loader2 } from 'lucide-vue-next'
import { Avatar, AvatarImage, AvatarFallback } from '@/components/ui/avatar'

const router = useRouter()
const route = useRoute()

const keyword = ref('')
const activeTab = ref('posts') // posts, users
const loading = ref(false)
const list = ref([])
const pagination = reactive({
  pageNum: 1,
  pageSize: 20,
  total: 0,
  hasMore: true
})

// 页面加载时，检查路由参数并自动搜索
onMounted(() => {
  if (route.query.q) {
    keyword.value = route.query.q
    handleSearch()
  }
})

// 监听路由参数变化（用于页面内切换搜索词）
watch(() => route.query.q, (val, oldVal) => {
  if (val && val !== oldVal) {
    keyword.value = val
    handleSearch()
  }
})

const handleSearch = (isLoadMore = false) => {
  if (!keyword.value.trim()) return

  if (!isLoadMore) {
    loading.value = true
    list.value = []
    pagination.pageNum = 1
  } else {
    pagination.pageNum++
  }

  const api = activeTab.value === 'posts' ? '/post/list' : '/user/search'
  // Post API params
  const params = activeTab.value === 'posts' ? {
    keyword: keyword.value,
    pageNum: pagination.pageNum,
    pageSize: pagination.pageSize
  } : {
    keyword: keyword.value
  }

  request.get(api, { params })
    .then(res => {
      if (res.code === '200') {
        const data = res.data
        const newItems = Array.isArray(data) ? data : (data.list || [])
        
        if (isLoadMore) {
          list.value = [...list.value, ...newItems]
        } else {
          list.value = newItems
        }

        if (activeTab.value === 'posts') {
          pagination.total = data.total
          pagination.hasMore = list.value.length < data.total
        } else {
          // 用户搜索没有分页信息
          pagination.hasMore = false
        }
      }
    })
    .finally(() => {
      loading.value = false
    })
}

// HTML 转义防止 XSS
const escapeHtml = (text) => {
  return text
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}

// 关键词高亮
const highlight = (text) => {
  if (!text) return ''
  let safeText = escapeHtml(text)
  if (!keyword.value) return safeText
  // 转义关键词以匹配转义后的文本（简单处理，实际应匹配原始内容位置）
  // 这里简化处理：假设关键词不包含特殊字符，或者只匹配不影响HTML结构的词
  const reg = new RegExp(`(${keyword.value})`, 'gi') 
  return safeText.replace(reg, '<span class="text-[rgb(33,111,85)] font-bold bg-green-50 px-0.5 rounded">$1</span>')
}

const switchTab = (tab) => {
  activeTab.value = tab
  if (keyword.value) {
    handleSearch()
  }
}
</script>

<template>
  <div class="min-h-screen bg-[#f0f2f5]">
    <!-- 顶部搜索栏 -->
    <div class="bg-white sticky top-0 z-40 border-b border-gray-100 shadow-sm">
      <div class="container mx-auto px-4 max-w-2xl h-16 flex items-center gap-3">
        <Button variant="ghost" size="icon" class="rounded-full hover:bg-gray-100" @click="router.back()">
          <ChevronLeft class="w-6 h-6 text-gray-600" />
        </Button>
        <div class="flex-1 relative">
          <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <Input v-model="keyword" placeholder="搜索感兴趣的内容或用户..." 
            class="pl-9 h-10 bg-gray-50 border-transparent focus:bg-white focus:border-[rgb(33,111,85)] rounded-full transition-all"
            @keyup.enter="handleSearch()" autofocus />
        </div>
        <Button class="bg-[rgb(33,111,85)] hover:bg-[rgb(28,95,72)] text-white rounded-full px-6" @click="handleSearch()">
          搜索
        </Button>
      </div>
      
      <!-- Tabs -->
      <div class="container mx-auto px-4 max-w-2xl flex">
        <div class="flex-1 text-center py-3 cursor-pointer text-sm font-bold border-b-2 transition-colors relative"
          :class="activeTab === 'posts' ? 'text-[rgb(33,111,85)] border-[rgb(33,111,85)]' : 'text-gray-500 border-transparent hover:text-gray-700'"
          @click="switchTab('posts')">
          <div class="flex items-center justify-center gap-1">
            <FileText class="w-4 h-4" /> 帖子
          </div>
        </div>
        <div class="flex-1 text-center py-3 cursor-pointer text-sm font-bold border-b-2 transition-colors relative"
          :class="activeTab === 'users' ? 'text-[rgb(33,111,85)] border-[rgb(33,111,85)]' : 'text-gray-500 border-transparent hover:text-gray-700'"
          @click="switchTab('users')">
          <div class="flex items-center justify-center gap-1">
            <User class="w-4 h-4" /> 用户
          </div>
        </div>
      </div>
    </div>

    <!-- 内容区域 -->
    <div class="container mx-auto px-4 max-w-2xl py-6">
      <!-- 加载中 -->
      <div v-if="loading && list.length === 0" class="text-center py-10">
        <Loader2 class="w-8 h-8 text-[rgb(33,111,85)] animate-spin mx-auto" />
        <p class="text-xs text-gray-400 mt-2">搜索中...</p>
      </div>

      <!-- 空状态 -->
      <div v-else-if="list.length === 0 && !loading && keyword" class="text-center py-20 text-gray-400">
        <div class="w-20 h-20 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
          <Search class="w-8 h-8 text-gray-300" />
        </div>
        <p>没有找到相关结果</p>
      </div>

      <!-- 列表 -->
      <div v-else class="space-y-4">
        <!-- 帖子列表 -->
        <template v-if="activeTab === 'posts'">
          <div v-for="post in list" :key="post.id" 
            class="bg-white p-4 rounded-xl shadow-sm border border-gray-100 cursor-pointer hover:shadow-md transition-all"
            @click="router.push(`/post/${post.id}`)">
            <p class="text-gray-800 text-sm mb-2 line-clamp-3 leading-relaxed" v-html="highlight(post.content)"></p>
            <div class="flex gap-2 mb-2" v-if="post.imgList && post.imgList.length">
               <img v-for="(img, i) in post.imgList.slice(0, 3)" :key="i" :src="img" class="w-20 h-20 object-cover rounded-lg bg-gray-100" />
            </div>
            <div class="flex justify-between items-center text-xs text-gray-400">
              <span>{{ post.nickname }}</span>
              <span>{{ new Date(post.createTime).toLocaleDateString() }}</span>
            </div>
          </div>
          
          <div v-if="pagination.hasMore" class="text-center pt-2">
            <Button variant="ghost" size="sm" @click="handleSearch(true)" :disabled="loading">
              {{ loading ? '加载中...' : '加载更多' }}
            </Button>
          </div>
        </template>

        <!-- 用户列表 -->
        <template v-if="activeTab === 'users'">
          <div v-for="user in list" :key="user.id" 
            class="bg-white p-4 rounded-xl shadow-sm border border-gray-100 flex items-center gap-4 cursor-pointer hover:shadow-md transition-all"
            @click="router.push(`/user/${user.id}`)">
            <Avatar class="w-12 h-12 border border-gray-100">
              <AvatarImage :src="user.avatar" />
              <AvatarFallback>{{ user.nickname?.[0] }}</AvatarFallback>
            </Avatar>
            <div class="flex-1">
              <div class="font-bold text-gray-800" v-html="highlight(user.nickname)"></div>
              <div class="text-xs text-gray-500 mt-0.5">ID: <span v-html="highlight(user.account)"></span></div>
              <div class="text-xs text-gray-400 mt-1 line-clamp-1">{{ user.bio || '这个人很懒，什么都没写' }}</div>
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>
