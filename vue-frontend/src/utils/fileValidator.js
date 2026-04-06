/**
 * 文件验证工具
 * 用于前端文件上传前的安全验证
 */

/**
 * 验证图片文件
 * @param {File} file - 要验证的文件对象
 * @returns {{valid: boolean, error?: string}} 验证结果
 */
export const validateImageFile = (file) => {
  // 1. 检查文件是否存在
  if (!file) {
    return { valid: false, error: '请选择文件' }
  }
  
  // 2. 检查文件大小（10MB）
  const maxSize = 10 * 1024 * 1024
  if (file.size > maxSize) {
    return { valid: false, error: '图片大小不能超过10MB' }
  }
  
  // 3. 检查MIME类型
  const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
  if (!allowedTypes.includes(file.type)) {
    return { valid: false, error: '只支持JPG、PNG、GIF、WebP格式' }
  }
  
  // 4. 检查文件扩展名
  const fileName = file.name.toLowerCase()
  const allowedExts = ['.jpg', '.jpeg', '.png', '.gif', '.webp']
  const hasValidExt = allowedExts.some(ext => fileName.endsWith(ext))
  if (!hasValidExt) {
    return { valid: false, error: '文件扩展名不合法' }
  }
  
  // 5. 检查文件名安全性（防止路径遍历）
  if (fileName.includes('..') || fileName.includes('/') || fileName.includes('\\')) {
    return { valid: false, error: '文件名包含非法字符' }
  }
  
  return { valid: true }
}

/**
 * 验证视频文件
 * @param {File} file - 要验证的文件对象
 * @returns {{valid: boolean, error?: string}} 验证结果
 */
export const validateVideoFile = (file) => {
  if (!file) {
    return { valid: false, error: '请选择文件' }
  }
  
  // 视频文件限制50MB
  const maxSize = 50 * 1024 * 1024
  if (file.size > maxSize) {
    return { valid: false, error: '视频大小不能超过50MB' }
  }
  
  const allowedTypes = ['video/mp4', 'video/webm', 'video/ogg']
  if (!allowedTypes.includes(file.type)) {
    return { valid: false, error: '只支持MP4、WebM、OGG格式' }
  }
  
  const fileName = file.name.toLowerCase()
  const allowedExts = ['.mp4', '.webm', '.ogg']
  const hasValidExt = allowedExts.some(ext => fileName.endsWith(ext))
  if (!hasValidExt) {
    return { valid: false, error: '文件扩展名不合法' }
  }
  
  if (fileName.includes('..') || fileName.includes('/') || fileName.includes('\\')) {
    return { valid: false, error: '文件名包含非法字符' }
  }
  
  return { valid: true }
}
