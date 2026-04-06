// File: vue-frontend/src/stores/app.js
import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const isLoading = ref(false)
  
  // 全局消息队列
  const toasts = ref([])

  // 启动 Loading
  const startLoading = (timeout = 0) => {
    isLoading.value = true
    if (timeout > 0) setTimeout(stopLoading, timeout)
  }

  const stopLoading = () => {
    isLoading.value = false
  }

  // 清除所有loading状态
  const clearLoading = () => {
    isLoading.value = false
  }

  // 显示全局 Toast
  // type: 'success' | 'error' | 'info' | 'warning'
  const showToast = (text, type = 'success') => {
    const id = Date.now()
    toasts.value.push({ id, text, type })
    
    // 3秒后自动消失
    setTimeout(() => {
      const index = toasts.value.findIndex(m => m.id === id)
      if (index !== -1) toasts.value.splice(index, 1)
    }, 3000)
  }

  // 确认对话框状态
  const confirmDialog = ref({
    open: false,
    title: '',
    message: '',
    confirmText: '确认',
    cancelText: '取消',
    type: 'warning',
    onConfirm: null,
    onCancel: null
  })

  // 显示确认对话框
  const showConfirm = (options) => {
    return new Promise((resolve) => {
      confirmDialog.value = {
        open: true,
        title: options.title || '确认操作',
        message: options.message,
        confirmText: options.confirmText || '确认',
        cancelText: options.cancelText || '取消',
        type: options.type || 'warning',
        onConfirm: () => resolve(true),
        onCancel: () => resolve(false)
      }
    })
  }

  return { 
    isLoading, 
    startLoading, 
    stopLoading,
    clearLoading,
    toasts, 
    showToast,
    confirmDialog,
    showConfirm
  }
})