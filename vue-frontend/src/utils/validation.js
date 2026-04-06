/**
 * 表单验证工具函数
 * 提供统一的、友好的中文验证提示
 */

/**
 * 验证非空
 * @param {string} value - 要验证的值
 * @param {string} fieldName - 字段名称，用于错误提示
 * @returns {{valid: boolean, message?: string}}
 */
export const validateRequired = (value, fieldName = '内容') => {
  if (!value || !value.trim()) {
    return { valid: false, message: `请输入${fieldName}` }
  }
  return { valid: true }
}

/**
 * 验证文件大小
 * @param {File} file - 文件对象
 * @param {number} maxSizeMB - 最大大小（MB）
 * @returns {{valid: boolean, message?: string}}
 */
export const validateFileSize = (file, maxSizeMB = 10) => {
  if (!file) {
    return { valid: false, message: '请选择文件' }
  }
  if (file.size > maxSizeMB * 1024 * 1024) {
    return { valid: false, message: `文件大小不能超过${maxSizeMB}MB` }
  }
  return { valid: true }
}

/**
 * 验证图片格式
 * @param {File} file - 文件对象
 * @returns {{valid: boolean, message?: string}}
 */
export const validateImageType = (file) => {
  if (!file) {
    return { valid: false, message: '请选择图片' }
  }
  const validTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp']
  if (!validTypes.includes(file.type)) {
    return { valid: false, message: '只支持 JPG、PNG、GIF、WebP 格式的图片' }
  }
  return { valid: true }
}

/**
 * 验证手机号
 * @param {string} phone - 手机号
 * @returns {{valid: boolean, message?: string}}
 */
export const validatePhone = (phone) => {
  if (!phone || !phone.trim()) {
    return { valid: false, message: '请输入手机号码' }
  }
  const phoneReg = /^1[3-9]\d{9}$/
  if (!phoneReg.test(phone.trim())) {
    return { valid: false, message: '请输入正确的手机号码（11位数字）' }
  }
  return { valid: true }
}

/**
 * 验证学号
 * @param {string} id - 学号
 * @returns {{valid: boolean, message?: string}}
 */
export const validateStudentId = (id) => {
  if (!id || !id.trim()) {
    return { valid: false, message: '请输入学号' }
  }
  if (id.trim().length < 6) {
    return { valid: false, message: '请输入正确的学号（至少6位）' }
  }
  return { valid: true }
}

/**
 * 验证密码（要求6-20位，不强制强密码）
 * @param {string} password - 密码
 * @returns {{valid: boolean, message?: string}}
 */
export const validatePassword = (password) => {
  if (!password) {
    return { valid: false, message: '请输入密码' }
  }
  if (password.length < 6) {
    return { valid: false, message: '密码长度不能少于6位' }
  }
  if (password.length > 20) {
    return { valid: false, message: '密码长度不能超过20位' }
  }
  return { valid: true }
}

/**
 * 检查密码强度等级（不验证，只返回强度信息）
 * @param {string} password - 密码
 * @returns {{level: string, message: string, color: string}}
 * level: 'weak' | 'medium' | 'strong'
 */
export const checkPasswordStrength = (password) => {
  if (!password) {
    return { level: 'none', message: '请输入密码', color: 'gray' }
  }

  let score = 0
  
  // 长度评分
  if (password.length >= 8) score += 2
  else if (password.length >= 6) score += 1
  
  // 包含数字
  if (/\d/.test(password)) score += 1
  
  // 包含小写字母
  if (/[a-z]/.test(password)) score += 1
  
  // 包含大写字母
  if (/[A-Z]/.test(password)) score += 1
  
  // 包含特殊字符
  if (/[!@#$%^&*(),.?":{}|<>]/.test(password)) score += 1

  // 根据分数返回强度等级
  if (score <= 2) {
    return { 
      level: 'weak', 
      message: '密码强度：弱（建议使用字母+数字组合）', 
      color: 'red' 
    }
  } else if (score <= 4) {
    return { 
      level: 'medium', 
      message: '密码强度：中等（可以添加大写字母或特殊字符）', 
      color: 'orange' 
    }
  } else {
    return { 
      level: 'strong', 
      message: '密码强度：强', 
      color: 'green' 
    }
  }
}

/**
 * 验证邮箱
 * @param {string} email - 邮箱地址
 * @returns {{valid: boolean, message?: string}}
 */
export const validateEmail = (email) => {
  if (!email || !email.trim()) {
    return { valid: false, message: '请输入邮箱地址' }
  }
  const emailReg = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailReg.test(email.trim())) {
    return { valid: false, message: '请输入正确的邮箱地址' }
  }
  return { valid: true }
}

/**
 * 验证内容长度
 * @param {string} content - 内容
 * @param {number} minLength - 最小长度
 * @param {number} maxLength - 最大长度
 * @param {string} fieldName - 字段名称
 * @returns {{valid: boolean, message?: string}}
 */
export const validateLength = (content, minLength, maxLength, fieldName = '内容') => {
  if (!content) {
    return { valid: false, message: `请输入${fieldName}` }
  }
  const length = content.trim().length
  if (length < minLength) {
    return { valid: false, message: `${fieldName}不能少于${minLength}个字符` }
  }
  if (length > maxLength) {
    return { valid: false, message: `${fieldName}不能超过${maxLength}个字符` }
  }
  return { valid: true }
}

/**
 * 验证 URL
 * @param {string} url - URL地址
 * @returns {{valid: boolean, message?: string}}
 */
export const validateUrl = (url) => {
  if (!url || !url.trim()) {
    return { valid: false, message: '请输入网址' }
  }
  try {
    new URL(url.trim())
    return { valid: true }
  } catch {
    return { valid: false, message: '请输入正确的网址格式' }
  }
}

/**
 * 验证数字范围
 * @param {number} value - 数值
 * @param {number} min - 最小值
 * @param {number} max - 最大值
 * @param {string} fieldName - 字段名称
 * @returns {{valid: boolean, message?: string}}
 */
export const validateNumberRange = (value, min, max, fieldName = '数值') => {
  if (value === null || value === undefined || value === '') {
    return { valid: false, message: `请输入${fieldName}` }
  }
  const num = Number(value)
  if (isNaN(num)) {
    return { valid: false, message: `${fieldName}必须是数字` }
  }
  if (num < min || num > max) {
    return { valid: false, message: `${fieldName}必须在${min}到${max}之间` }
  }
  return { valid: true }
}
