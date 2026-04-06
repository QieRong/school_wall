<script setup>
import { ref, onMounted, onActivated, onDeactivated, onUnmounted, computed, reactive, nextTick, watch } from 'vue'

//定义组件名，用于keep-alive缓存
defineOptions({ name: 'Home' })
import { useRouter } from 'vue-router'
import { useDialogKeyboard } from '@/composables/useKeyboard'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle, CardFooter } from '@/components/ui/card'
import { Avatar, AvatarImage, AvatarFallback } from '@/components/ui/avatar'
import { Badge } from '@/components/ui/badge'
import { Input } from '@/components/ui/input'
import { Separator } from '@/components/ui/separator'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import {
  DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger, DropdownMenuLabel, DropdownMenuSeparator
} from '@/components/ui/dropdown-menu'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import {
  Heart, MessageCircle, Share2, MoreHorizontal,
  Search, TrendingUp, Bell, User,
  Flame, Menu, ShoppingBag, HelpCircle, LayoutGrid, X, Maximize2, Minimize2, BookOpen, Coffee, Hash
} from 'lucide-vue-next'
import * as LucideIcons from 'lucide-vue-next'
import request from '@/api/request'
import { getPostList } from '@/api/post'
import { submitReport } from '@/api/report'
import { getUserProfile } from '@/api/user'
import PostModal from '@/components/PostModal.vue'
import NotificationBell from '@/components/NotificationBell.vue'
import SkeletonLoader from '@/components/SkeletonLoader.vue'
import ListEndIndicator from '@/components/ListEndIndicator.vue'
import LazyImage from '@/components/LazyImage.vue'
import BackToTop from '@/components/BackToTop.vue'
import { handleImageError } from '@/utils/imageUtils'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { useWsStore } from '@/stores/ws'
import { useLikeWithDebounce } from '@/composables/useLikeWithDebounce'
import { DEFAULT_CENTER, loadAmapScript, initAmapSecurity } from '@/utils/mapConfig'

const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()
const wsStore = useWsStore()
const posts = ref([])
const hotPosts = ref([])
const announcements = ref([])
const banners = ref([])  // 轮播图数据
const currentBannerIndex = ref(0)  // 当前轮播图索引
let bannerTimer = null  // 轮播定时器
const loading = ref(false)
const searchQuery = ref('')
const showPostModal = ref(false)
const showLogoutDialog = ref(false)
const showMobileSidebar = ref(false)
const currentAnnouncement = ref(null)
const showAnnouncementDetail = ref(false) // 公告详情弹窗

const showReportDialog = ref(false)
const reportTargetId = ref(null)
const reportType = ref('')
const reportDesc = ref('')
const reportTypes = ['垃圾广告', '辱骂攻击', '色情低俗', '涉政敏感', '欺诈骗钱', '其他原因']

// 位置地图弹窗
const showLocationMap = ref(false)
const locationName = ref('')
const locationCoords = ref(null)
const locationMapLoading = ref(true)
const nearbyPOIs = ref([])
let locationMapInstance = null

// 导航菜单缩放控制
const categoriesVisible = ref(true)  // 分类按钮是否完全可见
const categoriesRef = ref(null)       // 分类按钮容器的引用

const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0, pages: 0, jumpPage: '' })

// 手动跳转页码
const handleJumpPage = () => {
  const page = parseInt(pagination.jumpPage)
  if (isNaN(page) || page < 1 || page > pagination.pages) {
    appStore.showToast(`请输入1-${pagination.pages}之间的页码`, 'error')
    return
  }
  pagination.jumpPage = ''
  changePage(page)
}

// 搜索历史
const searchHistory = ref([])
const showSearchHistory = ref(false)
const MAX_SEARCH_HISTORY = 10

// 加载搜索历史
const loadSearchHistory = () => {
  try {
    const saved = localStorage.getItem('search_history')
    if (saved) searchHistory.value = JSON.parse(saved).slice(0, MAX_SEARCH_HISTORY)
  } catch (e) { searchHistory.value = [] }
}

// 保存搜索历史
const saveSearchHistory = (keyword) => {
  if (!keyword || !keyword.trim()) return
  const cleanKeyword = keyword.trim()
  const list = searchHistory.value.filter(k => k !== cleanKeyword)
  list.unshift(cleanKeyword)
  searchHistory.value = list.slice(0, MAX_SEARCH_HISTORY)
  localStorage.setItem('search_history', JSON.stringify(searchHistory.value))
}

// 清空搜索历史
const clearSearchHistory = () => {
  searchHistory.value = []
  localStorage.removeItem('search_history')
  appStore.showToast('搜索历史已清空', 'success')
}

// 使用历史搜索
const useHistorySearch = (keyword) => {
  searchQuery.value = keyword
  showSearchHistory.value = false
  fetchPosts(1)
}

// 删除单条历史
const removeHistoryItem = (keyword) => {
  searchHistory.value = searchHistory.value.filter(k => k !== keyword)
  localStorage.setItem('search_history', JSON.stringify(searchHistory.value))
}

const allCategories = ref([])

// 图标名称转换（支持横杠命名：graduation-cap -> GraduationCap）
const getLucideIcon = (name) => {
  if (!name) return LucideIcons.Hash
  const pascalName = name.split('-').map(w => w.charAt(0).toUpperCase() + w.slice(1).toLowerCase()).join('')
  return LucideIcons[pascalName] || LucideIcons[name] || LucideIcons.Hash
}

const categories = computed(() => {
  const styleMap = {
    1: { color: 'text-pink-500', bg: 'bg-pink-50' },
    2: { color: 'text-blue-500', bg: 'bg-blue-50' },
    3: { color: 'text-orange-500', bg: 'bg-orange-50' },
    4: { color: 'text-purple-500', bg: 'bg-purple-50' },
    5: { color: 'text-green-500', bg: 'bg-green-50' },
    6: { color: 'text-cyan-500', bg: 'bg-cyan-50' },
    7: { color: 'text-red-500', bg: 'bg-red-50' },
    8: { color: 'text-gray-500', bg: 'bg-gray-50' },
  }
  return allCategories.value.map(c => ({
    ...c,
    icon: getLucideIcon(c.icon),
    ...(styleMap[c.id] || { color: 'text-emerald-500', bg: 'bg-emerald-50' }) // 新增类型的默认高亮样式
  }))
})

// 从后端获取分类
const fetchCategories = async () => {
  try {
    const res = await request.get('/index/categories')
    if (res.code === '200') allCategories.value = res.data || []
  } catch (e) {
    console.error('加载分区失败', e)
  }
}

const currentFeedType = ref(0)  // 0=推荐广场, 1=我的关注, null=未选择
const currentCategory = ref(null)  // 分类ID, null=未选择
const currentUser = computed(() => userStore.user)

// 首页公告：优先置顶，再按时间取最新，总共最多3条
const displayedAnnouncements = computed(() => {
  const topped = announcements.value.filter(a => a.isTop)
  const normal = announcements.value.filter(a => !a.isTop)
  const result = [...topped, ...normal]
  return result.slice(0, 3)
})

const checkAuth = (actionName = '操作') => {
  if (!currentUser.value) {
    appStore.showToast(`游客无法进行${actionName}，请先登录`, 'error')
    return false
  }
  return true
}


const fetchPosts = async (page = 1) => {
  loading.value = true
  try {
    const currentUserId = currentUser.value?.id || 0
    const res = await getPostList(page, pagination.pageSize, currentUserId, currentFeedType.value, currentCategory.value, searchQuery.value, 0)
    if (res.code === '200') {
      const pageInfo = res.data
      posts.value = pageInfo.list
      pagination.total = pageInfo.total
      pagination.pages = pageInfo.pages
      pagination.pageNum = pageInfo.pageNum

      posts.value.forEach(p => {
        //兼容JSON和逗号分隔两种格式
        if (p.images) {
          try {
            p.imgList = p.images.startsWith('[') ? JSON.parse(p.images) : p.images.split(',')
          } catch (e) {
            p.imgList = p.images.split(',')
          }
        } else {
          p.imgList = []
        }
        if (p.pollOptions) {
          try { p.parsedPoll = JSON.parse(p.pollOptions) } catch (e) { }
          //加载投票结果
          fetchVoteResult(p)
        }
      })
    }
  } catch (error) {
    console.error('帖子加载失败:', error)
  } finally {
    loading.value = false
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

const fetchHotPosts = async () => {
  try { const res = await request.get('/post/hot'); if (res.code === '200') hotPosts.value = res.data } catch (e) { }
}
const fetchAnnouncements = async () => {
  try { const res = await request.get('/index/announcements'); if (res.code === '200') announcements.value = res.data } catch (e) { }
}

// 获取轮播图
const fetchBanners = async () => {
  try {
    const res = await request.get('/index/banner')
    if (res.code === '200') {
      banners.value = res.data
      startBannerTimer()
    }
  } catch (e) { }
}

// 轮播图自动播放
const startBannerTimer = () => {
  if (bannerTimer) clearInterval(bannerTimer)
  if (banners.value.length > 1) {
    bannerTimer = setInterval(() => {
      currentBannerIndex.value = (currentBannerIndex.value + 1) % banners.value.length
    }, 4000)
  }
}

// 切换轮播图
const changeBanner = (index) => {
  currentBannerIndex.value = index
  startBannerTimer()  // 重置定时器
}

// 上一张/下一张
const prevBanner = () => {
  currentBannerIndex.value = (currentBannerIndex.value - 1 + banners.value.length) % banners.value.length
  startBannerTimer()
}
const nextBanner = () => {
  currentBannerIndex.value = (currentBannerIndex.value + 1) % banners.value.length
  startBannerTimer()
}

const switchFeed = (type) => {
  if (type === 1 && !currentUser.value) return appStore.showToast('请先登录', 'error')
  currentFeedType.value = type
  currentCategory.value = null  // 清空分类选择
  searchQuery.value = ''
  pagination.jumpPage = ''  // 问题6：清空页码跳转输入框
  fetchPosts(1)
}

const filterCategory = (catId) => {
  currentCategory.value = catId
  currentFeedType.value = null  // 清空推荐/关注选择
  searchQuery.value = ''
  pagination.jumpPage = ''  // 问题6：清空页码跳转输入框
  appStore.showToast(`已切换到：${getCategory(catId)?.name || '全部'}`, 'info')
  fetchPosts(1)
}

// 安全获取分类对象
const getCategory = (catId) => {
  if (catId === null || catId === undefined || catId === 0) return null
  return categories.value.find(c => c.id == catId) || null
}

const handleHotSearch = (post) => {
  // 保存当前页码到sessionStorage
  sessionStorage.setItem('home_page_num', pagination.pageNum)
  sessionStorage.setItem('home_scroll_y', window.scrollY)
  router.push(`/post/${post.id}`)
}

// 处理评论按钮点击，跳转到帖子详情页
const handleCommentClick = (post) => {
  // 保存当前页码和滚动位置
  sessionStorage.setItem('home_page_num', pagination.pageNum)
  sessionStorage.setItem('home_scroll_y', window.scrollY)
  // 跳转到帖子详情页
  router.push(`/post/${post.id}`)
}

const changePage = (newPage) => {
  if (newPage < 1 || newPage > pagination.pages) return
  pagination.jumpPage = '' // 清空跳转输入框
  fetchPosts(newPage)
}

// 使用点赞防抖Composable
const likeApiCall = async (postId, userId) => {
  const res = await request.post(`/post/like/${postId}?userId=${userId}`)
  if (res.code === '200') {
    return res.data
  }
  throw new Error(res.msg || '操作失败')
}

const { handleLike: likeHandler, isLoading: isLikeLoading, getCooldownRemaining } = useLikeWithDebounce(likeApiCall)

const shareLoadingMap = reactive({})

const handleLike = async (post) => {
  if (!checkAuth('点赞')) return

  // 保存原始状态
  const originalLiked = post.isLiked
  const originalCount = post.likeCount || 0

  // 调用防抖处理器
  const result = await likeHandler(post.id, currentUser.value.id, post.isLiked)

  if (!result.success) {
    // 处理各种失败情况
    if (result.type === 'cooldown') {
      appStore.showToast(`⏰ ${result.message}`, 'warning')
    } else if (result.type === 'spam') {
      // 反作弊触发,强制取消点赞
      if (result.forcedUnlike) {
        post.isLiked = false
        post.likeCount = Math.max(0, originalCount - 1)
      }
      appStore.showToast(`⚠️ ${result.message}`, 'error')
    } else if (result.type === 'loading') {
      // 静默拒绝,不提示
      return
    } else {
      appStore.showToast(result.message || '操作失败', 'error')
    }
    return
  }

  // 成功:更新UI
  const isLiked = result.data
  post.isLiked = isLiked

  if (isLiked) {
    post.likeCount = originalCount + 1
    appStore.showToast('❤️ 点赞成功', 'success')
  } else {
    post.likeCount = Math.max(0, originalCount - 1)
    appStore.showToast('已取消点赞', 'info')
  }
}

const handleShare = async (post) => {
  if (!checkAuth('分享')) return
  if (shareLoadingMap[post.id]) return // 防止重复点击

  shareLoadingMap[post.id] = true
  try {
    post.shareCount = (post.shareCount || 0) + 1
    await request.post(`/post/share/${post.id}`)
    navigator.clipboard.writeText(window.location.href + `/post/${post.id}`)
    appStore.showToast('🔗 链接已复制，快去分享吧！', 'success')
  } catch (e) {
    post.shareCount--
  } finally {
    shareLoadingMap[post.id] = false
  }
}

// 渲染带标签高亮的内容
const renderContentWithTags = (content) => {
  if (!content) return ''
  return content.replace(/#([^\s#]+)/g, '<span class="tag-highlight" data-tag="$1">#$1</span>')
}

// 点击标签跳转搜索
const handleTagClick = (e, post) => {
  if (e.target.classList.contains('tag-highlight')) {
    e.stopPropagation()
    const tag = e.target.dataset.tag
    searchKeyword.value = tag
    loadPosts()
  }
}

const handleVote = async (post, index) => {
  if (!checkAuth('投票')) return

  // 检查是否已投票
  if (post.myVote !== null && post.myVote !== undefined) {
    appStore.showToast('您已经投过票了', 'warning')
    return
  }

  // 检查投票是否截止
  if (post.pollEndTime && new Date(post.pollEndTime) <= new Date()) {
    appStore.showToast('投票已截止', 'error')
    return
  }

  // 防止重复点击
  if (post.voting) return
  post.voting = true

  try {
    const res = await request.post('/post/vote', { postId: post.id, userId: currentUser.value.id, index })
    if (res.code === '200') {
      post.voteResult = res.data
      post.myVote = index
      appStore.showToast('投票成功', 'success')
    } else {
      appStore.showToast(res.msg || '投票失败', 'error')
    }
  } catch (e) {
    appStore.showToast(e.msg || '投票失败', 'error')
  } finally {
    post.voting = false
  }
}

// 获取投票结果
const fetchVoteResult = async (post) => {
  if (!post.parsedPoll) return
  try {
    const res = await request.get('/post/vote/result', {
      params: { postId: post.id, userId: currentUser.value?.id || 0 }
    })
    if (res.code === '200') {
      post.voteResult = res.data.counts
      post.myVote = res.data.myVote
    }
  } catch (e) { }
}

// 计算投票百分比
const getVotePercent = (post, idx) => {
  if (!post.voteResult || post.voteResult.length === 0) return 0
  const total = post.voteResult.reduce((sum, v) => sum + (v.count || 0), 0)
  if (total === 0) return 0
  const item = post.voteResult.find(v => v.option_index === idx || v.optionIndex === idx)
  return Math.round(((item?.count || 0) / total) * 100)
}

//计算帖子投票总数
const getPostVoteCount = (post) => {
  if (!post.voteResult || post.voteResult.length === 0) return 0
  return post.voteResult.reduce((sum, v) => sum + (v.count || 0), 0)
}

const openReportDialog = (post) => {
  if (!checkAuth('举报')) return
  reportTargetId.value = post.id
  reportType.value = ''
  reportDesc.value = ''
  showReportDialog.value = true
}

const confirmReport = async () => {
  if (!reportType.value) return appStore.showToast('请选择举报类型', 'error')
  const fullReason = reportType.value + (reportDesc.value ? `: ${reportDesc.value}` : '')
  try {
    await submitReport({ userId: currentUser.value.id, postId: reportTargetId.value, reason: fullReason })
    showReportDialog.value = false
    appStore.showToast('举报成功，功德 +1 🛡️', 'success')
  } catch (e) { appStore.showToast('举报失败', 'error') }
}

const handlePublish = () => {
  if (!checkAuth('发布帖子')) return
  showPostModal.value = true
}

// 媒体预览相关
const showMediaLightbox = ref(false)
const lightboxMediaList = ref([])
const lightboxIndex = ref(0)

// Get post media list (images + video)
const getPostMediaList = (post) => {
  const list = []
  if (post.imgList) {
    post.imgList.forEach(url => list.push({ type: 'image', url }))
  }
  if (post.video) {
    list.push({ type: 'video', url: post.video })
  }
  return list.slice(0, 9)
}

// 九宫格布局计算
const getMediaGridClass = (count) => {
  if (count === 1) return 'grid-cols-1 max-w-[260px]'
  if (count === 2) return 'grid-cols-2 max-w-[260px]'
  if (count === 4) return 'grid-cols-2 max-w-[260px]'
  return 'grid-cols-3 max-w-[300px]'
}

// 打开媒体预览
const openMediaPreview = (post, idx) => {
  lightboxMediaList.value = getPostMediaList(post)
  lightboxIndex.value = idx
  showMediaLightbox.value = true
  document.body.style.overflow = 'hidden'
}

// 关闭媒体预览
const closeMediaLightbox = () => {
  showMediaLightbox.value = false
  document.body.style.overflow = 'auto'
}

// 上一个媒体
const prevMedia = () => {
  if (lightboxIndex.value > 0) lightboxIndex.value--
}

// 下一个媒体
const nextMedia = () => {
  if (lightboxIndex.value < lightboxMediaList.value.length - 1) lightboxIndex.value++
}

// 打开位置地图
const showLocationOnMap = async (location) => {
  locationName.value = location
  showLocationMap.value = true
  locationMapLoading.value = true
  locationCoords.value = null
  nearbyPOIs.value = []
  document.body.style.overflow = 'hidden'

  // 等待DOM更新后初始化地图
  await nextTick()
  setTimeout(() => initLocationMap(location), 300)
}

// 初始化位置地图
// 位置地图全屏状态
const locationMapFullscreen = ref(false)
// 位置搜索失败状态
const locationSearchFailed = ref(false)

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

const initLocationMap = async (location) => {
  locationSearchFailed.value = false

  try {
    // 初始化安全配置
    initAmapSecurity()

    // 加载地图脚本
    await loadAmapScript()

    // 创建地图实例 - 添加触摸缩放支持
    locationMapInstance = new window.AMap.Map('home-location-map', {
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
  } catch (error) {
    console.error('地图初始化失败:', error)
    locationMapLoading.value = false
    locationSearchFailed.value = true
  }
}

// 关闭位置地图
const closeLocationMap = () => {
  showLocationMap.value = false
  locationMapFullscreen.value = false  // 重置全屏状态
  locationSearchFailed.value = false   // 重置错误状态
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
    window.open(`https://uri.amap.com/marker?position=${lng},${lat}&name=${encodeURIComponent(locationName.value)}&coordinate=gaode&callnative=1`)
  }
}

const handleLogoutClick = () => showLogoutDialog.value = true
const confirmLogout = () => {
  userStore.logout()
  appStore.showToast('已退出登录', 'success')
  router.replace('/login')
}
const viewAnnouncement = (item) => {
  currentAnnouncement.value = item
  showAnnouncementDetail.value = true
}

// 任务2.2：时间格式化方法
const formatTime = (timeStr) => {
  if (!timeStr) return '近期'
  try {
    const date = new Date(timeStr)
    // 检查日期是否有效
    if (isNaN(date.getTime())) return '近期'
    return date.toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    })
  } catch (e) {
    return '近期'
  }
}

// 添加键盘导航支持
useDialogKeyboard(showLogoutDialog, () => {
  showLogoutDialog.value = false
}, confirmLogout)

useDialogKeyboard(showReportDialog, () => {
  showReportDialog.value = false
})

// 焦点管理：对话框打开时保存当前焦点，关闭时恢复
let previousFocus = null
watch(showReportDialog, (val) => {
  if (val) {
    previousFocus = document.activeElement
  } else if (previousFocus) {
    nextTick(() => {
      previousFocus?.focus()
      previousFocus = null
    })
  }
})

// 监听搜索框变化：当搜索框被清空时，自动回到第一页显示所有帖子
watch(searchQuery, (newVal, oldVal) => {
  // 只有当搜索框从有内容变为空时才触发
  if (oldVal && oldVal.trim() && !newVal.trim()) {
    fetchPosts(1)
  }
})

// 监听来自 ws 的新公告事件
watch(() => wsStore.unreadCount, () => {
  fetchAnnouncements()
})

//保存滚动位置
const savedScrollY = ref(0)

onMounted(() => {
  // 检查是否有保存的页码（从详情页返回）
  const savedPage = sessionStorage.getItem('home_page_num')
  const savedScroll = sessionStorage.getItem('home_scroll_y')

  if (savedPage) {
    // 恢复页码
    fetchPosts(parseInt(savedPage))
    // 清除保存的页码
    sessionStorage.removeItem('home_page_num')

    // 恢复滚动位置
    if (savedScroll) {
      nextTick(() => {
        window.scrollTo(0, parseInt(savedScroll))
        sessionStorage.removeItem('home_scroll_y')
      })
    }
  } else {
    fetchPosts()
  }

  fetchHotPosts()
  fetchAnnouncements()
  fetchBanners()
  loadSearchHistory()
  fetchCategories()

  // 刷新用户数据（关注数/粉丝数/获赞数等）
  if (userStore.isLogged && userStore.user?.id) {
    getUserProfile(userStore.user.id, userStore.user.id).then(res => {
      if (res.code === '200' && res.data) {
        // 用 updateInfo 而非 login，避免覆盖 token
        userStore.updateInfo(res.data)
      }
    }).catch(() => {})
  }

  // 【暂时禁用】监听分类按钮区域的可见性，实现动态缩放
  // 由于会导致布局问题，暂时注释掉
  /*
  let observerTimer = null
  nextTick(() => {
    if (categoriesRef.value) {
      const observer = new IntersectionObserver(
        (entries) => {
          if (observerTimer) clearTimeout(observerTimer)
          observerTimer = setTimeout(() => {
            categoriesVisible.value = entries[0].isIntersecting
          }, 200)
        },
        {
          threshold: 0,
          rootMargin: '-120px 0px 0px 0px'
        }
      )
      const firstCategoryBtn = categoriesRef.value.querySelector('button')
      if (firstCategoryBtn) {
        observer.observe(firstCategoryBtn)
      }
    }
  })
  */
})

//使用keep-alive时恢复滚动位置
onActivated(() => {
  if (savedScrollY.value > 0) {
    window.scrollTo(0, savedScrollY.value)
  }
  // 刷新用户信息(保持显示一致)
  if (userStore.isLogged && userStore.user?.id) {
    getUserProfile(userStore.user.id, userStore.user.id).then(res => {
      if (res.code === '200' && res.data) {
        // 用 updateInfo 而非 login，避免覆盖 token
        userStore.updateInfo(res.data)
      }
    }).catch(() => {})
  }
})

onDeactivated(() => {
  savedScrollY.value = window.scrollY
  //恢复body滚动，防止切换页面后无法滚动/点击
  document.body.style.overflow = 'auto'
  // 关闭所有弹窗
  showMediaLightbox.value = false
  showLocationMap.value = false
  // 销毁地图实例
  if (locationMapInstance) {
    locationMapInstance.destroy()
    locationMapInstance = null
  }
})

//组件卸载时也要恢复body滚动
onUnmounted(() => {
  document.body.style.overflow = 'auto'
  // 销毁地图实例
  if (locationMapInstance) {
    locationMapInstance.destroy()
    locationMapInstance = null
  }
})
</script>

<template>
  <!-- 主容器 - 添加微妙的渐变背景 -->
  <div class="min-h-screen flex flex-col font-sans relative"
    style="background: linear-gradient(135deg, #f0f4f3 0%, #e8eeec 50%, #f5f7f6 100%);">
    <!-- 装饰性背景图案 -->
    <div class="fixed inset-0 pointer-events-none opacity-[0.03]"
      style="background-image: url('data:image/svg+xml,%3Csvg width=&quot;60&quot; height=&quot;60&quot; viewBox=&quot;0 0 60 60&quot; xmlns=&quot;http://www.w3.org/2000/svg&quot;%3E%3Cg fill=&quot;none&quot; fill-rule=&quot;evenodd&quot;%3E%3Cg fill=&quot;%23216f55&quot; fill-opacity=&quot;1&quot;%3E%3Cpath d=&quot;M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z&quot;/%3E%3C/g%3E%3C/g%3E%3C/svg%3E');">
    </div>

    <PostModal :open="showPostModal" @update:open="showPostModal = $event" :user="currentUser"
      @refresh="fetchPosts(1)" />

    <Dialog :open="showLogoutDialog" @update:open="showLogoutDialog = $event">
      <DialogContent class="sm:max-w-[400px] bg-white rounded-xl">
        <DialogHeader>
          <DialogTitle class="text-xl text-gray-800">退出登录</DialogTitle>
        </DialogHeader>
        <div class="py-4 text-gray-600">确定要退出当前账号吗？</div>
        <DialogFooter><Button variant="outline" @click="showLogoutDialog = false">取消</Button><Button
            class="bg-red-500 hover:bg-red-600 text-white border-none" @click="confirmLogout">确认退出</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

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

    <Dialog :open="!!currentAnnouncement" @update:open="currentAnnouncement = null">
      <DialogContent class="sm:max-w-[500px] bg-white rounded-xl">
        <DialogHeader>
          <DialogTitle class="text-lg font-bold text-[rgb(33,111,85)] flex items-center gap-2">
            <Bell class="w-5 h-5" /> {{ currentAnnouncement?.title }}
          </DialogTitle>
        </DialogHeader>
        <div class="py-4 text-gray-700 leading-relaxed whitespace-pre-wrap max-h-[60vh] overflow-y-auto">{{ currentAnnouncement?.content }}</div>
        <div class="flex items-center justify-between mt-2">
          <Button variant="outline" size="sm" class="text-[rgb(33,111,85)] border-[rgb(33,111,85)] hover:bg-emerald-50"
             @click="() => { currentAnnouncement = null; router.push('/notices?tab=1') }">
             查看历史公告
          </Button>
          <div class="text-right text-xs text-gray-400">发布于：{{ currentAnnouncement?.createTime ? new
            Date(currentAnnouncement.createTime).toLocaleDateString() : '近期' }}</div>
        </div>
      </DialogContent>
    </Dialog>

    <header class="sticky top-0 z-50 w-full shadow-md transition-colors duration-300 bg-[rgb(33,111,85)]">
      <div class="container mx-auto px-4 h-16 flex items-center justify-between max-w-7xl">
        <div class="flex items-center gap-3 cursor-pointer group" @click="router.push('/home')">
          <img src="/Round_school_logo.png"
            class="w-10 h-10 drop-shadow-md group-hover:rotate-12 transition-transform duration-500" />
          <div class="flex flex-col"><span
              class="text-xl font-extrabold tracking-wider leading-none text-white">张家界学院</span><span
              class="text-xs text-white/80 tracking-[0.4em] mt-0.5 pl-0.5 font-medium">表白墙</span></div>
        </div>

        <!-- 问题6修复：添加搜索历史功能 -->
        <!-- 问题6修复：添加搜索历史功能 -->
        <div class="hidden md:flex flex-1 max-w-md mx-12 items-center gap-2">
          <div class="relative w-full group">
            <Input v-model="searchQuery" placeholder="🔍 搜索广场帖子..."
              class="pl-4 bg-white/10 border-transparent text-white placeholder:text-white/60 focus:bg-white focus:text-gray-800 focus:placeholder:text-gray-400 rounded-full h-10 transition-all shadow-inner focus:shadow-lg"
              @keyup.enter="() => { saveSearchHistory(searchQuery); fetchPosts(1); showSearchHistory = false }"
              @focus="showSearchHistory = true" @blur="setTimeout(() => showSearchHistory = false, 200)" />
            <!-- 搜索历史下拉框 -->
            <div v-if="showSearchHistory && searchHistory.length > 0 && !searchQuery"
              class="absolute top-full left-0 right-0 mt-2 bg-white rounded-xl shadow-xl border border-gray-100 overflow-hidden z-50 animate-in fade-in slide-in-from-top-2">
              <div class="flex items-center justify-between px-4 py-2 border-b border-gray-100">
                <span class="text-xs font-medium text-gray-500">🕐 搜索历史</span>
                <button class="text-xs text-gray-400 hover:text-red-500 transition-colors"
                  @mousedown.prevent="clearSearchHistory">
                  清空
                </button>
              </div>
              <div class="max-h-60 overflow-y-auto">
                <div v-for="keyword in searchHistory" :key="keyword"
                  class="flex items-center justify-between px-4 py-2.5 hover:bg-gray-50 cursor-pointer group transition-colors"
                  @mousedown.prevent="useHistorySearch(keyword)">
                  <span class="text-sm text-gray-700">{{ keyword }}</span>
                  <X class="w-4 h-4 text-gray-300 opacity-0 group-hover:opacity-100 hover:text-red-500 transition-all"
                    @mousedown.prevent.stop="removeHistoryItem(keyword)" />
                </div>
              </div>
            </div>
          </div>
          
          <!-- 全局搜索入口 -->
           <Button 
            class="bg-white/20 text-white hover:bg-white/30 rounded-full shrink-0" 
            size="icon" 
            title="全站搜索 (搜用户/更多内容)"
            @click="router.push({ path: '/search', query: searchQuery ? { q: searchQuery } : {} })">
            <Search class="w-5 h-5" />
          </Button>
        </div>

        <div class="flex items-center gap-4">
          <template v-if="currentUser">
            <div class="bell-wrapper text-white hover:bg-white/10 rounded-full p-1 transition">
              <NotificationBell :user="currentUser" />
            </div>
            <div class="pl-4 border-l border-white/20">
              <DropdownMenu>
                <DropdownMenuTrigger as-child>
                  <div class="flex items-center gap-3 cursor-pointer group">
                    <Avatar
                      class="h-10 w-10 border-2 border-white/50 group-hover:scale-105 transition-transform shadow-sm">
                      <AvatarImage :src="currentUser.avatar || ''" />
                      <AvatarFallback class="bg-white text-[rgb(33,111,85)] font-bold">{{ currentUser.nickname?.[0] }}
                      </AvatarFallback>
                    </Avatar>
                    <div class="hidden lg:flex flex-col text-right"><span
                        class="font-bold text-white text-sm shadow-sm group-hover:opacity-90">{{ currentUser.nickname
                        }}</span></div>
                  </div>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end"
                  class="w-48 rounded-xl shadow-xl border-gray-100 mt-2 bg-white backdrop-blur-md">
                  <DropdownMenuLabel>我的账号</DropdownMenuLabel>
                  <DropdownMenuSeparator />
                  <DropdownMenuItem class="cursor-pointer" @click="router.push(`/user/${currentUser.id}`)">
                    <User class="w-4 h-4 mr-2" /> 个人中心
                  </DropdownMenuItem>
                  <DropdownMenuItem class="cursor-pointer" @click="router.push(`/message`)">
                    <Mail class="w-4 h-4 mr-2" /> 我的私信
                  </DropdownMenuItem>
                  <DropdownMenuSeparator />
                  <DropdownMenuItem class="cursor-pointer text-red-500 focus:bg-red-50 focus:text-red-600"
                    @click="handleLogoutClick">
                    <LogOut class="w-4 h-4 mr-2" /> 退出登录
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            </div>
          </template>
          <template v-else>
            <Button @click="router.push('/login')"
              class="rounded-full px-6 bg-white text-[rgb(33,111,85)] hover:bg-gray-100 font-bold shadow-lg border-none">登录
              / 注册</Button>
          </template>
          <Button class="md:hidden text-white" variant="ghost" size="icon"
            @click="showMobileSidebar = !showMobileSidebar">
            <Menu class="w-6 h-6" />
          </Button>
        </div>
      </div>
    </header>

    <main class="container mx-auto px-4 py-8 max-w-7xl flex-grow">
      <div class="grid grid-cols-1 md:grid-cols-12 gap-6">

        <div class="hidden md:block md:col-span-3 space-y-6">
          <Card class="border-none shadow-lg bg-white rounded-2xl overflow-hidden">
            <!-- 用户卡片顶部装饰 -->
            <div class="h-28 relative overflow-hidden">
              <div class="absolute inset-0 bg-gradient-to-br from-[rgb(33,111,85)] via-emerald-500 to-teal-500"></div>
              <div
                class="absolute inset-0 bg-[url('data:image/svg+xml,%3Csvg width=&quot;40&quot; height=&quot;40&quot; viewBox=&quot;0 0 40 40&quot; xmlns=&quot;http://www.w3.org/2000/svg&quot;%3E%3Cg fill=&quot;%23ffffff&quot; fill-opacity=&quot;0.1&quot;%3E%3Cpath d=&quot;M20 20c0-5.5-4.5-10-10-10s-10 4.5-10 10 4.5 10 10 10 10-4.5 10-10zm10 0c0-5.5-4.5-10-10-10s-10 4.5-10 10 4.5 10 10 10 10-4.5 10-10z&quot;/%3E%3C/g%3E%3C/svg%3E')]">
              </div>
            </div>
            <div class="px-6 pb-6 text-center -mt-14 relative z-10">
              <Avatar
                class="h-24 w-24 border-4 border-white mx-auto shadow-xl bg-white cursor-pointer ring-4 ring-emerald-100 hover:ring-emerald-200 transition-all"
                @click="currentUser && router.push(`/user/${currentUser.id}`)">
                <AvatarImage :src="currentUser?.avatar || ''" />
                <AvatarFallback
                  class="bg-gradient-to-br from-emerald-50 to-teal-50 text-[rgb(33,111,85)] text-2xl font-bold">
                  <User class="w-12 h-12" />
                </AvatarFallback>
              </Avatar>
              <h3 class="mt-4 font-bold text-xl text-gray-800 truncate">{{ currentUser?.nickname || '游客' }}</h3>
              <p class="text-xs text-gray-500 mt-1 flex items-center justify-center gap-1">
                <span class="w-2 h-2 rounded-full" :class="currentUser ? 'bg-green-400' : 'bg-gray-300'"></span>
                {{ currentUser ? '张院认证用户' : '未登录' }}
              </p>
              <!-- 快捷统计 -->
              <div v-if="currentUser" class="mt-4 pt-4 border-t border-gray-100 grid grid-cols-3 gap-2">
                <div class="text-center cursor-pointer hover:bg-gray-50 rounded-lg py-2 transition-colors"
                  @click="router.push(`/follow/followings/${currentUser.id}`)">
                  <div class="text-lg font-bold text-[rgb(33,111,85)]">{{ currentUser.followingCount || 0 }}</div>
                  <div class="text-[10px] text-gray-400">关注</div>
                </div>
                <div class="text-center cursor-pointer hover:bg-gray-50 rounded-lg py-2 transition-colors"
                  @click="router.push(`/follow/followers/${currentUser.id}`)">
                  <div class="text-lg font-bold text-[rgb(33,111,85)]">{{ currentUser.followerCount || 0 }}</div>
                  <div class="text-[10px] text-gray-400">粉丝</div>
                </div>
                <div class="text-center cursor-pointer hover:bg-gray-50 rounded-lg py-2 transition-colors"
                  @click="router.push(`/user/${currentUser.id}`)">
                  <div class="text-lg font-bold text-pink-500">{{ currentUser.totalLikes || 0 }}</div>
                  <div class="text-[10px] text-gray-400">获赞</div>
                </div>
              </div>
            </div>
          </Card>

          <nav class="sticky top-24 self-start no-scrollbar flex flex-col space-y-2">
            <!-- 推荐广场和我的关注 -->
            <Button variant="ghost"
              class="w-full justify-start gap-3 text-gray-600 hover:shadow-md transition-all rounded-xl h-12 font-medium"
              :class="currentFeedType === 0 ? 'nav-btn-active' : 'bg-white hover:bg-gray-50'"
              @click="switchFeed(0)"><span class="bg-gray-100 p-1.5 rounded-lg text-gray-600">
                <Search class="w-4 h-4" />
              </span> 推荐广场</Button>
            <Button variant="ghost"
              class="w-full justify-start gap-3 text-gray-600 hover:shadow-md transition-all rounded-xl h-12 font-medium"
              :class="currentFeedType === 1 ? 'nav-btn-active' : 'bg-white hover:bg-gray-50'"
              @click="switchFeed(1)"><span class="bg-pink-50 p-1.5 rounded-lg text-pink-500">
                <User class="w-4 h-4" />
              </span> 我的关注</Button>

            <!-- 可缩放部分：分类和特殊功能按钮 -->
            <div class="space-y-2">
              <Separator class="my-2" />
              <div class="text-xs font-bold text-gray-400 px-2 mb-2">分区浏览</div>
              
              <!-- 分类按钮区域（监听目标） -->
              <div ref="categoriesRef">
                <Button v-for="cat in categories" :key="cat.id" variant="ghost"
                  class="w-full justify-start gap-3 text-gray-600 hover:shadow-md transition-all rounded-xl h-12 font-medium"
                  :class="currentCategory === cat.id ? 'nav-btn-active' : 'bg-white hover:bg-gray-50'"
                  @click="filterCategory(cat.id)">
                  <span class="p-1.5 rounded-lg" :class="cat.bg + ' ' + cat.color">
                    <component :is="cat.icon || Heart" class="w-4 h-4" />
                  </span> {{ cat.name }}专区
                </Button>
              </div>

              <Separator class="my-2" />
              <!-- 故事链入口 -->
              <Button variant="ghost"
                class="w-full justify-start gap-3 text-gray-600 hover:shadow-md transition-all rounded-xl h-12 font-medium bg-gradient-to-r from-amber-50 to-orange-50 hover:from-amber-100 hover:to-orange-100"
                @click="router.push('/story')">
                <span class="p-1.5 rounded-lg bg-amber-100 text-amber-600">
                  <BookOpen class="w-4 h-4" />
                </span> 协作故事链
              </Button>
              <!-- 漂流瓶入口 -->
              <Button variant="ghost"
                class="w-full justify-start gap-3 text-gray-600 hover:shadow-md transition-all rounded-xl h-12 font-medium bg-gradient-to-r from-cyan-50 to-blue-50 hover:from-cyan-100 hover:to-blue-100"
                @click="router.push('/bottle')">
                <span class="p-1.5 rounded-lg bg-cyan-100 text-cyan-600">
                  <Mail class="w-4 h-4" />
                </span> 漂流瓶
              </Button>
              <!-- 热词墙入口 -->
              <Button variant="ghost"
                class="w-full justify-start gap-3 text-gray-600 hover:shadow-md transition-all rounded-xl h-12 font-medium bg-gradient-to-r from-purple-50 to-indigo-50 hover:from-purple-100 hover:to-indigo-100"
                @click="router.push('/hotword')">
                <span class="p-1.5 rounded-lg bg-purple-100 text-purple-600">
                  <TrendingUp class="w-4 h-4" />
                </span> 热词墙
              </Button>
            </div>
          </nav>
        </div>

        <div class="col-span-1 md:col-span-6 space-y-6">
          <!-- 轮播图 -->
          <div v-if="banners.length > 0" class="relative rounded-2xl overflow-hidden shadow-lg group">
            <div class="relative h-48 md:h-56 lg:h-64">
              <transition-group name="banner-fade">
                <div v-for="(banner, index) in banners" :key="banner.id" v-show="index === currentBannerIndex"
                  class="absolute inset-0">
                  <LazyImage :src="banner.imageUrl" :alt="banner.title" type="post"
                    class="w-full h-full object-contain bg-gradient-to-br from-gray-100 to-gray-200" />
                  <div class="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent"></div>
                  <div class="absolute bottom-4 left-4 right-4 text-white">
                    <h3 class="text-lg font-bold drop-shadow-lg">{{ banner.title }}</h3>
                  </div>
                </div>
              </transition-group>
            </div>
            <!-- 左右箭头 -->
            <button v-if="banners.length > 1" @click="prevBanner"
              class="absolute left-2 top-1/2 -translate-y-1/2 bg-black/30 hover:bg-black/50 text-white rounded-full p-2 opacity-0 group-hover:opacity-100 transition-opacity">
              <ChevronLeft class="w-5 h-5" />
            </button>
            <button v-if="banners.length > 1" @click="nextBanner"
              class="absolute right-2 top-1/2 -translate-y-1/2 bg-black/30 hover:bg-black/50 text-white rounded-full p-2 opacity-0 group-hover:opacity-100 transition-opacity">
              <ChevronRight class="w-5 h-5" />
            </button>
            <!-- 指示器 -->
            <div v-if="banners.length > 1" class="absolute bottom-2 left-1/2 -translate-x-1/2 flex gap-2">
              <button v-for="(_, index) in banners" :key="index" @click="changeBanner(index)"
                class="w-2 h-2 rounded-full transition-all"
                :class="index === currentBannerIndex ? 'bg-white w-6' : 'bg-white/50 hover:bg-white/80'">
              </button>
            </div>
          </div>

          <Card
            class="border-none shadow-sm rounded-2xl p-5 hover:shadow-md transition-shadow bg-white cursor-pointer group"
            @click="handlePublish">
            <div class="flex gap-4 items-center">
              <Avatar class="h-11 w-11 border border-gray-100">
                <AvatarImage :src="currentUser?.avatar || ''" />
                <AvatarFallback class="bg-gray-100 text-gray-400">
                  <User class="w-6 h-6" />
                </AvatarFallback>
              </Avatar>
              <div
                class="flex-1 bg-gray-50 h-11 flex items-center px-5 text-sm text-gray-400 rounded-2xl hover:bg-gray-100 transition-colors group-hover:text-gray-500 group-hover:bg-gray-100">
                分享你的校园生活... (点击发布)</div>
            </div>
          </Card>

          <!--统一骨架屏组件 -->
          <SkeletonLoader v-if="loading" type="post" :count="3" />

          <template v-else>
            <!--帖子列表为空时的友好提示 -->
            <div v-if="posts.length === 0" class="flex flex-col items-center justify-center py-20 px-6">
              <div
                class="w-32 h-32 bg-gradient-to-br from-emerald-100 to-teal-100 rounded-full flex items-center justify-center shadow-lg mb-6">
                <span class="text-6xl">📭</span>
              </div>
              <h3 class="text-xl font-bold text-gray-700 mb-2">
                {{ searchQuery ? '未找到相关内容' : (currentFeedType === 1 ? '还没有关注的人发布内容' : '暂无内容') }}
              </h3>
              <p class="text-gray-400 text-sm mb-6 text-center max-w-md">
                {{ searchQuery ? '试试其他关键词，或者浏览推荐内容' : (currentFeedType === 1 ? '去发现感兴趣的人，关注他们的动态' : '成为第一个发布内容的人吧！') }}
              </p>
              <div class="flex gap-3 flex-wrap justify-center">
                <Button v-if="searchQuery"
                  class="bg-gradient-to-r from-[rgb(33,111,85)] to-emerald-600 hover:from-[rgb(28,95,72)] hover:to-emerald-700 text-white rounded-full px-6"
                  @click="searchQuery = ''; fetchPosts(1)">
                  🔍 浏览全部内容
                </Button>
                <Button v-else-if="currentFeedType === 1"
                  class="bg-gradient-to-r from-[rgb(33,111,85)] to-emerald-600 hover:from-[rgb(28,95,72)] hover:to-emerald-700 text-white rounded-full px-6"
                  @click="switchFeed(0)">
                  🌟 发现精彩内容
                </Button>
                <Button v-else
                  class="bg-gradient-to-r from-[rgb(33,111,85)] to-emerald-600 hover:from-[rgb(28,95,72)] hover:to-emerald-700 text-white rounded-full px-6"
                  @click="handlePublish">
                  ✍️ 发布第一条内容
                </Button>
                <Button v-if="currentCategory !== 0" variant="outline"
                  class="rounded-full px-6 border-[rgb(33,111,85)] text-[rgb(33,111,85)] hover:bg-emerald-50"
                  @click="filterCategory(0)">
                  📂 查看全部分类
                </Button>
              </div>
            </div>

            <Card v-else v-for="post in posts" :key="post.id"
              class="border-none shadow-sm hover:shadow-md transition-all rounded-2xl bg-white overflow-hidden cursor-pointer"
              @click="() => { sessionStorage.setItem('home_page_num', pagination.pageNum); sessionStorage.setItem('home_scroll_y', window.scrollY); router.push(`/post/${post.id}`) }">
              <CardHeader class="p-5 pb-3 flex flex-row items-start justify-between">
                <div class="flex gap-3">
                  <Avatar
                    class="h-11 w-11 border-2 border-transparent transition-all"
                    :class="post.isAnonymous === 1 ? 'cursor-default' : 'hover:border-[rgb(33,111,85)] cursor-pointer'"
                    @click.stop="post.isAnonymous === 1 ? null : router.push(`/user/${post.userId}`)">
                    <AvatarImage :src="post.avatar || ''" />
                    <AvatarFallback class="bg-gray-50 text-gray-500 font-bold">U</AvatarFallback>
                  </Avatar>
                  <div>
                    <div class="font-bold text-gray-800 text-sm flex items-center gap-2">
                      {{ post.nickname || '匿名用户' }}
                      <template v-if="getCategory(post.category)">
                        <Badge 
                          :class="getCategory(post.category).bg + ' ' + getCategory(post.category).color"
                          class="border-none rounded-md px-2 cursor-pointer hover:opacity-80"
                          @click.stop="filterCategory(post.category)">
                          {{getCategory(post.category).name}}
                        </Badge>
                      </template>
                      <template v-else-if="post.category">
                        <Badge 
                          class="bg-gray-50 text-gray-400 border-none rounded-md px-2 cursor-pointer hover:opacity-80"
                          @click.stop="filterCategory(post.category)">
                          其它专区
                        </Badge>
                      </template>
                    </div>
                    <div class="text-xs text-gray-400 mt-1 flex items-center gap-2">
                      <span>{{ new Date(post.createTime).toLocaleString() }}</span>
                      <span v-if="post.location"
                        class="flex items-center gap-1 bg-gray-100 px-1.5 rounded text-gray-500 cursor-pointer hover:bg-cyan-100 hover:text-cyan-600 transition-colors"
                        @click.stop="showLocationOnMap(post.location)">
                        <MapPin class="w-3 h-3" /> {{ post.location }}
                      </span>
                    </div>
                  </div>
                </div>
                <DropdownMenu>
                  <DropdownMenuTrigger as-child><Button variant="ghost" size="icon"
                      class="h-8 w-8 text-gray-400 hover:bg-gray-100 rounded-full" @click.stop>
                      <MoreHorizontal class="w-4 h-4" />
                    </Button></DropdownMenuTrigger>
                  <DropdownMenuContent align="end" class="bg-white/95 backdrop-blur-sm shadow-xl rounded-xl border border-gray-100 z-50 min-w-[120px]">
                    <DropdownMenuItem @click.stop="openReportDialog(post)" class="text-red-600 cursor-pointer focus:bg-red-50 focus:text-red-700">
                      <AlertTriangle class="w-4 h-4 mr-2" /> 举报
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </CardHeader>

              <CardContent class="px-5 py-2">
                <p class="text-gray-700 leading-relaxed text-[15px] whitespace-pre-line rich-text mb-3 post-content"
                  v-html="renderContentWithTags(post.content)" @click="handleTagClick($event, post)"></p>
                <!-- AI 使用声明 -->
                <div v-if="post.isAiAssisted === 1"
                  class="mb-3 flex items-center gap-1.5 text-[11px] text-purple-500 bg-purple-50 border border-purple-100 rounded-lg px-3 py-1.5 w-fit">
                  <span>⚡</span>
                  <span>内容经 AI 协助优化，请注意甄别</span>
                </div>
                <!-- 九宫格媒体展示 - 图片+视频统一 -->
                <div v-if="getPostMediaList(post).length" class="grid gap-1 mb-3"
                  :class="getMediaGridClass(getPostMediaList(post).length)">
                  <div v-for="(media, idx) in getPostMediaList(post)" :key="idx"
                    class="relative overflow-hidden bg-gray-100 cursor-pointer group rounded-md aspect-square"
                    @click.stop="openMediaPreview(post, idx)">
                    <!-- 图片 -->
                    <LazyImage v-if="media.type === 'image'" :src="media.url" type="post"
                      class="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105" />
                    <!-- 视频缩略图 -->
                    <div v-else class="w-full h-full relative">
                      <video :src="media.url" class="w-full h-full object-cover" preload="metadata"></video>
                      <div
                        class="absolute inset-0 bg-black/30 flex items-center justify-center group-hover:bg-black/40 transition-colors">
                        <div class="w-10 h-10 bg-white/90 rounded-full flex items-center justify-center shadow-lg">
                          <div
                            class="w-0 h-0 border-t-[6px] border-t-transparent border-l-[10px] border-l-gray-800 border-b-[6px] border-b-transparent ml-0.5">
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <div v-if="post.parsedPoll" class="mt-2 mb-4 space-y-2 bg-gray-50 p-3 rounded-xl border border-gray-100"
                  @click.stop>
                  <div class="flex items-center justify-between mb-2">
                    <div class="text-xs font-bold text-gray-500 flex items-center gap-1">
                      <BarChart2 class="w-3 h-3" /> 投票
                      <span class="text-gray-400 font-normal ml-1">{{ getPostVoteCount(post) }}人参与</span>
                    </div>
                    <span v-if="post.myVote !== null && post.myVote !== undefined"
                      class="text-xs text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded">已投票</span>
                  </div>
                  <div v-for="(opt, idx) in post.parsedPoll" :key="idx"
                    class="relative h-9 bg-white border rounded-lg overflow-hidden flex items-center text-sm transition-all"
                    :class="[
                      post.myVote === idx ? 'border-[rgb(33,111,85)] bg-emerald-50' : 'border-gray-200',
                      post.myVote === null || post.myVote === undefined ? 'cursor-pointer hover:border-gray-300' : 'cursor-default'
                    ]" @click.stop="handleVote(post, idx)">
                    <!-- 投票进度条 -->
                    <div v-if="post.voteResult" class="absolute inset-0 bg-emerald-100 transition-all"
                      :style="{ width: getVotePercent(post, idx) + '%' }"></div>
                    <div class="relative z-10 flex items-center justify-between w-full px-4">
                      <div class="flex items-center gap-2">
                        <span v-if="post.myVote === idx" class="text-[rgb(33,111,85)]">✓</span>
                        <span class="text-gray-700">{{ opt }}</span>
                      </div>
                      <span v-if="post.voteResult" class="text-xs text-gray-500">{{ getVotePercent(post, idx) }}%</span>
                      <span v-else class="text-xs text-gray-400">点击投票</span>
                    </div>
                  </div>
                </div>
              </CardContent>

              <CardFooter class="px-2 py-2 border-t border-gray-50 grid grid-cols-3 gap-1 mt-2">
                <Button variant="ghost" :class="[
                  'w-full rounded-xl transition-all',
                  post.isLiked
                    ? 'text-pink-500 bg-pink-50 hover:text-pink-600 hover:bg-pink-100'
                    : 'text-gray-500 hover:text-pink-500 hover:bg-pink-50'
                ]" @click.stop="handleLike(post)">
                  <Heart :class="['w-4 h-4 mr-1.5 transition-all', post.isLiked && 'fill-current']" />
                  <span class="text-xs font-medium">{{ post.likeCount || 0 }}</span>
                </Button>
                <Button variant="ghost" class="w-full rounded-xl text-gray-500 hover:text-blue-500 hover:bg-blue-50"
                  @click.stop="handleCommentClick(post)">
                  <MessageCircle class="w-4 h-4 mr-1.5" /><span class="text-xs font-medium">{{ post.commentCount || 0
                  }}</span>
                </Button>
                <Button variant="ghost"
                  class="w-full rounded-xl text-gray-500 hover:text-[rgb(33,111,85)] hover:bg-green-50"
                  @click.stop="handleShare(post)">
                  <Share2 class="w-4 h-4 mr-1.5" /><span class="text-xs font-medium">{{ post.shareCount || 0 }}</span>
                </Button>
              </CardFooter>
            </Card>
          </template>

          <!--列表底部提示 -->
          <ListEndIndicator v-if="!loading && posts.length > 0 && pagination.pageNum >= pagination.pages" text="已经到底啦"
            icon="📭" />

          <div v-if="pagination.pages > 0" class="bg-white rounded-2xl shadow-sm p-4 md:p-6">
            <!-- 桌面端：一行显示 -->
            <div class="hidden md:flex items-center justify-center gap-6">
              <Button variant="outline" size="icon" :disabled="pagination.pageNum <= 1"
                class="h-10 w-10 rounded-xl border-2 border-gray-200 hover:border-primary hover:bg-primary/5 disabled:opacity-40 disabled:cursor-not-allowed transition-all"
                @click="changePage(pagination.pageNum - 1)">
                <ChevronLeft class="w-5 h-5" />
              </Button>

              <div class="flex items-center gap-3">
                <span class="text-sm text-gray-500 font-medium">第</span>
                <span
                  class="text-lg font-bold text-primary px-3 py-1 bg-primary/10 rounded-lg min-w-[3rem] text-center">
                  {{ pagination.pageNum }}
                </span>
                <span class="text-sm text-gray-500 font-medium">/</span>
                <span class="text-lg font-bold text-gray-700 px-3 py-1 bg-gray-100 rounded-lg min-w-[3rem] text-center">
                  {{ pagination.pages }}
                </span>
                <span class="text-sm text-gray-500 font-medium">页</span>
              </div>

              <Button variant="outline" size="icon" :disabled="pagination.pageNum >= pagination.pages"
                class="h-10 w-10 rounded-xl border-2 border-gray-200 hover:border-primary hover:bg-primary/5 disabled:opacity-40 disabled:cursor-not-allowed transition-all"
                @click="changePage(pagination.pageNum + 1)">
                <ChevronRight class="w-5 h-5" />
              </Button>

              <div class="h-8 w-px bg-gray-200"></div>

              <div class="flex items-center gap-3">
                <span class="text-sm text-gray-600 font-medium whitespace-nowrap">跳转到</span>
                <Input v-model.number="pagination.jumpPage" type="number" min="1" :max="pagination.pages"
                  class="w-20 h-10 text-center text-base font-semibold border-2 border-gray-200 rounded-xl focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all"
                  placeholder="页码" @keyup.enter="handleJumpPage"
                  @input="(e) => { if (e.target.value && parseInt(e.target.value) < 1) e.target.value = 1 }" />
                <Button size="icon"
                  class="h-10 w-10 rounded-xl bg-primary hover:bg-primary/90 text-white shadow-sm hover:shadow-md transition-all"
                  @click="handleJumpPage">
                  <ChevronRight class="w-5 h-5" />
                </Button>
              </div>
            </div>

            <!-- 移动端：分两行显示 -->
            <div class="md:hidden space-y-3">
              <!-- 第一行：上一页、页码、下一页 -->
              <div class="flex items-center justify-center gap-3">
                <Button variant="outline" size="icon" :disabled="pagination.pageNum <= 1"
                  class="h-9 w-9 rounded-lg border-2 border-gray-200 hover:border-primary hover:bg-primary/5 disabled:opacity-40 disabled:cursor-not-allowed transition-all"
                  @click="changePage(pagination.pageNum - 1)">
                  <ChevronLeft class="w-4 h-4" />
                </Button>

                <div class="flex items-center gap-2">
                  <span class="text-xs text-gray-500 font-medium">第</span>
                  <span
                    class="text-base font-bold text-primary px-2 py-1 bg-primary/10 rounded-lg min-w-[2.5rem] text-center">
                    {{ pagination.pageNum }}
                  </span>
                  <span class="text-xs text-gray-500 font-medium">/</span>
                  <span
                    class="text-base font-bold text-gray-700 px-2 py-1 bg-gray-100 rounded-lg min-w-[2.5rem] text-center">
                    {{ pagination.pages }}
                  </span>
                  <span class="text-xs text-gray-500 font-medium">页</span>
                </div>

                <Button variant="outline" size="icon" :disabled="pagination.pageNum >= pagination.pages"
                  class="h-9 w-9 rounded-lg border-2 border-gray-200 hover:border-primary hover:bg-primary/5 disabled:opacity-40 disabled:cursor-not-allowed transition-all"
                  @click="changePage(pagination.pageNum + 1)">
                  <ChevronRight class="w-4 h-4" />
                </Button>
              </div>

              <!-- 第二行：跳转功能 -->
              <div class="flex items-center justify-center gap-2 pt-2 border-t border-gray-100">
                <span class="text-xs text-gray-600 font-medium">跳转到</span>
                <Input v-model.number="pagination.jumpPage" type="number" min="1" :max="pagination.pages"
                  class="w-16 h-9 text-center text-sm font-semibold border-2 border-gray-200 rounded-lg focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all"
                  placeholder="页码" @keyup.enter="handleJumpPage"
                  @input="(e) => { if (e.target.value && parseInt(e.target.value) < 1) e.target.value = 1 }" />
                <Button size="icon"
                  class="h-9 w-9 rounded-lg bg-primary hover:bg-primary/90 text-white shadow-sm hover:shadow-md transition-all"
                  @click="handleJumpPage">
                  <ChevronRight class="w-4 h-4" />
                </Button>
              </div>
            </div>
          </div>
        </div>

        <div class="hidden md:block md:col-span-3 space-y-6 sticky top-24 self-start">
          <Card
            class="border-none shadow-lg bg-gradient-to-br from-[rgb(33,111,85)] to-emerald-800 text-white rounded-2xl">
            <CardHeader class="pb-2 flex flex-row items-center justify-between">
              <CardTitle class="flex items-center gap-2 text-base font-bold">
                <Bell class="w-4 h-4" /> 校园公告
              </CardTitle>
              <span class="text-[10px] text-white/70 cursor-pointer hover:text-white hover:underline"
                @click="router.push('/notices?tab=1')">查看更多</span>
            </CardHeader>
            <CardContent class="text-sm opacity-95 space-y-4 pb-6">
              <!-- 任务1.2：修改v-for使用displayedAnnouncements，限制首页最多显示2条 -->
              <div v-for="item in displayedAnnouncements" :key="item.id"
                class="group cursor-pointer hover:bg-white/10 p-2 rounded transition max-h-[200px] overflow-hidden" @click="viewAnnouncement(item)">
                <div class="flex gap-2 items-start mb-1"><span v-if="item.isTop"
                    class="bg-white/20 text-[10px] px-1.5 rounded font-bold whitespace-nowrap mt-0.5">置顶</span><span
                    class="font-medium hover:underline line-clamp-1 break-all">{{ item.title }}</span></div>
                <p class="text-xs text-white/70 line-clamp-2 leading-relaxed break-words pl-8">{{ item.content }}</p>
              </div>
            </CardContent>
          </Card>

          <Card class="border-none shadow-sm rounded-2xl bg-white">
            <CardHeader class="pb-3 border-b border-gray-50">
              <CardTitle class="flex items-center gap-2 text-base text-gray-800 font-bold">
                <Flame class="w-4 h-4 text-red-500" /> 热门帖子榜
              </CardTitle>
            </CardHeader>
            <CardContent class="pt-4 space-y-2">
              <div v-for="(item, i) in hotPosts" :key="item.id"
                class="flex items-center gap-3 p-2 rounded-xl hover:bg-gray-50 cursor-pointer group transition-colors"
                @click="handleHotSearch(item)">
                <span class="text-sm font-bold w-5 h-5 flex items-center justify-center rounded"
                  :class="i < 3 ? 'text-red-500 bg-red-50' : 'text-gray-400 bg-gray-100'">{{ i + 1 }}</span>
                <span class="text-sm text-gray-600 flex-1 truncate group-hover:text-[rgb(33,111,85)]">{{ item.content
                }}</span>
                <span class="text-xs text-gray-300 flex items-center gap-1">
                  <Heart class="w-3 h-3" /> {{ item.likeCount }}
                </span>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </main>

    <!--移动端侧边栏 - 使用@click.self确保只有点击遮罩层本身才关闭 -->
    <Transition name="sidebar-fade">
      <div v-if="showMobileSidebar" class="fixed inset-0 z-50 bg-black/50 md:hidden"
        @click.self="showMobileSidebar = false">
        <Transition name="sidebar-slide">
          <div v-if="showMobileSidebar" class="w-72 h-full bg-white p-6 flex flex-col gap-4 shadow-2xl">
            <div class="flex items-center justify-between mb-2">
              <h3 class="font-bold text-lg text-[rgb(33,111,85)]">功能导航</h3>
              <Button variant="ghost" size="icon" class="h-8 w-8 rounded-full" @click="showMobileSidebar = false">
                <X class="w-4 h-4" />
              </Button>
            </div>

            <!--移动端搜索框 -->
            <div class="relative mb-2">
              <Input v-model="searchQuery" placeholder="🔍 搜索内容..."
                class="pl-4 pr-10 bg-gray-50 border-gray-200 rounded-full h-10 text-sm"
                @keyup.enter="() => { saveSearchHistory(searchQuery); fetchPosts(1); showMobileSidebar = false }" />
              <Button v-if="searchQuery" variant="ghost" size="icon"
                class="absolute right-1 top-1/2 -translate-y-1/2 h-8 w-8 rounded-full" @click="searchQuery = ''"
                aria-label="清空搜索">
                <X class="w-3 h-3" />
              </Button>
            </div>

            <div class="border-t border-gray-100 pt-4 space-y-1">
              <Button variant="ghost"
                class="w-full justify-start h-11 rounded-xl hover:bg-emerald-50 hover:text-[rgb(33,111,85)]"
                @click="() => { filterCategory(0); showMobileSidebar = false }">
                <LayoutGrid class="w-4 h-4 mr-3" /> 全部帖子
              </Button>
              <Button variant="ghost"
                class="w-full justify-start h-11 rounded-xl hover:bg-emerald-50 hover:text-[rgb(33,111,85)]"
                @click="() => { switchFeed(1); showMobileSidebar = false }">
                <User class="w-4 h-4 mr-3" /> 我的关注
              </Button>
            </div>

            <!-- 分类快捷入口 -->
            <div class="border-t border-gray-100 pt-4">
              <p class="text-xs text-gray-400 mb-3 px-1">分类浏览</p>
              <div class="grid grid-cols-3 gap-2">
                <div v-for="cat in categories" :key="cat.id"
                  class="flex flex-col items-center p-3 rounded-xl cursor-pointer transition-all hover:scale-105"
                  :class="cat.bg" @click="() => { filterCategory(cat.id); showMobileSidebar = false }">
                  <component :is="cat.icon" class="w-5 h-5 mb-1" :class="cat.color" />
                  <span class="text-xs font-medium" :class="cat.color">{{ cat.name }}</span>
                </div>
              </div>
            </div>
          </div>
        </Transition>
      </div>
    </Transition>

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
          <div id="home-location-map" class="w-full h-full"></div>

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

    <!-- 媒体预览弹窗（图片+视频） -->
    <div v-if="showMediaLightbox && lightboxMediaList.length"
      class="fixed inset-0 z-[100] bg-black/95 flex items-center justify-center animate-in fade-in duration-200"
      @click="closeMediaLightbox">
      <!-- 关闭按钮 -->
      <Button variant="ghost" size="icon"
        class="absolute top-4 right-4 text-white/70 hover:text-white hover:bg-white/20 rounded-full z-10"
        @click.stop="closeMediaLightbox">
        <X class="w-6 h-6" />
      </Button>

      <!-- 内容区域 -->
      <div class="relative w-full h-full flex items-center justify-center p-4" @click.stop>
        <!-- 图片 -->
        <img v-if="lightboxMediaList[lightboxIndex]?.type === 'image'" :src="lightboxMediaList[lightboxIndex]?.url"
          class="max-w-full max-h-full object-contain select-none" @error="(e) => handleImageError(e, 'post')" />
        <!-- 视频 -->
        <video v-else-if="lightboxMediaList[lightboxIndex]?.type === 'video'"
          :src="lightboxMediaList[lightboxIndex]?.url" controls autoplay
          class="max-w-full max-h-full object-contain"></video>

        <!-- 左右切换 -->
        <Button v-if="lightboxIndex > 0" variant="ghost" size="icon"
          class="absolute left-4 top-1/2 -translate-y-1/2 text-white/60 hover:text-white hover:bg-white/20 rounded-full w-12 h-12"
          @click.stop="prevMedia">
          <ChevronLeft class="w-8 h-8" />
        </Button>
        <Button v-if="lightboxIndex < lightboxMediaList.length - 1" variant="ghost" size="icon"
          class="absolute right-4 top-1/2 -translate-y-1/2 text-white/60 hover:text-white hover:bg-white/20 rounded-full w-12 h-12"
          @click.stop="nextMedia">
          <ChevronRight class="w-8 h-8" />
        </Button>
      </div>

      <!-- 计数 -->
      <div class="absolute bottom-6 left-1/2 -translate-x-1/2 text-white/80 bg-black/50 px-4 py-2 rounded-full text-sm">
        {{ lightboxIndex + 1 }} / {{ lightboxMediaList.length }}
      </div>
    </div>

    <!-- 历史公告弹窗 - 问题14修复 -->
    <Dialog :open="showAllAnnouncements" @update:open="showAllAnnouncements = $event">
      <DialogContent class="max-w-lg max-h-[80vh] overflow-hidden flex flex-col">
        <DialogHeader>
          <DialogTitle class="flex items-center gap-2">
            <Bell class="w-5 h-5 text-[rgb(33,111,85)]" /> 全部公告
          </DialogTitle>
        </DialogHeader>
        <div class="flex-1 overflow-y-auto space-y-3 pr-2">
          <div v-if="announcements.length === 0" class="text-center text-gray-400 py-8">
            暂无公告
          </div>
          <div v-else v-for="item in announcements" :key="item.id"
            class="group p-4 rounded-xl border border-gray-100 hover:border-[rgb(33,111,85)]/30 hover:bg-emerald-50/30 transition-all cursor-pointer"
            @click="showAllAnnouncements = false; viewAnnouncement(item)">
            <div class="flex items-start gap-2 mb-2">
              <span v-if="item.isTop"
                class="bg-[rgb(33,111,85)] text-white text-[10px] px-1.5 py-0.5 rounded font-bold shrink-0">置顶</span>
              <span class="font-medium text-gray-800 group-hover:text-[rgb(33,111,85)] line-clamp-1">{{ item.title }}</span>
            </div>
            <p class="text-sm text-gray-500 line-clamp-2 leading-relaxed">{{ item.content }}</p>
            <div class="text-xs text-gray-400 mt-2">{{ formatTime(item.createTime) }}</div>
          </div>
        </div>
      </DialogContent>
    </Dialog>

    <!-- 公告详情弹窗 -->
    <Dialog :open="showAnnouncementDetail" @update:open="showAnnouncementDetail = $event">
      <DialogContent class="max-w-lg max-h-[80vh] overflow-hidden flex flex-col">
        <DialogHeader>
          <DialogTitle class="flex items-center gap-2">
            <Bell class="w-5 h-5 text-[rgb(33,111,85)]" />
            <span class="line-clamp-1">{{ currentAnnouncement?.title || '公告详情' }}</span>
          </DialogTitle>
        </DialogHeader>
        <div class="flex-1 overflow-y-auto pr-2 space-y-4">
          <div v-if="currentAnnouncement?.isTop" class="inline-flex">
            <span class="bg-[rgb(33,111,85)] text-white text-[10px] px-2 py-0.5 rounded font-bold">置顶</span>
          </div>
          <p class="text-gray-700 leading-relaxed whitespace-pre-line text-sm">{{ currentAnnouncement?.content }}</p>
          <div class="text-xs text-gray-400 pt-2 border-t border-gray-100">
            发布时间：{{ currentAnnouncement?.createTime ? new Date(currentAnnouncement.createTime).toLocaleString('zh-CN', { year:'numeric', month:'2-digit', day:'2-digit', hour:'2-digit', minute:'2-digit', second:'2-digit' }) : '未知' }}
          </div>
        </div>
      </DialogContent>
    </Dialog>

    <footer style="background-color: rgb(33, 111, 85);" class="text-white/80 py-10 mt-auto border-t border-white/10">
      <div class="container mx-auto px-4 max-w-7xl">
        <div class="grid grid-cols-1 md:grid-cols-4 gap-8 mb-8">
          <div class="space-y-4">
            <h4 class="text-white font-bold text-lg flex items-center gap-2"><img src="/Round_school_logo.png"
                class="w-8 h-8" /> 张家界学院表白墙</h4>
            <p class="text-xs leading-relaxed opacity-80 text-white rich-text">
              这里是张家界学院专属的社交平台。我们致力于为同学们提供一个温暖、真实、有趣的交流空间。
            </p>
          </div>
          <div>
            <h5 class="text-white font-bold mb-4 text-sm">快速导航</h5>
            <ul class="space-y-2 text-xs opacity-80">
              <li>关于我们</li>
              <li>免责声明</li>
            </ul>
          </div>
          <div>
            <h5 class="text-white font-bold mb-4 text-sm">友情链接</h5>
            <ul class="space-y-2 text-xs opacity-80">
              <li>张家界学院官网</li>
              <li>教务系统</li>
            </ul>
          </div>
          <div>
            <h5 class="text-white font-bold mb-4 text-sm">联系我们</h5>
            <ul class="space-y-2 text-xs opacity-80">
              <li>邮箱: admin@zjjc.edu.cn</li>
            </ul>
          </div>
        </div>
        <Separator class="bg-white/20 my-6" />
        <div class="text-center text-xs opacity-60">
          <p>© 2025 张家界学院表白墙. All rights reserved.</p>
        </div>
      </div>
    </footer>

    <!-- 回到顶部按钮 -->
    <BackToTop />
  </div>
</template>

<style scoped>
.rich-text {
  text-indent: 2em;
  line-height: 1.8;
  text-align: justify;
}

.nav-btn-active {
  background: linear-gradient(135deg, rgba(33, 111, 85, 0.12) 0%, rgba(16, 185, 129, 0.15) 100%) !important;
  color: rgb(5, 150, 105) !important;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.15), 
              inset 3px 0 0 0 rgb(16, 185, 129) !important;
  transform: translateX(2px);
  font-weight: 600 !important;
}

.bell-wrapper :deep(button) {
  color: white !important;
}

.bell-wrapper :deep(button):hover {
  background-color: rgba(255, 255, 255, 0.1) !important;
}

/* 轮播图动画 */
.banner-fade-enter-active,
.banner-fade-leave-active {
  transition: opacity 0.5s ease;
}

.banner-fade-enter-from,
.banner-fade-leave-to {
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

/*移动端侧边栏动画 */
.sidebar-fade-enter-active,
.sidebar-fade-leave-active {
  transition: opacity 0.3s ease;
}

.sidebar-fade-enter-from,
.sidebar-fade-leave-to {
  opacity: 0;
}

.sidebar-slide-enter-active,
.sidebar-slide-leave-active {
  transition: transform 0.3s ease;
}

.sidebar-slide-enter-from,
.sidebar-slide-leave-to {
  transform: translateX(-100%);
}
</style>