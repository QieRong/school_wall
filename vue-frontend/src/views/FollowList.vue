<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getFollowings, getFollowers, followUser, unfollowUser } from '@/api/social'
import { useAppStore } from '@/stores/app'
import { Avatar, AvatarImage, AvatarFallback } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import { ChevronLeft, UserPlus, UserCheck, Users, Loader2 } from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()

const type = ref(route.params.type) // 'followings' or 'followers'
const userId = ref(route.params.id)
const list = ref([])
const loading = ref(true) //初始值设为 true，防止闪烁
const error = ref(false) //错误状态

const currentUser = computed(() => {
  try { return JSON.parse(localStorage.getItem('user')) } catch { return null }
})

const title = computed(() => type.value === 'followings' ? '关注列表' : '粉丝列表')

//验证路由参数
const validateParams = () => {
  const validTypes = ['followings', 'followers']
  const validId = /^\d+$/.test(userId.value)
  
  if (!validTypes.includes(type.value) || !validId) {
    error.value = true
    appStore.showToast('页面参数错误，请检查链接', 'error')
    return false
  }
  return true
}

const fetchData = async () => {
  //先验证参数
  if (!validateParams()) {
    loading.value = false
    return
  }
  
  error.value = false
  loading.value = true
  try {
    let res
    if (type.value === 'followings') {
      res = await getFollowings(userId.value)
    } else {
      res = await getFollowers(userId.value)
    }

    if (res.code === '200') {
      //确保 res.data 是数组，防止 map 报错导致白屏
      const data = Array.isArray(res.data) ? res.data : []
      list.value = data.map(item => ({
        ...item,
        isMutual: item.isMutual === 1,
        isFollowed: type.value === 'followings' ? true : (item.isMutual === 1)
      }))
    } else {
      error.value = true
      appStore.showToast(res.msg || '加载失败', 'error')
    }
  } catch (e) {
    error.value = true
    appStore.showToast('网络错误，请重试', 'error')
  } finally {
    loading.value = false
  }
}

//添加loading状态防止重复点击
const followLoadingMap = ref({})

const handleFollow = async (user) => {
  if (!currentUser.value) return appStore.showToast('请先登录', 'error')

  // 目标ID：关注列表中是 targetId，粉丝列表中是 followerId
  const targetId = type.value === 'followings' ? user.targetId : user.followerId
  
  // 防止重复点击
  if (followLoadingMap.value[targetId]) return
  followLoadingMap.value[targetId] = true

  try {
    if (user.isFollowed) {
      await unfollowUser({ userId: currentUser.value.id, targetId })
      user.isFollowed = false
      appStore.showToast('已取关', 'success')
    } else {
      await followUser({ userId: currentUser.value.id, targetId })
      user.isFollowed = true
      appStore.showToast('关注成功', 'success')
    }
  } catch (e) {
    appStore.showToast('操作失败，请重试', 'error')
  } finally {
    followLoadingMap.value[targetId] = false
  }
}

const goUser = (user) => {
  const targetId = type.value === 'followings' ? user.targetId : user.followerId
  router.push(`/user/${targetId}`)
}

watch(() => route.params, (newParams) => {
  if (newParams.type && newParams.id) {
    type.value = newParams.type
    userId.value = newParams.id
    fetchData()
  }
})

onMounted(fetchData)
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-gray-50 via-emerald-50/30 to-teal-50/20 pb-10">
    <!-- 装饰背景 -->
    <div class="fixed inset-0 pointer-events-none overflow-hidden">
      <div class="absolute top-32 -right-32 w-80 h-80 bg-gradient-to-br from-emerald-100/40 to-teal-100/40 rounded-full blur-3xl"></div>
      <div class="absolute bottom-32 -left-32 w-64 h-64 bg-gradient-to-br from-cyan-100/40 to-blue-100/40 rounded-full blur-3xl"></div>
    </div>
    
    <header class="sticky top-0 z-40 bg-white/80 backdrop-blur-md border-b border-white/20 h-14 flex items-center px-4 shadow-lg shadow-black/5">
      <Button variant="ghost" size="icon" class="-ml-2 mr-2 hover:bg-emerald-50 hover:text-[rgb(33,111,85)] rounded-full transition-all" @click="router.back()">
        <ChevronLeft class="w-6 h-6" />
      </Button>
      <span class="font-bold bg-gradient-to-r from-[rgb(33,111,85)] to-emerald-600 bg-clip-text text-transparent text-lg">{{ title }}</span>
      <span class="ml-2 text-xs bg-emerald-100 text-emerald-600 px-2 py-0.5 rounded-full font-medium">{{ list.length }}</span>
    </header>

    <div class="max-w-2xl mx-auto p-4 relative z-10">
      <!-- 加载状态 -->
      <div v-if="loading" class="space-y-4">
        <div v-for="i in 5" :key="i" class="bg-white/80 backdrop-blur-md p-5 rounded-2xl border border-white/20 shadow-lg animate-pulse">
          <div class="flex items-center gap-4">
            <div class="w-14 h-14 bg-gradient-to-br from-gray-200 to-gray-300 rounded-full"></div>
            <div class="flex-1">
              <div class="h-4 bg-gradient-to-r from-gray-200 to-gray-300 rounded-full w-24 mb-2"></div>
              <div class="h-3 bg-gradient-to-r from-gray-200 to-gray-300 rounded-full w-32"></div>
            </div>
          </div>
        </div>
      </div>

      <!--错误状态 -->
      <div v-else-if="error" class="bg-white/80 backdrop-blur-md rounded-2xl p-12 text-center shadow-lg border border-white/20 mt-8">
        <div class="w-20 h-20 mx-auto mb-6 bg-gradient-to-br from-red-100 to-red-200 rounded-full flex items-center justify-center">
          <span class="text-4xl">😵</span>
        </div>
        <p class="text-xl font-bold text-gray-600 mb-2">页面加载失败</p>
        <p class="text-sm text-gray-500 mb-4">请检查链接是否正确，或返回上一页重试</p>
        <Button class="bg-gradient-to-r from-[rgb(33,111,85)] to-emerald-600 hover:from-[rgb(28,95,72)] hover:to-emerald-700 text-white rounded-full px-6"
          @click="router.back()">
          返回上一页
        </Button>
      </div>

      <!-- 空状态 -->
      <div v-else-if="list.length === 0" class="bg-white/80 backdrop-blur-md rounded-2xl p-12 text-center shadow-lg border border-white/20 mt-8">
        <div class="w-20 h-20 mx-auto mb-6 bg-gradient-to-br from-gray-100 to-gray-200 rounded-full flex items-center justify-center">
          <Users class="w-10 h-10 text-gray-400" />
        </div>
        <p class="text-xl font-bold text-gray-600 mb-2">这里空空如也</p>
        <p class="text-sm text-gray-500">{{ type === 'followings' ? '还没有关注任何人' : '还没有粉丝' }}</p>
      </div>

      <!-- 列表 -->
      <div v-else class="space-y-4">
        <div v-for="item in list" :key="item.id"
          class="bg-white/80 backdrop-blur-md p-5 rounded-2xl border border-white/20 flex items-center justify-between shadow-lg hover:shadow-xl transition-all duration-300 group">
          <div class="flex items-center gap-4 cursor-pointer" @click="goUser(item)">
            <div class="relative">
              <Avatar class="w-14 h-14 ring-2 ring-white/50 shadow-md group-hover:scale-105 transition-transform">
                <AvatarImage :src="item.avatar" />
                <AvatarFallback class="bg-gradient-to-br from-[rgb(33,111,85)] to-emerald-600 text-white font-bold">{{ item.nickname?.[0] }}</AvatarFallback>
              </Avatar>
              <div v-if="item.isMutual" class="absolute -bottom-1 -right-1 w-6 h-6 bg-gradient-to-br from-pink-500 to-rose-500 rounded-full flex items-center justify-center shadow-md">
                <span class="text-white text-[10px]">💕</span>
              </div>
            </div>
            <div>
              <div class="font-bold text-gray-800 group-hover:text-[rgb(33,111,85)] transition-colors">{{ item.nickname }}</div>
              <div class="text-xs text-gray-400 mt-1 line-clamp-1">{{ item.signature || '这个人很懒，什么都没写~' }}</div>
            </div>
          </div>

          <div v-if="currentUser && currentUser.id !== (type === 'followings' ? item.targetId : item.followerId)">
            <Button size="sm"
              class="rounded-full h-9 px-5 text-xs font-medium transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed"
              :class="[
                item.isFollowed 
                  ? 'bg-gray-100 text-gray-500 border border-gray-200 hover:bg-red-50 hover:text-red-500 hover:border-red-200' 
                  : 'bg-gradient-to-r from-[rgb(33,111,85)] to-emerald-600 hover:from-[rgb(28,95,72)] hover:to-emerald-700 text-white shadow-lg shadow-emerald-500/25',
                !followLoadingMap[(type === 'followings' ? item.targetId : item.followerId)] && 'hover:scale-105'
              ]"
              :disabled="followLoadingMap[(type === 'followings' ? item.targetId : item.followerId)]"
              @click="handleFollow(item)">
              <Loader2 v-if="followLoadingMap[(type === 'followings' ? item.targetId : item.followerId)]" class="w-3.5 h-3.5 mr-1 animate-spin" />
              <UserCheck v-else-if="item.isFollowed && item.isMutual" class="w-3.5 h-3.5 mr-1" />
              <UserPlus v-else-if="!item.isFollowed" class="w-3.5 h-3.5 mr-1" />
              <span v-if="followLoadingMap[(type === 'followings' ? item.targetId : item.followerId)]">处理中</span>
              <span v-else-if="item.isFollowed && item.isMutual">互相关注</span>
              <span v-else-if="item.isFollowed">已关注</span>
              <span v-else>关注</span>
            </Button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>