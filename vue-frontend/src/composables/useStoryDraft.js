import { ref, watch } from 'vue'

/**
 * 故事续写草稿管理
 * 使用LocalStorage保存草稿，防止用户内容丢失
 */
export function useStoryDraft(storyId, userId) {
  const draftKey = `story_draft_${storyId}_${userId}`
  const lastSaveTime = ref(null)
  const hasDraft = ref(false)

  // 加载草稿
  const loadDraft = () => {
    try {
      const draftStr = localStorage.getItem(draftKey)
      if (draftStr) {
        const draft = JSON.parse(draftStr)
        
        // 检查草稿是否过期（24小时）
        const age = Date.now() - draft.timestamp
        if (age < 24 * 60 * 60 * 1000) {
          lastSaveTime.value = draft.timestamp
          hasDraft.value = true
          return draft
        } else {
          // 过期草稿自动清除
          clearDraft()
        }
      }
    } catch (error) {
      console.error('加载草稿失败:', error)
    }
    return null
  }

  // 保存草稿
  const saveDraft = (formData) => {
    try {
      const draft = {
        ...formData,
        storyId,
        userId,
        timestamp: Date.now()
      }
      localStorage.setItem(draftKey, JSON.stringify(draft))
      lastSaveTime.value = Date.now()
      hasDraft.value = true
      return true
    } catch (error) {
      console.error('保存草稿失败:', error)
      return false
    }
  }

  // 清除草稿
  const clearDraft = () => {
    try {
      localStorage.removeItem(draftKey)
      lastSaveTime.value = null
      hasDraft.value = false
      return true
    } catch (error) {
      console.error('清除草稿失败:', error)
      return false
    }
  }

  // 获取草稿信息文本
  const getDraftInfo = () => {
    if (!lastSaveTime.value) return null
    const time = new Date(lastSaveTime.value).toLocaleTimeString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit'
    })
    return `草稿已保存于 ${time}`
  }

  return {
    loadDraft,
    saveDraft,
    clearDraft,
    getDraftInfo,
    hasDraft,
    lastSaveTime
  }
}
