<template>
  <Transition name="fade-slide">
    <button v-if="isVisible" class="back-to-top-btn" @click="scrollToTop" aria-label="回到顶部">
      <ChevronUp class="w-6 h-6" />
    </button>
  </Transition>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ChevronUp } from 'lucide-vue-next'

const isVisible = ref(false)
const scrollThreshold = 300

const handleScroll = () => {
  isVisible.value = window.scrollY > scrollThreshold
}

const scrollToTop = () => {
  // 问题5修复：使用自定义缓动函数实现更丝滑的滚动动画
  const startPosition = window.pageYOffset
  const duration = 800 // 800ms 动画时长
  let startTime = null

  // easeInOutCubic 缓动函数：开始和结束时慢，中间快
  const easeInOutCubic = (t) => {
    return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2
  }

  const animation = (currentTime) => {
    if (startTime === null) startTime = currentTime
    const timeElapsed = currentTime - startTime
    const progress = Math.min(timeElapsed / duration, 1)
    const ease = easeInOutCubic(progress)
    
    window.scrollTo(0, startPosition * (1 - ease))
    
    if (progress < 1) {
      requestAnimationFrame(animation)
    }
  }

  requestAnimationFrame(animation)
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll, { passive: true })
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
.back-to-top-btn {
  position: fixed;
  right: 32px;
  bottom: 32px;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgb(33, 111, 85) 0%, #10b981 100%);
  color: white;
  border: none;
  box-shadow: 0 4px 12px rgba(33, 111, 85, 0.3);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
  transition: all 0.3s ease;
}

.back-to-top-btn:hover {
  transform: translateY(-4px) scale(1.1);
  box-shadow: 0 8px 20px rgba(33, 111, 85, 0.4);
}

.back-to-top-btn:active {
  transform: translateY(-2px) scale(1.05);
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.3s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(20px);
}

@media (max-width: 768px) {
  .back-to-top-btn {
    right: 20px;
    bottom: 80px;
    width: 44px;
    height: 44px;
  }
}
</style>
