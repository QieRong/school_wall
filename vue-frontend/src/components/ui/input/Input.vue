<script setup>
import { computed } from 'vue'
import { useVModel } from '@vueuse/core'
import { cn } from '@/lib/utils'

const props = defineProps({
  modelValue: {
    type: [String, Number],
    default: ''
  },
  defaultValue: {
    type: [String, Number],
    default: ''
  },
  type: {
    type: String,
    default: 'text'
  },
  placeholder: {
    type: String,
    default: ''
  },
  disabled: {
    type: Boolean,
    default: false
  },
  readonly: {
    type: Boolean,
    default: false
  },
  state: {
    type: String,
    default: 'default',
    validator: (v) => ['default', 'error', 'success'].includes(v)
  },
  errorMessage: {
    type: String,
    default: ''
  },
  successMessage: {
    type: String,
    default: ''
  },
  class: {
    type: [String, Object, Array],
    default: ''
  }
})

const emits = defineEmits(['update:modelValue', 'focus', 'blur', 'input', 'change'])

const modelValue = useVModel(props, 'modelValue', emits, {
  passive: true,
  defaultValue: props.defaultValue
})

const baseClasses = 'flex h-10 w-full rounded-lg border bg-white px-3 py-2 text-base transition-all duration-normal placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-offset-1 disabled:cursor-not-allowed disabled:opacity-50 disabled:bg-gray-50'

const stateClasses = computed(() => {
  const states = {
    default: 'border-gray-200 focus:border-primary-500 focus:ring-primary-500',
    error: 'border-error-500 focus:border-error-500 focus:ring-error-500',
    success: 'border-success-500 focus:border-success-500 focus:ring-success-500'
  }
  return states[props.state]
})

const hasPrefix = computed(() => !!slots.prefix)
const hasSuffix = computed(() => !!slots.suffix)

const slots = defineSlots()

const handleFocus = (e) => emits('focus', e)
const handleBlur = (e) => emits('blur', e)
const handleInput = (e) => emits('input', e)
const handleChange = (e) => emits('change', e)
</script>

<template>
  <div class="w-full">
    <div class="relative">
      <!-- 前缀插槽 -->
      <div v-if="hasPrefix" class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500">
        <slot name="prefix" />
      </div>

      <!-- 输入框 -->
      <input
        v-model="modelValue"
        :type="type"
        :placeholder="placeholder"
        :disabled="disabled"
        :readonly="readonly"
        :class="cn(
          baseClasses,
          stateClasses,
          {
            'pl-10': hasPrefix,
            'pr-10': hasSuffix
          },
          props.class
        )"
        @focus="handleFocus"
        @blur="handleBlur"
        @input="handleInput"
        @change="handleChange"
      />

      <!-- 后缀插槽 -->
      <div v-if="hasSuffix" class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500">
        <slot name="suffix" />
      </div>
    </div>

    <!-- 错误消息 -->
    <p v-if="state === 'error' && errorMessage" class="mt-1 text-sm text-error-500">
      {{ errorMessage }}
    </p>

    <!-- 成功消息 -->
    <p v-if="state === 'success' && successMessage" class="mt-1 text-sm text-success-500">
      {{ successMessage }}
    </p>
  </div>
</template>
