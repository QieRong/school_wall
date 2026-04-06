<script setup>
import { ref, onMounted, computed, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPostDetail } from '@/api/post'
import { getComments, sendComment } from '@/api/comment'
import { toggleCollection, checkCollectStatus } from '@/api/social'
import { submitReport } from '@/api/report'
import { useAppStore } from '@/stores/app'
import { useLikeWithDebounce } from '@/composables/useLikeWithDebounce'
import { Button } from '@/components/ui/button'
import { Avatar, AvatarImage, AvatarFallback } from '@/components/ui/avatar'
import { Badge } from '@/components/ui/badge'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import {
  ChevronLeft, Heart, MessageCircle, Share2, Send, X, Home,
  ChevronRight, Eye, MoreHorizontal, AlertTriangle, Star, MapPin, BarChart2,
  ImagePlus, Loader2, Maximize2, Minimize2
} from 'lucide-vue-next'
import EmojiPicker from '@/components/EmojiPicker.vue'
import {
  DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger
} from '@/components/ui/dropdown-menu'
import request from '@/api/request'
import { formatTime as formatTimeUtil } from '@/utils/timeFormat'
import { DEFAULT_CENTER, loadAmapScript, initAmapSecurity } from '@/utils/mapConfig'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()

const post = ref(null)
const comments = ref([])
const loading = ref(true)
const commentContent = ref('')
const replyingTo = ref(null)
const isCollected = ref(false)
const showEmoji = ref(false)

// 评论图片相关
const commentImgInput = ref(null)
const commentImgPreview = ref('')  // 本地预览URL
const commentImgUrl = ref('')      // 上传后的服务器URL
const commentImgUploading = ref(false)

// 评论图片灯箱（点击评论中的图片放大查看）
const showCommentImgLightbox = ref(false)
const commentLightboxImg = ref('')

// 位置地图弹窗
const showLocationMap = ref(false)
const locationName = ref('')
const locationCoords = ref(null)
const locationMapLoading = ref(true)
const locationSearchFailed = ref(false)  // 位置搜索失败状态
const nearbyPOIs = ref([])
let locationMapInstance = null

const showReportDialog = ref(false)
const reportType = ref('')
const reportDesc = ref('')
const reportTypes = ['垃圾广告', '辱骂攻击', '色情低俗', '涉政敏感', '欺诈骗钱', '其他原因']
// 表情选择器通过 EmojiPicker 组件实现

const currentUser = computed(() => {
  try { return JSON.parse(localStorage.getItem('user')) } catch { return null }
})

// 投票相关
const voteResult = ref(null)
const myVote = ref(null)
const totalVotes = ref(0)
const pollEndTime = ref(null)
const isPollExpired = computed(() => pollEndTime.value && new Date(pollEndTime.value) < new Date())

// 媒体列表（图片+视频统一管理）
const mediaList = computed(() => {
  const list = []
  if (post.value?.imgList) {
    post.value.imgList.forEach(url => {
      list.push({ type: 'image', url })
    })
  }
  if (post.value?.video) {
    list.push({ type: 'video', url: post.value.video })
  }
  return list.slice(0, 9) // 最多9个
})

// 九宫格布局计算
const getMediaGridClass = (count) => {
  if (count === 1) return 'grid-cols-1 max-w-[350px]'
  if (count === 2) return 'grid-cols-2 max-w-[350px]'
  if (count === 4) return 'grid-cols-2 max-w-[350px]'
  return 'grid-cols-3 max-w-[400px]'
}

// 媒体预览相关
const showMediaPreview = ref(false)
const mediaPreviewIndex = ref(0)

const openMediaPreview = (idx) => {
  mediaPreviewIndex.value = idx
  showMediaPreview.value = true
  document.body.style.overflow = 'hidden'
}

const closeMediaPreview = () => {
  showMediaPreview.value = false
  document.body.style.overflow = 'auto'
}

const prevMedia = () => {
  if (mediaPreviewIndex.value > 0) mediaPreviewIndex.value--
}

const nextMedia = () => {
  if (mediaPreviewIndex.value < mediaList.value.length - 1) mediaPreviewIndex.value++
}

// 使用统一的时间格式化工具
const formatTime = formatTimeUtil

// 渲染带标签高亮的内容
const renderContentWithTags = (content) => {
  if (!content) return ''
  // 先转义HTML字符防止XSS
  const escaped = content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;')
  // 将 #标签 转换为可点击的链接样式
  return escaped.replace(/#([^\s#]+)/g, '<span class="tag-highlight" data-tag="$1">#$1</span>')
}

// 点击标签跳转搜索
const handleTagClick = (e) => {
  if (e.target.classList.contains('tag-highlight')) {
    const tag = e.target.dataset.tag
    router.push({ path: '/', query: { keyword: tag } })
  }
}

//移动端键盘弹出时调整输入框位置 - 问题8修复：添加备用方案
const inputBarStyle = ref({})
const isInputFocused = ref(false)

import { throttle } from '@/utils/performance'
const handleVisualViewportResize = throttle(() => {
  if (window.visualViewport) {
    const viewport = window.visualViewport
    const bottomOffset = window.innerHeight - viewport.height - viewport.offsetTop
    if (bottomOffset > 50) {
      // 键盘弹出
      inputBarStyle.value = { bottom: `${bottomOffset}px`, paddingBottom: '12px' }
    } else {
      inputBarStyle.value = {}
    }
  }
}, 100)

// 备用方案：滚动到输入框位置
const scrollToInput = () => {
  isInputFocused.value = true
  nextTick(() => {
    const inputEl = document.getElementById('cmt-input')
    if (inputEl) {
      // 延迟执行，等待键盘弹出
      setTimeout(() => {
        inputEl.scrollIntoView({ behavior: 'smooth', block: 'center' })
      }, 300)
    }
  })
}

const handleInputBlur = () => {
  isInputFocused.value = false
}

const loadData = async () => {
  const postId = route.params.id
  loading.value = true
  try {
    const postRes = await getPostDetail(postId, currentUser.value?.id)
    if (postRes.code === '200') {
      post.value = postRes.data
      //兼容JSON和逗号分隔两种格式
      if (post.value.images) {
        try {
          post.value.imgList = post.value.images.startsWith('[') ? JSON.parse(post.value.images) : post.value.images.split(',')
        } catch (e) {
          post.value.imgList = post.value.images.split(',')
        }
      } else {
        post.value.imgList = []
      }
      if (post.value.pollOptions) {
        try { post.value.parsedPoll = JSON.parse(post.value.pollOptions) } catch (e) { }
        // 加载投票结果
        await fetchVoteResult()
      }
      if (currentUser.value) {
        const cRes = await checkCollectStatus(currentUser.value.id, postId)
        isCollected.value = cRes.data
      }
    } else {
      throw new Error('帖子不存在')
    }
    await fetchComments()
  } catch (e) {
    appStore.showToast(e.message || '加载失败', 'error')
    router.back()
  } finally {
    loading.value = false
  }
}

const fetchComments = async () => {
  const res = await getComments(route.params.id)
  if (res.code === '200') {
    const rawList = res.data
    const roots = []
    const replyMap = {}

    rawList.forEach(c => {
      c.children = []
      c.showReplies = false
      const isRoot = !c.parentId || c.parentId === 0 || c.parentId === '0';

      if (isRoot) {
        roots.push(c)
      } else {
        if (!replyMap[c.parentId]) replyMap[c.parentId] = []
        replyMap[c.parentId].push(c)
      }
    })

    roots.forEach(root => {
      if (replyMap[root.id]) root.children = replyMap[root.id]
    })

    comments.value = roots.reverse()
  }
}

const handleSubmit = async () => {
  if (!currentUser.value) return appStore.showToast('请先登录', 'error')
  // 允许纯图片评论：内容为空但有图片也可以发送
  if (!commentContent.value.trim() && !commentImgUrl.value) {
    return appStore.showToast('请输入内容或添加图片', 'error')
  }

  const payload = {
    postId: post.value.id,
    userId: currentUser.value.id,
    content: commentContent.value || '[图片]',  // 纯图片时显示占位文字
    parentId: replyingTo.value ? replyingTo.value.rootId : 0,
    replyUserId: replyingTo.value ? replyingTo.value.userId : null,
    imgUrl: commentImgUrl.value || null
  }

  try {
    const res = await sendComment(payload)
    if (res.code === '200') {
      appStore.showToast('评论成功', 'success')
      commentContent.value = ''
      replyingTo.value = null
      showEmoji.value = false
      clearCommentImg()
      await fetchComments()
    } else {
      appStore.showToast(res.msg, 'error')
    }
  } catch (e) {
    appStore.showToast('发送失败', 'error')
  }
}

// 评论图片选择
const handleCommentImgSelect = () => {
  commentImgInput.value?.click()
}

// 评论图片上传
const handleCommentImgChange = async (e) => {
  const file = e.target.files?.[0]
  if (!file) return

  // 限制10MB
  if (file.size > 10 * 1024 * 1024) {
    appStore.showToast('图片不能超过10MB', 'error')
    return
  }

  // 本地预览
  commentImgPreview.value = URL.createObjectURL(file)
  commentImgUploading.value = true

  // 上传到服务器
  const formData = new FormData()
  formData.append('file', file)
  formData.append('userId', currentUser.value.id)

  try {
    const res = await request.post('/file/upload', formData)
    if (res.code === '200') {
      commentImgUrl.value = res.data
      appStore.showToast('图片上传成功', 'success')
    } else {
      clearCommentImg()
      appStore.showToast('上传失败', 'error')
    }
  } catch (err) {
    clearCommentImg()
    appStore.showToast('上传失败', 'error')
  } finally {
    commentImgUploading.value = false
    e.target.value = ''
  }
}

// 清除评论图片
const clearCommentImg = () => {
  if (commentImgPreview.value) URL.revokeObjectURL(commentImgPreview.value)
  commentImgPreview.value = ''
  commentImgUrl.value = ''
}

// 打开评论图片灯箱
const openCommentImgLightbox = (imgUrl) => {
  commentLightboxImg.value = imgUrl
  showCommentImgLightbox.value = true
  document.body.style.overflow = 'hidden'
}

// 关闭评论图片灯箱
const closeCommentImgLightbox = () => {
  showCommentImgLightbox.value = false
  document.body.style.overflow = 'auto'
}

// 打开位置地图
const openLocationMap = async (location) => {
  locationName.value = location
  showLocationMap.value = true
  locationMapLoading.value = true
  locationCoords.value = null
  nearbyPOIs.value = []
  document.body.style.overflow = 'hidden'

  await nextTick()
  setTimeout(() => initLocationMap(location), 300)
}

// 位置地图全屏状态
const locationMapFullscreen = ref(false)

// 切换位置地图全屏
const toggleLocationMapFullscreen = () => {
  locationMapFullscreen.value = !locationMapFullscreen.value
  nextTick(() => {
    if (locationMapInstance) {
      setTimeout(() => {
        locationMapInstance.resize()
      }, 300)
    }
  })
}

// 初始化位置地图
const initLocationMap = async (location) => {
  locationSearchFailed.value = false  // 重置搜索失败状态

  try {
    // 初始化安全配置
    initAmapSecurity()

    // 加载地图脚本
    await loadAmapScript()

    // 创建地图
    createLocationMap(location)
  } catch (error) {
    console.error('地图加载失败:', error)
    locationMapLoading.value = false
    locationSearchFailed.value = true
  }
}

// 创建位置地图
const createLocationMap = (location) => {
  // 创建地图实例
  locationMapInstance = new window.AMap.Map('location-map-container', {
    zoom: 16,
    center: DEFAULT_CENTER,
    mapStyle: 'amap://styles/light',
    touchZoom: true,       // 支持触摸缩放
    scrollWheel: true,     // 支持滚轮缩放
    doubleClickZoom: true  // 支持双击缩放
  })

  // 添加缩放控件
  window.AMap.plugin(['AMap.ToolBar', 'AMap.Scale'], () => {
    const toolbar = new window.AMap.ToolBar({
      position: 'RB',
      liteStyle: true
    })
    locationMapInstance.addControl(toolbar)

    const scale = new window.AMap.Scale()
    locationMapInstance.addControl(scale)
  })

  // 搜索位置
  const placeSearch = new window.AMap.PlaceSearch({
    pageSize: 10,
    pageIndex: 1,
    city: '张家界',
    citylimit: false
  })

  placeSearch.search(location, (status, result) => {
    locationMapLoading.value = false

    if (status === 'complete' && result.poiList?.pois?.length > 0) {
      const poi = result.poiList.pois[0]
      const coords = [poi.location.lng, poi.location.lat]
      locationCoords.value = coords

      // 移动地图到该位置
      locationMapInstance.setCenter(coords)
      locationMapInstance.setZoom(17)

      // 添加标记
      const marker = new window.AMap.Marker({
        position: coords,
        title: poi.name,
        animation: 'AMAP_ANIMATION_DROP'
      })
      locationMapInstance.add(marker)

      // 添加信息窗体
      const infoWindow = new window.AMap.InfoWindow({
        content: `<div style="padding: 8px 12px; font-size: 14px;">
          <div style="font-weight: bold; color: #216f55; margin-bottom: 4px;">${poi.name}</div>
          <div style="color: #666; font-size: 12px;">${poi.address || location}</div>
        </div>`,
        offset: new window.AMap.Pixel(0, -30)
      })
      infoWindow.open(locationMapInstance, coords)

      // 获取周边POI
      nearbyPOIs.value = result.poiList.pois.slice(0, 5).map(p => ({
        name: p.name,
        address: p.address,
        distance: p.distance,
        coords: [p.location.lng, p.location.lat]
      }))
    } else {
      // 搜索失败，使用默认位置并提示
      locationMapInstance.setCenter(DEFAULT_CENTER)
      locationSearchFailed.value = true
    }
  })
}

// 关闭位置地图
const closeLocationMap = () => {
  showLocationMap.value = false
  locationSearchFailed.value = false  // 重置搜索失败状态
  document.body.style.overflow = 'auto'
  if (locationMapInstance) {
    locationMapInstance.destroy()
    locationMapInstance = null
  }
}

// 在地图上选择POI
const selectPOI = (poi) => {
  if (locationMapInstance && poi.coords) {
    locationMapInstance.setCenter(poi.coords)
    locationMapInstance.setZoom(18)
  }
}

// 打开导航
const openNavigation = () => {
  if (locationCoords.value) {
    const [lng, lat] = locationCoords.value
    // 打开高德地图导航
    window.open(`https://uri.amap.com/marker?position=${lng},${lat}&name=${encodeURIComponent(locationName.value)}&coordinate=gaode&callnative=1`)
  }
}

const addEmoji = (emoji) => {
  commentContent.value += emoji
  showEmoji.value = false
  // 确保输入框重新获得焦点，这样用户可以继续输入
  nextTick(() => {
    const input = document.getElementById('cmt-input')
    if (input) {
      input.focus()
      // 将光标移到末尾
      input.setSelectionRange(input.value.length, input.value.length)
    }
  })
}

// 获取投票结果
const fetchVoteResult = async () => {
  if (!post.value?.parsedPoll) return
  try {
    const res = await request.get('/post/vote/result', {
      params: { postId: post.value.id, userId: currentUser.value?.id || 0 }
    })
    if (res.code === '200') {
      voteResult.value = res.data.counts || []
      myVote.value = res.data.myVote
      //从counts中计算总投票数
      totalVotes.value = res.data.totalVotes || (voteResult.value ? voteResult.value.reduce((sum, v) => sum + (v.count || 0), 0) : 0)
      pollEndTime.value = res.data.pollEndTime
    }
  } catch (e) {
    console.error('获取投票结果失败:', e)
  }
}

// 计算投票百分比
const getVotePercent = (idx) => {
  if (!voteResult.value || voteResult.value.length === 0) return 0
  const total = voteResult.value.reduce((sum, v) => sum + (v.count || 0), 0)
  if (total === 0) return 0
  const item = voteResult.value.find(v => v.option_index === idx || v.optionIndex === idx)
  return Math.round(((item?.count || 0) / total) * 100)
}

// 投票
const handleVote = async (idx) => {
  if (!currentUser.value) return appStore.showToast('请先登录', 'error')
  if (myVote.value !== null && myVote.value !== undefined) {
    return appStore.showToast('您已经投过票了', 'info')
  }
  try {
    const res = await request.post('/post/vote', {
      postId: post.value.id,
      userId: currentUser.value.id,
      index: idx
    })
    if (res.code === '200') {
      appStore.showToast('投票成功', 'success')
      myVote.value = idx
      await fetchVoteResult()
    } else {
      appStore.showToast(res.msg, 'error')
    }
  } catch (e) {
    appStore.showToast('投票失败', 'error')
  }
}

const handleReply = (comment, rootId) => {
  replyingTo.value = {
    id: comment.id, rootId: rootId || comment.id,
    userId: comment.userId, nickname: comment.nickname
  }
  setTimeout(() => document.getElementById('cmt-input')?.focus(), 100)
}
const cancelReply = () => {
  replyingTo.value = null
  commentContent.value = ''
  clearCommentImg()
}

const collectLoading = ref(false)

// 点赞API调用函数
const likeApiCall = async (postId, userId) => {
  const res = await request.post(`/post/like/${postId}?userId=${userId}`)
  return res
}

// 使用防抖点赞composable
// 使用防抖点赞composable
const { handleLike: debouncedHandleLike } = useLikeWithDebounce(likeApiCall)

// 点赞处理
const handleLike = async () => {
  if (!currentUser.value) return appStore.showToast('请登录', 'error')
  if (!post.value) return

  const postId = post.value.id
  const originalLiked = post.value.isLiked
  const originalCount = post.value.likeCount || 0

  const result = await debouncedHandleLike(postId, currentUser.value.id, originalLiked)

  if (result.success) {
    const res = result.data
    if (res.code === '200') {
      const isLiked = res.data
      post.value.isLiked = isLiked
      post.value.likeCount = isLiked ? originalCount + 1 : Math.max(0, originalCount - 1)

      if (isLiked) {
        appStore.showToast('❤️ 点赞成功', 'success')
      } else {
        appStore.showToast('已取消点赞', 'info')
      }
    } else {
      appStore.showToast(res.msg || '操作失败', 'error')
    }
  } else {
    if (result.type === 'cooldown') {
      appStore.showToast(result.message, 'warning')
    } else if (result.type === 'spam') {
      if (result.forcedUnlike) {
        post.value.isLiked = false
        post.value.likeCount = Math.max(0, originalCount - 1)
      }
      appStore.showToast(result.message, 'warning')
    } else if (result.type === 'loading') {
      appStore.showToast(result.message, 'warning')
    } else {
      appStore.showToast(result.message || '操作失败', 'error')
    }
  }
}

const handleCollect = async () => {
  if (!currentUser.value) return appStore.showToast('请登录', 'error')
  if (collectLoading.value) return // 防止重复点击

  collectLoading.value = true
  try {
    await toggleCollection({ userId: currentUser.value.id, postId: post.value.id })
    isCollected.value = !isCollected.value
    appStore.showToast(isCollected.value ? '⭐ 已收藏' : '已取消收藏', 'success')
  } catch (e) {
    appStore.showToast('操作失败', 'error')
  } finally {
    collectLoading.value = false
  }
}

const openReportDialog = () => {
  if (!currentUser.value) return appStore.showToast('请登录', 'error')
  reportType.value = ''
  reportDesc.value = ''
  showReportDialog.value = true
}
const confirmReport = async () => {
  if (!reportType.value) return appStore.showToast('请选择举报类型', 'error')
  const fullReason = reportType.value + (reportDesc.value ? `: ${reportDesc.value}` : '')
  try {
    await submitReport({ userId: currentUser.value.id, postId: post.value.id, reason: fullReason })
    showReportDialog.value = false
    appStore.showToast('举报成功，正义值 +1 🛡️', 'success')
  } catch (e) { appStore.showToast('举报失败', 'error') }
}

onMounted(() => {
  loadData()
  //监听移动端键盘弹出
  if (window.visualViewport) {
    window.visualViewport.addEventListener('resize', handleVisualViewportResize)
  }
})

// 组件卸载时移除监听并恢复body样式
import { onUnmounted } from 'vue'
onUnmounted(() => {
  //恢复body滚动，防止返回上一页后页面无法滚动/点击
  document.body.style.overflow = 'auto'

  if (window.visualViewport) {
    window.visualViewport.removeEventListener('resize', handleVisualViewportResize)
  }

  // 销毁地图实例
  if (locationMapInstance) {
    locationMapInstance.destroy()
    locationMapInstance = null
  }
})
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-gray-50 via-emerald-50/30 to-teal-50/20 pb-24 relative">
    <!-- 装饰背景 -->
    <div class="fixed inset-0 pointer-events-none overflow-hidden">
      <div
        class="absolute top-40 -right-32 w-80 h-80 bg-gradient-to-br from-emerald-100/40 to-teal-100/40 rounded-full blur-3xl">
      </div>
      <div
        class="absolute bottom-40 -left-32 w-64 h-64 bg-gradient-to-br from-cyan-100/40 to-blue-100/40 rounded-full blur-3xl">
      </div>
    </div>

    <Dialog :open="showReportDialog" @update:open="showReportDialog = $event">
      <DialogContent class="sm:max-w-[500px] bg-white rounded-xl">
        <DialogHeader>
          <DialogTitle class="text-lg font-bold text-red-600 flex items-center gap-2">
            <AlertTriangle class="w-5 h-5" /> 违规举报
          </DialogTitle>
        </DialogHeader>
        <div class="py-4 space-y-4">
          <div><Label class="text-gray-700 mb-2 block">请选择举报类型 (必选)</Label>
            <div class="grid grid-cols-2 gap-2">
              <div v-for="type in reportTypes" :key="type"
                class="border rounded-lg p-3 cursor-pointer transition-all flex items-center gap-2"
                :class="reportType === type ? 'border-red-500 bg-red-50 text-red-600' : 'border-gray-200 hover:border-gray-300'"
                @click="reportType = type">
                <div class="w-4 h-4 rounded-full border flex items-center justify-center"
                  :class="reportType === type ? 'border-red-500' : 'border-gray-300'">
                  <div v-if="reportType === type" class="w-2 h-2 bg-red-500 rounded-full"></div>
                </div><span class="text-sm">{{ type }}</span>
              </div>
            </div>
          </div>
          <div><Label class="text-gray-700 mb-2 block">详细说明 (可选)</Label><Textarea v-model="reportDesc"
              placeholder="请描述具体违规情况... (Enter换行)" class="bg-gray-50 resize-none" /></div>
        </div>
        <DialogFooter><Button variant="outline" @click="showReportDialog = false">取消</Button><Button
            class="bg-red-500 hover:bg-red-600 text-white" @click="confirmReport">提交举报</Button></DialogFooter>
      </DialogContent>
    </Dialog>

    <header
      class="sticky top-0 z-40 bg-white/80 backdrop-blur-md border-b border-white/20 h-14 flex items-center px-4 shadow-lg shadow-black/5">
      <Button variant="ghost" size="icon"
        class="-ml-2 mr-2 hover:bg-emerald-50 hover:text-[rgb(33,111,85)] rounded-full transition-all"
        @click="router.back()">
        <ChevronLeft class="w-6 h-6" />
      </Button>
      <div class="flex items-center gap-3" v-if="post">
        <Avatar class="w-9 h-9 ring-2 ring-white/50 shadow-md cursor-pointer transition-transform hover:scale-105"
          @click="router.push(`/user/${post.userId}`)">
          <AvatarImage :src="post.avatar" />
          <AvatarFallback class="bg-gradient-to-br from-[rgb(33,111,85)] to-emerald-600 text-white">{{
            post.nickname?.[0] }}</AvatarFallback>
        </Avatar>
        <span class="font-bold text-sm bg-gradient-to-r from-gray-800 to-gray-600 bg-clip-text text-transparent">{{
          post.nickname }}</span>
        <Badge v-if="post.isAnonymous" variant="outline"
          class="text-[10px] h-5 px-2 text-gray-400 bg-gray-50 border-gray-200">匿名</Badge>
      </div>
      <div class="ml-auto">
        <DropdownMenu>
          <DropdownMenuTrigger as-child><Button variant="ghost" size="icon" class="rounded-full hover:bg-gray-100">
              <MoreHorizontal class="w-5 h-5 text-gray-600" />
            </Button></DropdownMenuTrigger>
          <DropdownMenuContent align="end" class="bg-white backdrop-blur-md border-white/20 shadow-xl rounded-xl">
            <DropdownMenuItem @click="router.push('/')" class="cursor-pointer hover:bg-emerald-50 text-gray-700">
              <Home class="w-4 h-4 mr-2" /> 返回首页
            </DropdownMenuItem>
            <DropdownMenuItem @click="openReportDialog" class="text-red-600 cursor-pointer hover:bg-red-50">
              <AlertTriangle class="w-4 h-4 mr-2" /> 举报
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </header>

    <div v-if="post"
      class="max-w-3xl mx-auto mt-6 px-4 animate-in fade-in slide-in-from-bottom-4 duration-500 relative z-10">
      <div class="bg-white/80 backdrop-blur-md rounded-3xl shadow-xl p-6 mb-6 border border-white/20">
        <div class="flex justify-between items-center mb-5">
          <Badge
            class="bg-gradient-to-r from-emerald-50 to-teal-50 text-emerald-600 hover:from-emerald-100 hover:to-teal-100 border border-emerald-200 px-4 py-1.5 rounded-full font-medium shadow-sm">
            #
            {{ ['其他', '表白', '寻物', '闲置', '吐槽', '活动', '求助'][post.category] || '话题' }}</Badge>
          <div class="flex items-center gap-3 text-xs text-gray-400">
            <span class="bg-gray-100 px-3 py-1 rounded-full">{{ new Date(post.createTime).toLocaleString() }}</span>
            <span v-if="post.location"
              class="flex items-center gap-1 bg-gradient-to-r from-gray-100 to-gray-50 px-3 py-1 rounded-full text-gray-500 border border-gray-200 cursor-pointer hover:bg-emerald-50 hover:text-emerald-600 hover:border-emerald-200 transition-all"
              @click="openLocationMap(post.location)">
              <MapPin class="w-3 h-3" /> {{ post.location }}
            </span>
          </div>
        </div>
        <!-- eslint-disable-next-line vue/no-v-html -->
        <p class="text-[16px] leading-relaxed text-gray-800 mb-5 whitespace-pre-wrap tracking-wide text-justify post-content"
          style="text-indent: 2em;" v-html="renderContentWithTags(post.content)" @click="handleTagClick"></p>
        <!-- AI 使用声明 -->
        <div v-if="post.isAiAssisted === 1"
          class="mb-5 flex items-center gap-1.5 text-[11px] text-purple-500 bg-purple-50 border border-purple-100 rounded-lg px-3 py-1.5 w-fit">
          <span>⚡</span>
          <span>内容经 AI 协助优化，请注意甄别</span>
        </div>
        <!-- 九宫格媒体展示 - 图片+视频统一展示 -->
        <div v-if="mediaList.length" class="grid gap-1.5 mb-4" :class="getMediaGridClass(mediaList.length)">
          <div v-for="(media, idx) in mediaList" :key="idx"
            class="relative overflow-hidden bg-gray-100 cursor-pointer group rounded-lg"
            :class="mediaList.length === 1 ? 'aspect-auto max-h-[350px]' : 'aspect-square'"
            @click="openMediaPreview(idx)">
            <!-- 图片 -->
            <img v-if="media.type === 'image'" :src="media.url" loading="lazy"
              class="w-full h-full transition-transform duration-300 group-hover:scale-105"
              :class="mediaList.length === 1 ? 'object-contain' : 'object-cover'"
              @error="(e) => e.target.src = '/图片加载失败.png'" />
            <!-- 视频缩略图 -->
            <div v-else class="w-full h-full relative">
              <video :src="media.url" class="w-full h-full object-cover" preload="metadata"></video>
              <!-- 播放按钮遮罩 -->
              <div
                class="absolute inset-0 bg-black/30 flex items-center justify-center group-hover:bg-black/40 transition-colors">
                <div class="w-12 h-12 bg-white/90 rounded-full flex items-center justify-center shadow-lg">
                  <div
                    class="w-0 h-0 border-t-[8px] border-t-transparent border-l-[14px] border-l-gray-800 border-b-[8px] border-b-transparent ml-1">
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div v-if="post.parsedPoll" class="mt-2 mb-4 space-y-2 bg-gray-50 p-3 rounded-xl border border-gray-100">
          <div class="flex items-center justify-between mb-2">
            <div class="text-xs font-bold text-gray-500 flex items-center gap-1">
              <BarChart2 class="w-3 h-3" /> 投票
              <span class="text-gray-400 font-normal ml-1">{{ totalVotes }}人参与</span>
            </div>
            <div v-if="pollEndTime" class="text-xs" :class="isPollExpired ? 'text-red-500' : 'text-gray-400'">
              {{ isPollExpired ? '已截止' : '截止: ' + new Date(pollEndTime).toLocaleString('zh-CN', {
                month: 'numeric', day:
                  'numeric', hour: '2-digit', minute: '2-digit'
              }) }}
            </div>
          </div>
          <div v-for="(opt, idx) in post.parsedPoll" :key="idx"
            class="relative h-9 bg-white border rounded-lg overflow-hidden flex items-center text-sm transition-all"
            :class="[
              myVote === idx ? 'border-[rgb(33,111,85)] bg-emerald-50' : 'border-gray-200',
              isPollExpired || myVote !== null ? 'cursor-default' : 'cursor-pointer hover:border-gray-300'
            ]" @click="!isPollExpired && handleVote(idx)">
            <!-- 投票进度条 -->
            <div class="absolute inset-0 bg-emerald-100 transition-all" :style="{ width: getVotePercent(idx) + '%' }">
            </div>
            <div class="relative z-10 flex items-center justify-between w-full px-4">
              <div class="flex items-center gap-2">
                <span v-if="myVote === idx" class="text-[rgb(33,111,85)]">✓</span>
                <span class="text-gray-700">{{ opt }}</span>
              </div>
              <span class="text-xs text-gray-500">{{ getVotePercent(idx) }}%</span>
            </div>
          </div>
        </div>
        <div class="flex items-center justify-between pt-5 border-t border-gray-100/50 text-sm text-gray-500">
          <div class="flex items-center gap-8">
            <span
              class="flex items-center gap-2 px-3 py-1.5 rounded-full bg-gray-50 hover:bg-gray-100 transition-colors">
              <Eye class="w-4 h-4" /> <span class="font-medium">{{ post.viewCount }}</span>
            </span>
            <span
              class="flex items-center gap-2 px-3 py-1.5 rounded-full cursor-pointer transition-all duration-300 hover:scale-105"
              :class="post.isLiked ? 'bg-pink-50 text-pink-500' : 'bg-gray-50 hover:bg-pink-50 hover:text-pink-500'"
              @click="handleLike">
              <Heart class="w-4 h-4" :class="{ 'fill-current animate-pulse': post.isLiked }" />
              <span class="font-medium">{{ post.likeCount }}</span>
            </span>
            <span
              class="flex items-center gap-2 px-3 py-1.5 rounded-full cursor-pointer transition-all duration-300 hover:scale-105"
              :class="isCollected ? 'bg-yellow-50 text-yellow-500' : 'bg-gray-50 hover:bg-yellow-50 hover:text-yellow-500'"
              @click="handleCollect">
              <Star class="w-4 h-4" :class="{ 'fill-current': isCollected }" />
              <span class="font-medium">{{ isCollected ? '已收藏' : '收藏' }}</span>
            </span>
          </div>
          <Button variant="ghost" size="sm"
            class="text-gray-500 hover:text-[rgb(33,111,85)] hover:bg-emerald-50 rounded-full px-4 py-2 font-medium transition-all">
            <Share2 class="w-4 h-4 mr-1.5" /> 分享
          </Button>
        </div>
      </div>

      <!-- 评论区 - 简洁设计 -->
      <div class="bg-white/80 backdrop-blur-md rounded-2xl shadow-lg p-5 mb-8 border border-white/20">
        <div class="flex items-center justify-between mb-4">
          <h3 class="font-bold text-gray-800 flex items-center text-base">
            <MessageCircle class="w-5 h-5 mr-2 text-[rgb(33,111,85)]" />
            评论 <span class="text-gray-400 text-sm ml-1 font-normal">{{ comments.length }}</span>
          </h3>
        </div>

        <!-- 评论列表 -->
        <div class="space-y-4">
          <div v-for="comment in comments" :key="comment.id"
            class="p-3 bg-gray-50/80 rounded-xl hover:bg-gray-100/80 transition-colors">
            <div class="flex gap-3">
              <Avatar class="w-9 h-9 cursor-pointer flex-shrink-0" @click="router.push(`/user/${comment.userId}`)">
                <AvatarImage :src="comment.avatar" />
                <AvatarFallback class="bg-emerald-100 text-emerald-700 text-sm">{{ comment.nickname?.[0] }}
                </AvatarFallback>
              </Avatar>
              <div class="flex-1 min-w-0">
                <div class="flex items-center justify-between mb-1">
                  <span class="text-sm font-medium text-gray-800">{{ comment.nickname }}</span>
                  <span class="text-xs text-gray-400">{{ formatTime(comment.createTime) }}</span>
                </div>
                <p class="text-gray-700 text-sm leading-relaxed break-words">{{ comment.content }}</p>
                <!-- 评论图片 - 缩略图样式 -->
                <div v-if="comment.imgUrl"
                  class="mt-2 w-16 h-16 rounded-lg overflow-hidden cursor-pointer group relative"
                  @click="openCommentImgLightbox(comment.imgUrl)">
                  <img :src="comment.imgUrl" loading="lazy"
                    class="w-full h-full object-cover transition-transform group-hover:scale-110"
                    @error="(e) => e.target.parentElement.style.display = 'none'" />
                  <div class="absolute inset-0 bg-black/0 group-hover:bg-black/10 transition-colors"></div>
                </div>
                <!-- 操作按钮 -->
                <div class="flex items-center gap-3 mt-2">
                  <button class="text-xs text-gray-400 hover:text-[rgb(33,111,85)] transition-colors"
                    @click="handleReply(comment, comment.id)">回复</button>
                  <button v-if="comment.children && comment.children.length > 0"
                    class="text-xs text-[rgb(33,111,85)] hover:underline"
                    @click="comment.showReplies = !comment.showReplies">
                    {{ comment.showReplies ? '收起' : `${comment.children.length}条回复` }}
                  </button>
                </div>
                <!-- 子评论 -->
                <div v-if="comment.showReplies && comment.children && comment.children.length"
                  class="mt-3 space-y-2 pl-2 border-l-2 border-emerald-100">
                  <div v-for="reply in comment.children" :key="reply.id" class="flex gap-2 p-2 bg-white/60 rounded-lg">
                    <Avatar class="w-6 h-6 cursor-pointer flex-shrink-0" @click="router.push(`/user/${reply.userId}`)">
                      <AvatarImage :src="reply.avatar" />
                      <AvatarFallback class="text-xs bg-gray-100">{{ reply.nickname?.[0] }}</AvatarFallback>
                    </Avatar>
                    <div class="flex-1 min-w-0">
                      <div class="flex items-center gap-1 flex-wrap">
                        <span class="text-xs font-medium text-gray-700">{{ reply.nickname }}</span>
                        <span class="text-xs text-gray-400">回复</span>
                        <span class="text-xs font-medium text-[rgb(33,111,85)]">@{{ reply.replyNickname }}</span>
                      </div>
                      <p class="text-gray-600 text-xs leading-relaxed mt-0.5 break-words">{{ reply.content }}</p>
                      <!-- 子评论图片 - 缩略图样式 -->
                      <div v-if="reply.imgUrl"
                        class="mt-1.5 w-12 h-12 rounded overflow-hidden cursor-pointer group relative"
                        @click="openCommentImgLightbox(reply.imgUrl)">
                        <img :src="reply.imgUrl" loading="lazy"
                          class="w-full h-full object-cover transition-transform group-hover:scale-110"
                          @error="(e) => e.target.parentElement.style.display = 'none'" />
                        <div class="absolute inset-0 bg-black/0 group-hover:bg-black/10 transition-colors"></div>
                      </div>
                      <button class="text-[10px] text-gray-400 mt-1 hover:text-[rgb(33,111,85)]"
                        @click="handleReply(reply, comment.id)">回复</button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 空状态 -->
          <div v-if="comments.length === 0" class="text-center py-10 text-gray-400">
            <MessageCircle class="w-12 h-12 mx-auto mb-3 opacity-20" />
            <p class="text-sm">暂无评论，快来抢沙发~</p>
          </div>
        </div>
      </div>
    </div>

    <div
      class="fixed bottom-0 left-0 w-full bg-white/90 backdrop-blur-xl border-t border-white/20 p-4 pb-6 z-50 shadow-[0_-8px_30px_rgba(0,0,0,0.08)] transition-all"
      :style="inputBarStyle">

      <div class="max-w-3xl mx-auto flex gap-3 items-end relative">
        <div
          class="relative flex-1 bg-gray-50 rounded-2xl px-4 py-3 border-2 border-transparent focus-within:bg-white focus-within:border-[rgb(33,111,85)] focus-within:shadow-lg focus-within:shadow-emerald-500/10 transition-all duration-300">
          <!-- 回复提示 -->
          <div v-if="replyingTo"
            class="inline-flex items-center gap-1 bg-white px-2 py-0.5 rounded-md text-xs text-[rgb(33,111,85)] mb-1 shadow-sm border border-gray-100">
            <span>回复 @{{ replyingTo.nickname }}</span>
            <X class="w-3 h-3 cursor-pointer hover:text-red-500" @click="cancelReply" />
          </div>
          <!-- 图片预览 -->
          <div v-if="commentImgPreview" class="mb-2 relative inline-block">
            <img :src="commentImgPreview" class="h-16 w-16 object-cover rounded-lg border border-gray-200" />
            <div v-if="commentImgUploading"
              class="absolute inset-0 bg-black/40 rounded-lg flex items-center justify-center">
              <Loader2 class="w-5 h-5 text-white animate-spin" />
            </div>
            <button v-else @click="clearCommentImg"
              class="absolute -top-1.5 -right-1.5 bg-black/60 text-white rounded-full p-0.5 hover:bg-red-500 transition">
              <X class="w-3 h-3" />
            </button>
          </div>
          <!-- 输入区域 - 问题4修复：更直观的评论字数限制提示 -->
          <div class="flex items-center">
            <input id="cmt-input" v-model="commentContent" :placeholder="replyingTo ? '' : '写下你的评论... (Enter发送)'"
              maxlength="500" class="bg-transparent border-none outline-none w-full text-sm h-6"
              @keyup.enter="handleSubmit" @focus="scrollToInput" @blur="handleInputBlur" />
            <span class="text-xs mr-2 whitespace-nowrap transition-colors px-2 py-0.5 rounded-full"
              :class="commentContent.length > 480 ? 'text-red-600 font-bold bg-red-50 animate-pulse' : commentContent.length > 450 ? 'text-orange-600 font-medium bg-orange-50' : commentContent.length > 400 ? 'text-yellow-600 bg-yellow-50' : 'text-gray-400'">
              {{ commentContent.length }}/500
              <span v-if="commentContent.length > 480" class="ml-1">⚠️</span>
            </span>
            <!-- 图片按钮 -->
            <ImagePlus class="w-5 h-5 text-gray-400 cursor-pointer hover:text-[rgb(33,111,85)] flex-shrink-0 transition"
              :class="{ 'text-[rgb(33,111,85)]': commentImgPreview }" @click="handleCommentImgSelect" />
            <!-- 表情选择器 -->
            <div class="ml-2">
              <EmojiPicker @select="addEmoji" />
            </div>
          </div>
        </div>
        <!-- 隐藏的文件输入 -->
        <input type="file" ref="commentImgInput" accept="image/*" class="hidden" @change="handleCommentImgChange" />
        <!-- 发送按钮 -->
        <Button size="icon"
          class="rounded-full bg-gradient-to-r from-[rgb(33,111,85)] to-emerald-600 hover:from-[rgb(28,95,72)] hover:to-emerald-700 shadow-lg shadow-emerald-500/25 flex-shrink-0 w-11 h-11 transition-all duration-300 hover:scale-105 active:scale-95"
          :disabled="(!commentContent.trim() && !commentImgUrl) || commentImgUploading" @click="handleSubmit">
          <Send class="w-4 h-4 text-white" />
        </Button>
      </div>
    </div>

    <!-- 媒体预览弹窗（图片+视频） -->
    <div v-if="showMediaPreview && mediaList.length"
      class="fixed inset-0 z-[100] bg-black/95 flex items-center justify-center animate-in fade-in duration-200"
      @click="closeMediaPreview">
      <!-- 关闭按钮 -->
      <Button variant="ghost" size="icon"
        class="absolute top-4 right-4 text-white/70 hover:text-white hover:bg-white/20 rounded-full z-10"
        @click.stop="closeMediaPreview">
        <X class="w-6 h-6" />
      </Button>

      <!-- 内容区域 -->
      <div class="relative w-full h-full flex items-center justify-center p-4" @click.stop>
        <!-- 图片 -->
        <img v-if="mediaList[mediaPreviewIndex]?.type === 'image'" :src="mediaList[mediaPreviewIndex]?.url"
          class="max-w-full max-h-full object-contain select-none" />
        <!-- 视频 -->
        <video v-else-if="mediaList[mediaPreviewIndex]?.type === 'video'" :src="mediaList[mediaPreviewIndex]?.url"
          controls autoplay class="max-w-full max-h-full object-contain"></video>

        <!-- 左右切换 -->
        <Button v-if="mediaPreviewIndex > 0" variant="ghost" size="icon"
          class="absolute left-4 top-1/2 -translate-y-1/2 text-white/60 hover:text-white hover:bg-white/20 rounded-full w-12 h-12"
          @click.stop="prevMedia">
          <ChevronLeft class="w-8 h-8" />
        </Button>
        <Button v-if="mediaPreviewIndex < mediaList.length - 1" variant="ghost" size="icon"
          class="absolute right-4 top-1/2 -translate-y-1/2 text-white/60 hover:text-white hover:bg-white/20 rounded-full w-12 h-12"
          @click.stop="nextMedia">
          <ChevronRight class="w-8 h-8" />
        </Button>
      </div>

      <!-- 计数 -->
      <div class="absolute bottom-6 left-1/2 -translate-x-1/2 text-white/80 bg-black/50 px-4 py-2 rounded-full text-sm">
        {{ mediaPreviewIndex + 1 }} / {{ mediaList.length }}
      </div>
    </div>

    <!-- 评论图片灯箱 -->
    <div v-if="showCommentImgLightbox"
      class="fixed inset-0 z-[100] bg-black/95 backdrop-blur-sm flex items-center justify-center animate-in fade-in duration-300"
      @click="closeCommentImgLightbox">
      <Button variant="ghost" size="icon"
        class="absolute top-6 right-6 text-white/70 hover:text-white hover:bg-white/20 rounded-full"
        @click.stop="closeCommentImgLightbox">
        <X class="w-8 h-8" />
      </Button>
      <img :src="commentLightboxImg" class="max-h-[90vh] max-w-[90vw] object-contain shadow-2xl rounded-sm select-none"
        @click.stop />
    </div>

    <!-- 位置地图弹窗 -->
    <div v-if="showLocationMap"
      class="fixed inset-0 z-[100] bg-black/60 backdrop-blur-sm flex items-center justify-center animate-in fade-in duration-300"
      :class="locationMapFullscreen ? 'p-0' : 'p-4'" @click="closeLocationMap">
      <div
        class="bg-white shadow-2xl overflow-hidden animate-in zoom-in-95 slide-in-from-bottom-4 duration-300 flex flex-col transition-all"
        :class="locationMapFullscreen ? 'w-full h-full rounded-none' : 'w-full max-w-lg rounded-3xl max-h-[90vh]'"
        @click.stop>
        <!-- 头部 -->
        <div class="bg-gradient-to-r from-[rgb(33,111,85)] to-emerald-600 text-white p-4 shrink-0">
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 bg-white/20 rounded-full flex items-center justify-center">
                <MapPin class="w-5 h-5" />
              </div>
              <div>
                <h3 class="font-bold text-lg">{{ locationName }}</h3>
                <p class="text-white/80 text-xs">双指缩放或滚轮放大地图</p>
              </div>
            </div>
            <div class="flex items-center gap-2">
              <Button variant="ghost" size="icon" class="text-white/80 hover:text-white hover:bg-white/20 rounded-full"
                @click.stop="toggleLocationMapFullscreen" :title="locationMapFullscreen ? '退出全屏' : '全屏查看'">
                <Maximize2 v-if="!locationMapFullscreen" class="w-5 h-5" />
                <Minimize2 v-else class="w-5 h-5" />
              </Button>
              <Button variant="ghost" size="icon" class="text-white/80 hover:text-white hover:bg-white/20 rounded-full"
                @click="closeLocationMap">
                <X class="w-5 h-5" />
              </Button>
            </div>
          </div>
        </div>

        <!-- 地图区域 -->
        <div class="relative bg-gray-100 shrink-0 transition-all"
          :class="locationMapFullscreen ? 'flex-1' : 'h-[280px]'">
          <div id="location-map-container" class="w-full h-full"></div>

          <!-- 加载状态 -->
          <div v-if="locationMapLoading" class="absolute inset-0 bg-white/80 flex flex-col items-center justify-center">
            <div class="w-10 h-10 border-3 border-emerald-500 border-t-transparent rounded-full animate-spin mb-3">
            </div>
            <p class="text-gray-500 text-sm">正在搜索位置...</p>
          </div>

          <!-- 搜索失败提示 -->
          <div v-if="locationSearchFailed && !locationMapLoading"
            class="absolute top-2 left-2 right-2 bg-yellow-50 border border-yellow-200 rounded-lg px-3 py-2 flex items-center gap-2 z-10">
            <AlertTriangle class="w-4 h-4 text-yellow-600 shrink-0" />
            <span class="text-xs text-yellow-700">未找到精确位置，已显示默认区域</span>
          </div>

          <!-- 缩放提示 -->
          <div v-if="!locationMapLoading"
            class="absolute bottom-2 left-2 bg-black/50 text-white text-[10px] px-2 py-1 rounded-full z-10">
            双指/滚轮缩放地图
          </div>
        </div>

        <!-- 周边信息 -->
        <div class="p-4 max-h-[200px] overflow-y-auto">
          <div v-if="nearbyPOIs.length > 0">
            <h4 class="text-xs font-bold text-gray-500 mb-3 flex items-center gap-1">
              <span class="w-1 h-3 bg-emerald-500 rounded-full"></span>
              周边地点
            </h4>
            <div class="space-y-2">
              <div v-for="(poi, idx) in nearbyPOIs" :key="idx"
                class="flex items-center gap-3 p-3 bg-gray-50 rounded-xl cursor-pointer hover:bg-emerald-50 hover:border-emerald-200 border border-transparent transition-all group"
                @click="selectPOI(poi)">
                <div
                  class="w-8 h-8 bg-white rounded-full flex items-center justify-center shadow-sm text-emerald-600 font-bold text-sm group-hover:bg-emerald-500 group-hover:text-white transition-colors">
                  {{ idx + 1 }}
                </div>
                <div class="flex-1 min-w-0">
                  <div class="text-sm font-medium text-gray-800 truncate">{{ poi.name }}</div>
                  <div class="text-xs text-gray-500 truncate">{{ poi.address }}</div>
                </div>
                <div v-if="poi.distance" class="text-xs text-gray-400 whitespace-nowrap">
                  {{ poi.distance > 1000 ? (poi.distance / 1000).toFixed(1) + 'km' : poi.distance + 'm' }}
                </div>
              </div>
            </div>
          </div>

          <div v-else-if="!locationMapLoading" class="text-center py-6 text-gray-400">
            <MapPin class="w-8 h-8 mx-auto mb-2 opacity-30" />
            <p class="text-sm">未找到精确位置信息</p>
          </div>
        </div>

        <!-- 底部操作 -->
        <div class="p-4 border-t border-gray-100 flex gap-3">
          <Button variant="outline" class="flex-1 rounded-xl h-11 border-gray-200 hover:bg-gray-50"
            @click="closeLocationMap">
            关闭
          </Button>
          <Button
            class="flex-1 rounded-xl h-11 bg-gradient-to-r from-[rgb(33,111,85)] to-emerald-600 hover:from-[rgb(28,95,72)] hover:to-emerald-700 text-white shadow-lg shadow-emerald-500/20"
            :disabled="!locationCoords" @click="openNavigation">
            <MapPin class="w-4 h-4 mr-2" />
            导航到这里
          </Button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.no-scrollbar::-webkit-scrollbar {
  display: none;
}

.slide-fade-enter-active {
  transition: all 0.3s ease-out;
}

.slide-fade-leave-active {
  transition: all 0.2s cubic-bezier(1, 0.5, 0.8, 1);
}

.slide-fade-enter-from,
.slide-fade-leave-to {
  transform: translateY(20px);
  opacity: 0;
}

/* 标签高亮样式 */
.post-content :deep(.tag-highlight) {
  color: rgb(33, 111, 85);
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.post-content :deep(.tag-highlight:hover) {
  color: rgb(16, 185, 129);
  text-decoration: underline;
}
</style>