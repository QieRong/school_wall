/**
 * 图片工具函数模块
 * 统一管理图片加载失败时的 fallback 逻辑
 */

/**
 * 图片类型枚举
 */
export const ImageType = {
  AVATAR: 'avatar',  // 用户头像
  POST: 'post',       // 帖子图片
  COVER: 'cover'     // 封面图片
}

/**
 * 标准化图片 URL
 * 将完整 URL 转换为相对路径，确保在不同环境下都能正确访问
 * @param {string} url - 原始图片 URL
 * @param {string} type - 图片类型 ('avatar' | 'post' | 'cover')
 * @returns {string} 标准化后的图片 URL
 */
export function getImageUrl(url, type = ImageType.POST) {
  // 空值返回默认图片
  if (!url) {
    if (type === ImageType.AVATAR) return '/default.png'
    if (type === ImageType.COVER) return '/default_cover.jpg'
    return '/default.png'
  }

  // 如果是完整 URL（http://localhost:19090/files/...），提取相对路径
  if (url.includes('://')) {
    try {
      const urlObj = new URL(url)
      // 只取路径部分，如 /files/2/20260130/xxx.png
      const result = urlObj.pathname
      return result
    } catch (e) {
      // URL 解析失败，返回原 URL
      return url
    }
  }

  // 已经是相对路径，直接返回
  return url
}

/**
 * 获取头像 URL（便捷方法）
 * @param {string} avatarUrl - 头像 URL
 * @returns {string} 标准化后的头像 URL
 */
export function getAvatarUrl(avatarUrl) {
  return getImageUrl(avatarUrl, ImageType.AVATAR)
}

/**
 * 获取封面 URL（便捷方法）
 * @param {string} coverUrl - 封面 URL
 * @returns {string} 标准化后的封面 URL
 */
export function getCoverUrl(coverUrl) {
  return getImageUrl(coverUrl, ImageType.COVER)
}

/**
 * Fallback 图片路径映射
 */
const FALLBACK_MAP = {
  [ImageType.AVATAR]: '/default.png',
  [ImageType.POST]: '/图片加载失败.png'
}

/**
 * 获取图片的 fallback 路径
 * @param {string} type - 图片类型 ('avatar' | 'post')
 * @returns {string} fallback 图片路径
 */
export function getFallbackImage(type = ImageType.POST) {
  return FALLBACK_MAP[type] || FALLBACK_MAP[ImageType.POST]
}

/**
 * 为 img 元素添加错误处理
 * 防止 fallback 图片本身加载失败时触发无限循环
 * @param {Event} event - error 事件
 * @param {string} type - 图片类型 ('avatar' | 'post')
 */
export function handleImageError(event, type = ImageType.POST) {
  const img = event.target
  const fallbackUrl = getFallbackImage(type)
  
  // 检查当前 src 是否已经是任何 fallback 图片（防止二次错误）
  const allFallbacks = Object.values(FALLBACK_MAP)
  const isAlreadyFallback = allFallbacks.some(fb => img.src.endsWith(fb))
  
  if (isAlreadyFallback) {
    return
  }
  
  img.src = fallbackUrl
}
