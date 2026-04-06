<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useDashboardStore } from '@/stores/dashboard'
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs'
import { Button } from '@/components/ui/button'
import { ThumbsUp, Flag, MessageCircle, Crown, Medal, Award, ArrowRight } from 'lucide-vue-next'

const router = useRouter()
const dashboardStore = useDashboardStore()
const activeTab = ref('likes')

// 显示前10名
const rankings = computed(() => {
  const allRankings = dashboardStore.userRankings || []
  return allRankings.slice(0, 10)
})

const hasMore = computed(() => {
  const allRankings = dashboardStore.userRankings || []
  return allRankings.length > 10
})

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
  if (index === 0) return 'bg-yellow-50 border-yellow-200'
  if (index === 1) return 'bg-gray-50 border-gray-200'
  if (index === 2) return 'bg-amber-50 border-amber-200'
  return 'bg-white border-gray-100'
}

const viewFullRankings = () => {
  // 跳转到完整排行榜页面,携带当前标签类型
  router.push(`/admin/rankings?type=${activeTab.value}`)
}
</script>

<template>
  <div class="h-full flex flex-col">
    <Tabs v-model="activeTab" class="h-full flex flex-col">
      <TabsList class="grid grid-cols-3 mb-3 flex-shrink-0">
        <TabsTrigger value="likes" class="text-xs">
          <div class="flex items-center justify-center gap-1">
            <ThumbsUp class="w-3 h-3" />
            <span>校园达人</span>
          </div>
        </TabsTrigger>
        <TabsTrigger value="reports" class="text-xs">
          <div class="flex items-center justify-center gap-1">
            <Flag class="w-3 h-3" />
            <span>热心市民</span>
          </div>
        </TabsTrigger>
        <TabsTrigger value="comments" class="text-xs">
          <div class="flex items-center justify-center gap-1">
            <MessageCircle class="w-3 h-3" />
            <span>活跃评论</span>
          </div>
        </TabsTrigger>
      </TabsList>

      <div class="flex-1 flex flex-col min-h-0">
        <div class="flex-1 space-y-2 overflow-y-auto">
          <div v-for="(user, index) in rankings" :key="user.userId" :class="[
            'flex items-center gap-3 p-2.5 rounded-lg border transition-all duration-200',
            getRankBgColor(index),
            'hover:shadow-md hover:scale-[1.02]'
          ]">
            <div class="w-7 text-center flex-shrink-0">
              <component v-if="getRankIcon(index)" :is="getRankIcon(index)"
                :class="['w-5 h-5 mx-auto', getRankColor(index)]" />
              <span v-else class="text-sm font-medium text-gray-500">{{ index + 1 }}</span>
            </div>
            <img :src="user.avatar || '/default.png'"
              class="w-9 h-9 rounded-full border-2 border-white shadow-sm flex-shrink-0" />
            <div class="flex-1 min-w-0">
              <div class="text-sm font-medium text-gray-800 truncate">
                {{ user.nickname || '匿名用户' }}
              </div>
              <div class="text-xs text-gray-500">
                {{ activeTab === 'likes' ? '获赞' : activeTab === 'reports' ? '举报' : '评论' }}
              </div>
            </div>
            <div class="flex flex-col items-end flex-shrink-0">
              <div class="text-base font-bold text-[rgb(33,111,85)]">{{ user.value }}</div>
              <div class="text-xs text-gray-400">
                {{ activeTab === 'likes' ? '次' : activeTab === 'reports' ? '条' : '条' }}
              </div>
            </div>
          </div>

          <div v-if="rankings.length === 0" class="text-center py-8 text-gray-400 text-sm">
            <MessageCircle class="w-12 h-12 mx-auto mb-2 opacity-30" />
            <div>暂无数据</div>
          </div>
        </div>

        <!-- 查看更多按钮 -->
        <div v-if="hasMore" class="mt-3 pt-3 border-t flex-shrink-0">
          <Button @click="viewFullRankings" variant="ghost"
            class="w-full text-[rgb(33,111,85)] hover:bg-[rgb(33,111,85)]/10 text-xs h-8 group">
            <span>查看完整排行榜</span>
            <ArrowRight class="w-3.5 h-3.5 ml-1 group-hover:translate-x-1 transition-transform" />
          </Button>
        </div>
      </div>
    </Tabs>
  </div>
</template>
