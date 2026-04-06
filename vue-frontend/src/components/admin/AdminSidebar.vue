<template>
  <aside 
    class="fixed inset-y-0 left-0 z-sidebar bg-gradient-to-b from-[#1a5c45] to-[#216f55] text-white transition-all duration-300 ease-in-out flex flex-col shadow-xl"
    :class="[
      collapsed ? 'w-16' : 'w-64',
      mobileOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'
    ]"
  >
    <!-- Logo -->
    <div class="h-16 flex items-center justify-center border-b border-white/10 shrink-0">
      <img src="/Round_school_logo.png" alt="Logo" class="w-8 h-8 rounded-full bg-white p-0.5" />
      <span v-if="!collapsed" class="ml-3 font-bold text-lg truncate tracking-wide">表白墙管理后台</span>
    </div>

    <!-- Menu -->
    <nav class="flex-1 overflow-y-auto py-4 custom-scrollbar">
      <ul class="space-y-1 px-2">
        <li v-for="item in menuItems" :key="item.path">
          <router-link 
            :to="item.path"
            class="group flex items-center px-3 py-2.5 rounded-lg transition-all duration-200 relative overflow-hidden"
            :class="[
              $route.path === item.path 
                ? 'bg-white/15 text-white shadow-sm' 
                : 'text-white/80 hover:bg-white/10 hover:text-white hover:translate-x-1'
            ]"
          >
            <!-- Active Indicator -->
            <div 
              v-if="$route.path === item.path"
              class="absolute left-0 top-1/2 -translate-y-1/2 w-1 h-8 bg-white rounded-r-full"
            ></div>

            <component :is="item.icon" class="w-5 h-5 shrink-0 transition-transform group-hover:scale-110" />
            
            <span 
              v-if="!collapsed" 
              class="ml-3 text-sm font-medium truncate flex-1 transition-opacity duration-200"
            >
              {{ item.label }}
            </span>

            <!-- Badge -->
            <span 
              v-if="!collapsed && item.badge && item.badge() > 0"
              class="ml-auto bg-red-500 text-white text-xs font-bold px-2 py-0.5 rounded-full min-w-[1.25rem] text-center"
            >
              {{ item.badge() > 99 ? '99+' : item.badge() }}
            </span>
            <div
              v-else-if="collapsed && item.badge && item.badge() > 0"
              class="absolute top-1 right-1 w-2.5 h-2.5 bg-red-500 rounded-full border-2 border-[#1a5c45]"
            ></div>
          </router-link>
        </li>
      </ul>
    </nav>

    <!-- Footer / User Info -->
    <div class="p-4 border-t border-white/10 shrink-0">
      <button 
        @click="handleLogout"
        class="flex items-center w-full px-2 py-2 rounded-lg text-white/70 hover:bg-white/10 hover:text-white transition-colors"
      >
        <LogOut class="w-5 h-5" />
        <span v-if="!collapsed" class="ml-3 text-sm">退出登录</span>
      </button>
    </div>
  </aside>

  <!-- Mobile Overlay -->
  <div 
    v-if="mobileOpen"
    class="fixed inset-0 bg-black/50 z-[998] lg:hidden backdrop-blur-sm transition-opacity"
    @click="$emit('close-mobile')"
  ></div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAdminStore } from '@/stores/admin'
import { 
  LayoutDashboard, 
  ShieldAlert, 
  Users, 
  Flag, 
  Ban, 
  Megaphone, 
  LayoutGrid, 
  Images, 
  Ship, 
  Flame, 
  BookOpen, 
  FileText, 
  LogOut,
  BarChart3
} from 'lucide-vue-next'

const props = defineProps({
  collapsed: {
    type: Boolean,
    default: false
  },
  mobileOpen: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close-mobile'])

const router = useRouter()
const route = useRoute()
const adminStore = useAdminStore()

const menuItems = [
  {
    icon: LayoutDashboard,
    label: '数据总览',
    path: '/admin/dashboard'
  },
  {
    icon: BarChart3,
    label: '数据大屏',
    path: '/admin/data-dashboard'
  },
  {
    icon: ShieldAlert,
    label: '内容审核',
    path: '/admin/audit',
    badge: () => adminStore.pendingAuditCount
  },
  {
    icon: Users,
    label: '用户管理',
    path: '/admin/users'
  },
  {
    icon: Flag,
    label: '举报处理',
    path: '/admin/reports',
    badge: () => adminStore.pendingReportCount
  },
  {
    icon: Ban,
    label: '敏感词管理',
    path: '/admin/sensitive'
  },
  {
    icon: Megaphone,
    label: '公告管理',
    path: '/admin/announcements'
  },
  {
    icon: LayoutGrid,
    label: '分类管理',
    path: '/admin/categories'
  },
  {
    icon: Images,
    label: '轮播图管理',
    path: '/admin/banners'
  },
  {
    icon: Ship,
    label: '漂流瓶管理',
    path: '/admin/bottles'
  },
  {
    icon: Flame,
    label: '热词墙管理',
    path: '/admin/hotwords'
  },
  {
    icon: BookOpen,
    label: '故事接龙',
    path: '/admin/stories'
  }
  // {
  //   icon: FileText,
  //   label: '操作日志',
  //   path: '/admin/logs'
  // }
]

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  router.push('/login')
}
</script>
