<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import {
  Home, MessageCircle, User, Bell, Search, Menu, X,
  Heart, Flame, BookOpen, Mail, LogOut, Settings
} from 'lucide-vue-next'
import { Avatar, AvatarImage, AvatarFallback } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
  DropdownMenuLabel,
  DropdownMenuSeparator
} from '@/components/ui/dropdown-menu'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const appStore = useAppStore()

const showMobileSidebar = ref(false)
const currentUser = computed(() => userStore.user)

// 导航菜单项
const navItems = [
  { name: '首页', path: '/home', icon: Home },
  { name: '私信', path: '/message', icon: MessageCircle, badge: 0 },
  { name: '漂流瓶', path: '/bottle', icon: Mail },
  { name: '热词墙', path: '/hotword', icon: Flame },
  { name: '故事大厅', path: '/story', icon: BookOpen }
]

// 判断当前路由是否激活
const isActive = (path) => {
  return route.path === path || route.path.startsWith(path + '/')
}

// 退出登录
const handleLogout = () => {
  userStore.logout()
  appStore.showToast('已退出登录', 'success')
  router.replace('/login')
}

// 跳转到个人主页
const goToProfile = () => {
  if (currentUser.value) {
    router.push(`/user/${currentUser.value.id}`)
  }
}

// 关闭移动端侧边栏
const closeMobileSidebar = () => {
  showMobileSidebar.value = false
}

// 监听路由变化，关闭移动端侧边栏
const unwatchRoute = router.afterEach(() => {
  closeMobileSidebar()
})

onUnmounted(() => {
  unwatchRoute()
})
</script>

<template>
  <div class="min-h-screen bg-background">
    <!-- 顶部导航栏 - 固定定位 -->
    <header class="fixed top-0 left-0 right-0 h-16 bg-primary-500 text-white shadow-md z-header">
      <div class="h-full max-w-7xl mx-auto px-4 flex items-center justify-between">
        <!-- Logo和标题 -->
        <div class="flex items-center gap-3">
          <button class="lg:hidden p-2 hover:bg-primary-600 rounded-lg transition-colors"
            @click="showMobileSidebar = !showMobileSidebar">
            <Menu class="w-6 h-6" />
          </button>
          <h1 class="text-xl font-bold cursor-pointer" @click="router.push('/home')">
            张家界学院表白墙
          </h1>
        </div>

        <!-- 右侧用户区域 -->
        <div class="flex items-center gap-4">
          <!-- 通知铃铛 -->
          <button class="relative p-2 hover:bg-primary-600 rounded-lg transition-colors">
            <Bell class="w-5 h-5" />
            <span class="absolute top-1 right-1 w-2 h-2 bg-error-500 rounded-full"></span>
          </button>

          <!-- 用户下拉菜单 -->
          <DropdownMenu v-if="currentUser">
            <DropdownMenuTrigger as-child>
              <button class="flex items-center gap-2 hover:bg-primary-600 rounded-lg px-3 py-2 transition-colors">
                <Avatar class="w-8 h-8">
                  <AvatarImage :src="currentUser.avatar" :alt="currentUser.nickname" />
                  <AvatarFallback>{{ currentUser.nickname?.[0] || 'U' }}</AvatarFallback>
                </Avatar>
                <span class="hidden md:inline text-sm font-medium">{{ currentUser.nickname }}</span>
              </button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" class="w-56 z-user-menu bg-white backdrop-blur-md">
              <DropdownMenuLabel>我的账号</DropdownMenuLabel>
              <DropdownMenuSeparator />
              <DropdownMenuItem @click="goToProfile">
                <User class="w-4 h-4 mr-2" />
                个人主页
              </DropdownMenuItem>
              <DropdownMenuItem @click="router.push('/message')">
                <MessageCircle class="w-4 h-4 mr-2" />
                私信消息
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem @click="handleLogout" class="text-error-500">
                <LogOut class="w-4 h-4 mr-2" />
                退出登录
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>

          <!-- 未登录状态 -->
          <Button v-else variant="secondary" size="sm" @click="router.push('/login')">
            登录
          </Button>
        </div>
      </div>
    </header>

    <!-- 主体内容区域 -->
    <div class="pt-16 flex">
      <!-- 侧边栏 - 桌面端固定，移动端抽屉 -->
      <!-- 移动端遮罩 -->
      <Transition enter-active-class="transition-opacity duration-normal" enter-from-class="opacity-0"
        enter-to-class="opacity-100" leave-active-class="transition-opacity duration-normal"
        leave-from-class="opacity-100" leave-to-class="opacity-0">
        <div v-if="showMobileSidebar" class="fixed inset-0 bg-black/50 z-sidebar lg:hidden"
          @click="closeMobileSidebar" />
      </Transition>

      <!-- 侧边栏内容 -->
      <aside
        class="fixed lg:sticky top-16 left-0 h-[calc(100vh-4rem)] w-60 bg-white border-r border-gray-200 z-sidebar transition-transform duration-normal lg:translate-x-0"
        :class="{ 'translate-x-0': showMobileSidebar, '-translate-x-full': !showMobileSidebar }">
        <nav class="p-4 space-y-2">
          <!-- 关闭按钮 - 仅移动端显示 -->
          <button class="lg:hidden absolute top-4 right-4 p-2 hover:bg-gray-100 rounded-lg transition-colors"
            @click="closeMobileSidebar">
            <X class="w-5 h-5" />
          </button>

          <!-- 导航菜单项 -->
          <router-link v-for="item in navItems" :key="item.path" :to="item.path"
            class="flex items-center gap-3 px-4 py-3 rounded-lg transition-all duration-normal" :class="isActive(item.path)
              ? 'bg-primary-50 text-primary-600 font-medium'
              : 'text-gray-700 hover:bg-gray-100'">
            <component :is="item.icon" class="w-5 h-5" />
            <span>{{ item.name }}</span>
            <span v-if="item.badge" class="ml-auto bg-error-500 text-white text-xs px-2 py-0.5 rounded-full">
              {{ item.badge }}
            </span>
          </router-link>
        </nav>
      </aside>

      <!-- 主内容区域 -->
      <main class="flex-1 min-w-0">
        <div class="max-w-7xl mx-auto px-4 py-6">
          <router-view />
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
/* 确保侧边栏在桌面端始终显示 */
@media (min-width: 1024px) {
  aside {
    display: block !important;
  }
}
</style>
