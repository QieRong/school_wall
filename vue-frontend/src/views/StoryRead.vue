<script setup>
import { ref, onMounted, computed, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import {
  ArrowLeft, BookOpen, Users, Heart, Star, PenTool,
  ChevronLeft, ChevronRight, Flame
} from 'lucide-vue-next'
import ParagraphCard from '@/components/ParagraphCard.vue'
import ContributionRank from '@/components/ContributionRank.vue'
import StoryContinueModal from '@/components/StoryContinueModal.vue'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { useStoryDraft } from '@/composables/useStoryDraft'
import { useLikeWithDebounce } from '@/composables/useLikeWithDebounce'
import {
  getStoryDetail, checkCanContinue, continueStory, toggleLike,
  toggleFavorite, updateReadProgress, acquireStoryLock
} from '@/api/story'
import { useWsStore } from '@/stores/ws'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const appStore = useAppStore()
const wsStore = useWsStore()
const currentUser = computed(() => userStore.user)

// 草稿管理
const draftManager = computed(() => {
  if (!currentUser.value) return null
  return useStoryDraft(storyId.value, currentUser.value.id)
})

const storyId = computed(() => route.params.id)
const loading = ref(true)
const story = ref(null)
const paragraphs = ref([])
const currentIndex = ref(0)
const contributionRank = ref([])

// 续写相关
const showContinueDialog = ref(false)
const continueLoading = ref(false)
const canContinueResult = ref(null)

// 动画开关
const enableAnimation = ref(false)

//阅读追踪
const readParagraphs = ref(new Set())
const readProgress = ref(0)

// 当前段落
const currentParagraph = computed(() => paragraphs.value[currentIndex.value] || null)

// 点赞API调用函数
const likeApiCall = async (paragraphId, userId) => {
  const res = await toggleLike(paragraphId, userId)
  return res
}

// 使用防抖点赞composable
const { handleLike: debouncedHandleLike, isLoading: isLikeLoading, getCooldownRemaining } = useLikeWithDebounce(likeApiCall)

// 点赞处理
const handleLike = async () => {
  if (!currentUser.value) {
    return appStore.showToast('请先登录', 'error')
  }
  if (!currentParagraph.value) return

  const paragraphId = currentParagraph.value.id
  const originalLiked = currentParagraph.value.isLikedByMe
  const originalCount = currentParagraph.value.likeCount || 0

  const result = await debouncedHandleLike(paragraphId, currentUser.value.id, originalLiked)

  if (result.success) {
    const res = result.data
    if (res.code === '200') {
      currentParagraph.value.isLikedByMe = res.data.liked
      currentParagraph.value.likeCount = res.data.likeCount

      if (res.data.liked) {
        appStore.showToast('❤️ 点赞成功', 'success')
      } else {
        appStore.showToast('已取消点赞', 'info')
      }
    } else {
      appStore.showToast(res.msg || '操作失败', 'error')
    }
  } else {
    if (result.type === 'cooldown') {
      appStore.showToast(result.message, 'warning')
    } else if (result.type === 'spam') {
      if (result.forcedUnlike) {
        currentParagraph.value.isLikedByMe = false
        currentParagraph.value.likeCount = Math.max(0, originalCount - 1)
      }
      appStore.showToast(result.message, 'warning')
    } else if (result.type === 'loading') {
      appStore.showToast(result.message, 'warning')
    } else {
      appStore.showToast(result.message || '操作失败', 'error')
    }
  }
}

// 加载故事详情
const loadStory = async () => {
  loading.value = true
  try {
    const res = await getStoryDetail(storyId.value, currentUser.value?.id)
    if (res.code === '200') {
      story.value = res.data
      paragraphs.value = res.data.paragraphs || []
      contributionRank.value = res.data.contributionRank || []
    }
  } catch (e) {
    appStore.showToast('加载故事失败', 'error')
  } finally {
    loading.value = false
  }
}

// 切换段落（添加阅读追踪）
const goToParagraph = (index) => {
  if (index >= 0 && index < paragraphs.value.length) {
    currentIndex.value = index

    //记录已阅读的段落
    readParagraphs.value.add(index)

    // 计算阅读进度
    readProgress.value = Math.round(
      (readParagraphs.value.size / paragraphs.value.length) * 100
    )

    // 更新阅读进度
    if (currentUser.value) {
      updateReadProgress(storyId.value, currentUser.value.id, paragraphs.value[index].sequence)
    }
  }
}

// 收藏
const handleFavorite = async () => {
  if (!currentUser.value) {
    return appStore.showToast('请先登录', 'error')
  }
  try {
    const res = await toggleFavorite(storyId.value, currentUser.value.id)
    if (res.code === '200') {
      story.value.isFavorited = res.data
      appStore.showToast(res.data ? '已收藏' : '已取消收藏', 'success')
    }
  } catch (e) {
    appStore.showToast('操作失败', 'error')
  }
}

//检查阅读要求
const checkReadRequirement = () => {
  const len = paragraphs.value.length
  if (len === 0) return { canContinue: false, message: '故事还没有开始' }

  // 需要阅读最近 3 段
  const requiredReads = []
  for (let i = Math.max(0, len - 3); i < len; i++) {
    requiredReads.push(i)
  }

  const unreadIndexes = requiredReads.filter(idx => !readParagraphs.value.has(idx))

  if (unreadIndexes.length > 0) {
    return {
      canContinue: false,
      message: `请先阅读最近 ${unreadIndexes.length} 段内容`,
      firstUnread: unreadIndexes[0]
    }
  }

  return { canContinue: true }
}

// 打开续写弹窗（添加阅读校验）
const openContinueDialog = async () => {
  if (!currentUser.value) {
    return appStore.showToast('请先登录', 'error')
  }

  //检查阅读要求
  const readCheck = checkReadRequirement()
  if (!readCheck.canContinue) {
    appStore.showToast(`⚠️ ${readCheck.message}，了解故事走向后再续写`, 'warning')

    // 自动跳转到第一个未读段落
    if (readCheck.firstUnread !== undefined) {
      setTimeout(() => {
        goToParagraph(readCheck.firstUnread)
      }, 500)
    }
    return
  }

  // 检查续写资格
  try {
    const res = await checkCanContinue(storyId.value, currentUser.value.id)
    canContinueResult.value = res.data

    //检查是否被他人锁定
    if (!res.data.canContinue && res.data.lockedByOther) {
      appStore.showToast(`🔒 旅人 [${res.data.lockingUserNickname}] 正在续写，预计将在 ${res.data.lockExpireSeconds}秒 内释放`, 'warning')
      return
    }

    //检查是否连续续写
    if (!res.data.canContinue && res.data.reason === 'CONSECUTIVE_WRITE') {
      appStore.showToast('⚠️ 不能连续续写，让其他旅人也参与吧', 'warning')
      return
    }

    // 其他不能续写的情况（除了锁定或连续续写之外的理由可以直接抛出或者提示）
    if (!res.data.canContinue) {
      appStore.showToast(`⚠️ ${res.data.reason}`, 'warning')
      return
    }

    //尝试获取锁
    try {
      await acquireStoryLock(storyId.value, currentUser.value.id)
      showContinueDialog.value = true
    } catch (e) {
      appStore.showToast(e.msg || '获取续写锁失败，请稍后重试', 'error')
    }
  } catch (e) {
    appStore.showToast('检查续写资格失败', 'error')
  }
}

// 提交续写（从弹窗组件接收数据）
const handleContinueSubmit = async (formData) => {
  continueLoading.value = true
  try {
    const res = await continueStory(storyId.value, {
      userId: currentUser.value.id,
      content: formData.content,
      penName: formData.penName || null,
      imageUrl: formData.imageUrl || null,
      isKeyPoint: formData.isKeyPoint,
      isAiAssisted: formData.isAiAssisted || 0  // 是否使用了AI润色
    })
    if (res.code === '200') {
      appStore.showToast('🎉 续写成功！', 'success')
      showContinueDialog.value = false

      // 提交成功后清除草稿
      if (draftManager.value) {
        draftManager.value.clearDraft()
      }

      loadStory()
      // 跳转到最新段落
      setTimeout(() => {
        currentIndex.value = paragraphs.value.length - 1
      }, 500)
    } else {
      appStore.showToast(res.msg || '续写失败', 'error')
      // 提交失败时保存草稿
      if (draftManager.value) {
        draftManager.value.saveDraft(formData)
      }
    }
  } catch (e) {
    appStore.showToast(e.message || '续写失败', 'error')
    // 提交失败时保存草稿
    if (draftManager.value) {
      draftManager.value.saveDraft(formData)
    }
  } finally {
    continueLoading.value = false
  }
}

// 获取最近3段上下文
const contextParagraphs = computed(() => {
  const len = paragraphs.value.length
  if (len <= 3) return paragraphs.value
  return paragraphs.value.slice(-3)
})

// 键盘导航
const handleKeydown = (e) => {
  if (showContinueDialog.value) return
  if (e.key === 'ArrowLeft') goToParagraph(currentIndex.value - 1)
  if (e.key === 'ArrowRight') goToParagraph(currentIndex.value + 1)
}

// ==== 锁机制与WebSocket ====
const lockInterval = ref(null)

const formatLockTime = computed(() => {
  if (!story.value || !story.value.lockExpireSeconds) return '0秒'
  const m = Math.floor(story.value.lockExpireSeconds / 60)
  const s = story.value.lockExpireSeconds % 60
  return m > 0 ? `${m}分${s}秒` : `${s}秒`
})

const startLockCountdown = () => {
  if (lockInterval.value) clearInterval(lockInterval.value)
  lockInterval.value = setInterval(() => {
    if (story.value && story.value.lockExpireSeconds > 0) {
      story.value.lockExpireSeconds--
    } else {
      if (story.value) story.value.lockedByOther = false
      if (lockInterval.value) clearInterval(lockInterval.value)
    }
  }, 1000)
}

import { watch } from 'vue'
watch(() => story.value?.lockedByOther, (val) => {
  if (val) startLockCountdown()
})

watch(() => wsStore.messages, (newVal) => {
  if (newVal.length === 0) return
  const lastMsg = newVal[newVal.length - 1]
  if (typeof lastMsg === 'string') {
    try {
      const data = JSON.parse(lastMsg)
      if (data.type === 'STORY_LOCK_RELEASED' && String(data.storyId) === String(storyId.value)) {
        if (story.value && story.value.lockedByOther) {
          story.value.lockedByOther = false
          story.value.lockExpireSeconds = 0
          appStore.showToast('✅ 锁已释放，你可以续写了！', 'success')
          if (lockInterval.value) {
            clearInterval(lockInterval.value)
            lockInterval.value = null
          }
        }
      } else if (data.type === 'STORY_NEW_PARAGRAPH' && String(data.storyId) === String(storyId.value)) {
        // 其他人发布了新段落，刷新详情
        loadStory()
        appStore.showToast('✨ 故事有新进展了！', 'info')
      }
    } catch { }
  }
}, { deep: true })


onMounted(() => {
  loadStory()
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
  if (lockInterval.value) clearInterval(lockInterval.value)
})
</script>


<template>
  <div class="min-h-screen story-read-bg">
    <!-- 顶部导航 -->
    <header class="sticky top-0 z-50 bg-[rgb(33,111,85)]/90 backdrop-blur-md border-b border-emerald-700/50">
      <div class="container mx-auto px-4 h-14 flex items-center justify-between max-w-6xl">
        <button class="custom-nav-btn" @click="router.back()">
          <ArrowLeft class="w-5 h-5 mr-2" /> 返回大厅
        </button>
        <h1 v-if="story" class="text-lg font-bold text-white font-serif truncate max-w-[200px]">
          {{ story.title }}
        </h1>
        <div class="flex items-center gap-2">
          <button class="custom-nav-btn" @click="handleFavorite">
            <Star class="w-5 h-5" :class="story?.isFavorited ? 'fill-emerald-400 text-emerald-400' : ''" />
          </button>
        </div>
      </div>
    </header>

    <div v-if="loading" class="flex items-center justify-center min-h-[60vh]">
      <div class="text-center text-emerald-700">
        <div class="text-4xl mb-4 animate-bounce">📜</div>
        <p>正在展开卷轴...</p>
      </div>
    </div>

    <div v-else-if="story" class="container mx-auto px-4 py-6 max-w-4xl">
      <!-- 锁定状态栏 -->
      <div v-if="story.lockedByOther" class="bg-yellow-50 border border-yellow-200 text-yellow-800 px-4 py-3 rounded-xl mb-6 flex items-center gap-3">
        <div class="p-2 bg-yellow-100 rounded-lg">
          <PenTool class="w-5 h-5 text-yellow-600 animate-pulse" />
        </div>
        <div class="flex-1">
          <h3 class="font-medium text-sm">正在被他人续写中</h3>
          <p class="text-xs text-yellow-600/80 mt-0.5">旅人 [{{ story.lockingUserNickname }}] 正在撰写新段落，预计在 <span class="font-bold">{{ formatLockTime }}</span> 内释放</p>
        </div>
      </div>

      <!-- 故事信息 -->
      <div class="text-center mb-8">
        <Badge class="mb-2 bg-emerald-600 text-white">{{ story.categoryName }}</Badge>
        <h1 class="text-2xl font-bold text-emerald-900 font-serif mb-2">{{ story.title }}</h1>
        <p v-if="story.worldSetting" class="text-sm text-emerald-700 italic mb-4">「{{ story.worldSetting }}」</p>
        <div class="flex items-center justify-center gap-4 text-sm text-emerald-600">
          <span class="flex items-center gap-1">
            <BookOpen class="w-4 h-4" /> {{ story.paragraphCount }}段
          </span>
          <span class="flex items-center gap-1">
            <Users class="w-4 h-4" /> {{ story.participantCount }}人参与
          </span>
          <span class="flex items-center gap-1">
            <Heart class="w-4 h-4" /> {{ story.totalLikes }}赞
          </span>
        </div>
      </div>

      <!-- 时间轴 -->
      <div class="timeline-container mb-6 overflow-x-auto pb-4">
        <div class="flex items-center gap-2 min-w-max px-4">
          <div v-for="(p, idx) in paragraphs" :key="p.id" class="timeline-node cursor-pointer" :class="{
            'active': idx === currentIndex,
            'hot': p.isHot,
            'key-point': p.isKeyPoint
          }" @click="goToParagraph(idx)">
            <div class="node-icon">
              <Flame v-if="p.isKeyPoint" class="w-4 h-4 text-orange-500" />
              <Heart v-else-if="p.isHot" class="w-4 h-4 text-pink-500" />
              <PenTool v-else class="w-4 h-4" />
            </div>
            <div class="node-label">{{ idx + 1 }}</div>
          </div>
        </div>
      </div>

      <!-- 段落内容 -->
      <ParagraphCard v-if="currentParagraph" :paragraph="currentParagraph" :show-animation="enableAnimation"
        :is-like-loading="isLikeLoading(currentParagraph.id)"
        :cooldown-remaining="getCooldownRemaining(currentParagraph.id)" @like="handleLike" />

      <!-- 段落计数 -->
      <div v-if="currentParagraph" class="text-center mt-4 text-sm text-emerald-500">
        第 {{ currentIndex + 1 }} / {{ paragraphs.length }} 段
      </div>

      <!-- 导航按钮 -->
      <div class="flex items-center justify-between mt-6">
        <Button variant="outline" class="border-emerald-600 text-emerald-700" :disabled="currentIndex === 0"
          @click="goToParagraph(currentIndex - 1)">
          <ChevronLeft class="w-5 h-5 mr-1" /> 上一段
        </Button>

        <Button v-if="story.status === 1" class="bg-emerald-600 hover:bg-emerald-700 text-white"
          @click="openContinueDialog">
          <PenTool class="w-5 h-5 mr-1" /> 续写故事
        </Button>
        <Badge v-else class="bg-gray-500 text-white">故事已完结</Badge>

        <Button variant="outline" class="border-emerald-600 text-emerald-700"
          :disabled="currentIndex === paragraphs.length - 1" @click="goToParagraph(currentIndex + 1)">
          下一段
          <ChevronRight class="w-5 h-5 ml-1" />
        </Button>
      </div>

      <!-- 贡献度排行 -->
      <ContributionRank :rankings="contributionRank" :max-display="5" class="mt-8" />
    </div>

    <!-- 续写弹窗 -->
    <StoryContinueModal
      :open="showContinueDialog"
      @update:open="showContinueDialog = $event"
      :story-id="storyId"
      :can-continue-result="canContinueResult"
      :context-paragraphs="contextParagraphs"
      :loading="continueLoading"
      @submit="handleContinueSubmit" />
  </div>
</template>


<style scoped>
.story-read-bg {
  background: linear-gradient(180deg, #d1fae5 0%, #a7f3d0 100%);
  min-height: 100vh;
}

.timeline-container {
  background: rgba(255, 255, 255, 0.5);
  border-radius: 12px;
  padding: 12px;
}

.timeline-node {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 10px;
  border-radius: 8px;
  transition: all 0.2s;
  min-width: 56px;
  /*确保移动端点击区域至少 44x44px */
  min-height: 44px;
  cursor: pointer;
}

.timeline-node:hover {
  background: rgba(33, 111, 85, 0.1);
}

.timeline-node.active {
  background: rgba(33, 111, 85, 0.2);
}

.timeline-node .node-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #d1fae5;
  border: 2px solid rgb(33, 111, 85);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #065f46;
}

.timeline-node.active .node-icon {
  background: rgb(33, 111, 85);
  color: white;
}

.timeline-node.key-point .node-icon {
  border-color: #34d399;
  background: #a7f3d0;
}

.timeline-node.hot .node-icon {
  border-color: #ec4899;
  background: #fce7f3;
}

.timeline-node .node-label {
  font-size: 10px;
  color: #065f46;
  margin-top: 4px;
}

.create-dialog-bg {
  background: linear-gradient(135deg, #fefce8 0%, #fef9c3 100%);
}

.story-textarea {
  font-family: 'Noto Serif SC', serif;
  line-height: 1.8;
}

/* 自定义导航按钮 */
.custom-nav-btn {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  background: transparent;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.custom-nav-btn:hover {
  background: rgba(5, 150, 105, 0.5);
}

.custom-nav-btn:active {
  transform: scale(0.95);
}
</style>
