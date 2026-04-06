<script setup>
import { computed } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { useDashboardStore } from '@/stores/dashboard'
import { Users, UserPlus, Activity } from 'lucide-vue-next'
import EmptyState from './EmptyState.vue'

use([CanvasRenderer, LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent])

const dashboardStore = useDashboardStore()
const stats = computed(() => dashboardStore.userStats || {})

// 检查是否有任何数据
const hasAnyData = computed(() => {
  return stats.value && (
    (stats.value.total && stats.value.total > 0) ||
    (stats.value.todayNew && stats.value.todayNew > 0) ||
    (stats.value.todayActive && stats.value.todayActive > 0)
  )
})

const hasTrendData = computed(() => {
  const trend = stats.value.trend || []
  return trend.length > 0 && trend.some(t => t.value > 0)
})

const hasCreditData = computed(() => {
  const dist = stats.value.creditDistribution || {}
  return Object.keys(dist).length > 0
})

// 7天趋势图
const trendOption = computed(() => {
  const trend = stats.value.trend || []
  
  if (!hasTrendData.value) {
    return {
      title: {
        text: '暂无趋势数据',
        left: 'center',
        top: 'center',
        textStyle: { color: '#9ca3af', fontSize: 12 }
      }
    }
  }
  
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
    xAxis: { 
      type: 'category', 
      data: trend.map(t => t.date?.slice(5) || ''),
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisLabel: { color: '#6b7280', fontSize: 10 }
    },
    yAxis: { 
      type: 'value',
      axisLine: { show: false },
      splitLine: { lineStyle: { color: '#f3f4f6' } }
    },
    series: [{
      data: trend.map(t => t.value || 0),
      type: 'line',
      smooth: true,
      areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, 
        colorStops: [{ offset: 0, color: 'rgba(33,111,85,0.3)' }, { offset: 1, color: 'rgba(33,111,85,0.05)' }] } },
      itemStyle: { color: 'rgb(33,111,85)' },
      lineStyle: { width: 2, color: 'rgb(33,111,85)' }
    }]
  }
})

// 信誉分分布饼图
const creditOption = computed(() => {
  const dist = stats.value.creditDistribution || {}
  const data = Object.entries(dist).map(([name, value]) => ({ name, value }))
  const colors = ['#ef4444', '#f97316', '#eab308', '#22c55e', '#10b981']
  
  if (!hasCreditData.value) {
    return {
      title: {
        text: '暂无信誉分数据',
        left: 'center',
        top: 'center',
        textStyle: { color: '#9ca3af', fontSize: 12 }
      }
    }
  }
  
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c}人 ({d}%)' },
    legend: { bottom: '0', left: 'center', textStyle: { color: '#6b7280', fontSize: 10 } },
    series: [{
      type: 'pie',
      radius: ['35%', '60%'],
      center: ['50%', '40%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      data: data.map((d, i) => ({ ...d, itemStyle: { color: colors[i] } }))
    }]
  }
})
</script>

<template>
  <!-- 空状态 -->
  <EmptyState 
    v-if="!hasAnyData" 
    type="users"
    message="暂无用户数据"
    hint="等待用户注册和活动，数据将自动显示"
  />
  
  <!-- 有数据时显示 -->
  <div v-else class="space-y-4">
    <!-- 统计数字 -->
    <div class="grid grid-cols-3 gap-3">
      <div class="text-center p-3 bg-[rgb(33,111,85)]/10 rounded-lg">
        <Users class="w-5 h-5 mx-auto text-[rgb(33,111,85)]" />
        <div class="text-xl font-bold text-gray-800 mt-1">{{ stats.total || 0 }}</div>
        <div class="text-xs text-gray-500">总用户</div>
      </div>
      <div class="text-center p-3 bg-blue-50 rounded-lg">
        <UserPlus class="w-5 h-5 mx-auto text-blue-600" />
        <div class="text-xl font-bold text-gray-800 mt-1">{{ stats.todayNew || 0 }}</div>
        <div class="text-xs text-gray-500">今日新增</div>
      </div>
      <div class="text-center p-3 bg-purple-50 rounded-lg">
        <Activity class="w-5 h-5 mx-auto text-purple-600" />
        <div class="text-xl font-bold text-gray-800 mt-1">{{ stats.todayActive || 0 }}</div>
        <div class="text-xs text-gray-500">今日活跃</div>
      </div>
    </div>
    
    <!-- 7天趋势 -->
    <div>
      <div class="text-sm font-medium text-gray-700 mb-2">7天用户增长</div>
      <VChart :option="trendOption" autoresize class="h-[120px]" />
    </div>
    
    <!-- 信誉分分布 -->
    <div>
      <div class="text-sm font-medium text-gray-700 mb-2">信誉分分布</div>
      <VChart :option="creditOption" autoresize class="h-[150px]" />
    </div>
  </div>
</template>
