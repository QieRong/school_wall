<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { LayoutDashboard, FileText, Users, AlertTriangle, LogOut, Megaphone, LayoutGrid, Image, ShieldAlert, Loader2, Waves, BookOpen, Flame, Menu, X, ChevronRight, Home, ChevronLeft } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const appStore = useAppStore()

const sidebarOpen = ref(false)
const sidebarCollapsed = ref(false)
const showLogoutDialog = ref(false)
const loggingOut = ref(false)

// 面包屑导航配置
const breadcrumbMap = {
  '/admin/dashboard': [{ name: '数据大屏', path: '/admin/dashboard' }],
  '/admin/audit': [{ name: '内容审核', path: '/admin/audit' }],
  '/admin/users': [{ name: '用户管理', path: '/admin/users' }],
  '/admin/category': [{ name: '板块分类', path: '/admin/category' }],
  '/admin/announcement': [{ name: '公告管理', path: '/admin/announcement' }],
  '/admin/banner': [{ name: '轮播图管理', path: '/admin/banner' }],
  '/admin/sensitive': [{ name: '敏感词库', path: '/admin/sensitive' }],
  '/admin/report': [{ name: '举报处理', path: '/admin/report' }],
  '/admin/logs': [{ name: '操作日志', path: '/admin/logs' }],
  '/admin/bottle': [{ name: '漂流瓶管理', path: '/admin/bottle' }],
  '/admin/hotword': [{ name: '热词墙管理', path: '/admin/hotword' }],
  '/admin/story': [{ name: '故事链管理', path: '/admin/story' }],
  '/admin/rankings': [{ name: '排行榜', path: '/admin/rankings' }]
}

// 计算当前面包屑
const breadcrumbs = computed(() => {
  const crumbs = [{ name: '首页', path: '/admin/dashboard', icon: Home }]
  const current = breadcrumbMap[route.path]
  if (current && route.path !== '/admin/dashboard') {
    crumbs.push(...current)
  }
  return crumbs
})

const openLogoutDialog = () => {
  showLogoutDialog.value = true
}

const confirmLogout = () => {
  loggingOut.value = true
  setTimeout(() => {
    userStore.logout()
    appStore.showToast('已退出登录', 'success')
    router.replace('/login')
  }, 500)
}

const closeSidebar = () => {
  sidebarOpen.value = false
}

const toggleSidebarCollapse = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
}
</script>

<template>
  <div class="min-h-screen flex bg-gray-50">
    <!-- 移动端顶部栏 -->
    <div
      class="md:hidden fixed top-0 left-0 right-0 h-14 bg-white border-b border-gray-200 flex items-center justify-between px-4 z-header shadow-sm">
      <div class="flex items-center gap-2">
        <img src="/Round_school_logo.png" class="w-7 h-7" />
        <div>
          <h1 class="font-semibold text-sm text-gray-800">管理控制台</h1>
          <p class="text-[9px] text-gray-400 uppercase tracking-wider">Admin Panel</p>
        </div>
      </div>
      <Button variant="ghost" size="icon" @click="sidebarOpen = true">
        <Menu class="w-5 h-5" />
      </Button>
    </div>

    <!-- 移动端遮罩层 -->
    <div v-if="sidebarOpen" class="md:hidden fixed inset-0 bg-black/50 z-dropdown transition-opacity"
      @click="closeSidebar">
    </div>

    <!-- 响应式侧边栏 -->
    <aside class="admin-sidebar flex flex-col h-screen transition-all duration-300 ease-in-out" :class="[
      'fixed md:sticky top-0 z-sidebar',
      sidebarCollapsed ? 'w-16' : 'w-56',
      sidebarOpen ? 'translate-x-0' : '-translate-x-full md:translate-x-0'
    ]">

      <!-- 移动端关闭按钮 -->
      <div class="md:hidden absolute top-3 right-3">
        <Button variant="ghost" size="icon" @click="closeSidebar">
          <X class="w-4 h-4" />
        </Button>
      </div>

      <!-- Logo区域 -->
      <div class="h-14 flex items-center border-b border-white/10 px-4 flex-shrink-0">
        <img src="/Round_school_logo.png" class="w-7 h-7 flex-shrink-0" />
        <div v-show="!sidebarCollapsed" class="ml-2.5 overflow-hidden">
          <h1 class="font-bold text-sm text-white leading-tight">管理控制台</h1>
          <p class="text-[9px] text-white/60 uppercase tracking-wider">Admin Panel</p>
        </div>
      </div>

      <!-- 导航菜单 -->
      <nav class="flex-1 py-3 px-2 space-y-0.5 overflow-y-auto custom-scrollbar">
        <!-- 数据与内容 -->
        <div v-show="!sidebarCollapsed" class="text-[10px] font-semibold text-white/50 px-3 mb-1.5 mt-1">数据与内容</div>
        <router-link to="/admin/dashboard" active-class="admin-menu-active"
          class="admin-menu-item flex items-center gap-2.5 px-3 py-2 text-sm font-medium text-white/80 rounded-md hover:bg-white/10 transition-all border-l-2 border-transparent"
          @click="closeSidebar">
          <LayoutDashboard class="w-4 h-4 flex-shrink-0" />
          <span v-show="!sidebarCollapsed">数据大屏</span>
        </router-link>

        <router-link to="/admin/audit" active-class="admin-menu-active"
          class="admin-menu-item flex items-center gap-2.5 px-3 py-2 text-sm font-medium text-white/80 rounded-md hover:bg-white/10 transition-all border-l-2 border-transparent"
          @click="closeSidebar">
          <FileText class="w-4 h-4 flex-shrink-0" />
          <span v-show="!sidebarCollapsed">内容审核</span>
        </router-link>

        <!-- 运营与设置 -->
        <div v-show="!sidebarCollapsed" class="text-[10px] font-semibold text-white/50 px-3 mb-1.5 mt-3">运营与设置</div>
        <router-link to="/admin/users" active-class="admin-menu-active"
          class="admin-menu-item flex items-center gap-2.5 px-3 py-2 text-sm font-medium text-white/80 rounded-md hover:bg-white/10 transition-all border-l-2 border-transparent"
          @click="closeSidebar">
          <Users class="w-4 h-4 flex-shrink-0" />
          <span v-show="!sidebarCollapsed">用户管理</span>
        </router-link>

        <router-link to="/admin/category" active-class="admin-menu-active"
          class="admin-menu-item flex items-center gap-2.5 px-3 py-2 text-sm font-medium text-white/80 rounded-md hover:bg-white/10 transition-all border-l-2 border-transparent"
          @click="closeSidebar">
          <LayoutGrid class="w-4 h-4 flex-shrink-0" />
          <span v-show="!sidebarCollapsed">板块分类</span>
        </router-link>

        <router-link to="/admin/announcement" active-class="admin-menu-active"
          class="admin-menu-item flex items-center gap-2.5 px-3 py-2 text-sm font-medium text-white/80 rounded-md hover:bg-white/10 transition-all border-l-2 border-transparent"
          @click="closeSidebar">
          <Megaphone class="w-4 h-4 flex-shrink-0" />
          <span v-show="!sidebarCollapsed">公告管理</span>
        </router-link>

        <router-link to="/admin/banner" active-class="admin-menu-active"
          class="admin-menu-item flex items-center gap-2.5 px-3 py-2 text-sm font-medium text-white/80 rounded-md hover:bg-white/10 transition-all border-l-2 border-transparent"
          @click="closeSidebar">
          <Image class="w-4 h-4 flex-shrink-0" />
          <span v-show="!sidebarCollapsed">轮播图管理</span>
        </router-link>

        <router-link to="/admin/sensitive" active-class="admin-menu-active"
          class="admin-menu-item flex items-center gap-2.5 px-3 py-2 text-sm font-medium text-white/80 rounded-md hover:bg-white/10 transition-all border-l-2 border-transparent"
          @click="closeSidebar">
          <AlertTriangle class="w-4 h-4 flex-shrink-0" />
          <span v-show="!sidebarCollapsed">敏感词库</span>
        </router-link>

        <router-link to="/admin/report" active-class="admin-menu-active"
          class="admin-menu-item flex items-center gap-2.5 px-3 py-2 text-sm font-medium text-white/80 rounded-md hover:bg-white/10 transition-all border-l-2 border-transparent"
          @click="closeSidebar">
          <ShieldAlert class="w-4 h-4 flex-shrink-0" />
          <span v-show="!sidebarCollapsed">举报处理</span>
        </router-link>

        <!-- <router-link to="/admin/logs" active-class="admin-menu-active"
          class="admin-menu-item flex items-center gap-2.5 px-3 py-2 text-sm font-medium text-white/80 rounded-md hover:bg-white/10 transition-all border-l-2 border-transparent"
          @click="closeSidebar">
          <FileText class="w-4 h-4 flex-shrink-0" />
          <span v-show="!sidebarCollapsed">操作日志</span>
        </router-link> -->

        <!-- 特色功能 -->
        <div v-show="!sidebarCollapsed" class="text-[10px] font-semibold text-white/50 px-3 mb-1.5 mt-3">特色功能</div>
        <router-link to="/admin/bottle" active-class="admin-menu-active"
          class="admin-menu-item flex items-center gap-2.5 px-3 py-2 text-sm font-medium text-white/80 rounded-md hover:bg-white/10 transition-all border-l-2 border-transparent"
          @click="closeSidebar">
          <Waves class="w-4 h-4 flex-shrink-0" />
          <span v-show="!sidebarCollapsed">漂流瓶管理</span>
        </router-link>

        <router-link to="/admin/hotword" active-class="admin-menu-active"
          class="admin-menu-item flex items-center gap-2.5 px-3 py-2 text-sm font-medium text-white/80 rounded-md hover:bg-white/10 transition-all border-l-2 border-transparent"
          @click="closeSidebar">
          <Flame class="w-4 h-4 flex-shrink-0" />
          <span v-show="!sidebarCollapsed">热词墙管理</span>
        </router-link>

        <router-link to="/admin/story" active-class="admin-menu-active"
          class="admin-menu-item flex items-center gap-2.5 px-3 py-2 text-sm font-medium text-white/80 rounded-md hover:bg-white/10 transition-all border-l-2 border-transparent"
          @click="closeSidebar">
          <BookOpen class="w-4 h-4 flex-shrink-0" />
          <span v-show="!sidebarCollapsed">故事链管理</span>
        </router-link>
      </nav>

      <!-- 底部操作区 -->
      <div class="border-t border-white/10 p-2 flex-shrink-0">
        <!-- 折叠按钮 (仅桌面端显示) -->
        <Button variant="ghost" size="sm"
          class="hidden md:flex w-full justify-center mb-2 text-white/70 hover:text-white hover:bg-white/10"
          @click="toggleSidebarCollapse">
          <ChevronLeft v-if="!sidebarCollapsed" class="w-4 h-4" />
          <ChevronRight v-if="sidebarCollapsed" class="w-4 h-4" />
          <span v-show="!sidebarCollapsed" class="ml-2 text-xs">收起</span>
        </Button>

        <!-- 退出登录按钮 -->
        <Button variant="outline" size="sm"
          :class="sidebarCollapsed ? 'w-full justify-center px-2' : 'w-full justify-start'"
          class="text-red-300 hover:text-red-200 hover:bg-red-500/20 border-red-400/30 bg-transparent"
          @click="openLogoutDialog">
          <LogOut class="w-4 h-4" :class="!sidebarCollapsed && 'mr-2'" />
          <span v-show="!sidebarCollapsed">退出登录</span>
        </Button>
      </div>
    </aside>

    <!-- 退出确认弹窗 -->
    <Dialog :open="showLogoutDialog" @update:open="showLogoutDialog = $event">
      <DialogContent class="sm:max-w-[380px]">
        <DialogHeader>
          <DialogTitle class="text-gray-800 flex items-center gap-2">
            <LogOut class="w-5 h-5 text-red-500" /> 退出确认
          </DialogTitle>
        </DialogHeader>
        <div class="py-4">
          <p class="text-gray-600">确定要退出管理后台吗？</p>
          <p class="text-sm text-gray-400 mt-2">退出后需要重新登录才能访问管理功能。</p>
        </div>
        <DialogFooter class="gap-2">
          <Button variant="outline" @click="showLogoutDialog = false" :disabled="loggingOut">取消</Button>
          <Button variant="destructive" @click="confirmLogout" :disabled="loggingOut">
            <Loader2 v-if="loggingOut" class="w-4 h-4 mr-2 animate-spin" />
            {{ loggingOut ? '退出中...' : '确认退出' }}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- 主内容区 -->
    <main class="flex-1 flex flex-col min-w-0 pt-14 md:pt-0">
      <!-- 面包屑导航 -->
      <div class="bg-white border-b border-gray-200 px-4 md:px-6 py-3 flex-shrink-0">
        <nav class="flex items-center gap-2 text-sm">
          <router-link v-for="(crumb, index) in breadcrumbs" :key="crumb.path" :to="crumb.path"
            class="flex items-center gap-2 transition-colors"
            :class="index === breadcrumbs.length - 1 ? 'text-primary font-medium' : 'text-gray-500 hover:text-gray-700'">
            <component v-if="crumb.icon" :is="crumb.icon" class="w-4 h-4" />
            <span>{{ crumb.name }}</span>
            <ChevronRight v-if="index < breadcrumbs.length - 1" class="w-4 h-4 text-gray-400" />
          </router-link>
        </nav>
      </div>

      <!-- 页面内容 -->
      <div class="flex-1 overflow-auto p-4 md:p-6">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </main>
  </div>
</template>

<style scoped>
/* 侧边栏渐变背景 */
.admin-sidebar {
  background: linear-gradient(180deg, #1a5c45 0%, #216f55 100%);
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.1);
}

/* 菜单项激活状态 */
.admin-menu-active {
  background: rgba(255, 255, 255, 0.15) !important;
  color: white !important;
  border-left-color: white !important;
}

/* 菜单项悬停动画 */
.admin-menu-item {
  position: relative;
  transition: all 0.2s ease;
}

.admin-menu-item:hover {
  transform: translateX(4px);
  background: rgba(255, 255, 255, 0.1);
  color: white;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 确保过渡期间没有背景色 */
.fade-enter-active>*,
.fade-leave-active>* {
  background: transparent !important;
}

.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 3px;
}

.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.3);
}
</style>
