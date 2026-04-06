<template>
  <div class="my-posts-page">
    <div class="page-header">
      <button class="back-btn" @click="$router.back()">← 返回</button>
      <h1>我的投稿</h1>
    </div>

    <div class="posts-list">
      <div v-for="item in posts" :key="item.id" class="post-item">
        <div class="post-info">
          <span class="post-name">{{ item.name }}</span>
          <span class="heat-badge" :class="getHeatClass(item.heatLevel)">{{ item.heatLevel }}</span>
          <span class="post-votes">{{ item.totalVotes }}票</span>
          <span class="post-status" :class="{ archived: item.status === 2 }">
            {{ item.status === 1 ? '活跃' : '已归档' }}
          </span>
        </div>
        <div class="post-actions">
          <button class="delete-btn" @click="handleDelete(item)">删除</button>
        </div>
      </div>

      <div v-if="!posts.length" class="empty">暂无投稿</div>
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
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { getMyPosts, deleteHotword } from '@/api/hotword'

const userStore = useUserStore()
const appStore = useAppStore()
const posts = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const getHeatClass = (level) => {
  const map = { '新芽': 'heat-sprout', '升温': 'heat-warm', '火爆': 'heat-hot', '现象级': 'heat-viral' }
  return map[level] || ''
}

const loadData = async (page = 1) => {
  pageNum.value = page
  const res = await getMyPosts(userStore.user?.id, page, pageSize.value)
  posts.value = res.data?.list || []
  total.value = res.data?.total || 0
}

const handleDelete = async (item) => {
  const confirmed = await appStore.showConfirm({
    title: '删除热词',
    message: `确定要删除热词"${item.name}"吗？删除后无法恢复。`,
    confirmText: '确认删除',
    cancelText: '取消',
    type: 'danger'
  })
  if (!confirmed) return
  try {
    await deleteHotword(item.id, userStore.user?.id)
    loadData(pageNum.value)
  } catch (e) {
    appStore.showToast('删除失败', 'error')
  }
}

onMounted(() => loadData())
</script>

<style scoped>
.my-posts-page {
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

.posts-list {
  max-width: 800px;
  margin: 0 auto;
}

.post-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  margin-bottom: 12px;
}

.post-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.post-name {
  font-size: 18px;
  font-weight: bold;
}

.heat-badge {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
}

.heat-sprout {
  background: rgba(144, 238, 144, 0.3);
  color: #90ee90;
}

.heat-warm {
  background: rgba(255, 165, 0, 0.3);
  color: #ffa500;
}

.heat-hot {
  background: rgba(255, 69, 0, 0.3);
  color: #ff4500;
}

.heat-viral {
  background: rgba(255, 0, 128, 0.3);
  color: #ff69b4;
}

.post-votes {
  color: #888;
}

.post-status {
  color: #4ade80;
}

.post-status.archived {
  color: #888;
}

.delete-btn {
  background: rgba(255, 100, 100, 0.2);
  border: 1px solid #ff6b6b;
  color: #ff6b6b;
  padding: 6px 16px;
  border-radius: 15px;
  cursor: pointer;
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
  background: rgba(255, 255, 255, 0.1);
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
