// File: vue-frontend/src/composables/usePerformance.js
// Vue 组合式函数：提供防抖和节流功能

import { ref, onUnmounted } from 'vue'

/**
 * 防抖 Hook - 延迟执行，多次触发只执行最后一次
 * @param {Function} fn - 要执行的函数
 * @param {number} delay - 延迟时间（毫秒）
 * @returns {Function} 防抖后的函数
 */
export function useDebounce(fn, delay = 300) {
  let timer = null
  
  const debouncedFn = (...args) => {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      fn(...args)
    }, delay)
  }
  
  // 组件卸载时清理定时器
  onUnmounted(() => {
    if (timer) clearTimeout(timer)
  })
  
  return debouncedFn
}

/**
 * 节流 Hook - 固定时间间隔执行
 * @param {Function} fn - 要执行的函数
 * @param {number} delay - 时间间隔（毫秒）
 * @returns {Function} 节流后的函数
 */
export function useThrottle(fn, delay = 200) {
  let lastTime = 0
  
  return (...args) => {
    const now = Date.now()
    if (now - lastTime >= delay) {
      lastTime = now
      fn(...args)
    }
  }
}

/**
 * 滚动加载 Hook - 监听滚动事件，触底时加载更多
 * @param {Function} loadMore - 加载更多的回调函数
 * @param {Object} options - 配置选项
 * @returns {Object} 包含滚动处理函数和状态
 */
export function useScrollLoad(loadMore, options = {}) {
  const {
    threshold = 100, // 距离底部多少像素时触发
    throttleDelay = 200 // 节流延迟
  } = options
  
  const isLoading = ref(false)
  const hasMore = ref(true)
  
  const handleScroll = useThrottle(() => {
    if (isLoading.value || !hasMore.value) return
    
    const scrollTop = window.scrollY || document.documentElement.scrollTop
    const windowHeight = window.innerHeight
    const documentHeight = document.documentElement.scrollHeight
    
    if (scrollTop + windowHeight >= documentHeight - threshold) {
      isLoading.value = true
      loadMore().finally(() => {
        isLoading.value = false
      })
    }
  }, throttleDelay)
  
  return {
    handleScroll,
    isLoading,
    hasMore
  }
}
