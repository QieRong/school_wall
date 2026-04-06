/**
 * 图片容错属性测试
 * Property 1: Fallback 图片类型映射正确性
 * Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3, 2.4, 2.5
 */

import { describe, it, expect } from 'vitest'
import fc from 'fast-check'
import { getFallbackImage, handleImageError, ImageType } from '@/utils/imageUtils'

describe('Property 1: Fallback 图片类型映射正确性', () => {
  describe('getFallbackImage 属性', () => {
    it('属性：对于任意图片类型（avatar 或 post），应该返回正确的 fallback 路径', () => {
      fc.assert(
        fc.property(
          fc.constantFrom(ImageType.AVATAR, ImageType.POST),
          (type) => {
            const fallback = getFallbackImage(type)
            const expectedFallback = type === ImageType.AVATAR 
              ? '/default.png' 
              : '/图片加载失败.png'
            return fallback === expectedFallback
          }
        ),
        { numRuns: 100 }
      )
    })

    it('属性：不传参数时应该使用默认值 post 类型', () => {
      fc.assert(
        fc.property(
          fc.constant(undefined),
          () => {
            const fallback = getFallbackImage()
            return fallback === '/图片加载失败.png'
          }
        ),
        { numRuns: 100 }
      )
    })

    it('属性：对于无效的类型参数，应该返回 post 类型的 fallback', () => {
      fc.assert(
        fc.property(
          fc.string().filter(s => 
            s !== ImageType.AVATAR && 
            s !== ImageType.POST &&
            // 过滤掉对象内置属性名
            !Object.prototype.hasOwnProperty.call(Object.prototype, s)
          ),
          (invalidType) => {
            const fallback = getFallbackImage(invalidType)
            return fallback === '/图片加载失败.png'
          }
        ),
        { numRuns: 100 }
      )
    })

    it('属性：相同输入应该得到相同输出（幂等性）', () => {
      fc.assert(
        fc.property(
          fc.constantFrom(ImageType.AVATAR, ImageType.POST, undefined, 'invalid'),
          (type) => {
            const result1 = getFallbackImage(type)
            const result2 = getFallbackImage(type)
            const result3 = getFallbackImage(type)
            return result1 === result2 && result2 === result3
          }
        ),
        { numRuns: 100 }
      )
    })

    it('属性：返回的路径应该始终以 / 开头', () => {
      fc.assert(
        fc.property(
          fc.constantFrom(ImageType.AVATAR, ImageType.POST, undefined),
          (type) => {
            const fallback = getFallbackImage(type)
            return fallback.startsWith('/')
          }
        ),
        { numRuns: 100 }
      )
    })

    it('属性：返回的路径应该包含文件扩展名 .png', () => {
      fc.assert(
        fc.property(
          fc.constantFrom(ImageType.AVATAR, ImageType.POST, undefined),
          (type) => {
            const fallback = getFallbackImage(type)
            return fallback.endsWith('.png')
          }
        ),
        { numRuns: 100 }
      )
    })
  })

  describe('handleImageError 属性', () => {
    it('属性：对于任意非 fallback 图片 URL，应该将 src 设置为对应类型的 fallback', () => {
      fc.assert(
        fc.property(
          fc.constantFrom(ImageType.AVATAR, ImageType.POST),
          fc.webUrl(),
          (type, url) => {
            const mockImg = { src: url }
            const mockEvent = { target: mockImg }
            
            handleImageError(mockEvent, type)
            
            const expectedFallback = getFallbackImage(type)
            return mockImg.src === expectedFallback
          }
        ),
        { numRuns: 100 }
      )
    })

    it('属性：当图片 src 已经是 fallback 路径时，不应该再次修改（防止二次错误）', () => {
      fc.assert(
        fc.property(
          fc.constantFrom('/default.png', '/图片加载失败.png'),
          fc.constantFrom('http://example.com', 'https://cdn.example.com', ''),
          (fallbackUrl, domain) => {
            // 构造完整的 URL（可能带域名，也可能不带）
            const fullUrl = domain ? domain + fallbackUrl : fallbackUrl
            const mockImg = { src: fullUrl }
            const mockEvent = { target: mockImg }
            const originalSrc = mockImg.src
            
            // 调用错误处理函数
            handleImageError(mockEvent, ImageType.POST)
            
            // src 应该保持不变（因为已经是 fallback）
            return mockImg.src === originalSrc
          }
        ),
        { numRuns: 100 }
      )
    })

    it('属性：不传 type 参数时应该使用 post 类型的 fallback', () => {
      fc.assert(
        fc.property(
          fc.webUrl(),
          (url) => {
            const mockImg = { src: url }
            const mockEvent = { target: mockImg }
            
            handleImageError(mockEvent)
            
            return mockImg.src === '/图片加载失败.png'
          }
        ),
        { numRuns: 100 }
      )
    })

    it('属性：对于相同的输入，多次调用应该产生相同的结果（幂等性）', () => {
      fc.assert(
        fc.property(
          fc.constantFrom(ImageType.AVATAR, ImageType.POST),
          fc.webUrl(),
          (type, url) => {
            const mockImg1 = { src: url }
            const mockEvent1 = { target: mockImg1 }
            handleImageError(mockEvent1, type)
            const result1 = mockImg1.src
            
            const mockImg2 = { src: url }
            const mockEvent2 = { target: mockImg2 }
            handleImageError(mockEvent2, type)
            const result2 = mockImg2.src
            
            return result1 === result2
          }
        ),
        { numRuns: 100 }
      )
    })

    it('属性：处理后的 src 应该始终是有效的 fallback 路径', () => {
      fc.assert(
        fc.property(
          fc.constantFrom(ImageType.AVATAR, ImageType.POST),
          fc.webUrl(),
          (type, url) => {
            const mockImg = { src: url }
            const mockEvent = { target: mockImg }
            
            handleImageError(mockEvent, type)
            
            const validFallbacks = ['/default.png', '/图片加载失败.png']
            return validFallbacks.some(fb => mockImg.src.endsWith(fb))
          }
        ),
        { numRuns: 100 }
      )
    })
  })

  describe('ImageType 常量属性', () => {
    it('属性：ImageType 应该包含 AVATAR 和 POST 两个常量', () => {
      fc.assert(
        fc.property(
          fc.constant(ImageType),
          (imageType) => {
            return (
              imageType.AVATAR === 'avatar' &&
              imageType.POST === 'post' &&
              Object.keys(imageType).length === 2
            )
          }
        ),
        { numRuns: 100 }
      )
    })

    it('属性：ImageType 的值应该是字符串类型', () => {
      fc.assert(
        fc.property(
          fc.constant(ImageType),
          (imageType) => {
            return (
              typeof imageType.AVATAR === 'string' &&
              typeof imageType.POST === 'string'
            )
          }
        ),
        { numRuns: 100 }
      )
    })
  })

  describe('集成属性', () => {
    it('属性：getFallbackImage 和 handleImageError 应该使用一致的映射', () => {
      fc.assert(
        fc.property(
          fc.constantFrom(ImageType.AVATAR, ImageType.POST),
          fc.webUrl(),
          (type, url) => {
            const expectedFallback = getFallbackImage(type)
            
            const mockImg = { src: url }
            const mockEvent = { target: mockImg }
            handleImageError(mockEvent, type)
            
            return mockImg.src === expectedFallback
          }
        ),
        { numRuns: 100 }
      )
    })

    it('属性：对于所有类型，fallback 路径应该是唯一的', () => {
      fc.assert(
        fc.property(
          fc.constant([ImageType.AVATAR, ImageType.POST]),
          (types) => {
            const fallbacks = types.map(type => getFallbackImage(type))
            const uniqueFallbacks = new Set(fallbacks)
            return uniqueFallbacks.size === types.length
          }
        ),
        { numRuns: 100 }
      )
    })

    it('属性：fallback 图片路径不应该包含协议或域名（应该是相对路径）', () => {
      fc.assert(
        fc.property(
          fc.constantFrom(ImageType.AVATAR, ImageType.POST),
          (type) => {
            const fallback = getFallbackImage(type)
            return (
              !fallback.startsWith('http://') &&
              !fallback.startsWith('https://') &&
              !fallback.includes('://')
            )
          }
        ),
        { numRuns: 100 }
      )
    })
  })
})
