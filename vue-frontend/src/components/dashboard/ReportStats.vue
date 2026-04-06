<script setup>
import { computed } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { useDashboardStore } from '@/stores/dashboard'
import { useRouter } from 'vue-router'
import { Flag, AlertCircle, CheckCircle2 } from 'lucide-vue-next'

use([CanvasRenderer, BarChart, GridComponent, TooltipComponent])

const router = useRouter()
const dashboardStore = useDashboardStore()
const stats = computed(() => dashboardStore.reportStats || {})

const hasData = computed(() => {
  const dist = stats.value.distribution || {}
  return Object.keys(dist).length > 0
})

const distributionOption = computed(() => {
  const dist = stats.value.distribution || {}
  const entries = Object.entries(dist)
  
  if (entries.length === 0) {
    return {
      title: {
        text: '暂无数据',
        left: 'center',
        top: 'center',
        textStyle: { color: '#9ca3af', fontSize: 14 }
      }
    }
  }
  
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
    xAxis: { 
      type: 'category', 
      data: entries.map(([k]) => k),
      axisLabel: { color: '#6b7280', fontSize: 10 }
    },
    yAxis: { type: 'value', axisLine: { show: false }, splitLine: { lineStyle: { color: '#f3f4f6' } } },
    series: [{
      data: entries.map(([, v]) => v),
      type: 'bar',
      barWidth: '50%',
      itemStyle: { color: '#f97316', borderRadius: [4, 4, 0, 0] }
    }]
  }
})

const goToReports = () => router.push('/admin/report')
</script>

<template>
  <div class="space-y-4">
    <div class="grid grid-cols-3 gap-3">
      <div class="text-center p-3 bg-red-50 rounded-lg">
        <Flag class="w-5 h-5 mx-auto text-red-600" />
        <div class="text-xl font-bold text-gray-800 mt-1">{{ stats.pending || 0 }}</div>
        <div class="text-xs text-gray-500">待处理</div>
      </div>
      <div class="text-center p-3 bg-orange-50 rounded-lg">
        <AlertCircle class="w-5 h-5 mx-auto text-orange-600" />
        <div class="text-xl font-bold text-gray-800 mt-1">{{ stats.todayNew || 0 }}</div>
        <div class="text-xs text-gray-500">今日新增</div>
      </div>
      <div class="text-center p-3 bg-green-50 rounded-lg cursor-pointer hover:bg-green-100" @click="goToReports">
        <CheckCircle2 class="w-5 h-5 mx-auto text-green-600" />
        <div class="text-xl font-bold text-gray-800 mt-1">{{ stats.handleRate?.toFixed(1) || 0 }}%</div>
        <div class="text-xs text-gray-500">处理率</div>
      </div>
    </div>
    <div>
      <div class="text-sm font-medium text-gray-700 mb-2">举报类型分布</div>
      <VChart :option="distributionOption" autoresize class="h-[150px]" />
    </div>
  </div>
</template>
