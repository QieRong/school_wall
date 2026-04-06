<script setup>
import { useAppStore } from '@/stores/app'
const appStore = useAppStore()
</script>

<template>
  <div v-if="appStore.isLoading" class="fixed inset-0 z-[9999] flex items-center justify-center bg-[rgb(33,111,85)]"
    v-motion :initial="{ opacity: 0 }" :enter="{ opacity: 1 }" :leave="{ opacity: 0, transition: { duration: 500 } }">
    <div class="flex flex-col md:flex-row items-center gap-8 p-10">

      <div class="relative w-32 h-32 flex items-center justify-center rounded-full overflow-hidden" v-motion
        :initial="{ scale: 0, rotate: -180 }" :enter="{
          scale: 1,
          rotate: 0,
          transition: {
            type: 'spring',
            stiffness: 200,
            damping: 15,
            mass: 1
          }
        }">
        <img src="/Round_school_logo.png" alt="Logo" class="w-full h-full object-cover" />
      </div>

      <div class="overflow-hidden">
        <h1 class="text-5xl md:text-6xl text-white tracking-widest font-bold drop-shadow-lg font-serif" v-motion
          :initial="{ opacity: 0, y: 50 }" :enter="{
            opacity: 1,
            y: 0,
            transition: {
              delay: 100,   // 更快开始 (0.1s)
              duration: 600, // 更快浮现 (0.6s)
              ease: 'easeOut'
            }
          }">
          张家界学院
        </h1>
      </div>
    </div>
  </div>
</template>