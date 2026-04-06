/**
 * 时间格式化工具函数
 * 符合中国用户习惯的时间显示
 */

/**
 * 格式化时间为相对时间或绝对时间
 * @param {string|Date} time - 时间字符串或Date对象
 * @returns {string} 格式化后的时间字符串
 */
export const formatTime = (time) => {
  if (!time) return ''
  
  const date = new Date(time)
  const now = new Date()
  
  // 计算时间差（毫秒）
  const diff = now.getTime() - date.getTime()
  
  // 小于1分钟：显示"刚刚"
  if (diff < 60 * 1000) {
    return '刚刚'
  }
  
  // 小于1小时：显示"X分钟前"
  if (diff < 60 * 60 * 1000) {
    const minutes = Math.floor(diff / (60 * 1000))
    return `${minutes}分钟前`
  }
  
  // 小于24小时：显示"X小时前"
  if (diff < 24 * 60 * 60 * 1000) {
    const hours = Math.floor(diff / (60 * 60 * 1000))
    return `${hours}小时前`
  }
  
  // 判断是否是昨天
  const yesterday = new Date(now)
  yesterday.setDate(yesterday.getDate() - 1)
  if (
    date.getDate() === yesterday.getDate() &&
    date.getMonth() === yesterday.getMonth() &&
    date.getFullYear() === yesterday.getFullYear()
  ) {
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    return `昨天 ${hours}:${minutes}`
  }
  
  // 超过2天：显示"MM-DD HH:mm"
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  
  // 如果是今年，不显示年份
  if (date.getFullYear() === now.getFullYear()) {
    return `${month}-${day} ${hours}:${minutes}`
  }
  
  // 如果不是今年，显示完整日期
  return `${date.getFullYear()}-${month}-${day} ${hours}:${minutes}`
}

/**
 * 格式化为完整日期时间
 * @param {string|Date} time - 时间字符串或Date对象
 * @returns {string} 格式化后的完整时间字符串
 */
export const formatFullTime = (time) => {
  if (!time) return ''
  
  const date = new Date(time)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

/**
 * 格式化为日期（不含时间）
 * @param {string|Date} time - 时间字符串或Date对象
 * @returns {string} 格式化后的日期字符串
 */
export const formatDate = (time) => {
  if (!time) return ''
  
  const date = new Date(time)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  
  return `${year}-${month}-${day}`
}

/**
 * 格式化为时间（不含日期）
 * @param {string|Date} time - 时间字符串或Date对象
 * @returns {string} 格式化后的时间字符串
 */
export const formatTimeOnly = (time) => {
  if (!time) return ''
  
  const date = new Date(time)
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  
  return `${hours}:${minutes}`
}

/**
 * 格式化为相对日期（用于列表显示）
 * @param {string|Date} time - 时间字符串或Date对象
 * @returns {string} 格式化后的相对日期字符串
 */
export const formatRelativeDate = (time) => {
  if (!time) return ''
  
  const date = new Date(time)
  const now = new Date()
  
  // 计算天数差
  const diffDays = Math.floor((now.getTime() - date.getTime()) / (24 * 60 * 60 * 1000))
  
  if (diffDays === 0) {
    return '今天'
  } else if (diffDays === 1) {
    return '昨天'
  } else if (diffDays === 2) {
    return '前天'
  } else if (diffDays < 7) {
    return `${diffDays}天前`
  } else {
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${month}-${day}`
  }
}
