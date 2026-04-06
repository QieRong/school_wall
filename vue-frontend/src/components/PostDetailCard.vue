<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Heart, MessageCircle, Share2, MapPin, Clock, Eye } from 'lucide-vue-next'
import { Avatar, AvatarImage, AvatarFallback } from '@/components/ui/avatar'
import { Card } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'

const props = defineProps({
  post: {
    type: Object,
    required: true
  },
  currentUser: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['like', 'share', 'viewLocation', 'comment'])

const router = useRouter()
const showLikeAnimation = ref(false)

// 处理图片数组
const images = computed(() => {
  if (!props.post.images) return []
  try {
    return typeof props.post.images === 'string'
      ? JSON.parse(props.post.images)
      : props.post.images
  } catch {
    return props.post.images.split(',').filter(Boolean)
  }
})

// 图片网格布局类
const imageGridClass = computed(() => {
  const count = images.value.length
  if (count === 1) return 'grid-cols-1'
  if (count === 2) return 'grid-cols-2'
  if (count === 3) return 'grid-cols-3'
  if (count === 4) return 'grid-cols-2'
  return 'grid-cols-3'
})

// 格式化时间
const formatTime = (time) => {
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 跳转到用户主页
const goToProfile = () => {
  if (props.post.isAnonymous === 1) return // 匿名帖子禁止点击跳转
  if (props.post.userId) {
    router.push(`/user/${props.post.userId}`)
  }
}

// 点赞动画
const handleLike = () => {
  showLikeAnimation.value = true
  setTimeout(() => {
    showLikeAnimation.value = false
  }, 600)
  emit('like')
}
</script>

<template>
  <Card variant="elevated" padding="none" class="max-w-4xl mx-auto overflow-hidden">
    <!-- 用户信息 -->
    <div class="p-6 border-b border-gray-100">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-4" :class="{ 'cursor-pointer': post.isAnonymous !== 1 }" @click="goToProfile">
          <Avatar class="w-12 h-12 ring-2 ring-gray-100">
            <AvatarImage :src="post.avatar" :alt="post.nickname" />
            <AvatarFallback>{{ post.nickname?.[0] || 'U' }}</AvatarFallback>
          </Avatar>
          <div>
            <div class="font-semibold text-gray-900">{{ post.nickname || '匿名用户' }}</div>
            <div class="flex items-center gap-2 text-sm text-gray-500">
              <Clock class="w-3.5 h-3.5" />
              <span>{{ formatTime(post.createTime) }}</span>
            </div>
          </div>
        </div>

        <!-- 分类标签 -->
        <Badge v-if="post.categoryName" variant="secondary">
          {{ post.categoryName }}
        </Badge>
      </div>
    </div>

    <!-- 帖子内容 -->
    <div class="p-6">
      <p class="text-gray-800 text-lg leading-relaxed whitespace-pre-wrap break-words">
        {{ post.content }}
      </p>
    </div>

    <!-- 图片网格 -->
    <div v-if="images.length > 0" class="px-6 pb-6">
      <div :class="['grid gap-3', imageGridClass]">
        <img v-for="(img, index) in images" :key="index" :src="img" :alt="`图片${index + 1}`"
          class="w-full h-full object-cover rounded-xl aspect-square cursor-pointer hover:opacity-90 transition-opacity"
          loading="lazy" />
      </div>
    </div>

    <!-- 位置信息 -->
    <div v-if="post.location" class="px-6 pb-6">
      <button class="flex items-center gap-2 text-gray-600 hover:text-primary-600 transition-colors"
        @click="emit('viewLocation')">
        <MapPin class="w-4 h-4" />
        <span class="text-sm">{{ post.location }}</span>
      </button>
    </div>

    <!-- AI 使用声明 -->
    <div v-if="post.isAiAssisted" class="px-6 pb-4">
      <span class="inline-flex items-center gap-1.5 text-xs text-purple-500 bg-purple-50 border border-purple-100 rounded-full px-3 py-1.5">
        ⚡ 内容经 AI 协助优化，请注意甄别
      </span>
    </div>

    <!-- 统计信息 -->
    <div class="px-6 py-4 border-t border-gray-100 flex items-center gap-6 text-sm text-gray-500">
      <div class="flex items-center gap-1">
        <Eye class="w-4 h-4" />
        <span>{{ post.viewCount || 0 }} 浏览</span>
      </div>
      <div class="flex items-center gap-1">
        <Heart class="w-4 h-4" />
        <span>{{ post.likeCount || 0 }} 点赞</span>
      </div>
      <div class="flex items-center gap-1">
        <MessageCircle class="w-4 h-4" />
        <span>{{ post.commentCount || 0 }} 评论</span>
      </div>
    </div>

    <!-- 互动按钮 -->
    <div class="px-6 py-4 border-t border-gray-100 flex items-center gap-4">
      <Button variant="outline" size="lg" class="flex-1 relative"
        :class="{ 'text-error-500 border-error-500': post.isLiked }" @click="handleLike">
        <Heart class="w-5 h-5 mr-2 transition-transform" :class="{ 'fill-current scale-110': post.isLiked }" />
        <span>{{ post.isLiked ? '已点赞' : '点赞' }}</span>

        <!-- 点赞动画 -->
        <Transition enter-active-class="transition-all duration-500" enter-from-class="opacity-0 scale-0"
          enter-to-class="opacity-100 scale-150" leave-active-class="transition-all duration-300"
          leave-from-class="opacity-100 scale-150" leave-to-class="opacity-0 scale-0">
          <Heart v-if="showLikeAnimation"
            class="absolute inset-0 m-auto w-12 h-12 text-error-500 fill-current pointer-events-none" />
        </Transition>
      </Button>

      <Button variant="outline" size="lg" class="flex-1" @click="$emit('comment')">
        <MessageCircle class="w-5 h-5 mr-2" />
        <span>评论</span>
      </Button>

      <Button variant="outline" size="lg" class="flex-1" @click="emit('share')">
        <Share2 class="w-5 h-5 mr-2" />
        <span>分享</span>
      </Button>
    </div>
  </Card>
</template>
