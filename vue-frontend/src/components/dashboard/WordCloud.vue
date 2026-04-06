<script setup>
import { computed, onMounted } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'
import { useDashboardStore } from '@/stores/dashboard'
import EmptyState from './EmptyState.vue'
import 'echarts-wordcloud'

use([CanvasRenderer])

const dashboardStore = useDashboardStore()
const words = computed(() => dashboardStore.wordCloud || [])
const hasData = computed(() => words.value && words.value.length > 0)

const chartOption = computed(() => {
  const data = words.value.map(w => ({
    name: w.word,
    value: w.count,
    textStyle: {
      color: `hsl(${Math.random() * 360}, 70%, 50%)`
    }
  }))

  return {
    tooltip: { show: true, formatter: '{b}: {c}次' },
    series: [{
      type: 'wordCloud',
      shape: 'circle',
      left: 'center',
      top: 'center',
      width: '90%',
      height: '90%',
      sizeRange: [12, 40],
      rotationRange: [-45, 45],
      rotationStep: 15,
      gridSize: 8,
      drawOutOfBound: false,
      textStyle: {
        fontFamily: 'sans-serif',
        fontWeight: 'bold'
      },
      emphasis: {
        textStyle: {
          shadowBlur: 10,
          shadowColor: '#333'
        }
      },
      data
    }]
  }
})

onMounted(() => {
  dashboardStore.fetchWordCloud(7)
})
</script>

<template>
  <div class="h-full">
    <VChart v-if="hasData" :option="chartOption" autoresize class="h-full" />
    <EmptyState 
      v-else
      type="content"
      message="暂无热门话题"
      hint="当用户发布内容时，热门话题词云将自动生成"
    />
  </div>
</template>
