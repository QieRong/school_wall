/**
 * 示例测试文件
 * 验证测试环境配置是否正确
 */

import { describe, it, expect } from 'vitest'
import fc from 'fast-check'
import { validPostContent, validCategory, isSortedDescending } from './utils/generators.js'

describe('测试环境验证', () => {
  it('Vitest 应该正常工作', () => {
    expect(1 + 1).toBe(2)
  })

  it('应该能够使用 fast-check', () => {
    fc.assert(
      fc.property(fc.integer(), fc.integer(), (a, b) => {
        return a + b === b + a // 加法交换律
      }),
      { numRuns: 100 }
    )
  })
})

describe('数据生成器测试', () => {
  it('validPostContent 应该生成 1-2000 字符的非空字符串', () => {
    fc.assert(
      fc.property(validPostContent, (content) => {
        return content.length >= 1 && 
               content.length <= 2000 && 
               content.trim().length > 0
      }),
      { numRuns: 100 }
    )
  })

  it('validCategory 应该生成 1-5 之间的整数', () => {
    fc.assert(
      fc.property(validCategory, (category) => {
        return category >= 1 && category <= 5
      }),
      { numRuns: 100 }
    )
  })
})

describe('工具函数测试', () => {
  it('isSortedDescending 应该正确检测降序排列', () => {
    expect(isSortedDescending([5, 4, 3, 2, 1])).toBe(true)
    expect(isSortedDescending([5, 5, 4, 3, 3])).toBe(true)
    expect(isSortedDescending([1, 2, 3, 4, 5])).toBe(false)
    expect(isSortedDescending([])).toBe(true)
    expect(isSortedDescending([1])).toBe(true)
  })

  it('isSortedDescending 应该支持自定义键函数', () => {
    const posts = [
      { likeCount: 100 },
      { likeCount: 50 },
      { likeCount: 10 }
    ]
    expect(isSortedDescending(posts, p => p.likeCount)).toBe(true)
  })
})

describe('属性测试示例', () => {
  /**
   * **Feature: functional-testing, Property Example: 数组排序后应该有序**
   */
  it('数组排序后应该有序', () => {
    fc.assert(
      fc.property(fc.array(fc.integer()), (arr) => {
        const sorted = [...arr].sort((a, b) => b - a) // 降序排序
        return isSortedDescending(sorted)
      }),
      { numRuns: 100 }
    )
  })

  /**
   * **Feature: functional-testing, Property Example: 字符串长度应该非负**
   */
  it('字符串长度应该非负', () => {
    fc.assert(
      fc.property(fc.string(), (str) => {
        return str.length >= 0
      }),
      { numRuns: 100 }
    )
  })
})
