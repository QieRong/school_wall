<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useDashboardStore } from '@/stores/dashboard'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Badge } from '@/components/ui/badge'
import { ThumbsUp, Flag, MessageCircle, Crown, Medal, Award, TrendingUp } from 'lucide-vue-next'

const route = useRoute()
const dashboardStore = useDashboardStore()
const activeTab = ref(route.query.type || 'likes')

const rankings = computed(() => dashboardStore.userRankings || [])

watch(activeTab, (type) => {
  dashboardStore.fetchUserRankings(type)
}, { immediate: true })

const getRankIcon = (index) => {
  if (index === 0) return Crown
  if (index === 1) return Medal
  if (index === 2) return Award
  return null
}

const getRankColor = (index) => {
  if (index === 0) return 'text-yellow-500'
  if (index === 1) return 'text-gray-400'
  if (index === 2) return 'text-amber-600'
  return 'text-gray-300'
}

const getRankBgColor = (index) => {
  if (index === 0) return 'bg-gradient-to-r from-yellow-50 to-amber-50 border-yellow-300'
  if (index === 1) return 'bg-gradient-to-r from-gray-50 to-slate-50 border-gray-300'
  if (index === 2) return 'bg-gradient-to-r from-amber-50 to-orange-50 border-amber-300'
  return 'bg-white border-gray-200'
}

const getTabTitle = computed(() => {
  const titles = {
    likes: '校园达人榜',
    reports: '热心市民榜',
    comments: '活跃评论榜'
  }
  return titles[activeTab.value] || '用户排行榜'
})

const getTabDescription = computed(() => {
  const descriptions = {
    likes: '根据获赞数量排名，展示最受欢迎的用户',
    reports: '根据举报贡献排名，感谢维护社区环境',
    comments: '根据评论数量排名，展示最活跃的用户'
  }
  return descriptions[activeTab.value] || ''
})
</script>

<template>
  <div class="space-y-4">
    <!-- 页面标题 -->
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-xl font-bold text-gray-800 flex items-center gap-2">
          <TrendingUp class="w-5 h-5 text-[rgb(33,111,85)]" />
          {{ getTabTitle }}
        </h2>
        <p class="text-sm text-gray-500 mt-1">{{ getTabDescription }}</p>
      </div>
      <Badge variant="outline" class="text-xs">
        共 {{ rankings.length }} 位用户
      </Badge>
    </div>

    <!-- 排行榜卡片 -->
    <Card>
      <CardHeader class="pb-3">
        <Tabs v-model="activeTab">
          <TabsList class="grid grid-cols-3">
            <TabsTrigger value="likes">
              <div class="flex items-center justify-center gap-2">
                <ThumbsUp class="w-4 h-4" />
                <span>校园达人</span>
              </div>
            </TabsTrigger>
            <TabsTrigger value="reports">
              <div class="flex items-center justify-center gap-2">
                <Flag class="w-4 h-4" />
                <span>热心市民</span>
              </div>
            </TabsTrigger>
            <TabsTrigger value="comments">
              <div class="flex items-center justify-center gap-2">
                <MessageCircle class="w-4 h-4" />
                <span>活跃评论</span>
              </div>
            </TabsTrigger>
          </TabsList>
        </Tabs>
      </CardHeader>

      <CardContent>
        <!-- 前三名特殊展示 -->
        <div v-if="rankings.length >= 3" class="grid grid-cols-3 gap-4 mb-6 pb-6 border-b">
          <!-- 第二名 -->
          <div class="flex flex-col items-center">
            <div class="relative">
              <img :src="rankings[1]?.avatar || '/default.png'" 
                class="w-20 h-20 rounded-full border-4 border-gray-300 shadow-lg" />
              <div class="absolute -bottom-2 left-1/2 -translate-x-1/2 bg-gray-400 text-white rounded-full w-8 h-8 flex items-center justify-center shadow-md">
                <Medal class="w-5 h-5" />
              </div>
            </div>
            <div class="mt-4 text-center">
              <div class="font-bold text-gray-800">{{ rankings[1]?.nickname }}</div>
              <div class="text-2xl font-bold text-gray-500 mt-1">{{ rankings[1]?.value }}</div>
              <div class="text-xs text-gray-400">
                {{ activeTab === 'likes' ? '获赞' : activeTab === 'reports' ? '举报' : '评论' }}
              </div>
            </div>
          </div>

          <!-- 第一名 -->
          <div class="flex flex-col items-center -mt-4">
            <div class="relative">
              <img :src="rankings[0]?.avatar || '/default.png'" 
                class="w-24 h-24 rounded-full border-4 border-yellow-400 shadow-xl" />
              <div class="absolute -bottom-2 left-1/2 -translate-x-1/2 bg-yellow-500 text-white rounded-full w-10 h-10 flex items-center justify-center shadow-lg">
                <Crown class="w-6 h-6" />
              </div>
            </div>
            <div class="mt-4 text-center">
              <div class="font-bold text-gray-800 text-lg">{{ rankings[0]?.nickname }}</div>
              <div class="text-3xl font-bold text-yellow-600 mt-1">{{ rankings[0]?.value }}</div>
              <div class="text-xs text-gray-400">
                {{ activeTab === 'likes' ? '获赞' : activeTab === 'reports' ? '举报' : '评论' }}
              </div>
            </div>
          </div>

          <!-- 第三名 -->
          <div class="flex flex-col items-center">
            <div class="relative">
              <img :src="rankings[2]?.avatar || '/default.png'" 
                class="w-20 h-20 rounded-full border-4 border-amber-400 shadow-lg" />
              <div class="absolute -bottom-2 left-1/2 -translate-x-1/2 bg-amber-600 text-white rounded-full w-8 h-8 flex items-center justify-center shadow-md">
                <Award class="w-5 h-5" />
              </div>
            </div>
            <div class="mt-4 text-center">
              <div class="font-bold text-gray-800">{{ rankings[2]?.nickname }}</div>
              <div class="text-2xl font-bold text-amber-600 mt-1">{{ rankings[2]?.value }}</div>
              <div class="text-xs text-gray-400">
                {{ activeTab === 'likes' ? '获赞' : activeTab === 'reports' ? '举报' : '评论' }}
              </div>
            </div>
          </div>
        </div>

        <!-- 完整排行榜列表 -->
        <div class="space-y-2">
          <div v-for="(user, index) in rankings" :key="user.userId"
            :class="[
              'flex items-center gap-4 p-3 rounded-lg border-2 transition-all duration-200',
              getRankBgColor(index),
              'hover:shadow-md hover:scale-[1.01]'
            ]">
            <div class="w-10 text-center flex-shrink-0">
              <component v-if="getRankIcon(index)" :is="getRankIcon(index)" 
                :class="['w-6 h-6 mx-auto', getRankColor(index)]" />
              <span v-else class="text-lg font-bold text-gray-500">{{ index + 1 }}</span>
            </div>
            <img :src="user.avatar || '/default.png'" 
              class="w-12 h-12 rounded-full border-2 border-white shadow-sm flex-shrink-0" />
            <div class="flex-1 min-w-0">
              <div class="text-base font-medium text-gray-800 truncate">
                {{ user.nickname || '匿名用户' }}
              </div>
              <div class="text-sm text-gray-500">
                {{ activeTab === 'likes' ? '获赞数' : activeTab === 'reports' ? '举报数' : '评论数' }}
              </div>
            </div>
            <div class="flex flex-col items-end flex-shrink-0">
              <div class="text-xl font-bold text-[rgb(33,111,85)]">{{ user.value }}</div>
              <div class="text-xs text-gray-400">
                {{ activeTab === 'likes' ? '次' : activeTab === 'reports' ? '条' : '条' }}
              </div>
            </div>
          </div>

          <div v-if="rankings.length === 0" class="text-center py-16 text-gray-400">
            <MessageCircle class="w-16 h-16 mx-auto mb-4 opacity-30" />
            <div class="text-lg font-medium">暂无数据</div>
            <div class="text-sm mt-2">还没有用户上榜哦</div>
          </div>
        </div>
      </CardContent>
    </Card>
  </div>
</template>
