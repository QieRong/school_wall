<script setup>
import { computed } from 'vue'
import { useDashboardStore } from '@/stores/dashboard'
import { useRouter } from 'vue-router'
import { AlertTriangle, AlertCircle, AlertOctagon, ExternalLink } from 'lucide-vue-next'
import EmptyState from './EmptyState.vue'

const router = useRouter()
const dashboardStore = useDashboardStore()
const alerts = computed(() => dashboardStore.alerts || [])
const hasAlerts = computed(() => alerts.value && alerts.value.length > 0)

const getAlertStyle = (level) => {
  const styles = {
    red: { bg: 'bg-red-50 border-red-200', icon: 'text-red-500', text: 'text-red-700' },
    orange: { bg: 'bg-orange-50 border-orange-200', icon: 'text-orange-500', text: 'text-orange-700' },
    yellow: { bg: 'bg-yellow-50 border-yellow-200', icon: 'text-yellow-500', text: 'text-yellow-700' }
  }
  return styles[level] || styles.yellow
}

const getAlertIcon = (level) => {
  if (level === 'red') return AlertOctagon
  if (level === 'orange') return AlertTriangle
  return AlertCircle
}

const handleClick = (alert) => {
  if (alert.type === 'HIGH_REPORT' && alert.targetId) {
    router.push(`/admin/report?postId=${alert.targetId}`)
  } else if (alert.type === 'SPAM' && alert.targetId) {
    router.push(`/admin/users?userId=${alert.targetId}`)
  } else if (alert.type === 'QUEUE_OVERFLOW') {
    router.push('/admin/audit')
  }
}

const formatTime = (timestamp) => {
  if (!timestamp) return ''
  return new Date(timestamp).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}
</script>

<template>
  <div class="h-full">
    <!-- 有警报时显示列表 -->
    <div v-if="hasAlerts" class="h-full overflow-y-auto space-y-2 pr-1 custom-scrollbar">
      <div v-for="(alert, index) in alerts" :key="index"
        :class="['p-3 rounded-lg border cursor-pointer transition hover:shadow-sm', getAlertStyle(alert.level).bg]"
        @click="handleClick(alert)">
        <div class="flex items-start gap-3">
          <component :is="getAlertIcon(alert.level)" :class="['w-5 h-5 mt-0.5', getAlertStyle(alert.level).icon]" />
          <div class="flex-1 min-w-0">
            <div :class="['text-sm font-medium', getAlertStyle(alert.level).text]">{{ alert.message }}</div>
            <div class="text-xs text-gray-400 mt-1">{{ formatTime(alert.timestamp) }}</div>
          </div>
          <ExternalLink class="w-4 h-4 text-gray-400" />
        </div>
      </div>
    </div>
    
    <!-- 无警报时显示空状态 -->
    <EmptyState 
      v-else
      type="alert"
      message="✅ 系统运行正常"
      hint="暂无安全警报，所有指标正常"
    />
  </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar { width: 4px; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #d1d5db; border-radius: 2px; }
</style>
