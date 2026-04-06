<script setup>
import { ref, computed, watch } from 'vue'
import { Avatar, AvatarImage, AvatarFallback } from '@/components/ui/avatar'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Heart, Flame, User, Clock, ImageIcon } from 'lucide-vue-next'

const props = defineProps({
  paragraph: {
    type: Object,
    required: true
  },
  showAnimation: {
    type: Boolean,
    default: false
  },
  isLikeLoading: {
    type: Boolean,
    default: false
  },
  cooldownRemaining: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['like'])

// 逐字浮现动画状态
const displayedContent = ref('')
const isAnimating = ref(false)

// 格式化时间
const formattedTime = computed(() => {
  if (!props.paragraph.createTime) return ''
  const date = new Date(props.paragraph.createTime)
  const now = new Date()
  const diff = now - date

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`

  return `${date.getMonth() + 1}月${date.getDate()}日`
})

// 显示名称（优先笔名）
const displayName = computed(() => {
  return props.paragraph.penName || props.paragraph.authorNickname || '匿名旅人'
})

// 逐字浮现动画
const animateContent = () => {
  if (!props.showAnimation || !props.paragraph.content) {
    displayedContent.value = props.paragraph.content
    return
  }

  isAnimating.value = true
  displayedContent.value = ''
  const content = props.paragraph.content
  let index = 0

  const timer = setInterval(() => {
    if (index < content.length) {
      displayedContent.value += content[index]
      index++
    } else {
      clearInterval(timer)
      isAnimating.value = false
    }
  }, 30) // 每30ms显示一个字
}

// 点赞处理
const handleLike = () => {
  emit('like', props.paragraph.id)
}

// 监听段落变化，触发动画
watch(() => props.paragraph.id, () => {
  if (props.showAnimation) {
    animateContent()
  } else {
    displayedContent.value = props.paragraph.content
  }
}, { immediate: true })
</script>

<template>
  <div class="paragraph-card">
    <!-- 顶部：作者信息和徽章 -->
    <div class="flex items-center justify-between mb-4">
      <div class="flex items-center gap-3">
        <Avatar class="w-10 h-10 border-2 border-emerald-300 shadow-sm">
          <AvatarImage :src="paragraph.authorAvatar" />
          <AvatarFallback class="bg-emerald-100">
            <User class="w-5 h-5 text-emerald-600" />
          </AvatarFallback>
        </Avatar>
        <div>
          <div class="font-medium text-emerald-900 font-serif">
            {{ displayName }}
          </div>
          <div class="text-xs text-emerald-500 flex items-center gap-1">
            <Clock class="w-3 h-3" />
            {{ formattedTime }}
          </div>
        </div>
      </div>

      <!-- 徽章 -->
      <div class="flex items-center gap-2">
        <Badge v-if="paragraph.isKeyPoint" class="key-point-badge">
          <Flame class="w-3 h-3 mr-1" /> 关键转折
        </Badge>
        <Badge v-else-if="paragraph.isHot" class="hot-badge">
          <Heart class="w-3 h-3 mr-1" /> 热门
        </Badge>
      </div>
    </div>

    <!-- 段落内容 -->
    <div class="paragraph-content font-serif text-lg leading-relaxed text-emerald-900">
      <span v-if="showAnimation">{{ displayedContent }}</span>
      <span v-else>{{ paragraph.content }}</span>
      <span v-if="isAnimating" class="cursor-blink">|</span>
    </div>

    <!-- 插画展示 -->
    <div v-if="paragraph.imageUrl" class="mt-4 image-container">
      <img :src="paragraph.imageUrl" :alt="'段落插画'" class="paragraph-image" loading="lazy" />
      <div class="image-overlay">
        <ImageIcon class="w-4 h-4" /> 插画
      </div>
    </div>

    <!-- AI 使用声明 -->
    <div v-if="paragraph.isAiAssisted"
      class="mt-3 flex items-center gap-1.5 text-[11px] text-purple-500 bg-purple-50 border border-purple-100 rounded-lg px-3 py-1.5 w-fit">
      <span>⚡</span>
      <span>内容经 AI 协助优化，请注意甄别</span>
    </div>

    <!-- 底部：点赞和序号 -->
    <div class="flex items-center justify-between mt-6 pt-4 border-t border-emerald-200/50">
      <Button variant="ghost" class="like-button"
        :class="{ 'liked': paragraph.isLikedByMe, 'disabled': isLikeLoading || cooldownRemaining > 0 }"
        :disabled="isLikeLoading || cooldownRemaining > 0" @click="handleLike">
        <Heart class="w-5 h-5 mr-1 transition-all"
          :class="paragraph.isLikedByMe ? 'fill-pink-500 text-pink-500 scale-110' : ''" />
        <span class="like-count">
          {{ cooldownRemaining > 0 ? `${cooldownRemaining}s` : (paragraph.likeCount || 0) }}
        </span>
      </Button>

      <div class="text-sm text-emerald-500 font-serif">
        第 {{ paragraph.sequence }} 段
      </div>
    </div>
  </div>
</template>

<style scoped>
.paragraph-card {
  background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
  border: 2px solid rgb(33, 111, 85);
  border-radius: 12px;
  padding: 24px;
  box-shadow:
    0 4px 20px rgba(33, 111, 85, 0.15),
    inset 0 1px 0 rgba(255, 255, 255, 0.5);
  position: relative;
  overflow: hidden;
}

.paragraph-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.65' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%' height='100%' filter='url(%23noise)' opacity='0.05'/%3E%3C/svg%3E");
  pointer-events: none;
  opacity: 0.3;
}

.paragraph-content {
  white-space: pre-wrap;
  line-height: 2;
  position: relative;
  z-index: 1;
  min-height: 100px;
  color: #065f46;
  font-weight: 500;
}

.cursor-blink {
  animation: blink 0.8s infinite;
  color: rgb(33, 111, 85);
}

@keyframes blink {

  0%,
  50% {
    opacity: 1;
  }

  51%,
  100% {
    opacity: 0;
  }
}

.key-point-badge {
  background: linear-gradient(135deg, #a7f3d0 0%, #6ee7b7 100%);
  color: #065f46;
  border: 1px solid #34d399;
  animation: glow 2s ease-in-out infinite;
}

.hot-badge {
  background: linear-gradient(135deg, #fce7f3 0%, #fbcfe8 100%);
  color: #be185d;
  border: 1px solid #f472b6;
}

@keyframes glow {

  0%,
  100% {
    box-shadow: 0 0 5px rgba(52, 211, 153, 0.5);
  }

  50% {
    box-shadow: 0 0 15px rgba(52, 211, 153, 0.8);
  }
}

.image-container {
  position: relative;
  border-radius: 8px;
  overflow: hidden;
}

.paragraph-image {
  width: 100%;
  max-height: 300px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid rgb(33, 111, 85);
}

.image-overlay {
  position: absolute;
  bottom: 8px;
  right: 8px;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.like-button {
  color: #065f46;
  transition: all 0.2s;
}

.like-button:hover {
  color: #ec4899;
  background: rgba(236, 72, 153, 0.1);
}

.like-button.liked {
  color: #ec4899;
}

.like-button.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.like-count {
  font-weight: 600;
  min-width: 20px;
}

.like-button:active .like-count {
  animation: pop 0.3s ease;
}

@keyframes pop {
  0% {
    transform: scale(1);
  }

  50% {
    transform: scale(1.3);
  }

  100% {
    transform: scale(1);
  }
}
</style>
