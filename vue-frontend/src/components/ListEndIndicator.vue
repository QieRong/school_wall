<template>
  <div class="list-end-indicator">
    <div class="divider-line"></div>
    <div class="end-text">
      <span class="icon">{{ icon }}</span>
      <span>{{ text }}</span>
    </div>
    <div class="divider-line"></div>
  </div>
</template>

<script setup>
defineProps({
  text: {
    type: String,
    default: '没有更多了'
  },
  icon: {
    type: String,
    default: '📭'
  }
})
</script>

<style scoped>
.list-end-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 32px 16px;
  margin: 16px 0;
}

.divider-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(to right, transparent, rgba(156, 163, 175, 0.3), transparent);
  max-width: 120px;
}

.end-text {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #9ca3af;
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
}

.icon {
  font-size: 18px;
  opacity: 0.8;
}

/* 动画效果 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.list-end-indicator {
  animation: fadeIn 0.5s ease-out;
}
</style>
