<script setup>
import { ref, onMounted, watch } from 'vue'
import {
  DropdownMenu, DropdownMenuTrigger, DropdownMenuContent,
  DropdownMenuItem, DropdownMenuSeparator
} from '@/components/ui/dropdown-menu'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Bell, Trash2, CheckCheck, RefreshCw, MessageCircle, Volume2, VolumeX } from 'lucide-vue-next'
import { useWsStore } from '@/stores/ws'
import { useRouter } from 'vue-router'
import request from '@/api/request'

const props = defineProps({ 
  user: {
    type: Object,
    default: () => ({})
  }
})
const wsStore = useWsStore()
const router = useRouter()

const notices = ref([])
const announcements = ref([]) // 未读公告列表（过滤掉已读的）
const unseenAnnCount = ref(0)   // 未读公告数量（用于叠加到铃铛红点）
const loading = ref(false)
const isOpen = ref(false)
const showAllNotices = ref(false)
const allNotices = ref([])
const allNoticesLoading = ref(false)
const allNoticesPagination = ref({ pageNum: 1, pageSize: 20, total: 0, hasMore: true })

// ===== 公告已读状态管理（localStorage 按用户ID隔离） =====

// 获取当前用户的已读公告 ID 集合
const getSeenAnnIds = () => {
  try {
    const key = `seenAnnIds_${props.user?.id}`
    return new Set(JSON.parse(localStorage.getItem(key) || '[]'))
  } catch {
    return new Set()
  }
}

// 将指定公告 ID 标记为已读并持久化
const markAnnSeen = (annId) => {
  try {
    const key = `seenAnnIds_${props.user?.id}`
    const ids = getSeenAnnIds()
    ids.add(annId)
    localStorage.setItem(key, JSON.stringify([...ids]))
  } catch { }
}

// 通知音效
const hasUserInteracted = ref(false)
const soundEnabled = ref(true) // 提示音开关，默认开启

// 从 localStorage 读取用户设置
onMounted(() => {
  const savedSetting = localStorage.getItem('notificationSoundEnabled')
  if (savedSetting !== null) {
    soundEnabled.value = savedSetting === 'true'
  }

  fetchUnread()
  // 监听用户首次交互以解锁音频
  document.addEventListener('click', enableAudio, { once: true })
  document.addEventListener('keydown', enableAudio, { once: true })
})

// 切换提示音开关
const toggleSound = () => {
  soundEnabled.value = !soundEnabled.value
  localStorage.setItem('notificationSoundEnabled', soundEnabled.value.toString())

  if (soundEnabled.value) {
    // 开启时播放一次提示音作为反馈
    wsStore.playNotificationSound()
  }
}

//监听用户首次交互，解锁音频播放
const enableAudio = () => {
  hasUserInteracted.value = true
  // 解锁音频上下文
  wsStore.initAudio()
  // 移除监听器
  document.removeEventListener('click', enableAudio)
  document.removeEventListener('keydown', enableAudio)
}


// 移除本地播放逻辑和监听器，逻辑已移至 ws.js
// playNotificationSound 和 watch 均不再需要

// 获取未读数
const fetchUnread = async () => {
  if (!props.user) return
  try {
    const res = await request.get('/notice/unread', { params: { userId: props.user.id } })
    if (res.code === '200') wsStore.unreadCount = res.data
  } catch (e) { }
}

// 获取通知列表，公告只展示未读的（未加入 localStorage 已读集合的）
const fetchNotices = async () => {
  if (!props.user) return
  loading.value = true
  try {
    const [noticeRes, annRes] = await Promise.all([
      request.get('/notice/list', {
        params: { userId: props.user.id, pageNum: 1, pageSize: 8 }
      }),
      request.get('/index/announcements')
    ])
    if (noticeRes.code === '200') notices.value = noticeRes.data.list
    if (annRes.code === '200') {
      const seenIds = getSeenAnnIds()
      const allAnns = annRes.data || []
      // 过滤掉已读公告，只保留未读的，最多显示2条
      const unread = allAnns.filter(a => !seenIds.has(a.id))
      announcements.value = unread.slice(0, 2)
      unseenAnnCount.value = unread.length
    }
  } finally {
    loading.value = false
  }
}

// 监听下拉框打开
watch(isOpen, (val) => {
  if (val) fetchNotices()
})

// 一键已读：个人通知 + 当前展示的所有未读公告
const readAll = async () => {
  await request.post('/notice/readAll', null, { params: { userId: props.user.id } })
  wsStore.unreadCount = 0
  notices.value.forEach(n => n.isRead = 1)
  // 将当前下拉中所有未读公告加入已读集合并清空列表
  announcements.value.forEach(a => markAnnSeen(a.id))
  announcements.value = []
  unseenAnnCount.value = 0
}

// 删除已读
const clearRead = async () => {
  await request.post('/notice/clear', null, { params: { userId: props.user.id } })
  notices.value = notices.value.filter(n => n.isRead === 0)
}

// 点击跳转并标记已读
const handleClick = async (notice) => {
  // 公告类型条目：标记已读后跳转历史公告页
  if (notice._isAnnouncement) {
    // 将该公告 ID 记入 localStorage 已读集合，下次不再展示
    markAnnSeen(notice._annId)
    // 从当前列表移除，立即反映已读效果
    announcements.value = announcements.value.filter(a => a.id !== notice._annId)
    unseenAnnCount.value = Math.max(0, unseenAnnCount.value - 1)
    isOpen.value = false
    router.push('/notices?tab=1')
    return
  }

  // 标记已读
  if (notice.isRead === 0) {
    try {
      await request.post('/notice/read', null, { params: { id: notice.id } })
      notice.isRead = 1
      if (wsStore.unreadCount > 0) wsStore.unreadCount--
    } catch (e) { }
  }

  showAllNotices.value = false
  isOpen.value = false

  const t = notice.type
  const rid = notice.relatedId
  const c = notice.content || ''

  // type=2: 收到评论 → 跳帖子详情
  if (t === 2 && rid) {
    router.push(`/post/${rid}`)
    return
  }
  // type=3: 收到点赞 → 跳帖子详情
  if (t === 3 && rid) {
    router.push(`/post/${rid}`)
    return
  }
  // type=4: @我 → 跳帖子详情
  if (t === 4 && rid) {
    router.push(`/post/${rid}`)
    return
  }
  // type=1: 系统通知（细分）
  if (t === 1) {
    if (c.includes('漂流瓶')) {
      // 漂流瓶回复 → 跳漂流瓶页并带上瓶子ID，使详情弹窗自动弹出
      router.push(rid ? `/bottle?bottleId=${rid}` : '/bottle')
    } else if (c.includes('故事') || c.includes('Story')) {
      // 故事链相关 → 跳故事大厅
      router.push('/story')
    } else if (rid) {
      // 其他带有关联ID的系统通知（如举报确认的帖子ID）→ 跳帖子详情
      router.push(`/post/${rid}`)
    }
    // 无 relatedId 的管理通知（删帖/封禁提示）→ 纯展示，不跳转
  }
}

// 根据通知类型和内容智能返回语义化标签（与 NoticeList.vue 保持一致）
const getNoticeTag = (notice) => {
  if (notice.type === 2) return { text: '[评论]', cls: 'text-blue-500' }
  if (notice.type === 3) return { text: '[点赞]', cls: 'text-pink-500' }
  if (notice.type === 4) return { text: '[@提及]', cls: 'text-purple-500' }
  if (notice.type === 1) {
    const c = notice.content || ''
    if (c.includes('漂流瓶')) return { text: '[漂流瓶]', cls: 'text-cyan-600' }
    if (c.includes('故事') || c.includes('Story')) return { text: '[故事]', cls: 'text-amber-600' }
    if (c.includes('成就')) return { text: '[成就]', cls: 'text-yellow-600' }
    if (c.includes('封禁') || c.includes('封号') || c.includes('删帖')) return { text: '[系统]', cls: 'text-red-500' }
    return { text: '[系统]', cls: 'text-gray-400' }
  }
  return null
}

// 移除 onMounted 中的重复代码，已经在上面定义了
</script>

<template>
  <div class="relative flex items-center gap-2">
    <!-- WebSocket 连接状态指示 -->
    <div v-if="!wsStore.isConnected"
      class="flex items-center gap-2 px-3 py-1.5 bg-red-50 border border-red-200 rounded-full animate-pulse">
      <div class="w-2 h-2 bg-red-500 rounded-full"></div>
      <span class="text-xs text-red-600 font-medium">连接断开</span>
      <button @click="wsStore.manualReconnect()" class="ml-1 text-red-600 hover:text-red-700 transition-colors"
        title="点击重连">
        <RefreshCw class="w-3.5 h-3.5" />
      </button>
    </div>

    <!--私信图标 -->
    <Button variant="ghost" size="icon"
      class="relative rounded-full text-gray-600 hover:text-[rgb(33,111,85)] hover:bg-green-50"
      @click="router.push('/message')" title="私信">
      <MessageCircle class="w-5 h-5 transition-all" :class="{ 'animate-swing': wsStore.chatUnreadCount > 0 }" />
      <span v-if="wsStore.chatUnreadCount > 0"
        class="absolute top-2 right-2 w-2.5 h-2.5 bg-pink-500 border-2 border-white rounded-full animate-pulse">
      </span>
    </Button>

    <!-- 原有的铃铛图标 -->
    <DropdownMenu :open="isOpen" @update:open="isOpen = $event">
      <DropdownMenuTrigger as-child>
        <Button variant="ghost" size="icon"
          class="relative rounded-full text-gray-600 hover:text-[rgb(33,111,85)] hover:bg-green-50" title="系统通知">
          <Bell class="w-5 h-5 transition-all" :class="{ 'animate-swing': (wsStore.unreadCount + unseenAnnCount) > 0 }" />
          <!-- 红点数量 = 个人通知未读数 + 未读公告数 -->
          <span v-if="(wsStore.unreadCount + unseenAnnCount) > 0"
            class="absolute top-2 right-2 w-2.5 h-2.5 bg-red-500 border-2 border-white rounded-full animate-pulse">
          </span>
        </Button>
      </DropdownMenuTrigger>

      <DropdownMenuContent align="end" class="w-80 p-0 rounded-xl shadow-xl border-gray-100 bg-white backdrop-blur-md">
        <div class="p-3 flex justify-between items-center border-b border-gray-50">
          <span class="font-bold text-sm text-gray-700">通知中心</span>
          <div class="flex gap-1">
            <Button variant="ghost" size="icon" class="h-6 w-6" :title="soundEnabled ? '关闭提示音' : '开启提示音'"
              @click.stop="toggleSound">
              <Volume2 v-if="soundEnabled" class="w-3.5 h-3.5 text-green-500" />
              <VolumeX v-else class="w-3.5 h-3.5 text-gray-400" />
            </Button>
            <Button variant="ghost" size="icon" class="h-6 w-6" title="一键已读" @click.stop="readAll">
              <CheckCheck class="w-3.5 h-3.5 text-blue-500" />
            </Button>
            <Button variant="ghost" size="icon" class="h-6 w-6" title="清空已读" @click.stop="clearRead">
              <Trash2 class="w-3.5 h-3.5 text-gray-400 hover:text-red-500" />
            </Button>
          </div>
        </div>

        <div class="max-h-[300px] overflow-y-auto no-scrollbar py-1">
          <div v-if="loading" class="text-center py-4 text-xs text-gray-400">加载中...</div>
          <div v-else-if="notices.length === 0 && announcements.length === 0" class="text-center py-8 text-gray-400">
            <div class="text-2xl mb-1">🔕</div>
            <div class="text-xs">暂无新通知</div>
          </div>

          <!-- 未读公告条目（只展示未读的，点击后消失） -->
          <DropdownMenuItem v-for="ann in announcements" :key="'ann-' + ann.id"
            class="cursor-pointer p-3 focus:bg-blue-50 border-b border-blue-50 items-start gap-3 bg-blue-50/60"
            @click="handleClick({ _isAnnouncement: true, _annId: ann.id })">
            <div class="w-8 h-8 rounded-full bg-[rgb(33,111,85)] flex items-center justify-center flex-shrink-0">
              <Bell class="w-4 h-4 text-white" />
            </div>
            <div class="flex-1 overflow-hidden">
              <div class="flex justify-between items-start">
                <span class="text-sm font-bold text-[rgb(33,111,85)] truncate">
                  <span v-if="ann.isTop" class="text-[10px] bg-[rgb(33,111,85)] text-white px-1 rounded mr-1">置顶</span>校园公告
                </span>
                <span class="text-[10px] text-gray-400 whitespace-nowrap ml-2">
                  {{ new Date(ann.createTime).toLocaleDateString() }}
                </span>
              </div>
              <p class="text-xs text-gray-700 mt-0.5 font-medium line-clamp-1">{{ ann.title }}</p>
              <p class="text-xs text-gray-500 mt-0.5 line-clamp-1">{{ ann.content }}</p>
            </div>
            <!-- 未读红点（公告始终是未读态，因为过滤后只显示未读的） -->
            <div class="w-2 h-2 bg-red-500 rounded-full mt-2 flex-shrink-0"></div>
          </DropdownMenuItem>

          <DropdownMenuItem v-for="notice in notices" :key="notice.id"
            class="cursor-pointer p-3 focus:bg-gray-50 border-b border-gray-50 last:border-0 items-start gap-3"
            @click="handleClick(notice)">

            <img :src="notice.senderAvatar || '/default.png'"
              class="w-8 h-8 rounded-full border border-gray-100 flex-shrink-0" />

            <div class="flex-1 overflow-hidden">
              <div class="flex justify-between items-start">
                <span class="text-sm font-bold text-gray-800 truncate">{{ notice.senderName || '系统通知' }}</span>
                <span class="text-[10px] text-gray-400 whitespace-nowrap ml-2">{{ new
                  Date(notice.createTime).toLocaleDateString() }}</span>
              </div>

              <p class="text-xs text-gray-600 mt-0.5 line-clamp-2">
                <!-- 语义化标签：根据通知类型和内容动态展示 -->
                <span v-if="getNoticeTag(notice)" :class="getNoticeTag(notice).cls">
                  {{ getNoticeTag(notice).text }} 
                </span>
                {{ notice.content }}
              </p>
            </div>

            <div v-if="notice.isRead === 0" class="w-2 h-2 bg-red-500 rounded-full mt-2 flex-shrink-0"></div>
          </DropdownMenuItem>
        </div>

        <DropdownMenuSeparator />
        <div class="p-2 text-center">
          <span class="text-xs text-[rgb(33,111,85)] cursor-pointer hover:underline"
            @click="openAllNotices">查看全部通知</span>
        </div>
      </DropdownMenuContent>
    </DropdownMenu>

    <!-- 问题15：全部通知弹窗 -->
    <Dialog :open="showAllNotices" @update:open="showAllNotices = $event">
      <DialogContent class="max-w-lg max-h-[80vh] overflow-hidden flex flex-col">
        <DialogHeader>
          <DialogTitle class="flex items-center gap-2">
            <Bell class="w-5 h-5 text-[rgb(33,111,85)]" /> 全部通知
            <span class="text-sm font-normal text-gray-400">共 {{ allNoticesPagination.total }} 条</span>
          </DialogTitle>
        </DialogHeader>
        <div class="flex-1 overflow-y-auto space-y-2 pr-2">
          <div v-if="allNoticesLoading && allNotices.length === 0" class="text-center text-gray-400 py-8">
            加载中...
          </div>
          <div v-else-if="allNotices.length === 0" class="text-center text-gray-400 py-8">
            🔕 暂无通知
          </div>
          <div v-else v-for="notice in allNotices" :key="notice.id"
            class="group p-3 rounded-xl border border-gray-100 hover:border-[rgb(33,111,85)]/30 hover:bg-emerald-50/30 transition-all cursor-pointer flex items-start gap-3"
            @click="handleClick(notice); showAllNotices = false">
            <img :src="notice.senderAvatar || '/default.png'" class="w-10 h-10 rounded-full border border-gray-100 flex-shrink-0" />
            <div class="flex-1 min-w-0">
              <div class="flex items-center justify-between">
                <span class="font-medium text-gray-800">{{ notice.senderName || '系统通知' }}</span>
                <span class="text-xs text-gray-400">{{ new Date(notice.createTime).toLocaleString('zh-CN') }}</span>
              </div>
              <p class="text-sm text-gray-600 mt-1 line-clamp-2">
                <span v-if="notice.type === 2" class="text-blue-500">[评论] </span>
                <span v-if="notice.type === 3" class="text-pink-500">[点赞] </span>
                {{ notice.content }}
              </p>
            </div>
            <div v-if="notice.isRead === 0" class="w-2 h-2 bg-red-500 rounded-full flex-shrink-0 mt-2"></div>
          </div>
          <!-- 加载更多 -->
          <div v-if="allNoticesPagination.hasMore" class="text-center py-3">
            <Button variant="ghost" size="sm" @click="loadMoreNotices" :disabled="allNoticesLoading">
              {{ allNoticesLoading ? '加载中...' : '加载更多' }}
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  </div>
</template>

<style scoped>
@keyframes swing {

  0%,
  100% {
    transform: rotate(0deg);
  }

  20% {
    transform: rotate(15deg);
  }

  40% {
    transform: rotate(-10deg);
  }

  60% {
    transform: rotate(5deg);
  }

  80% {
    transform: rotate(-5deg);
  }
}

.animate-swing {
  animation: swing 1s ease-in-out infinite;
  transform-origin: top center;
}
</style>