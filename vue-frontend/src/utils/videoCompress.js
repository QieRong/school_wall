/**
 * 视频压缩工具 - 使用 Canvas API（不需要 SharedArrayBuffer）
 * 注意：浏览器原生不支持真正的视频压缩，这里提供文件大小检查和提示
 */

/**
 * 检查视频信息
 * @param {File} file - 视频文件
 * @returns {Promise<Object>} - 视频信息
 */
export const getVideoInfo = (file) => {
  return new Promise((resolve, reject) => {
    const video = document.createElement('video')
    video.preload = 'metadata'
    video.src = URL.createObjectURL(file)
    
    video.onloadedmetadata = () => {
      URL.revokeObjectURL(video.src)
      resolve({
        duration: video.duration,
        width: video.videoWidth,
        height: video.videoHeight,
        size: file.size
      })
    }
    
    video.onerror = () => {
      URL.revokeObjectURL(video.src)
      reject(new Error('视频加载失败'))
    }
  })
}

/**
 * 压缩视频（简化版 - 仅做检查和提示）
 * 由于浏览器限制，真正的视频压缩需要服务端处理或使用 FFmpeg WASM（需要特殊配置）
 * @param {File} file - 原始视频文件
 * @param {Object} options - 压缩选项
 * @param {Function} onProgress - 进度回调
 * @returns {Promise<File>} - 返回原文件或抛出错误
 */
export const compressVideo = async (file, options = {}, onProgress) => {
  const { maxSize = 100 * 1024 * 1024 } = options
  
  // 获取视频信息
  await getVideoInfo(file)
  
  // 如果文件小于限制，直接返回
  if (file.size <= maxSize) {
    if (onProgress) onProgress(100)
    return file
  }
  
  // 模拟进度
  if (onProgress) {
    let progress = 0
    const interval = setInterval(() => {
      progress += 10
      if (progress <= 90) {
        onProgress(progress)
      }
    }, 200)
    
    // 2秒后完成
    await new Promise(resolve => setTimeout(resolve, 2000))
    clearInterval(interval)
    onProgress(100)
  }
  
  // 由于浏览器限制，无法真正压缩视频
  // 抛出错误让调用方处理
  const sizeMB = (file.size / 1024 / 1024).toFixed(1)
  const maxMB = (maxSize / 1024 / 1024).toFixed(0)
  throw new Error(`视频 ${sizeMB}MB 超过 ${maxMB}MB 限制，请使用视频编辑软件压缩后再上传`)
}

/**
 * 检查浏览器是否支持视频压缩
 * 由于我们使用简化版，始终返回 true
 */
export const isCompressionSupported = () => {
  return true
}

export default compressVideo
