/**
 * 图片压缩工具
 * 使用 Canvas 进行前端图片压缩，减少上传带宽和存储空间
 */

/**
 * 压缩图片
 * @param {File} file - 原始图片文件
 * @param {Object} options - 压缩选项
 * @param {number} options.maxWidth - 最大宽度，默认 1920
 * @param {number} options.maxHeight - 最大高度，默认 1920
 * @param {number} options.quality - 压缩质量 0-1，默认 0.8
 * @param {number} options.maxSize - 最大文件大小(KB)，超过才压缩，默认 500KB
 * @returns {Promise<File>} - 压缩后的文件
 */
export async function compressImage(file, options = {}) {
  const {
    maxWidth = 1920,
    maxHeight = 1920,
    quality = 0.8,
    maxSize = 500 // KB
  } = options

  // 如果文件小于 maxSize，不压缩
  if (file.size <= maxSize * 1024) {
    return file
  }

  // 只处理图片
  if (!file.type.startsWith('image/')) {
    return file
  }

  // GIF 不压缩（会丢失动画）
  if (file.type === 'image/gif') {
    return file
  }

  return new Promise((resolve) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      const img = new Image()
      img.onload = () => {
        // 计算压缩后的尺寸
        let { width, height } = img
        
        if (width > maxWidth) {
          height = Math.round((height * maxWidth) / width)
          width = maxWidth
        }
        if (height > maxHeight) {
          width = Math.round((width * maxHeight) / height)
          height = maxHeight
        }

        // 创建 Canvas
        const canvas = document.createElement('canvas')
        canvas.width = width
        canvas.height = height
        
        const ctx = canvas.getContext('2d')
        ctx.fillStyle = '#ffffff'
        ctx.fillRect(0, 0, width, height)
        ctx.drawImage(img, 0, 0, width, height)

        // 转换为 Blob
        canvas.toBlob(
          (blob) => {
            if (!blob) {
              resolve(file) // 压缩失败，返回原文件
              return
            }
            
            // 创建新的 File 对象
            const compressedFile = new File([blob], file.name, {
              type: 'image/jpeg',
              lastModified: Date.now()
            })
            
            resolve(compressedFile)
          },
          'image/jpeg',
          quality
        )
      }
      img.onerror = () => resolve(file) // 加载失败，返回原文件
      img.src = e.target.result
    }
    reader.onerror = () => resolve(file)
    reader.readAsDataURL(file)
  })
}

/**
 * 批量压缩图片
 * @param {File[]} files - 图片文件数组
 * @param {Object} options - 压缩选项
 * @returns {Promise<File[]>} - 压缩后的文件数组
 */
export async function compressImages(files, options = {}) {
  return Promise.all(files.map(file => compressImage(file, options)))
}
