<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getBlacklist, unblockUser } from '@/api/social'
import { useAppStore } from '@/stores/app'
import { Avatar, AvatarImage, AvatarFallback } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import { ChevronLeft, UserX, Loader2, Ban } from 'lucide-vue-next'

const router = useRouter()
const appStore = useAppStore()

const list = ref([])
const loading = ref(true)
const error = ref(false)

const currentUser = computed(() => {
  try { return JSON.parse(localStorage.getItem('user')) } catch { return null }
})

const fetchData = async () => {
  loading.value = true
  error.value = false
  try {
    const res = await getBlacklist(currentUser.value.id)
    if (res.code === '200') {
      list.value = Array.isArray(res.data) ? res.data : []
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

const handleUnblock = async (user) => {
  if (user.loading) return
  user.loading = true
  
  try {
    const res = await unblockUser({ userId: currentUser.value.id, targetId: user.targetId || user.id })
    if (res.code === '200') {
      appStore.showToast('已解除拉黑', 'success')
      // 从列表中移除
      list.value = list.value.filter(item => item.id !== user.id)
    } else {
      appStore.showToast(res.msg || '操作失败', 'error')
    }
  } catch (e) {
    appStore.showToast('操作失败，请重试', 'error')
  } finally {
    user.loading = false
  }
}

onMounted(() => {
  if (!currentUser.value) {
    appStore.showToast('请先登录', 'error')
    router.push('/login')
    return
  }
  fetchData()
})
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-gray-50 via-emerald-50/30 to-teal-50/20 pb-10">
    <header class="sticky top-0 z-40 bg-white/80 backdrop-blur-md border-b border-white/20 h-14 flex items-center px-4 shadow-lg shadow-black/5">
      <Button variant="ghost" size="icon" class="-ml-2 mr-2 hover:bg-emerald-50 hover:text-[rgb(33,111,85)] rounded-full transition-all" @click="router.back()">
        <ChevronLeft class="w-6 h-6" />
      </Button>
      <span class="font-bold bg-gradient-to-r from-gray-800 to-gray-600 bg-clip-text text-transparent text-lg">黑名单管理</span>
      <span class="ml-2 text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded-full font-medium">{{ list.length }}</span>
    </header>

    <div class="max-w-2xl mx-auto p-4 relative z-10">
      <!-- 加载状态 -->
      <div v-if="loading" class="space-y-4">
        <div v-for="i in 3" :key="i" class="bg-white/80 backdrop-blur-md p-5 rounded-2xl border border-white/20 shadow-lg animate-pulse">
          <div class="flex items-center gap-4">
            <div class="w-14 h-14 bg-gray-200 rounded-full"></div>
            <div class="flex-1">
              <div class="h-4 bg-gray-200 rounded-full w-24 mb-2"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else-if="list.length === 0" class="bg-white/80 backdrop-blur-md rounded-2xl p-12 text-center shadow-lg border border-white/20 mt-8">
        <div class="w-20 h-20 mx-auto mb-6 bg-gradient-to-br from-gray-100 to-gray-200 rounded-full flex items-center justify-center">
          <Ban class="w-10 h-10 text-gray-400" />
        </div>
        <p class="text-xl font-bold text-gray-600 mb-2">没有拉黑任何人</p>
        <p class="text-sm text-gray-500">被拉黑的用户将不会出现在您的消息列表和推荐中</p>
      </div>

      <!-- 列表 -->
      <div v-else class="space-y-4">
        <div v-for="item in list" :key="item.id"
          class="bg-white/80 backdrop-blur-md p-5 rounded-2xl border border-white/20 flex items-center justify-between shadow-lg hover:shadow-xl transition-all duration-300">
          <div class="flex items-center gap-4">
            <Avatar class="w-14 h-14 ring-2 ring-white/50 shadow-md grayscale opacity-80">
              <AvatarImage :src="item.avatar" />
              <AvatarFallback class="bg-gray-400 text-white font-bold">{{ item.nickname?.[0] }}</AvatarFallback>
            </Avatar>
            <div>
              <div class="font-bold text-gray-600">{{ item.nickname }}</div>
              <div class="text-xs text-red-400 mt-1 flex items-center gap-1">
                 <Ban class="w-3 h-3" /> 已拉黑
              </div>
            </div>
          </div>

          <Button size="sm" variant="outline"
            class="rounded-full h-9 px-5 text-xs font-medium border-gray-200 hover:bg-emerald-50 hover:text-emerald-600 hover:border-emerald-200 transition-all"
            :disabled="item.loading"
            @click="handleUnblock(item)">
            <Loader2 v-if="item.loading" class="w-3.5 h-3.5 mr-1 animate-spin" />
            <UserX v-else class="w-3.5 h-3.5 mr-1" />
            <span>解除拉黑</span>
          </Button>
        </div>
      </div>
    </div>
  </div>
</template>
