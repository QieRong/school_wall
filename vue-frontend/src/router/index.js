// File: vue-frontend/src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'
import { useAppStore } from '@/stores/app'

// 保存滚动位置的 Map
const scrollPositions = new Map()

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  // 滚动行为配置
  scrollBehavior(to, from, savedPosition) {
    // 如果有保存的位置（浏览器前进/后退）
    if (savedPosition) {
      return savedPosition
    }

    // 如果是从详情页返回列表页，恢复滚动位置
    if (from.name === 'post-detail' && to.name === 'home') {
      const savedScroll = scrollPositions.get('home')
      if (savedScroll) {
        return { top: savedScroll, behavior: 'instant' }
      }
    }

    // 如果是从用户详情返回首页
    if (from.name === 'profile' && to.name === 'home') {
      const savedScroll = scrollPositions.get('home')
      if (savedScroll) {
        return { top: savedScroll, behavior: 'instant' }
      }
    }

    // 默认滚动到顶部
    return { top: 0, behavior: 'smooth' }
  },
  routes: [
    { 
      path: '/', 
      redirect: '/home'
    },
    { 
      path: '/home', 
      name: 'home', 
      component: () => import('@/views/Home.vue')
    },
    { path: '/login', name: 'login', component: () => import('@/views/Login.vue') },
    { path: '/user/:id', name: 'profile', component: () => import('@/views/Profile.vue') },
    { path: '/post/:id', name: 'post-detail', component: () => import('@/views/PostDetail.vue') },

    // 【核心新增】
    { path: '/message', name: 'message', component: () => import('@/views/Message.vue') },
    { path: '/follow/:type/:id', name: 'follow-list', component: () => import('@/views/FollowList.vue') },
    { path: '/settings/blacklist', name: 'blacklist', component: () => import('@/views/Blacklist.vue') },
    { path: '/bottle', name: 'bottle', component: () => import('@/views/Bottle.vue') },

    // 热词墙
    { path: '/hotword', name: 'hotword', component: () => import('@/views/HotwordWall.vue') },
    { path: '/hotword/my-posts', name: 'hotword-my-posts', component: () => import('@/views/HotwordMyPosts.vue') },
    { path: '/hotword/my-votes', name: 'hotword-my-votes', component: () => import('@/views/HotwordMyVotes.vue') },
    { path: '/hotword/museum', name: 'hotword-museum', component: () => import('@/views/HotwordMuseum.vue') },

    // 通知列表 (历史公告)
    { path: '/notices', name: 'notice-list', component: () => import('@/views/NoticeList.vue') },

    // 全局搜索
    { path: '/search', name: 'search', component: () => import('@/views/Search.vue') },

    // 协作故事链
    { path: '/story', name: 'story-hall', component: () => import('@/views/StoryHall.vue') },
    { path: '/story/:id', name: 'story-read', component: () => import('@/views/StoryRead.vue') },
    { path: '/story/my', name: 'my-story', component: () => import('@/views/MyStory.vue') },
    { path: '/story/archive', name: 'story-archive', component: () => import('@/views/StoryArchive.vue') },

    // 后台路由
    {
      path: '/admin',
      component: () => import('@/layout/AdminLayout.vue'),
      redirect: '/admin/dashboard',
      beforeEnter: (to, from, next) => {
        const appStore = useAppStore()
        
        const redirectToLogin = (clearStorage = false) => {
          if (clearStorage) localStorage.removeItem('user')
          next('/login')
        }

        const userStr = localStorage.getItem('user')
        const token = localStorage.getItem('token')
        
        if (!userStr || !token) return redirectToLogin()

        try {
          const user = JSON.parse(userStr)
          
          // 严格验证用户角色
          if (user?.role !== 1) {
            console.warn('非管理员用户尝试访问后台')
            appStore.showToast('无权访问管理后台', 'error')
            return redirectToLogin()
          }

          // 验证token是否过期（如果token包含过期时间）
          if (token) {
            try {
              const tokenParts = token.split('.')
              if (tokenParts.length === 3) {
                const payload = JSON.parse(atob(tokenParts[1]))
                if (payload.exp && payload.exp * 1000 < Date.now()) {
                  console.warn('Token已过期')
                  appStore.showToast('登录已过期，请重新登录', 'error')
                  return redirectToLogin(true)
                }
              }
            } catch (e) {
              console.error('Token解析失败:', e)
            }
          }

          next()
        } catch (e) {
          console.error('用户信息解析失败:', e)
          redirectToLogin(true)
        }
      },
      children: [
        { path: 'dashboard', component: () => import('@/views/admin/Dashboard.vue') },
        { path: 'rankings', component: () => import('@/views/admin/Rankings.vue') },
        { path: 'audit', component: () => import('@/views/admin/PostAudit.vue') },
        { path: 'users', component: () => import('@/views/admin/UserManage.vue') },
        { path: 'sensitive', component: () => import('@/views/admin/SensitiveManage.vue') },
        { path: 'announcement', component: () => import('@/views/admin/AnnouncementManage.vue') },
        { path: 'category', component: () => import('@/views/admin/CategoryManage.vue') },
        { path: 'report', component: () => import('@/views/admin/ReportManage.vue') },
        { path: 'banner', component: () => import('@/views/admin/BannerManage.vue') },
        { path: 'bottle', component: () => import('@/views/admin/BottleManage.vue') },
        { path: 'hotword', component: () => import('@/views/admin/HotwordManage.vue') },
        { path: 'story', component: () => import('@/views/admin/StoryManage.vue') },
        { path: 'logs', component: () => import('@/views/admin/SystemLogs.vue') },
      ]
    }
  ]
})

// 全局路由守卫
router.beforeEach((to, from, next) => {
  console.log(`路由导航: ${from.path} -> ${to.path}`)
  
  const token = localStorage.getItem('token')
  const userStr = localStorage.getItem('user')
  
  // 如果要去登录页，直接放行
  if (to.path === '/login') {
    // 如果已经登录了，重定向到首页
    if (token && userStr) {
      console.log('已登录，重定向到首页')
      return next('/home')
    }
    return next()
  }
  
  // 如果没有token或user，重定向到登录页
  if (!token || !userStr) {
    console.log('未登录，重定向到登录页')
    return next('/login')
  }
  
  // 已登录，放行
  next()
})

// 【简化】路由完成后的处理
router.afterEach((to) => {
  console.log(`路由完成: ${to.name || '/'}`)
})

export { scrollPositions }
export default router