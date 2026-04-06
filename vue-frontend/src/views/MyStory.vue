<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import {
  ArrowLeft, BookOpen, PenTool, Heart, Star, Trash2,
  Users, Clock, AlertTriangle, FileText, Award
} from 'lucide-vue-next'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import {
  getMyCreatedStories, getMyParticipatedStories,
  getMyParagraphs, getMyFavorites, getMyAchievements, deleteStory
} from '@/api/story'
import StoryAchievementBadge from '@/components/StoryAchievementBadge.vue'

const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()
const currentUser = computed(() => userStore.user)

// 当前标签
const activeTab = ref('created')

// 数据状态
const loading = ref(false)
const createdStories = ref([])
const participatedStories = ref([])
const myParagraphs = ref([])
const myFavorites = ref([])
const myAchievements = ref([])

// 所有成就类型（6个成就）
const allAchievementTypes = ['opener', 'key_puzzle', 'long_creator', 'story_king', 'active_writer', 'popular_author']

// 成就描述
const achievementDescriptions = {
  opener: { name: '开篇者', desc: '创建第一个故事' },
  key_puzzle: { name: '关键拼图', desc: '段落成为关键转折点' },
  long_creator: { name: '长篇缔造者', desc: '单故事贡献超过10段' },
  story_king: { name: '故事之王', desc: '总贡献度排名第一' },
  active_writer: { name: '活跃作者', desc: '累计续写50段' },
  popular_author: { name: '人气作者', desc: '获得100个点赞' }
}

// 检查成就是否已解锁
const isAchievementUnlocked = (type) => {
  return myAchievements.value.some(a => a.achievementType === type)
}

// 获取成就解锁时间
const getAchievementTime = (type) => {
  const achievement = myAchievements.value.find(a => a.achievementType === type)
  return achievement ? formatTime(achievement.createTime) : null
}

// 删除确认弹窗
const showDeleteDialog = ref(false)
const storyToDelete = ref(null)
const deleteLoading = ref(false)

// 状态映射
const statusMap = {
  1: { label: '进行中', color: 'bg-green-100 text-green-700' },
  2: { label: '已完结', color: 'bg-blue-100 text-blue-700' },
  3: { label: '已归档', color: 'bg-gray-100 text-gray-700' }
}

// 加载我创建的故事
const loadCreatedStories = async () => {
  if (!currentUser.value) return
  loading.value = true
  try {
    console.log('加载我创建的故事，用户ID:', currentUser.value.id)
    const res = await getMyCreatedStories(currentUser.value.id)
    console.log('返回的故事列表:', res.data)
    if (res.code === '200') {
      createdStories.value = res.data.list || []
    }
  } catch (e) {
    console.error('加载失败:', e)
    appStore.showToast('加载失败', 'error')
  } finally {
    loading.value = false
  }
}

// 加载我参与的故事
const loadParticipatedStories = async () => {
  if (!currentUser.value) return
  loading.value = true
  try {
    console.log('加载我参与的故事，用户ID:', currentUser.value.id)
    const res = await getMyParticipatedStories(currentUser.value.id)
    console.log('返回的参与故事列表:', res.data)
    if (res.code === '200') {
      participatedStories.value = res.data.list || []
      console.log('参与的故事数量:', participatedStories.value.length)
    }
  } catch (e) {
    console.error('加载参与故事失败:', e)
    appStore.showToast('加载失败', 'error')
  } finally {
    loading.value = false
  }
}

// 加载我的段落
const loadMyParagraphs = async () => {
  if (!currentUser.value) return
  loading.value = true
  try {
    const res = await getMyParagraphs(currentUser.value.id)
    if (res.code === '200') {
      myParagraphs.value = res.data.list || []
    }
  } catch (e) {
    appStore.showToast('加载失败', 'error')
  } finally {
    loading.value = false
  }
}

// 加载我的收藏
const loadMyFavorites = async () => {
  if (!currentUser.value) return
  loading.value = true
  try {
    const res = await getMyFavorites(currentUser.value.id)
    if (res.code === '200') {
      myFavorites.value = res.data.list || []
    }
  } catch (e) {
    appStore.showToast('加载失败', 'error')
  } finally {
    loading.value = false
  }
}

// 加载我的成就
const loadMyAchievements = async () => {
  if (!currentUser.value) return
  loading.value = true
  try {
    const res = await getMyAchievements(currentUser.value.id)
    if (res.code === '200') {
      myAchievements.value = res.data || []
    }
  } catch (e) {
    appStore.showToast('加载失败', 'error')
  } finally {
    loading.value = false
  }
}

// 切换标签时加载数据
watch(activeTab, (tab) => {
  switch (tab) {
    case 'created':
      loadCreatedStories()
      break
    case 'participated':
      loadParticipatedStories()
      break
    case 'paragraphs':
      loadMyParagraphs()
      break
    case 'favorites':
      loadMyFavorites()
      break
    case 'achievements':
      loadMyAchievements()
      break
  }
})

// 进入故事
const goToStory = (storyId) => {
  router.push(`/story/${storyId}`)
}

// 打开删除确认
const openDeleteDialog = (story) => {
  storyToDelete.value = story
  showDeleteDialog.value = true
}

// 确认删除
const confirmDelete = async () => {
  if (!storyToDelete.value) return

  deleteLoading.value = true
  try {
    const res = await deleteStory(storyToDelete.value.id, currentUser.value.id)
    if (res.code === '200') {
      appStore.showToast('删除成功', 'success')
      showDeleteDialog.value = false
      // 从列表中移除
      createdStories.value = createdStories.value.filter(s => s.id !== storyToDelete.value.id)
    } else {
      appStore.showToast(res.msg || '删除失败', 'error')
    }
  } catch (e) {
    appStore.showToast('删除失败', 'error')
  } finally {
    deleteLoading.value = false
  }
}

// 格式化时间
const formatTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now - date

  if (diff < 86400000) {
    const hours = Math.floor(diff / 3600000)
    if (hours < 1) return '刚刚'
    return `${hours}小时前`
  }
  if (diff < 604800000) {
    return `${Math.floor(diff / 86400000)}天前`
  }
  return `${date.getMonth() + 1}月${date.getDate()}日`
}

onMounted(() => {
  if (!currentUser.value) {
    router.push('/login')
    return
  }
  loadCreatedStories()
})
</script>

<template>
  <div class="min-h-screen my-story-bg">
    <!-- 顶部导航 -->
    <header class="sticky top-0 z-50 bg-[rgb(33,111,85)]/90 backdrop-blur-md border-b border-emerald-700/50">
      <div class="container mx-auto px-4 h-14 flex items-center justify-between max-w-4xl">
        <button class="custom-nav-btn" @click="router.back()">
          <ArrowLeft class="w-5 h-5 mr-2" /> 返回
        </button>
        <h1 class="text-lg font-bold text-white font-serif">我的故事</h1>
        <div class="w-20"></div>
      </div>
    </header>

    <div class="container mx-auto px-4 py-6 max-w-4xl">
      <Tabs v-model="activeTab" class="w-full">
        <TabsList class="tabs-list">
          <TabsTrigger value="created" class="tab-trigger">
            <BookOpen class="w-4 h-4 mr-1" /> 我创建的
          </TabsTrigger>
          <TabsTrigger value="participated" class="tab-trigger">
            <PenTool class="w-4 h-4 mr-1" /> 我参与的
          </TabsTrigger>
          <!-- 隐藏我的段落（以故事维度展示已足够） -->
          <TabsTrigger value="paragraphs" class="tab-trigger" v-if="false">
            <FileText class="w-4 h-4 mr-1" /> 我的段落
          </TabsTrigger>
          <TabsTrigger value="favorites" class="tab-trigger">
            <Star class="w-4 h-4 mr-1" /> 我的收藏
          </TabsTrigger>
          <!-- 隐藏成就标签 -->
          <TabsTrigger value="achievements" class="tab-trigger" v-if="false">
            <Award class="w-4 h-4 mr-1" /> 成就
          </TabsTrigger>
        </TabsList>

        <!-- 我创建的故事 -->
        <TabsContent value="created" class="tab-content">
          <div v-if="loading" class="loading-state">
            <div class="animate-pulse">📚</div>
            <p>加载中...</p>
          </div>
          <div v-else-if="createdStories.length === 0" class="empty-state">
            <div class="text-4xl mb-2">📝</div>
            <p>还没有创建过故事</p>
            <Button class="mt-4 bg-emerald-600 hover:bg-emerald-700" @click="router.push('/story')">
              去创建故事
            </Button>
          </div>
          <div v-else class="story-list">
            <div v-for="story in createdStories" :key="story.id" class="story-card">
              <div class="story-main" @click="goToStory(story.id)">
                <div class="story-header">
                  <h3 class="story-title">{{ story.title }}</h3>
                  <Badge :class="statusMap[story.status]?.color">
                    {{ statusMap[story.status]?.label }}
                  </Badge>
                </div>
                <div class="story-stats">
                  <span>
                    <FileText class="w-3 h-3" /> {{ story.paragraphCount }}段
                  </span>
                  <span>
                    <Users class="w-3 h-3" /> {{ story.participantCount }}人
                  </span>
                  <span>
                    <Heart class="w-3 h-3" /> {{ story.totalLikes }}
                  </span>
                  <span>
                    <Clock class="w-3 h-3" /> {{ formatTime(story.createTime) }}
                  </span>
                </div>
              </div>
              <Button variant="ghost" size="sm" class="delete-btn" @click.stop="openDeleteDialog(story)">
                <Trash2 class="w-4 h-4" />
              </Button>
            </div>
          </div>
        </TabsContent>

        <!-- 我参与的故事 -->
        <TabsContent value="participated" class="tab-content">
          <div v-if="loading" class="loading-state">
            <div class="animate-pulse">📚</div>
            <p>加载中...</p>
          </div>
          <div v-else-if="participatedStories.length === 0" class="empty-state">
            <div class="text-4xl mb-2">✍️</div>
            <p>还没有参与过故事</p>
            <Button class="mt-4 bg-emerald-600 hover:bg-emerald-700" @click="router.push('/story')">
              去参与故事
            </Button>
          </div>
          <div v-else class="story-list">
            <div v-for="story in participatedStories" :key="story.id" class="story-card" @click="goToStory(story.id)">
              <div class="story-main">
                <div class="story-header">
                  <h3 class="story-title">{{ story.title }}</h3>
                  <Badge :class="statusMap[story.status]?.color">
                    {{ statusMap[story.status]?.label }}
                  </Badge>
                </div>
                <div class="story-stats">
                  <span>
                    <FileText class="w-3 h-3" /> {{ story.paragraphCount }}段
                  </span>
                  <span>
                    <Users class="w-3 h-3" /> {{ story.participantCount }}人
                  </span>
                  <span>
                    <Heart class="w-3 h-3" /> {{ story.totalLikes }}
                  </span>
                </div>
                <div class="my-contribution">
                  我的贡献: {{ story.myParagraphCount || 0 }}段 · {{ story.myContribution || 0 }}分
                </div>
              </div>
            </div>
          </div>
        </TabsContent>

        <!-- 我的段落 -->
        <TabsContent value="paragraphs" class="tab-content" v-if="false">
          <div v-if="loading" class="loading-state">
            <div class="animate-pulse">📚</div>
            <p>加载中...</p>
          </div>
          <div v-else-if="myParagraphs.length === 0" class="empty-state">
            <div class="text-4xl mb-2">📄</div>
            <p>还没有续写过段落</p>
          </div>
          <div v-else class="paragraph-list">
            <div v-for="para in myParagraphs" :key="para.id" class="paragraph-card" @click="goToStory(para.storyId)">
              <div class="para-header">
                <span class="para-story">{{ para.storyTitle }}</span>
                <span class="para-sequence">第{{ para.sequence }}段</span>
              </div>
              <p class="para-content">{{ para.content }}</p>
              <div class="para-footer">
                <span class="para-likes">
                  <Heart class="w-3 h-3" :class="{ 'fill-pink-500 text-pink-500': para.likeCount > 0 }" />
                  {{ para.likeCount }}
                </span>
                <Badge v-if="para.isKeyPoint" class="bg-orange-100 text-orange-700">关键转折</Badge>
                <Badge v-else-if="para.isHot" class="bg-pink-100 text-pink-700">热门</Badge>
                <span class="para-time">{{ formatTime(para.createTime) }}</span>
              </div>
            </div>
          </div>
        </TabsContent>

        <!-- 我的收藏 -->
        <TabsContent value="favorites" class="tab-content">
          <div v-if="loading" class="loading-state">
            <div class="animate-pulse">📚</div>
            <p>加载中...</p>
          </div>
          <div v-else-if="myFavorites.length === 0" class="empty-state">
            <div class="text-4xl mb-2">⭐</div>
            <p>还没有收藏故事</p>
          </div>
          <div v-else class="story-list">
            <div v-for="story in myFavorites" :key="story.id" class="story-card" @click="goToStory(story.id)">
              <div class="story-main">
                <div class="story-header">
                  <h3 class="story-title">{{ story.title }}</h3>
                  <Badge :class="statusMap[story.status]?.color">
                    {{ statusMap[story.status]?.label }}
                  </Badge>
                </div>
                <div class="story-stats">
                  <span>
                    <FileText class="w-3 h-3" /> {{ story.paragraphCount }}段
                  </span>
                  <span>
                    <Users class="w-3 h-3" /> {{ story.participantCount }}人
                  </span>
                  <span>
                    <Heart class="w-3 h-3" /> {{ story.totalLikes }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </TabsContent>

        <!-- 我的成就 -->
        <TabsContent value="achievements" class="tab-content" v-if="false">
          <div v-if="loading" class="loading-state">
            <div class="animate-pulse">🏆</div>
            <p>加载中...</p>
          </div>
          <div v-else class="achievements-section">
            <div class="achievements-header">
              <h3 class="achievements-title">🏆 故事成就</h3>
              <p class="achievements-desc">完成特定目标解锁成就徽章</p>
            </div>

            <div class="achievements-grid">
              <div v-for="type in allAchievementTypes" :key="type" class="achievement-item"
                :class="{ unlocked: isAchievementUnlocked(type) }">
                <StoryAchievementBadge :type="type" :unlocked="isAchievementUnlocked(type)" size="lg" />
                <div class="achievement-info">
                  <div class="achievement-name">{{ achievementDescriptions[type].name }}</div>
                  <div class="achievement-desc">{{ achievementDescriptions[type].desc }}</div>
                  <div v-if="isAchievementUnlocked(type)" class="achievement-time">
                    {{ getAchievementTime(type) }} 获得
                  </div>
                </div>
              </div>
            </div>

            <div class="achievements-stats">
              <div class="stat-item">
                <span class="stat-value">{{ myAchievements.length }}</span>
                <span class="stat-label">已解锁</span>
              </div>
              <div class="stat-item">
                <span class="stat-value">{{ allAchievementTypes.length - myAchievements.length }}</span>
                <span class="stat-label">待解锁</span>
              </div>
            </div>
          </div>
        </TabsContent>
      </Tabs>
    </div>

    <!-- 删除确认弹窗 -->
    <Dialog v-model:open="showDeleteDialog">
      <DialogContent class="sm:max-w-md delete-dialog">
        <DialogHeader>
          <DialogTitle class="flex items-center gap-2 text-red-600">
            <AlertTriangle class="w-5 h-5" /> 确认删除
          </DialogTitle>
        </DialogHeader>
        <div class="py-4">
          <p class="text-gray-600 mb-2">确定要删除故事「{{ storyToDelete?.title }}」吗？</p>
          <p class="text-sm text-red-500">此操作将删除故事及所有段落，无法恢复！</p>
        </div>
        <DialogFooter>
          <Button variant="outline" @click="showDeleteDialog = false" :disabled="deleteLoading">
            取消
          </Button>
          <Button class="bg-red-600 hover:bg-red-700" @click="confirmDelete" :disabled="deleteLoading">
            {{ deleteLoading ? '删除中...' : '确认删除' }}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>

<style scoped>
.my-story-bg {
  background: linear-gradient(180deg, #d1fae5 0%, #a7f3d0 100%);
  min-height: 100vh;
}

.tabs-list {
  display: flex;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 12px;
  padding: 4px;
  margin-bottom: 20px;
  overflow-x: auto;
}

.tab-trigger {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 14px;
  color: #065f46;
  white-space: nowrap;
  transition: all 0.2s;
}

.tab-trigger[data-state="active"] {
  background: white;
  color: rgb(33, 111, 85);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.tab-content {
  min-height: 300px;
}

/* 加载和空状态 */
.loading-state,
.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #065f46;
}

.loading-state div,
.empty-state div {
  font-size: 48px;
  margin-bottom: 16px;
}

/* 故事列表 */
.story-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.story-card {
  display: flex;
  align-items: center;
  background: white;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: all 0.2s;
}

.story-card:hover {
  transform: translateX(4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.story-main {
  flex: 1;
}

.story-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.story-title {
  font-size: 16px;
  font-weight: 600;
  color: #065f46;
  font-family: 'Noto Serif SC', serif;
}

.story-stats {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #78716c;
}

.story-stats span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.my-contribution {
  margin-top: 8px;
  font-size: 12px;
  color: rgb(33, 111, 85);
  font-weight: 500;
}

.delete-btn {
  color: #dc2626;
  opacity: 0.6;
}

.delete-btn:hover {
  opacity: 1;
  background: rgba(220, 38, 38, 0.1);
}

/* 段落列表 */
.paragraph-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.paragraph-card {
  background: white;
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.2s;
}

.paragraph-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.para-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.para-story {
  font-size: 14px;
  font-weight: 600;
  color: rgb(33, 111, 85);
}

.para-sequence {
  font-size: 12px;
  color: #a8a29e;
}

.para-content {
  font-size: 14px;
  color: #57534e;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 8px;
}

.para-footer {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #78716c;
}

.para-likes {
  display: flex;
  align-items: center;
  gap: 4px;
}

.para-time {
  margin-left: auto;
}

/* 删除弹窗 */
.delete-dialog {
  background: white;
}

/* 成就区域 */
.achievements-section {
  padding: 20px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.achievements-header {
  text-align: center;
  margin-bottom: 24px;
}

.achievements-title {
  font-size: 20px;
  font-weight: 700;
  color: #065f46;
  margin-bottom: 4px;
}

.achievements-desc {
  font-size: 14px;
  color: #78716c;
}

.achievements-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.achievement-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border-radius: 12px;
  background: #f5f5f4;
  transition: all 0.2s;
}

.achievement-item.unlocked {
  background: linear-gradient(135deg, #d1fae5 0%, #a7f3d0 100%);
  border: 1px solid rgb(33, 111, 85);
}

.achievement-info {
  flex: 1;
}

.achievement-name {
  font-size: 14px;
  font-weight: 600;
  color: #065f46;
  margin-bottom: 2px;
}

.achievement-desc {
  font-size: 12px;
  color: #78716c;
}

.achievement-time {
  font-size: 11px;
  color: rgb(33, 111, 85);
  margin-top: 4px;
}

.achievements-stats {
  display: flex;
  justify-content: center;
  gap: 32px;
  padding-top: 16px;
  border-top: 1px solid #e5e7eb;
}

.achievements-stats .stat-item {
  text-align: center;
}

.achievements-stats .stat-value {
  display: block;
  font-size: 24px;
  font-weight: 700;
  color: rgb(33, 111, 85);
}

.achievements-stats .stat-label {
  font-size: 12px;
  color: #78716c;
}

/* 响应式 */
@media (max-width: 640px) {
  .tabs-list {
    gap: 4px;
  }

  .tab-trigger {
    padding: 8px;
    font-size: 12px;
  }

  .tab-trigger svg {
    display: none;
  }

  .achievements-grid {
    grid-template-columns: 1fr;
  }
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
