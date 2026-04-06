<script setup>
import { AvatarFallback } from "radix-vue";
import { cn } from "@/lib/utils";

const props = defineProps({
  delayMs: { type: Number, required: false },
  asChild: { type: Boolean, required: false },
  as: { type: null, required: false },
  class: { type: String, default: '' },
});
</script>

<template>
  <AvatarFallback v-bind="props" :class="cn('flex h-full w-full items-center justify-center rounded-full bg-gray-50 overflow-hidden', props.class)">
    <img src="/default.png" class="h-full w-full object-cover" alt="默认头像" />
  </AvatarFallback>
</template>
