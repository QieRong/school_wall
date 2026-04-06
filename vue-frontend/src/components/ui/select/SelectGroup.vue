<script setup>
import { SelectGroup } from 'radix-vue'
import { cn } from '@/lib/utils'
const props = defineProps({ class: { type: String, default: '' } })
</script>
<template>
  <SelectGroup :class="cn('p-1', props.class)">
    <slot />
  </SelectGroup>
</template>