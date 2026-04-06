<script setup>
import { computed } from 'vue'
import { useDashboardStore } from '@/stores/dashboard'
import { TrendingUp, TrendingDown, Minus, FileText, Users, MessageCircle } from 'lucide-vue-next'

const dashboardStore = useDashboardStore()
const comparison = computed(() => dashboardStore.weeklyComparison || {})

const getChangeStyle = (percent) => {
  if (percent > 0) return { color: 'text-green-600', bg: 'bg-green-50', icon: TrendingUp }
  if (percent < 0) return { color: 'text-red-600', bg: 'bg-red-50', icon: TrendingDown }
  return { color: 'text-gray-500', bg: 'bg-gray-50', icon: Minus }
}

const formatPercent = (percent) => {
  if (!percent) return '0%'
  const sign = percent > 0 ? '+' : ''
  return `${sign}${percent.toFixed(1)}%`
}

const hasData = computed(() => {
  return comparison.value && 
         (comparison.value.posts || comparison.value.dau || comparison.value.interactions)
})

const cards = computed(() => [
  {
    title: '发帖量',
    icon: FileText,
    iconColor: 'text-blue-600',
    iconBg: 'bg-blue-100',
    data: comparison.value.posts || {}
  },
  {
    title: '活跃用户',
    icon: Users,
    iconColor: 'text-[rgb(33,111,85)]',
    iconBg: 'bg-[rgb(33,111,85)]/10',
    data: comparison.value.dau || {}
  },
  {
    title: '互动量',
    icon: MessageCircle,
    iconColor: 'text-purple-600',
    iconBg: 'bg-purple-100',
    data: comparison.value.interactions || {}
  }
])
</script>

<template>
  <div v-if="hasData" class="grid grid-cols-3 gap-4">
    <div v-for="card in cards" :key="card.title" 
      class="p-4 bg-white rounded-xl border hover:shadow-md transition">
      <div class="flex items-center gap-3 mb-3">
        <div :class="['w-10 h-10 rounded-lg flex items-center justify-center', card.iconBg]">
          <component :is="card.icon" :class="['w-5 h-5', card.iconColor]" />
        </div>
        <div class="text-sm text-gray-500">{{ card.title }}</div>
      </div>
      <div class="flex items-end justify-between">
        <div>
          <div class="text-2xl font-bold text-gray-800">{{ card.data.current || 0 }}</div>
          <div class="text-xs text-gray-400">上周: {{ card.data.previous || 0 }}</div>
        </div>
        <div :class="['flex items-center gap-1 px-2 py-1 rounded-full text-sm font-medium', 
          getChangeStyle(card.data.changePercent).bg, getChangeStyle(card.data.changePercent).color]">
          <component :is="getChangeStyle(card.data.changePercent).icon" class="w-4 h-4" />
          {{ formatPercent(card.data.changePercent) }}
        </div>
      </div>
    </div>
  </div>
  <div v-else class="p-8 bg-gray-50 rounded-xl border border-dashed border-gray-300 text-center">
    <TrendingUp class="w-12 h-12 mx-auto text-gray-300 mb-3" />
    <div class="text-sm font-medium text-gray-500">暂无对比数据</div>
    <div class="text-xs text-gray-400 mt-1">等待系统统计周数据...</div>
  </div>
</template>
