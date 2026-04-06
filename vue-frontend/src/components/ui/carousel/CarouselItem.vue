<template>
  <div role="group" aria-roledescription="slide" :class="itemClass">
    <slot />
  </div>
</template>

<script setup>
import { computed } from "vue";
import { useCarousel } from "./useCarousel";
import { cn } from "@/lib/utils";

const { orientation } = useCarousel();

const itemClass = computed(() =>
  cn(
    "min-w-0 shrink-0 grow-0 basis-full",
    orientation === "horizontal" ? "pl-4" : "pt-4",
  )
);
</script>