/**
 * 敏感词高亮工具
 * 用于在内容审核时高亮显示敏感词
 */

/**
 * 高亮敏感词
 * @param {string} text - 原始文本
 * @param {string[]} sensitiveWords - 敏感词列表
 * @returns {string} - 带高亮标记的 HTML 字符串
 */
export function highlightSensitiveWords(text, sensitiveWords) {
  if (!text || !sensitiveWords || sensitiveWords.length === 0) {
    return escapeHtml(text)
  }

  let result = escapeHtml(text)

  sensitiveWords.forEach(word => {
    if (!word) return
    
    const escapedWord = escapeRegExp(word)
    const regex = new RegExp(escapedWord, 'gi')
    
    result = result.replace(regex, match => {
      return `<mark class="sensitive-word">${match}</mark>`
    })
  })

  return result
}

/**
 * 转义 HTML 特殊字符，防止 XSS
 * @param {string} text - 原始文本
 * @returns {string} - 转义后的文本
 */
function escapeHtml(text) {
  if (!text) return ''
  
  const map = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#039;'
  }
  
  return text.replace(/[&<>"']/g, m => map[m])
}

/**
 * 转义正则表达式特殊字符
 * @param {string} string - 原始字符串
 * @returns {string} - 转义后的字符串
 */
function escapeRegExp(string) {
  return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

/**
 * 检测文本中是否包含敏感词
 * @param {string} text - 原始文本
 * @param {string[]} sensitiveWords - 敏感词列表
 * @returns {boolean} - 是否包含敏感词
 */
export function containsSensitiveWords(text, sensitiveWords) {
  if (!text || !sensitiveWords || sensitiveWords.length === 0) {
    return false
  }

  return sensitiveWords.some(word => {
    if (!word) return false
    return text.toLowerCase().includes(word.toLowerCase())
  })
}

/**
 * 获取文本中的敏感词列表
 * @param {string} text - 原始文本
 * @param {string[]} sensitiveWords - 敏感词列表
 * @returns {string[]} - 匹配到的敏感词
 */
export function findSensitiveWords(text, sensitiveWords) {
  if (!text || !sensitiveWords || sensitiveWords.length === 0) {
    return []
  }

  const found = []
  const lowerText = text.toLowerCase()

  sensitiveWords.forEach(word => {
    if (!word) return
    if (lowerText.includes(word.toLowerCase())) {
      found.push(word)
    }
  })

  return [...new Set(found)]
}
