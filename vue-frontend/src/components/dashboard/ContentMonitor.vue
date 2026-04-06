<script setup>
import { computed } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { useDashboardStore } from '@/stores/dashboard'
import { FileCheck, Clock, CheckCircle } from 'lucide-vue-next'

use([CanvasRenderer, BarChart, GridComponent, TooltipComponent])

const dashboardStore = useDashboardStore()
const stats = computed(() => dashboardStore.contentStats || {})

const hasData = computed(() => {
  const words = stats.value.sensitiveWords || []
  return words.length > 0
})

const sensitiveOption = computed(() => {
  const words = stats.value.sensitiveWords || []
  
  if (words.length === 0) {
    return {
      title: {
        text: '暂无敏感词触发',
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
      data: words.map(w => w.word),
      axisLabel: { color: '#6b7280', fontSize: 10, rotate: 30 }
    },
    yAxis: { type: 'value', axisLine: { show: false }, splitLine: { lineStyle: { color: '#f3f4f6' } } },
    series: [{
      data: words.map(w => w.count),
      type: 'bar',
      barWidth: '60%',
      itemStyle: { color: '#ef4444', borderRadius: [4, 4, 0, 0] }
    }]
  }
})
</script>

<template>
  <div class="space-y-4">
    <div class="grid grid-cols-3 gap-3">
      <div class="text-center p-3 bg-yellow-50 rounded-lg">
        <Clock class="w-5 h-5 mx-auto text-yellow-600" />
        <div class="text-xl font-bold text-gray-800 mt-1">{{ stats.pendingAudit || 0 }}</div>
        <div class="text-xs text-gray-500">待审核</div>
      </div>
      <div class="text-center p-3 bg-blue-50 rounded-lg">
        <FileCheck class="w-5 h-5 mx-auto text-blue-600" />
        <div class="text-xl font-bold text-gray-800 mt-1">{{ stats.todayAudited || 0 }}</div>
        <div class="text-xs text-gray-500">今日已审</div>
      </div>
      <div class="text-center p-3 bg-green-50 rounded-lg">
        <CheckCircle class="w-5 h-5 mx-auto text-green-600" />
        <div class="text-xl font-bold text-gray-800 mt-1">{{ stats.passRate?.toFixed(1) || 0 }}%</div>
        <div class="text-xs text-gray-500">通过率</div>
      </div>
    </div>
    <div>
      <div class="text-sm font-medium text-gray-700 mb-2">敏感词触发统计</div>
      <VChart :option="sensitiveOption" autoresize class="h-[150px]" />
    </div>
  </div>
</template>
