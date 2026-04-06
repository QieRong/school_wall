<template>
  <div class="ranking-container">
    <div class="ranking-header">
      <h2 class="neon-title">🏆 热词榜</h2>
    </div>

    <!-- Tab 切换 -->
    <div class="ranking-tabs">
      <button v-for="tab in tabs" :key="tab.value" :class="['tab-btn', { active: activeTab === tab.value }]"
        @click="$emit('tabChange', tab.value)">
        {{ tab.label }}
      </button>
    </div>

    <!-- 榜单列表 -->
    <div class="ranking-list">
      <div v-for="(item, index) in rankings" :key="item.id" class="ranking-item" :class="getRankClass(index)"
        @click="$emit('itemClick', item.id)">
        <div class="rank-number">
          <span v-if="index < 3" class="trophy">{{ trophies[index] }}</span>
          <span v-else>{{ index + 1 }}</span>
        </div>

        <div class="rank-info">
          <span class="rank-name">{{ item.name }}</span>
          <span class="rank-votes">{{ item.totalVotes }}票</span>
        </div>

        <div class="rank-change" :class="getChangeClass(item.rankChange)">
          <span v-if="item.rankChange > 0">↑{{ item.rankChange }}</span>
          <span v-else-if="item.rankChange < 0">↓{{ Math.abs(item.rankChange) }}</span>
          <span v-else>-</span>
        </div>

        <span v-if="item.isRecommended" class="recommend-tag">荐</span>
      </div>

      <div v-if="!rankings.length" class="empty-tip">暂无数据</div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  rankings: { type: Array, default: () => [] },
  activeTab: { type: String, default: 'day' }
})

defineEmits(['tabChange', 'itemClick'])

const tabs = [
  { label: '日榜', value: 'day' },
  { label: '周榜', value: 'week' },
  { label: '月榜', value: 'month' },
  { label: '总榜', value: 'all' }
]

const trophies = ['🥇', '🥈', '🥉']

const getRankClass = (index) => {
  if (index === 0) return 'rank-gold'
  if (index === 1) return 'rank-silver'
  if (index === 2) return 'rank-bronze'
  return ''
}

const getChangeClass = (change) => {
  if (change > 0) return 'change-up'
  if (change < 0) return 'change-down'
  return 'change-same'
}
</script>

<style scoped>
.ranking-container {
  background: rgba(0, 0, 0, 0.6);
  border-radius: 16px;
  padding: 20px;
  height: 100%;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.ranking-header {
  text-align: center;
  margin-bottom: 16px;
}

.neon-title {
  color: #ff6b6b;
  font-size: 24px;
  text-shadow: 0 0 10px #ff6b6b, 0 0 20px #ff6b6b;
  animation: neon-flicker 2s infinite;
}

@keyframes neon-flicker {

  0%,
  100% {
    opacity: 1;
  }

  50% {
    opacity: 0.8;
  }
}

.ranking-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.tab-btn {
  flex: 1;
  padding: 8px;
  border: none;
  background: rgba(255, 255, 255, 0.1);
  color: #aaa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.tab-btn.active {
  background: linear-gradient(135deg, #059669, #10b981);
  color: white;
}

.ranking-list {
  flex: 1;
  overflow-y: auto;
  padding-right: 8px;
}

/* 自定义滚动条样式 */
.ranking-list::-webkit-scrollbar {
  width: 8px;
}

.ranking-list::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 10px;
}

.ranking-list::-webkit-scrollbar-thumb {
  background: linear-gradient(135deg, #059669, #10b981);
  border-radius: 10px;
  transition: all 0.3s;
}

.ranking-list::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(135deg, #10b981, #34d399);
  box-shadow: 0 0 10px rgba(16, 185, 129, 0.5);
}

.ranking-item {
  display: flex;
  align-items: center;
  padding: 12px;
  margin-bottom: 8px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s;
}

.ranking-item:hover {
  background: rgba(255, 255, 255, 0.1);
  transform: translateX(5px);
}

.rank-gold {
  border-left: 3px solid #ffd700;
}

.rank-silver {
  border-left: 3px solid #c0c0c0;
}

.rank-bronze {
  border-left: 3px solid #cd7f32;
}

.rank-number {
  width: 36px;
  text-align: center;
  font-size: 18px;
  color: #888;
}

.trophy {
  font-size: 24px;
}

.rank-info {
  flex: 1;
  margin-left: 12px;
}

.rank-name {
  display: block;
  color: #fff;
  font-weight: bold;
  margin-bottom: 4px;
}

.rank-votes {
  font-size: 12px;
  color: #888;
}

.rank-change {
  font-size: 14px;
  font-weight: bold;
  min-width: 40px;
  text-align: right;
}

.change-up {
  color: #4ade80;
}

.change-down {
  color: #f87171;
}

.change-same {
  color: #888;
}

.recommend-tag {
  background: linear-gradient(135deg, #ffd700, #ff8c00);
  color: #333;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 4px;
  margin-left: 8px;
}

.empty-tip {
  text-align: center;
  color: #666;
  padding: 40px;
}
</style>
