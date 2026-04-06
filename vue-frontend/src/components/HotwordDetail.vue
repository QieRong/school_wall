<template>
  <div class="detail-overlay" @click.self="$emit('close')">
    <div class="detail-card" :class="{ flipped: isFlipped }">
      <button class="close-btn" @click="$emit('close')">×</button>
      
      <!-- 热词名称 -->
      <h2 class="hotword-title neon-text">{{ detail?.name || hotword.name }}</h2>
      
      <!-- 热度等级徽章 -->
      <div class="heat-badge" :class="heatClass">
        {{ detail?.heatLevel || hotword.heatLevel }}
        <span v-if="detail?.isRecommended" class="recommend">官方推荐</span>
      </div>
      
      <!-- 票数 -->
      <div class="vote-info">
        <span class="vote-count">🔥 {{ detail?.totalVotes || hotword.totalVotes }} 票</span>
        <button class="vote-btn" @click="handleVote(1)">投一票</button>
        <button class="vote-btn strong" @click="handleVote(2)">强烈认同</button>
      </div>
      
      <!-- 释义 -->
      <div class="section">
        <div class="quote-mark">"</div>
        <p class="definition">{{ detail?.definition || hotword.definition }}</p>
        <div class="quote-mark end">"</div>
      </div>
      
      <!-- 例句 -->
      <div v-if="detail?.example || hotword.example" class="section">
        <label>例句</label>
        <p class="example">{{ detail?.example || hotword.example }}</p>
      </div>
      
      <!-- 标签 -->
      <div v-if="tags.length" class="tags">
        <span v-for="tag in tags" :key="tag" class="tag">{{ tag }}</span>
      </div>
      
      <!-- 配图 -->
      <div v-if="detail?.imageUrl || hotword.imageUrl" class="image-section">
        <img :src="detail?.imageUrl || hotword.imageUrl" alt="配图" />
      </div>
      
      <!-- 投稿者信息 -->
      <div class="author-info">
        <img :src="detail?.authorAvatar || hotword.authorAvatar || '/default.png'" class="avatar" />
        <span>{{ detail?.authorNickname || hotword.authorNickname || '匿名用户' }}</span>
        <span class="time">{{ formatTime(detail?.createTime || hotword.createTime) }}</span>
      </div>
      
      <!-- 图表区域 -->
      <div v-if="detail" class="charts-section">
        <div class="chart-tabs">
          <button :class="{ active: chartTab === '24h' }" @click="loadTrend(24)">24小时</button>
          <button :class="{ active: chartTab === '7d' }" @click="loadTrend(168)">7天</button>
        </div>
        <div ref="trendChart" class="chart"></div>
        <div ref="pieChart" class="chart pie"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { getHotwordDetail } from '@/api/hotword'
import * as echarts from 'echarts'

const props = defineProps({
  hotword: { type: Object, required: true }
})

const emit = defineEmits(['close', 'vote'])

const detail = ref(null)
const isFlipped = ref(false)
const chartTab = ref('24h')
const trendChart = ref(null)
const pieChart = ref(null)
let trendChartInstance = null
let pieChartInstance = null

const tags = computed(() => {
  const t = detail.value?.tags || props.hotword.tags
  if (Array.isArray(t)) return t
  if (typeof t === 'string') return t.split(',').filter(Boolean)
  return []
})

const heatClass = computed(() => {
  const level = detail.value?.heatLevel || props.hotword.heatLevel
  const map = { '新芽': 'heat-sprout', '升温': 'heat-warm', '火爆': 'heat-hot', '现象级': 'heat-viral' }
  return map[level] || 'heat-sprout'
})

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleDateString('zh-CN')
}

const handleVote = (count) => {
  emit('vote', props.hotword.id, count)
}

const loadDetail = async (hours = 24) => {
  try {
    const res = await getHotwordDetail(props.hotword.id, hours)
    detail.value = res.data
    chartTab.value = hours === 24 ? '24h' : '7d'
    renderCharts()
  } catch (e) {
    console.error('加载详情失败', e)
  }
}

const loadTrend = (hours) => {
  loadDetail(hours)
}

const renderCharts = () => {
  if (!detail.value) return
  
  // 趋势图
  if (trendChart.value && detail.value.voteTrend?.length) {
    if (!trendChartInstance) {
      trendChartInstance = echarts.init(trendChart.value)
    }
    trendChartInstance.setOption({
      title: { text: '热度趋势', textStyle: { color: '#fff', fontSize: 14 } },
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: detail.value.voteTrend.map(v => v.time), axisLabel: { color: '#aaa' } },
      yAxis: { type: 'value', axisLabel: { color: '#aaa' } },
      series: [{ data: detail.value.voteTrend.map(v => v.votes), type: 'line', smooth: true, areaStyle: { color: 'rgba(102,126,234,0.3)' }, lineStyle: { color: '#667eea' } }]
    })
  }
  
  // 饼图
  if (pieChart.value && detail.value.collegeDistribution) {
    const data = Object.entries(detail.value.collegeDistribution).map(([name, value]) => ({ name, value }))
    if (!pieChartInstance) {
      pieChartInstance = echarts.init(pieChart.value)
    }
    pieChartInstance.setOption({
      title: { text: '学院分布', textStyle: { color: '#fff', fontSize: 14 } },
      tooltip: { trigger: 'item' },
      series: [{ type: 'pie', radius: '60%', data, label: { color: '#fff' } }]
    })
  }
}

onMounted(() => {
  loadDetail(24)
  setTimeout(() => { isFlipped.value = true }, 100)
})
</script>

<style scoped>
.detail-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.85);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.detail-card {
  position: relative;
  width: 90%;
  max-width: 600px;
  max-height: 90vh;
  overflow-y: auto;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  border-radius: 20px;
  padding: 30px;
  transform: perspective(1000px) rotateY(-90deg);
  transition: transform 0.6s;
}

.detail-card.flipped {
  transform: perspective(1000px) rotateY(0);
}

.close-btn {
  position: absolute;
  top: 15px;
  right: 20px;
  background: none;
  border: none;
  color: #fff;
  font-size: 28px;
  cursor: pointer;
}

.hotword-title {
  text-align: center;
  font-size: 32px;
  margin-bottom: 16px;
}

.neon-text {
  color: #fff;
  text-shadow: 0 0 10px #ff6b6b, 0 0 20px #ff6b6b, 0 0 30px #ff6b6b;
}

.heat-badge {
  text-align: center;
  padding: 8px 20px;
  border-radius: 20px;
  display: inline-block;
  margin: 0 auto 20px;
  width: fit-content;
}

.heat-sprout { background: rgba(144, 238, 144, 0.3); color: #90ee90; }
.heat-warm { background: rgba(255, 165, 0, 0.3); color: #ffa500; }
.heat-hot { background: rgba(255, 69, 0, 0.3); color: #ff4500; }
.heat-viral { background: linear-gradient(135deg, rgba(255, 0, 128, 0.4), rgba(128, 0, 255, 0.4)); color: #ff69b4; }

.recommend {
  margin-left: 10px;
  background: linear-gradient(135deg, #ffd700, #ff8c00);
  color: #333;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
}

.vote-info {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 15px;
  margin-bottom: 24px;
}

.vote-count {
  color: #ff6b6b;
  font-size: 20px;
}

.vote-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 20px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  cursor: pointer;
  transition: transform 0.2s;
}

.vote-btn:hover { transform: scale(1.05); }
.vote-btn.strong { background: linear-gradient(135deg, #ff6b6b, #ff8c00); }

.section {
  margin-bottom: 20px;
  position: relative;
}

.section label {
  color: #888;
  font-size: 14px;
  margin-bottom: 8px;
  display: block;
}

.quote-mark {
  font-size: 48px;
  color: rgba(255,255,255,0.1);
  position: absolute;
  top: -10px;
  left: -10px;
}

.quote-mark.end {
  left: auto;
  right: -10px;
  bottom: -30px;
  top: auto;
}

.definition {
  color: #fff;
  font-size: 18px;
  line-height: 1.6;
  padding: 0 30px;
}

.example {
  color: #aaa;
  font-style: italic;
}

.tags {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.tag {
  padding: 6px 14px;
  background: rgba(102, 126, 234, 0.3);
  border-radius: 15px;
  color: #667eea;
  font-size: 14px;
}

.image-section img {
  width: 100%;
  max-height: 300px;
  object-fit: cover;
  border-radius: 12px;
  margin-bottom: 20px;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #888;
  font-size: 14px;
  padding-top: 16px;
  border-top: 1px solid rgba(255,255,255,0.1);
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
}

.time { margin-left: auto; }

.charts-section {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid rgba(255,255,255,0.1);
}

.chart-tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

.chart-tabs button {
  padding: 6px 16px;
  border: 1px solid #555;
  background: transparent;
  color: #aaa;
  border-radius: 15px;
  cursor: pointer;
}

.chart-tabs button.active {
  background: #667eea;
  border-color: #667eea;
  color: #fff;
}

.chart {
  height: 200px;
  margin-bottom: 20px;
}

.chart.pie {
  height: 250px;
}
</style>
