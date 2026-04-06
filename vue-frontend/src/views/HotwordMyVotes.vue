<template>
  <div class="my-votes-page">
    <div class="page-header">
      <button class="back-btn" @click="$router.back()">← 返回</button>
      <h1>我的投票</h1>
    </div>
    
    <div class="votes-list">
      <div v-for="item in votes" :key="item.id" class="vote-item" @click="goToDetail(item.hotwordId)">
        <div class="vote-info">
          <span class="hotword-name">{{ item.hotwordName }}</span>
          <span class="vote-count">投了 {{ item.voteCount }} 票</span>
        </div>
        <span class="vote-time">{{ formatTime(item.createTime) }}</span>
      </div>
      
      <div v-if="!votes.length" class="empty">暂无投票记录</div>
    </div>
    
    <div v-if="total > pageSize" class="pagination">
      <button :disabled="pageNum <= 1" @click="loadData(pageNum - 1)">上一页</button>
      <span>{{ pageNum }} / {{ Math.ceil(total / pageSize) }}</span>
      <button :disabled="pageNum >= Math.ceil(total / pageSize)" @click="loadData(pageNum + 1)">下一页</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getMyVotes } from '@/api/hotword'

const router = useRouter()
const userStore = useUserStore()
const votes = ref([])
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

const loadData = async (page = 1) => {
  pageNum.value = page
  const res = await getMyVotes(userStore.user?.id, page, pageSize.value)
  votes.value = res.data?.list || []
  total.value = res.data?.total || 0
}

const goToDetail = (hotwordId) => {
  router.push(`/hotword?highlight=${hotwordId}`)
}

onMounted(() => loadData())
</script>

<style scoped>
.my-votes-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  padding: 20px;
  color: #fff;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 30px;
}

.back-btn {
  background: none;
  border: 1px solid #555;
  color: #aaa;
  padding: 8px 16px;
  border-radius: 20px;
  cursor: pointer;
}

.votes-list {
  max-width: 800px;
  margin: 0 auto;
}

.vote-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: rgba(255,255,255,0.05);
  border-radius: 12px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: background 0.3s;
}

.vote-item:hover {
  background: rgba(255,255,255,0.1);
}

.vote-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hotword-name {
  font-size: 16px;
  font-weight: bold;
}

.vote-count {
  color: #ff6b6b;
  font-size: 14px;
}

.vote-time {
  color: #666;
  font-size: 14px;
}

.empty {
  text-align: center;
  color: #666;
  padding: 60px;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 20px;
  margin-top: 30px;
}

.pagination button {
  padding: 8px 16px;
  background: rgba(255,255,255,0.1);
  border: none;
  color: #fff;
  border-radius: 8px;
  cursor: pointer;
}

.pagination button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
