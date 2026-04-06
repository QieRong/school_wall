<script setup>
import { SelectContent, SelectPortal, SelectViewport, useForwardPropsEmits } from 'radix-vue'
import { cn } from '@/lib/utils'

const props = defineProps({
  position: { type: String, default: 'popper' },
  side: { type: String, default: undefined },
  sideOffset: { type: Number, default: 4 },
  align: { type: String, default: undefined },
  alignOffset: { type: Number, default: undefined },
  avoidCollisions: { type: Boolean, default: undefined },
  collisionBoundary: { type: [Object, Array], default: undefined },
  collisionPadding: { type: [Number, Object], default: undefined },
  arrowPadding: { type: Number, default: undefined },
  sticky: { type: String, default: undefined },
  hideWhenDetached: { type: Boolean, default: undefined },
  updatePositionStrategy: { type: String, default: undefined },
  prioritizePosition: { type: Boolean, default: undefined },
  asChild: { type: Boolean, default: false },
  as: { type: String, default: undefined },
  class: { type: String, default: '' },
})
const emits = defineEmits(['closeAutoFocus', 'escapeKeyDown', 'pointerDownOutside'])

const forwarded = useForwardPropsEmits(props, emits)
</script>

<template>
  <SelectPortal>
    <SelectContent v-bind="forwarded" :class="cn(
      'relative z-50 max-h-96 min-w-[8rem] overflow-hidden rounded-md border bg-popover text-popover-foreground shadow-md data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95 data-[side=bottom]:slide-in-from-top-2 data-[side=left]:slide-in-from-right-2 data-[side=right]:slide-in-from-left-2 data-[side=top]:slide-in-from-bottom-2',
      position === 'popper' &&
      'data-[side=bottom]:translate-y-1 data-[side=left]:-translate-x-1 data-[side=right]:translate-x-1 data-[side=top]:-translate-y-1',
      props.class,
    )">
      <SelectViewport
        :class="cn('p-1', position === 'popper' && 'h-[var(--radix-select-trigger-height)] w-full min-w-[var(--radix-select-trigger-width)]')">
        <slot />
      </SelectViewport>
    </SelectContent>
  </SelectPortal>
</template>