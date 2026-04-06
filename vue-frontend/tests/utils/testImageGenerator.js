/**
 * 测试图片生成器
 * 用于生成各种尺寸和格式的测试图片
 */

/**
 * 创建指定尺寸的测试图片
 * @param {number} width - 宽度
 * @param {number} height - 高度
 * @param {string} color - 颜色（可选）
 * @param {string} format - 格式 'image/jpeg' | 'image/png'
 * @returns {Promise<File>} - 测试图片文件
 */
export async function createTestImage(width, height, color = '#4CAF50', format = 'image/jpeg') {
  return new Promise((resolve) => {
    const canvas = document.createElement('canvas')
    canvas.width = width
    canvas.height = height
    
    const ctx = canvas.getContext('2d')
    
    // 填充背景色
    ctx.fillStyle = color
    ctx.fillRect(0, 0, width, height)
    
    // 添加一些文字标识尺寸
    ctx.fillStyle = '#ffffff'
    ctx.font = 'bold 20px Arial'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText(`${width}x${height}`, width / 2, height / 2)
    
    canvas.toBlob((blob) => {
      const file = new File([blob], `test-${width}x${height}.jpg`, {
        type: format,
        lastModified: Date.now()
      })
      resolve(file)
    }, format, 0.9)
  })
}

/**
 * 创建带透明度的 PNG 测试图片
 * @param {number} width - 宽度
 * @param {number} height - 高度
 * @returns {Promise<File>} - 测试图片文件
 */
export async function createTransparentPNG(width, height) {
  return new Promise((resolve) => {
    const canvas = document.createElement('canvas')
    canvas.width = width
    canvas.height = height
    
    const ctx = canvas.getContext('2d')
    
    // 创建半透明渐变
    const gradient = ctx.createLinearGradient(0, 0, width, height)
    gradient.addColorStop(0, 'rgba(76, 175, 80, 0.8)')
    gradient.addColorStop(1, 'rgba(33, 150, 243, 0.5)')
    
    ctx.fillStyle = gradient
    ctx.fillRect(0, 0, width, height)
    
    canvas.toBlob((blob) => {
      const file = new File([blob], `transparent-${width}x${height}.png`, {
        type: 'image/png',
        lastModified: Date.now()
      })
      resolve(file)
    }, 'image/png')
  })
}

/**
 * 创建随机尺寸的测试图片
 * @param {number} minWidth - 最小宽度
 * @param {number} maxWidth - 最大宽度
 * @param {number} minHeight - 最小高度
 * @param {number} maxHeight - 最大高度
 * @returns {Promise<File>} - 测试图片文件
 */
export async function createRandomSizeImage(
  minWidth = 100,
  maxWidth = 5000,
  minHeight = 100,
  maxHeight = 5000
) {
  const width = Math.floor(Math.random() * (maxWidth - minWidth + 1)) + minWidth
  const height = Math.floor(Math.random() * (maxHeight - minHeight + 1)) + minHeight
  return createTestImage(width, height)
}

/**
 * 创建指定比例的测试图片
 * @param {number} aspectRatio - 宽高比（宽度/高度）
 * @param {number} baseSize - 基准尺寸
 * @returns {Promise<File>} - 测试图片文件
 */
export async function createAspectRatioImage(aspectRatio, baseSize = 1000) {
  const width = Math.round(baseSize * Math.sqrt(aspectRatio))
  const height = Math.round(baseSize / Math.sqrt(aspectRatio))
  return createTestImage(width, height)
}
