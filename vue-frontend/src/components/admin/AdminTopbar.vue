<template>
  <header class="h-16 bg-white border-b border-gray-200 px-4 flex items-center justify-between sticky top-0 z-header shadow-sm">
    <div class="flex items-center gap-4">
      <button 
        @click="$emit('toggle-sidebar')"
        class="p-2 rounded-lg hover:bg-gray-100 text-gray-600 transition-colors focus:outline-none focus:ring-2 focus:ring-primary-500/20"
      >
        <Menu class="w-5 h-5" />
      </button>

      <!-- Breadcrumb -->
      <nav class="hidden md:flex items-center text-sm text-gray-500">
        <router-link to="/admin/dashboard" class="hover:text-primary-600 transition-colors">首页</router-link>
        <span class="mx-2 text-gray-300">/</span>
        <span class="font-medium text-gray-900">{{ currentPageTitle }}</span>
      </nav>
    </div>

    <div class="flex items-center gap-2 md:gap-4">
      <!-- Search (Hidden on mobile) -->
      <div class="hidden md:block relative w-64">
        <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
        <input 
          type="text" 
          placeholder="全局搜索..." 
          class="w-full pl-10 pr-4 py-2 bg-gray-50 border border-gray-200 rounded-full text-sm focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20 transition-all"
        />
      </div>

      <AdminNotificationBell />

      <div class="w-px h-6 bg-gray-200 mx-1"></div>

      <!-- User Dropdown -->
      <div class="relative" ref="userDropdownRef">
        <button 
          @click="toggleUserDropdown"
          class="flex items-center gap-2 hover:bg-gray-50 p-1.5 rounded-full pr-3 transition-colors focus:outline-none focus:ring-2 focus:ring-primary-500/20"
        >
          <img 
            :src="user.avatar || '/default.png'" 
            class="w-8 h-8 rounded-full object-cover border border-gray-200"
            alt="User"
          />
          <span class="hidden md:block text-sm font-medium text-gray-700">{{ user.nickname || '管理员' }}</span>
          <ChevronDown class="hidden md:block w-4 h-4 text-gray-400 transition-transform duration-200" :class="{ 'rotate-180': isUserDropdownOpen }" />
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
            v-if="isUserDropdownOpen"
            class="absolute right-0 mt-2 w-48 bg-white rounded-xl shadow-lg border border-gray-100 py-1 z-dropdown origin-top-right"
          >
            <div class="px-4 py-2 border-b border-gray-100 md:hidden">
              <p class="text-sm font-medium text-gray-900">{{ user.nickname || '管理员' }}</p>
              <p class="text-xs text-gray-500 truncate">{{ user.email || 'admin@example.com' }}</p>
            </div>
            
            <router-link 
              to="/admin/profile" 
              class="flex items-center px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
              @click="isUserDropdownOpen = false"
            >
              <User class="w-4 h-4 mr-2" />
              个人资料
            </router-link>
            <router-link 
              to="/admin/settings" 
              class="flex items-center px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
              @click="isUserDropdownOpen = false"
            >
              <Settings class="w-4 h-4 mr-2" />
              设置
            </router-link>
            
            <div class="border-t border-gray-100 my-1"></div>
            
            <button 
              @click="handleLogout"
              class="flex w-full items-center px-4 py-2 text-sm text-red-600 hover:bg-red-50"
            >
              <LogOut class="w-4 h-4 mr-2" />
              退出登录
            </button>
          </div>
        </transition>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { onClickOutside } from '@vueuse/core'
import { Menu, Search, ChevronDown, User, Settings, LogOut } from 'lucide-vue-next'
import AdminNotificationBell from './AdminNotificationBell.vue'

defineEmits(['toggle-sidebar'])

const route = useRoute()
const router = useRouter()
const userDropdownRef = ref(null)
const isUserDropdownOpen = ref(false)

// 模拟用户信息，实际应从 store 获取
const user = ref({
  nickname: 'Admin',
  avatar: '/default.png',
  email: 'admin@kiro.com'
})

// 根据路由 meta 获取标题
const currentPageTitle = computed(() => {
  return route.meta.title || '管理后台'
})

onClickOutside(userDropdownRef, () => {
  isUserDropdownOpen.value = false
})

const toggleUserDropdown = () => {
  isUserDropdownOpen.value = !isUserDropdownOpen.value
}

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  router.push('/login')
}
</script>
