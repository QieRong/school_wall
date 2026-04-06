<template>
  <div class="museum-page">
    <div class="page-header">
      <button class="back-btn" @click="$router.back()">← 返回</button>
      <h1>🏛️ 校园历史博物馆</h1>
      <p class="subtitle">那些曾经火过的梗...</p>
    </div>
    
    <div class="museum-grid">
      <div v-for="item in hotwords" :key="item.id" class="museum-item">
        <div class="newspaper-clip">
          <div class="clip-header">
            <span class="clip-date">{{ formatDate(item.createTime) }}</span>
          </div>
          <h3 class="clip-title">{{ item.name }}</h3>
          <p class="clip-content">{{ item.definition }}</p>
          <div class="clip-footer">
            <span>曾获 {{ item.totalVotes }} 票</span>
            <span class="heat-level">{{ item.heatLevel }}</span>
          </div>
        </div>
      </div>
      
      <div v-if="!hotwords.length" class="empty">
        <p>博物馆暂时空空如也</p>
        <p>30天无人投票的热词会被收藏到这里</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getHotwordList } from '@/api/hotword'

const hotwords = ref([])

const formatDate = (time) => {
  if (!time) return ''
  const d = new Date(time)
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
}

const loadData = async () => {
  const res = await getHotwordList(2) // status=2 归档
  hotwords.value = res.data || []
}

onMounted(() => loadData())
</script>

<style scoped>
.museum-page {
  min-height: 100vh;
  background: #f5f0e1;
  background-image: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23d4c4a8' fill-opacity='0.4'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
  padding: 20px;
}

.page-header {
  text-align: center;
  margin-bottom: 40px;
}

.back-btn {
  position: absolute;
  left: 20px;
  top: 20px;
  background: #8b4513;
  border: none;
  color: #f5f0e1;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
}

h1 {
  color: #3d2914;
  font-family: 'Times New Roman', serif;
  font-size: 36px;
  margin-bottom: 8px;
}

.subtitle {
  color: #8b7355;
  font-style: italic;
}

.museum-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.museum-item {
  transform: rotate(calc(-2deg + 4deg * var(--i, 0)));
}

.museum-item:nth-child(odd) { --i: 1; }
.museum-item:nth-child(even) { --i: -1; }

.newspaper-clip {
  background: #fffef5;
  border: 1px solid #d4c4a8;
  padding: 20px;
  box-shadow: 3px 3px 10px rgba(0,0,0,0.1);
  position: relative;
}

.newspaper-clip::before {
  content: '';
  position: absolute;
  top: -10px;
  left: 50%;
  transform: translateX(-50%);
  width: 30px;
  height: 20px;
  background: #8b4513;
  clip-path: polygon(50% 0%, 0% 100%, 100% 100%);
}

.clip-header {
  border-bottom: 1px solid #d4c4a8;
  padding-bottom: 8px;
  margin-bottom: 12px;
}

.clip-date {
  font-family: 'Times New Roman', serif;
  color: #8b7355;
  font-size: 12px;
}

.clip-title {
  font-family: 'SimSun', serif;
  font-size: 24px;
  color: #3d2914;
  margin-bottom: 12px;
}

.clip-content {
  font-family: 'SimSun', serif;
  color: #5d4e37;
  line-height: 1.8;
  font-size: 14px;
}

.clip-footer {
  display: flex;
  justify-content: space-between;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px dashed #d4c4a8;
  font-size: 12px;
  color: #8b7355;
}

.heat-level {
  background: #8b4513;
  color: #f5f0e1;
  padding: 2px 8px;
  border-radius: 4px;
}

.empty {
  grid-column: 1 / -1;
  text-align: center;
  color: #8b7355;
  padding: 60px;
  font-style: italic;
}
</style>
