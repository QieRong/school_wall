<script setup>
import { computed, watch, onMounted, onUnmounted } from 'vue'
import { cn } from '@/lib/utils'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: ''
  },
  size: {
    type: String,
    default: 'md',
    validator: (v) => ['sm', 'md', 'lg', 'xl', 'full'].includes(v)
  },
  closeOnEsc: {
    type: Boolean,
    default: true
  },
  closeOnClickOutside: {
    type: Boolean,
    default: true
  },
  showClose: {
    type: Boolean,
    default: true
  },
  class: {
    type: [String, Object, Array],
    default: ''
  }
})

const emits = defineEmits(['update:modelValue', 'close', 'open'])

const sizeClasses = computed(() => {
  const sizes = {
    sm: 'max-w-sm',
    md: 'max-w-md',
    lg: 'max-w-lg',
    xl: 'max-w-xl',
    full: 'max-w-full mx-4'
  }
  return sizes[props.size]
})

const close = () => {
  emits('update:modelValue', false)
  emits('close')
}

const handleEscape = (e) => {
  if (props.closeOnEsc && e.key === 'Escape' && props.modelValue) {
    close()
  }
}

const handleBackdropClick = () => {
  if (props.closeOnClickOutside) {
    close()
  }
}

watch(() => props.modelValue, (newVal) => {
  if (newVal) {
    emits('open')
    document.body.style.overflow = 'hidden'
  } else {
    document.body.style.overflow = ''
  }
})

onMounted(() => {
  document.addEventListener('keydown', handleEscape)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleEscape)
  document.body.style.overflow = ''
})
</script>

<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition-opacity duration-normal ease-out"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition-opacity duration-normal ease-in"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div
        v-if="modelValue"
        class="fixed inset-0 bg-black/50 z-modal-backdrop"
        @click="handleBackdropClick"
      />
    </Transition>

    <Transition
      enter-active-class="transition-all duration-normal ease-out"
      enter-from-class="opacity-0 scale-95"
      enter-to-class="opacity-100 scale-100"
      leave-active-class="transition-all duration-normal ease-in"
      leave-from-class="opacity-100 scale-100"
      leave-to-class="opacity-0 scale-95"
    >
      <div
        v-if="modelValue"
        class="fixed inset-0 flex items-center justify-center z-modal p-4"
        @click.self="handleBackdropClick"
      >
        <div
          :class="cn(
            'relative w-full bg-white rounded-xl shadow-2xl',
            sizeClasses,
            props.class
          )"
          @click.stop
        >
          <!-- Header -->
          <div v-if="title || showClose" class="flex items-center justify-between p-6 border-b border-gray-200">
            <h2 v-if="title" class="text-xl font-semibold text-gray-900">
              {{ title }}
            </h2>
            <button
              v-if="showClose"
              type="button"
              class="text-gray-400 hover:text-gray-600 transition-colors"
              @click="close"
            >
              <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          <!-- Content -->
          <div class="p-6">
            <slot />
          </div>

          <!-- Footer -->
          <div v-if="$slots.footer" class="p-6 border-t border-gray-200">
            <slot name="footer" />
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>
