<template>
  <div class="hotword-bubble" :class="[heatLevelClass, { recommended: hotword.isRecommended }]" @click="$emit('click')"
    @mousedown="startLongPress" @mouseup="endLongPress" @mouseleave="endLongPress" @touchstart="startLongPress"
    @touchend="endLongPress">
    <div class="bubble-content">
      <span class="hotword-name">{{ hotword.name }}</span>
      <span class="vote-count" v-if="showVotes">{{ hotword.totalVotes }}票</span>
    </div>

    <!-- 投票按钮 -->
    <button class="vote-btn" @click.stop="handleVote(1)" :disabled="!canVote">
      🔥
    </button>

    <!-- 官方推荐标识 -->
    <span v-if="hotword.isRecommended" class="recommend-badge">官方推荐</span>

    <!-- 火焰动画 -->
    <div v-if="showFlame" class="flame-animation" :class="{ strong: isStrongVote }"></div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  hotword: { type: Object, required: true }
})

const emit = defineEmits(['click', 'vote'])

const showVotes = ref(false)
const showFlame = ref(false)
const isStrongVote = ref(false)
const longPressTimer = ref(null)
const canVote = ref(true)

const heatLevelClass = computed(() => {
  const level = props.hotword.heatLevel || '新芽'
  const map = { '新芽': 'level-sprout', '升温': 'level-warm', '火爆': 'level-hot', '现象级': 'level-viral' }
  const className = map[level] || 'level-sprout'
  console.log(`气泡 ${props.hotword.name}: heatLevel=${level}, class=${className}`)
  return className
})

const handleVote = (count) => {
  showFlame.value = true
  isStrongVote.value = count === 2
  emit('vote', props.hotword.id, count)
  setTimeout(() => { showFlame.value = false }, 800)
}

const startLongPress = () => {
  longPressTimer.value = setTimeout(() => {
    handleVote(2) // 强烈认同
  }, 800)
}

const endLongPress = () => {
  if (longPressTimer.value) {
    clearTimeout(longPressTimer.value)
    longPressTimer.value = null
  }
}
</script>

<style scoped>
.hotword-bubble {
  position: absolute;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.3s, box-shadow 0.3s;
  animation: float 6s ease-in-out infinite;
  backdrop-filter: blur(10px);
  user-select: none;
}

.hotword-bubble:hover {
  transform: scale(1.2);
  z-index: 10;
}

.hotword-bubble:hover .vote-count {
  opacity: 1;
}

.hotword-bubble:active {
  transform: scale(1.15);
}

/* 热度等级样式 */
.level-sprout {
  background: rgba(144, 238, 144, 0.3);
  border: 2px solid rgba(144, 238, 144, 0.6);
  box-shadow: 0 0 15px rgba(144, 238, 144, 0.3);
}

.level-warm {
  background: rgba(255, 165, 0, 0.3);
  border: 2px solid rgba(255, 165, 0, 0.6);
  box-shadow: 0 0 20px rgba(255, 165, 0, 0.4);
}

.level-hot {
  background: rgba(255, 69, 0, 0.3);
  border: 3px solid rgba(255, 69, 0, 0.8);
  box-shadow: 0 0 30px rgba(255, 69, 0, 0.5);
  animation: float 6s ease-in-out infinite, glow 1.5s ease-in-out infinite;
}

.level-viral {
  background: linear-gradient(135deg, rgba(255, 0, 128, 0.4), rgba(128, 0, 255, 0.4));
  border: 3px solid rgba(255, 0, 128, 0.8);
  box-shadow: 0 0 40px rgba(255, 0, 128, 0.6), 0 0 60px rgba(128, 0, 255, 0.4);
  animation: float 6s ease-in-out infinite, pulse 1s ease-in-out infinite;
}

.bubble-content {
  text-align: center;
  color: white;
  padding: 10px;
}

.hotword-name {
  font-weight: bold;
  text-shadow: 0 0 10px currentColor;
}

.level-sprout .hotword-name {
  font-size: 12px;
}

.level-warm .hotword-name {
  font-size: 14px;
}

.level-hot .hotword-name {
  font-size: 16px;
}

.level-viral .hotword-name {
  font-size: 18px;
}

.vote-count {
  display: block;
  font-size: 10px;
  opacity: 0;
  transition: opacity 0.3s;
  margin-top: 4px;
}

.vote-btn {
  position: absolute;
  bottom: -5px;
  right: -5px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 2px solid white;
  background: linear-gradient(135deg, #ff6b6b, #ff4757);
  cursor: pointer;
  font-size: 16px;
  opacity: 1;
  transition: opacity 0.3s, transform 0.2s;
  box-shadow: 0 2px 8px rgba(255, 71, 87, 0.4);
  animation: pulse-hint 2s ease-in-out infinite;
  z-index: 10;
}

@keyframes pulse-hint {

  0%,
  100% {
    transform: scale(1);
    box-shadow: 0 2px 8px rgba(255, 71, 87, 0.4);
  }

  50% {
    transform: scale(1.15);
    box-shadow: 0 4px 12px rgba(255, 71, 87, 0.7);
  }
}

.hotword-bubble:hover .vote-btn {
  opacity: 1;
  animation: none;
}

.vote-btn:hover {
  transform: scale(1.3);
  background: linear-gradient(135deg, #ff4757, #ff3838);
  box-shadow: 0 4px 15px rgba(255, 71, 87, 0.8);
}

.vote-btn:active {
  transform: scale(1.1);
}

.recommend-badge {
  position: absolute;
  top: -8px;
  left: 50%;
  transform: translateX(-50%);
  background: linear-gradient(135deg, #ffd700, #ff8c00);
  color: #333;
  font-size: 8px;
  padding: 2px 6px;
  border-radius: 10px;
  white-space: nowrap;
}

.flame-animation {
  position: absolute;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 100, 0, 0.8) 0%, transparent 70%);
  animation: flame 0.8s ease-out forwards;
}

.flame-animation.strong {
  background: radial-gradient(circle, rgba(255, 50, 0, 1) 0%, rgba(255, 150, 0, 0.8) 50%, transparent 70%);
}

@keyframes float {

  0%,
  100% {
    transform: translateY(0);
  }

  50% {
    transform: translateY(-15px);
  }
}

@keyframes glow {

  0%,
  100% {
    box-shadow: 0 0 30px rgba(255, 69, 0, 0.5);
  }

  50% {
    box-shadow: 0 0 50px rgba(255, 69, 0, 0.8);
  }
}

@keyframes pulse {

  0%,
  100% {
    transform: scale(1);
  }

  50% {
    transform: scale(1.05);
  }
}

@keyframes flame {
  0% {
    opacity: 1;
    transform: scale(1);
  }

  100% {
    opacity: 0;
    transform: scale(2);
  }
}
</style>
