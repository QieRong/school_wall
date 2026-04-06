<template>
  <div class="hotword-wall">
    <!-- 顶部导航栏 -->
    <header class="top-header">
      <button class="back-btn" @click="router.push('/home')">
        <span>←</span> 返回首页
      </button>
      <h1 class="page-title">🔥 校园热词墙</h1>
      <div class="header-spacer"></div>
    </header>

    <!-- 主内容区 -->
    <div class="wall-container">
      <!-- 使用提示 -->
      <Transition name="hint-fade">
        <div v-if="showHint" class="usage-hint">
          <div class="hint-content">
            <span class="hint-icon">💡</span>
            <span class="hint-text">点击气泡查看详情，点击🔥按钮投票支持！长按🔥投2票表示强烈认同</span>
          </div>
        </div>
      </Transition>

      <!-- 左侧热词气泡区 -->
      <div class="bubble-area" ref="bubbleArea">
        <!-- 空状态提示 -->
        <div v-if="hotwords.length === 0" class="empty-bubble-state">
          <div class="empty-icon">🎈</div>
          <div class="empty-text">暂无热词气泡</div>
          <div class="empty-hint">数据库中还没有热词数据</div>
          <button class="empty-action-btn" @click="showPostModal = true">
            ✏️ 立即投稿第一个热词
          </button>
        </div>

        <!-- 气泡列表 -->
        <HotwordBubble v-for="hotword in hotwords" :key="hotword.id" :hotword="hotword" :style="getBubbleStyle(hotword)"
          @click="showDetail(hotword)" @vote="handleVote" />
      </div>

      <!-- 右侧榜单 -->
      <div class="ranking-panel">
        <HotwordRanking :rankings="rankings" :activeTab="rankingType" @tabChange="handleRankingTabChange"
          @itemClick="focusBubble" />
      </div>
    </div>

    <!-- 底部固定栏 -->
    <div class="bottom-bar">
      <div class="quota-info">
        <span class="fire-icon">🔥</span>
        <span>今日剩余 {{ remainingQuota }} 票</span>
      </div>
      <button class="post-btn" @click="showPostModal = true">
        <span>✏️</span> 投稿热词
      </button>
      <div class="my-menu">
        <button class="my-btn" @click="showMyMenu = !showMyMenu">我的 ▼</button>
        <div v-if="showMyMenu" class="dropdown-menu">
          <div @click="goToMyPosts">我的投稿</div>
          <div @click="goToMyVotes">我的投票</div>
          <div @click="goToMuseum">校园博物馆</div>
        </div>
      </div>
    </div>

    <!-- 投稿弹窗 -->
    <HotwordPostModal v-if="showPostModal" @close="showPostModal = false" @success="handlePostSuccess" />

    <!-- 详情弹窗 -->
    <HotwordDetail v-if="selectedHotword" :hotword="selectedHotword" @close="selectedHotword = null"
      @vote="handleVote" />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { getHotwordList, getRanking, getRemainingQuota, voteHotword } from '@/api/hotword'
import HotwordBubble from '@/components/HotwordBubble.vue'
import HotwordRanking from '@/components/HotwordRanking.vue'
import HotwordPostModal from '@/components/HotwordPostModal.vue'
import HotwordDetail from '@/components/HotwordDetail.vue'

const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()
const userId = computed(() => userStore.user?.id)

const hotwords = ref([])
const rankings = ref([])
const remainingQuota = ref(5)
const rankingType = ref('day')
const showPostModal = ref(false)
const showMyMenu = ref(false)
const selectedHotword = ref(null)
const bubbleArea = ref(null)
const showHint = ref(true)

// 榜单请求版本号，用于防止快速切换时的竞态
let rankingRequestId = 0

// 气泡位置缓存
const bubblePositions = ref(new Map())

// 网格布局算法 - 避免重叠
const calculateLayout = (items) => {
  const gridSize = 6
  const usedSlots = new Set()
  const map = new Map()
  
  const random = (seed) => {
    const x = Math.sin(seed++) * 10000;
    return x - Math.floor(x);
  }

  items.forEach((item) => {
    let slot = Math.floor(random(item.id) * (gridSize * gridSize))
    let retries = 0
    while (usedSlots.has(slot) && retries < 50) {
      slot = (slot + 1) % (gridSize * gridSize)
      retries++
    }
    usedSlots.add(slot)
    
    const row = Math.floor(slot / gridSize)
    const col = slot % gridSize
    
    const cellW = 100 / gridSize
    const cellH = 100 / gridSize
    
    // 居中偏移 + 随机扰动 (±3%)
    const jitterX = (random(item.id + 1) - 0.5) * 6
    const jitterY = (random(item.id + 2) - 0.5) * 6
    
    // 🔧 计算位置并限制在 10%-90% 的安全区域
    const left = Math.max(10, Math.min(90, col * cellW + (cellW / 2) + jitterX))
    const top = Math.max(10, Math.min(90, row * cellH + (cellH / 2) + jitterY))
    
    map.set(item.id, { left, top })
  })
  
  bubblePositions.value = map
}

// 加载数据
const loadData = async () => {
  try {
    console.log('开始加载热词数据...')
    const [listRes, rankRes] = await Promise.all([
      getHotwordList(1),
      getRanking(rankingType.value)
    ])
    console.log('热词列表完整响应:', JSON.stringify(listRes, null, 2))
    console.log('榜单完整响应:', JSON.stringify(rankRes, null, 2))

    // 确保数据格式正确
    const rawData = listRes.data || []
    console.log('原始热词数据:', rawData)

    // 前端兜底：如果后端没有返回heatLevel，在前端计算
    hotwords.value = rawData.map(hw => {
      if (!hw.heatLevel) {
        const votes = hw.totalVotes || 0
        if (votes <= 10) hw.heatLevel = '新芽'
        else if (votes <= 50) hw.heatLevel = '升温'
        else if (votes <= 200) hw.heatLevel = '火爆'
        else hw.heatLevel = '现象级'
        console.warn(`热词 ${hw.name} 缺少heatLevel字段，前端已补充为: ${hw.heatLevel}`)
      }
      return hw
    })
    
    calculateLayout(hotwords.value)

    rankings.value = rankRes.data || []

    console.log('处理后的热词数量:', hotwords.value.length)
    console.log('处理后的热词详情:', hotwords.value)
    console.log('榜单数量:', rankings.value.length)

    if (userId.value) {
      const quotaRes = await getRemainingQuota(userId.value)
      remainingQuota.value = quotaRes.data ?? 5
    }
  } catch (e) {
    console.error('加载热词数据失败:', e)
    console.error('错误详情:', e.response?.data || e.message)
    appStore.showToast('加载热词数据失败，请刷新重试', 'error')
  }
}

// 气泡定位算法：根据热度等级分配尺寸和位置
const getBubbleStyle = (hotword) => {
  const baseSize = 60
  const sizeMap = { '新芽': 1, '升温': 1.5, '火爆': 2.0, '现象级': 2.8 }
  const scale = sizeMap[hotword.heatLevel] || 1
  const size = baseSize * scale

  const pos = bubblePositions.value.get(hotword.id) || { left: 50, top: 50 }

  // 🔧 根据气泡尺寸动态计算安全边界
  // 假设容器宽度为 1000px（可根据实际调整），气泡尺寸占比
  const containerWidth = 1000  // 估算的容器宽度
  const bubbleSizePercent = (size / containerWidth) * 100  // 气泡尺寸占容器的百分比
  
  // 最小边距：气泡半径 + 额外缓冲 2%
  const minMargin = (bubbleSizePercent / 2) + 2
  const maxMargin = 100 - minMargin

  return {
    width: `${size}px`,
    height: `${size}px`,
    left: `${Math.max(minMargin, Math.min(maxMargin, pos.left))}%`,
    top: `${Math.max(minMargin, Math.min(maxMargin, pos.top))}%`,
    animationDelay: `${(hotword.id % 5) * 0.5}s`
  }
}

//投票动画状态
const votingHotwordId = ref(null)

// 投票处理（优化版 - 添加二次确认）
const handleVote = async (hotwordId, count = 1) => {
  if (!userId.value) {
    appStore.showToast('请先登录', 'warning')
    return
  }

  //投票前获取最新配额，防止多标签页同时投票
  try {
    const quotaRes = await getRemainingQuota(userId.value)
    const latestQuota = quotaRes.data ?? 5

    // 更新本地配额
    remainingQuota.value = latestQuota

    if (latestQuota < count) {
      appStore.showToast(`今日投票已用完（剩余${latestQuota}票），明天再来吧~ 🌙`, 'warning')
      return
    }

    //多票投票时显示确认提示
    if (count > 1) {
      const confirmed = await appStore.showConfirm({
        title: '确认投票',
        message: `确定要投 ${count} 票吗？（剩余 ${latestQuota} 票）`,
        confirmText: '确认投票',
        cancelText: '取消',
        type: 'warning'
      })
      if (!confirmed) return
    }

  } catch (e) {
    console.error('获取投票配额失败', e)
    // 降级到本地配额检查
    if (remainingQuota.value < count) {
      appStore.showToast('今日投票已用完，明天再来吧~', 'warning')
      return
    }
  }

  //触发投票动画
  votingHotwordId.value = hotwordId

  // 执行投票
  try {
    const res = await voteHotword(hotwordId, userId.value, count)
    if (res.data) {
      remainingQuota.value = res.data.remainingQuota

      // 更新本地数据
      const hw = hotwords.value.find(h => h.id === hotwordId)
      if (hw) {
        hw.totalVotes = res.data.totalVotes
        hw.heatLevel = res.data.heatLevel
      }

      //延迟清除动画状态
      setTimeout(() => {
        votingHotwordId.value = null
      }, 600)

      appStore.showToast(`投票成功！剩余 ${res.data.remainingQuota} 票 🔥`, 'success')
      loadData() // 刷新榜单
    }
  } catch (e) {
    votingHotwordId.value = null
    const errorMsg = e.response?.data?.message || '投票失败'
    appStore.showToast(errorMsg, 'error')

    // 投票失败后重新获取配额
    try {
      const quotaRes = await getRemainingQuota(userId.value)
      remainingQuota.value = quotaRes.data ?? 5
    } catch { }
  }
}

// 榜单切换
const handleRankingTabChange = async (type) => {
  rankingType.value = type
  const currentRequestId = ++rankingRequestId
  const res = await getRanking(type)
  // 只有最新请求的结果才更新数据，丢弃过时响应
  if (currentRequestId === rankingRequestId) {
    rankings.value = res.data || []
  }
}

// 聚焦气泡
const focusBubble = (hotwordId) => {
  const hw = hotwords.value.find(h => h.id === hotwordId)
  if (hw) selectedHotword.value = hw
}

// 显示详情
const showDetail = (hotword) => {
  selectedHotword.value = hotword
}

// 投稿成功
const handlePostSuccess = () => {
  showPostModal.value = false
  loadData()
}

// 导航
const goToMyPosts = () => { showMyMenu.value = false; router.push('/hotword/my-posts') }
const goToMyVotes = () => { showMyMenu.value = false; router.push('/hotword/my-votes') }
const goToMuseum = () => { showMyMenu.value = false; router.push('/hotword/museum') }

// WebSocket 监听
let ws = null
const setupWebSocket = () => {
  const token = localStorage.getItem('token')
  if (!token) return

  let apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:19090'
  if (apiUrl.endsWith('/')) apiUrl = apiUrl.slice(0, -1)
  
  let wsUrl = apiUrl.replace(/^http/, 'ws')
  wsUrl = `${wsUrl}/ws/${token}`
  
  ws = new WebSocket(wsUrl)

  ws.onmessage = (event) => {
    try {
      const msg = JSON.parse(event.data)
      if (msg.type === 'HOTWORD_VOTE_UPDATE') {
        const hw = hotwords.value.find(h => h.id === msg.hotwordId)
        if (hw) {
          hw.totalVotes = msg.totalVotes
          hw.heatLevel = msg.heatLevel
        }
        // 实时刷新榜单
        getRanking(rankingType.value).then(res => {
          rankings.value = res.data || []
        })
      } else if (msg.type === 'HOTWORD_NEW') {
        loadData()
      }
    } catch (e) { }
  }
}

onMounted(() => {
  loadData()
  setupWebSocket()
  // 4秒后自动隐藏使用提示
  setTimeout(() => { showHint.value = false }, 4000)
})

onUnmounted(() => {
  if (ws) ws.close()
})
</script>

<style scoped>
.hotword-wall {
  min-height: 100vh;
  background: linear-gradient(135deg, #059669 0%, #10b981 30%, #34d399 70%, #6ee7b7 100%);
  position: relative;
  overflow: hidden;
}

/* 全局滚动条样式 */
.hotword-wall ::-webkit-scrollbar {
  width: 10px;
  height: 10px;
}

.hotword-wall ::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 10px;
}

.hotword-wall ::-webkit-scrollbar-thumb {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.6), rgba(52, 211, 153, 0.8));
  border-radius: 10px;
  transition: all 0.3s;
}

.hotword-wall ::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.8), rgba(52, 211, 153, 1));
  box-shadow: 0 0 10px rgba(16, 185, 129, 0.5);
}

.top-header {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: rgba(255, 255, 255, 0.2);
  border: none;
  border-radius: 25px;
  color: white;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.back-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: translateX(-4px);
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: white;
  text-shadow: 2px 2px 8px rgba(0, 0, 0, 0.3);
  margin: 0;
}

.header-spacer {
  width: 100px;
}

.wall-container {
  display: flex;
  height: calc(100vh - 140px);
  padding: 20px;
  position: relative;
}

.usage-hint {
  position: absolute;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 50;
  animation: slideDown 0.5s ease-out;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateX(-50%) translateY(-20px);
  }

  to {
    opacity: 1;
    transform: translateX(-50%) translateY(0);
  }
}

.hint-content {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  padding: 12px 24px;
  border-radius: 25px;
  display: flex;
  align-items: center;
  gap: 10px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  border: 2px solid rgba(16, 185, 129, 0.3);
  animation: pulse-border 2s ease-in-out infinite;
}

@keyframes pulse-border {

  0%,
  100% {
    border-color: rgba(16, 185, 129, 0.3);
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  }

  50% {
    border-color: rgba(16, 185, 129, 0.6);
    box-shadow: 0 4px 25px rgba(16, 185, 129, 0.3);
  }
}

.hint-icon {
  font-size: 20px;
  animation: bounce 2s ease-in-out infinite;
}

@keyframes bounce {

  0%,
  100% {
    transform: translateY(0);
  }

  50% {
    transform: translateY(-5px);
  }
}

.hint-text {
  color: rgb(33, 111, 85);
  font-size: 14px;
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(255, 255, 255, 0.8);
}

.bubble-area {
  flex: 7;
  position: relative;
  overflow: hidden;
  margin-top: 60px;
  min-height: 500px;
}

.empty-bubble-state {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  color: white;
}

.empty-icon {
  font-size: 80px;
  margin-bottom: 20px;
  animation: float 3s ease-in-out infinite;
}

.empty-text {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 10px;
  text-shadow: 2px 2px 8px rgba(0, 0, 0, 0.3);
}

.empty-hint {
  font-size: 16px;
  opacity: 0.8;
  text-shadow: 1px 1px 4px rgba(0, 0, 0, 0.3);
  margin-bottom: 30px;
}

.empty-action-btn {
  background: linear-gradient(135deg, rgb(33, 111, 85) 0%, #10b981 100%);
  color: white;
  border: none;
  padding: 14px 32px;
  border-radius: 30px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(16, 185, 129, 0.4);
}

.empty-action-btn:hover {
  transform: translateY(-2px) scale(1.05);
  box-shadow: 0 8px 25px rgba(16, 185, 129, 0.6);
}

.empty-action-btn:active {
  transform: translateY(0) scale(1);
}

/* 如果气泡区域有滚动，添加自定义滚动条样式 */
.bubble-area::-webkit-scrollbar {
  width: 10px;
  height: 10px;
}

.bubble-area::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 10px;
}

.bubble-area::-webkit-scrollbar-thumb {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.3), rgba(255, 255, 255, 0.5));
  border-radius: 10px;
  transition: all 0.3s;
}

.bubble-area::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.5), rgba(255, 255, 255, 0.7));
}

.ranking-panel {
  flex: 3;
  margin-left: 20px;
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 70px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
  padding: 0 24px;
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.1);
  border-top: 1px solid rgba(34, 197, 94, 0.2);
}

.quota-info {
  color: rgb(5, 150, 105);
  font-size: 15px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: linear-gradient(135deg, #f0fdf4 0%, #dcfce7 100%);
  border-radius: 25px;
  border: 1px solid rgba(34, 197, 94, 0.3);
}

.fire-icon {
  font-size: 20px;
  animation: flicker 1s infinite;
}

@keyframes flicker {

  0%,
  100% {
    opacity: 1;
    transform: scale(1);
  }

  50% {
    opacity: 0.8;
    transform: scale(1.1);
  }
}

.post-btn {
  background: linear-gradient(135deg, rgb(33, 111, 85) 0%, #10b981 100%);
  color: white;
  border: none;
  padding: 14px 32px;
  border-radius: 30px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(16, 185, 129, 0.4);
}

.post-btn:hover {
  transform: translateY(-2px) scale(1.02);
  box-shadow: 0 8px 25px rgba(16, 185, 129, 0.5);
}

.my-menu {
  position: relative;
}

.my-btn {
  background: white;
  color: rgb(33, 111, 85);
  border: 2px solid rgb(33, 111, 85);
  padding: 12px 24px;
  border-radius: 25px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.my-btn:hover {
  background: rgb(33, 111, 85);
  color: white;
}

.dropdown-menu {
  position: absolute;
  bottom: 100%;
  right: 0;
  background: white;
  border-radius: 16px;
  overflow: hidden;
  margin-bottom: 12px;
  min-width: 150px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
  border: 1px solid rgba(34, 197, 94, 0.2);
}

.dropdown-menu div {
  padding: 14px 20px;
  color: #333;
  cursor: pointer;
  transition: all 0.2s;
  font-weight: 500;
}

.dropdown-menu div:hover {
  background: linear-gradient(135deg, #f0fdf4 0%, #dcfce7 100%);
  color: rgb(33, 111, 85);
}

/* 提示条淡出动画 */
.hint-fade-leave-active {
  transition: all 0.8s ease;
}
.hint-fade-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(-20px);
}
</style>
