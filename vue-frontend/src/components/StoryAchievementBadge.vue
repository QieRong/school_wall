<script setup>
import { computed } from 'vue'

const props = defineProps({
  type: {
    type: String,
    required: true,
    validator: (val) => ['opener', 'key_puzzle', 'long_creator', 'story_king', 'active_writer', 'popular_author'].includes(val)
  },
  unlocked: {
    type: Boolean,
    default: false
  },
  size: {
    type: String,
    default: 'md',
    validator: (val) => ['sm', 'md', 'lg'].includes(val)
  },
  showLabel: {
    type: Boolean,
    default: true
  }
})

// 成就配置
const achievementConfig = {
  opener: {
    icon: '🖋️',
    label: '开篇者',
    description: '创建第一个故事',
    color: 'gold'
  },
  key_puzzle: {
    icon: '🧩',
    label: '关键拼图',
    description: '段落成为关键转折点',
    color: 'purple'
  },
  long_creator: {
    icon: '📚',
    label: '长篇缔造者',
    description: '单故事贡献超过10段',
    color: 'blue'
  },
  story_king: {
    icon: '👑',
    label: '故事之王',
    description: '总贡献度排名第一',
    color: 'emerald'
  },
  active_writer: {
    icon: '✍️',
    label: '活跃作者',
    description: '累计续写50段',
    color: 'teal'
  },
  popular_author: {
    icon: '⭐',
    label: '人气作者',
    description: '获得100个点赞',
    color: 'cyan'
  }
}

const config = computed(() => achievementConfig[props.type])

// 尺寸类
const sizeClass = computed(() => {
  switch (props.size) {
    case 'sm': return 'badge-sm'
    case 'lg': return 'badge-lg'
    default: return 'badge-md'
  }
})
</script>

<template>
  <div class="achievement-badge" :class="[sizeClass, config.color, { unlocked, locked: !unlocked }]"
    :title="config.description">
    <div class="badge-icon">
      <span class="icon-emoji">{{ config.icon }}</span>
      <div v-if="unlocked" class="glow-effect"></div>
    </div>
    <div v-if="showLabel" class="badge-label">{{ config.label }}</div>
  </div>
</template>

<style scoped>
.achievement-badge {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  cursor: default;
  transition: all 0.3s;
}

/* 尺寸 */
.badge-sm .badge-icon {
  width: 40px;
  height: 40px;
  font-size: 20px;
}

.badge-sm .badge-label {
  font-size: 10px;
}

.badge-md .badge-icon {
  width: 56px;
  height: 56px;
  font-size: 28px;
}

.badge-md .badge-label {
  font-size: 12px;
}

.badge-lg .badge-icon {
  width: 72px;
  height: 72px;
  font-size: 36px;
}

.badge-lg .badge-label {
  font-size: 14px;
}

/* 图标容器 */
.badge-icon {
  position: relative;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.icon-emoji {
  position: relative;
  z-index: 1;
}

/* 未解锁状态 */
.locked .badge-icon {
  background: #e5e7eb;
  filter: grayscale(100%);
  opacity: 0.5;
}

.locked .badge-label {
  color: #9ca3af;
}

/* 解锁状态 - 金色 */
.unlocked.gold .badge-icon {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  border: 3px solid #f59e0b;
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.4);
}

.unlocked.gold .badge-label {
  color: #b45309;
  font-weight: 600;
}

/* 解锁状态 - 紫色 */
.unlocked.purple .badge-icon {
  background: linear-gradient(135deg, #f3e8ff 0%, #e9d5ff 100%);
  border: 3px solid #a855f7;
  box-shadow: 0 4px 12px rgba(168, 85, 247, 0.4);
}

.unlocked.purple .badge-label {
  color: #7c3aed;
  font-weight: 600;
}

/* 解锁状态 - 绿色 */
.unlocked.emerald .badge-icon {
  background: linear-gradient(135deg, #d1fae5 0%, #a7f3d0 100%);
  border: 3px solid rgb(33, 111, 85);
  box-shadow: 0 4px 12px rgba(33, 111, 85, 0.4);
}

.unlocked.emerald .badge-label {
  color: #065f46;
  font-weight: 600;
}

/* 解锁状态 - 青色 */
.unlocked.teal .badge-icon {
  background: linear-gradient(135deg, #ccfbf1 0%, #99f6e4 100%);
  border: 3px solid #14b8a6;
  box-shadow: 0 4px 12px rgba(20, 184, 166, 0.4);
}

.unlocked.teal .badge-label {
  color: #0f766e;
  font-weight: 600;
}

/* 解锁状态 - 天蓝色 */
.unlocked.cyan .badge-icon {
  background: linear-gradient(135deg, #cffafe 0%, #a5f3fc 100%);
  border: 3px solid #06b6d4;
  box-shadow: 0 4px 12px rgba(6, 182, 212, 0.4);
}

.unlocked.cyan .badge-label {
  color: #0e7490;
  font-weight: 600;
}

/* 发光效果 */
.glow-effect {
  position: absolute;
  top: -4px;
  left: -4px;
  right: -4px;
  bottom: -4px;
  border-radius: 50%;
  animation: glow 2s ease-in-out infinite;
  pointer-events: none;
}

.gold .glow-effect {
  background: radial-gradient(circle, rgba(245, 158, 11, 0.3) 0%, transparent 70%);
}

.purple .glow-effect {
  background: radial-gradient(circle, rgba(168, 85, 247, 0.3) 0%, transparent 70%);
}

.blue .glow-effect {
  background: radial-gradient(circle, rgba(59, 130, 246, 0.3) 0%, transparent 70%);
}

.emerald .glow-effect {
  background: radial-gradient(circle, rgba(33, 111, 85, 0.3) 0%, transparent 70%);
}

.teal .glow-effect {
  background: radial-gradient(circle, rgba(20, 184, 166, 0.3) 0%, transparent 70%);
}

.cyan .glow-effect {
  background: radial-gradient(circle, rgba(6, 182, 212, 0.3) 0%, transparent 70%);
}

/* 解锁状态 - 琥珀色 */
.unlocked.amber .badge-icon {
  background: linear-gradient(135deg, #fef3c7 0%, #fcd34d 100%);
  border: 3px solid #d97706;
  box-shadow: 0 4px 12px rgba(217, 119, 6, 0.4);
}

.unlocked.amber .badge-label {
  color: #92400e;
  font-weight: 600;
}

/* 发光效果 */
.glow-effect {
  position: absolute;
  top: -4px;
  left: -4px;
  right: -4px;
  bottom: -4px;
  border-radius: 50%;
  animation: glow 2s ease-in-out infinite;
  pointer-events: none;
}

.gold .glow-effect {
  background: radial-gradient(circle, rgba(245, 158, 11, 0.3) 0%, transparent 70%);
}

.purple .glow-effect {
  background: radial-gradient(circle, rgba(168, 85, 247, 0.3) 0%, transparent 70%);
}

.blue .glow-effect {
  background: radial-gradient(circle, rgba(59, 130, 246, 0.3) 0%, transparent 70%);
}

.amber .glow-effect {
  background: radial-gradient(circle, rgba(217, 119, 6, 0.3) 0%, transparent 70%);
}

@keyframes glow {

  0%,
  100% {
    opacity: 0.5;
    transform: scale(1);
  }

  50% {
    opacity: 1;
    transform: scale(1.1);
  }
}

/* 悬停效果 */
.unlocked:hover {
  transform: translateY(-4px);
}

.unlocked:hover .badge-icon {
  transform: scale(1.1);
}

/* 标签 */
.badge-label {
  text-align: center;
  white-space: nowrap;
}
</style>
