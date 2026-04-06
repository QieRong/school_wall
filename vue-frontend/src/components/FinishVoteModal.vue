<script setup>
import { ref, computed, watch, onUnmounted } from 'vue'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Progress } from '@/components/ui/progress'
import { 
  Vote, Clock, CheckCircle, XCircle, AlertCircle,
  ThumbsUp, ThumbsDown, Trophy, Users
} from 'lucide-vue-next'

const props = defineProps({
  open: {
    type: Boolean,
    default: false
  },
  voteInfo: {
    type: Object,
    default: null
  },
  loading: {
    type: Boolean,
    default: false
  },
  hasVoted: {
    type: Boolean,
    default: false
  },
  myVote: {
    type: Boolean,
    default: null
  }
})

const emit = defineEmits(['update:open', 'vote', 'initiate'])

// 倒计时
const remainingTime = ref('')
let countdownTimer = null

// 计算投票进度
const agreePercent = computed(() => {
  if (!props.voteInfo) return 0
  const total = props.voteInfo.agreeCount + props.voteInfo.disagreeCount
  if (total === 0) return 0
  return Math.round((props.voteInfo.agreeCount / total) * 100)
})

const disagreePercent = computed(() => {
  return 100 - agreePercent.value
})

// 是否达到通过条件
const isPassing = computed(() => {
  if (!props.voteInfo) return false
  const total = props.voteInfo.totalVoters
  return props.voteInfo.agreeCount > total / 2
})

// 投票状态文本
const statusText = computed(() => {
  if (!props.voteInfo) return ''
  switch (props.voteInfo.status) {
    case 1: return '投票进行中'
    case 2: return '投票已通过'
    case 3: return '投票已否决'
    case 4: return '投票已过期'
    default: return ''
  }
})

// 状态颜色
const statusColor = computed(() => {
  if (!props.voteInfo) return 'gray'
  switch (props.voteInfo.status) {
    case 1: return 'amber'
    case 2: return 'green'
    case 3: return 'red'
    case 4: return 'gray'
    default: return 'gray'
  }
})

// 更新倒计时
const updateCountdown = () => {
  if (!props.voteInfo?.expireTime) {
    remainingTime.value = ''
    return
  }
  
  const now = new Date().getTime()
  const expire = new Date(props.voteInfo.expireTime).getTime()
  const diff = expire - now
  
  if (diff <= 0) {
    remainingTime.value = '已结束'
    return
  }
  
  const hours = Math.floor(diff / (1000 * 60 * 60))
  const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
  const seconds = Math.floor((diff % (1000 * 60)) / 1000)
  
  if (hours > 0) {
    remainingTime.value = `${hours}小时${minutes}分钟`
  } else if (minutes > 0) {
    remainingTime.value = `${minutes}分${seconds}秒`
  } else {
    remainingTime.value = `${seconds}秒`
  }
}

// 关闭弹窗
const handleClose = () => {
  emit('update:open', false)
}

// 投票
const handleVote = (agree) => {
  emit('vote', agree)
}

// 发起投票
const handleInitiate = () => {
  emit('initiate')
}

// 监听弹窗打开
watch(() => props.open, (val) => {
  if (val) {
    updateCountdown()
    countdownTimer = setInterval(updateCountdown, 1000)
  } else {
    if (countdownTimer) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
  }
})

onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
})
</script>

<template>
  <Dialog :open="open" @update:open="handleClose">
    <DialogContent class="sm:max-w-md vote-dialog">
      <DialogHeader>
        <DialogTitle class="flex items-center gap-2 text-amber-800 font-serif">
          <Vote class="w-5 h-5" /> 完结投票
        </DialogTitle>
      </DialogHeader>

      <!-- 无投票时 - 发起投票 -->
      <div v-if="!voteInfo" class="no-vote-state">
        <div class="icon-wrapper">
          <Trophy class="w-16 h-16 text-amber-400" />
        </div>
        <h3 class="title">发起完结投票</h3>
        <p class="description">
          您可以发起完结投票，让参与者共同决定故事是否结束。
          投票将持续24小时，超过50%同意即可完结。
        </p>
        <div class="requirement-hint">
          <AlertCircle class="w-4 h-4" />
          <span>仅贡献度前10的用户可发起投票</span>
        </div>
      </div>

      <!-- 有投票时 - 显示投票状态 -->
      <div v-else class="vote-state">
        <!-- 状态徽章 -->
        <div class="status-header">
          <Badge :class="`status-badge-${statusColor}`">
            <CheckCircle v-if="voteInfo.status === 2" class="w-3 h-3 mr-1" />
            <XCircle v-else-if="voteInfo.status === 3" class="w-3 h-3 mr-1" />
            <Clock v-else class="w-3 h-3 mr-1" />
            {{ statusText }}
          </Badge>
          <div v-if="voteInfo.status === 1" class="countdown">
            <Clock class="w-4 h-4" />
            <span>剩余 {{ remainingTime }}</span>
          </div>
        </div>

        <!-- 投票进度 -->
        <div class="vote-progress">
          <div class="progress-header">
            <span class="agree-label">
              <ThumbsUp class="w-4 h-4" /> 同意 {{ voteInfo.agreeCount }}
            </span>
            <span class="disagree-label">
              反对 {{ voteInfo.disagreeCount }} <ThumbsDown class="w-4 h-4" />
            </span>
          </div>
          <div class="progress-bar">
            <div class="agree-bar" :style="{ width: agreePercent + '%' }"></div>
            <div class="disagree-bar" :style="{ width: disagreePercent + '%' }"></div>
          </div>
          <div class="progress-footer">
            <span>{{ agreePercent }}%</span>
            <span class="total-voters">
              <Users class="w-3 h-3" /> 共 {{ voteInfo.totalVoters }} 人可投票
            </span>
            <span>{{ disagreePercent }}%</span>
          </div>
        </div>

        <!-- 通过提示 -->
        <div v-if="isPassing && voteInfo.status === 1" class="passing-hint">
          <CheckCircle class="w-4 h-4" />
          <span>已达到通过条件，投票结束后故事将完结</span>
        </div>

        <!-- 已投票状态 -->
        <div v-if="hasVoted" class="voted-state">
          <div class="voted-icon" :class="myVote ? 'agree' : 'disagree'">
            <ThumbsUp v-if="myVote" class="w-5 h-5" />
            <ThumbsDown v-else class="w-5 h-5" />
          </div>
          <span>您已投票：{{ myVote ? '同意' : '反对' }}</span>
        </div>

        <!-- 投票按钮 -->
        <div v-if="voteInfo.status === 1 && !hasVoted" class="vote-buttons">
          <Button 
            class="agree-btn" 
            :disabled="loading"
            @click="handleVote(true)"
          >
            <ThumbsUp class="w-5 h-5 mr-2" />
            同意完结
          </Button>
          <Button 
            class="disagree-btn" 
            :disabled="loading"
            @click="handleVote(false)"
          >
            <ThumbsDown class="w-5 h-5 mr-2" />
            反对完结
          </Button>
        </div>
      </div>

      <DialogFooter>
        <Button v-if="!voteInfo" variant="outline" @click="handleClose">
          取消
        </Button>
        <Button 
          v-if="!voteInfo" 
          class="initiate-btn" 
          :disabled="loading"
          @click="handleInitiate"
        >
          <Vote class="w-4 h-4 mr-2" />
          发起投票
        </Button>
        <Button v-else variant="outline" @click="handleClose">
          关闭
        </Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>

<style scoped>
.vote-dialog {
  background: linear-gradient(135deg, #fefce8 0%, #fef9c3 100%);
}

/* 无投票状态 */
.no-vote-state {
  text-align: center;
  padding: 24px 16px;
}

.icon-wrapper {
  margin-bottom: 16px;
}

.no-vote-state .title {
  font-size: 18px;
  font-weight: 600;
  color: #92400e;
  margin-bottom: 8px;
}

.no-vote-state .description {
  font-size: 14px;
  color: #78716c;
  line-height: 1.6;
  margin-bottom: 16px;
}

.requirement-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 13px;
  color: #d97706;
  padding: 8px 16px;
  background: rgba(217, 119, 6, 0.1);
  border-radius: 8px;
}

/* 投票状态 */
.vote-state {
  padding: 16px 0;
}

.status-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.status-badge-amber {
  background: rgba(217, 119, 6, 0.2);
  color: #b45309;
}

.status-badge-green {
  background: rgba(34, 197, 94, 0.2);
  color: #15803d;
}

.status-badge-red {
  background: rgba(239, 68, 68, 0.2);
  color: #dc2626;
}

.status-badge-gray {
  background: rgba(107, 114, 128, 0.2);
  color: #4b5563;
}

.countdown {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #d97706;
}

/* 投票进度 */
.vote-progress {
  margin-bottom: 20px;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.agree-label {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  font-weight: 600;
  color: #16a34a;
}

.disagree-label {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  font-weight: 600;
  color: #dc2626;
}

.progress-bar {
  display: flex;
  height: 12px;
  border-radius: 6px;
  overflow: hidden;
  background: #e5e7eb;
}

.agree-bar {
  background: linear-gradient(90deg, #22c55e 0%, #16a34a 100%);
  transition: width 0.3s ease;
}

.disagree-bar {
  background: linear-gradient(90deg, #ef4444 0%, #dc2626 100%);
  transition: width 0.3s ease;
}

.progress-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
  font-size: 12px;
  color: #78716c;
}

.total-voters {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 通过提示 */
.passing-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 12px;
  background: rgba(34, 197, 94, 0.1);
  border: 1px solid rgba(34, 197, 94, 0.3);
  border-radius: 8px;
  color: #15803d;
  font-size: 13px;
  margin-bottom: 16px;
}

/* 已投票状态 */
.voted-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  background: rgba(217, 119, 6, 0.1);
  border-radius: 8px;
  color: #92400e;
  font-size: 14px;
}

.voted-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.voted-icon.agree {
  background: rgba(34, 197, 94, 0.2);
  color: #16a34a;
}

.voted-icon.disagree {
  background: rgba(239, 68, 68, 0.2);
  color: #dc2626;
}

/* 投票按钮 */
.vote-buttons {
  display: flex;
  gap: 12px;
}

.agree-btn {
  flex: 1;
  background: linear-gradient(135deg, #22c55e 0%, #16a34a 100%);
  color: white;
  border: none;
}

.agree-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #16a34a 0%, #15803d 100%);
}

.disagree-btn {
  flex: 1;
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: white;
  border: none;
}

.disagree-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #dc2626 0%, #b91c1c 100%);
}

/* 发起投票按钮 */
.initiate-btn {
  background: linear-gradient(135deg, #d97706 0%, #b45309 100%);
  color: white;
  border: none;
}

.initiate-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #b45309 0%, #92400e 100%);
}
</style>
