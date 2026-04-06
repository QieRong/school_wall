<script setup>
import { AvatarRoot } from "radix-vue"; //必须统一为 radix-vue
import { cn } from "@/lib/utils";
import { avatarVariant } from ".";

const props = defineProps({
  class: { type: String, default: '' },
  size: { type: String, default: "sm" },
  shape: { type: String, default: "circle" },
});
</script>

<template>
  <AvatarRoot :class="cn(avatarVariant({ size, shape }), props.class)">
    <slot />
  </AvatarRoot>
</template>