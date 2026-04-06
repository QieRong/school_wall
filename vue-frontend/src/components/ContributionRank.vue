<script setup>
import { computed } from 'vue'
import { Avatar, AvatarImage, AvatarFallback } from '@/components/ui/avatar'
import { Trophy, User, PenTool, Heart, Flame, Crown, Medal } from 'lucide-vue-next'

const props = defineProps({
  rankings: {
    type: Array,
    default: () => []
  },
  maxDisplay: {
    type: Number,
    default: 10
  },
  showTitle: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['userClick'])

// 显示的排行数据
const displayRankings = computed(() => {
  return props.rankings.slice(0, props.maxDisplay)
})

// 获取排名样式
const getRankStyle = (index) => {
  if (index === 0) return 'gold'
  if (index === 1) return 'silver'
  if (index === 2) return 'bronze'
  return 'normal'
}

// 获取排名图标
const getRankIcon = (index) => {
  if (index === 0) return Crown
  if (index === 1) return Medal
  if (index === 2) return Medal
  return null
}

// 点击用户
const handleUserClick = (userId) => {
  emit('userClick', userId)
}
</script>

<template>
  <div class="contribution-rank">
    <!-- 标题 -->
    <h3 v-if="showTitle" class="rank-title">
      <Trophy class="w-5 h-5 text-yellow-500" />
      贡献度排行
    </h3>

    <!-- 空状态 -->
    <div v-if="displayRankings.length === 0" class="empty-state">
      <div class="text-4xl mb-2">📝</div>
      <p>暂无贡献数据</p>
    </div>

    <!-- 排行列表 -->
    <div v-else class="rank-list">
      <div v-for="(rank, idx) in displayRankings" :key="rank.userId" class="rank-item" :class="getRankStyle(idx)"
        @click="handleUserClick(rank.userId)">
        <!-- 排名 -->
        <div class="rank-number" :class="getRankStyle(idx)">
          <component v-if="getRankIcon(idx)" :is="getRankIcon(idx)" class="w-4 h-4" />
          <span v-else>{{ idx + 1 }}</span>
        </div>

        <!-- 头像 -->
        <Avatar class="rank-avatar" :class="getRankStyle(idx)">
          <AvatarImage :src="rank.avatar" />
          <AvatarFallback>
            <User class="w-4 h-4" />
          </AvatarFallback>
        </Avatar>

        <!-- 用户信息 -->
        <div class="rank-info">
          <div class="rank-name">{{ rank.nickname }}</div>
          <div class="rank-stats">
            <span class="stat-item">
              <PenTool class="w-3 h-3" /> {{ rank.paragraphCount }}段
            </span>
            <span class="stat-item">
              <Heart class="w-3 h-3" /> {{ rank.likeReceived }}赞
            </span>
            <span v-if="rank.keyPointCount > 0" class="stat-item key">
              <Flame class="w-3 h-3" /> {{ rank.keyPointCount }}
            </span>
          </div>
        </div>

        <!-- 贡献分数 -->
        <div class="rank-points" :class="getRankStyle(idx)">
          {{ rank.points }}<span class="unit">分</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.contribution-rank {
  background: rgba(209, 250, 229, 0.5);
  border-radius: 12px;
  padding: 16px;
  border: 1px solid rgba(33, 111, 85, 0.2);
}

.rank-title {
  font-weight: 700;
  color: #065f46;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
}

.empty-state {
  text-align: center;
  padding: 24px;
  color: #065f46;
  opacity: 0.7;
}

.rank-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.rank-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  background: rgba(255, 255, 255, 0.5);
}

.rank-item:hover {
  background: rgba(33, 111, 85, 0.1);
  transform: translateX(4px);
}

/* 前三名特殊样式 */
.rank-item.gold {
  background: linear-gradient(135deg, rgba(250, 204, 21, 0.2) 0%, rgba(234, 179, 8, 0.1) 100%);
  border: 1px solid rgba(234, 179, 8, 0.3);
}

.rank-item.silver {
  background: linear-gradient(135deg, rgba(156, 163, 175, 0.2) 0%, rgba(107, 114, 128, 0.1) 100%);
  border: 1px solid rgba(156, 163, 175, 0.3);
}

.rank-item.bronze {
  background: linear-gradient(135deg, rgba(33, 111, 85, 0.2) 0%, rgba(5, 150, 105, 0.1) 100%);
  border: 1px solid rgba(33, 111, 85, 0.3);
}

/* 排名数字 */
.rank-number {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}

.rank-number.gold {
  background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.4);
}

.rank-number.silver {
  background: linear-gradient(135deg, #9ca3af 0%, #6b7280 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(107, 114, 128, 0.4);
}

.rank-number.bronze {
  background: linear-gradient(135deg, rgb(33, 111, 85) 0%, #059669 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(33, 111, 85, 0.4);
}

.rank-number.normal {
  background: rgba(33, 111, 85, 0.15);
  color: #065f46;
}

/* 头像 */
.rank-avatar {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
}

.rank-avatar.gold {
  border: 2px solid #f59e0b;
}

.rank-avatar.silver {
  border: 2px solid #9ca3af;
}

.rank-avatar.bronze {
  border: 2px solid rgb(33, 111, 85);
}

/* 用户信息 */
.rank-info {
  flex: 1;
  min-width: 0;
}

.rank-name {
  font-size: 14px;
  font-weight: 600;
  color: #065f46;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rank-stats {
  display: flex;
  gap: 8px;
  margin-top: 2px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 11px;
  color: #059669;
}

.stat-item.key {
  color: #ea580c;
}

/* 贡献分数 */
.rank-points {
  font-size: 16px;
  font-weight: 700;
  color: #065f46;
  flex-shrink: 0;
}

.rank-points.gold {
  color: #d97706;
  text-shadow: 0 1px 2px rgba(217, 119, 6, 0.3);
}

.rank-points.silver {
  color: #6b7280;
}

.rank-points.bronze {
  color: rgb(33, 111, 85);
}

.rank-points .unit {
  font-size: 12px;
  font-weight: 400;
  margin-left: 2px;
}
</style>
