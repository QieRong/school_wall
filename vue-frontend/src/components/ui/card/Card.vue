<script setup>
import { computed } from 'vue'
import { cn } from '@/lib/utils'

// 禁用自动 attrs 继承，改由模板中 v-bind="$attrs" 手动绑定，确保 @click 等事件正确透传
defineOptions({ inheritAttrs: false })
const props = defineProps({
  variant: {
    type: String,
    default: 'default',
    validator: (v) => ['default', 'bordered', 'elevated', 'flat'].includes(v)
  },
  padding: {
    type: String,
    default: 'md',
    validator: (v) => ['none', 'sm', 'md', 'lg'].includes(v)
  },
  hoverable: {
    type: Boolean,
    default: false
  },
  class: {
    type: [String, Object, Array],
    default: ''
  }
})

const baseClasses = 'rounded-lg bg-card text-card-foreground transition-all duration-normal'

const variantClasses = computed(() => {
  const variants = {
    default: 'border border-gray-200 shadow-sm',
    bordered: 'border-2 border-gray-300',
    elevated: 'shadow-lg',
    flat: 'bg-gray-50'
  }
  return variants[props.variant]
})

const paddingClasses = computed(() => {
  const paddings = {
    none: '',
    sm: 'p-3',
    md: 'p-4',
    lg: 'p-6'
  }
  return paddings[props.padding]
})

const hoverClasses = computed(() => {
  return props.hoverable ? 'hover:shadow-md hover:-translate-y-0.5 cursor-pointer' : ''
})
</script>

<template>
  <div
    v-bind="$attrs"
    :class="cn(
      baseClasses,
      variantClasses,
      paddingClasses,
      hoverClasses,
      props.class
    )"
  >
    <slot />
  </div>
</template>
