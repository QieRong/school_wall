// File: vue-frontend/src/stores/ws.js
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useAppStore } from './app'
import { logger } from '@/utils/logger'

export const useWsStore = defineStore('ws', () => {
  const socket = ref(null)
  const isConnected = ref(false)
  const unreadCount = ref(0)  // 系统通知未读数
  const chatUnreadCount = ref(0)  // 【新增】私信未读数
  const appStore = useAppStore()
  
  let pingInterval = null
  let reconnectTimer = null
  let reconnectCount = 0
  let pongTimeout = null  // 【新增】心跳超时检测
  let lastToken = null    // 保存token用于重连
  const maxReconnectCount = 10  // 【优化】增加最大重连次数到10次
  
  // 音频对象
  const soundEnabled = ref(true)

  // 初始化音频 (仅用于触发浏览器音频上下文，可选)
  const initAudio = () => {
    const saved = localStorage.getItem('notificationSoundEnabled')
    if (saved !== null) {
      soundEnabled.value = saved === 'true'
    }
    // 预加载一个静音片段或空操作来解锁
    try {
      const audio = new Audio('/notification.mp3')
      audio.volume = 0
      audio.play().then(() => {
        audio.pause()
        audio.currentTime = 0
      }).catch(e => {})
    } catch {}
  }

  // 播放提示音
  const playNotificationSound = () => {
    // 每次播放前重新检查开关状态
    const saved = localStorage.getItem('notificationSoundEnabled')
    const isEnabled = saved !== null ? saved === 'true' : true
    
    if (!isEnabled) return

    try {
      // 每次创建新的音频对象，避免AudioContext被回收或冻结
      const audio = new Audio('/notification.mp3')
      audio.volume = 0.5
      const promise = audio.play()
      
      if (promise !== undefined) {
        promise.catch(error => {
          console.warn('自动播放被阻止或音频解码失败:', error)
          // 可以尝试再次交互后播放，或者记录日志
        })
      }
    } catch (e) {
      console.error('播放提示音异常:', e)
    }
  }

  const connect = (token) => {
    if (socket.value) return
    lastToken = token  // 保存token

    // 【修复】使用相对路径，通过Vite代理连接WebSocket
    // 开发环境：ws://localhost:5173/ws/{token} -> 代理到 -> ws://localhost:19090/ws/{token}
    // 生产环境：需要配置正确的WebSocket地址
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.host
    const wsUrl = `${protocol}//${host}/ws/${token}`
    
    logger.log('WebSocket连接URL:', wsUrl)
    socket.value = new WebSocket(wsUrl)

    socket.value.onopen = () => {
      isConnected.value = true
      reconnectCount = 0
      startHeartbeat()
      initAudio() // 初始化音频
      // 【优化】重连成功后提示
      if (reconnectCount > 0) {
        appStore.showToast('✅ 已重新连接', 'success')
      }
    }

    socket.value.onclose = () => {
      isConnected.value = false
      socket.value = null
      stopHeartbeat()
      // 只在非主动断开时显示提示和重连
      if (lastToken) {
        if (reconnectCount === 0) {
          appStore.showToast('📡 连接已断开，正在重连...', 'warning')
        }
        tryReconnect(token)
      }
    }

    socket.value.onerror = () => {
      // WebSocket错误，等待重连
    }

    socket.value.onmessage = (event) => {
      if (event.data === 'pong') {
        // 【修复】收到pong，清除超时定时器
        if (pongTimeout) {
          clearTimeout(pongTimeout)
          pongTimeout = null
        }
        return
      }
      try {
        const msg = JSON.parse(event.data)
        handleMessage(msg)
      } catch (e) {}
    }
  }

  // 心跳检测: 每30秒发送 ping
  const startHeartbeat = () => {
    stopHeartbeat()
    pingInterval = setInterval(() => {
      if (socket.value && socket.value.readyState === WebSocket.OPEN) {
        socket.value.send('ping')
        
        // 【修复】设置超时检测，5秒内没收到pong则认为连接断开
        pongTimeout = setTimeout(() => {
          if (socket.value) {
            socket.value.close()
          }
        }, 5000)
      }
    }, 30000)
  }

  const stopHeartbeat = () => {
    if (pingInterval) clearInterval(pingInterval)
    if (pongTimeout) clearTimeout(pongTimeout)
  }

  // 【优化】断线重连: 最多10次，并提供手动重连
  const tryReconnect = (token) => {
    if (reconnectCount >= maxReconnectCount) {
      // 【新增】重连失败后提示用户，并提供手动重连选项
      appStore.showToast('❌ 连接失败，请检查网络或刷新页面重试', 'error')
      return
    }
    reconnectCount++
    const delay = Math.min(reconnectCount * 2000, 10000) // 递增延迟，最多10秒
    
    // 【优化】显示重连进度
    if (reconnectCount > 1) {
      appStore.showToast(`🔄 正在重连 (${reconnectCount}/${maxReconnectCount})...`, 'info')
    }
    
    reconnectTimer = setTimeout(() => connect(token), delay)
  }

  // 【新增】手动重连功能
  const manualReconnect = () => {
    if (lastToken) {
      reconnectCount = 0  // 重置重连次数
      if (socket.value) {
        socket.value.close()
      }
      connect(lastToken)
      appStore.showToast('🔄 正在重新连接...', 'info')
    } else {
      appStore.showToast('⚠️ 无法重连，请刷新页面', 'warning')
    }
  }

  // 断开连接（静默断开，不显示提示）
  const disconnect = () => {
    stopHeartbeat()
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    if (socket.value) {
      // 移除onclose监听，避免触发重连和提示
      socket.value.onclose = null
      socket.value.close()
      socket.value = null
    }
    isConnected.value = false
    reconnectCount = 0
    lastToken = null
  }

  // 故事链事件监听器
  const storyListeners = ref([])

  const addStoryListener = (callback) => {
    storyListeners.value.push(callback)
    return () => {
      storyListeners.value = storyListeners.value.filter(cb => cb !== callback)
    }
  }

  const handleMessage = (msg) => {
    if (msg.type === 'NOTICE') {
      unreadCount.value++
      appStore.showToast(`🔔 ${msg.content}`, 'info')
      playNotificationSound()
    } else if (msg.type === 'CHAT') {
      chatUnreadCount.value++
      appStore.showToast(`💌 收到私信: ${msg.data.content}`, 'info')
      playNotificationSound()
    } else if (msg.type === 'ANNOUNCEMENT') {
      // 新公告通知
      unreadCount.value++
      appStore.showToast(`📢 新公告: ${msg.title}`, 'info')
      playNotificationSound()
    }
    // 故事链相关消息
    else if (msg.type === 'STORY_NEW_PARAGRAPH') {
      appStore.showToast(`📝 故事有新段落: ${msg.data.storyTitle}`, 'info')
      storyListeners.value.forEach(cb => cb(msg))
    } else if (msg.type === 'STORY_LIKE_UPDATE') {
      storyListeners.value.forEach(cb => cb(msg))
    } else if (msg.type === 'STORY_FINISHED') {
      appStore.showToast(`🎉 故事已完结: ${msg.data.storyTitle}`, 'success')
      storyListeners.value.forEach(cb => cb(msg))
    } else if (msg.type === 'STORY_ACHIEVEMENT') {
      // appStore.showToast(`🏆 恭喜获得成就: ${msg.data.achievementName}`, 'success')
      storyListeners.value.forEach(cb => cb(msg))
    }
  }

  return { 
    connect, 
    disconnect,
    manualReconnect,
    isConnected, 
    unreadCount,  // 系统通知未读数
    chatUnreadCount,  // 【新增】私信未读数
    addStoryListener,
    initAudio, // 导出供外部初始化（如首次点击后）
    playNotificationSound // 导出供外部未读测试
  }
})