<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import { Badge } from '@/components/ui/badge'
import {
  PenTool, Clock, AlertCircle,
  Flame, X, Save, FileText, Sparkles, Loader2
} from 'lucide-vue-next'
import { useStoryDraft } from '@/composables/useStoryDraft'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { useAiStore } from '@/stores/ai'
import request from '@/api/request'
import { releaseStoryLock, heartbeatStoryLock } from '@/api/story'

const props = defineProps({
  open: {
    type: Boolean,
    default: false
  },
  canContinueResult: {
    type: Object,
    default: null
  },
  contextParagraphs: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  storyId: {
    type: [String, Number],
    required: true
  }
})

const emit = defineEmits(['update:open', 'submit'])

const appStore = useAppStore()
const userStore = useUserStore()
const aiStore = useAiStore()

// 表单数据
const form = ref({
  content: '',
  penName: '',
  imageUrl: '',
  isKeyPoint: false
})

// 草稿管理
const { loadDraft, saveDraft, clearDraft, getDraftInfo, hasDraft } = useStoryDraft(
  props.storyId,
  userStore.user?.id
)

// 笔名选项
const penNameOptions = [
  { value: '', label: '使用真实昵称' },
  { value: '旅人A', label: '旅人A' },
  { value: '旅人B', label: '旅人B' },
  { value: '旅人C', label: '旅人C' },
  { value: '神秘来客', label: '神秘来客' },
  { value: '匿名作者', label: '匿名作者' }
]

// 字数统计
const contentLength = computed(() => form.value.content.length)
const isContentValid = computed(() => contentLength.value >= 100 && contentLength.value <= 500)

// 草稿信息
const draftInfo = computed(() => getDraftInfo())

// 自动保存定时器
let autoSaveTimer = null

// AI润色相关状态
const aiPolishing = ref(false)
const aiAssisted = ref(false) // 是否已应用AI润色结果

// AI润色功能
const handleAiPolish = async () => {
  if (!form.value.content.trim()) {
    appStore.showToast('请先输入内容再润色', 'warning')
    return
  }

  if (aiStore.remaining <= 0) {
    appStore.showToast('今日AI次数已用完，明天再来吧~', 'error')
    return
  }

  aiPolishing.value = true
  try {
    const res = await request.post('/ai/polish', {
      userId: userStore.user.id,
      content: form.value.content,
      style: '更加优美动人，适合故事续写'
    }, { timeout: 30000 })

    if (res.code === '200') {
      form.value.content = res.data.content || form.value.content
      // 更新Store中的AI次数
      aiStore.updateRemaining(res.data.remaining)
      aiAssisted.value = true  // 润色成功即标记
      appStore.showToast('✨ AI润色完成', 'success')
    } else {
      appStore.showToast(res.msg || 'AI润色失败', 'error')
    }
  } catch (e) {
    console.error('AI润色错误:', e)
    if (e.message?.includes('超时')) {
      appStore.showToast('AI 开小差了，请稍后再试 🤖', 'warning')
    } else {
      appStore.showToast('AI润色失败，请重试', 'error')
    }
  } finally {
    aiPolishing.value = false
  }
}

// 自动保存草稿（防抖2秒）
const autoSave = () => {
  clearTimeout(autoSaveTimer)
  autoSaveTimer = setTimeout(() => {
    if (form.value.content.trim()) {
      saveDraft(form.value)
    }
  }, 2000)
}

// 监听内容变化，自动保存
watch(() => form.value.content, () => {
  if (props.open && form.value.content.trim()) {
    autoSave()
  }
})

// 手动保存草稿
const handleSaveDraft = () => {
  if (saveDraft(form.value)) {
    appStore.showToast('草稿已保存', 'success')
  } else {
    appStore.showToast('保存草稿失败', 'error')
  }
}

// 加载草稿
const handleLoadDraft = () => {
  const draft = loadDraft()
  if (draft) {
    form.value = {
      content: draft.content || '',
      penName: draft.penName || '',
      imageUrl: draft.imageUrl || '',
      isKeyPoint: draft.isKeyPoint || false
    }
    appStore.showToast('已恢复草稿', 'success')
  }
}

// 清除草稿
const handleClearDraft = () => {
  if (clearDraft()) {
    form.value = {
      content: '',
      penName: '',
      imageUrl: '',
      isKeyPoint: false
    }
    appStore.showToast('草稿已清除', 'success')
  }
}

// 剩余时间格式化
const remainingTimeText = computed(() => {
  if (!props.canContinueResult?.remainingSeconds) return ''
  const seconds = props.canContinueResult.remainingSeconds
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  if (hours > 0) return `${hours}小时${minutes}分钟`
  return `${minutes}分钟`
})

// 关闭弹窗
const handleClose = () => {
  // 关闭前自动保存草稿
  if (form.value.content.trim()) {
    saveDraft(form.value)
  }
  emit('update:open', false)
}

// 提交续写
const handleSubmit = () => {
  if (!isContentValid.value) return
  emit('submit', { ...form.value, isAiAssisted: aiAssisted.value ? 1 : 0 })
  // 提交成功后会由父组件清除草稿
}

// 打开弹窗时加载草稿
watch(() => props.open, (val) => {
  if (val) {
    // 获取AI剩余次数
    aiStore.fetchRemaining(userStore.user?.id)

    // 尝试加载草稿
    const draft = loadDraft()
    if (draft && draft.content) {
      form.value = {
        content: draft.content || '',
        penName: draft.penName || '',
        imageUrl: draft.imageUrl || '',
        isKeyPoint: draft.isKeyPoint || false
      }
    } else {
      // 没有草稿，重置表单
      form.value = {
        content: '',
        penName: '',
        imageUrl: '',
        isKeyPoint: false
      }
      aiAssisted.value = false  // 重置AI状态
    }
  }
})

// ================= 锁的管理 =================
let heartbeatTimer = null

watch(() => props.open, (isOpen) => {
  if (isOpen) {
    if (userStore.user?.id) {
      heartbeatTimer = setInterval(() => {
        heartbeatStoryLock(props.storyId, userStore.user.id).catch(() => {})
      }, 30 * 1000)
    }
  } else {
    if (heartbeatTimer) {
      clearInterval(heartbeatTimer)
      heartbeatTimer = null
    }
    if (userStore.user?.id) {
      releaseStoryLock(props.storyId, userStore.user.id).catch(() => {})
    }
  }
})

onBeforeUnmount(() => {
  if (heartbeatTimer) clearInterval(heartbeatTimer)
  if (userStore.user?.id) {
    releaseStoryLock(props.storyId, userStore.user.id).catch(() => {})
  }
})

// 组件卸载时清理定时器
onMounted(() => {
  return () => {
    clearTimeout(autoSaveTimer)
  }
})
</script>

<template>
  <Dialog :open="open" @update:open="handleClose">
    <DialogContent class="sm:max-w-2xl continue-dialog">
      <DialogHeader class="dialog-header-fixed">
        <DialogTitle class="flex items-center gap-2 text-emerald-800 font-serif">
          <PenTool class="w-5 h-5" /> 续写故事
        </DialogTitle>
      </DialogHeader>

      <div class="dialog-body-scrollable">
        <!-- 不满足续写条件 -->
        <div v-if="canContinueResult && !canContinueResult.canContinue" class="ineligible-state">
          <div class="icon-wrapper">
            <AlertCircle class="w-12 h-12 text-emerald-600" />
          </div>
          <p class="reason-text">{{ canContinueResult.reason }}</p>
          <div v-if="canContinueResult.remainingSeconds" class="countdown">
            <Clock class="w-4 h-4" />
            <span>还需等待 {{ remainingTimeText }}</span>
          </div>
          <div v-if="canContinueResult.unreadCount" class="unread-hint">
            <span>还需阅读 {{ canContinueResult.unreadCount }} 段内容</span>
          </div>
        </div>

        <!-- 可以续写 -->
        <div v-else class="continue-form">
          <!-- 上下文预览 -->
          <div class="context-section">
            <div class="context-header">
              <span class="context-title">📜 前文回顾</span>
              <Badge variant="outline" class="text-amber-600">最近3段</Badge>
            </div>
            <div class="context-content">
              <div v-for="(p, idx) in contextParagraphs" :key="p.id" class="context-paragraph">
                <span class="paragraph-number">{{ idx + 1 }}</span>
                <p class="paragraph-text">{{ p.content }}</p>
              </div>
              <div v-if="contextParagraphs.length === 0" class="empty-context">
                暂无前文内容
              </div>
            </div>
          </div>

          <!-- 续写位置指示 -->
          <div class="position-indicator">
            <div class="indicator-line"></div>
            <span class="indicator-text">✍️ 从这里开始续写</span>
            <div class="indicator-line"></div>
          </div>

          <!-- 草稿信息提示 -->
          <div v-if="hasDraft && draftInfo" class="draft-info">
            <FileText class="w-4 h-4 text-blue-600" />
            <span class="draft-text">{{ draftInfo }}</span>
            <Button variant="ghost" size="sm" class="clear-draft-btn" @click="handleClearDraft">
              <X class="w-3 h-3" />
            </Button>
          </div>

          <!-- 续写编辑区 -->
          <div class="editor-section">
            <Textarea v-model="form.content" placeholder="接续前文，写下你的故事... (Enter换行)" class="story-textarea"
              :maxlength="500" />
            <div class="editor-toolbar">
              <Button variant="ghost" size="sm" @click="handleAiPolish" :disabled="!form.content.trim() || aiPolishing"
                class="ai-polish-btn">
                <Loader2 v-if="aiPolishing" class="w-3 h-3 mr-1 animate-spin" />
                <Sparkles v-else class="w-3 h-3 mr-1" />
                <span class="text-xs">{{ aiPolishing ? 'AI润色中...' : `AI润色 (${aiStore.remaining})` }}</span>
              </Button>
              <div class="char-count" :class="{ 'invalid': !isContentValid && contentLength > 0 }">
                <span v-if="contentLength < 100" class="hint">至少100字</span>
                {{ contentLength }}/500
              </div>
            </div>
          </div>

          <!-- 选项区域 -->
          <div class="options-section">
            <!-- 笔名选择 -->
            <div class="option-item">
              <label class="option-label">笔名</label>
              <select v-model="form.penName" class="pen-name-select">
                <option v-for="opt in penNameOptions" :key="opt.value" :value="opt.value">
                  {{ opt.label }}
                </option>
              </select>
            </div>

            <!-- 关键剧情点 -->
            <label class="key-point-option">
              <input type="checkbox" v-model="form.isKeyPoint" class="key-point-checkbox" />
              <Flame class="w-4 h-4 text-orange-500" />
              <span>标记为关键剧情点</span>
            </label>
          </div>
        </div>
      </div>

      <DialogFooter v-if="canContinueResult?.canContinue" class="dialog-footer-fixed">
        <div class="footer-left">
          <Button variant="ghost" size="sm" @click="handleSaveDraft" :disabled="!form.content.trim()"
            class="whitespace-nowrap">
            <Save class="w-4 h-4 mr-1" />
            <span class="text-sm">草稿</span>
          </Button>
        </div>
        <div class="footer-right">
          <Button variant="outline" @click="handleClose" :disabled="loading">
            取消
          </Button>
          <Button class="submit-btn" :disabled="loading || !isContentValid" @click="handleSubmit">
            <PenTool class="w-4 h-4 mr-2" />
            {{ loading ? '提交中...' : '提交续写' }}
          </Button>
        </div>
      </DialogFooter>

      <DialogFooter v-else class="dialog-footer-fixed">
        <Button variant="outline" @click="handleClose">
          我知道了
        </Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>

<style scoped>
.continue-dialog {
  background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
  max-height: 90vh;
  height: 90vh;      /* 固定占用高度，避免内容撑破 */
  display: flex;
  flex-direction: column;
  overflow: hidden;  /* 防止内容溢出容器 */
}

.dialog-header-fixed {
  flex-shrink: 0;
  padding: 16px 24px 12px;
  border-bottom: 1px solid rgba(33, 111, 85, 0.1);
}

.dialog-body-scrollable {
  flex: 1;
  overflow-y: auto;
  padding: 12px 24px;
  min-height: 0; /* 关键：让 flex 子元素可以缩减 */
}

.dialog-footer-fixed {
  flex-shrink: 0;
  padding: 12px 24px 16px;
  border-top: 1px solid rgba(33, 111, 85, 0.1);
  background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
}

/* 不满足条件状态 */
.ineligible-state {
  padding: 32px 16px;
  text-align: center;
}

.icon-wrapper {
  margin-bottom: 16px;
}

.reason-text {
  color: #065f46;
  font-size: 16px;
  margin-bottom: 12px;
}

.countdown {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #059669;
  font-size: 14px;
  padding: 8px 16px;
  background: rgba(33, 111, 85, 0.1);
  border-radius: 20px;
  margin: 0 auto;
  width: fit-content;
}

.unread-hint {
  margin-top: 12px;
  color: rgb(33, 111, 85);
  font-size: 14px;
}

/* 续写表单 */
.continue-form {
  padding: 8px 0;
}

/* 上下文区域 */
.context-section {
  background: rgba(0, 0, 0, 0.03);
  border-radius: 12px;
  padding: 12px;
  margin-bottom: 16px;
}

.context-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.context-title {
  font-size: 14px;
  font-weight: 600;
  color: #065f46;
}

.context-content {
  max-height: 110px;
  overflow-y: auto;
}

.context-paragraph {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  padding-bottom: 8px;
  border-bottom: 1px dashed rgba(33, 111, 85, 0.2);
}

.context-paragraph:last-child {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

.paragraph-number {
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba(33, 111, 85, 0.2);
  color: #065f46;
  font-size: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.paragraph-text {
  font-size: 13px;
  color: #78716c;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.empty-context {
  text-align: center;
  color: #a8a29e;
  padding: 16px;
}

/* 位置指示器 */
.position-indicator {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 16px 0;
}

.indicator-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgb(33, 111, 85), transparent);
}

.indicator-text {
  font-size: 13px;
  color: rgb(33, 111, 85);
  white-space: nowrap;
}

/* 编辑区 */
.editor-section {
  position: relative;
  margin-bottom: 16px;
}

.story-textarea {
  min-height: 130px;
  max-height: 260px;
  background: #ecfdf5;
  border: 2px solid rgb(33, 111, 85);
  border-radius: 8px;
  font-family: 'Noto Serif SC', serif;
  font-size: 15px;
  line-height: 2;
  padding: 12px 16px;
  resize: vertical;
  background-image: repeating-linear-gradient(transparent,
      transparent 29px,
      rgba(33, 111, 85, 0.1) 30px);
  background-size: 100% 30px;
}

.story-textarea:focus {
  outline: none;
  border-color: #059669;
  box-shadow: 0 0 0 3px rgba(33, 111, 85, 0.2);
}

.char-count {
  position: absolute;
  bottom: 8px;
  right: 12px;
  font-size: 12px;
  color: #065f46;
  background: rgba(236, 253, 245, 0.9);
  padding: 2px 8px;
  border-radius: 4px;
}

.char-count.invalid {
  color: #dc2626;
}

.char-count .hint {
  margin-right: 8px;
  color: #dc2626;
}

/* 选项区域 */
.options-section {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: center;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.option-label {
  font-size: 14px;
  color: #065f46;
  font-weight: 500;
}

.pen-name-select {
  padding: 6px 12px;
  border: 1px solid rgb(33, 111, 85);
  border-radius: 6px;
  background: #ecfdf5;
  color: #065f46;
  font-size: 14px;
  cursor: pointer;
}

.pen-name-select:focus {
  outline: none;
  border-color: #059669;
}

.key-point-option {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: 14px;
  color: #065f46;
}

.key-point-checkbox {
  width: 16px;
  height: 16px;
  border-radius: 4px;
  border: 1px solid rgb(33, 111, 85);
  cursor: pointer;
}

/* 提交按钮 */
.submit-btn {
  background: linear-gradient(135deg, rgb(33, 111, 85) 0%, #059669 100%);
  color: white;
  border: none;
}

.submit-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #059669 0%, #065f46 100%);
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 草稿信息提示 */
.draft-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: rgba(59, 130, 246, 0.1);
  border: 1px solid rgba(59, 130, 246, 0.3);
  border-radius: 8px;
  margin-bottom: 12px;
  font-size: 13px;
  color: #1e40af;
}

.draft-text {
  flex: 1;
}

.clear-draft-btn {
  padding: 4px;
  height: auto;
  color: #64748b;
}

.clear-draft-btn:hover {
  color: #dc2626;
  background: rgba(220, 38, 38, 0.1);
}

/* 底部按钮布局 */
:deep(.dialog-footer) {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.footer-left {
  flex: 0;
}

.footer-right {
  display: flex;
  gap: 8px;
}
</style>


/* AI润色按钮样式 */
.editor-toolbar {
display: flex;
justify-content: space-between;
align-items: center;
margin-top: 8px;
}

.ai-polish-btn {
color: #7c3aed;
border: 1px solid #c4b5fd;
background: linear-gradient(135deg, #faf5ff 0%, #f3e8ff 100%);
padding: 6px 12px;
height: auto;
font-size: 12px;
transition: all 0.2s;
}

.ai-polish-btn:hover:not(:disabled) {
background: linear-gradient(135deg, #f3e8ff 0%, #e9d5ff 100%);
border-color: #a78bfa;
transform: translateY(-1px);
box-shadow: 0 2px 8px rgba(124, 58, 237, 0.2);
}

.ai-polish-btn:disabled {
opacity: 0.5;
cursor: not-allowed;
}
