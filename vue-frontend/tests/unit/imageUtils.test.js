/**
 * 图片工具函数单元测试
 */
import { describe, it, expect } from 'vitest'
import { getFallbackImage, handleImageError, ImageType } from '@/utils/imageUtils'

describe('imageUtils', () => {
  describe('getFallbackImage', () => {
    it('应该为 avatar 类型返回 /default.png', () => {
      const result = getFallbackImage(ImageType.AVATAR)
      expect(result).toBe('/default.png')
    })

    it('应该为 post 类型返回 /图片加载失败.png', () => {
      const result = getFallbackImage(ImageType.POST)
      expect(result).toBe('/图片加载失败.png')
    })

    it('应该为字符串 "avatar" 返回 /default.png', () => {
      const result = getFallbackImage('avatar')
      expect(result).toBe('/default.png')
    })

    it('应该为字符串 "post" 返回 /图片加载失败.png', () => {
      const result = getFallbackImage('post')
      expect(result).toBe('/图片加载失败.png')
    })

    it('不传参数时应该返回默认值 /图片加载失败.png', () => {
      const result = getFallbackImage()
      expect(result).toBe('/图片加载失败.png')
    })

    it('传入无效类型时应该返回默认值 /图片加载失败.png', () => {
      const result = getFallbackImage('invalid-type')
      expect(result).toBe('/图片加载失败.png')
    })
  })

  describe('handleImageError', () => {
    it('应该将图片 src 设置为对应类型的 fallback', () => {
      const mockImg = { src: 'http://example.com/image.jpg' }
      const mockEvent = { target: mockImg }

      handleImageError(mockEvent, ImageType.POST)
      expect(mockImg.src).toBe('/图片加载失败.png')
    })

    it('应该为 avatar 类型设置正确的 fallback', () => {
      const mockImg = { src: 'http://example.com/avatar.jpg' }
      const mockEvent = { target: mockImg }

      handleImageError(mockEvent, ImageType.AVATAR)
      expect(mockImg.src).toBe('/default.png')
    })

    it('不传类型参数时应该使用默认的 post 类型', () => {
      const mockImg = { src: 'http://example.com/image.jpg' }
      const mockEvent = { target: mockImg }

      handleImageError(mockEvent)
      expect(mockImg.src).toBe('/图片加载失败.png')
    })

    it('当 src 已经是 fallback 图片时不应该再次修改（防止二次错误）', () => {
      const mockImg = { src: 'http://localhost:3000/default.png' }
      const mockEvent = { target: mockImg }
      const originalSrc = mockImg.src

      handleImageError(mockEvent, ImageType.AVATAR)
      expect(mockImg.src).toBe(originalSrc)
    })

    it('当 src 已经是帖子 fallback 图片时不应该再次修改', () => {
      const mockImg = { src: 'http://localhost:3000/图片加载失败.png' }
      const mockEvent = { target: mockImg }
      const originalSrc = mockImg.src

      handleImageError(mockEvent, ImageType.POST)
      expect(mockImg.src).toBe(originalSrc)
    })
  })

  describe('ImageType 常量', () => {
    it('应该导出 AVATAR 常量', () => {
      expect(ImageType.AVATAR).toBe('avatar')
    })

    it('应该导出 POST 常量', () => {
      expect(ImageType.POST).toBe('post')
    })
  })
})
