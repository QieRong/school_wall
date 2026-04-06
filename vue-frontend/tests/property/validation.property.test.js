/**
 * 表单验证属性测试
 * Property 2: 表单验证一致性
 * Validates: Requirements 3.1, 3.2, 3.3
 */

import { describe, it, expect } from 'vitest'
import fc from 'fast-check'
import {
  validateRequired,
  validateFileSize,
  validateImageType,
  validatePhone,
  validateStudentId
} from '@/utils/validation'

describe('Property 2: 表单验证一致性', () => {
  describe('validateRequired 属性', () => {
    it('属性：空字符串和只有空格的字符串都应该被拒绝', () => {
      fc.assert(
        fc.property(
          fc.integer({ min: 0, max: 20 }),
          (spaceCount) => {
            const input = ' '.repeat(spaceCount)
            const result = validateRequired(input)
            return result.valid === false && /[\u4e00-\u9fa5]/.test(result.message)
          }
        ),
        { numRuns: 100 }
      )
    })

    it('属性：任何非空的有效字符串都应该被接受', () => {
      fc.assert(
        fc.property(
          fc.string({ minLength: 1, maxLength: 100 }).filter(s => s.trim().length > 0),
          (input) => {
            const result = validateRequired(input)
            return result.valid === true && result.message === undefined
          }
        ),
        { numRuns: 100 }
      )
    })

    it('属性：验证结果应该是确定性的', () => {
      fc.assert(
        fc.property(
          fc.string({ maxLength: 50 }),
          (input) => {
            const result1 = validateRequired(input)
            const result2 = validateRequired(input)
            const result3 = validateRequired(input)
            return result1.valid === result2.valid && result2.valid === result3.valid
          }
        ),
        { numRuns: 100 }
      )
    })

    it('属性：错误消息应该包含中文', () => {
      fc.assert(
        fc.property(
          fc.constant(''),
          fc.string({ minLength: 1, maxLength: 20 }),
          (input, fieldName) => {
            const result = validateRequired(input, fieldName)
            if (!result.valid) {
              return /[\u4e00-\u9fa5]/.test(result.message)
            }
            return true
          }
        ),
        { numRuns: 100 }
      )
    })
  })

  describe('validateFileSize 属性', () => {
    it('属性：文件大小小于等于限制时应该通过', () => {
      fc.assert(
        fc.property(
          fc.integer({ min: 1, max: 10 }),
          fc.integer({ min: 1, max: 10 }),
          (fileSizeMB, maxSizeMB) => {
            fc.pre(fileSizeMB <= maxSizeMB)
            const file = { size: fileSizeMB * 1024 * 1024 }
            const result = validateFileSize(file, maxSizeMB)
            return result.valid === true
          }
        ),
        { numRuns: 100 }
      )
    })

    it('属性：文件大小超过限制时应该被拒绝', () => {
      fc.assert(
        fc.property(
          fc.integer({ min: 2, max: 20 }),
          fc.integer({ min: 1, max: 10 }),
          (fileSizeMB, maxSizeMB) => {
            fc.pre(fileSizeMB > maxSizeMB)
            const file = { size: fileSizeMB * 1024 * 1024 }
            const result = validateFileSize(file, maxSizeMB)
            return result.valid === false && /[\u4e00-\u9fa5]/.test(result.message)
          }
        ),
        { numRuns: 100 }
      )
    })

    it('属性：错误消息应该包含具体的大小限制', () => {
      fc.assert(
        fc.property(
          fc.integer({ min: 1, max: 50 }),
          (maxSizeMB) => {
            const file = { size: (maxSizeMB + 1) * 1024 * 1024 }
            const result = validateFileSize(file, maxSizeMB)
            return result.message.includes(`${maxSizeMB}MB`)
          }
        ),
        { numRuns: 100 }
      )
    })
  })

  describe('validateImageType 属性', () => {
    const validTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp']
    const invalidTypes = ['text/plain', 'application/pdf', 'video/mp4', 'audio/mp3']

    it('属性：所有有效的图片类型都应该被接受', () => {
      fc.assert(
        fc.property(
          fc.constantFrom(...validTypes),
          (type) => {
            const file = { type }
            const result = validateImageType(file)
            return result.valid === true
          }
        ),
        { numRuns: 100 }
      )
    })

    it('属性：所有无效的文件类型都应该被拒绝', () => {
      fc.assert(
        fc.property(
          fc.constantFrom(...invalidTypes),
          (type) => {
            const file = { type }
            const result = validateImageType(file)
            return result.valid === false && /[\u4e00-\u9fa5]/.test(result.message)
          }
        ),
        { numRuns: 100 }
      )
    })

    it('属性：错误消息应该列出支持的格式', () => {
      fc.assert(
        fc.property(
          fc.constantFrom(...invalidTypes),
          (type) => {
            const file = { type }
            const result = validateImageType(file)
            return result.message.includes('JPG') || result.message.includes('PNG')
          }
        ),
        { numRuns: 100 }
      )
    })
  })

  describe('validatePhone 属性', () => {
    it('属性：所有1开头的11位数字都应该被正确验证', () => {
      fc.assert(
        fc.property(
          fc.integer({ min: 3, max: 9 }),
          fc.array(fc.integer({ min: 0, max: 9 }), { minLength: 9, maxLength: 9 }),
          (secondDigit, restDigits) => {
            const phone = '1' + secondDigit + restDigits.join('')
            const result = validatePhone(phone)
            return result.valid === true
          }
        ),
        { numRuns: 100 }
      )
    })

    it('属性：不是11位的数字串都应该被拒绝', () => {
      fc.assert(
        fc.property(
          fc.integer({ min: 1, max: 20 }).filter(n => n !== 11),
          (length) => {
            const phone = '1' + '3'.repeat(length - 1)
            const result = validatePhone(phone)
            return result.valid === false && /[\u4e00-\u9fa5]/.test(result.message)
          }
        ),
        { numRuns: 100 }
      )
    })

    it('属性：包含非数字字符的输入都应该被拒绝', () => {
      fc.assert(
        fc.property(
          fc.string({ minLength: 11, maxLength: 11 }).filter(s => /[^0-9]/.test(s)),
          (phone) => {
            const result = validatePhone(phone)
            return result.valid === false
          }
        ),
        { numRuns: 100 }
      )
    })

    it('属性：验证应该忽略前后空格', () => {
      fc.assert(
        fc.property(
          fc.integer({ min: 0, max: 5 }),
          fc.integer({ min: 0, max: 5 }),
          (leftSpaces, rightSpaces) => {
            const phone = ' '.repeat(leftSpaces) + '13800138000' + ' '.repeat(rightSpaces)
            const result = validatePhone(phone)
            return result.valid === true
          }
        ),
        { numRuns: 100 }
      )
    })
  })

  describe('validateStudentId 属性', () => {
    it('属性：任何长度大于等于6的非空字符串都应该被接受', () => {
      fc.assert(
        fc.property(
          fc.string({ minLength: 6, maxLength: 20 }).filter(s => s.trim().length >= 6),
          (id) => {
            const result = validateStudentId(id)
            return result.valid === true
          }
        ),
        { numRuns: 100 }
      )
    })

    it('属性：任何长度小于6的字符串都应该被拒绝', () => {
      fc.assert(
        fc.property(
          fc.string({ minLength: 0, maxLength: 5 }),
          (id) => {
            const result = validateStudentId(id)
            return result.valid === false && /[\u4e00-\u9fa5]/.test(result.message)
          }
        ),
        { numRuns: 100 }
      )
    })

    it('属性：错误消息应该提示至少6位或请输入学号', () => {
      fc.assert(
        fc.property(
          fc.string({ minLength: 0, maxLength: 5 }),
          (id) => {
            const result = validateStudentId(id)
            // 空字符串返回"请输入学号"，非空但少于6位返回"至少6位"
            return result.message.includes('6') || result.message.includes('请输入学号')
          }
        ),
        { numRuns: 100 }
      )
    })
  })

  describe('通用属性', () => {
    it('属性：所有验证函数都应该返回包含valid字段的对象', () => {
      fc.assert(
        fc.property(
          fc.string({ maxLength: 50 }),
          fc.integer({ min: 1, max: 100 }),
          (str, num) => {
            const results = [
              validateRequired(str),
              validateFileSize({ size: num * 1024 }),
              validateImageType({ type: 'image/png' }),
              validatePhone(str),
              validateStudentId(str)
            ]
            return results.every(r => typeof r.valid === 'boolean')
          }
        ),
        { numRuns: 100 }
      )
    })

    it('属性：失败的验证都应该返回中文错误消息', () => {
      fc.assert(
        fc.property(
          fc.constant(''),
          (emptyStr) => {
            const results = [
              validateRequired(emptyStr),
              validateFileSize({ size: 100 * 1024 * 1024 }),
              validateImageType({ type: 'text/plain' }),
              validatePhone('123'),
              validateStudentId('123')
            ]
            return results.every(r => 
              r.valid === false && 
              r.message !== undefined && 
              /[\u4e00-\u9fa5]/.test(r.message)
            )
          }
        ),
        { numRuns: 100 }
      )
    })

    it('属性：成功的验证都不应该返回错误消息', () => {
      fc.assert(
        fc.property(
          fc.string({ minLength: 1, maxLength: 50 }).filter(s => s.trim().length > 0),
          (str) => {
            const results = [
              validateRequired(str),
              validateFileSize({ size: 1024 }),
              validateImageType({ type: 'image/png' }),
              validatePhone('13800138000'),
              validateStudentId('123456')
            ]
            return results.every(r => r.valid === true && r.message === undefined)
          }
        ),
        { numRuns: 100 }
      )
    })

    it('属性：相同输入应该得到相同的验证结果（幂等性）', () => {
      fc.assert(
        fc.property(
          fc.string({ maxLength: 50 }),
          fc.integer({ min: 1, max: 100 }),
          (str, num) => {
            const file = { size: num * 1024 * 1024 }
            
            // 多次调用应该得到相同结果
            const r1 = validateRequired(str)
            const r2 = validateRequired(str)
            const f1 = validateFileSize(file, 10)
            const f2 = validateFileSize(file, 10)
            
            return r1.valid === r2.valid && f1.valid === f2.valid
          }
        ),
        { numRuns: 100 }
      )
    })
  })
})
