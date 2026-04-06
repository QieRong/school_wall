<template>
  <div class="relative" ref="bellRef">
    <button 
      @click="toggleDropdown"
      class="relative p-2 rounded-full hover:bg-gray-100 transition-colors focus:outline-none focus:ring-2 focus:ring-primary-500/20"
    >
      <Bell class="w-6 h-6 text-gray-600" />
      <span 
        v-if="count > 0"
        class="absolute top-1 right-1 flex items-center justify-center w-4 h-4 text-[10px] font-bold text-white bg-red-500 rounded-full border-2 border-white animate-pulse"
      >
        {{ count > 9 ? '9+' : count }}
      </span>
    </button>

    <transition
      enter-active-class="transition duration-200 ease-out"
      enter-from-class="transform scale-95 opacity-0"
      enter-to-class="transform scale-100 opacity-100"
      leave-active-class="transition duration-75 ease-in"
      leave-from-class="transform scale-100 opacity-100"
      leave-to-class="transform scale-95 opacity-0"
    >
      <div 
        v-if="isOpen"
        class="absolute right-0 mt-2 w-80 bg-white rounded-xl shadow-lg border border-gray-100 py-2 z-dropdown origin-top-right"
      >
        <div class="px-4 py-2 border-b border-gray-100 flex justify-between items-center">
          <h3 class="font-semibold text-gray-800">通知</h3>
          <button 
            v-if="notifications.length > 0"
            @click="markAllRead"
            class="text-xs text-primary-600 hover:text-primary-700 font-medium"
          >
            全部已读
          </button>
        </div>

        <div class="max-h-96 overflow-y-auto custom-scrollbar">
          <div v-if="notifications.length === 0" class="px-4 py-8 text-center text-gray-500">
            <BellOff class="w-8 h-8 mx-auto mb-2 text-gray-300" />
            <p class="text-sm">暂无新通知</p>
          </div>
          
          <ul v-else class="divide-y divide-gray-50">
            <li 
              v-for="notification in notifications" 
              :key="notification.id"
              class="hover:bg-gray-50 transition-colors"
            >
              <a 
                :href="notification.link" 
                @click.prevent="handleNotificationClick(notification)"
                class="block px-4 py-3"
              >
                <div class="flex items-start">
                  <div 
                    class="shrink-0 w-2 h-2 mt-1.5 rounded-full mr-3"
                    :class="notification.read ? 'bg-transparent' : 'bg-red-500'"
                  ></div>
                  <div class="flex-1 min-w-0">
                    <p class="text-sm font-medium text-gray-900 truncate">
                      {{ notification.title }}
                    </p>
                    <p class="text-sm text-gray-500 line-clamp-2 mt-0.5">
                      {{ notification.content }}
                    </p>
                    <p class="text-xs text-gray-400 mt-1">
                      {{ formatTime(notification.createTime) }}
                    </p>
                  </div>
                </div>
              </a>
            </li>
          </ul>
        </div>

        <div class="px-4 py-2 border-t border-gray-100 text-center">
          <router-link 
            to="/admin/notifications" 
            class="text-xs text-gray-500 hover:text-primary-600 transition-colors"
            @click="isOpen = false"
          >
            查看所有通知
          </router-link>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { onClickOutside } from '@vueuse/core'
import { Bell, BellOff } from 'lucide-vue-next'
import { useAdminStore } from '@/stores/admin'

const router = useRouter()
const adminStore = useAdminStore()
const bellRef = ref(null)
const isOpen = ref(false)

const count = computed(() => adminStore.notificationCount)
const notifications = computed(() => adminStore.notifications)

onClickOutside(bellRef, () => {
  isOpen.value = false
})

const toggleDropdown = () => {
  isOpen.value = !isOpen.value
}

const markAllRead = () => {
  adminStore.markAllAsRead()
}

const handleNotificationClick = (notification) => {
  adminStore.markAsRead(notification.id)
  isOpen.value = false
  if (notification.link) {
    router.push(notification.link)
  }
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return `${date.getMonth() + 1}月${date.getDate()}日`
}
</script>
