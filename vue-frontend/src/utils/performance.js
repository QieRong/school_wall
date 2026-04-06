// File: vue-frontend/src/utils/performance.js
// 性能优化工具函数：防抖和节流

/**
 * 防抖函数 - 延迟执行，多次触发只执行最后一次
 * 适用场景：搜索输入、窗口resize、表单验证
 * @param {Function} fn - 要执行的函数
 * @param {number} delay - 延迟时间（毫秒）
 * @returns {Function} 防抖后的函数
 */
export function debounce(fn, delay = 300) {
  let timer = null
  return function (...args) {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      fn.apply(this, args)
    }, delay)
  }
}

/**
 * 节流函数 - 固定时间间隔执行
 * 适用场景：滚动加载、按钮点击、拖拽
 * @param {Function} fn - 要执行的函数
 * @param {number} delay - 时间间隔（毫秒）
 * @returns {Function} 节流后的函数
 */
export function throttle(fn, delay = 200) {
  let lastTime = 0
  return function (...args) {
    const now = Date.now()
    if (now - lastTime >= delay) {
      lastTime = now
      fn.apply(this, args)
    }
  }
}
