<template>
  <div class="relative" role="region" aria-roledescription="carousel" tabindex="0" @keydown="onKeyDown">
    <slot />
  </div>
</template>

<script setup>
import { useProvideCarousel } from "./useCarousel";

const props = defineProps({
  opts: { type: Object, default: () => ({}) },
  plugins: { type: Array, default: () => [] },
  orientation: { type: String, default: "horizontal" },
  setApi: { type: Function },
});

// 使用我们重写的 provide 函数
const { scrollPrev, scrollNext } = useProvideCarousel(props);

const onKeyDown = (event) => {
  if (event.key === "ArrowLeft") {
    event.preventDefault();
    scrollPrev();
  } else if (event.key === "ArrowRight") {
    event.preventDefault();
    scrollNext();
  }
};
</script>