/**
 * 文件上传验证单元测试
 * Feature: pre-defense-quality-fixes
 * Requirements: 1.1, 1.2, 1.3, 1.4, 1.5
 */

import { describe, it, expect } from 'vitest'
import { createMockFile, isValidImageType, isValidVideoType, isValidFileSize } from '../helpers/testUtils'

// 允许的文件类型常量
const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp']
const ALLOWED_VIDEO_TYPES = ['video/mp4', 'video/webm', 'video/ogg', 'video/quicktime']
const MAX_IMAGE_SIZE = 10 * 1024 * 1024 // 10MB
const MAX_VIDEO_SIZE = 100 * 1024 * 1024 // 100MB

/**
 * 文件 MIME 类型验证函数
 */
function validateFileMimeType(file, type) {
  const allowedTypes = type === 'image' ? ALLOWED_IMAGE_TYPES : ALLOWED_VIDEO_TYPES
  const isValid = allowedTypes.includes(file.type)
  
  return {
    isValid,
    errorMessage: isValid ? null : `不支持的${type === 'image' ? '图片' : '视频'}格式`
  }
}

/**
 * 文件大小验证函数
 */
function validateFileSize(file, type) {
  const maxSize = type === 'image' ? MAX_IMAGE_SIZE : MAX_VIDEO_SIZE
  const isValid = file.size > 0 && file.size <= maxSize
  
  return {
    isValid,
    errorMessage: isValid ? null : `文件过大，最大${maxSize / 1024 / 1024}MB`
  }
}

/**
 * 完整文件验证函数
 */
function validateFile(file, type) {
  // 先验证 MIME 类型
  const mimeValidation = validateFileMimeType(file, type)
  if (!mimeValidation.isValid) return mimeValidation
  
  // 再验证大小
  const sizeValidation = validateFileSize(file, type)
  if (!sizeValidation.isValid) return sizeValidation
  
  return { isValid: true, errorMessage: null }
}

/**
 * 批量文件验证函数
 */
function validateFiles(files, type) {
  const results = []
  const validFiles = []
  
  for (const file of files) {
    const validation = validateFile(file, type)
    results.push({
      file,
      ...validation
    })
    
    if (validation.isValid) {
      validFiles.push(file)
    }
  }
  
  return {
    results,
    validFiles,
    hasErrors: results.some(r => !r.isValid)
  }
}

describe('文件上传验证 - 单元测试', () => {
  
  describe('MIME 类型验证逻辑', () => {
    
    it('应该接受所有允许的图片格式', () => {
      ALLOWED_IMAGE_TYPES.forEach(type => {
        const file = { name: 'test.jpg', type, size: 1000 }
        const result = validateFileMimeType(file, 'image')
        expect(result.isValid).toBe(true)
        expect(result.errorMessage).toBeNull()
      })
    })
    
    it('应该接受所有允许的视频格式', () => {
      ALLOWED_VIDEO_TYPES.forEach(type => {
        const file = { name: 'test.mp4', type, size: 1000 }
        const result = validateFileMimeType(file, 'video')
        expect(result.isValid).toBe(true)
        expect(result.errorMessage).toBeNull()
      })
    })
    
    it('应该拒绝 .txt 文件', () => {
      const file = { name: 'test.txt', type: 'text/plain', size: 1000 }
      const result = validateFileMimeType(file, 'image')
      expect(result.isValid).toBe(false)
      expect(result.errorMessage).toContain('不支持')
    })
    
    it('应该拒绝 .pdf 文件', () => {
      const file = { name: 'test.pdf', type: 'application/pdf', size: 1000 }
      const result = validateFileMimeType(file, 'image')
      expect(result.isValid).toBe(false)
      expect(result.errorMessage).toContain('不支持')
    })
    
    it('应该拒绝 .zip 文件', () => {
      const file = { name: 'test.zip', type: 'application/zip', size: 1000 }
      const result = validateFileMimeType(file, 'video')
      expect(result.isValid).toBe(false)
      expect(result.errorMessage).toContain('不支持')
    })
    
    it('应该拒绝空 MIME 类型', () => {
      const file = { name: 'test', type: '', size: 1000 }
      const result = validateFileMimeType(file, 'image')
      expect(result.isValid).toBe(false)
    })
    
    it('应该拒绝未知 MIME 类型', () => {
      const file = { name: 'test', type: 'application/unknown', size: 1000 }
      const result = validateFileMimeType(file, 'image')
      expect(result.isValid).toBe(false)
    })
  })
  
  describe('文件大小验证逻辑', () => {
    
    it('应该接受小于最大限制的图片', () => {
      const file = { name: 'test.jpg', type: 'image/jpeg', size: 5 * 1024 * 1024 } // 5MB
      const result = validateFileSize(file, 'image')
      expect(result.isValid).toBe(true)
      expect(result.errorMessage).toBeNull()
    })
    
    it('应该接受等于最大限制的图片', () => {
      const file = { name: 'test.jpg', type: 'image/jpeg', size: MAX_IMAGE_SIZE }
      const result = validateFileSize(file, 'image')
      expect(result.isValid).toBe(true)
    })
    
    it('应该拒绝超过最大限制的图片', () => {
      const file = { name: 'test.jpg', type: 'image/jpeg', size: MAX_IMAGE_SIZE + 1 }
      const result = validateFileSize(file, 'image')
      expect(result.isValid).toBe(false)
      expect(result.errorMessage).toContain('过大')
      expect(result.errorMessage).toContain('10MB')
    })
    
    it('应该接受小于最大限制的视频', () => {
      const file = { name: 'test.mp4', type: 'video/mp4', size: 50 * 1024 * 1024 } // 50MB
      const result = validateFileSize(file, 'video')
      expect(result.isValid).toBe(true)
    })
    
    it('应该拒绝超过最大限制的视频', () => {
      const file = { name: 'test.mp4', type: 'video/mp4', size: MAX_VIDEO_SIZE + 1 }
      const result = validateFileSize(file, 'video')
      expect(result.isValid).toBe(false)
      expect(result.errorMessage).toContain('过大')
      expect(result.errorMessage).toContain('100MB')
    })
    
    it('应该拒绝大小为 0 的文件', () => {
      const file = { name: 'test.jpg', type: 'image/jpeg', size: 0 }
      const result = validateFileSize(file, 'image')
      expect(result.isValid).toBe(false)
    })
    
    it('应该拒绝负数大小的文件', () => {
      const file = { name: 'test.jpg', type: 'image/jpeg', size: -1 }
      const result = validateFileSize(file, 'image')
      expect(result.isValid).toBe(false)
    })
  })
  
  describe('批量验证逻辑', () => {
    
    it('应该独立验证每个文件', () => {
      const files = [
        { name: 'valid.jpg', type: 'image/jpeg', size: 1000 },
        { name: 'invalid.txt', type: 'text/plain', size: 1000 },
        { name: 'valid2.png', type: 'image/png', size: 2000 }
      ]
      
      const result = validateFiles(files, 'image')
      
      expect(result.results).toHaveLength(3)
      expect(result.results[0].isValid).toBe(true)
      expect(result.results[1].isValid).toBe(false)
      expect(result.results[2].isValid).toBe(true)
    })
    
    it('应该只返回有效的文件', () => {
      const files = [
        { name: 'valid.jpg', type: 'image/jpeg', size: 1000 },
        { name: 'invalid.txt', type: 'text/plain', size: 1000 },
        { name: 'toolarge.jpg', type: 'image/jpeg', size: MAX_IMAGE_SIZE + 1 }
      ]
      
      const result = validateFiles(files, 'image')
      
      expect(result.validFiles).toHaveLength(1)
      expect(result.validFiles[0].name).toBe('valid.jpg')
    })
    
    it('应该标记是否有错误', () => {
      const validFiles = [
        { name: 'valid1.jpg', type: 'image/jpeg', size: 1000 },
        { name: 'valid2.png', type: 'image/png', size: 2000 }
      ]
      
      const invalidFiles = [
        { name: 'valid.jpg', type: 'image/jpeg', size: 1000 },
        { name: 'invalid.txt', type: 'text/plain', size: 1000 }
      ]
      
      const validResult = validateFiles(validFiles, 'image')
      const invalidResult = validateFiles(invalidFiles, 'image')
      
      expect(validResult.hasErrors).toBe(false)
      expect(invalidResult.hasErrors).toBe(true)
    })
    
    it('应该处理空数组', () => {
      const result = validateFiles([], 'image')
      
      expect(result.results).toHaveLength(0)
      expect(result.validFiles).toHaveLength(0)
      expect(result.hasErrors).toBe(false)
    })
    
    it('应该处理全部无效的文件', () => {
      const files = [
        { name: 'invalid1.txt', type: 'text/plain', size: 1000 },
        { name: 'invalid2.pdf', type: 'application/pdf', size: 2000 }
      ]
      
      const result = validateFiles(files, 'image')
      
      expect(result.validFiles).toHaveLength(0)
      expect(result.hasErrors).toBe(true)
    })
  })
  
  describe('错误消息生成', () => {
    
    it('应该为无效 MIME 类型生成清晰的错误消息', () => {
      const file = { name: 'test.txt', type: 'text/plain', size: 1000 }
      const result = validateFileMimeType(file, 'image')
      
      expect(result.errorMessage).toBeTruthy()
      expect(result.errorMessage).toContain('不支持')
      expect(result.errorMessage).toContain('图片')
    })
    
    it('应该为超大文件生成包含大小限制的错误消息', () => {
      const file = { name: 'test.jpg', type: 'image/jpeg', size: MAX_IMAGE_SIZE + 1 }
      const result = validateFileSize(file, 'image')
      
      expect(result.errorMessage).toBeTruthy()
      expect(result.errorMessage).toContain('过大')
      expect(result.errorMessage).toContain('10MB')
    })
    
    it('应该为视频文件生成正确的大小限制消息', () => {
      const file = { name: 'test.mp4', type: 'video/mp4', size: MAX_VIDEO_SIZE + 1 }
      const result = validateFileSize(file, 'video')
      
      expect(result.errorMessage).toContain('100MB')
    })
  })
  
  describe('边界情况', () => {
    
    it('应该处理 null 文件', () => {
      expect(() => validateFile(null, 'image')).toThrow()
    })
    
    it('应该处理 undefined 文件', () => {
      expect(() => validateFile(undefined, 'image')).toThrow()
    })
    
    it('应该处理缺少 type 属性的文件', () => {
      const file = { name: 'test.jpg', size: 1000 }
      const result = validateFileMimeType(file, 'image')
      expect(result.isValid).toBe(false)
    })
    
    it('应该处理缺少 size 属性的文件', () => {
      const file = { name: 'test.jpg', type: 'image/jpeg' }
      const result = validateFileSize(file, 'image')
      expect(result.isValid).toBe(false)
    })
    
    it('应该处理非常大的文件大小', () => {
      const file = { name: 'huge.jpg', type: 'image/jpeg', size: Number.MAX_SAFE_INTEGER }
      const result = validateFileSize(file, 'image')
      expect(result.isValid).toBe(false)
    })
  })
})
