<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { Button } from '@/components/ui/button'
import { Smile } from 'lucide-vue-next'

const emit = defineEmits(['select'])
const showPicker = ref(false)
const pickerRef = ref(null)
const buttonRef = ref(null)
const showAbove = ref(true) // 默认显示在上方

// 常用表情列表
const emojiCategories = [
  {
    name: '😀',
    emojis: ['😀', '😁', '😂', '🤣', '😃', '😄', '😅', '😆', '😉', '😊', '😋', '😎', '😍', '🥰', '😘', '😗', '🙂', '🤗', '🤩', '🤔', '🤨', '😐', '🙄', '😏', '😥', '😮', '😯', '😪', '😫', '😴', '😌', '😛', '😜', '😝', '😒', '😔', '😕', '🙃', '🤑', '😲']
  },
  {
    name: '❤️',
    emojis: ['❤️', '🧡', '💛', '💚', '💙', '💜', '🖤', '🤍', '💔', '❣️', '💕', '💞', '💓', '💗', '💖', '💘', '💝', '💟', '🫶', '💑', '💏']
  },
  {
    name: '👍',
    emojis: ['👍', '👎', '👊', '✊', '🤛', '🤜', '🤞', '✌️', '🤟', '🤘', '👌', '🤏', '👈', '👉', '👆', '👇', '☝️', '✋', '🖐️', '👋', '🤙', '💪', '🙏']
  },
  {
    name: '🌸',
    emojis: ['🌸', '🌹', '🌺', '🌻', '🌼', '🌷', '🌱', '🌲', '🌳', '🌴', '🌵', '🍀', '🍁', '🍂', '🍃', '🌙', '⭐', '🌟', '✨', '💫', '🔥', '💥', '⚡']
  },
  {
    name: '🎉',
    emojis: ['🎉', '🎊', '🎁', '🎈', '🎀', '🏆', '🥇', '🎯', '🎮', '🎲', '🎵', '🎶', '🎤', '🎧', '📷', '📱', '💻', '⌚', '💡', '📚', '✏️', '💰', '💎']
  }
]

const activeCategory = ref(0)

const selectEmoji = (emoji) => {
  emit('select', emoji)
  showPicker.value = false
}

const togglePicker = (event) => {
  showPicker.value = !showPicker.value

  // 判断应该向上还是向下弹出
  if (showPicker.value && event.target) {
    const rect = event.target.getBoundingClientRect()
    const spaceAbove = rect.top
    const spaceBelow = window.innerHeight - rect.bottom

    // 如果上方空间不足300px，则向下弹出
    showAbove.value = spaceAbove > 300
  }
}

const closePicker = () => {
  showPicker.value = false
}

// 动态计算面板位置类名
const panelClass = computed(() => {
  if (showAbove.value) {
    return 'absolute bottom-full left-0 mb-2'
  } else {
    return 'absolute top-full left-0 mt-2'
  }
})
</script>

<template>
  <div class="relative inline-flex emoji-picker-wrapper shrink-0">
    <!-- 触发按钮 - 和其他工具按钮一致的样式 -->
    <div ref="buttonRef" @click.stop="togglePicker" class="emoji-tool-btn group cursor-pointer">
      <div class="emoji-tool-icon bg-yellow-100 text-yellow-600 group-hover:bg-yellow-500 group-hover:text-white">
        <Smile class="w-4 h-4" />
      </div>
      <span class="emoji-tool-text whitespace-nowrap">表情</span>
    </div>

    <!-- 表情面板 - 智能定位（向上或向下） -->
    <Transition name="fade">
      <div v-if="showPicker" ref="pickerRef"
        :class="[panelClass, 'w-80 bg-white rounded-2xl shadow-2xl border border-gray-100 z-[100000] overflow-hidden']"
        @click.stop>
        <!-- 分类标签 -->
        <div class="flex border-b border-gray-100 p-2 gap-1 bg-gray-50/50">
          <button v-for="(cat, idx) in emojiCategories" :key="idx"
            class="w-9 h-9 flex items-center justify-center text-lg rounded-xl transition-all duration-200"
            :class="activeCategory === idx ? 'bg-yellow-100 scale-110 shadow-sm' : 'hover:bg-gray-100'"
            @click="activeCategory = idx">
            {{ cat.name }}
          </button>
        </div>
        <!-- 表情列表 -->
        <div class="p-3 h-44 overflow-y-auto">
          <div class="grid grid-cols-8 gap-1">
            <button v-for="emoji in emojiCategories[activeCategory].emojis" :key="emoji"
              class="w-8 h-8 flex items-center justify-center text-xl hover:bg-yellow-50 rounded-lg transition-all duration-150 hover:scale-125 active:scale-100"
              @click="selectEmoji(emoji)">
              {{ emoji }}
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- 遮罩层 - 透明，只用于关闭 -->
    <div v-if="showPicker" class="fixed inset-0 z-[99999]" @click="closePicker"></div>
  </div>
</template>

<style scoped>
.emoji-tool-btn {
  @apply flex items-center gap-1.5 px-3 py-1.5 rounded-xl transition-all duration-200 shrink-0 hover:bg-gray-50;
}

.emoji-tool-icon {
  @apply w-8 h-8 rounded-lg flex items-center justify-center transition-all duration-200 shrink-0;
}

.emoji-tool-text {
  @apply text-xs font-medium text-gray-600 whitespace-nowrap;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

/* 确保父容器是相对定位 */
.emoji-picker-wrapper {
  position: relative;
}
</style>
