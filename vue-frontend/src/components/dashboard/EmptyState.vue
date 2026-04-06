<script setup>
import { Database, TrendingUp, Users, FileText, AlertCircle, BarChart3 } from 'lucide-vue-next'

const props = defineProps({
  type: {
    type: String,
    default: 'default',
    validator: (value) => ['default', 'chart', 'users', 'content', 'alert', 'stats'].includes(value)
  },
  message: {
    type: String,
    default: ''
  },
  hint: {
    type: String,
    default: ''
  }
})

const iconMap = {
  default: Database,
  chart: TrendingUp,
  users: Users,
  content: FileText,
  alert: AlertCircle,
  stats: BarChart3
}

const defaultMessages = {
  default: '暂无数据',
  chart: '暂无图表数据',
  users: '暂无用户数据',
  content: '暂无内容数据',
  alert: '暂无警报',
  stats: '暂无统计数据'
}

const defaultHints = {
  default: '数据正在收集中，请稍后查看',
  chart: '当有数据时，图表将自动显示',
  users: '等待用户注册和活动',
  content: '等待用户发布内容',
  alert: '系统运行正常，无需警报',
  stats: '统计数据将在有活动时显示'
}

const IconComponent = iconMap[props.type] || iconMap.default
const displayMessage = props.message || defaultMessages[props.type]
const displayHint = props.hint || defaultHints[props.type]
</script>

<template>
  <div class="flex flex-col items-center justify-center h-full py-8 px-4">
    <div class="w-16 h-16 rounded-full bg-gray-100 flex items-center justify-center mb-4">
      <component :is="IconComponent" class="w-8 h-8 text-gray-400" />
    </div>
    
    <h3 class="text-base font-medium text-gray-700 mb-2">
      {{ displayMessage }}
    </h3>
    
    <p class="text-sm text-gray-500 text-center max-w-xs">
      {{ displayHint }}
    </p>
    
    <slot name="action"></slot>
  </div>
</template>

<style scoped>
/* 添加淡入动画 */
.flex {
  animation: fadeIn 0.3s ease-in;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
