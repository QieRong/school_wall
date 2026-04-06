// File: vue-frontend/src/composables/useKeyboard.js
// 键盘导航和快捷键支持

import { onMounted, onUnmounted } from 'vue'

/**
 * 键盘快捷键 Hook
 * @param {Object} shortcuts - 快捷键配置对象 { 'Escape': callback, 'Enter': callback }
 */
export function useKeyboardShortcuts(shortcuts) {
  const handleKeydown = (event) => {
    const key = event.key
    
    // 检查是否有对应的快捷键处理函数
    if (shortcuts[key]) {
      // 防止在输入框中触发（除非明确指定）
      const isInputElement = ['INPUT', 'TEXTAREA'].includes(event.target.tagName)
      if (!isInputElement || shortcuts[key].allowInInput) {
        event.preventDefault()
        shortcuts[key].handler ? shortcuts[key].handler(event) : shortcuts[key](event)
      }
    }
    
    // 组合键支持
    if (event.ctrlKey || event.metaKey) {
      const comboKey = `Ctrl+${key}`
      if (shortcuts[comboKey]) {
        event.preventDefault()
        shortcuts[comboKey].handler ? shortcuts[comboKey].handler(event) : shortcuts[comboKey](event)
      }
    }
  }
  
  onMounted(() => {
    window.addEventListener('keydown', handleKeydown)
  })
  
  onUnmounted(() => {
    window.removeEventListener('keydown', handleKeydown)
  })
}

/**
 * 对话框键盘导航 Hook
 * @param {Ref} isOpen - 对话框是否打开的响应式引用
 * @param {Function} onClose - 关闭对话框的回调
 * @param {Function} onConfirm - 确认操作的回调（可选）
 */
export function useDialogKeyboard(isOpen, onClose, onConfirm = null) {
  const shortcuts = {
    'Escape': () => {
      if (isOpen.value) {
        onClose()
      }
    }
  }
  
  if (onConfirm) {
    shortcuts['Enter'] = {
      handler: () => {
        if (isOpen.value) {
          onConfirm()
        }
      },
      allowInInput: false
    }
  }
  
  useKeyboardShortcuts(shortcuts)
}

/**
 * Tab 键焦点管理
 * @param {Ref} containerRef - 容器元素的引用
 */
export function useFocusTrap(containerRef) {
  const handleKeydown = (event) => {
    if (event.key !== 'Tab' || !containerRef.value) return
    
    const focusableElements = containerRef.value.querySelectorAll(
      'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
    )
    
    const firstElement = focusableElements[0]
    const lastElement = focusableElements[focusableElements.length - 1]
    
    if (event.shiftKey) {
      // Shift + Tab
      if (document.activeElement === firstElement) {
        event.preventDefault()
        lastElement.focus()
      }
    } else {
      // Tab
      if (document.activeElement === lastElement) {
        event.preventDefault()
        firstElement.focus()
      }
    }
  }
  
  onMounted(() => {
    window.addEventListener('keydown', handleKeydown)
  })
  
  onUnmounted(() => {
    window.removeEventListener('keydown', handleKeydown)
  })
}

/**
 * 自动聚焦到第一个输入框
 * @param {Ref} containerRef - 容器元素的引用
 * @param {Boolean} enabled - 是否启用自动聚焦
 */
export function useAutoFocus(containerRef, enabled = true) {
  onMounted(() => {
    if (!enabled || !containerRef.value) return
    
    setTimeout(() => {
      const firstInput = containerRef.value.querySelector('input, textarea')
      if (firstInput) {
        firstInput.focus()
      }
    }, 100)
  })
}
