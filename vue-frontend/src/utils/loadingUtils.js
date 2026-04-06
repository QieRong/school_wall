/**
 * 加载状态优化工具
 * 确保最小加载时间，避免加载动画闪烁
 */

/**
 * 带最小加载时间的数据加载函数
 * @param {Function} fetchFn - 数据获取函数
 * @param {number} minTime - 最小加载时间（毫秒），默认500ms
 * @returns {Promise} 返回获取的数据
 */
export async function loadDataWithMinTime(fetchFn, minTime = 500) {
  const startTime = Date.now()
  
  // 执行数据获取
  const result = await fetchFn()
  
  // 计算实际耗时
  const elapsed = Date.now() - startTime
  const remaining = Math.max(0, minTime - elapsed)
  
  // 如果加载太快，等待剩余时间
  if (remaining > 0) {
    await new Promise(resolve => setTimeout(resolve, remaining))
  }
  
  return result
}

/**
 * 创建带加载状态的函数
 * @param {Ref<boolean>} loadingRef - Vue ref，用于表示加载状态
 * @param {Function} fetchFn - 数据获取函数
 * @param {number} minTime - 最小加载时间（毫秒）
 */
export async function withLoading(loadingRef, fetchFn, minTime = 500) {
  loadingRef.value = true
  try {
    return await loadDataWithMinTime(fetchFn, minTime)
  } finally {
    loadingRef.value = false
  }
}

export default {
  loadDataWithMinTime,
  withLoading
}
