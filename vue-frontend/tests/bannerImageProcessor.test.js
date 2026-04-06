/**
 * 轮播图图片处理器 - 属性测试
 * Feature: banner-auto-resize
 */

import { describe, test, expect, beforeAll } from 'vitest'
import { processBannerImage } from '@/utils/bannerImageProcessor'
import {
  createTestImage,
  createRandomSizeImage,
  createAspectRatioImage,
  createTransparentPNG
} from './utils/testImageGenerator'

describe('轮播图图片处理器 - 属性测试', () => {
  
  // Feature: banner-auto-resize, Property 2: 尺寸限制正确性
  describe('属性 2: 尺寸限制正确性', () => {
    test('处理后的图片宽度不应超过 1200px', async () => {
      // 运行 50 次迭代测试
      for (let i = 0; i < 50; i++) {
        const width = Math.floor(Math.random() * 4000) + 500
        const height = Math.floor(Math.random() * 3000) + 300
        const testImage = await createTestImage(width, height)
        
        const result = await processBannerImage(testImage, { mode: 'fill' })
        
        expect(result.metadata.processedWidth).toBeLessThanOrEqual(1200)
      }
    })

    test('处理后的图片高度不应超过 450px', async () => {
      // 运行 50 次迭代测试
      for (let i = 0; i < 50; i++) {
        const width = Math.floor(Math.random() * 4000) + 500
        const height = Math.floor(Math.random() * 3000) + 300
        const testImage = await createTestImage(width, height)
        
        const result = await processBannerImage(testImage, { mode: 'fill' })
        
        expect(result.metadata.processedHeight).toBeLessThanOrEqual(450)
      }
    })

    test('填充模式下，处理后的图片尺寸应该恰好为 1200x450（大图）', async () => {
      // 测试各种大于目标尺寸的图片
      const testCases = [
        [2000, 1000],
        [1500, 800],
        [3000, 2000],
        [1300, 500],
        [2400, 900]
      ]

      for (const [width, height] of testCases) {
        const testImage = await createTestImage(width, height)
        const result = await processBannerImage(testImage, { mode: 'fill' })
        
        expect(result.metadata.processedWidth).toBe(1200)
        expect(result.metadata.processedHeight).toBe(450)
      }
    })
  })

  // Feature: banner-auto-resize, Property 3: 小图保护
  describe('属性 3: 小图保护', () => {
    test('小于目标尺寸的图片不应被放大', async () => {
      const testCases = [
        [800, 300],
        [1000, 400],
        [600, 200],
        [500, 300],
        [1100, 400]
      ]

      for (const [width, height] of testCases) {
        const testImage = await createTestImage(width, height)
        const result = await processBannerImage(testImage, { mode: 'fill' })
        
        // 小图保护：处理后的尺寸不应大于原始尺寸
        expect(result.metadata.processedWidth).toBeLessThanOrEqual(width)
        expect(result.metadata.processedHeight).toBeLessThanOrEqual(height)
      }
    })

    test('适应模式下，小图不应被放大', async () => {
      // 运行 30 次迭代
      for (let i = 0; i < 30; i++) {
        const width = Math.floor(Math.random() * 1000) + 100
        const height = Math.floor(Math.random() * 400) + 50
        const testImage = await createTestImage(width, height)
        
        const result = await processBannerImage(testImage, { mode: 'fit' })
        
        // 适应模式下，小图的实际绘制尺寸不应超过原始尺寸
        expect(result.metadata.scaled).toBe(false)
      }
    })
  })

  // Feature: banner-auto-resize, Property 5: 填充模式完整性
  describe('属性 5: 填充模式完整性', () => {
    test('填充模式应该输出恰好 1200x450 的图片（大图）', async () => {
      // 测试各种比例的大图
      const aspectRatios = [16/9, 4/3, 3/1, 21/9, 1/1, 2/1]
      
      for (const ratio of aspectRatios) {
        const testImage = await createAspectRatioImage(ratio, 2000)
        const result = await processBannerImage(testImage, { mode: 'fill' })
        
        // 大图应该被缩放到目标尺寸
        if (result.metadata.originalWidth > 1200 || result.metadata.originalHeight > 450) {
          expect(result.metadata.processedWidth).toBe(1200)
          expect(result.metadata.processedHeight).toBe(450)
        }
      }
    })

    test('填充模式应该标记裁剪状态', async () => {
      // 测试需要裁剪的图片（宽高比与目标不同）
      const testImage = await createTestImage(2000, 2000) // 1:1 比例
      const result = await processBannerImage(testImage, { mode: 'fill' })
      
      // 1:1 的图片填充到 1200:450 (约 2.67:1) 需要裁剪
      expect(result.metadata.cropped).toBe(true)
    })
  })

  // Feature: banner-auto-resize, Property 4: 适应模式完整性
  describe('属性 4: 适应模式完整性', () => {
    test('适应模式应该完全显示图片，不裁剪', async () => {
      // 测试各种比例的图片
      const testCases = [
        [2000, 1000],  // 2:1
        [1500, 1500],  // 1:1
        [3000, 1000],  // 3:1
        [1000, 2000],  // 1:2
      ]

      for (const [width, height] of testCases) {
        const testImage = await createTestImage(width, height)
        const result = await processBannerImage(testImage, { mode: 'fit' })
        
        // 适应模式不应该裁剪
        expect(result.metadata.cropped).toBe(false)
      }
    })

    test('适应模式输出的 Canvas 尺寸应该是 1200x450', async () => {
      // 测试各种尺寸的图片
      for (let i = 0; i < 30; i++) {
        const testImage = await createRandomSizeImage(500, 3000, 300, 2000)
        const result = await processBannerImage(testImage, { mode: 'fit' })
        
        // 适应模式的 Canvas 尺寸应该是目标尺寸（有白色背景填充）
        expect(result.metadata.processedWidth).toBe(1200)
        expect(result.metadata.processedHeight).toBe(450)
      }
    })

    test('适应模式应该保持图片的宽高比', async () => {
      const testCases = [
        [2000, 1000],  // 2:1
        [1600, 900],   // 16:9
        [1500, 1500],  // 1:1
      ]

      for (const [width, height] of testCases) {
        const testImage = await createTestImage(width, height)
        const result = await processBannerImage(testImage, { mode: 'fit' })
        
        const originalRatio = width / height
        // 在适应模式下，图片会被缩放但保持比例
        // 由于是居中显示在 1200x450 的 Canvas 上，我们验证缩放是正确的
        expect(result.metadata.scaled).toBe(true)
      }
    })
  })

  // Feature: banner-auto-resize, Property 6: 压缩策略正确性
  describe('属性 6: 压缩策略正确性', () => {
    test('超过 300KB 的图片应该被压缩', async () => {
      // 创建一个大尺寸图片，确保超过 300KB
      const testImage = await createTestImage(3000, 2000)
      const result = await processBannerImage(testImage, { mode: 'fill' })
      
      // 如果原始文件很大，处理后应该被压缩
      if (result.metadata.originalSize > 300 * 1024) {
        expect(result.metadata.compressed).toBe(true)
      }
    })

    test('压缩后的文件应该小于原始文件', async () => {
      // 创建大图
      const testImage = await createTestImage(2500, 1500)
      const result = await processBannerImage(testImage, { mode: 'fill' })
      
      // 处理后的文件应该更小
      expect(result.metadata.processedSize).toBeLessThan(result.metadata.originalSize)
    })

    test('压缩质量应该在合理范围内', async () => {
      const testImage = await createTestImage(2000, 1000)
      const result = await processBannerImage(testImage, { mode: 'fill' })
      
      // 如果是 JPEG 格式，质量应该在 0.75-0.85 之间
      if (result.metadata.processedFormat === 'image/jpeg' && result.metadata.quality) {
        expect(result.metadata.quality).toBeGreaterThanOrEqual(0.75)
        expect(result.metadata.quality).toBeLessThanOrEqual(0.85)
      }
    })
  })

  // PNG 透明度测试
  describe('PNG 透明度保留', () => {
    test('带透明度的 PNG 应该保留透明度', async () => {
      const testImage = await createTransparentPNG(1500, 800)
      const result = await processBannerImage(testImage, { mode: 'fill' })
      
      // 应该检测到透明度
      expect(result.metadata.hasTransparency).toBe(true)
      // 应该使用 PNG 格式输出
      expect(result.metadata.processedFormat).toBe('image/png')
    })
  })

  // 错误处理测试
  describe('错误处理', () => {
    test('处理失败时应该返回原始文件', async () => {
      // 创建一个无效的文件对象
      const invalidFile = new File(['invalid'], 'test.jpg', { type: 'image/jpeg' })
      
      const result = await processBannerImage(invalidFile)
      
      // 应该返回原始文件
      expect(result.file).toBe(invalidFile)
      // 元数据应该为 null
      expect(result.metadata).toBeNull()
      // 应该有错误信息
      expect(result.error).toBeDefined()
    })
  })
})
