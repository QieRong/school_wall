<script setup>
import { AvatarImage } from 'radix-vue' //必须统一为 radix-vue
import { cn } from '@/lib/utils'
import { computed } from 'vue'
import { getAvatarUrl } from '@/utils/imageUtils'

const props = defineProps({
  src: { type: String, default: '' }, // 允许空字符串
  class: { type: String, default: '' }
})

// 标准化头像 URL，确保在各环境下都能正确访问
// 将完整 URL (http://localhost:19090/files/...) 转换为相对路径 (/files/...)
const normalizedSrc = computed(() => getAvatarUrl(props.src))
</script>

<template>
  <AvatarImage
    :src="normalizedSrc"
    :class="cn('aspect-square h-full w-full object-cover', props.class)"
  />
</template>