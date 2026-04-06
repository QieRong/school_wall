<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent } from '@/components/ui/card'
import {
  Search, BookOpen, Users, Heart, Calendar,
  ArrowLeft, Library, Star, Clock, FileText
} from 'lucide-vue-next'
import { useAppStore } from '@/stores/app'
import { getArchiveList, searchStory } from '@/api/story'

const router = useRouter()
const appStore = useAppStore()

// 数据状态
const loading = ref(false)
const stories = ref([])
const searchKeyword = ref('')
const selectedCategory = ref(null)
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)

// 分类选项
const categories = [
  { value: null, label: '全部', icon: '📚' },
  { value: 1, label: '奇幻校园', icon: '🏰' },
  { value: 2, label: '悬疑推理', icon: '🔍' },
  { value: 3, label: '浪漫物语', icon: '💕' },
  { value: 4, label: '搞笑日常', icon: '😄' },
  { value: 5, label: '科幻未来', icon: '🚀' }
]

// 是否有更多
const hasMore = computed(() => stories.value.length < total.value)

// 加载档案馆列表
const loadArchive = async (append = false) => {
  loading.value = true
  try {
    const res = await getArchiveList(currentPage.value, pageSize.value, selectedCategory.value)
    if (res.code === '200') {
      if (append) {
        stories.value = [...stories.value, ...res.data.records]
      } else {
        stories.value = res.data.records || []
      }
      total.value = res.data.total || 0
    }
  } catch (e) {
    appStore.showToast('加载失败', 'error')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = async () => {
  if (!searchKeyword.value.trim()) {
    currentPage.value = 1
    loadArchive()
    return
  }

  loading.value = true
  try {
    const res = await searchStory(searchKeyword.value, selectedCategory.value, 2) // status=2 已完结
    if (res.code === '200') {
      stories.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (e) {
    appStore.showToast('搜索失败', 'error')
  } finally {
    loading.value = false
  }
}

// 切换分类
const handleCategoryChange = (category) => {
  selectedCategory.value = category
  currentPage.value = 1
  searchKeyword.value = ''
  loadArchive()
}

// 加载更多
const loadMore = () => {
  if (loading.value || !hasMore.value) return
  currentPage.value++
  loadArchive(true)
}

// 进入故事阅读
const goToStory = (storyId) => {
  router.push(`/story/${storyId}`)
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`
}

// 格式化字数
const formatWordCount = (count) => {
  if (!count) return '0'
  if (count >= 10000) return (count / 10000).toFixed(1) + '万'
  if (count >= 1000) return (count / 1000).toFixed(1) + 'k'
  return count.toString()
}

onMounted(() => {
  loadArchive()
})
</script>

<template>
  <div class="min-h-screen archive-bg">
    <!-- 顶部导航 -->
    <header class="sticky top-0 z-50 archive-header">
      <div class="container mx-auto px-4 h-14 flex items-center justify-between max-w-6xl">
        <Button variant="ghost" class="text-amber-100 hover:bg-amber-800/50" @click="router.back()">
          <ArrowLeft class="w-5 h-5 mr-2" /> 返回大厅
        </Button>
        <h1 class="text-lg font-bold text-amber-100 font-serif flex items-center gap-2">
          <Library class="w-5 h-5" /> 故事档案馆
        </h1>
        <div class="w-24"></div>
      </div>
    </header>

    <div class="container mx-auto px-4 py-6 max-w-6xl">
      <!-- 搜索和筛选 -->
      <div class="search-section mb-8">
        <div class="search-box">
          <Search class="search-icon" />
          <Input v-model="searchKeyword" placeholder="搜索故事标题..." class="search-input" @keyup.enter="handleSearch" />
          <Button class="search-btn" @click="handleSearch">搜索</Button>
        </div>

        <!-- 分类筛选 -->
        <div class="category-filter">
          <button v-for="cat in categories" :key="cat.value" class="category-btn"
            :class="{ active: selectedCategory === cat.value }" @click="handleCategoryChange(cat.value)">
            <span class="cat-icon">{{ cat.icon }}</span>
            <span class="cat-label">{{ cat.label }}</span>
          </button>
        </div>
      </div>

      <!-- 统计信息 -->
      <div class="stats-bar mb-6">
        <div class="stat-item">
          <BookOpen class="w-4 h-4" />
          <span>共 {{ total }} 部完结故事</span>
        </div>
      </div>

      <!-- 加载中 -->
      <div v-if="loading && stories.length === 0" class="loading-state">
        <div class="text-5xl mb-4 animate-pulse">📚</div>
        <p>正在翻阅档案...</p>
      </div>

      <!-- 空状态 -->
      <div v-else-if="stories.length === 0" class="empty-state">
        <div class="text-5xl mb-4">📖</div>
        <p>档案馆暂无故事</p>
        <p class="text-sm mt-2">完结的故事将在这里永久保存</p>
      </div>

      <!-- 故事书架 -->
      <div v-else class="bookshelf">
        <div v-for="story in stories" :key="story.id" class="book-card" @click="goToStory(story.id)">
          <!-- 书脊装饰 -->
          <div class="book-spine">
            <span class="spine-category">{{ story.categoryName }}</span>
          </div>

          <!-- 书籍内容 -->
          <div class="book-content">
            <!-- 推荐标识 -->
            <div v-if="story.isRecommended" class="recommend-badge">
              <Star class="w-3 h-3" /> 推荐
            </div>

            <h3 class="book-title">{{ story.title }}</h3>

            <p class="book-preview">{{ story.firstParagraphPreview }}</p>

            <div class="book-stats">
              <span class="stat">
                <FileText class="w-3 h-3" /> {{ story.paragraphCount }}段
              </span>
              <span class="stat">
                <Users class="w-3 h-3" /> {{ story.participantCount }}人
              </span>
              <span class="stat">
                <Heart class="w-3 h-3" /> {{ story.totalLikes }}
              </span>
            </div>

            <div class="book-footer">
              <span class="finish-date">
                <Calendar class="w-3 h-3" />
                {{ formatDate(story.finishTime) }} 完结
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 加载更多 -->
      <div v-if="hasMore && stories.length > 0" class="load-more">
        <Button variant="outline" class="load-more-btn" :disabled="loading" @click="loadMore">
          {{ loading ? '加载中...' : '加载更多' }}
        </Button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.archive-bg {
  background: linear-gradient(180deg, #292524 0%, #44403c 50%, #57534e 100%);
  min-height: 100vh;
}

.archive-header {
  background: rgba(41, 37, 36, 0.95);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid rgba(217, 119, 6, 0.3);
}

/* 搜索区域 */
.search-section {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 16px;
  padding: 20px;
  border: 1px solid rgba(217, 119, 6, 0.2);
}

.search-box {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  position: relative;
}

.search-icon {
  position: absolute;
  left: 16px;
  width: 20px;
  height: 20px;
  color: #a8a29e;
}

.search-input {
  flex: 1;
  padding-left: 48px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(217, 119, 6, 0.3);
  color: #fef3c7;
  border-radius: 8px;
}

.search-input::placeholder {
  color: #a8a29e;
}

.search-btn {
  background: linear-gradient(135deg, #d97706 0%, #b45309 100%);
  color: white;
  border: none;
}

/* 分类筛选 */
.category-filter {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.category-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(217, 119, 6, 0.2);
  color: #d6d3d1;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.category-btn:hover {
  background: rgba(217, 119, 6, 0.1);
  border-color: rgba(217, 119, 6, 0.4);
}

.category-btn.active {
  background: rgba(217, 119, 6, 0.2);
  border-color: #d97706;
  color: #fbbf24;
}

.cat-icon {
  font-size: 16px;
}

/* 统计栏 */
.stats-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  color: #a8a29e;
  font-size: 14px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 加载和空状态 */
.loading-state,
.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #a8a29e;
}

/* 书架布局 */
.bookshelf {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

/* 书籍卡片 */
.book-card {
  display: flex;
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  border-radius: 4px 12px 12px 4px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow:
    4px 4px 12px rgba(0, 0, 0, 0.3),
    -2px 0 8px rgba(0, 0, 0, 0.1);
}

.book-card:hover {
  transform: translateY(-4px) rotate(-1deg);
  box-shadow:
    6px 8px 20px rgba(0, 0, 0, 0.4),
    -2px 0 8px rgba(0, 0, 0, 0.1);
}

/* 书脊 */
.book-spine {
  width: 24px;
  background: linear-gradient(180deg, #b45309 0%, #92400e 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  writing-mode: vertical-rl;
  text-orientation: mixed;
  padding: 8px 0;
}

.spine-category {
  font-size: 10px;
  color: #fef3c7;
  letter-spacing: 2px;
}

/* 书籍内容 */
.book-content {
  flex: 1;
  padding: 16px;
  position: relative;
}

.recommend-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%);
  color: white;
  font-size: 11px;
  border-radius: 4px;
}

.book-title {
  font-size: 16px;
  font-weight: 700;
  color: #92400e;
  margin-bottom: 8px;
  font-family: 'Noto Serif SC', serif;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.book-preview {
  font-size: 13px;
  color: #78716c;
  line-height: 1.6;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.book-stats {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.book-stats .stat {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #92400e;
}

.book-footer {
  border-top: 1px dashed rgba(146, 64, 14, 0.2);
  padding-top: 8px;
}

.finish-date {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: #a8a29e;
}

/* 加载更多 */
.load-more {
  text-align: center;
  padding: 32px 0;
}

.load-more-btn {
  border-color: rgba(217, 119, 6, 0.5);
  color: #fbbf24;
}

.load-more-btn:hover {
  background: rgba(217, 119, 6, 0.1);
}

/* 响应式 */
@media (max-width: 640px) {
  .bookshelf {
    grid-template-columns: 1fr;
  }

  .category-filter {
    overflow-x: auto;
    flex-wrap: nowrap;
    padding-bottom: 8px;
  }

  .category-btn {
    flex-shrink: 0;
  }
}
</style>
