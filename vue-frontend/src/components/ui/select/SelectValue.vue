<script setup>
import { SelectValue } from 'radix-vue'

const props = defineProps({
  placeholder: { type: String, default: '' },
  asChild: { type: Boolean, default: false },
  as: { type: String, default: undefined },
})
</script>

<template>
  <SelectValue v-bind="props">
    <slot />
  </SelectValue>
</template>