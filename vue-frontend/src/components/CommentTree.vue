<script setup>
import { ref, computed } from 'vue'
import { Avatar, AvatarImage, AvatarFallback } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import { Heart, MessageCircle, ChevronDown, ChevronUp } from 'lucide-vue-next'

const props = defineProps({
  comment: {
    type: Object,
    required: true
  },
  level: {
    type: Number,
    default: 0
  },
  maxLevel: {
    type: Number,
    default: 3
  },
  currentUser: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['like', 'reply', 'delete'])

const showReplies = ref(true)

// 格式化时间
const formatTime = (time) => {
  const now = new Date()
  const commentTime = new Date(time)
  const diff = now - commentTime
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return commentTime.toLocaleDateString()
}

// 缩进样式
const indentClass = computed(() => {
  if (props.level === 0) return ''
  if (props.level === 1) return 'ml-8'
  if (props.level === 2) return 'ml-16'
  return 'ml-16'
})

// 是否显示回复按钮
const canReply = computed(() => props.level < props.maxLevel)

// 切换回复显示
const toggleReplies = () => {
  showReplies.value = !showReplies.value
}
</script>

<template>
  <div :class="['py-4', indentClass]">
    <!-- 评论主体 -->
    <div class="flex gap-3">
      <!-- 头像 -->
      <Avatar class="w-8 h-8 flex-shrink-0">
        <AvatarImage :src="comment.avatar" :alt="comment.nickname" />
        <AvatarFallback>{{ comment.nickname?.[0] || 'U' }}</AvatarFallback>
      </Avatar>

      <!-- 评论内容 -->
      <div class="flex-1 min-w-0">
        <!-- 用户名和时间 -->
        <div class="flex items-center gap-2 mb-1">
          <span class="font-medium text-gray-900 text-sm">{{ comment.nickname || '匿名用户' }}</span>
          <span class="text-xs text-gray-500">{{ formatTime(comment.createTime) }}</span>
        </div>

        <!-- 评论文本 -->
        <p class="text-gray-800 text-sm leading-relaxed whitespace-pre-wrap break-words mb-2">
          <span v-if="comment.replyToNickname" class="text-primary-600">
            @{{ comment.replyToNickname }}
          </span>
          {{ comment.content }}
        </p>

        <!-- 操作按钮 -->
        <div class="flex items-center gap-4">
          <!-- 点赞 -->
          <button class="flex items-center gap-1 text-gray-500 hover:text-error-500 transition-colors text-xs"
            :class="{ 'text-error-500': comment.isLiked }" @click="emit('like', comment.id)">
            <Heart class="w-3.5 h-3.5" :class="{ 'fill-current': comment.isLiked }" />
            <span>{{ comment.likeCount || 0 }}</span>
          </button>

          <!-- 回复 -->
          <button v-if="canReply"
            class="flex items-center gap-1 text-gray-500 hover:text-primary-600 transition-colors text-xs"
            @click="emit('reply', comment)">
            <MessageCircle class="w-3.5 h-3.5" />
            <span>回复</span>
          </button>

          <!-- 删除 -->
          <button v-if="currentUser?.id === comment.userId"
            class="text-xs text-gray-500 hover:text-error-500 transition-colors" @click="emit('delete', comment.id)">
            删除
          </button>
        </div>
      </div>
    </div>

    <!-- 子评论 -->
    <div v-if="comment.replies && comment.replies.length > 0" class="mt-2">
      <!-- 折叠/展开按钮 -->
      <button v-if="comment.replies.length > 0"
        class="flex items-center gap-1 text-xs text-primary-600 hover:text-primary-700 transition-colors ml-11 mb-2"
        @click="toggleReplies">
        <component :is="showReplies ? ChevronUp : ChevronDown" class="w-3.5 h-3.5" />
        <span>{{ showReplies ? '收起' : '展开' }} {{ comment.replies.length }} 条回复</span>
      </button>

      <!-- 回复列表 -->
      <div v-show="showReplies" class="space-y-0 border-l-2 border-gray-100 pl-2">
        <CommentTree v-for="reply in comment.replies" :key="reply.id" :comment="reply" :level="level + 1"
          :max-level="maxLevel" :current-user="currentUser" @like="emit('like', $event)" @reply="emit('reply', $event)"
          @delete="emit('delete', $event)" />
      </div>
    </div>
  </div>
</template>
