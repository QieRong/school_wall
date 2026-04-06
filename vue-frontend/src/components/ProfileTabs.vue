<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { FileText, Heart, Users } from 'lucide-vue-next'

const props = defineProps({
  activeTab: {
    type: String,
    default: 'posts'
  }
})

const emit = defineEmits(['update:activeTab'])

const isSticky = ref(false)

const tabs = [
  { id: 'posts', name: '帖子', icon: FileText },
  { id: 'likes', name: '点赞', icon: Heart },
  { id: 'following', name: '关注', icon: Users }
]

// 监听滚动实现吸附效果
const handleScroll = () => {
  const scrollTop = window.pageYOffset || document.documentElement.scrollTop
  isSticky.value = scrollTop > 400
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<template>
  <div class="bg-white border-b border-gray-200 transition-all duration-normal"
    :class="{ 'sticky top-16 z-sticky shadow-md': isSticky }">
    <div class="max-w-4xl mx-auto px-4">
      <nav class="flex gap-1">
        <button v-for="tab in tabs" :key="tab.id"
          class="flex items-center gap-2 px-6 py-4 text-sm font-medium transition-all duration-normal relative" :class="activeTab === tab.id
            ? 'text-primary-600'
            : 'text-gray-600 hover:text-gray-900'" @click="emit('update:activeTab', tab.id)">
          <component :is="tab.icon" class="w-4 h-4" />
          <span>{{ tab.name }}</span>

          <!-- 激活指示器 -->
          <div v-if="activeTab === tab.id" class="absolute bottom-0 left-0 right-0 h-0.5 bg-primary-600" />
        </button>
      </nav>
    </div>
  </div>
</template>
