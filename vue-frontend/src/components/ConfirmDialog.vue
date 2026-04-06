<script setup>
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { AlertTriangle, Info, AlertCircle } from 'lucide-vue-next'

const props = defineProps({
  open: Boolean,
  title: { type: String, default: '确认操作' },
  message: { type: String, required: true },
  confirmText: { type: String, default: '确认' },
  cancelText: { type: String, default: '取消' },
  type: { type: String, default: 'warning' }
})

const emit = defineEmits(['update:open', 'confirm', 'cancel'])

const handleConfirm = () => {
  emit('confirm')
  emit('update:open', false)
}

const handleCancel = () => {
  emit('cancel')
  emit('update:open', false)
}

const iconComponent = {
  warning: AlertTriangle,
  danger: AlertCircle,
  info: Info
}

const iconColor = {
  warning: 'text-orange-500',
  danger: 'text-red-500',
  info: 'text-blue-500'
}

const buttonClass = {
  warning: 'bg-orange-500 hover:bg-orange-600',
  danger: 'bg-red-500 hover:bg-red-600',
  info: 'bg-blue-500 hover:bg-blue-600'
}
</script>

<template>
  <Dialog :open="open" @update:open="emit('update:open', $event)" class="z-[10001]">
    <DialogContent class="sm:max-w-[420px] bg-white rounded-xl z-[10001]">
      <DialogDescription class="sr-only">确认对话框</DialogDescription>
      <DialogHeader>
        <DialogTitle class="flex items-center gap-2 text-lg font-bold">
          <component :is="iconComponent[type]" class="w-5 h-5" :class="iconColor[type]" />
          <span>{{ title }}</span>
        </DialogTitle>
      </DialogHeader>
      <div class="py-4">
        <p class="text-gray-600 leading-relaxed">{{ message }}</p>
      </div>
      <DialogFooter class="gap-2">
        <Button variant="outline" @click="handleCancel" class="rounded-lg">{{ cancelText }}</Button>
        <Button :class="buttonClass[type]" class="text-white rounded-lg" @click="handleConfirm">
          {{ confirmText }}
        </Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>
