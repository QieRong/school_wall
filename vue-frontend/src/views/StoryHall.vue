<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { ArrowLeft, BookOpen, Users, Heart, Sparkles, PenTool, Crown, Search } from 'lucide-vue-next'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { getStoryList, createStory } from '@/api/story'

const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()
const currentUser = computed(() => userStore.user)

// 状态
const loading = ref(false)
const stories = ref([])
const activeCategory = ref(null)
const pagination = ref({ page: 1, size: 12, total: 0 })

// 分类配置
const categories = [
  { value: null, label: '全部', icon: '📚' },
  { value: 1, label: '奇幻校园', icon: '✨' },
  { value: 2, label: '悬疑推理', icon: '🔍' },
  { value: 3, label: '浪漫物语', icon: '💕' },
  { value: 4, label: '搞笑日常', icon: '😄' },
  { value: 5, label: '科幻未来', icon: '🚀' }
]

// 创建故事弹窗
const showCreateDialog = ref(false)
const createForm = ref({
  title: '',
  category: 1,
  worldSetting: '',
  content: ''
})
const createLoading = ref(false)


// 加载故事列表
const loadStories = async () => {
  loading.value = true
  try {
    const res = await getStoryList(activeCategory.value, 1, pagination.value.page, pagination.value.size)
    if (res.code === '200') {
      stories.value = res.data.list || []
      pagination.value.total = res.data.total
    }
  } catch (e) {
    console.error('加载故事列表失败', e)
  } finally {
    loading.value = false
  }
}

// 切换分类
const switchCategory = (category) => {
  activeCategory.value = category
  pagination.value.page = 1
  loadStories()
}

// 进入故事
const enterStory = (story) => {
  router.push(`/story/${story.id}`)
}

// 创建故事
const handleCreate = async () => {
  if (!currentUser.value) {
    return appStore.showToast('请先登录', 'error')
  }
  if (!createForm.value.title.trim()) {
    return appStore.showToast('请输入故事标题', 'error')
  }
  if (!createForm.value.content.trim()) {
    return appStore.showToast('请输入开篇内容', 'error')
  }
  if (createForm.value.content.length < 100) {
    return appStore.showToast('开篇内容至少100字', 'error')
  }
  if (createForm.value.content.length > 500) {
    return appStore.showToast('开篇内容最多500字', 'error')
  }

  createLoading.value = true
  try {
    const res = await createStory({
      userId: currentUser.value.id,
      title: createForm.value.title,
      category: createForm.value.category,
      worldSetting: createForm.value.worldSetting || null,
      content: createForm.value.content
    })
    if (res.code === '200') {
      appStore.showToast('🎉 故事已开启，等待旅人续写...', 'success')
      showCreateDialog.value = false
      createForm.value = { title: '', category: 1, worldSetting: '', content: '' }
      loadStories()
    } else {
      appStore.showToast(res.msg || '创建失败', 'error')
    }
  } catch (e) {
    appStore.showToast(e.message || '创建失败', 'error')
  } finally {
    createLoading.value = false
  }
}

// 获取分类样式
const getCategoryStyle = (category) => {
  const styles = {
    1: 'bg-purple-100 text-purple-700',
    2: 'bg-blue-100 text-blue-700',
    3: 'bg-pink-100 text-pink-700',
    4: 'bg-emerald-100 text-emerald-700',
    5: 'bg-cyan-100 text-cyan-700'
  }
  return styles[category] || 'bg-gray-100 text-gray-700'
}

onMounted(() => {
  loadStories()
})
</script>


<template>
  <div class="min-h-screen story-hall-bg">
    <!-- 顶部导航 -->
    <header class="sticky top-0 z-50 bg-[rgb(33,111,85)]/90 backdrop-blur-md border-b border-emerald-700/50">
      <div class="container mx-auto px-4 h-14 flex items-center justify-between max-w-6xl">
        <button class="custom-nav-btn" @click="router.back()">
          <ArrowLeft class="w-5 h-5 mr-2" /> 返回
        </button>
        <h1 class="text-xl font-bold text-white flex items-center gap-2 font-serif">
          <BookOpen class="w-6 h-6" /> 协作故事链
        </h1>
        <button class="custom-nav-btn" @click="router.push('/story/my')">
          我的故事
        </button>
      </div>
    </header>

    <!-- 分类筛选 -->
    <div class="container mx-auto px-4 max-w-6xl mt-6">
      <div class="flex flex-wrap gap-2 justify-center">
        <button v-for="cat in categories" :key="cat.value"
          class="px-4 py-2 rounded-full text-sm font-medium transition-all flex items-center gap-1" :class="activeCategory === cat.value
            ? 'bg-emerald-600 text-white shadow-lg'
            : 'bg-emerald-100/80 text-emerald-800 hover:bg-emerald-200'" @click="switchCategory(cat.value)">
          <span>{{ cat.icon }}</span>
          {{ cat.label }}
        </button>
      </div>
    </div>

    <!-- 故事列表 -->
    <main class="container mx-auto px-4 py-6 max-w-6xl">
      <div v-if="loading" class="text-center py-20 text-emerald-700">
        <div class="text-4xl mb-4 animate-bounce">📜</div>
        <p>正在展开卷轴...</p>
      </div>

      <div v-else-if="stories.length === 0" class="text-center py-20 text-emerald-700">
        <div class="text-6xl mb-4">📖</div>
        <p class="text-lg">还没有故事，成为第一个开篇者吧！</p>
      </div>

      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <!-- 故事卡片 -->
        <div v-for="story in stories" :key="story.id" class="story-scroll cursor-pointer group"
          @click="enterStory(story)">
          <div class="scroll-content">
            <!-- 推荐标识 -->
            <div v-if="story.isRecommended" class="absolute -top-2 -right-2 z-10">
              <div class="w-10 h-10 bg-emerald-500 rounded-full flex items-center justify-center shadow-lg">
                <Crown class="w-5 h-5 text-white" />
              </div>
            </div>

            <!-- 分类标签 -->
            <Badge :class="getCategoryStyle(story.category)" class="mb-3">
              {{ story.categoryName }}
            </Badge>

            <!-- 标题 -->
            <h3 class="text-lg font-bold text-emerald-900 mb-2 font-serif line-clamp-1 group-hover:text-emerald-700">
              {{ story.title }}
            </h3>

            <!-- 预览 -->
            <p class="text-sm text-emerald-700/80 line-clamp-3 mb-4 leading-relaxed">
              {{ story.firstParagraphPreview }}...
            </p>

            <!-- 统计 -->
            <div class="flex items-center justify-between text-xs text-emerald-600">
              <div class="flex items-center gap-3">
                <span class="flex items-center gap-1">
                  <BookOpen class="w-3 h-3" /> {{ story.paragraphCount }}段
                </span>
                <span class="flex items-center gap-1">
                  <Users class="w-3 h-3" /> {{ story.participantCount }}人
                </span>
                <span class="flex items-center gap-1">
                  <Heart class="w-3 h-3" /> {{ story.totalLikes }}
                </span>
              </div>
            </div>

            <!-- 创建者 -->
            <div class="mt-3 pt-3 border-t border-emerald-200 flex items-center justify-between">
              <span class="text-xs text-emerald-600">by {{ story.creatorNickname }}</span>
              <span class="text-xs text-emerald-500">{{ story.statusName }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 加载更多 -->
      <div v-if="stories.length > 0 && stories.length < pagination.total" class="text-center mt-8">
        <Button variant="outline" class="border-emerald-600 text-emerald-700" @click="pagination.page++; loadStories()">
          加载更多
        </Button>
      </div>
    </main>

    <!-- 创建按钮 -->
    <Button
      class="fixed bottom-6 right-6 w-14 h-14 rounded-full bg-emerald-600 hover:bg-emerald-700 shadow-xl text-white"
      @click="showCreateDialog = true">
      <PenTool class="w-6 h-6" />
    </Button>

    <!-- 创建故事弹窗 -->
    <Dialog v-model:open="showCreateDialog">
      <DialogContent class="sm:max-w-lg create-dialog-bg">
        <DialogHeader>
          <DialogTitle class="flex items-center gap-2 text-emerald-800 font-serif">
            <Sparkles class="w-5 h-5" /> 开启新故事
          </DialogTitle>
        </DialogHeader>
        <div class="space-y-4 py-4">
          <!-- 标题 -->
          <div>
            <label class="text-sm font-medium text-emerald-700 mb-2 block">故事标题</label>
            <Input v-model="createForm.title" placeholder="给你的故事起个名字..." maxlength="30"
              class="bg-emerald-50 border-emerald-200" />
            <div class="text-right text-xs text-emerald-500 mt-1">{{ createForm.title.length }}/30</div>
          </div>

          <!-- 分类 -->
          <div>
            <label class="text-sm font-medium text-emerald-700 mb-2 block">故事分类</label>
            <div class="grid grid-cols-5 gap-2">
              <button v-for="cat in categories.slice(1)" :key="cat.value"
                class="p-2 rounded-lg border-2 transition-all text-center text-xs" :class="createForm.category === cat.value
                  ? 'border-emerald-500 bg-emerald-50'
                  : 'border-emerald-200 hover:border-emerald-300'" @click="createForm.category = cat.value">
                <div class="text-lg">{{ cat.icon }}</div>
                <div class="mt-1">{{ cat.label }}</div>
              </button>
            </div>
          </div>

          <!-- 世界观设定 -->
          <div>
            <label class="text-sm font-medium text-emerald-700 mb-2 block">世界观设定 (选填)</label>
            <Input v-model="createForm.worldSetting" placeholder="描述故事的背景设定..." maxlength="200"
              class="bg-emerald-50 border-emerald-200" />
            <div class="text-right text-xs text-emerald-500 mt-1">{{ createForm.worldSetting.length }}/200</div>
          </div>

          <!-- 开篇内容 -->
          <div>
            <label class="text-sm font-medium text-emerald-700 mb-2 block">开篇内容</label>
            <Textarea v-model="createForm.content" placeholder="写下故事的开篇，让旅人们接力续写... (Enter换行)"
              class="min-h-[150px] bg-emerald-50 border-emerald-200 story-textarea" maxlength="500" />
            <div class="text-right text-xs mt-1"
              :class="createForm.content.length < 100 ? 'text-red-500' : 'text-emerald-500'">
              {{ createForm.content.length }}/500 (至少100字)
            </div>
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" @click="showCreateDialog = false">取消</Button>
          <Button class="bg-emerald-600 hover:bg-emerald-700 text-white" :disabled="createLoading"
            @click="handleCreate">
            <PenTool class="w-4 h-4 mr-2" /> 开启故事
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>


<style scoped>
.story-hall-bg {
  background: linear-gradient(180deg, #d1fae5 0%, #a7f3d0 50%, #6ee7b7 100%);
  background-attachment: fixed;
}

.story-scroll {
  position: relative;
  background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 4px 20px rgba(33, 111, 85, 0.15);
  border: 2px solid rgb(33, 111, 85);
  transition: all 0.3s ease;
}

.story-scroll::before,
.story-scroll::after {
  content: '';
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  width: 90%;
  height: 12px;
  background: linear-gradient(90deg, #059669, rgb(33, 111, 85), #059669);
  border-radius: 6px;
}

.story-scroll::before {
  top: -6px;
}

.story-scroll::after {
  bottom: -6px;
}

.story-scroll:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 30px rgba(33, 111, 85, 0.25);
}

.scroll-content {
  position: relative;
}

.create-dialog-bg {
  background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
}

.story-textarea {
  font-family: 'Noto Serif SC', serif;
  line-height: 1.8;
  background-image: repeating-linear-gradient(transparent,
      transparent 27px,
      rgba(33, 111, 85, 0.2) 27px,
      rgba(33, 111, 85, 0.2) 28px);
  background-position: 0 8px;
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
