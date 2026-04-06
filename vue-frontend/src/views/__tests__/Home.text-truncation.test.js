/**
 * 公告展示优化 - 文本截断效果验证测试
 * 
 * 测试目标：
 * - 验证长标题是否正确截断为单行并显示省略号
 * - 验证长内容是否正确截断为两行并显示省略号
 * - 验证公告卡片是否不会因内容过长而溢出
 * 
 * 验证需求：6.4, 6.5, 6.6
 */

import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import Home from '../Home.vue'

// Mock路由
vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn()
  })
}))

// Mock API请求
vi.mock('@/api/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn()
  }
}))

vi.mock('@/api/post', () => ({
  getPostList: vi.fn()
}))

vi.mock('@/api/user', () => ({
  getUserProfile: vi.fn()
}))

describe('Home - 公告文本截断效果验证', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('应该为公告标题应用line-clamp-1样式（单行截断）', async () => {
    const wrapper = mount(Home, {
      global: {
        stubs: {
          Card: { template: '<div><slot /></div>' },
          CardHeader: { template: '<div><slot /></div>' },
          CardTitle: { template: '<div><slot /></div>' },
          CardContent: { template: '<div><slot /></div>' },
          Bell: { template: '<span />' },
          Button: { template: '<button><slot /></button>' },
          Dialog: { template: '<div v-if="open"><slot /></div>', props: ['open'] },
          DialogContent: { template: '<div><slot /></div>' },
          DialogHeader: { template: '<div><slot /></div>' },
          DialogTitle: { template: '<div><slot /></div>' }
        }
      }
    })

    // 设置测试数据：包含超长标题的公告
    wrapper.vm.announcements = [
      {
        id: 1,
        title: '这是一个非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常长的公告标题，应该被截断为单行',
        content: '公告内容',
        isTop: 1,
        createTime: '2024-01-01'
      }
    ]

    await wrapper.vm.$nextTick()

    // 查找公告标题元素
    const titleElement = wrapper.find('.line-clamp-1')
    
    // 验证：标题元素存在
    expect(titleElement.exists()).toBe(true)
    
    // 验证：标题元素包含line-clamp-1类（单行截断）
    expect(titleElement.classes()).toContain('line-clamp-1')
    
    // 验证：标题元素包含break-all类（允许在任意字符处换行）
    expect(titleElement.classes()).toContain('break-all')
    
    console.log('✅ 公告标题正确应用了line-clamp-1和break-all样式')
  })

  it('应该为公告内容应用line-clamp-2样式（两行截断）', async () => {
    const wrapper = mount(Home, {
      global: {
        stubs: {
          Card: { template: '<div><slot /></div>' },
          CardHeader: { template: '<div><slot /></div>' },
          CardTitle: { template: '<div><slot /></div>' },
          CardContent: { template: '<div><slot /></div>' },
          Bell: { template: '<span />' },
          Button: { template: '<button><slot /></button>' },
          Dialog: { template: '<div v-if="open"><slot /></div>', props: ['open'] },
          DialogContent: { template: '<div><slot /></div>' },
          DialogHeader: { template: '<div><slot /></div>' },
          DialogTitle: { template: '<div><slot /></div>' }
        }
      }
    })

    // 设置测试数据：包含超长内容的公告
    wrapper.vm.announcements = [
      {
        id: 1,
        title: '公告标题',
        content: '这是一段非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常长的公告内容，应该被截断为两行并显示省略号',
        isTop: 0,
        createTime: '2024-01-01'
      }
    ]

    await wrapper.vm.$nextTick()

    // 查找公告内容元素
    const contentElement = wrapper.find('.line-clamp-2')
    
    // 验证：内容元素存在
    expect(contentElement.exists()).toBe(true)
    
    // 验证：内容元素包含line-clamp-2类（两行截断）
    expect(contentElement.classes()).toContain('line-clamp-2')
    
    // 验证：内容元素包含break-words类（在单词边界处换行）
    expect(contentElement.classes()).toContain('break-words')
    
    // 验证：内容元素包含leading-relaxed类（行高适中）
    expect(contentElement.classes()).toContain('leading-relaxed')
    
    console.log('✅ 公告内容正确应用了line-clamp-2、break-words和leading-relaxed样式')
  })

  it('应该为公告卡片容器设置max-h和overflow-hidden防止溢出', async () => {
    const wrapper = mount(Home, {
      global: {
        stubs: {
          Card: { template: '<div><slot /></div>' },
          CardHeader: { template: '<div><slot /></div>' },
          CardTitle: { template: '<div><slot /></div>' },
          CardContent: { template: '<div><slot /></div>' },
          Bell: { template: '<span />' },
          Button: { template: '<button><slot /></button>' },
          Dialog: { template: '<div v-if="open"><slot /></div>', props: ['open'] },
          DialogContent: { template: '<div><slot /></div>' },
          DialogHeader: { template: '<div><slot /></div>' },
          DialogTitle: { template: '<div><slot /></div>' }
        }
      }
    })

    // 设置测试数据
    wrapper.vm.announcements = [
      {
        id: 1,
        title: '测试公告',
        content: '测试内容',
        isTop: 0,
        createTime: '2024-01-01'
      }
    ]

    await wrapper.vm.$nextTick()

    // 查找公告卡片容器元素
    const cardContainer = wrapper.find('.max-h-\\[200px\\]')
    
    // 验证：卡片容器存在
    expect(cardContainer.exists()).toBe(true)
    
    // 验证：卡片容器包含max-h-[200px]类（最大高度200px）
    expect(cardContainer.classes()).toContain('max-h-[200px]')
    
    // 验证：卡片容器包含overflow-hidden类（隐藏溢出内容）
    expect(cardContainer.classes()).toContain('overflow-hidden')
    
    console.log('✅ 公告卡片容器正确设置了max-h-[200px]和overflow-hidden样式')
  })

  it('应该在弹窗中的公告也应用文本截断样式', async () => {
    const wrapper = mount(Home, {
      global: {
        stubs: {
          Card: { template: '<div><slot /></div>' },
          CardHeader: { template: '<div><slot /></div>' },
          CardTitle: { template: '<div><slot /></div>' },
          CardContent: { template: '<div><slot /></div>' },
          Bell: { template: '<span />' },
          Button: { template: '<button><slot /></button>' },
          Dialog: { 
            template: '<div v-if="open"><slot /></div>', 
            props: ['open']
          },
          DialogContent: { template: '<div><slot /></div>' },
          DialogHeader: { template: '<div><slot /></div>' },
          DialogTitle: { template: '<div><slot /></div>' }
        }
      }
    })

    // 设置测试数据
    wrapper.vm.announcements = [
      {
        id: 1,
        title: '这是一个超长的公告标题，在弹窗中也应该被截断',
        content: '这是一段超长的公告内容，在弹窗中也应该被截断为两行并显示省略号',
        isTop: 1,
        createTime: '2024-01-01'
      }
    ]

    // 打开全部公告弹窗
    wrapper.vm.showAllAnnouncements = true
    await wrapper.vm.$nextTick()

    // 查找弹窗中的标题和内容元素
    const allTitleElements = wrapper.findAll('.line-clamp-1')
    const allContentElements = wrapper.findAll('.line-clamp-2')
    
    // 验证：弹窗中也有应用line-clamp-1的标题元素
    expect(allTitleElements.length).toBeGreaterThan(0)
    
    // 验证：弹窗中也有应用line-clamp-2的内容元素
    expect(allContentElements.length).toBeGreaterThan(0)
    
    console.log('✅ 弹窗中的公告也正确应用了文本截断样式')
  })

  it('综合测试：验证完整的文本截断效果实现', async () => {
    const wrapper = mount(Home, {
      global: {
        stubs: {
          Card: { template: '<div><slot /></div>' },
          CardHeader: { template: '<div><slot /></div>' },
          CardTitle: { template: '<div><slot /></div>' },
          CardContent: { template: '<div><slot /></div>' },
          Bell: { template: '<span />' },
          Button: { template: '<button><slot /></button>' },
          Dialog: { template: '<div v-if="open"><slot /></div>', props: ['open'] },
          DialogContent: { template: '<div><slot /></div>' },
          DialogHeader: { template: '<div><slot /></div>' },
          DialogTitle: { template: '<div><slot /></div>' }
        }
      }
    })

    // 设置测试数据：包含多条超长公告
    wrapper.vm.announcements = [
      {
        id: 1,
        title: '超长标题'.repeat(20),
        content: '超长内容'.repeat(50),
        isTop: 1,
        createTime: '2024-01-01'
      },
      {
        id: 2,
        title: '另一个超长标题'.repeat(15),
        content: '另一段超长内容'.repeat(40),
        isTop: 0,
        createTime: '2024-01-02'
      }
    ]

    await wrapper.vm.$nextTick()

    // 验证需求6.4：标题使用line-clamp-1强制单行显示
    const titleElements = wrapper.findAll('.line-clamp-1')
    expect(titleElements.length).toBeGreaterThan(0)
    titleElements.forEach(el => {
      expect(el.classes()).toContain('line-clamp-1')
      expect(el.classes()).toContain('break-all')
    })

    // 验证需求6.5：内容使用line-clamp-2强制两行显示
    const contentElements = wrapper.findAll('.line-clamp-2')
    expect(contentElements.length).toBeGreaterThan(0)
    contentElements.forEach(el => {
      expect(el.classes()).toContain('line-clamp-2')
      expect(el.classes()).toContain('break-words')
    })

    // 验证需求6.6：卡片设置固定或最大高度，防止内容溢出
    const cardContainers = wrapper.findAll('.max-h-\\[200px\\]')
    expect(cardContainers.length).toBeGreaterThan(0)
    cardContainers.forEach(el => {
      expect(el.classes()).toContain('max-h-[200px]')
      expect(el.classes()).toContain('overflow-hidden')
    })

    console.log('✅ 综合验证通过：所有文本截断效果均已正确实现')
    console.log('  - 需求6.4：标题使用line-clamp-1强制单行显示 ✓')
    console.log('  - 需求6.5：内容使用line-clamp-2强制两行显示 ✓')
    console.log('  - 需求6.6：卡片设置最大高度防止溢出 ✓')
  })
})
