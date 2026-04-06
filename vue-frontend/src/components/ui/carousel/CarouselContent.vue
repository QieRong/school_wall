<template>
  <div ref="carouselRef" class="overflow-hidden">
    <div :class="contentClass">
      <slot />
    </div>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { useCarousel } from "./useCarousel";
import { cn } from "@/lib/utils";

// 这里现在能正确 inject 到数据了
const { carouselRef, orientation } = useCarousel();

const contentClass = computed(() =>
  cn(
    "flex",
    orientation === "horizontal" ? "-ml-4" : "-mt-4 flex-col",
    // 传递给组件的 class 会自动透传，这里处理默认样式
  )
);
</script>