<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
import { useDashboardStore } from '@/stores/dashboard'
import { useWsStore } from '@/stores/ws'
import { FileText, MessageCircle, AlertTriangle, Flag, Clock } from 'lucide-vue-next'
import EmptyState from './EmptyState.vue'

const dashboardStore = useDashboardStore()
const wsStore = useWsStore()

const feed = computed(() => dashboardStore.realtimeFeed)
const hasFeed = computed(() => feed.value && feed.value.length > 0)

const getIcon = (type) => {
  const icons = {
    NEW_POST: FileText,
    NEW_COMMENT: MessageCircle,
    SENSITIVE_BLOCKED: AlertTriangle,
    NEW_REPORT: Flag
  }
  return icons[type] || FileText
}

const getIconColor = (type) => {
  const colors = {
    NEW_POST: 'text-blue-500 bg-blue-100',
    NEW_COMMENT: 'text-green-500 bg-green-100',
    SENSITIVE_BLOCKED: 'text-red-500 bg-red-100',
    NEW_REPORT: 'text-orange-500 bg-orange-100'
  }
  return colors[type] || 'text-gray-500 bg-gray-100'
}

const formatTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

// 监听 WebSocket 消息
const handleWsMessage = (data) => {
  if (data.type === 'DASHBOARD_EVENT') {
    dashboardStore.addRealtimeEvent({
      ...data.data,
      timestamp: Date.now()
    })
  }
}

onMounted(() => {
  // 注册 WebSocket 消息处理
  if (wsStore.ws) {
    wsStore.ws.addEventListener('message', (e) => {
      try {
        const data = JSON.parse(e.data)
        handleWsMessage(data)
      } catch (err) {}
    })
  }
})
</script>

<template>
  <div class="h-full flex flex-col">
    <!-- 有动态时显示列表 -->
    <div v-if="hasFeed" class="flex-1 overflow-y-auto space-y-2 pr-1 custom-scrollbar">
      <TransitionGroup name="feed">
        <div v-for="(item, index) in feed" :key="item.timestamp + index"
          class="flex items-start gap-3 p-2 bg-gray-50 rounded-lg hover:bg-gray-100 transition">
          <div :class="['w-8 h-8 rounded-full flex items-center justify-center', getIconColor(item.eventType)]">
            <component :is="getIcon(item.eventType)" class="w-4 h-4" />
          </div>
          <div class="flex-1 min-w-0">
            <div class="text-sm text-gray-700 truncate">{{ item.content }}</div>
            <div class="text-xs text-gray-400 mt-1">{{ formatTime(item.timestamp) }}</div>
          </div>
        </div>
      </TransitionGroup>
    </div>
    
    <!-- 无动态时显示空状态 -->
    <EmptyState 
      v-else
      type="stats"
      message="暂无实时动态"
      hint="等待用户活动，动态将实时显示"
    />
  </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar { width: 4px; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #d1d5db; border-radius: 2px; }
.feed-enter-active { animation: slideIn 0.3s ease-out; }
.feed-leave-active { animation: slideOut 0.2s ease-in; }
@keyframes slideIn { from { opacity: 0; transform: translateY(-10px); } to { opacity: 1; transform: translateY(0); } }
@keyframes slideOut { from { opacity: 1; } to { opacity: 0; } }
</style>
