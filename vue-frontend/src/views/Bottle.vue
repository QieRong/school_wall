<script setup>
import { ref, onMounted, computed, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Avatar, AvatarImage, AvatarFallback } from '@/components/ui/avatar'
import { Badge } from '@/components/ui/badge'
import { Textarea } from '@/components/ui/textarea'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { Anchor, Send, Archive, MessageCircle, ArrowLeft, Trophy, Waves, Heart, RotateCcw, Star, X, User, Clock, AlertTriangle, Loader2 } from 'lucide-vue-next'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import request from '@/api/request'
import ListEndIndicator from '@/components/ListEndIndicator.vue'
import {
  throwBottle, fishBottle, replyBottle, releaseBottle, collectBottle,
  getMySentBottles, getMyCollectedBottles, getBottleDetail, deleteBottle, getAchievements
} from '@/api/bottle'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const appStore = useAppStore()
const currentUser = computed(() => userStore.user)

// 状态
const activeTab = ref('ocean') // ocean | my | collection | achievement
const loading = ref(false)
const fishLoading = ref(false)
const cooldown = ref(0)
const cooldownEndTime = ref(null)
let cooldownTimer = null

// 冷却持久化键名
const COOLDOWN_KEY = 'bottle_cooldown_end_time'

// 投放弹窗
const showThrowDialog = ref(false)
const throwForm = ref({ content: '', direction: 1, isAnonymous: false }) // 默认值改为1（樱花海岸）
const directions = [
  { value: 1, label: '樱花海岸', desc: '文艺抒情' },
  { value: 2, label: '深海秘境', desc: '心事秘密' },
  { value: 3, label: '星辰大海', desc: '梦想寄语' }
]

// 打捞结果
const showFishResult = ref(false)
const fishedBottle = ref(null)

// 回复弹窗
const showReplyDialog = ref(false)
const replyContent = ref('')

// 我的瓶子列表
const myBottles = ref([])
const myCollections = ref([])
const achievements = ref([])
const pagination = ref({ pageNum: 1, pageSize: 10, total: 0 })

// 瓶子详情
const showDetailDialog = ref(false)
const detailBottle = ref(null)

// 删除确认
const showDeleteDialog = ref(false)
const deleteTarget = ref(null)
const deleteLoading = ref(false)

// 检查登录
const checkAuth = (action = '操作') => {
  if (!currentUser.value) {
    appStore.showToast(`请先登录后再${action}`, 'error')
    return false
  }
  return true
}

//投放成功动画状态
const showThrowAnimation = ref(false)

// 投放漂流瓶
const handleThrow = async () => {
  if (!checkAuth('投放漂流瓶')) return
  if (!throwForm.value.content.trim()) {
    return appStore.showToast('请写下你想说的话', 'error')
  }
  if (throwForm.value.content.length > 200) {
    return appStore.showToast('内容不能超过200字', 'error')
  }
  loading.value = true
  try {
    await throwBottle({
      userId: currentUser.value.id,
      content: throwForm.value.content,
      direction: throwForm.value.direction,
      isAnonymous: throwForm.value.isAnonymous ? 1 : 0
    })

    //显示投放动画
    showThrowAnimation.value = true
    setTimeout(() => {
      showThrowAnimation.value = false
      appStore.showToast('🌊 漂流瓶已投入大海，等待有缘人发现', 'success')
    }, 1500)

    showThrowDialog.value = false
    throwForm.value = { content: '', direction: 1, isAnonymous: false }

    // 刷新我的瓶子列表（如果当前在"我的瓶子"标签）
    if (activeTab.value === 'my') {
      fetchMyBottles()
    }

    // 刷新成就（可能获得"海洋之心"成就）
    fetchAchievements()
  } catch (e) {
    appStore.showToast(e.message || '投放失败', 'error')
  } finally {
    loading.value = false
  }
}

// 打捞漂流瓶
const handleFish = async () => {
  if (!checkAuth('打捞漂流瓶')) return

  //先检查本地冷却
  if (cooldown.value > 0) {
    return appStore.showToast(`请等待 ${cooldown.value} 秒后再打捞`, 'error')
  }

  fishLoading.value = true
  try {
    //打捞前先同步后端冷却状态
    await syncCooldownWithBackend()

    //再次检查冷却（防止多标签页同时操作）
    if (cooldown.value > 0) {
      return appStore.showToast(`操作太频繁，请等待 ${cooldown.value} 秒`, 'warning')
    }

    const res = await fishBottle(currentUser.value.id)
    if (res.code === '200' && res.data) {
      if (res.data.success) {
        const bottle = res.data.bottle
        fishedBottle.value = bottle
        showFishResult.value = true
        // 使用后端返回的冷却时间
        const cooldownSeconds = res.data.cooldownSeconds || 60
        startCooldown(cooldownSeconds)
      } else {
        // 如果 success 为 false，说明此时没捞到，或者在冷却中
        appStore.showToast(res.data.message || '大海空空如也，稍后再试吧', 'info')
        if (res.data.cooldownSeconds) {
          startCooldown(res.data.cooldownSeconds)
        }
      }
    } else {
      appStore.showToast(res.msg || '大海空空如也，稍后再试吧', 'info')
    }
  } catch (e) {
    appStore.showToast(e.message || '打捞失败', 'error')
  } finally {
    fishLoading.value = false
  }
}

// 冷却计时
const startCooldown = (seconds = 60) => {
  // 计算冷却结束时间
  cooldownEndTime.value = Date.now() + seconds * 1000

  // 持久化到 localStorage
  localStorage.setItem(COOLDOWN_KEY, cooldownEndTime.value.toString())

  // 更新倒计时显示
  updateCooldown()

  // 启动定时器
  if (cooldownTimer) clearInterval(cooldownTimer)
  cooldownTimer = setInterval(() => {
    updateCooldown()
  }, 1000)
}

// 更新冷却倒计时
const updateCooldown = () => {
  if (!cooldownEndTime.value) {
    cooldown.value = 0
    if (cooldownTimer) {
      clearInterval(cooldownTimer)
      cooldownTimer = null
    }
    return
  }

  const remaining = Math.ceil((cooldownEndTime.value - Date.now()) / 1000)

  if (remaining <= 0) {
    // 冷却结束
    cooldown.value = 0
    cooldownEndTime.value = null
    localStorage.removeItem(COOLDOWN_KEY)
    if (cooldownTimer) {
      clearInterval(cooldownTimer)
      cooldownTimer = null
    }
  } else {
    cooldown.value = remaining
  }
}

// 恢复冷却状态（从 localStorage）
const restoreCooldown = () => {
  const savedEndTime = localStorage.getItem(COOLDOWN_KEY)
  if (savedEndTime) {
    cooldownEndTime.value = parseInt(savedEndTime)
    updateCooldown()

    // 如果还在冷却中，启动定时器
    if (cooldown.value > 0) {
      if (cooldownTimer) clearInterval(cooldownTimer)
      cooldownTimer = setInterval(() => {
        updateCooldown()
      }, 1000)
    }
  }
}

// 同步后端冷却状态
const syncCooldownWithBackend = async () => {
  if (!currentUser.value) return

  try {
    const res = await request.get('/bottle/cooldown-status', {
      params: { userId: currentUser.value.id }
    })
    if (res.code === '200' && res.data && res.data.remainingSeconds > 0) {
      startCooldown(res.data.remainingSeconds)
    } else {
      // 清除本地过期的冷却状态
      localStorage.removeItem(COOLDOWN_KEY)
      cooldown.value = 0
      cooldownEndTime.value = null
    }
  } catch (e) {
    console.error('同步冷却状态失败，使用本地状态', e)
    // 降级到本地状态
    restoreCooldown()
  }
}

// 回复漂流瓶
const handleReply = async () => {
  if (!replyContent.value.trim()) {
    return appStore.showToast('请输入回复内容', 'error')
  }
  loading.value = true
  try {
    await replyBottle({
      bottleId: fishedBottle.value.id,
      userId: currentUser.value.id,
      content: replyContent.value
    })
    appStore.showToast('💌 回复成功，瓶子已放回大海', 'success')
    showReplyDialog.value = false
    showFishResult.value = false
    replyContent.value = ''
  } catch (e) {
    appStore.showToast(e.message || '回复失败', 'error')
  } finally {
    loading.value = false
  }
}

// 放回漂流瓶
const handleRelease = async () => {
  loading.value = true
  try {
    await releaseBottle(fishedBottle.value.id)
    appStore.showToast('🌊 瓶子已放回大海', 'success')
    showFishResult.value = false
  } catch (e) {
    appStore.showToast(e.message || '操作失败', 'error')
  } finally {
    loading.value = false
  }
}

// 珍藏漂流瓶
const handleCollect = async () => {
  loading.value = true
  try {
    await collectBottle(fishedBottle.value.id, currentUser.value.id)
    appStore.showToast('⭐ 珍藏成功，可在珍藏馆查看', 'success')
    showFishResult.value = false
    // 主动刷新珍藏列表，确保切换到珍藏馆 Tab 时数据是最新的
    fetchCollections()
  } catch (e) {
    appStore.showToast(e.message || '珍藏失败', 'error')
  } finally {
    loading.value = false
  }
}

// 获取我的瓶子
const fetchMyBottles = async () => {
  if (!currentUser.value) return
  loading.value = true
  try {
    const res = await getMySentBottles(currentUser.value.id, pagination.value.pageNum, pagination.value.pageSize)
    if (res.code === '200') {
      myBottles.value = res.data.list || []
      pagination.value.total = res.data.total
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

// 获取珍藏列表
const fetchCollections = async () => {
  if (!currentUser.value) return
  loading.value = true
  try {
    const res = await getMyCollectedBottles(currentUser.value.id, pagination.value.pageNum, pagination.value.pageSize)
    if (res.code === '200') {
      myCollections.value = res.data.list || []
      pagination.value.total = res.data.total
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

// 获取成就
const fetchAchievements = async () => {
  if (!currentUser.value) return
  loading.value = true
  try {
    const res = await getAchievements(currentUser.value.id)
    if (res.code === '200') {
      achievements.value = res.data || []
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

// 查看瓶子详情
const viewDetail = async (bottle) => {
  try {
    const res = await getBottleDetail(bottle.id, currentUser.value?.id)
    if (res.code === '200') {
      detailBottle.value = res.data
      showDetailDialog.value = true
    }
  } catch (e) {
    appStore.showToast('获取详情失败', 'error')
  }
}

// 删除瓶子 - 触发确认对话框
const handleDelete = (bottle) => {
  deleteTarget.value = bottle
  showDeleteDialog.value = true
}

// 确认删除
const confirmDelete = async () => {
  if (!deleteTarget.value) return

  deleteLoading.value = true
  try {
    await deleteBottle(deleteTarget.value.id, currentUser.value.id)
    appStore.showToast('✅ 删除成功', 'success')
    showDeleteDialog.value = false
    deleteTarget.value = null
    fetchMyBottles()
  } catch (e) {
    appStore.showToast(e.message || '删除失败', 'error')
  } finally {
    deleteLoading.value = false
  }
}

// 切换标签
const switchTab = (tab) => {
  activeTab.value = tab
  pagination.value.pageNum = 1
  if (tab === 'my') fetchMyBottles()
  else if (tab === 'collection') fetchCollections()
  else if (tab === 'achievement') fetchAchievements()
}

// 状态文本
const getStatusText = (status) => {
  const map = { 0: '漂流中', 1: '已被捞起', 2: '已被珍藏', 3: '已过期' }
  return map[status] || '未知'
}

const getStatusClass = (status) => {
  const map = { 0: 'bg-blue-100 text-blue-700', 1: 'bg-yellow-100 text-yellow-700', 2: 'bg-pink-100 text-pink-700', 3: 'bg-gray-100 text-gray-500' }
  return map[status] || 'bg-gray-100 text-gray-500'
}

// 成就图标（已废弃，改用后端返回的图标）
const achievementIcons = {
  'FIRST_FISH': '🎣',
  'FISH_50': '🐟',
  'REPLY_10': '💙',
  'COLLECT_30': '⭐'
}

// 格式化时间
const formatTime = (timeStr) => {
  if (!timeStr) return ''
  // 将 ISO 8601 格式转换为友好格式
  // 2025-12-21T19:19:02 -> 2025-12-21 19:19:02
  return timeStr.replace('T', ' ').substring(0, 19)
}

onMounted(async () => {
  if (currentUser.value) {
    fetchAchievements()
    // 优先同步后端冷却状态，而不是本地状态
    syncCooldownWithBackend()
  }

  // 如果是从通知跳转过来（带有 bottleId 参数），自动弹出对应漂流瓶的详情弹窗
  const targetBottleId = route.query.bottleId
  if (targetBottleId) {
    try {
      const res = await getBottleDetail(targetBottleId, currentUser.value?.id)
      if (res.code === '200' && res.data) {
        detailBottle.value = res.data
        showDetailDialog.value = true
      }
    } catch (e) {
      console.error('自动弹出漂流瓶详情失败', e)
    }
  }
})

onUnmounted(() => {
  if (cooldownTimer) clearInterval(cooldownTimer)
})
</script>

<template>
  <div class="min-h-screen relative overflow-hidden"
    style="background: linear-gradient(180deg, #064e3b 0%, #047857 30%, #10b981 70%, #6ee7b7 100%);">
    <!-- 波浪动画背景 -->
    <div class="absolute inset-0 overflow-hidden pointer-events-none">
      <div class="wave wave1"></div>
      <div class="wave wave2"></div>
      <div class="wave wave3"></div>
    </div>

    <!-- 顶部导航 -->
    <header class="sticky top-0 z-50 bg-white/10 backdrop-blur-md border-b border-white/20">
      <div class="container mx-auto px-4 h-14 flex items-center justify-between max-w-4xl">
        <Button variant="ghost" class="text-white hover:bg-white/20 whitespace-nowrap flex-shrink-0"
          @click="router.push('/home')">
          <ArrowLeft class="w-5 h-5 mr-2" /> 返回
        </Button>
        <h1 class="text-xl font-bold text-white flex items-center gap-2">
          <Waves class="w-6 h-6" /> 漂流瓶
        </h1>
        <div class="w-20"></div>
      </div>
    </header>

    <!-- 标签切换 -->
    <div class="container mx-auto px-4 max-w-4xl mt-4">
      <div class="flex gap-2 bg-white/10 backdrop-blur-md rounded-full p-1">
        <button v-for="tab in [
          { key: 'ocean', label: '海洋', icon: Waves },
          { key: 'my', label: '我的瓶子', icon: Send },
          { key: 'collection', label: '珍藏馆', icon: Star },
          { key: 'achievement', label: '成就', icon: Trophy }
        ]" :key="tab.key"
          class="flex-1 py-2 px-4 rounded-full text-sm font-medium transition-all flex items-center justify-center gap-1"
          :class="activeTab === tab.key ? 'bg-white text-emerald-700 shadow' : 'text-white/80 hover:text-white'"
          @click="switchTab(tab.key)">
          <component :is="tab.icon" class="w-4 h-4" />
          {{ tab.label }}
        </button>
      </div>
    </div>

    <!-- 主内容区 -->
    <main class="container mx-auto px-4 py-6 max-w-4xl relative z-10">
      <!-- 海洋页面 -->
      <div v-if="activeTab === 'ocean'" class="flex flex-col items-center justify-center min-h-[60vh] space-y-8">
        <!-- 漂流瓶动画 -->
        <div class="relative w-40 h-40">
          <div class="bottle-float absolute inset-0 flex items-center justify-center"
            :class="{ 'bottle-throw': showThrowAnimation }">
            <div class="text-8xl">🍾</div>
          </div>
        </div>

        <p class="text-white/80 text-center text-lg">将心事装进瓶子，让它漂向远方...</p>

        <!-- 操作按钮 -->
        <div class="flex gap-4">
          <Button size="lg"
            class="bg-white text-emerald-700 hover:bg-emerald-50 shadow-lg px-8 whitespace-nowrap flex-shrink-0"
            @click="showThrowDialog = true">
            <Send class="w-5 h-5 mr-2" /> 投放瓶子
          </Button>
          <Button size="lg"
            class="bg-emerald-600 text-white hover:bg-emerald-700 shadow-lg px-8 relative whitespace-nowrap flex-shrink-0"
            :disabled="fishLoading || cooldown > 0" @click="handleFish">
            <Anchor class="w-5 h-5 mr-2" />
            <!--冷却动画 - 圆形进度条 -->
            <span v-if="cooldown > 0" class="flex items-center gap-2">
              <div class="relative w-4 h-4">
                <svg class="w-4 h-4 -rotate-90" viewBox="0 0 16 16">
                  <circle cx="8" cy="8" r="6" fill="none" stroke="currentColor" stroke-opacity="0.2" stroke-width="2" />
                  <circle cx="8" cy="8" r="6" fill="none" stroke="currentColor" stroke-width="2" stroke-dasharray="37.7"
                    :stroke-dashoffset="37.7 * (cooldown / 60)" class="transition-all duration-1000" />
                </svg>
              </div>
              {{ cooldown }}s
            </span>
            <span v-else>打捞瓶子</span>
          </Button>
        </div>
      </div>

      <!-- 我的瓶子列表 -->
      <div v-else-if="activeTab === 'my'" class="space-y-4">
        <div v-if="!currentUser" class="text-center py-20 text-white/60">
          请先登录查看你的漂流瓶
        </div>
        <div v-else-if="myBottles.length === 0 && !loading" class="text-center py-20 text-white/60">
          <div class="text-6xl mb-4">🌊</div>
          <p>你还没有投放过漂流瓶</p>
        </div>
        <template v-else>
          <Card v-for="bottle in myBottles" :key="bottle.id" class="bg-white/90 backdrop-blur border-none shadow-lg">
            <CardContent class="p-4">
              <div class="flex justify-between items-start mb-2">
                <Badge :class="getStatusClass(bottle.status)">{{ getStatusText(bottle.status) }}</Badge>
                <span class="text-xs text-gray-400">{{ formatTime(bottle.createTime) }}</span>
              </div>
              <p class="text-gray-700 mb-3 line-clamp-3">{{ bottle.content }}</p>
              <div class="flex justify-between items-center text-sm text-gray-500">
                <span>被查看 {{ bottle.viewCount || 0 }} 次</span>
                <div class="flex gap-2">
                  <Button size="sm" variant="ghost" class="whitespace-nowrap" @click="viewDetail(bottle)">查看详情</Button>
                  <Button size="sm" variant="ghost" class="text-red-500 hover:text-red-600 whitespace-nowrap"
                    @click="handleDelete(bottle)">删除</Button>
                </div>
              </div>
            </CardContent>
          </Card>
          <!--列表底部提示 -->
          <ListEndIndicator v-if="myBottles.length > 0" text="已经到底啦" icon="🍾" />
        </template>
      </div>

      <!-- 珍藏馆 -->
      <div v-else-if="activeTab === 'collection'" class="space-y-4">
        <div v-if="!currentUser" class="text-center py-20 text-white/60">
          请先登录查看珍藏馆
        </div>
        <div v-else-if="myCollections.length === 0 && !loading" class="text-center py-20 text-white/60">
          <div class="text-6xl mb-4">⭐</div>
          <p>珍藏馆空空如也，快去打捞瓶子吧</p>
        </div>
        <template v-else>
          <Card v-for="bottle in myCollections" :key="bottle.id"
            class="bg-white/90 backdrop-blur border-none shadow-lg">
            <CardContent class="p-4">
              <div class="flex items-center gap-3 mb-3">
                <Avatar class="w-10 h-10">
                  <AvatarImage :src="bottle.isAnonymous === 1 ? '/default.png' : (bottle.avatar || '/default.png')" />
                  <AvatarFallback>
                    <User class="w-5 h-5" />
                  </AvatarFallback>
                </Avatar>
                <div>
                  <div class="font-medium text-gray-800">{{ bottle.isAnonymous === 1 ? '匿名' : (bottle.nickname || '匿名') }}</div>
                  <div class="text-xs text-gray-400">{{ formatTime(bottle.createTime) }}</div>
                </div>
              </div>
              <p class="text-gray-700 mb-3">{{ bottle.content }}</p>
              <Button size="sm" variant="outline" class="whitespace-nowrap" @click="viewDetail(bottle)">查看详情</Button>
            </CardContent>
          </Card>
          <!--列表底部提示 -->
          <ListEndIndicator v-if="myCollections.length > 0" text="珍藏已全部展示" icon="⭐" />
        </template>
      </div>

      <!-- 成就页面 -->
      <div v-else-if="activeTab === 'achievement'" class="space-y-4">
        <div v-if="!currentUser" class="text-center py-20 text-white/60">
          请先登录查看成就
        </div>
        <div v-else class="grid grid-cols-2 gap-4">
          <Card v-for="ach in [
            { type: 'FIRST_FISH', name: '初访者', desc: '首次打捞漂流瓶', icon: '🎣' },
            { type: 'FISH_50', name: '捕瓶达人', desc: '打捞50个漂流瓶', icon: '🐟' },
            { type: 'REPLY_10', name: '海洋之心', desc: '收到10次回复', icon: '💙' },
            { type: 'COLLECT_30', name: '收藏家', desc: '珍藏30个漂流瓶', icon: '⭐' }
          ]" :key="ach.type" class="backdrop-blur border-none shadow-lg transition-transform hover:scale-105"
            :class="achievements.some(a => a.achievementType === ach.type) ? 'bg-white/90' : 'bg-white/30'">
            <CardContent class="p-4 text-center">
              <div class="text-4xl mb-2">{{ ach.icon }}</div>
              <div class="font-bold"
                :class="achievements.some(a => a.achievementType === ach.type) ? 'text-gray-800' : 'text-white/60'">
                {{ ach.name }}
              </div>
              <div class="text-xs"
                :class="achievements.some(a => a.achievementType === ach.type) ? 'text-gray-500' : 'text-white/40'">
                {{ ach.desc }}
              </div>
              <Badge v-if="achievements.some(a => a.achievementType === ach.type)"
                class="mt-2 bg-green-100 text-green-700">
                已获得</Badge>
            </CardContent>
          </Card>
        </div>
      </div>
    </main>

    <!-- 投放弹窗 -->
    <Dialog :open="showThrowDialog" @update:open="showThrowDialog = $event">
      <DialogContent class="sm:max-w-md bg-gradient-to-b from-emerald-50 to-white">
        <DialogHeader>
          <DialogTitle class="flex items-center gap-2 text-emerald-700">
            <Send class="w-5 h-5" /> 投放漂流瓶
          </DialogTitle>
        </DialogHeader>
        <div class="space-y-4 py-4">
          <div>
            <label class="text-sm font-medium text-gray-700 mb-2 block">写下你的心事</label>
            <Textarea v-model="throwForm.content" placeholder="在这里写下你想说的话... (Enter换行)"
              class="min-h-[120px] resize-none" maxlength="200" />
            <div class="text-right text-xs text-gray-400 mt-1">{{ throwForm.content.length }}/200</div>
          </div>
          <div>
            <label class="text-sm font-medium text-gray-700 mb-2 block">漂流方向</label>
            <div class="grid grid-cols-3 gap-2">
              <button v-for="d in directions" :key="d.value" class="p-3 rounded-lg border-2 transition-all text-center"
                :class="throwForm.direction === d.value ? 'border-emerald-500 bg-emerald-50' : 'border-gray-200 hover:border-gray-300'"
                @click="throwForm.direction = d.value">
                <div class="font-medium text-sm">{{ d.label }}</div>
                <div class="text-xs text-gray-400">{{ d.desc }}</div>
              </button>
            </div>
          </div>
          <div class="flex items-center gap-2 pt-2">
            <input type="checkbox" id="anonymousCheck" v-model="throwForm.isAnonymous" class="w-4 h-4 text-emerald-600 rounded">
            <label for="anonymousCheck" class="text-sm text-gray-700 font-medium">不显示我的昵称和头像（匿名投放）</label>
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" class="whitespace-nowrap" @click="showThrowDialog = false">取消</Button>
          <Button class="bg-emerald-600 hover:bg-emerald-700 whitespace-nowrap flex-shrink-0" :disabled="loading"
            @click="handleThrow">
            <Send class="w-4 h-4 mr-2" /> 投入大海
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- 打捞结果弹窗 -->
    <Dialog :open="showFishResult" @update:open="showFishResult = $event">
      <DialogContent class="sm:max-w-md bg-gradient-to-b from-amber-50 to-white">
        <DialogHeader>
          <DialogTitle class="flex items-center gap-2 text-amber-700">
            <Anchor class="w-5 h-5" /> 捞到一个漂流瓶！
          </DialogTitle>
        </DialogHeader>
        <div v-if="fishedBottle" class="py-4">
          <div class="flex items-center gap-3 mb-4">
            <Avatar class="w-12 h-12 border-2 border-amber-200">
              <AvatarImage :src="fishedBottle.isAnonymous === 1 ? '/default.png' : (fishedBottle.avatar || '/default.png')" />
              <AvatarFallback class="bg-amber-100">
                <User class="w-6 h-6 text-amber-600" />
              </AvatarFallback>
            </Avatar>
            <div>
              <div class="font-medium">{{ fishedBottle.isAnonymous === 1 ? '匿名' : (fishedBottle.nickname || '匿名') }}</div>
              <div class="text-xs text-gray-400">{{ formatTime(fishedBottle.createTime) }}</div>
            </div>
          </div>
          <div class="bg-amber-50 rounded-lg p-4 mb-4 border border-amber-100">
            <p class="text-gray-700 whitespace-pre-wrap">{{ fishedBottle.content }}</p>
          </div>
          <div class="flex gap-2">
            <Button class="flex-1 whitespace-nowrap" variant="outline" @click="showReplyDialog = true">
              <MessageCircle class="w-4 h-4 mr-2" /> 回复
            </Button>
            <Button class="flex-1 whitespace-nowrap" variant="outline" @click="handleRelease">
              <RotateCcw class="w-4 h-4 mr-2" /> 放回
            </Button>
            <Button class="flex-1 bg-pink-500 hover:bg-pink-600 text-white whitespace-nowrap" @click="handleCollect">
              <Heart class="w-4 h-4 mr-2" /> 珍藏
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>

    <!-- 回复弹窗 -->
    <Dialog :open="showReplyDialog" @update:open="showReplyDialog = $event">
      <DialogContent class="sm:max-w-md">
        <DialogHeader>
          <DialogTitle class="flex items-center gap-2">
            <MessageCircle class="w-5 h-5" /> 回复漂流瓶
          </DialogTitle>
        </DialogHeader>
        <div class="py-4">
          <Textarea v-model="replyContent" placeholder="写下你想对TA说的话... (Enter换行)" class="min-h-[100px] resize-none"
            maxlength="200" />
          <div class="text-right text-xs text-gray-400 mt-1">{{ replyContent.length }}/200</div>
        </div>
        <DialogFooter>
          <Button variant="outline" class="whitespace-nowrap" @click="showReplyDialog = false">取消</Button>
          <Button class="bg-emerald-600 hover:bg-emerald-700 whitespace-nowrap" :disabled="loading"
            @click="handleReply">发送回复</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- 瓶子详情弹窗 -->
    <Dialog :open="showDetailDialog" @update:open="showDetailDialog = $event">
      <DialogContent class="sm:max-w-lg max-h-[85vh] flex flex-col">
        <DialogHeader class="flex-shrink-0">
          <DialogTitle>瓶子详情</DialogTitle>
        </DialogHeader>
        <div v-if="detailBottle" class="flex-1 overflow-y-auto min-h-0 py-4 space-y-4">
          <div class="bg-gray-50 rounded-lg p-4">
            <p class="text-gray-700 whitespace-pre-wrap">{{ detailBottle.content }}</p>
            <div class="flex justify-between items-center mt-3 text-sm text-gray-400">
              <span>{{ formatTime(detailBottle.createTime) }}</span>
              <Badge :class="getStatusClass(detailBottle.status)">{{ getStatusText(detailBottle.status) }}</Badge>
            </div>
          </div>
          <!-- 回复列表 -->
          <div v-if="detailBottle.replies && detailBottle.replies.length > 0">
            <h4 class="font-medium text-gray-700 mb-2">收到的回复</h4>
            <div class="space-y-2">
              <div v-for="reply in detailBottle.replies" :key="reply.id" class="bg-blue-50 rounded-lg p-3">
                <div class="flex items-center gap-2 mb-1">
                  <Avatar class="w-6 h-6">
                    <AvatarImage :src="reply.avatar || '/default.png'" />
                    <AvatarFallback>
                      <User class="w-3 h-3" />
                    </AvatarFallback>
                  </Avatar>
                  <span class="text-sm font-medium">{{ reply.nickname || '匿名' }}</span>
                  <span class="text-xs text-gray-400">{{ formatTime(reply.createTime) }}</span>
                </div>
                <p class="text-sm text-gray-600">{{ reply.content }}</p>
              </div>
            </div>
          </div>
        </div>
      </DialogContent>
    </Dialog>

    <!-- 删除确认对话框 -->
    <Dialog :open="showDeleteDialog" @update:open="showDeleteDialog = $event">
      <DialogContent class="sm:max-w-md">
        <DialogHeader>
          <DialogTitle class="text-red-600 flex items-center gap-2">
            <AlertTriangle class="w-5 h-5" /> 确认删除
          </DialogTitle>
        </DialogHeader>
        <div class="py-4 space-y-3">
          <p class="text-gray-700">确定要删除这个漂流瓶吗？</p>
          <div class="bg-red-50 border border-red-200 rounded-lg p-3">
            <p class="text-sm text-red-600 flex items-center gap-2">
              <AlertTriangle class="w-4 h-4" />
              删除后无法恢复，且会失去所有回复记录
            </p>
          </div>
          <div v-if="deleteTarget" class="bg-gray-50 rounded-lg p-3">
            <p class="text-sm text-gray-600 line-clamp-2">
              {{ deleteTarget.content }}
            </p>
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" class="whitespace-nowrap" @click="showDeleteDialog = false">取消</Button>
          <Button class="bg-red-500 hover:bg-red-600 text-white whitespace-nowrap" :disabled="deleteLoading"
            @click="confirmDelete">
            <Loader2 v-if="deleteLoading" class="w-4 h-4 mr-2 animate-spin" />
            确认删除
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>

<style scoped>
/* 波浪动画 */
.wave {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 200%;
  height: 100px;
  background: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 1200 120'%3E%3Cpath fill='%23ffffff' fill-opacity='0.1' d='M0,60 C150,120 350,0 600,60 C850,120 1050,0 1200,60 L1200,120 L0,120 Z'/%3E%3C/svg%3E") repeat-x;
  animation: wave 8s linear infinite;
}

.wave1 {
  bottom: 0;
  opacity: 1;
  animation-delay: 0s;
}

.wave2 {
  bottom: 10px;
  opacity: 0.5;
  animation-delay: -2s;
  animation-duration: 10s;
}

.wave3 {
  bottom: 20px;
  opacity: 0.3;
  animation-delay: -4s;
  animation-duration: 12s;
}

@keyframes wave {
  0% {
    transform: translateX(0);
  }

  100% {
    transform: translateX(-50%);
  }
}

/* 漂流瓶漂浮动画 */
.bottle-float {
  animation: float 3s ease-in-out infinite;
}

@keyframes float {

  0%,
  100% {
    transform: translateY(0) rotate(-5deg);
  }

  50% {
    transform: translateY(-20px) rotate(5deg);
  }
}

/*投放动画 - 瓶子抛入海浪 */
.bottle-throw {
  animation: throw-bottle 1.5s ease-out forwards !important;
}

@keyframes throw-bottle {
  0% {
    transform: translateY(0) rotate(-5deg) scale(1);
    opacity: 1;
  }

  30% {
    transform: translateY(-60px) rotate(180deg) scale(0.8);
  }

  60% {
    transform: translateY(100px) rotate(360deg) scale(0.6);
    opacity: 0.8;
  }

  100% {
    transform: translateY(200px) rotate(540deg) scale(0.3);
    opacity: 0;
  }
}
</style>
