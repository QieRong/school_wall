<script setup>
import { computed } from 'vue'
import { Button } from '@/components/ui/button'
import { Loader2 } from 'lucide-vue-next'

const props = defineProps({
  loading: {
    type: Boolean,
    default: false
  },
  disabled: {
    type: Boolean,
    default: false
  },
  variant: {
    type: String,
    default: 'default'
  },
  size: {
    type: String,
    default: 'default'
  },
  class: {
    type: String,
    default: ''
  },
  loadingText: {
    type: String,
    default: '处理中...'
  }
})

const emit = defineEmits(['click'])

const isDisabled = computed(() => props.loading || props.disabled)

const handleClick = (event) => {
  if (!isDisabled.value) {
    emit('click', event)
  }
}
</script>

<template>
  <Button
    :variant="variant"
    :size="size"
    :disabled="isDisabled"
    :class="[
      props.class,
      { 'cursor-not-allowed opacity-60': isDisabled }
    ]"
    @click="handleClick"
  >
    <template v-if="loading">
      <Loader2 class="w-4 h-4 mr-2 animate-spin" />
      <span>{{ loadingText }}</span>
    </template>
    <template v-else>
      <slot />
    </template>
  </Button>
</template>

<style scoped>
@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.animate-spin {
  animation: spin 1s linear infinite;
}
</style>
