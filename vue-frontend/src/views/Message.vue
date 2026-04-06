<script setup>
import { ref, onMounted, computed, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getContacts, getHistory, sendMessage, withdrawMessage } from '@/api/chat'
import { useAppStore } from '@/stores/app'
import { useWsStore } from '@/stores/ws'
import { Avatar, AvatarImage, AvatarFallback } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { Send, ChevronLeft, MoreHorizontal, Trash2, Clock, Loader2, AlertTriangle, User, UserX, Flag, Smile, Image as ImageIcon, Film, X } from 'lucide-vue-next'
import { DropdownMenu, DropdownMenuTrigger, DropdownMenuContent, DropdownMenuItem, DropdownMenuSeparator } from '@/components/ui/dropdown-menu'
import { formatRelativeDate } from '@/utils/timeFormat'
import { uploadFile } from '@/api/file'
import { blockUser } from '@/api/social'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const wsStore = useWsStore()

const currentUser = computed(() => JSON.parse(localStorage.getItem('user') || '{}'))
const contacts = ref([])
const activeChat = ref(null)

// 去重后的联系人列表
const uniqueContacts = computed(() => {
  const userMap = new Map()

  contacts.value.forEach((contact) => {
    // 【修复】统一使用 String 类型作为 Key，避免 ID 类型不一致导致去重失败
    const userId = String(contact.id)

    // 如果用户已存在,比较时间戳,保留最新的
    if (userMap.has(userId)) {
      const existing = userMap.get(userId)
      const existingTime = new Date(existing.latestTime || existing.createTime).getTime()
      const currentTime = new Date(contact.latestTime || contact.createTime).getTime()

      if (currentTime > existingTime) {
        // 保留在线状态(如果任一记录显示在线,则显示在线)
        contact.isOnline = contact.isOnline || existing.isOnline
        // 累加未读数 (修复字符串拼接问题)
        contact.unreadCount = (parseInt(contact.unreadCount) || 0) + (parseInt(existing.unreadCount) || 0)
        // 优先保留有头像的记录（防止临时创建的空头像覆盖了API返回的真实头像）
        if (!contact.avatar && existing.avatar) {
          contact.avatar = existing.avatar
          contact.nickname = existing.nickname
        }
        userMap.set(userId, contact)
      } else {
        // 更新现有记录的在线状态和未读数
        existing.isOnline = existing.isOnline || contact.isOnline
        existing.unreadCount = (parseInt(existing.unreadCount) || 0) + (parseInt(contact.unreadCount) || 0)
        // 优先保留有头像的记录
        if (!existing.avatar && contact.avatar) {
          existing.avatar = contact.avatar
          existing.nickname = contact.nickname
        }
      }
    } else {
      userMap.set(userId, contact)
    }
  })

  // 转换为数组并按最后消息时间降序排序
  return Array.from(userMap.values()).sort((a, b) => {
    const timeA = new Date(a.latestTime || a.createTime).getTime()
    const timeB = new Date(b.latestTime || b.createTime).getTime()
    return timeB - timeA
  })
})
const messages = ref([])
const inputContent = ref('')
const isMobile = ref(window.innerWidth < 768)
const showChatWindow = ref(false)
const sending = ref(false) // 发送中状态
const showEmojiPicker = ref(false)
const fileInput = ref(null)
const showImagePreview = ref(false)
const previewImageUrl = ref('')

const emojis = ['😀','😃','😄','😁','😆','😅','😂','🤣','😊','😇','🙂','🙃','😉','😌','😍','🥰','😘','😗','😙','😚','😋','😛','😝','😜','🤪','🤨','🧐','🤓','😎','🤩','🥳','😏','😒','😞','😔','😟','😕','🙁','☹️','😣','😖','😫','😩','🥺','😢','😭','😤','😠','😡','🤬','🤯','😳','🥵','🥶','😱','😨','😰','😥','😓','🤗','🤔','🤭','🤫','🤥','😶','😐','😑','😬','🙄','😯','😦','😧','😮','😲','🥱','😴','🤤','😪','😵','🤐','🥴','🤢','🤮','🤧','😷','🤒','🤕','🤑','🤠','😈','👿','👹','👺','🤡','💩','👻','💀','☠️','👽','👾','🤖','👋','👌','✌️','🤞','🤟','🤘','🤙','👈','👉','👆','👇','☝️','👍','👎','✊','👊','🤛','🤜','👏','🙌','👐','🤲','🤝','🙏','✍️','💅','🤳','💪','🦾','🦿','🦵','🦶','👂','🦻','👃','🧠','🫀','🫁','🦷','🦴','👀','👁','👅','👄','💋','🩸']

const insertEmoji = (emoji) => {
  inputContent.value += emoji
  showEmojiPicker.value = false
}

const triggerFile = () => {
  fileInput.value.click()
}

const handleFileUpload = async (event) => {
  const files = event.target.files
  if (!files || files.length === 0) return

  // 【前端校验】限制一次最多选择9个文件
  if (files.length > 9) {
    appStore.showToast('一次最多只能发送9个文件', 'warning')
    event.target.value = ''
    return
  }

  sending.value = true
  
  // 【批量发送】遍历所有选中的文件逐个发送
  for (const file of Array.from(files)) {
    // 确定消息类型及对应大小限制
    let msgType = 0
    if (file.type.startsWith('image/')) {
      msgType = 1
      if (file.size > 10 * 1024 * 1024) {
        appStore.showToast(`图片 ${file.name} 超过10MB，已跳过`, 'error')
        continue
      }
    } else if (file.type.startsWith('video/')) {
      msgType = 3
      if (file.size > 100 * 1024 * 1024) {
        appStore.showToast(`视频 ${file.name} 超过100MB，已跳过`, 'error')
        continue
      }
    } else {
      appStore.showToast(`文件 ${file.name} 格式不支持`, 'warning')
      continue
    }

    try {
      const formData = new FormData()
      formData.append('file', file)
      formData.append('userId', currentUser.value.id)

      const uploadRes = await uploadFile(formData)
      if (uploadRes.code === '200') {
        const fileUrl = uploadRes.data
        
        // 发送消息
        const res = await sendMessage({
          senderId: currentUser.value.id,
          receiverId: activeChat.value.id,
          content: fileUrl,
          type: msgType
        })

        if (res.code === '200') {
          // 发送成功一条就刷新一次，体验更好
          await loadContacts()
        } else {
          appStore.showToast(res.msg, 'error')
        }
      } else {
        appStore.showToast(uploadRes.msg, 'error')
      }
    } catch (e) {
      appStore.showToast('发送中断，请检查网络', 'error')
    }
  }
  
  sending.value = false
  // 清空 input 防止重复选择同一文件不触发 change
  event.target.value = ''
}

const previewImage = (url) => {
  previewImageUrl.value = url
  showImagePreview.value = true
}

//计算总未读数(使用去重后的列表)
const totalUnreadCount = computed(() => {
  return uniqueContacts.value.reduce((sum, contact) => sum + (parseInt(contact.unreadCount) || 0), 0)
})

//首次私信限制逻辑
// 规则：A给B第一次发消息时只能发送一条，除非B回复后才能继续发送
// 如果双方互相关注，则不限制（暂未实现互相关注检查）
const isInputDisabled = computed(() => {
  if (!activeChat.value || !messages.value.length) return false

  // TODO: 如果双方互相关注，不限制
  // if (activeChat.value.isMutualFollow) return false

  // 检查是否有对方的回复
  const hasReceiverReply = messages.value.some(msg => msg.senderId !== currentUser.value.id)

  // 如果对方已经回复过，不限制
  if (hasReceiverReply) {
    return false
  }

  // 如果对方从未回复过，检查自己是否已经发送了消息
  const hasSentMessage = messages.value.some(msg => msg.senderId === currentUser.value.id)

  // 如果已经发送过消息且对方未回复，禁用输入
  return hasSentMessage
})

//输入框占位符
const inputPlaceholder = computed(() => {
  if (isInputDisabled.value) {
    return '等待对方回复后才能继续发送...'
  }
  return 'Enter 发送，Ctrl+Enter 换行'
})

//撤回确认弹窗状态
const showWithdrawDialog = ref(false)
const withdrawTarget = ref(null)
const withdrawing = ref(false)

//拉黑确认弹窗状态
const showBlockDialog = ref(false)
const blocking = ref(false)

//聊天菜单功能
const goToUserProfile = (userId) => {
  router.push(`/user/${userId}`)
}

const openBlockDialog = () => {
  showBlockDialog.value = true
}

const confirmBlock = async () => {
  if (!activeChat.value || blocking.value) return

  blocking.value = true
  try {
    const res = await blockUser({ userId: currentUser.value.id, targetId: activeChat.value.id })
    if (res.code === '200') {
      appStore.showToast('已拉黑该用户', 'success')
      showBlockDialog.value = false
      // 从联系人列表中移除
      contacts.value = contacts.value.filter(c => c.id !== activeChat.value.id)
      activeChat.value = null
      showChatWindow.value = false
    } else {
      appStore.showToast(res.msg || '拉黑失败', 'error')
    }
  } catch (e) {
    appStore.showToast('操作失败，请重试', 'error')
  } finally {
    blocking.value = false
  }
}

const reportUser = () => {
  appStore.showToast('举报功能开发中...', 'info')
}

// 使用统一的时间格式化工具
const formatTime = formatRelativeDate

const loadContacts = async () => {
  const res = await getContacts(currentUser.value.id)
  if (res.code === '200') {
    contacts.value = res.data

    //如果从个人主页点击私信进来，自动选择或创建会话
    if (route.query.targetId) {
      // 【修复】统一转为 String 进行比较
      const targetId = String(route.query.targetId)
      const target = uniqueContacts.value.find(c => String(c.id) === targetId)
      
      if (target) {
        // 已有会话，直接选中
        selectContact(target)
      } else {
        // 没有会话，创建一个临时会话对象
        const newContact = {
          id: parseInt(targetId), // 保持 id 为数字类型以便后端兼容
          nickname: route.query.targetName || '用户',
          avatar: '',
          latestMsg: '',
          latestTime: new Date().toISOString(),
          unreadCount: 0,
          isOnline: false
        }
        // 添加到联系人列表顶部
        contacts.value.unshift(newContact)
        // 自动选中
        selectContact(newContact)
      }
    }
  }
}

const selectContact = async (contact) => {
  activeChat.value = contact
  showChatWindow.value = true

  // 清除当前联系人的未读数
  if (contact.unreadCount > 0) {
    // 先将本地未读数清零
    contact.unreadCount = 0
    // 等待 computed totalUnreadCount 重新计算后再同步到 wsStore
    await nextTick()
    wsStore.chatUnreadCount = totalUnreadCount.value
  }

  // 拉取聊天记录（后端 getHistory 会自动指行 readAll 标记已读）
  const res = await getHistory(currentUser.value.id, contact.id)
  if (res.code === '200') {
    messages.value = res.data
    scrollToBottom()
  }
}

const handleSend = async () => {
  //检查是否被禁用
  if (isInputDisabled.value) {
    appStore.showToast('请等待对方回复后再发送消息', 'warning')
    return
  }

  if (!inputContent.value.trim() || sending.value) return

  sending.value = true
  const tempMsg = {
    id: Date.now(), senderId: currentUser.value.id, content: inputContent.value,
    type: 0, isWithdrawn: 0, createTime: new Date()
  }
  messages.value.push(tempMsg)
  const textToSend = inputContent.value
  inputContent.value = ''
  scrollToBottom()

  try {
    const res = await sendMessage({
      senderId: currentUser.value.id,
      receiverId: activeChat.value.id,
      content: textToSend,
      type: 0
    })

    if (res.code !== '200') {
      appStore.showToast(res.msg, 'error')
      messages.value.pop()
      inputContent.value = textToSend
    } else {
      loadContacts()
    }
  } catch (e) {
    appStore.showToast('发送失败，请重试', 'error')
    messages.value.pop()
    inputContent.value = textToSend
  } finally {
    sending.value = false
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    const el = document.getElementById('chat-container')
    if (el) el.scrollTop = el.scrollHeight
  })
}

//打开撤回确认弹窗
const openWithdrawDialog = (msg) => {
  withdrawTarget.value = msg
  showWithdrawDialog.value = true
}

//确认撤回
const confirmWithdraw = async () => {
  if (!withdrawTarget.value || withdrawing.value) return

  withdrawing.value = true
  try {
    const res = await withdrawMessage(withdrawTarget.value.id, currentUser.value.id)
    if (res.code === '200') {
      withdrawTarget.value.isWithdrawn = 1
      appStore.showToast('消息已撤回', 'success')
      showWithdrawDialog.value = false
      withdrawTarget.value = null
    } else {
      appStore.showToast(res.msg || '撤回失败', 'error')
    }
  } catch (e) {
    appStore.showToast('撤回失败，请重试', 'error')
  } finally {
    withdrawing.value = false
  }
}

// 监听私信未读数变化（如 WebSocket 收到对方发来的新私信）
watch(() => wsStore.chatUnreadCount, () => {
  loadContacts()
  if (activeChat.value) {
    getHistory(currentUser.value.id, activeChat.value.id).then(res => {
      if (res.code === '200') messages.value = res.data
      scrollToBottom()
    })
  }
})

//使用函数引用以便清理，并添加节流优化
import { throttle } from '@/utils/performance'
const handleResize = throttle(() => {
  isMobile.value = window.innerWidth < 768
}, 200)
window.addEventListener('resize', handleResize)

//移动端键盘适配
const setupMobileKeyboardAdaptation = () => {
  // 只在移动端且支持 visualViewport 的浏览器中启用
  if (!window.visualViewport || window.innerWidth >= 768) {
    return null
  }

  const viewport = window.visualViewport
  const inputBar = document.querySelector('.chat-input-bar')

  if (!inputBar) return null

  let initialHeight = window.innerHeight

  const handleViewportChange = () => {
    // 只在移动端处理
    if (window.innerWidth >= 768) return

    const currentHeight = viewport.height
    const heightDiff = initialHeight - currentHeight

    // 键盘弹出（高度差大于 150px 认为是键盘）
    if (heightDiff > 150) {
      inputBar.style.transform = `translateY(-${heightDiff}px)`
      inputBar.style.transition = 'transform 0.2s ease-out'

      // 滚动到底部，确保输入框可见
      nextTick(() => {
        const chatContainer = document.getElementById('chat-container')
        if (chatContainer) {
          chatContainer.scrollTop = chatContainer.scrollHeight
        }
      })
    } else {
      // 键盘收起
      inputBar.style.transform = 'translateY(0)'
      inputBar.style.transition = 'transform 0.2s ease-out'
    }
  }

  viewport.addEventListener('resize', handleViewportChange)
  viewport.addEventListener('scroll', handleViewportChange)

  // 返回清理函数
  return () => {
    viewport.removeEventListener('resize', handleViewportChange)
    viewport.removeEventListener('scroll', handleViewportChange)
    if (inputBar) {
      inputBar.style.transform = ''
      inputBar.style.transition = ''
    }
  }
}

onMounted(() => {
  loadContacts()

  // 设置移动端键盘适配
  const cleanupKeyboard = setupMobileKeyboardAdaptation()

  // 组件卸载时清理
  onUnmounted(() => {
    if (cleanupKeyboard) {
      cleanupKeyboard()
    }
  })
})

//组件卸载时清理事件监听
import { onUnmounted } from 'vue'
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<template>
  <div
    class="h-[calc(100vh-64px)] bg-gradient-to-br from-slate-50 via-emerald-50/50 to-teal-50/40 flex overflow-hidden border-t border-gray-200/50 relative">
    <!-- 装饰背景 - 增强对比度 -->
    <div class="absolute inset-0 pointer-events-none overflow-hidden">
      <div
        class="absolute top-20 -right-20 w-64 h-64 bg-gradient-to-br from-emerald-200/60 to-teal-200/60 rounded-full blur-3xl">
      </div>
      <div
        class="absolute bottom-20 -left-20 w-48 h-48 bg-gradient-to-br from-cyan-200/60 to-blue-200/60 rounded-full blur-3xl">
      </div>
      <!--网格背景 -->
      <div
        class="absolute inset-0 bg-[linear-gradient(to_right,#80808012_1px,transparent_1px),linear-gradient(to_bottom,#80808012_1px,transparent_1px)] bg-[size:24px_24px]">
      </div>
    </div>

    <!-- 联系人列表 - 增强对比度 -->
    <div v-if="!isMobile || !showChatWindow"
      class="w-full md:w-80 bg-white/95 backdrop-blur-xl border-r border-gray-200/80 flex flex-col relative z-10 shadow-xl">
      <div class="p-5 border-b border-gray-200 bg-gradient-to-r from-white to-gray-50/80 backdrop-blur-md shadow-sm">
        <div class="flex items-center gap-3">
          <Button variant="ghost" size="icon" @click="router.push('/home')"
            class="hover:bg-emerald-50 hover:text-[rgb(33,111,85)] rounded-full -ml-2">
            <ChevronLeft class="w-5 h-5" />
          </Button>
          <div class="w-1 h-6 bg-gradient-to-b from-[rgb(33,111,85)] to-emerald-500 rounded-full"></div>
          <span class="font-bold text-gray-800 text-lg">消息</span>
          <!--只在有未读消息时显示红色高亮数字 -->
          <span v-if="totalUnreadCount > 0"
            class="text-xs bg-gradient-to-r from-red-500 to-pink-500 text-white px-2 py-0.5 rounded-full font-bold shadow-lg animate-pulse">
            {{ totalUnreadCount > 99 ? '99+' : totalUnreadCount }}
          </span>
        </div>
      </div>
      <div class="flex-1 overflow-y-auto">
        <!--联系人列表为空时显示空状态 -->
        <div v-if="uniqueContacts.length === 0"
          class="flex flex-col items-center justify-center h-full p-6 text-center">
          <div
            class="w-20 h-20 bg-gradient-to-br from-emerald-100 to-teal-100 rounded-full flex items-center justify-center shadow-lg mb-4">
            <span class="text-4xl">💬</span>
          </div>
          <p class="text-gray-600 font-medium mb-2">还没有聊天记录</p>
          <p class="text-sm text-gray-400 mb-4">去关注感兴趣的人，开启社交之旅</p>
          <Button
            class="bg-gradient-to-r from-[rgb(33,111,85)] to-emerald-600 hover:from-[rgb(28,95,72)] hover:to-emerald-700 text-white rounded-full px-6"
            @click="router.push('/home')">
            🔍 发现好友
          </Button>
        </div>

        <!-- 联系人列表 - 增强悬停效果 -->
        <div v-else v-for="c in uniqueContacts" :key="c.id" @click="selectContact(c)"
          class="p-4 flex gap-4 hover:bg-gradient-to-r hover:from-emerald-50 hover:to-teal-50 cursor-pointer border-b border-gray-100 transition-all duration-200 hover:shadow-md"
          :class="{ 'bg-gradient-to-r from-emerald-100/80 to-teal-100/80 border-l-4 border-l-[rgb(33,111,85)] shadow-md': activeChat?.id === c.id }">
          <div class="relative">
            <Avatar class="w-12 h-12 rounded-xl border-2 border-white shadow-md">
              <AvatarImage :src="c.avatar" />
              <AvatarFallback class="bg-gradient-to-br from-[rgb(33,111,85)] to-emerald-600 text-white">{{
                c.nickname?.[0] }}</AvatarFallback>
            </Avatar>
            <span v-if="c.unreadCount > 0"
              class="absolute -top-1 -right-1 bg-gradient-to-r from-red-500 to-pink-500 text-white text-[10px] px-1.5 rounded-full min-w-[18px] text-center font-bold shadow-lg animate-pulse">{{
                c.unreadCount > 99 ? '99+' : c.unreadCount }}</span>
            <span v-if="c.isOnline"
              class="absolute -bottom-0.5 -right-0.5 w-3.5 h-3.5 bg-green-400 rounded-full border-2 border-white"></span>
          </div>
          <div class="flex-1 min-w-0">
            <div class="flex justify-between mb-1.5">
              <span class="font-bold text-gray-800 truncate">{{ c.nickname }}</span>
              <span class="text-[10px] text-gray-400 bg-gray-100 px-2 py-0.5 rounded-full">{{ formatTime(c.latestTime)
              }}</span>
            </div>
            <p class="text-xs text-gray-500 truncate">{{ c.latestMsg }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 聊天窗口 - 增强背景 -->
    <div v-if="(!isMobile || showChatWindow) && activeChat"
      class="flex-1 flex flex-col bg-gradient-to-br from-slate-50/90 to-emerald-50/50 relative z-10">
      <div
        class="h-16 bg-white/95 backdrop-blur-xl border-b border-gray-200 flex items-center px-5 justify-between shadow-md z-10">
        <div class="flex items-center gap-3">
          <Button v-if="isMobile" variant="ghost" size="icon" class="hover:bg-emerald-50 rounded-full"
            @click="showChatWindow = false">
            <ChevronLeft />
          </Button>
          <!--头像可点击，跳转到对方主页 -->
          <Avatar
            class="w-10 h-10 ring-2 ring-white/50 shadow-md cursor-pointer hover:ring-[rgb(33,111,85)] transition-all"
            @click="goToUserProfile(activeChat.id)" title="查看TA的主页">
            <AvatarImage :src="activeChat.avatar" />
            <AvatarFallback class="bg-gradient-to-br from-[rgb(33,111,85)] to-emerald-600 text-white">{{
              activeChat.nickname?.[0] }}</AvatarFallback>
          </Avatar>
          <div class="cursor-pointer hover:opacity-80 transition-opacity" @click="goToUserProfile(activeChat.id)">
            <span class="font-bold text-gray-800 block">{{ activeChat.nickname }}</span>
            <span class="text-xs" :class="activeChat.isOnline ? 'text-green-500' : 'text-gray-400'">
              {{ activeChat.isOnline ? '在线' : '离线' }}
            </span>
          </div>
        </div>

        <!--右上角三个点菜单 -->
        <DropdownMenu>
          <DropdownMenuTrigger as-child>
            <Button variant="ghost" size="icon" class="rounded-full hover:bg-gray-100">
              <MoreHorizontal class="text-gray-400" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" class="w-48 bg-white backdrop-blur-md">
            <DropdownMenuItem @click="goToUserProfile(activeChat.id)" class="cursor-pointer">
              <User class="w-4 h-4 mr-2" />
              查看TA的主页
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem @click="openBlockDialog" class="cursor-pointer text-orange-600">
              <UserX class="w-4 h-4 mr-2" />
              拉黑此人
            </DropdownMenuItem>
            <DropdownMenuItem @click="reportUser" class="cursor-pointer text-red-600">
              <Flag class="w-4 h-4 mr-2" />
              举报此人
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>

      <!-- 消息区域 - 增强对比度 -->
      <div id="chat-container"
        class="flex-1 overflow-y-auto p-6 space-y-5 bg-gradient-to-b from-transparent to-white/30">
        <div v-for="msg in messages" :key="msg.id" class="flex gap-3"
          :class="msg.senderId === currentUser.id ? 'flex-row-reverse' : ''">
          <!--消息中的头像也可点击 -->
          <Avatar
            class="w-10 h-10 rounded-xl flex-shrink-0 ring-2 ring-white shadow-lg cursor-pointer hover:ring-[rgb(33,111,85)] transition-all"
            @click="goToUserProfile(msg.senderId === currentUser.id ? currentUser.id : activeChat.id)"
            :title="msg.senderId === currentUser.id ? '我的主页' : '查看TA的主页'">
            <AvatarImage :src="msg.senderId === currentUser.id ? currentUser.avatar : activeChat.avatar" />
            <AvatarFallback class="bg-gradient-to-br from-[rgb(33,111,85)] to-emerald-600 text-white text-sm">
              {{ msg.senderId === currentUser.id ? currentUser.nickname?.[0] : activeChat.nickname?.[0] }}
            </AvatarFallback>
          </Avatar>
          <div class="max-w-[70%]">
            <div v-if="msg.isWithdrawn"
              class="text-center text-xs text-gray-500 bg-gray-100 px-3 py-1.5 rounded-full mb-2 shadow-sm">
              消息已撤回</div>
            <div v-else class="group relative">
              <!--消息气泡 - 增强对比度和阴影 -->
              <div
                class="px-4 py-3 rounded-2xl shadow-lg text-[15px] leading-relaxed break-words relative transition-all duration-200 hover:shadow-xl"
                :class="[
                  msg.senderId === currentUser.id ? 'bg-gradient-to-r from-[rgb(33,111,85)] to-emerald-600 text-white rounded-br-md' : 'bg-white text-gray-800 rounded-bl-md border border-gray-200 shadow-md',
                  (msg.type === 1 || msg.type === 3) ? 'p-1 bg-transparent border-0 shadow-none hover:shadow-none !bg-none' : ''
                ]">
                
                <!-- 文本/表情消息 -->
                <span v-if="msg.type === 0 || !msg.type">{{ msg.content }}</span>
                
                <!-- 图片消息 -->
                <div v-else-if="msg.type === 1" class="cursor-pointer group overflow-hidden rounded-lg relative" @click="previewImage(msg.content)">
                  <img :src="msg.content" class="max-w-[240px] max-h-[300px] object-cover hover:scale-105 transition-transform duration-300" alt="图片" />
                  <div class="absolute inset-0 bg-black/0 group-hover:bg-black/10 transition-colors"></div>
                </div>

                <!-- 视频消息 -->
                <div v-else-if="msg.type === 3" class="max-w-[280px]">
                  <video :src="msg.content" controls class="w-full rounded-lg bg-black/5" preload="metadata"></video>
                </div>
                
                <!-- 未知类型 -->
                 <span v-else class="text-sm opacity-70">[收到不支持的消息类型]</span>
              </div>
              <div v-if="msg.senderId === currentUser.id"
                class="absolute top-1/2 -translate-y-1/2 -left-10 opacity-0 group-hover:opacity-100 transition-all duration-200 cursor-pointer p-1.5 rounded-full hover:bg-red-50 shadow-md"
                @click="openWithdrawDialog(msg)">
                <Trash2 class="w-4 h-4 text-gray-400 hover:text-red-500" />
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 输入框 - 增强对比度 + 移动端键盘适配 -->
      <div
        class="chat-input-bar bg-white/95 backdrop-blur-xl border-t border-gray-200 p-4 shadow-[0_-4px_20px_rgba(0,0,0,0.08)] flex flex-col gap-2">
        
        <!-- 工具栏 -->
        <div class="flex gap-4 px-1 relative">
          <div class="p-1.5 hover:bg-gray-100 rounded-lg cursor-pointer transition-colors" @click="showEmojiPicker = !showEmojiPicker" title="表情">
             <Smile class="w-5 h-5 text-gray-500 hover:text-[rgb(33,111,85)]" />
          </div>
           <!-- Emoji Picker Popover -->
           <div v-if="showEmojiPicker" class="absolute bottom-10 left-0 bg-white shadow-2xl rounded-2xl p-4 w-72 h-64 overflow-y-auto border border-gray-100 z-50 grid grid-cols-8 gap-y-2 gap-x-1 scrollbar-hide">
              <span v-for="e in emojis" :key="e" @click="insertEmoji(e)" class="cursor-pointer hover:bg-emerald-50 p-1.5 rounded transition-colors text-lg flex items-center justify-center">{{e}}</span>
              <!-- 关闭遮罩 -->
              <div class="fixed inset-0 z-[-1]" @click="showEmojiPicker = false"></div>
           </div>

          <div class="p-1.5 hover:bg-gray-100 rounded-lg cursor-pointer transition-colors" @click="triggerFile" title="可批量选择图片/视频">
             <ImageIcon class="w-5 h-5 text-gray-500 hover:text-[rgb(33,111,85)]" />
          </div>
          <input type="file" ref="fileInput" class="hidden" @change="handleFileUpload" accept="image/*,video/*" multiple />
        </div>
        <div class="flex gap-3 items-end">
          <textarea v-model="inputContent" rows="1" :disabled="isInputDisabled"
            :aria-label="isInputDisabled ? '等待对方回复后才能发送' : '输入消息'" :aria-disabled="isInputDisabled"
            class="flex-1 bg-gray-50 rounded-2xl px-5 py-3 text-sm resize-none focus:bg-white border-2 border-gray-200 focus:border-[rgb(33,111,85)] focus:shadow-lg focus:shadow-emerald-500/20 transition-all duration-300 outline-none disabled:bg-gray-100 disabled:cursor-not-allowed disabled:text-gray-500"
            :placeholder="inputPlaceholder" @keydown.enter.exact.prevent="handleSend"
            @keydown.ctrl.enter="inputContent += '\n'"></textarea>
          <Button
            class="bg-gradient-to-r from-[rgb(33,111,85)] to-emerald-600 hover:from-[rgb(28,95,72)] hover:to-emerald-700 text-white px-6 py-3 rounded-2xl font-bold shadow-lg shadow-emerald-500/30 transition-all duration-300 hover:scale-105 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:scale-100"
            :disabled="!inputContent.trim() || sending || isInputDisabled" :aria-label="sending ? '正在发送' : '发送消息'"
            :aria-busy="sending" @click="handleSend">
            <Loader2 v-if="sending" class="w-4 h-4 animate-spin" />
            <Send v-else class="w-4 h-4" />
          </Button>
        </div>
      </div>
    </div>

    <!-- 空状态 (仿微信电脑版风格) -->
    <div v-else
      class="flex-1 flex items-center justify-center bg-[#f5f5f5] flex-col relative z-0 select-none"
      :class="isMobile ? 'hidden' : ''">
      <div class="flex flex-col items-center opacity-30 grayscale hover:grayscale-0 transition-all duration-500">
         <div class="w-20 h-20 mb-6 rounded-2xl bg-gray-200/50 flex items-center justify-center">
           <svg xmlns="http://www.w3.org/2000/svg" class="w-10 h-10 text-gray-400/80" viewBox="0 0 24 24" fill="currentColor" stroke="none">
             <path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z"/>
           </svg>
         </div>
         <span class="text-sm font-medium tracking-widest text-gray-400">未选择聊天</span>
      </div>
    </div>

    <!--撤回确认弹窗 -->
    <Dialog :open="showWithdrawDialog" @update:open="showWithdrawDialog = $event">
      <DialogContent class="sm:max-w-[380px] bg-white rounded-xl">
        <DialogHeader>
          <DialogTitle class="text-lg font-bold text-gray-800 flex items-center gap-2">
            <AlertTriangle class="w-5 h-5 text-orange-500" /> 撤回消息
          </DialogTitle>
        </DialogHeader>
        <div class="py-4">
          <p class="text-gray-600 mb-2">确定要撤回这条消息吗？</p>
          <p class="text-sm text-gray-400">撤回后对方将无法看到这条消息。</p>
          <div v-if="withdrawTarget" class="mt-4 p-3 bg-gray-50 rounded-lg border border-gray-100">
            <p class="text-sm text-gray-700 line-clamp-2">{{ withdrawTarget.content }}</p>
          </div>
        </div>
        <DialogFooter class="gap-2">
          <Button variant="outline" class="whitespace-nowrap" @click="showWithdrawDialog = false"
            :disabled="withdrawing">取消</Button>
          <Button class="bg-orange-500 hover:bg-orange-600 text-white whitespace-nowrap" @click="confirmWithdraw"
            :disabled="withdrawing">
            <Loader2 v-if="withdrawing" class="w-4 h-4 mr-2 animate-spin" />
            {{ withdrawing ? '撤回中...' : '确认撤回' }}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!--拉黑确认弹窗 -->
    <Dialog :open="showBlockDialog" @update:open="showBlockDialog = $event">
      <DialogContent class="sm:max-w-[400px] bg-white rounded-xl">
        <DialogHeader>
          <DialogTitle class="text-lg font-bold text-orange-600 flex items-center gap-2">
            <UserX class="w-5 h-5" /> 拉黑用户
          </DialogTitle>
        </DialogHeader>
        <div class="py-4">
          <p class="text-gray-600 mb-2">确定要拉黑 <span class="font-bold text-orange-600">{{ activeChat?.nickname }}</span>
            吗？</p>
          <div class="mt-4 p-3 bg-orange-50 rounded-lg border border-orange-200">
            <p class="text-sm text-orange-700 font-medium mb-2">⚠️ 拉黑后将产生以下影响：</p>
            <ul class="text-xs text-orange-600 space-y-1 ml-4 list-disc">
              <li>对方将无法向你发送私信</li>
              <li>你将无法看到对方的动态和评论</li>
              <li>对方不会收到拉黑通知</li>
              <li>可以在设置中解除拉黑</li>
            </ul>
          </div>
        </div>
        <DialogFooter class="gap-2">
          <Button variant="outline" class="whitespace-nowrap" @click="showBlockDialog = false"
            :disabled="blocking">取消</Button>
          <Button class="bg-red-500 hover:bg-red-600 text-white whitespace-nowrap" @click="confirmBlock"
            :disabled="blocking">
            <Loader2 v-if="blocking" class="w-4 h-4 mr-2 animate-spin" />
            {{ blocking ? '处理中...' : '确认拉黑' }}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- 图片预览弹窗 -->
    <Dialog :open="showImagePreview" @update:open="showImagePreview = $event">
      <DialogContent class="max-w-[90vw] max-h-[90vh] bg-transparent border-0 shadow-none p-0 flex items-center justify-center">
         <div class="relative">
           <img :src="previewImageUrl" class="max-w-full max-h-[85vh] rounded-lg shadow-2xl" />
           <Button variant="secondary" size="icon" class="absolute -top-4 -right-4 rounded-full shadow-lg" @click="showImagePreview = false">
             <X class="w-4 h-4" />
           </Button>
         </div>
      </DialogContent>
    </Dialog>
  </div>
</template>