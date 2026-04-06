<script setup>
import { TrendingUp, TrendingDown } from 'lucide-vue-next'

const props = defineProps({
  title: {
    type: String,
    required: true
  },
  value: {
    type: [Number, String],
    required: true
  },
  icon: {
    type: Object,
    default: null
  },
  iconBg: {
    type: String,
    default: 'bg-primary/10'
  },
  iconColor: {
    type: String,
    default: 'text-primary'
  },
  trend: {
    type: String,
    validator: (value) => ['up', 'down', null].includes(value),
    default: null
  },
  trendValue: {
    type: [Number, String],
    default: null
  },
  loading: {
    type: Boolean,
    default: false
  }
})
</script>

<template>
  <div
    class="admin-card bg-white rounded-xl p-6 border border-gray-200 hover:shadow-lg transition-all duration-300 hover:-translate-y-1">
    <div class="flex items-start justify-between">
      <!-- 左侧内容 -->
      <div class="flex-1">
        <p class="text-sm font-medium text-gray-600 mb-2">{{ title }}</p>

        <!-- 加载状态 -->
        <div v-if="loading" class="space-y-2">
          <div class="h-8 w-24 bg-gray-200 rounded animate-pulse"></div>
          <div class="h-4 w-16 bg-gray-200 rounded animate-pulse"></div>
        </div>

        <!-- 数据展示 -->
        <div v-else>
          <p class="text-3xl font-bold text-gray-900 mb-2">{{ value }}</p>

          <!-- 趋势指示器 -->
          <div v-if="trend && trendValue" class="flex items-center gap-1">
            <TrendingUp v-if="trend === 'up'" class="w-4 h-4 text-green-500" />
            <TrendingDown v-else-if="trend === 'down'" class="w-4 h-4 text-red-500" />
            <span :class="[
              'text-sm font-medium',
              trend === 'up' ? 'text-green-600' : 'text-red-600'
            ]">
              {{ trendValue }}
            </span>
          </div>
        </div>
      </div>

      <!-- 右侧图标 -->
      <div v-if="icon" :class="[
        'flex items-center justify-center w-14 h-14 rounded-xl',
        iconBg
      ]">
        <component :is="icon" :class="['w-7 h-7', iconColor]" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-card {
  position: relative;
  overflow: hidden;
}

.admin-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--color-primary-500), var(--color-primary-400));
  opacity: 0;
  transition: opacity 0.3s ease;
}

.admin-card:hover::before {
  opacity: 1;
}
</style>
