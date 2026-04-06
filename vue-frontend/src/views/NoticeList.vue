<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useWsStore } from '@/stores/ws'
import request from '@/api/request'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { 
  Bell, MessageCircle, Heart, Info, ChevronLeft, 
  CheckCheck, Trash2, Clock 
} from 'lucide-vue-next'
import { useAppStore } from '@/stores/app'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const wsStore = useWsStore()
const appStore = useAppStore()

const loading = ref(false)
const activeTab = ref(0)
const list = ref([])
const globalAnnouncements = ref([]) // 全局公告列表
const currentAnn = ref(null)        // 当前查看的公告详情
const showAnnDialog = ref(false)    // 公告详情弹窗
const pagination = reactive({
  pageNum: 1,
  pageSize: 20,
  total: 0,
  hasMore: true
})

const tabs = [
  { id: 0, label: '全部消息', icon: Bell, color: 'text-gray-600', bg: 'bg-gray-100' },
  // type=1 包含漂流瓶回复、故事链、封禁等所有系统消息，改名为"系统通知"更准确
  { id: 1, label: '系统通知', icon: Info, color: 'text-blue-600', bg: 'bg-blue-100' },
  { id: 2, label: '收到评论', icon: MessageCircle, color: 'text-green-600', bg: 'bg-green-100' },
  { id: 3, label: '收到点赞', icon: Heart, color: 'text-pink-600', bg: 'bg-pink-100' }
]

const fetchList = async (isLoadMore = false) => {
  if (!userStore.user?.id) return
  
  if (!isLoadMore) {
    loading.value = true
    list.value = []
    pagination.pageNum = 1
  } else {
    pagination.pageNum++
  }

  try {
    const res = await request.get('/notice/list', {
      params: {
        userId: userStore.user.id,
        type: activeTab.value,
        pageNum: pagination.pageNum,
        pageSize: pagination.pageSize
      }
    })
    
    if (res.code === '200') {
      // 过滤掉包含“成就解锁”字眼的系统通知（1类型系统通知）
      const newItems = (res.data.list || []).filter(item => !(item.type === 1 && item.content?.includes('成就')))
      if (isLoadMore) {
        list.value = [...list.value, ...newItems]
      } else {
        list.value = newItems
      }
      pagination.total = res.data.total
      pagination.hasMore = list.value.length < res.data.total
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

// 拉取全局公告
const fetchAnnouncements = async () => {
  try {
    const res = await request.get('/index/announcements')
    if (res.code === '200') globalAnnouncements.value = res.data || []
  } catch (e) {}
}

const handleTabChange = (id) => {
  activeTab.value = id
  if (id === 1) fetchAnnouncements()
  fetchList()
}

// 根据通知类型和内容智能返回语义化标签配置
const getNoticeTag = (item) => {
  if (item.type === 2) return { text: '[评论]', cls: 'text-blue-600' }
  if (item.type === 3) return { text: '[点赞]', cls: 'text-pink-600' }
  if (item.type === 4) return { text: '[@提及]', cls: 'text-purple-600' }
  if (item.type === 1) {
    const c = item.content || ''
    if (c.includes('漂流瓶')) return { text: '[漂流瓶]', cls: 'text-cyan-600' }
    if (c.includes('故事') || c.includes('Story')) return { text: '[故事]', cls: 'text-amber-600' }
    // if (c.includes('成就')) return { text: '[成就]', cls: 'text-yellow-600' }
    if (c.includes('封禁') || c.includes('封号')) return { text: '[封禁]', cls: 'text-red-600' }
    if (c.includes('删帖') || c.includes('封禁')) return { text: '[系统]', cls: 'text-red-600' }
    if (c.includes('公告')) return { text: '[公告]', cls: 'text-blue-600' }
    return { text: '[系统]', cls: 'text-gray-500' }
  }
  return null
}

// 标记单个已读，同时同步渠少铃铛未读计数
const markAsRead = async (notice) => {
  if (notice.isRead === 1) return
  try {
    await request.post('/notice/read', null, { params: { id: notice.id } })
    notice.isRead = 1
    // 同步更新铃铛红点计数
    if (wsStore.unreadCount > 0) wsStore.unreadCount--
  } catch (e) {}
}

// 点击通知
const handleClick = (notice) => {
  markAsRead(notice)

  const t = notice.type
  const rid = notice.relatedId
  const c = notice.content || ''

  // type=2: 收到评论 → 帖子详情
  if (t === 2 && rid) { router.push(`/post/${rid}`); return }
  // type=3: 收到点赞 → 帖子详情
  if (t === 3 && rid) { router.push(`/post/${rid}`); return }
  // type=4: @我 → 帖子详情
  if (t === 4 && rid) { router.push(`/post/${rid}`); return }
  // type=1: 系统通知（细分）
  if (t === 1) {
    if (c.includes('漂流瓶')) {
      // 漂流瓶回复通知 → 带上bottleId跳转，自动弹出详情弹窗
      router.push(rid ? `/bottle?bottleId=${rid}` : '/bottle')
    } else if (c.includes('故事') || c.includes('Story')) {
      router.push('/story')
    } else if (rid) {
      // 举报确认等带帖子ID的系统通知
      router.push(`/post/${rid}`)
    }
    // 无relatedId的删帖/封禁通知 → 不跳转，仅展示
  }
}

// 一键已读
const readAll = async () => {
  try {
    await request.post('/notice/readAll', null, { params: { userId: userStore.user.id } })
    list.value.forEach(item => item.isRead = 1)
    // 同步清零铃铛未读数
    wsStore.unreadCount = 0
    appStore.showToast('全部标记已读', 'success')
  } catch (e) {
    appStore.showToast('操作失败', 'error')
  }
}

// 清空已读
const clearRead = async () => {
  try {
    await request.post('/notice/clear', null, { params: { userId: userStore.user.id } })
    list.value = list.value.filter(n => n.isRead === 0)
    appStore.showToast('已清空已读消息', 'success')
  } catch (e) {
    appStore.showToast('操作失败', 'error')
  }
}

onMounted(() => {
  if (userStore.user) {
    // 支持 ?tab=1 参数直接定位到系统公告
    const tabParam = parseInt(route.query.tab)
    if (!isNaN(tabParam) && tabParam >= 0 && tabParam <= 3) {
      activeTab.value = tabParam
    }
    if (activeTab.value === 1) fetchAnnouncements()
    fetchList()
  } else {
    appStore.showToast('请先登录', 'error')
    router.replace('/login')
  }
})
</script>

<template>
  <div class="min-h-screen bg-[#f0f2f5] pb-20">
    <!-- 顶部导航 -->
    <div class="bg-white/80 backdrop-blur-md sticky top-0 z-40 border-b border-gray-100">
      <div class="container mx-auto px-4 max-w-4xl h-14 flex items-center justify-between">
        <div class="flex items-center gap-3">
          <Button variant="ghost" size="icon" class="rounded-full hover:bg-gray-100" @click="router.back()">
            <ChevronLeft class="w-5 h-5 text-gray-600" />
          </Button>
          <span class="font-bold text-gray-800">通知中心</span>
        </div>
        <div class="flex items-center gap-2">
          <Button variant="ghost" size="sm" class="text-xs text-gray-500 hover:text-[rgb(33,111,85)]" @click="readAll">
            <CheckCheck class="w-3.5 h-3.5 mr-1" /> 一键已读
          </Button>
          <Button variant="ghost" size="sm" class="text-xs text-gray-500 hover:text-red-600" @click="clearRead">
            <Trash2 class="w-3.5 h-3.5 mr-1" /> 清空已读
          </Button>
        </div>
      </div>
    </div>

    <div class="container mx-auto px-4 max-w-4xl py-6 flex flex-col md:flex-row gap-6">
      
      <!-- 左侧分类导航 -->
      <div class="w-full md:w-64 flex-shrink-0 space-y-2">
        <div v-for="tab in tabs" :key="tab.id"
          class="flex items-center gap-3 px-4 py-3 rounded-xl cursor-pointer transition-all duration-200 font-medium text-sm"
          :class="activeTab === tab.id ? 'bg-white shadow-sm ring-1 ring-black/5 text-gray-900' : 'text-gray-500 hover:bg-white/60 hover:text-gray-700'"
          @click="handleTabChange(tab.id)">
          <div class="w-8 h-8 rounded-lg flex items-center justify-center transition-colors"
            :class="activeTab === tab.id ? tab.bg + ' ' + tab.color : 'bg-gray-100 text-gray-400'">
            <component :is="tab.icon" class="w-4 h-4" />
          </div>
          {{ tab.label }}
        </div>
      </div>

      <!-- 公告详情弹窗 -->
      <Dialog :open="showAnnDialog" @update:open="showAnnDialog = $event">
        <DialogContent class="sm:max-w-[500px] bg-white rounded-xl">
          <DialogHeader>
            <DialogTitle class="text-lg font-bold text-[rgb(33,111,85)] flex items-center gap-2">
              <Bell class="w-5 h-5" /> {{ currentAnn?.title }}
            </DialogTitle>
          </DialogHeader>
          <div class="py-4 text-gray-700 leading-relaxed whitespace-pre-wrap max-h-[60vh] overflow-y-auto">{{ currentAnn?.content }}</div>
          <div class="text-right text-xs text-gray-400">发布于：{{ currentAnn?.createTime ? new Date(currentAnn.createTime).toLocaleString() : '近期' }}</div>
        </DialogContent>
      </Dialog>

      <!-- 右侧列表 -->
      <div class="flex-1 space-y-4">

        <!-- 系统公告 Tab：展示全局校园公告 -->
        <template v-if="activeTab === 1 && globalAnnouncements.length > 0">
          <div class="bg-gradient-to-r from-[rgb(33,111,85)] to-emerald-700 text-white rounded-2xl p-4 space-y-3">
            <div class="flex items-center gap-2 font-bold text-sm mb-1">
              <Bell class="w-4 h-4" /> 校园公告（共 {{ globalAnnouncements.length }} 条）
            </div>
            <div v-for="ann in globalAnnouncements" :key="'ga-' + ann.id"
              class="bg-white/10 rounded-xl p-3 cursor-pointer hover:bg-white/20 transition-colors"
              @click="currentAnn = ann; showAnnDialog = true">
              <div class="flex items-start gap-2">
                <span v-if="ann.isTop" class="text-[10px] bg-white text-[rgb(33,111,85)] px-1.5 py-0.5 rounded font-bold whitespace-nowrap mt-0.5">置顶</span>
                <div class="flex-1 min-w-0">
                  <p class="font-medium text-sm line-clamp-1">{{ ann.title }}</p>
                  <p class="text-xs text-white/70 line-clamp-2 mt-0.5 leading-relaxed">{{ ann.content }}</p>
                </div>
              </div>
              <div class="text-right text-[10px] text-white/50 mt-1">{{ new Date(ann.createTime).toLocaleDateString() }}</div>
            </div>
          </div>
        </template>

        <!-- 加载中 -->
        <div v-if="loading && list.length === 0" class="space-y-4">
          <div v-for="i in 5" :key="i" class="bg-white p-4 rounded-2xl animate-pulse flex gap-4">
            <div class="w-10 h-10 bg-gray-100 rounded-full flex-shrink-0"></div>
            <div class="flex-1 space-y-2">
              <div class="h-4 bg-gray-100 rounded w-1/3"></div>
              <div class="h-3 bg-gray-100 rounded w-3/4"></div>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-else-if="list.length === 0 && !(activeTab === 1 && globalAnnouncements.length > 0)" 
          class="flex flex-col items-center justify-center py-20 text-gray-400">
          <div class="w-20 h-20 bg-gray-100 rounded-full flex items-center justify-center mb-4">
             <Bell class="w-8 h-8 text-gray-300" />
          </div>
          <p class="text-sm">暂无消息通知</p>
        </div>

        <!-- 列表内容 -->
        <div v-else class="space-y-3">
          <div v-for="item in list" :key="item.id"
            class="group bg-white rounded-2xl p-4 border border-gray-100 shadow-sm hover:shadow-md transition-all cursor-pointer relative overflow-hidden"
            @click="handleClick(item)">
            
            <!-- 未读红点 -->
            <div v-if="item.isRead === 0" class="absolute top-4 right-4 w-2 h-2 bg-red-500 rounded-full"></div>

            <div class="flex gap-4">
              <img :src="item.senderAvatar || '/default.png'" 
                class="w-10 h-10 rounded-full border border-gray-100 flex-shrink-0 object-cover" />
              
              <div class="flex-1 min-w-0">
                <div class="flex justify-between items-start mb-1">
                  <span class="font-bold text-gray-800 text-sm">{{ item.senderName || '系统通知' }}</span>
                  <span class="text-xs text-gray-400 flex items-center gap-1">
                    <Clock class="w-3 h-3" />
                    {{ new Date(item.createTime).toLocaleString() }}
                  </span>
                </div>
                
                <p class="text-sm text-gray-600 leading-relaxed whitespace-pre-wrap">
                  <!-- 根据通知类型和内容智能展示语义化标签 -->
                  <span v-if="getNoticeTag(item)" :class="getNoticeTag(item).cls" class="font-medium">
                    {{ getNoticeTag(item).text }} 
                  </span>
                  {{ item.content }}
                </p>

                <!-- 图片附件预览 (如果有) -->
                <!-- 预留位置 -->
              </div>
            </div>
          </div>

          <!-- 加载更多 -->
          <div v-if="pagination.hasMore" class="text-center pt-4">
             <Button variant="outline" size="sm" @click="fetchList(true)" :disabled="loading" class="rounded-full px-6">
               {{ loading ? '加载中...' : '加载更多' }}
             </Button>
          </div>
          <div v-else class="text-center text-xs text-gray-300 py-4">
            - 到底了 -
          </div>
        </div>

      </div>
    </div>
  </div>
</template>
