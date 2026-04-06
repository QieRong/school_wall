/**
 * 轮播图图片处理器
 * 用于自动缩放、裁剪和压缩轮播图图片
 */

/**
 * 加载图片文件为 Image 对象
 * @param {File} file - 图片文件
 * @returns {Promise<HTMLImageElement>} - 加载完成的图片对象
 */
function loadImage(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    
    reader.onload = (e) => {
      const img = new Image()
      
      img.onload = () => resolve(img)
      img.onerror = () => reject(new Error('图片加载失败'))
      
      img.src = e.target.result
    }
    
    reader.onerror = () => reject(new Error('文件读取失败'))
    reader.readAsDataURL(file)
  })
}

/**
 * 检测浏览器是否支持 Canvas API
 * @returns {boolean} - 是否支持
 */
function isCanvasSupported() {
  try {
    const canvas = document.createElement('canvas')
    return !!(canvas.getContext && canvas.getContext('2d'))
  } catch (e) {
    return false
  }
}

/**
 * 将 Canvas 转换为 Blob
 * @param {HTMLCanvasElement} canvas - Canvas 元素
 * @param {string} type - 图片类型
 * @param {number} quality - 压缩质量 (0-1)
 * @returns {Promise<Blob>} - Blob 对象
 */
function canvasToBlob(canvas, type = 'image/jpeg', quality = 0.85) {
  return new Promise((resolve, reject) => {
    canvas.toBlob(
      (blob) => {
        if (blob) {
          resolve(blob)
        } else {
          reject(new Error('Canvas 转换失败'))
        }
      },
      type,
      quality
    )
  })
}

/**
 * 处理轮播图图片
 * @param {File} file - 原始图片文件
 * @param {Object} options - 处理选项
 * @param {number} options.targetWidth - 目标宽度，默认 1200
 * @param {number} options.targetHeight - 目标高度，默认 450
 * @param {string} options.mode - 处理模式 'fill' | 'fit'，默认 'fill'
 * @param {number} options.quality - 初始压缩质量，默认 0.85
 * @returns {Promise<Object>} - 处理结果
 */
export async function processBannerImage(file, options = {}) {
  const {
    targetWidth = 1200,
    targetHeight = 450,
    mode = 'fill',
    quality = 0.85
  } = options

  // 检查 Canvas 支持
  if (!isCanvasSupported()) {
    console.warn('浏览器不支持 Canvas API，将使用原图')
    return {
      file,
      previewUrl: URL.createObjectURL(file),
      metadata: null
    }
  }

  try {
    const startTime = Date.now()
    
    // 1. 加载图片
    const img = await loadImage(file)
    
    // 2. 获取原始尺寸
    const originalWidth = img.width
    const originalHeight = img.height
    const originalSize = file.size

    // 3. 计算处理后的尺寸
    let scale, outputWidth, outputHeight, scaled = false, cropped = false

    if (mode === 'fill') {
      // 填充模式：缩放并裁剪以填充整个区域
      // 计算缩放比例：选择能填充目标区域的最小缩放比例
      scale = Math.max(targetWidth / originalWidth, targetHeight / originalHeight)
      
      // 小图保护：如果原图已经小于目标尺寸，不放大
      if (originalWidth <= targetWidth && originalHeight <= targetHeight) {
        scale = 1
        outputWidth = originalWidth
        outputHeight = originalHeight
      } else {
        outputWidth = targetWidth
        outputHeight = targetHeight
      }
      
      // 判断是否需要缩放或裁剪
      if (scale !== 1) scaled = true
      if (originalWidth * scale > targetWidth || originalHeight * scale > targetHeight) {
        cropped = true
      }
    } else {
      // 适应模式：完全显示图片，不裁剪
      // 计算缩放比例：选择能完全显示的最大缩放比例，但不放大（最大为1）
      scale = Math.min(targetWidth / originalWidth, targetHeight / originalHeight, 1)
      outputWidth = Math.round(originalWidth * scale)
      outputHeight = Math.round(originalHeight * scale)
      
      if (scale !== 1) scaled = true
    }

    // 4. 创建 Canvas
    const canvas = document.createElement('canvas')
    
    if (mode === 'fill') {
      // 填充模式：输出尺寸为目标尺寸或原图尺寸（如果原图更小）
      canvas.width = outputWidth
      canvas.height = outputHeight
    } else {
      // 适应模式：Canvas 尺寸为目标尺寸，图片居中显示
      canvas.width = targetWidth
      canvas.height = targetHeight
    }
    
    const ctx = canvas.getContext('2d')
    
    // 5. 绘制图片
    if (mode === 'fill') {
      // 填充模式：居中裁剪
      const scaledWidth = originalWidth * scale
      const scaledHeight = originalHeight * scale
      
      // 白色背景
      ctx.fillStyle = '#ffffff'
      ctx.fillRect(0, 0, canvas.width, canvas.height)
      
      // 如果是小图保护，直接居中绘制原图
      if (scale === 1 && originalWidth <= targetWidth && originalHeight <= targetHeight) {
        const x = (canvas.width - originalWidth) / 2
        const y = (canvas.height - originalHeight) / 2
        ctx.drawImage(img, x, y, originalWidth, originalHeight)
      } else {
        // 正常填充：居中裁剪
        const x = (canvas.width - scaledWidth) / 2
        const y = (canvas.height - scaledHeight) / 2
        ctx.drawImage(img, x, y, scaledWidth, scaledHeight)
      }
    } else {
      // 适应模式：白色背景 + 居中显示，图片完全可见
      ctx.fillStyle = '#ffffff'
      ctx.fillRect(0, 0, canvas.width, canvas.height)
      
      // 计算居中位置
      const x = (targetWidth - outputWidth) / 2
      const y = (targetHeight - outputHeight) / 2
      
      // 绘制缩放后的图片
      ctx.drawImage(img, x, y, outputWidth, outputHeight)
    }

    // 6. 检测是否需要保留透明度（PNG 格式）
    const isPNG = file.type === 'image/png'
    let outputType = 'image/jpeg'
    let hasTransparency = false
    
    // 检测 PNG 是否有透明通道
    if (isPNG) {
      // 创建临时 canvas 检测透明度
      const tempCanvas = document.createElement('canvas')
      tempCanvas.width = 1
      tempCanvas.height = 1
      const tempCtx = tempCanvas.getContext('2d')
      tempCtx.drawImage(img, 0, 0, 1, 1)
      const imageData = tempCtx.getImageData(0, 0, 1, 1)
      hasTransparency = imageData.data[3] < 255
      
      // 如果有透明度，使用 PNG 格式输出
      if (hasTransparency) {
        outputType = 'image/png'
      }
    }

    // 7. 转换为 Blob（初始质量）
    let blob
    let currentQuality = quality
    let compressed = false
    
    if (outputType === 'image/png') {
      // PNG 格式不支持质量参数，直接转换
      blob = await canvasToBlob(canvas, 'image/png')
    } else {
      // JPEG 格式使用质量参数
      blob = await canvasToBlob(canvas, 'image/jpeg', quality)
    }

    // 8. 动态压缩（仅对 JPEG 格式）
    if (outputType === 'image/jpeg') {
      // 如果超过 300KB，应用压缩
      if (blob.size > 300 * 1024) {
        blob = await canvasToBlob(canvas, 'image/jpeg', 0.85)
        currentQuality = 0.85
        compressed = true
      }

      // 如果还超过 500KB，降低质量再次压缩
      if (blob.size > 500 * 1024) {
        blob = await canvasToBlob(canvas, 'image/jpeg', 0.75)
        currentQuality = 0.75
        compressed = true
      }
    }

    // 9. 创建新的 File 对象
    const processedFile = new File([blob], file.name, {
      type: outputType,
      lastModified: Date.now()
    })

    // 10. 生成预览 URL
    const previewUrl = URL.createObjectURL(blob)

    // 11. 计算处理时间
    const processingTime = Date.now() - startTime

    // 12. 返回处理结果
    return {
      file: processedFile,
      previewUrl,
      metadata: {
        originalWidth,
        originalHeight,
        originalSize,
        originalFormat: file.type,
        processedWidth: canvas.width,
        processedHeight: canvas.height,
        processedSize: processedFile.size,
        processedFormat: outputType,
        mode,
        scaled,
        cropped,
        compressed,
        hasTransparency,
        quality: outputType === 'image/jpeg' ? currentQuality : null,
        processingTime
      }
    }

  } catch (error) {
    console.error('图片处理失败:', error)
    
    // 失败时返回原图
    return {
      file,
      previewUrl: URL.createObjectURL(file),
      metadata: null,
      error: error.message
    }
  }
}

/**
 * 批量处理轮播图图片
 * @param {File[]} files - 图片文件数组
 * @param {Object} options - 处理选项
 * @returns {Promise<Object[]>} - 处理结果数组
 */
export async function processBannerImages(files, options = {}) {
  return Promise.all(files.map(file => processBannerImage(file, options)))
}
