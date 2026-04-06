<template>
  <div v-if="password" class="mt-2">
    <div class="flex items-center gap-2">
      <!-- 强度条 -->
      <div class="flex-1 h-2 bg-gray-200 rounded-full overflow-hidden">
        <div 
          class="h-full transition-all duration-300"
          :class="{
            'w-1/3 bg-red-500': strength.level === 'weak',
            'w-2/3 bg-orange-500': strength.level === 'medium',
            'w-full bg-green-500': strength.level === 'strong'
          }"
        ></div>
      </div>
      
      <!-- 强度文字 -->
      <span 
        class="text-xs font-medium whitespace-nowrap"
        :class="{
          'text-red-500': strength.level === 'weak',
          'text-orange-500': strength.level === 'medium',
          'text-green-500': strength.level === 'strong'
        }"
      >
        {{ strength.level === 'weak' ? '弱' : strength.level === 'medium' ? '中' : '强' }}
      </span>
    </div>
    
    <!-- 提示信息 -->
    <p class="text-xs mt-1" :class="`text-${strength.color}-600`">
      {{ strength.message }}
    </p>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { checkPasswordStrength } from '@/utils/validation'

const props = defineProps({
  password: {
    type: String,
    default: ''
  }
})

const strength = computed(() => checkPasswordStrength(props.password))
</script>
