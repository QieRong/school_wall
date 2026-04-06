<script setup>
import { computed } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { GaugeChart } from 'echarts/charts'
import { TooltipComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { useDashboardStore } from '@/stores/dashboard'
import { Activity } from 'lucide-vue-next'

use([CanvasRenderer, GaugeChart, TooltipComponent])

const dashboardStore = useDashboardStore()

const colorMap = { red: '#ef4444', yellow: '#f59e0b', green: '#22c55e' }

const hasData = computed(() => {
  return dashboardStore.healthScore &&
    typeof dashboardStore.healthScore.score === 'number'
})

const gaugeOption = computed(() => {
  const score = dashboardStore.healthScore?.score || 0
  const status = dashboardStore.healthScore?.status || 'green'

  return {
    series: [{
      type: 'gauge',
      startAngle: 180,
      endAngle: 0,
      min: 0,
      max: 100,
      splitNumber: 5,
      itemStyle: { color: colorMap[status] },
      progress: { show: true, width: 18 },
      pointer: { show: false },
      axisLine: {
        lineStyle: {
          width: 18,
          color: [[0.6, '#ef4444'], [0.8, '#f59e0b'], [1, '#22c55e']]
        }
      },
      axisTick: { show: false },
      splitLine: { show: false },
      axisLabel: { show: false },
      title: { offsetCenter: [0, '-40%'], fontSize: 12, color: '#6b7280' },
      detail: {
        fontSize: 28,
        offsetCenter: [0, '0%'],
        valueAnimation: true,
        color: colorMap[status],
        formatter: '{value}分'
      },
      data: [{ value: score, name: '平台健康度' }]
    }]
  }
})

const statusText = computed(() => {
  const status = dashboardStore.healthScore?.status
  const map = { red: '警告', yellow: '注意', green: '正常' }
  return map[status] || '正常'
})

const metrics = computed(() => dashboardStore.healthScore || {})
</script>

<template>
  <div class="h-full flex flex-col">
    <div v-if="hasData" class="flex-1 flex flex-col">
      <!-- 仪表盘 - 缩小高度 -->
      <VChart :option="gaugeOption" autoresize class="h-[160px]" />

      <!-- 指标数据 - 放在仪表盘下方的空白区域 -->
      <div class="grid grid-cols-2 gap-x-4 gap-y-3 text-gray-600 px-4 mt-2">
        <div class="flex justify-between items-center">
          <span class="text-xs">日活率</span>
          <span class="font-medium text-xs">{{ metrics.dauRate?.toFixed(1) || 0 }}%</span>
        </div>
        <div class="flex justify-between items-center">
          <span class="text-xs">发帖率</span>
          <span class="font-medium text-xs">{{ metrics.postRate?.toFixed(1) || 0 }}%</span>
        </div>
        <div class="flex justify-between items-center">
          <span class="text-xs">互动率</span>
          <span class="font-medium text-xs">{{ metrics.interactionRate?.toFixed(1) || 0 }}%</span>
        </div>
        <div class="flex justify-between items-center">
          <span class="text-xs">审核通过率</span>
          <span class="font-medium text-xs">{{ metrics.auditPassRate?.toFixed(1) || 0 }}%</span>
        </div>
      </div>
    </div>
    <div v-else class="flex-1 flex flex-col items-center justify-center text-gray-400">
      <Activity class="w-12 h-12 mb-3 opacity-30" />
      <div class="text-sm font-medium">暂无数据</div>
      <div class="text-xs mt-1 text-gray-300">等待系统统计...</div>
    </div>
  </div>
</template>
