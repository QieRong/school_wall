<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Heart, MessageCircle, Share2, MoreHorizontal, MapPin, Play, ChevronLeft, ChevronRight, X } from 'lucide-vue-next'
import { Avatar, AvatarImage, AvatarFallback } from '@/components/ui/avatar'
import { Card } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger
} from '@/components/ui/dropdown-menu'
import { Dialog, DialogContent } from '@/components/ui/dialog'

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

const emit = defineEmits(['like', 'comment', 'share', 'report', 'delete', 'viewLocation'])

const router = useRouter()

// 媒体预览状态
const showMediaPreview = ref(false)
const currentMediaIndex = ref(0)
const currentMediaType = ref('image') // 'image' or 'video'

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

// 处理视频
const video = computed(() => {
  return props.post.video || ''
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
  const now = new Date()
  const postTime = new Date(time)
  const diff = now - postTime
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return postTime.toLocaleDateString()
}

// 跳转到帖子详情
const goToDetail = () => {
  router.push(`/post/${props.post.id}`)
}

// 跳转到用户主页
const goToProfile = (e) => {
  e.stopPropagation()
  if (props.post.isAnonymous === 1) return // 匿名帖子禁止点击跳转
  if (props.post.userId) {
    router.push(`/user/${props.post.userId}`)
  }
}

// 点赞
const handleLike = (e) => {
  e.stopPropagation()
  emit('like', props.post.id)
}

// 评论
const handleComment = (e) => {
  e.stopPropagation()
  emit('comment', props.post.id)
}

// 分享
const handleShare = (e) => {
  e.stopPropagation()
  emit('share', props.post.id)
}

// 查看位置
const handleViewLocation = (e) => {
  e.stopPropagation()
  emit('viewLocation', props.post)
}

// 打开媒体预览
const openMediaPreview = (index, type, e) => {
  e.stopPropagation()
  currentMediaIndex.value = index
  currentMediaType.value = type
  showMediaPreview.value = true
}

// 上一个媒体
const prevMedia = () => {
  if (currentMediaType.value === 'image' && images.value.length > 0) {
    currentMediaIndex.value = (currentMediaIndex.value - 1 + images.value.length) % images.value.length
  }
}

// 下一个媒体
const nextMedia = () => {
  if (currentMediaType.value === 'image' && images.value.length > 0) {
    currentMediaIndex.value = (currentMediaIndex.value + 1) % images.value.length
  }
}

// 当前预览的媒体URL
const currentMediaUrl = computed(() => {
  if (currentMediaType.value === 'image') {
    return images.value[currentMediaIndex.value] || '/图片加载失败.png'
  } else {
    return video.value || ''
  }
})

// 图片加载失败处理
const handleImageError = (e) => {
  e.target.src = '/图片加载失败.png'
}

// 视频加载失败处理
const handleVideoError = (e) => {
  e.target.poster = '/图片加载失败.png'
}
</script>

<template>
  <Card variant="default" padding="none" hoverable class="overflow-hidden cursor-pointer" @click="goToDetail">
    <!-- 用户信息 -->
    <div class="p-4 flex items-center justify-between">
      <div class="flex items-center gap-3" :class="{ 'cursor-pointer': post.isAnonymous !== 1 }" @click="goToProfile">
        <Avatar class="w-10 h-10 ring-2 ring-gray-100">
          <AvatarImage :src="post.avatar" :alt="post.nickname" />
          <AvatarFallback>{{ post.nickname?.[0] || 'U' }}</AvatarFallback>
        </Avatar>
        <div>
          <div class="font-medium text-gray-900">{{ post.nickname || '匿名用户' }}</div>
          <div class="text-sm text-gray-500">{{ formatTime(post.createTime) }}</div>
        </div>
      </div>

      <!-- 更多操作 -->
      <DropdownMenu>
        <DropdownMenuTrigger as-child>
          <Button variant="ghost" size="sm" class="h-8 w-8 p-0" @click.stop>
            <MoreHorizontal class="w-4 h-4" />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" class="bg-white/95 backdrop-blur-sm shadow-lg border border-gray-200">
          <DropdownMenuItem v-if="currentUser?.id === post.userId" @click.stop="emit('delete', post.id)">
            删除帖子
          </DropdownMenuItem>
          <DropdownMenuItem v-else @click.stop="emit('report', post.id)">
            举报帖子
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </div>

    <!-- 帖子内容 -->
    <div class="px-4 pb-3">
      <!-- 分类标签 -->
      <Badge v-if="post.categoryName" variant="secondary" class="mb-2">
        {{ post.categoryName }}
      </Badge>

      <!-- 文本内容 -->
      <p class="text-gray-800 leading-relaxed whitespace-pre-wrap break-words">
        {{ post.content }}
      </p>
    </div>

    <!-- 图片网格 -->
    <div v-if="images.length > 0" class="px-4 pb-3">
      <div :class="['grid gap-2', imageGridClass]">
        <div v-for="(img, index) in images.slice(0, 9)" :key="index"
          class="relative w-full h-full aspect-square rounded-lg overflow-hidden group cursor-pointer"
          @click="openMediaPreview(index, 'image', $event)">
          <img :src="img" :alt="`图片${index + 1}`"
            class="w-full h-full object-cover transition-transform duration-300 group-hover:scale-110" loading="lazy"
            @error="handleImageError" />
          <!-- 悬停遮罩 -->
          <div
            class="absolute inset-0 bg-black/0 group-hover:bg-black/20 transition-all duration-300 flex items-center justify-center">
            <div
              class="w-12 h-12 rounded-full bg-white/0 group-hover:bg-white/90 flex items-center justify-center transition-all duration-300 scale-0 group-hover:scale-100">
              <svg class="w-6 h-6 text-gray-700" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
              </svg>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 视频 -->
    <div v-if="video" class="px-4 pb-3">
      <div class="relative rounded-lg overflow-hidden group cursor-pointer aspect-video bg-black"
        @click="openMediaPreview(0, 'video', $event)">
        <video :src="video" class="w-full h-full object-contain" preload="metadata" @error="handleVideoError"></video>
        <!-- 播放按钮遮罩 -->
        <div
          class="absolute inset-0 bg-black/30 group-hover:bg-black/40 transition-all duration-300 flex items-center justify-center">
          <div
            class="w-16 h-16 rounded-full bg-white/90 group-hover:bg-white flex items-center justify-center transition-all duration-300 group-hover:scale-110 shadow-lg">
            <Play class="w-8 h-8 text-gray-800 ml-1" fill="currentColor" />
          </div>
        </div>
        <!-- 视频标签 -->
        <div class="absolute top-3 left-3 px-2 py-1 bg-black/60 text-white text-xs rounded-lg backdrop-blur-sm">
          视频
        </div>
      </div>
    </div>

    <!-- 位置信息 -->
    <div v-if="post.location" class="px-4 pb-3">
      <button class="flex items-center gap-1 text-sm text-gray-500 hover:text-primary-600 transition-colors"
        @click="handleViewLocation">
        <MapPin class="w-4 h-4" />
        <span>{{ post.location }}</span>
      </button>
    </div>

    <!-- AI 使用声明 -->
    <div v-if="post.isAiAssisted" class="px-4 pb-2">
      <span class="inline-flex items-center gap-1 text-[11px] text-purple-500 bg-purple-50 border border-purple-100 rounded-full px-2.5 py-1">
        ⚡ 内容经 AI 协助优化，请注意甄别
      </span>
    </div>

    <!-- 互动按钮 -->
    <div class="px-4 py-3 border-t border-gray-100 flex items-center gap-6">
      <button class="flex items-center gap-2 text-gray-600 hover:text-error-500 transition-colors group"
        :class="{ 'text-error-500': post.isLiked }" @click="handleLike">
        <Heart class="w-5 h-5 transition-transform group-hover:scale-110" :class="{ 'fill-current': post.isLiked }" />
        <span class="text-sm font-medium">{{ post.likeCount || 0 }}</span>
      </button>

      <button class="flex items-center gap-2 text-gray-600 hover:text-primary-600 transition-colors group"
        @click="handleComment">
        <MessageCircle class="w-5 h-5 transition-transform group-hover:scale-110" />
        <span class="text-sm font-medium">{{ post.commentCount || 0 }}</span>
      </button>

      <button class="flex items-center gap-2 text-gray-600 hover:text-primary-600 transition-colors group"
        @click="handleShare">
        <Share2 class="w-5 h-5 transition-transform group-hover:scale-110" />
        <span class="text-sm font-medium">分享</span>
      </button>
    </div>

    <!-- 媒体预览对话框 -->
    <Dialog :open="showMediaPreview" @update:open="showMediaPreview = $event">
      <DialogContent
        class="sm:max-w-[95vw] md:max-w-[90vw] lg:max-w-[1200px] h-[90vh] p-0 bg-black/95 backdrop-blur-xl border-none rounded-2xl overflow-hidden"
        :show-close="false">
        <div class="relative w-full h-full flex items-center justify-center">
          <!-- 关闭按钮 -->
          <button @click="showMediaPreview = false"
            class="absolute top-4 right-4 z-50 w-10 h-10 rounded-full bg-white/10 hover:bg-white/20 backdrop-blur-sm text-white flex items-center justify-center transition-all duration-200 hover:scale-110">
            <X class="w-6 h-6" />
          </button>

          <!-- 图片预览 -->
          <div v-if="currentMediaType === 'image'" class="relative w-full h-full flex items-center justify-center p-4">
            <img :src="currentMediaUrl" alt="预览图片" class="max-w-full max-h-full object-contain rounded-lg shadow-2xl"
              @error="handleImageError" />

            <!-- 左右导航按钮 (仅多图时显示) -->
            <template v-if="images.length > 1">
              <button @click="prevMedia"
                class="absolute left-4 top-1/2 -translate-y-1/2 w-12 h-12 rounded-full bg-white/10 hover:bg-white/20 backdrop-blur-sm text-white flex items-center justify-center transition-all duration-200 hover:scale-110">
                <ChevronLeft class="w-6 h-6" />
              </button>
              <button @click="nextMedia"
                class="absolute right-4 top-1/2 -translate-y-1/2 w-12 h-12 rounded-full bg-white/10 hover:bg-white/20 backdrop-blur-sm text-white flex items-center justify-center transition-all duration-200 hover:scale-110">
                <ChevronRight class="w-6 h-6" />
              </button>

              <!-- 图片计数 -->
              <div
                class="absolute bottom-4 left-1/2 -translate-x-1/2 px-4 py-2 rounded-full bg-black/50 backdrop-blur-sm text-white text-sm">
                {{ currentMediaIndex + 1 }} / {{ images.length }}
              </div>
            </template>
          </div>

          <!-- 视频预览 -->
          <div v-else-if="currentMediaType === 'video'"
            class="relative w-full h-full flex items-center justify-center p-4">
            <video :src="currentMediaUrl" controls autoplay class="max-w-full max-h-full rounded-lg shadow-2xl"
              @error="handleVideoError">
              您的浏览器不支持视频播放
            </video>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  </Card>
</template>
