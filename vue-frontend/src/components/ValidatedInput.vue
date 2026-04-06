<script setup>
import { ref, computed, watch } from 'vue'
import { Input } from '@/components/ui/input'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  validator: {
    type: Function,
    default: null
  },
  placeholder: {
    type: String,
    default: ''
  },
  type: {
    type: String,
    default: 'text'
  },
  disabled: {
    type: Boolean,
    default: false
  },
  validateOnBlur: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['update:modelValue', 'validate'])

const errorMessage = ref('')
const isShaking = ref(false)
const hasError = computed(() => !!errorMessage.value)

// 验证函数
const validate = () => {
  if (!props.validator) {
    errorMessage.value = ''
    emit('validate', { valid: true })
    return { valid: true }
  }
  
  const result = props.validator(props.modelValue)
  errorMessage.value = result.valid ? '' : result.message
  
  // 如果验证失败，触发震动动画
  if (!result.valid) {
    triggerShake()
  }
  
  emit('validate', result)
  return result
}

// 触发震动动画
const triggerShake = () => {
  isShaking.value = true
  setTimeout(() => {
    isShaking.value = false
  }, 500)
}

// 输入时清除错误
const handleInput = (event) => {
  emit('update:modelValue', event.target.value)
  if (errorMessage.value) {
    errorMessage.value = ''
  }
}

// 失焦时验证
const handleBlur = () => {
  if (props.validateOnBlur) {
    validate()
  }
}

// 暴露验证方法给父组件
defineExpose({ validate })
</script>

<template>
  <div class="w-full">
    <div class="relative">
      <Input
        :value="modelValue"
        :type="type"
        :placeholder="placeholder"
        :disabled="disabled"
        :class="[
          'transition-all duration-200',
          {
            'border-red-500 focus:border-red-500 focus:ring-red-500': hasError,
            'animate-shake': isShaking
          }
        ]"
        @input="handleInput"
        @blur="handleBlur"
      />
    </div>
    
    <!-- 错误提示 -->
    <Transition name="error-slide">
      <div v-if="hasError" class="mt-1.5 text-sm text-red-500 flex items-center gap-1">
        <span class="text-xs">⚠️</span>
        <span>{{ errorMessage }}</span>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
/* 震动动画 */
@keyframes shake {
  0%, 100% { transform: translateX(0); }
  10%, 30%, 50%, 70%, 90% { transform: translateX(-4px); }
  20%, 40%, 60%, 80% { transform: translateX(4px); }
}

.animate-shake {
  animation: shake 0.5s cubic-bezier(.36,.07,.19,.97) both;
}

/* 错误提示滑入动画 */
.error-slide-enter-active,
.error-slide-leave-active {
  transition: all 0.3s ease;
}

.error-slide-enter-from {
  opacity: 0;
  transform: translateY(-8px);
}

.error-slide-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
