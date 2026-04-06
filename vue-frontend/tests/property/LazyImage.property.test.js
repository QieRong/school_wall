/**
 * LazyImage 组件属性测试
 * Property 3: 组件 Props 向后兼容性
 * Validates: Requirements 3.2
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import fc from 'fast-check'
import LazyImage from '@/components/LazyImage.vue'
import { ImageType } from '@/utils/imageUtils'

describe('Property 3: 组件 Props 向后兼容性', () => {
  let intersectionObserverMock

  beforeEach(() => {
    // Mock IntersectionObserver
    intersectionObserverMock = vi.fn(function(callback) {
      this.observe = vi.fn()
      this.unobserve = vi.fn()
      this.disconnect = vi.fn()
      
      setTimeout(() => {
        callback([{ isIntersecting: true }])
      }, 0)
    })
    
    global.IntersectionObserver = intersectionObserverMock
    
    // Mock Image 对象
    global.Image = class {
      constructor() {
        setTimeout(() => {
          if (this.onload) this.onload()
        }, 0)
      }
    }
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  describe('不传 type prop 的向后兼容性', () => {
    it('属性：对于任意 src，不传 type 时应该使用默认值 post', () => {
      fc.assert(
        fc.property(
          fc.webUrl(),
          (src) => {
            const wrapper = mount(LazyImage, {
              props: { src }
            })

            const img = wrapper.find('img')
            // 初始应该显示 post 类型的 fallback
            return img.attributes('src') === '/图片加载失败.png'
          }
        ),
        { numRuns: 100 }
      )
    })

    it('属性：对于任意 alt 文本，组件应该正常工作', () => {
      fc.assert(
        fc.property(
          fc.webUrl(),
          fc.string({ maxLength: 100 }),
          (src, alt) => {
            const wrapper = mount(LazyImage, {
              props: { src, alt }
            })

            const img = wrapper.find('img')
            return img.exists() && img.attributes('alt') === alt
          }
        ),
        { numRuns: 100 }
      )
    })
  })

  describe('placeholder prop 向后兼容性', () => {
    it('属性：对于任意 placeholder 路径，应该优先使用 placeholder', () => {
      fc.assert(
        fc.property(
          fc.webUrl(),
          fc.constantFrom('/custom1.png', '/custom2.png', '/fallback.jpg'),
          (src, placeholder) => {
            const wrapper = mount(LazyImage, {
              props: { src, placeholder }
            })

            const img = wrapper.find('img')
            return img.attributes('src') === placeholder
          }
        ),
        { numRuns: 100 }
      )
    })

    it('属性：placeholder 为空时应该使用 type 决定的 fallback', () => {
      fc.assert(
        fc.property(
          fc.webUrl(),
          fc.constantFrom(ImageType.AVATAR, ImageType.POST),
          (src, type) => {
            const wrapper = mount(LazyImage, {
              props: { src, type, placeholder: '' }
            })

            const img = wrapper.find('img')
            const expectedFallback = type === ImageType.AVATAR 
              ? '/default.png' 
              : '/图片加载失败.png'
            return img.attributes('src') === expectedFallback
          }
        ),
        { numRuns: 100 }
      )
    })
  })

  describe('type prop 功能', () => {
    it('属性：对于任意 src，type="avatar" 应该使用头像 fallback', () => {
      fc.assert(
        fc.property(
          fc.webUrl(),
          (src) => {
            const wrapper = mount(LazyImage, {
              props: { src, type: ImageType.AVATAR }
            })

            const img = wrapper.find('img')
            return img.attributes('src') === '/default.png'
          }
        ),
        { numRuns: 100 }
      )
    })

    it('属性：对于任意 src，type="post" 应该使用帖子 fallback', () => {
      fc.assert(
        fc.property(
          fc.webUrl(),
          (src) => {
            const wrapper = mount(LazyImage, {
              props: { src, type: ImageType.POST }
            })

            const img = wrapper.find('img')
            return img.attributes('src') === '/图片加载失败.png'
          }
        ),
        { numRuns: 100 }
      )
    })
  })

  describe('class prop 兼容性', () => {
    it('属性：对于任意 class 字符串，应该正确应用到 img 元素', () => {
      fc.assert(
        fc.property(
          fc.webUrl(),
          fc.string({ minLength: 1, maxLength: 50 }).filter(s => /^[a-z-]+$/.test(s)),
          (src, className) => {
            const wrapper = mount(LazyImage, {
              props: { src, class: className }
            })

            const img = wrapper.find('img')
            return img.classes().includes(className)
          }
        ),
        { numRuns: 100 }
      )
    })
  })

  describe('空值处理兼容性', () => {
    it('属性：对于空字符串或只有空格的 src，应该显示 fallback', () => {
      fc.assert(
        fc.property(
          fc.integer({ min: 0, max: 10 }),
          fc.constantFrom(ImageType.AVATAR, ImageType.POST),
          (spaceCount, type) => {
            const src = ' '.repeat(spaceCount)
            const wrapper = mount(LazyImage, {
              props: { src, type }
            })

            const img = wrapper.find('img')
            const expectedFallback = type === ImageType.AVATAR 
              ? '/default.png' 
              : '/图片加载失败.png'
            return img.attributes('src') === expectedFallback
          }
        ),
        { numRuns: 100 }
      )
    })
  })

  describe('组件渲染稳定性', () => {
    it('属性：对于任意有效的 props 组合，组件应该成功渲染', () => {
      fc.assert(
        fc.property(
          fc.webUrl(),
          fc.string({ maxLength: 50 }),
          fc.constantFrom(ImageType.AVATAR, ImageType.POST, undefined),
          (src, alt, type) => {
            const props = { src, alt }
            if (type !== undefined) {
              props.type = type
            }

            const wrapper = mount(LazyImage, { props })
            const img = wrapper.find('img')
            
            return img.exists() && img.attributes('alt') === alt
          }
        ),
        { numRuns: 100 }
      )
    })

    it('属性：相同的 props 应该产生相同的初始渲染结果（幂等性）', () => {
      fc.assert(
        fc.property(
          fc.webUrl(),
          fc.constantFrom(ImageType.AVATAR, ImageType.POST),
          (src, type) => {
            const wrapper1 = mount(LazyImage, {
              props: { src, type }
            })
            const wrapper2 = mount(LazyImage, {
              props: { src, type }
            })

            const img1 = wrapper1.find('img')
            const img2 = wrapper2.find('img')
            
            return img1.attributes('src') === img2.attributes('src')
          }
        ),
        { numRuns: 100 }
      )
    })
  })

  describe('必需属性验证', () => {
    it('属性：src 应该始终是必需的', () => {
      fc.assert(
        fc.property(
          fc.constant(LazyImage.props.src),
          (srcProp) => {
            return srcProp.required === true && srcProp.type === String
          }
        ),
        { numRuns: 100 }
      )
    })

    it('属性：type 应该有正确的验证器', () => {
      fc.assert(
        fc.property(
          fc.constantFrom('avatar', 'post', 'invalid', '', 123),
          (value) => {
            const validator = LazyImage.props.type.validator
            const isValid = validator(value)
            const shouldBeValid = value === 'avatar' || value === 'post'
            
            return isValid === shouldBeValid
          }
        ),
        { numRuns: 100 }
      )
    })
  })

  describe('loading 属性', () => {
    it('属性：对于任意 props，img 元素应该始终有 loading="lazy" 属性', () => {
      fc.assert(
        fc.property(
          fc.webUrl(),
          fc.constantFrom(ImageType.AVATAR, ImageType.POST, undefined),
          (src, type) => {
            const props = { src }
            if (type !== undefined) {
              props.type = type
            }

            const wrapper = mount(LazyImage, { props })
            const img = wrapper.find('img')
            
            return img.attributes('loading') === 'lazy'
          }
        ),
        { numRuns: 100 }
      )
    })
  })
})
