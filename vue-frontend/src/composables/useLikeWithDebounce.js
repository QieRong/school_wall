import { ref, reactive, computed, onUnmounted } from 'vue'

/**
 * 点赞防抖和反作弊Composable
 * @param {Function} likeApiCall - 点赞API调用函数 (contentId, userId) => Promise
 * @param {Object} options - 配置选项
 * @returns {Object} - 点赞处理函数和状态
 */
export function useLikeWithDebounce(likeApiCall, options = {}) {
  const {
    debounceTime = 3000,      // 防抖时间(毫秒)
    maxClicksInWindow = 3,    // 时间窗口内最大点击次数
    cooldownTime = 60000,     // 冷却时间(毫秒)
  } = options

  // 点击历史记录: { contentId: [timestamp1, timestamp2, ...] }
  const clickHistory = reactive({})

  // 冷却记录: { contentId: expiryTimestamp }
  const cooldownMap = reactive({})

  // 加载状态: { contentId: boolean }
  const loadingMap = reactive({})

  // 冷却倒计时: { contentId: remainingSeconds }
  const cooldownCountdown = reactive({})

  // 定时器集合
  const timers = new Map()

  // 从LocalStorage加载冷却记录
  const loadCooldownFromStorage = () => {
    try {
      const stored = localStorage.getItem('like_cooldowns')
      if (stored) {
        const records = JSON.parse(stored)
        const now = Date.now()
        
        // 过滤掉已过期的记录
        const validRecords = records.filter(r => r.expiryTime > now)
        
        // 恢复到cooldownMap
        validRecords.forEach(r => {
          cooldownMap[r.contentId] = r.expiryTime
          startCooldownCountdown(r.contentId, r.expiryTime)
        })
      }
    } catch (e) {
      console.error('加载冷却记录失败:', e)
    }
  }

  // 保存冷却记录到LocalStorage
  const saveCooldownToStorage = () => {
    try {
      const now = Date.now()
      const records = Object.entries(cooldownMap)
        .filter(([_, expiryTime]) => expiryTime > now)
        .map(([contentId, expiryTime]) => ({
          contentId,
          expiryTime,
          reason: 'spam'
        }))
      
      localStorage.setItem('like_cooldowns', JSON.stringify(records))
    } catch (e) {
      console.error('保存冷却记录失败:', e)
    }
  }

  // 开始冷却倒计时
  const startCooldownCountdown = (contentId, expiryTime) => {
    // 清除旧的定时器
    if (timers.has(contentId)) {
      clearInterval(timers.get(contentId))
    }

    const updateCountdown = () => {
      const now = Date.now()
      const remaining = Math.max(0, Math.ceil((expiryTime - now) / 1000))
      
      if (remaining > 0) {
        cooldownCountdown[contentId] = remaining
      } else {
        // 冷却结束
        delete cooldownMap[contentId]
        delete cooldownCountdown[contentId]
        clearInterval(timers.get(contentId))
        timers.delete(contentId)
        saveCooldownToStorage()
      }
    }

    // 立即更新一次
    updateCountdown()

    // 每秒更新
    const timer = setInterval(updateCountdown, 1000)
    timers.set(contentId, timer)
  }

  // 检查是否在冷却期
  const isCooldown = (contentId) => {
    const expiryTime = cooldownMap[contentId]
    if (!expiryTime) return false

    const now = Date.now()
    if (now >= expiryTime) {
      // 冷却已结束,清理记录
      delete cooldownMap[contentId]
      delete cooldownCountdown[contentId]
      saveCooldownToStorage()
      return false
    }

    return true
  }

  // 获取剩余冷却时间(秒)
  const getCooldownRemaining = (contentId) => {
    return cooldownCountdown[contentId] || 0
  }

  // 记录点击
  const recordClick = (contentId) => {
    const now = Date.now()
    
    if (!clickHistory[contentId]) {
      clickHistory[contentId] = []
    }

    // 添加当前点击
    clickHistory[contentId].push(now)

    // 清理超过时间窗口的点击记录
    clickHistory[contentId] = clickHistory[contentId].filter(
      timestamp => now - timestamp < debounceTime
    )
  }

  // 检查是否触发反作弊
  const checkSpam = (contentId) => {
    const clicks = clickHistory[contentId] || []
    return clicks.length > maxClicksInWindow
  }

  // 触发冷却
  const triggerCooldown = (contentId) => {
    const expiryTime = Date.now() + cooldownTime
    cooldownMap[contentId] = expiryTime
    startCooldownCountdown(contentId, expiryTime)
    saveCooldownToStorage()
  }

  // 点赞处理函数
  const handleLike = async (contentId, userId, currentLikeStatus) => {
    // 检查是否在冷却期
    if (isCooldown(contentId)) {
      const remaining = getCooldownRemaining(contentId)
      return {
        success: false,
        message: `操作过于频繁,请${remaining}秒后再试`,
        type: 'cooldown'
      }
    }

    // 检查是否正在加载
    if (loadingMap[contentId]) {
      return {
        success: false,
        message: '请勿重复点击',
        type: 'loading'
      }
    }

    // 记录点击
    recordClick(contentId)

    // 检查是否触发反作弊
    if (checkSpam(contentId)) {
      // 如果当前是已点赞状态,自动取消点赞
      if (currentLikeStatus) {
        try {
          loadingMap[contentId] = true
          await likeApiCall(contentId, userId)
        } catch (e) {
          console.error('取消点赞失败:', e)
        } finally {
          loadingMap[contentId] = false
        }
      }

      // 触发冷却
      triggerCooldown(contentId)

      return {
        success: false,
        message: '请勿重复操作,已进入冷却期',
        type: 'spam',
        forcedUnlike: currentLikeStatus
      }
    }

    // 正常执行点赞
    try {
      loadingMap[contentId] = true
      const result = await likeApiCall(contentId, userId)
      
      return {
        success: true,
        data: result,
        type: 'success'
      }
    } catch (error) {
      return {
        success: false,
        message: error.message || '操作失败',
        type: 'error'
      }
    } finally {
      loadingMap[contentId] = false
    }
  }

  // 检查是否正在加载
  const isLoading = (contentId) => {
    return loadingMap[contentId] || false
  }

  // 清理函数
  const cleanup = () => {
    timers.forEach(timer => clearInterval(timer))
    timers.clear()
  }

  // 组件卸载时清理
  onUnmounted(() => {
    cleanup()
  })

  // 初始化:加载冷却记录
  loadCooldownFromStorage()

  return {
    handleLike,
    isLoading,
    isCooldown,
    getCooldownRemaining,
  }
}
