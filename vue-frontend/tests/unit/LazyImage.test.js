/**
 * LazyImage 组件单元测试
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import LazyImage from '@/components/LazyImage.vue'
import { ImageType } from '@/utils/imageUtils'

describe('LazyImage 组件', () => {
  let intersectionObserverMock

  beforeEach(() => {
    // Mock IntersectionObserver
    intersectionObserverMock = vi.fn(function(callback) {
      this.observe = vi.fn()
      this.unobserve = vi.fn()
      this.disconnect = vi.fn()
      
      // 自动触发回调，模拟图片进入视口
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

  describe('基本渲染', () => {
    it('应该正确渲染 img 元素', () => {
      const wrapper = mount(LazyImage, {
        props: {
          src: 'http://example.com/image.jpg',
          alt: '测试图片'
        }
      })

      const img = wrapper.find('img')
      expect(img.exists()).toBe(true)
      expect(img.attributes('alt')).toBe('测试图片')
    })

    it('应该应用自定义 class', () => {
      const wrapper = mount(LazyImage, {
        props: {
          src: 'http://example.com/image.jpg',
          class: 'custom-class'
        }
      })

      const img = wrapper.find('img')
      expect(img.classes()).toContain('custom-class')
    })
  })

  describe('type prop 功能', () => {
    it('默认应该使用 post 类型', () => {
      const wrapper = mount(LazyImage, {
        props: {
          src: 'http://example.com/image.jpg'
        }
      })

      // 初始 src 应该是 post 类型的 fallback
      const img = wrapper.find('img')
      expect(img.attributes('src')).toBe('/图片加载失败.png')
    })

    it('type="avatar" 时应该使用头像 fallback', () => {
      const wrapper = mount(LazyImage, {
        props: {
          src: 'http://example.com/avatar.jpg',
          type: ImageType.AVATAR
        }
      })

      const img = wrapper.find('img')
      expect(img.attributes('src')).toBe('/default.png')
    })

    it('type="post" 时应该使用帖子 fallback', () => {
      const wrapper = mount(LazyImage, {
        props: {
          src: 'http://example.com/post.jpg',
          type: ImageType.POST
        }
      })

      const img = wrapper.find('img')
      expect(img.attributes('src')).toBe('/图片加载失败.png')
    })
  })

  describe('placeholder prop 向后兼容', () => {
    it('提供 placeholder 时应该使用 placeholder 而不是 type', () => {
      const wrapper = mount(LazyImage, {
        props: {
          src: 'http://example.com/image.jpg',
          type: ImageType.POST,
          placeholder: '/custom-placeholder.png'
        }
      })

      const img = wrapper.find('img')
      expect(img.attributes('src')).toBe('/custom-placeholder.png')
    })

    it('placeholder 为空字符串时应该使用 type', () => {
      const wrapper = mount(LazyImage, {
        props: {
          src: 'http://example.com/image.jpg',
          type: ImageType.AVATAR,
          placeholder: ''
        }
      })

      const img = wrapper.find('img')
      expect(img.attributes('src')).toBe('/default.png')
    })
  })

  describe('空 src 处理', () => {
    it('src 为空字符串时应该显示 fallback', async () => {
      const wrapper = mount(LazyImage, {
        props: {
          src: '',
          type: ImageType.POST
        }
      })

      await wrapper.vm.$nextTick()
      
      const img = wrapper.find('img')
      expect(img.attributes('src')).toBe('/图片加载失败.png')
    })

    it('src 只有空格时应该显示 fallback', async () => {
      const wrapper = mount(LazyImage, {
        props: {
          src: '   ',
          type: ImageType.AVATAR
        }
      })

      await wrapper.vm.$nextTick()
      
      const img = wrapper.find('img')
      expect(img.attributes('src')).toBe('/default.png')
    })
  })

  describe('图片加载成功场景', () => {
    it('图片加载成功后应该显示原图', async () => {
      global.Image = class {
        constructor() {
          setTimeout(() => {
            if (this.onload) this.onload()
          }, 0)
        }
      }

      const wrapper = mount(LazyImage, {
        props: {
          src: 'http://example.com/image.jpg',
          type: ImageType.POST
        }
      })

      await new Promise(resolve => setTimeout(resolve, 50))
      await wrapper.vm.$nextTick()

      const img = wrapper.find('img')
      expect(img.attributes('src')).toBe('http://example.com/image.jpg')
    })
  })

  describe('图片加载失败场景', () => {
    it('图片加载失败时应该显示 post 类型的 fallback', async () => {
      global.Image = class {
        constructor() {
          setTimeout(() => {
            if (this.onerror) this.onerror()
          }, 0)
        }
      }

      const wrapper = mount(LazyImage, {
        props: {
          src: 'http://example.com/broken-image.jpg',
          type: ImageType.POST
        }
      })

      await new Promise(resolve => setTimeout(resolve, 50))
      await wrapper.vm.$nextTick()

      const img = wrapper.find('img')
      expect(img.attributes('src')).toBe('/图片加载失败.png')
    })

    it('图片加载失败时应该显示 avatar 类型的 fallback', async () => {
      global.Image = class {
        constructor() {
          setTimeout(() => {
            if (this.onerror) this.onerror()
          }, 0)
        }
      }

      const wrapper = mount(LazyImage, {
        props: {
          src: 'http://example.com/broken-avatar.jpg',
          type: ImageType.AVATAR
        }
      })

      await new Promise(resolve => setTimeout(resolve, 50))
      await wrapper.vm.$nextTick()

      const img = wrapper.find('img')
      expect(img.attributes('src')).toBe('/default.png')
    })
  })

  describe('IntersectionObserver 集成', () => {
    it('应该创建 IntersectionObserver 实例', () => {
      mount(LazyImage, {
        props: {
          src: 'http://example.com/image.jpg'
        }
      })

      expect(intersectionObserverMock).toHaveBeenCalled()
    })

    it('应该观察 img 元素', async () => {
      const wrapper = mount(LazyImage, {
        props: {
          src: 'http://example.com/image.jpg'
        }
      })

      await wrapper.vm.$nextTick()

      const observeCall = intersectionObserverMock.mock.results[0].value.observe
      expect(observeCall).toHaveBeenCalled()
    })
  })

  describe('CSS 类应用', () => {
    it('加载中时应该应用 opacity-50 类', () => {
      const wrapper = mount(LazyImage, {
        props: {
          src: 'http://example.com/image.jpg'
        }
      })

      const img = wrapper.find('img')
      expect(img.classes()).toContain('opacity-50')
    })

    it('应该有 loading="lazy" 属性', () => {
      const wrapper = mount(LazyImage, {
        props: {
          src: 'http://example.com/image.jpg'
        }
      })

      const img = wrapper.find('img')
      expect(img.attributes('loading')).toBe('lazy')
    })
  })

  describe('prop 验证', () => {
    it('type prop 应该只接受 avatar 或 post', () => {
      const validator = LazyImage.props.type.validator
      
      expect(validator('avatar')).toBe(true)
      expect(validator('post')).toBe(true)
      expect(validator('invalid')).toBe(false)
    })

    it('src prop 应该是必需的', () => {
      expect(LazyImage.props.src.required).toBe(true)
    })

    it('type prop 应该有默认值 post', () => {
      expect(LazyImage.props.type.default).toBe(ImageType.POST)
    })
  })
})
