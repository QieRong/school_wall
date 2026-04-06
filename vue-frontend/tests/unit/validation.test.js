/**
 * 表单验证工具单元测试
 * 测试 utils/validation.js 中的各种验证函数
 */

import { describe, it, expect } from 'vitest'
import {
  validateRequired,
  validateFileSize,
  validateImageType,
  validatePhone,
  validateStudentId,
  validatePassword,
  validateEmail,
  validateLength,
  validateUrl,
  validateNumberRange,
  checkPasswordStrength
} from '@/utils/validation'

describe('表单验证工具', () => {
  describe('validateRequired - 非空验证', () => {
    it('应该拒绝空字符串', () => {
      const result = validateRequired('')
      expect(result.valid).toBe(false)
      expect(result.message).toContain('请输入')
      expect(result.message).toMatch(/[\u4e00-\u9fa5]/) // 包含中文
    })

    it('应该拒绝只有空格的字符串', () => {
      const result = validateRequired('   ')
      expect(result.valid).toBe(false)
      expect(result.message).toContain('请输入')
    })

    it('应该拒绝null', () => {
      const result = validateRequired(null)
      expect(result.valid).toBe(false)
    })

    it('应该拒绝undefined', () => {
      const result = validateRequired(undefined)
      expect(result.valid).toBe(false)
    })

    it('应该接受有效内容', () => {
      const result = validateRequired('有效内容')
      expect(result.valid).toBe(true)
      expect(result.message).toBeUndefined()
    })

    it('应该接受带空格的有效内容', () => {
      const result = validateRequired('  有效内容  ')
      expect(result.valid).toBe(true)
    })

    it('应该支持自定义字段名', () => {
      const result = validateRequired('', '用户名')
      expect(result.valid).toBe(false)
      expect(result.message).toContain('用户名')
    })
  })

  describe('validateFileSize - 文件大小验证', () => {
    it('应该拒绝null文件', () => {
      const result = validateFileSize(null)
      expect(result.valid).toBe(false)
      expect(result.message).toContain('请选择文件')
    })

    it('应该拒绝超过默认大小(10MB)的文件', () => {
      const file = { size: 11 * 1024 * 1024 } // 11MB
      const result = validateFileSize(file)
      expect(result.valid).toBe(false)
      expect(result.message).toContain('10MB')
      expect(result.message).toMatch(/[\u4e00-\u9fa5]/)
    })

    it('应该接受小于默认大小的文件', () => {
      const file = { size: 5 * 1024 * 1024 } // 5MB
      const result = validateFileSize(file)
      expect(result.valid).toBe(true)
    })

    it('应该接受正好等于默认大小的文件', () => {
      const file = { size: 10 * 1024 * 1024 } // 10MB
      const result = validateFileSize(file)
      expect(result.valid).toBe(true)
    })

    it('应该支持自定义最大大小', () => {
      const file = { size: 3 * 1024 * 1024 } // 3MB
      const result = validateFileSize(file, 2) // 最大2MB
      expect(result.valid).toBe(false)
      expect(result.message).toContain('2MB')
    })

    it('应该接受小文件', () => {
      const file = { size: 1024 } // 1KB
      const result = validateFileSize(file)
      expect(result.valid).toBe(true)
    })
  })

  describe('validateImageType - 图片格式验证', () => {
    it('应该拒绝null文件', () => {
      const result = validateImageType(null)
      expect(result.valid).toBe(false)
      expect(result.message).toContain('请选择图片')
    })

    it('应该接受JPEG格式', () => {
      const file = { type: 'image/jpeg' }
      const result = validateImageType(file)
      expect(result.valid).toBe(true)
    })

    it('应该接受JPG格式', () => {
      const file = { type: 'image/jpg' }
      const result = validateImageType(file)
      expect(result.valid).toBe(true)
    })

    it('应该接受PNG格式', () => {
      const file = { type: 'image/png' }
      const result = validateImageType(file)
      expect(result.valid).toBe(true)
    })

    it('应该接受GIF格式', () => {
      const file = { type: 'image/gif' }
      const result = validateImageType(file)
      expect(result.valid).toBe(true)
    })

    it('应该接受WebP格式', () => {
      const file = { type: 'image/webp' }
      const result = validateImageType(file)
      expect(result.valid).toBe(true)
    })

    it('应该拒绝PDF文件', () => {
      const file = { type: 'application/pdf' }
      const result = validateImageType(file)
      expect(result.valid).toBe(false)
      expect(result.message).toContain('JPG')
      expect(result.message).toContain('PNG')
      expect(result.message).toMatch(/[\u4e00-\u9fa5]/)
    })

    it('应该拒绝文本文件', () => {
      const file = { type: 'text/plain' }
      const result = validateImageType(file)
      expect(result.valid).toBe(false)
    })

    it('应该拒绝视频文件', () => {
      const file = { type: 'video/mp4' }
      const result = validateImageType(file)
      expect(result.valid).toBe(false)
    })
  })

  describe('validatePhone - 手机号验证', () => {
    it('应该拒绝空字符串', () => {
      const result = validatePhone('')
      expect(result.valid).toBe(false)
      expect(result.message).toContain('请输入手机号')
      expect(result.message).toMatch(/[\u4e00-\u9fa5]/)
    })

    it('应该接受有效的手机号', () => {
      const validPhones = [
        '13800138000',
        '15912345678',
        '18612345678',
        '19912345678'
      ]
      validPhones.forEach(phone => {
        const result = validatePhone(phone)
        expect(result.valid).toBe(true)
      })
    })

    it('应该拒绝不是1开头的号码', () => {
      const result = validatePhone('28012345678')
      expect(result.valid).toBe(false)
      expect(result.message).toContain('正确的手机号')
    })

    it('应该拒绝第二位不是3-9的号码', () => {
      const result = validatePhone('12012345678')
      expect(result.valid).toBe(false)
    })

    it('应该拒绝长度不是11位的号码', () => {
      const result = validatePhone('138001380')
      expect(result.valid).toBe(false)
    })

    it('应该拒绝包含非数字字符的号码', () => {
      const result = validatePhone('1380013800a')
      expect(result.valid).toBe(false)
    })

    it('应该处理带空格的手机号', () => {
      const result = validatePhone('  13800138000  ')
      expect(result.valid).toBe(true)
    })
  })

  describe('validateStudentId - 学号验证', () => {
    it('应该拒绝空字符串', () => {
      const result = validateStudentId('')
      expect(result.valid).toBe(false)
      expect(result.message).toContain('请输入学号')
      expect(result.message).toMatch(/[\u4e00-\u9fa5]/)
    })

    it('应该拒绝少于6位的学号', () => {
      const result = validateStudentId('12345')
      expect(result.valid).toBe(false)
      expect(result.message).toContain('至少6位')
    })

    it('应该接受6位学号', () => {
      const result = validateStudentId('123456')
      expect(result.valid).toBe(true)
    })

    it('应该接受更长的学号', () => {
      const result = validateStudentId('20210001')
      expect(result.valid).toBe(true)
    })

    it('应该处理带空格的学号', () => {
      const result = validateStudentId('  123456  ')
      expect(result.valid).toBe(true)
    })
  })

  describe('validatePassword - 密码验证', () => {
    it('应该拒绝空密码', () => {
      const result = validatePassword('')
      expect(result.valid).toBe(false)
      expect(result.message).toContain('请输入密码')
    })

    it('应该拒绝少于6位的密码', () => {
      const shortPasswords = ['1', '12', '123', '12345']
      shortPasswords.forEach(pwd => {
        const result = validatePassword(pwd)
        expect(result.valid).toBe(false)
        expect(result.message).toContain('不能少于6位')
      })
    })

    it('应该接受6-20位的密码', () => {
      const passwords = ['123456', '12345678', 'abcdef', 'password123']
      passwords.forEach(pwd => {
        const result = validatePassword(pwd)
        expect(result.valid).toBe(true)
      })
    })

    it('应该拒绝超过20位的密码', () => {
      const result = validatePassword('123456789012345678901')
      expect(result.valid).toBe(false)
      expect(result.message).toContain('不能超过20位')
    })

    it('应该接受管理员密码123456（6位）', () => {
      const result = validatePassword('123456')
      expect(result.valid).toBe(true)
    })

    it('应该接受弱密码但长度符合要求的密码', () => {
      const weakButValidPasswords = ['111111', 'aaaaaa', '123456']
      weakButValidPasswords.forEach(pwd => {
        const result = validatePassword(pwd)
        expect(result.valid).toBe(true)
      })
    })
  })

  describe('validateEmail - 邮箱验证', () => {
    it('应该接受有效的邮箱地址', () => {
      const validEmails = [
        'test@example.com',
        'user.name@example.com',
        'user+tag@example.co.uk'
      ]
      validEmails.forEach(email => {
        const result = validateEmail(email)
        expect(result.valid).toBe(true)
      })
    })

    it('应该拒绝无效的邮箱地址', () => {
      const invalidEmails = [
        'invalid',
        '@example.com',
        'user@',
        'user @example.com'
      ]
      invalidEmails.forEach(email => {
        const result = validateEmail(email)
        expect(result.valid).toBe(false)
        expect(result.message).toMatch(/[\u4e00-\u9fa5]/)
      })
    })
  })

  describe('checkPasswordStrength - 密码强度检查', () => {
    it('应该识别弱密码', () => {
      const weakPasswords = ['123', '12345', 'abc']
      weakPasswords.forEach(pwd => {
        const result = checkPasswordStrength(pwd)
        expect(result.level).toBe('weak')
        expect(result.color).toBe('red')
        expect(result.message).toContain('弱')
      })
    })

    it('应该识别中等强度密码', () => {
      const mediumPasswords = ['abc123', 'pass123']
      mediumPasswords.forEach(pwd => {
        const result = checkPasswordStrength(pwd)
        expect(result.level).toBe('medium')
        expect(result.color).toBe('orange')
        expect(result.message).toContain('中等')
      })
    })

    it('应该识别强密码', () => {
      const strongPasswords = ['Abc123!@#', 'MyP@ssw0rd', 'Secure#Pass123']
      strongPasswords.forEach(pwd => {
        const result = checkPasswordStrength(pwd)
        expect(result.level).toBe('strong')
        expect(result.color).toBe('green')
        expect(result.message).toContain('强')
      })
    })

    it('应该处理空密码', () => {
      const result = checkPasswordStrength('')
      expect(result.level).toBe('none')
      expect(result.message).toContain('请输入密码')
    })

    it('管理员密码123456应该被识别为弱密码', () => {
      const result = checkPasswordStrength('123456')
      expect(result.level).toBe('weak')
      expect(result.message).toContain('弱')
    })

    it('应该返回正确的颜色代码', () => {
      expect(checkPasswordStrength('123').color).toBe('red')
      expect(checkPasswordStrength('abc123').color).toBe('orange')
      expect(checkPasswordStrength('Abc123!@#').color).toBe('green')
    })

    it('应该提供改进建议', () => {
      const weakResult = checkPasswordStrength('123')
      expect(weakResult.message).toContain('建议')
      
      const mediumResult = checkPasswordStrength('abc123')
      expect(mediumResult.message).toContain('可以')
    })
  })

  describe('边界情况测试', () => {
    it('所有验证函数应该返回包含valid字段的对象', () => {
      const results = [
        validateRequired('test'),
        validateFileSize({ size: 1024 }),
        validateImageType({ type: 'image/png' }),
        validatePhone('13800138000'),
        validateStudentId('123456')
      ]
      
      results.forEach(result => {
        expect(result).toHaveProperty('valid')
        expect(typeof result.valid).toBe('boolean')
      })
    })

    it('失败的验证应该返回中文错误消息', () => {
      const results = [
        validateRequired(''),
        validateFileSize({ size: 100 * 1024 * 1024 }),
        validateImageType({ type: 'text/plain' }),
        validatePhone('123'),
        validateStudentId('123')
      ]
      
      results.forEach(result => {
        expect(result.valid).toBe(false)
        expect(result.message).toBeDefined()
        expect(result.message).toMatch(/[\u4e00-\u9fa5]/) // 包含中文字符
      })
    })

    it('成功的验证不应该返回错误消息', () => {
      const results = [
        validateRequired('test'),
        validateFileSize({ size: 1024 }),
        validateImageType({ type: 'image/png' }),
        validatePhone('13800138000'),
        validateStudentId('123456')
      ]
      
      results.forEach(result => {
        expect(result.valid).toBe(true)
        expect(result.message).toBeUndefined()
      })
    })
  })
})
