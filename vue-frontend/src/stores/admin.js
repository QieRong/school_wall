import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAdminStore = defineStore('admin', () => {
  // 侧边栏状态
  const sidebarCollapsed = ref(false)
  const toggleSidebar = () => {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  // 待处理事项统计
  const pendingAuditCount = ref(0)
  const pendingReportCount = ref(0)
  const notificationCount = ref(0)

  // 更新统计数据
  const updateStats = (stats) => {
    if (stats.pendingAudit !== undefined) pendingAuditCount.value = stats.pendingAudit
    if (stats.pendingReport !== undefined) pendingReportCount.value = stats.pendingReport
    if (stats.notifications !== undefined) notificationCount.value = stats.notifications
  }

  // 通知列表
  const notifications = ref([])
  const addNotification = (notification) => {
    notifications.value.unshift(notification)
    notificationCount.value++
    // 保持最近 50 条
    if (notifications.value.length > 50) {
      notifications.value.pop()
    }
  }

  const markAsRead = (id) => {
    const notification = notifications.value.find(n => n.id === id)
    if (notification && !notification.read) {
      notification.read = true
      notificationCount.value = Math.max(0, notificationCount.value - 1)
    }
  }

  const markAllAsRead = () => {
    notifications.value.forEach(n => n.read = true)
    notificationCount.value = 0
  }

  return {
    sidebarCollapsed,
    toggleSidebar,
    pendingAuditCount,
    pendingReportCount,
    notificationCount,
    notifications,
    updateStats,
    addNotification,
    markAsRead,
    markAllAsRead
  }
})
