<script setup>
import { ref, watch } from 'vue'
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Sparkles, Loader2, Check, X } from 'lucide-vue-next'
import request from '@/api/request'
import { useAppStore } from '@/stores/app'
import { useAiStore } from '@/stores/ai'

const props = defineProps({
  open: Boolean,
  userId: [Number, String],
  currentContent: String
})
const emit = defineEmits(['update:open', 'apply'])

const appStore = useAppStore()
const aiStore = useAiStore()
const show = ref(false)
const loading = ref(false)
const resultContent = ref('')
const polishComplete = ref(false)
const abortController = ref(null)

watch(() => props.open, async (val) => {
  show.value = val
  if (val) {
    await aiStore.fetchRemaining(props.userId)
    resultContent.value = ''
    polishComplete.value = false

    // 自动开始润色
    if (props.currentContent?.trim()) {
      handlePolish()
    } else {
      appStore.showToast('请先输入要润色的内容', 'error')
      show.value = false
      emit('update:open', false)
    }
  }
})
watch(show, (val) => emit('update:open', val))

const handlePolish = async () => {
  if (!props.currentContent?.trim()) {
    appStore.showToast('请先输入要润色的内容', 'error')
    show.value = false
    return
  }
  if (aiStore.remaining <= 0) {
    appStore.showToast('今日AI次数已用完，明天再来吧~', 'error')
    show.value = false
    return
  }

  loading.value = true
  resultContent.value = ''
  polishComplete.value = false
  abortController.value = new AbortController()

  try {
    const res = await request.post('/ai/polish', {
      userId: props.userId,
      content: props.currentContent,
      style: '更加优美动人'
    }, {
      timeout: 30000,
      signal: abortController.value.signal
    })

    if (res.code === '200') {
      resultContent.value = res.data.content || ''
      // 更新Store中的AI次数
      aiStore.updateRemaining(res.data.remaining)
      polishComplete.value = true
      if (!resultContent.value) {
        appStore.showToast('AI返回内容为空，请重试', 'error')
        show.value = false
      }
    } else {
      appStore.showToast(res.msg || '润色失败', 'error')
      show.value = false
    }
  } catch (e) {
    console.error('AI润色错误:', e)
    if (e.name === 'AbortError') {
      appStore.showToast('已取消AI润色', 'info')
    } else if (e.message?.includes('超时')) {
      appStore.showToast('AI 开小差了，请稍后再试 🤖', 'warning')
    } else {
      appStore.showToast('润色失败，请检查网络或稍后重试', 'error')
    }
    show.value = false
  } finally {
    loading.value = false
  }
}

const cancelPolish = () => {
  if (abortController.value) {
    abortController.value.abort()
  }
  loading.value = false
  show.value = false
}

const applyContent = () => {
  emit('apply', resultContent.value)
  show.value = false
  appStore.showToast('已应用润色结果 ✨', 'success')
}

const cancelApply = () => {
  show.value = false
  appStore.showToast('已取消', 'info')
}
</script>

<template>
  <Dialog :open="show" @update:open="show = $event">
    <DialogContent class="sm:max-w-[500px] bg-white p-0 rounded-2xl overflow-hidden border-none shadow-2xl"
      :showClose="false">
      <DialogDescription class="sr-only">AI写作助手</DialogDescription>

      <!-- 加载状态 -->
      <div v-if="loading" class="p-8 flex flex-col items-center justify-center">
        <div
          class="w-16 h-16 bg-gradient-to-br from-[rgb(33,111,85)] to-emerald-500 rounded-2xl flex items-center justify-center mb-4 animate-pulse">
          <Sparkles class="w-8 h-8 text-white" />
        </div>
        <h3 class="text-lg font-bold text-gray-800 mb-2">AI 正在润色中...</h3>
        <p class="text-sm text-gray-500 text-center">请稍候，正在为您的文字增添魔力 ✨</p>
        <div class="mt-4 flex items-center gap-2">
          <Loader2 class="w-5 h-5 text-[rgb(33,111,85)] animate-spin" />
          <span class="text-sm text-gray-400">处理中</span>
        </div>

        <!-- 取消按钮 -->
        <Button variant="outline" class="mt-4" @click="cancelPolish">
          取消
        </Button>
      </div>

      <!-- 润色完成 -->
      <div v-else-if="polishComplete" class="flex flex-col">
        <DialogHeader class="p-5 pb-4 border-b bg-gradient-to-r from-emerald-50 to-teal-50">
          <DialogTitle class="text-lg font-bold text-gray-800 flex items-center gap-2">
            <div class="w-8 h-8 bg-[rgb(33,111,85)] rounded-lg flex items-center justify-center">
              <Check class="w-5 h-5 text-white" />
            </div>
            润色完成
          </DialogTitle>
          <p class="text-sm text-gray-500 mt-1">今日剩余 {{ aiStore.remaining }} 次</p>
        </DialogHeader>

        <div class="p-5 space-y-4">
          <!-- 润色结果预览 -->
          <div
            class="bg-gradient-to-br from-emerald-50 to-green-50 rounded-xl border border-emerald-100 p-4 max-h-[300px] overflow-y-auto">
            <p class="text-gray-700 text-sm leading-relaxed whitespace-pre-wrap">{{ resultContent }}</p>
          </div>

          <!-- 确认提示 -->
          <div class="bg-amber-50 border border-amber-200 rounded-xl p-3 flex items-start gap-2">
            <Sparkles class="w-4 h-4 text-amber-500 mt-0.5 shrink-0" />
            <p class="text-sm text-amber-700">是否使用这个润色结果替换原内容？</p>
          </div>

          <!-- 操作按钮 -->
          <div class="flex gap-3">
            <Button variant="outline" class="flex-1 h-11 rounded-xl" @click="cancelApply">
              <X class="w-4 h-4 mr-2" />
              取消
            </Button>
            <Button
              class="flex-1 h-11 rounded-xl bg-gradient-to-r from-[rgb(33,111,85)] to-emerald-600 hover:from-[rgb(28,95,72)] hover:to-emerald-700 text-white font-medium"
              @click="applyContent">
              <Check class="w-4 h-4 mr-2" />
              使用此结果
            </Button>
          </div>
        </div>
      </div>
    </DialogContent>
  </Dialog>
</template>
