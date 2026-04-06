/**
 * RetryImage 组件属性测试
 * Property 5: 重试次数上限
 * Validates: Requirements 5.1, 5.3
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import fc from 'fast-check'
import RetryImage from '@/components/RetryImage.vue'
import { ImageType } from '@/utils/imageUtils'

describe('Property 5: 重试次数上限', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.clearAllTimers()
    vi.useRealTimers()
    vi.clearAllMocks()
  })

  describe('重试次数严格限制', () => {
    it('属性：对于任意 maxRetries 值（1-10），重试次数不应该超过该值', async () => {
      await fc.assert(
        fc.asyncProperty(
          fc.integer({ min: 1, max: 10 }),
          fc.webUrl(),
          async (maxRetries, src) => {
            const wrapper = mount(RetryImage, {
              props: { src, maxRetries }
            })

            const img = wrapper.find('img')
            
            // 触发 maxRetries + 5 次错误
            for (let i = 0; i < maxRetries + 5; i++) {
              await img.trigger('error')
              vi.advanceTimersByTime(500)
              await flushPromises()
            }
            
            // 重试次数不应该超过 maxRetries
            return wrapper.vm.retryCount <= maxRetries
          }
        ),
        { numRuns: 50 }
      )
    })

    it('属性：对于任意 maxRetries 值，达到上限后应该显示 fallback', async () => {
      await fc.assert(
        fc.asyncProperty(
          fc.integer({ min: 0, max: 5 }),
          fc.webUrl(),
          fc.constantFrom(ImageType.AVATAR, ImageType.POST),
          async (maxRetries, src, type) => {
            const wrapper = mount(RetryImage, {
              props: { src, maxRetries, type }
            })

            const img = wrapper.find('img')
            
            // 触发足够多的错误以达到上限
            for (let i = 0; i <= maxRetries; i++) {
              await img.trigger('error')
              vi.advanceTimersByTime(500)
              await flushPromises()
            }
            
            // 应该显示错误状态
            const expectedFallback = type === ImageType.AVATAR 
              ? '/default.png' 
              : '/图片加载失败.png'
            
            return wrapper.vm.hasError === true && 
                   wrapper.vm.imgSrc === expectedFallback
          }
        ),
        { numRuns: 50 }
      )
    })
  })

  describe('maxRetries 为 0 的特殊情况', () => {
    it('属性：对于任意 src，maxRetries=0 时第一次失败就应该显示 fallback', async () => {
      await fc.assert(
        fc.asyncProperty(
          fc.webUrl(),
          fc.constantFrom(ImageType.AVATAR, ImageType.POST),
          async (src, type) => {
            const wrapper = mount(RetryImage, {
              props: { src, maxRetries: 0, type }
            })

            const img = wrapper.find('img')
            await img.trigger('error')
            await flushPromises()
            
            return wrapper.vm.hasError === true && wrapper.vm.retryCount === 0
          }
        ),
        { numRuns: 50 }
      )
    })
  })

  describe('重试延迟一致性', () => {
    it('属性：对于任意 maxRetries 值，每次重试都应该有 500ms 延迟', async () => {
      await fc.assert(
        fc.asyncProperty(
          fc.integer({ min: 1, max: 5 }),
          fc.webUrl(),
          async (maxRetries, src) => {
            const wrapper = mount(RetryImage, {
              props: { src, maxRetries }
            })

            const img = wrapper.find('img')
            let allRetriesHaveDelay = true
            
            for (let i = 0; i < maxRetries; i++) {
              const srcBefore = wrapper.vm.imgSrc
              await img.trigger('error')
              
              // 立即检查，src 不应该改变
              if (wrapper.vm.imgSrc !== srcBefore && !wrapper.vm.imgSrc.includes('?retry=')) {
                allRetriesHaveDelay = false
                break
              }
              
              // 等待延迟后 src 应该改变
              vi.advanceTimersByTime(500)
              await flushPromises()
              
              if (i < maxRetries && !wrapper.vm.imgSrc.includes('?retry=')) {
                allRetriesHaveDelay = false
                break
              }
            }
            
            return allRetriesHaveDelay
          }
        ),
        { numRuns: 30 }
      )
    })
  })

  describe('重试 URL 包含时间戳参数', () => {
    it('属性：对于任意 src，重试时 URL 应该包含 retry 参数', async () => {
      await fc.assert(
        fc.asyncProperty(
          fc.webUrl(),
          fc.integer({ min: 1, max: 5 }),
          async (src, maxRetries) => {
            const wrapper = mount(RetryImage, {
              props: { src, maxRetries }
            })

            const img = wrapper.find('img')
            
            // 触发一次错误
            await img.trigger('error')
            vi.advanceTimersByTime(500)
            await flushPromises()
            
            // 如果还在重试（未达到上限），URL 应该包含 retry 参数
            if (wrapper.vm.retryCount > 0 && wrapper.vm.retryCount < maxRetries) {
              return wrapper.vm.imgSrc.includes('?retry=')
            }
            
            return true
          }
        ),
        { numRuns: 50 }
      )
    })
  })

  describe('手动重试重置', () => {
    it('属性：对于任意状态，手动重试应该重置 retryCount 为 0', async () => {
      await fc.assert(
        fc.asyncProperty(
          fc.webUrl(),
          fc.integer({ min: 0, max: 3 }),
          async (src, maxRetries) => {
            const wrapper = mount(RetryImage, {
              props: { src, maxRetries }
            })

            const img = wrapper.find('img')
            
            // 触发错误直到显示 fallback
            for (let i = 0; i <= maxRetries; i++) {
              await img.trigger('error')
              vi.advanceTimersByTime(500)
              await flushPromises()
            }
            
            // 手动重试
            const retryButton = wrapper.find('.cursor-pointer')
            if (retryButton.exists()) {
              await retryButton.trigger('click')
              await flushPromises()
              
              return wrapper.vm.retryCount === 0 && wrapper.vm.hasError === false
            }
            
            return true
          }
        ),
        { numRuns: 50 }
      )
    })
  })

  describe('src 变化重置', () => {
    it('属性：对于任意新 src，retryCount 应该重置为 0', async () => {
      await fc.assert(
        fc.asyncProperty(
          fc.webUrl(),
          fc.webUrl(),
          fc.integer({ min: 1, max: 5 }),
          async (src1, src2, maxRetries) => {
            fc.pre(src1 !== src2)
            
            const wrapper = mount(RetryImage, {
              props: { src: src1, maxRetries }
            })

            const img = wrapper.find('img')
            
            // 触发一些错误
            for (let i = 0; i < Math.min(2, maxRetries); i++) {
              await img.trigger('error')
              vi.advanceTimersByTime(500)
              await flushPromises()
            }
            
            const retryCountBefore = wrapper.vm.retryCount
            
            // 更改 src
            await wrapper.setProps({ src: src2 })
            await flushPromises()
            
            return retryCountBefore > 0 && wrapper.vm.retryCount === 0
          }
        ),
        { numRuns: 50 }
      )
    })
  })

  describe('fallback 类型一致性', () => {
    it('属性：对于任意 type，达到重试上限后应该显示对应类型的 fallback', async () => {
      await fc.assert(
        fc.asyncProperty(
          fc.webUrl(),
          fc.constantFrom(ImageType.AVATAR, ImageType.POST),
          fc.integer({ min: 0, max: 3 }),
          async (src, type, maxRetries) => {
            const wrapper = mount(RetryImage, {
              props: { src, type, maxRetries }
            })

            const img = wrapper.find('img')
            
            // 触发足够多的错误
            for (let i = 0; i <= maxRetries; i++) {
              await img.trigger('error')
              vi.advanceTimersByTime(500)
              await flushPromises()
            }
            
            const expectedFallback = type === ImageType.AVATAR 
              ? '/default.png' 
              : '/图片加载失败.png'
            
            return wrapper.vm.imgSrc === expectedFallback
          }
        ),
        { numRuns: 50 }
      )
    })
  })

  describe('maxRetries 验证器', () => {
    it('属性：对于任意整数，验证器应该正确判断是否在 0-10 范围内', () => {
      fc.assert(
        fc.property(
          fc.integer({ min: -5, max: 15 }),
          (value) => {
            const validator = RetryImage.props.maxRetries.validator
            const isValid = validator(value)
            const shouldBeValid = value >= 0 && value <= 10
            
            return isValid === shouldBeValid
          }
        ),
        { numRuns: 100 }
      )
    })
  })

  describe('组件状态一致性', () => {
    it('属性：对于任意 props，hasError 和 retryCount 应该保持一致', async () => {
      await fc.assert(
        fc.asyncProperty(
          fc.webUrl(),
          fc.integer({ min: 0, max: 5 }),
          async (src, maxRetries) => {
            const wrapper = mount(RetryImage, {
              props: { src, maxRetries }
            })

            const img = wrapper.find('img')
            
            // 触发一些错误
            for (let i = 0; i <= maxRetries; i++) {
              await img.trigger('error')
              vi.advanceTimersByTime(500)
              await flushPromises()
            }
            
            // 如果 hasError 为 true，retryCount 应该等于 maxRetries
            if (wrapper.vm.hasError) {
              return wrapper.vm.retryCount === maxRetries
            }
            
            // 如果 hasError 为 false，retryCount 应该小于 maxRetries
            return wrapper.vm.retryCount < maxRetries
          }
        ),
        { numRuns: 50 }
      )
    })
  })

  describe('空 src 不触发重试', () => {
    it('属性：对于空字符串或只有空格的 src，不应该触发重试机制', async () => {
      await fc.assert(
        fc.asyncProperty(
          fc.integer({ min: 0, max: 10 }),
          fc.integer({ min: 1, max: 5 }),
          async (spaceCount, maxRetries) => {
            const src = ' '.repeat(spaceCount)
            const wrapper = mount(RetryImage, {
              props: { src, maxRetries }
            })

            await flushPromises()
            
            // 应该直接显示 fallback，retryCount 应该为 0
            return wrapper.vm.hasError === true && wrapper.vm.retryCount === 0
          }
        ),
        { numRuns: 50 }
      )
    })
  })
})
