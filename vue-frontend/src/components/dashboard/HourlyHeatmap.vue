<script setup>
import { computed } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, MarkPointComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { useDashboardStore } from '@/stores/dashboard'
import EmptyState from './EmptyState.vue'

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent, LegendComponent, MarkPointComponent])

const dashboardStore = useDashboardStore()
const stats = computed(() => dashboardStore.hourlyStats || {})

const hours = Array.from({ length: 24 }, (_, i) => `${i}:00`)

const hasData = computed(() => {
  const posts = stats.value.posts || []
  const comments = stats.value.comments || []
  return posts.some(v => v > 0) || comments.some(v => v > 0)
})

const chartOption = computed(() => {
  const posts = stats.value.posts || Array(24).fill(0)
  const comments = stats.value.comments || Array(24).fill(0)
  const postPeak = stats.value.postPeakHour || 0
  const commentPeak = stats.value.commentPeakHour || 0

  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['发帖量', '评论量'], bottom: 0, textStyle: { color: '#6b7280' } },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
    xAxis: { 
      type: 'category', 
      data: hours,
      axisLabel: { color: '#6b7280', fontSize: 10, interval: 3 }
    },
    yAxis: { type: 'value', axisLine: { show: false }, splitLine: { lineStyle: { color: '#f3f4f6' } } },
    series: [
      {
        name: '发帖量',
        data: posts,
        type: 'line',
        smooth: true,
        itemStyle: { color: '#3b82f6' },
        areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, 
          colorStops: [{ offset: 0, color: 'rgba(59,130,246,0.3)' }, { offset: 1, color: 'rgba(59,130,246,0.05)' }] } },
        markPoint: {
          data: [{ name: '高峰', coord: [postPeak, posts[postPeak]], itemStyle: { color: '#3b82f6' } }],
          symbol: 'pin',
          symbolSize: 30
        }
      },
      {
        name: '评论量',
        data: comments,
        type: 'line',
        smooth: true,
        itemStyle: { color: 'rgb(33,111,85)' },
        markPoint: {
          data: [{ name: '高峰', coord: [commentPeak, comments[commentPeak]], itemStyle: { color: '#22c55e' } }],
          symbol: 'pin',
          symbolSize: 30
        }
      }
    ]
  }
})
</script>

<template>
  <div class="h-full">
    <VChart v-if="hasData" :option="chartOption" autoresize class="h-full" />
    <EmptyState 
      v-else
      type="chart"
      message="暂无互动数据"
      hint="当用户发帖和评论时，24小时热力曲线将自动显示"
    />
  </div>
</template>
