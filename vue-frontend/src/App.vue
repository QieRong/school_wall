<script setup>
import { onMounted, onUnmounted } from 'vue'
import { useAppStore } from '@/stores/app'
import { useWsStore } from '@/stores/ws'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import { CheckCircle2, XCircle, AlertTriangle, Info } from 'lucide-vue-next'

const appStore = useAppStore()
const wsStore = useWsStore()

// 全局网络状态监听
const handleOnline = () => {
  appStore.showToast('✅ 网络已恢复', 'success')
}

const handleOffline = () => {
  appStore.showToast('📡 网络已断开，请检查网络连接', 'error')
}

// 强制移除body上的pointer-events样式（修复退出登录后无法点击的问题）
const forceRemoveBodyPointerEvents = () => {
  if (document.body.style.pointerEvents === 'none') {
    console.warn('检测到body上有pointer-events: none，正在强制移除...')
    document.body.style.pointerEvents = ''
  }
}

// MutationObserver监听body样式变化
let bodyObserver = null

// 初始化检查登录并连接 WebSocket
onMounted(() => {
  // 强制移除body上的pointer-events样式
  forceRemoveBodyPointerEvents()

  // 创建MutationObserver监听body的style属性变化
  bodyObserver = new MutationObserver((mutations) => {
    mutations.forEach((mutation) => {
      if (mutation.type === 'attributes' && mutation.attributeName === 'style') {
        forceRemoveBodyPointerEvents()
      }
    })
  })

  // 开始监听body元素
  bodyObserver.observe(document.body, {
    attributes: true,
    attributeFilter: ['style']
  })

  // 尝试连接 WS
  const userStr = localStorage.getItem('user')
  if (userStr) {
    try {
      const user = JSON.parse(userStr)
      if (user.token) {
        wsStore.connect(user.token)
      }
    } catch (e) { }
  }

  // 添加网络状态监听
  window.addEventListener('online', handleOnline)
  window.addEventListener('offline', handleOffline)

  // 【新增】全局音频解锁：监听用户首次交互以预加载音频
  const enableAudio = () => {
    wsStore.initAudio()
    document.removeEventListener('click', enableAudio)
    document.removeEventListener('keydown', enableAudio)
    document.removeEventListener('touchstart', enableAudio)
  }
  document.addEventListener('click', enableAudio)
  document.addEventListener('keydown', enableAudio)
  document.addEventListener('touchstart', enableAudio) // 兼容移动端

  // 初始检查网络状态
  if (!navigator.onLine) {
    handleOffline()
  }
})

onUnmounted(() => {
  window.removeEventListener('online', handleOnline)
  window.removeEventListener('offline', handleOffline)

  // 断开MutationObserver
  if (bodyObserver) {
    bodyObserver.disconnect()
    bodyObserver = null
  }
})
</script>

<template>
  <div>
    <div
      class="fixed top-24 left-1/2 -translate-x-1/2 z-[9999] flex flex-col gap-2 pointer-events-none w-full max-w-md px-4 items-center">
      <TransitionGroup name="toast">
        <div v-for="msg in appStore.toasts" :key="msg.id"
          class="px-6 py-3 rounded-full shadow-2xl text-sm font-bold flex items-center gap-3 pointer-events-auto border-2 animate-in slide-in-from-top-4 fade-in duration-300"
          :class="{
            'bg-emerald-600 text-white border-emerald-400': msg.type === 'success',
            'bg-red-600 text-white border-red-400': msg.type === 'error',
            'bg-orange-600 text-white border-orange-400': msg.type === 'warning',
            'bg-pink-600 text-white border-pink-400': msg.type === 'info'
          }">
          <CheckCircle2 v-if="msg.type === 'success'" class="w-5 h-5" />
          <XCircle v-else-if="msg.type === 'error'" class="w-5 h-5" />
          <AlertTriangle v-else-if="msg.type === 'warning'" class="w-5 h-5" />
          <Info v-else class="w-5 h-5" />
          <span>{{ msg.text }}</span>
        </div>
      </TransitionGroup>
    </div>

    <!-- 全局确认对话框 -->
    <ConfirmDialog :open="appStore.confirmDialog.open" @update:open="appStore.confirmDialog.open = $event" :title="appStore.confirmDialog.title"
      :message="appStore.confirmDialog.message" :confirm-text="appStore.confirmDialog.confirmText"
      :cancel-text="appStore.confirmDialog.cancelText" :type="appStore.confirmDialog.type"
      @confirm="appStore.confirmDialog.onConfirm?.()" @cancel="appStore.confirmDialog.onCancel?.()" />

    <router-view />
  </div>
</template>

<style>
.toast-enter-active,
.toast-leave-active {
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(-20px) scale(0.95);
}

.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.page-fade-enter-from {
  opacity: 0;
  transform: translateY(5px);
}

.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-5px);
}
</style>