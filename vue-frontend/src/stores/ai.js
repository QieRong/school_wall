// AI功能状态管理
import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/api/request'

export const useAiStore = defineStore('ai', () => {
  // AI剩余次数
  const remaining = ref(10)
  
  // 是否正在加载
  const loading = ref(false)

  // 获取剩余次数
  const fetchRemaining = async (userId) => {
    if (!userId) return
    
    loading.value = true
    try {
      const res = await request.get('/ai/remaining', { params: { userId } })
      if (res.code === '200') {
        remaining.value = res.data
      }
    } catch (e) {
      console.error('获取AI次数失败:', e)
    } finally {
      loading.value = false
    }
  }

  // 更新剩余次数（在AI调用成功后由组件调用）
  const updateRemaining = (newRemaining) => {
    remaining.value = newRemaining
  }

  // 减少次数（乐观更新）
  const decreaseRemaining = () => {
    if (remaining.value > 0) {
      remaining.value--
    }
  }

  // 重置次数（用于登出或切换用户）
  const reset = () => {
    remaining.value = 10
    loading.value = false
  }

  return {
    remaining,
    loading,
    fetchRemaining,
    updateRemaining,
    decreaseRemaining,
    reset
  }
})
