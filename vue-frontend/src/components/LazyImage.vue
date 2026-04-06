<script setup>
import { ref, onMounted, computed } from 'vue'
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
  placeholder: {
    type: String,
    default: ''
  },
  class: {
    type: String,
    default: ''
  }
})

// 根据 type 计算 fallback 路径
const fallbackImage = computed(() => {
  // 如果提供了 placeholder，使用 placeholder（向后兼容）
  if (props.placeholder) {
    return props.placeholder
  }
  // 否则根据 type 自动选择
  return getFallbackImage(props.type)
})

const imgRef = ref(null)
const isLoaded = ref(false)
const hasError = ref(false)
const currentSrc = ref(fallbackImage.value)

onMounted(() => {
  // 如果 src 为空或只有空格，直接显示 fallback
  if (!props.src || props.src.trim() === '') {
    hasError.value = true
    currentSrc.value = fallbackImage.value
    return
  }

  // 使用 Intersection Observer 实现懒加载
  const observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          // 图片进入视口，开始加载
          const img = new Image()
          img.src = props.src

          img.onload = () => {
            currentSrc.value = props.src
            isLoaded.value = true
          }

          img.onerror = (event) => {
            hasError.value = true
            // 使用统一的错误处理函数，确保防止二次错误
            const tempImg = { src: props.src }
            handleImageError({ target: tempImg }, props.type)
            currentSrc.value = tempImg.src
          }

          // 停止观察
          observer.unobserve(imgRef.value)
        }
      })
    },
    {
      rootMargin: '50px' // 提前50px开始加载
    }
  )

  if (imgRef.value) {
    observer.observe(imgRef.value)
  }
})
</script>

<template>
  <img ref="imgRef" :src="currentSrc" :alt="alt" :class="[
    props.class,
    { 'opacity-50': !isLoaded && !hasError },
    { 'opacity-100 transition-opacity duration-300': isLoaded }
  ]" loading="lazy" />
</template>
