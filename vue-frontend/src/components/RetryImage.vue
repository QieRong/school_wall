<script setup>
/**
 * 图片加载失败重试组件
 * 当图片加载失败时自动重试，达到最大重试次数后显示 fallback 图片
 */
import { ref, watch, computed, onMounted } from 'vue'
import { RefreshCw } from 'lucide-vue-next'
import { getFallbackImage, handleImageError, ImageType } from '@/utils/imageUtils'

const props = defineProps({
  src: {
    type: String,
    required: true
  },
  alt: {
    type: String,
    default: ''
  },
  type: {
    type: String,
    default: ImageType.POST,
    validator: (value) => {
      return [ImageType.AVATAR, ImageType.POST].includes(value)
    }
  },
  fallback: {
    type: String,
    default: ''
  },
  maxRetries: {
    type: Number,
    default: 3,
    validator: (value) => {
      return value >= 0 && value <= 10
    }
  },
  class: {
    type: String,
    default: ''
  }
})

// 根据 type 计算 fallback 路径
const fallbackImage = computed(() => {
  // 如果提供了 fallback，使用 fallback（向后兼容）
  if (props.fallback) {
    return props.fallback
  }
  // 否则根据 type 自动选择
  return getFallbackImage(props.type)
})

const imgSrc = ref(props.src)
const hasError = ref(false)
const retryCount = ref(0)

// 检查 src 是否为空
onMounted(() => {
  if (!props.src || props.src.trim() === '') {
    hasError.value = true
    imgSrc.value = fallbackImage.value
  }
})

// 监听src变化
watch(() => props.src, (newSrc) => {
  if (!newSrc || newSrc.trim() === '') {
    hasError.value = true
    imgSrc.value = fallbackImage.value
    retryCount.value = 0
    return
  }

  imgSrc.value = newSrc
  hasError.value = false
  retryCount.value = 0
})

const handleError = () => {
  // 确保重试次数不超过 maxRetries
  if (retryCount.value < props.maxRetries) {
    // 自动重试
    retryCount.value++
    setTimeout(() => {
      imgSrc.value = props.src + '?retry=' + Date.now()
    }, 500)
  } else {
    // 达到最大重试次数，使用统一的错误处理函数
    hasError.value = true
    const tempImg = { src: props.src }
    handleImageError({ target: tempImg }, props.type)
    imgSrc.value = tempImg.src
  }
}

const handleRetry = () => {
  hasError.value = false
  retryCount.value = 0
  imgSrc.value = props.src + '?retry=' + Date.now()
}
</script>

<template>
  <div class="relative" :class="props.class">
    <img :src="imgSrc" :alt="alt" :class="props.class" @error="handleError" />
    <!-- 重试遮罩 -->
    <div v-if="hasError"
      class="absolute inset-0 bg-gray-100 flex flex-col items-center justify-center cursor-pointer hover:bg-gray-200 transition-colors"
      @click="handleRetry">
      <RefreshCw class="w-6 h-6 text-gray-400 mb-1" />
      <span class="text-xs text-gray-500">点击重试</span>
    </div>
  </div>
</template>
