// File: vue-frontend/src/stores/user.js
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useWsStore } from './ws'
import { logger } from '@/utils/logger'

export const useUserStore = defineStore('user', () => {
  // 初始状态从 localStorage 读取
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

  // 是否登录
  const isLogged = computed(() => !!user.value)
  
  // 是否管理员
  const isAdmin = computed(() => user.value?.role === 1)

  // 登录动作
  const login = (userData) => {
    user.value = userData
    localStorage.setItem('user', JSON.stringify(userData))
    
    // 保存token到localStorage（单独存储）
    if (userData.token) {
      localStorage.setItem('token', userData.token)
      
      // 登录成功后立即连接 WebSocket
      const wsStore = useWsStore()
      wsStore.connect(userData.token)
      logger.log('✅ 登录成功，Token已保存，WebSocket 已连接')
    }
  }

  // 退出动作
  const logout = () => {
    user.value = null
    localStorage.removeItem('user')
    localStorage.removeItem('token')  // 同时清除token
    
    // 退出时断开 WebSocket 连接
    const wsStore = useWsStore()
    wsStore.disconnect()
    logger.log('✅ 已退出，Token已清除，WebSocket 已断开')
  }

  // 更新资料（保护 token 不被后端 profile 接口返回的 null 覆盖）
  const updateInfo = (partialData) => {
    if (!user.value) return
    // 过滤掉 null/undefined 值，防止覆盖已有有效字段（尤其是 token）
    const safeData = {}
    for (const key of Object.keys(partialData)) {
      if (partialData[key] != null) {
        safeData[key] = partialData[key]
      }
    }
    user.value = { ...user.value, ...safeData }
    localStorage.setItem('user', JSON.stringify(user.value))
  }

  return { user, isLogged, isAdmin, login, logout, updateInfo }
})