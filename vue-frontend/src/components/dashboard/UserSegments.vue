<script setup>
import { computed } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart } from 'echarts/charts'
import { TooltipComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { useDashboardStore } from '@/stores/dashboard'

use([CanvasRenderer, PieChart, TooltipComponent, LegendComponent])

const dashboardStore = useDashboardStore()
const segments = computed(() => dashboardStore.userSegments || {})

const hasData = computed(() => {
  const total = (segments.value.creators || 0) + 
                (segments.value.interactors || 0) + 
                (segments.value.lurkers || 0)
  return total > 0
})

const pieOption = computed(() => {
  if (!hasData.value) {
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
    tooltip: { trigger: 'item', formatter: '{b}: {c}人 ({d}%)' },
    legend: { 
      bottom: '0%', 
      left: 'center', 
      textStyle: { color: '#6b7280', fontSize: 12 },
      itemGap: 15,
      itemWidth: 12,
      itemHeight: 12
    },
    series: [{
      type: 'pie',
      radius: ['35%', '60%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 12, fontWeight: 'bold' } },
      data: [
        { value: segments.value.creators || 0, name: '内容创作者', itemStyle: { color: '#3b82f6' } },
        { value: segments.value.interactors || 0, name: '高频互动者', itemStyle: { color: '#22c55e' } },
        { value: segments.value.lurkers || 0, name: '普通用户', itemStyle: { color: '#9ca3af' } }
      ]
    }]
  }
})
</script>

<template>
  <div class="h-full">
    <VChart :option="pieOption" autoresize class="h-full" />
  </div>
</template>
