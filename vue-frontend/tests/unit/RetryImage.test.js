/**
 * RetryImage 组件单元测试
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import RetryImage from '@/components/RetryImage.vue'
import { ImageType } from '@/utils/imageUtils'

describe('RetryImage 组件', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.clearAllTimers()
    vi.useRealTimers()
    vi.clearAllMocks()
  })

  describe('基本渲染', () => {
    it('应该正确渲染容器和 img 元素', () => {
      const wrapper = mount(RetryImage, {
        props: {
          src: 'http://example.com/image.jpg',
          alt: '测试图片'
        }
      })

      expect(wrapper.find('div.relative').exists()).toBe(true)
      const img = wrapper.find('img')
      expect(img.exists()).toBe(true)
      expect(img.attributes('alt')).toBe('测试图片')
    })

    it('应该应用自定义 class', () => {
      const wrapper = mount(RetryImage, {
        props: {
          src: 'http://example.com/image.jpg',
          class: 'custom-class'
        }
      })

      const container = wrapper.find('div.relative')
      expect(container.classes()).toContain('custom-class')
    })
  })

  describe('type prop 功能', () => {
    it('默认应该使用 post 类型', () => {
      const wrapper = mount(RetryImage, {
        props: {
          src: 'http://example.com/image.jpg'
        }
      })

      expect(wrapper.vm.fallbackImage).toBe('/图片加载失败.png')
    })

    it('type="avatar" 时应该使用头像 fallback', () => {
      const wrapper = mount(RetryImage, {
        props: {
          src: 'http://example.com/avatar.jpg',
          type: ImageType.AVATAR
        }
      })

      expect(wrapper.vm.fallbackImage).toBe('/default.png')
    })

    it('type="post" 时应该使用帖子 fallback', () => {
      const wrapper = mount(RetryImage, {
        props: {
          src: 'http://example.com/post.jpg',
          type: ImageType.POST
        }
      })

      expect(wrapper.vm.fallbackImage).toBe('/图片加载失败.png')
    })
  })

  describe('fallback prop 向后兼容', () => {
    it('提供 fallback 时应该使用 fallback 而不是 type', () => {
      const wrapper = mount(RetryImage, {
        props: {
          src: 'http://example.com/image.jpg',
          type: ImageType.POST,
          fallback: '/custom-fallback.png'
        }
      })

      expect(wrapper.vm.fallbackImage).toBe('/custom-fallback.png')
    })

    it('fallback 为空字符串时应该使用 type', () => {
      const wrapper = mount(RetryImage, {
        props: {
          src: 'http://example.com/image.jpg',
          type: ImageType.AVATAR,
          fallback: ''
        }
      })

      expect(wrapper.vm.fallbackImage).toBe('/default.png')
    })
  })

  describe('空 src 处理', () => {
    it('src 为空字符串时应该显示 fallback', async () => {
      const wrapper = mount(RetryImage, {
        props: {
          src: '',
          type: ImageType.POST
        }
      })

      await flushPromises()
      
      expect(wrapper.vm.hasError).toBe(true)
      expect(wrapper.vm.imgSrc).toBe('/图片加载失败.png')
    })

    it('src 只有空格时应该显示 fallback', async () => {
      const wrapper = mount(RetryImage, {
        props: {
          src: '   ',
          type: ImageType.AVATAR
        }
      })

      await flushPromises()
      
      expect(wrapper.vm.hasError).toBe(true)
      expect(wrapper.vm.imgSrc).toBe('/default.png')
    })
  })

  describe('重试机制', () => {
    it('图片加载失败时应该自动重试', async () => {
      const wrapper = mount(RetryImage, {
        props: {
          src: 'http://example.com/image.jpg',
          maxRetries: 3
        }
      })

      const img = wrapper.find('img')
      
      // 触发第一次错误
      await img.trigger('error')
      expect(wrapper.vm.retryCount).toBe(1)
      
      // 等待重试延迟
      vi.advanceTimersByTime(500)
      await flushPromises()
      
      // 应该添加了重试参数
      expect(wrapper.vm.imgSrc).toContain('?retry=')
    })

    it('应该在达到最大重试次数后停止重试', async () => {
      const wrapper = mount(RetryImage, {
        props: {
          src: 'http://example.com/image.jpg',
          maxRetries: 2
        }
      })

      const img = wrapper.find('img')
      
      // 触发多次错误
      await img.trigger('error')
      vi.advanceTimersByTime(500)
      await flushPromises()
      
      await img.trigger('error')
      vi.advanceTimersByTime(500)
      await flushPromises()
      
      await img.trigger('error')
      await flushPromises()
      
      // 应该达到最大重试次数
      expect(wrapper.vm.retryCount).toBe(2)
      expect(wrapper.vm.hasError).toBe(true)
    })

    it('达到最大重试次数后应该显示 fallback', async () => {
      const wrapper = mount(RetryImage, {
        props: {
          src: 'http://example.com/image.jpg',
          type: ImageType.POST,
          maxRetries: 1
        }
      })

      const img = wrapper.find('img')
      
      // 触发错误直到达到最大重试次数
      await img.trigger('error')
      vi.advanceTimersByTime(500)
      await flushPromises()
      
      await img.trigger('error')
      await flushPromises()
      
      expect(wrapper.vm.imgSrc).toBe('/图片加载失败.png')
    })

    it('应该尊重自定义的 maxRetries 值', async () => {
      const wrapper = mount(RetryImage, {
        props: {
          src: 'http://example.com/image.jpg',
          maxRetries: 5
        }
      })

      const img = wrapper.find('img')
      
      // 触发 5 次错误
      for (let i = 0; i < 5; i++) {
        await img.trigger('error')
        vi.advanceTimersByTime(500)
        await flushPromises()
      }
      
      expect(wrapper.vm.retryCount).toBe(5)
      expect(wrapper.vm.hasError).toBe(false)
      
      // 第 6 次应该显示 fallback
      await img.trigger('error')
      await flushPromises()
      
      expect(wrapper.vm.hasError).toBe(true)
    })
  })

  describe('手动重试功能', () => {
    it('点击重试按钮应该重置状态并重新加载', async () => {
      const wrapper = mount(RetryImage, {
        props: {
          src: 'http://example.com/image.jpg',
          maxRetries: 0
        }
      })

      const img = wrapper.find('img')
      
      // 触发错误显示重试按钮
      await img.trigger('error')
      await flushPromises()
      
      expect(wrapper.vm.hasError).toBe(true)
      
      // 点击重试按钮
      const retryButton = wrapper.find('.cursor-pointer')
      await retryButton.trigger('click')
      await flushPromises()
      
      expect(wrapper.vm.hasError).toBe(false)
      expect(wrapper.vm.retryCount).toBe(0)
      expect(wrapper.vm.imgSrc).toContain('?retry=')
    })

    it('重试按钮应该只在错误时显示', async () => {
      const wrapper = mount(RetryImage, {
        props: {
          src: 'http://example.com/image.jpg'
        }
      })

      // 初始状态不应该显示重试按钮
      expect(wrapper.find('.cursor-pointer').exists()).toBe(false)
      
      // 触发错误
      const img = wrapper.find('img')
      await img.trigger('error')
      vi.advanceTimersByTime(500)
      await flushPromises()
      
      // 多次重试后应该显示重试按钮
      for (let i = 0; i < 3; i++) {
        await img.trigger('error')
        vi.advanceTimersByTime(500)
        await flushPromises()
      }
      
      expect(wrapper.find('.cursor-pointer').exists()).toBe(true)
    })
  })

  describe('src 变化监听', () => {
    it('src 变化时应该重置状态', async () => {
      const wrapper = mount(RetryImage, {
        props: {
          src: 'http://example.com/image1.jpg'
        }
      })

      // 触发错误
      const img = wrapper.find('img')
      await img.trigger('error')
      vi.advanceTimersByTime(500)
      await flushPromises()
      
      expect(wrapper.vm.retryCount).toBeGreaterThan(0)
      
      // 更改 src
      await wrapper.setProps({ src: 'http://example.com/image2.jpg' })
      await flushPromises()
      
      expect(wrapper.vm.retryCount).toBe(0)
      expect(wrapper.vm.hasError).toBe(false)
      expect(wrapper.vm.imgSrc).toBe('http://example.com/image2.jpg')
    })

    it('src 变为空时应该显示 fallback', async () => {
      const wrapper = mount(RetryImage, {
        props: {
          src: 'http://example.com/image.jpg',
          type: ImageType.AVATAR
        }
      })

      await wrapper.setProps({ src: '' })
      await flushPromises()
      
      expect(wrapper.vm.hasError).toBe(true)
      expect(wrapper.vm.imgSrc).toBe('/default.png')
    })
  })

  describe('prop 验证', () => {
    it('type prop 应该只接受 avatar 或 post', () => {
      const validator = RetryImage.props.type.validator
      
      expect(validator('avatar')).toBe(true)
      expect(validator('post')).toBe(true)
      expect(validator('invalid')).toBe(false)
    })

    it('maxRetries prop 应该只接受 0-10 之间的数字', () => {
      const validator = RetryImage.props.maxRetries.validator
      
      expect(validator(0)).toBe(true)
      expect(validator(5)).toBe(true)
      expect(validator(10)).toBe(true)
      expect(validator(-1)).toBe(false)
      expect(validator(11)).toBe(false)
    })

    it('src prop 应该是必需的', () => {
      expect(RetryImage.props.src.required).toBe(true)
    })

    it('type prop 应该有默认值 post', () => {
      expect(RetryImage.props.type.default).toBe(ImageType.POST)
    })

    it('maxRetries prop 应该有默认值 3', () => {
      expect(RetryImage.props.maxRetries.default).toBe(3)
    })
  })

  describe('不同类型的 fallback', () => {
    it('avatar 类型失败后应该显示头像 fallback', async () => {
      const wrapper = mount(RetryImage, {
        props: {
          src: 'http://example.com/avatar.jpg',
          type: ImageType.AVATAR,
          maxRetries: 0
        }
      })

      const img = wrapper.find('img')
      await img.trigger('error')
      await flushPromises()
      
      expect(wrapper.vm.imgSrc).toBe('/default.png')
    })

    it('post 类型失败后应该显示帖子 fallback', async () => {
      const wrapper = mount(RetryImage, {
        props: {
          src: 'http://example.com/post.jpg',
          type: ImageType.POST,
          maxRetries: 0
        }
      })

      const img = wrapper.find('img')
      await img.trigger('error')
      await flushPromises()
      
      expect(wrapper.vm.imgSrc).toBe('/图片加载失败.png')
    })
  })
})
