<script setup>
import { ref, reactive, watch, nextTick, onUnmounted, computed, onMounted } from 'vue'
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription
} from '@/components/ui/dialog'
import { useDialogKeyboard } from '@/composables/useKeyboard'
import { Button } from '@/components/ui/button'
import { Switch } from '@/components/ui/switch'
import { Label } from '@/components/ui/label'
import { Input } from '@/components/ui/input'
import { Avatar, AvatarImage, AvatarFallback } from '@/components/ui/avatar'
import {
  X, ImagePlus, Loader2, Video, BarChart2, MapPin,
  Clock, Plus, Trash2, Heart, Search, ShoppingBag, HelpCircle, LayoutGrid,
  CheckCircle2, Sparkles, Send, Eye, Zap, Maximize2, Minimize2, Hash,
  AlertTriangle, RefreshCw, Coffee
} from 'lucide-vue-next'
import * as LucideIcons from 'lucide-vue-next'
import request from '@/api/request'
import { useAppStore } from '@/stores/app'
import { compressImage } from '@/utils/imageCompress'

import AiAssistant from '@/components/AiAssistant.vue'
import { DEFAULT_CENTER, loadAmapScript, initAmapSecurity } from '@/utils/mapConfig'

const props = defineProps({ open: Boolean, user: { type: Object, default: () => ({}) } })
const emit = defineEmits(['update:open', 'refresh'])
const appStore = useAppStore()

const loading = ref(false)
const show = ref(false)
const fileInput = ref(null)
const videoInput = ref(null)
const isSubmitted = ref(false)
const locationText = ref('')
const showAiAssistant = ref(false)
const textareaFocused = ref(false)
const expandedEditor = ref(false)

// 随机占位符
const placeholders = [
  '今天想说点什么？✨',
  '分享你的校园故事...',
  '有什么新鲜事想告诉大家？',
  '写下此刻的心情吧~',
  '记录美好瞬间 📸',
  '想对TA说些什么？💕'
]
const currentPlaceholder = ref(placeholders[Math.floor(Math.random() * placeholders.length)])

// 地图相关
const showMapDialog = ref(false)
const mapLoading = ref(true)
const mapSearchLoading = ref(false)
const poiList = ref([])
const mapSearchKeyword = ref('')
const recentLocations = ref([]) // 历史位置
let mapInstance = null
let placeSearch = null
let geocoder = null // 逆地理编码
let mapSearchTimer = null
// 单例标志：区分"程序移动 setCenter"和"用户拖拽"，避免 moveend -> reverseGeocode -> setCenter -> moveend 死循环
let isProgrammaticMove = false

// 加载历史位置
const loadRecentLocations = () => {
  try {
    const saved = localStorage.getItem('recent_locations')
    if (saved) {
      recentLocations.value = JSON.parse(saved).slice(0, 5)
    }
  } catch (e) { }
}

// 保存历史位置
const saveRecentLocation = (name) => {
  if (!name || name === '当前位置') return
  const cleanName = name.replace('📍 ', '')
  const list = recentLocations.value.filter(l => l !== cleanName)
  list.unshift(cleanName)
  recentLocations.value = list.slice(0, 5)
  localStorage.setItem('recent_locations', JSON.stringify(recentLocations.value))
}

// 删除某条历史位置
const removeRecentLocation = (name) => {
  recentLocations.value = recentLocations.value.filter(l => l !== name)
  localStorage.setItem('recent_locations', JSON.stringify(recentLocations.value))
}

// 媒体预览相关
const showVideoPreview = ref(false)
const previewVideoUrl = ref('')
const showImagePreview = ref(false)
const previewImageUrl = ref('')

const form = reactive({
  content: '', category: null, isAnonymous: false, visibility: 0,
  images: [], video: '', videoCover: '',
  poll: { enabled: false, options: ['', ''], endTime: '' },
  schedule: { enabled: false, time: '' }
})

const previewFiles = ref([])

// 图标名称映射表：将后端存储的字符串映射到 Lucide 组件
const ICON_MAP = {
  Heart, Search, ShoppingBag, Hash, Clock, HelpCircle, LayoutGrid,
  Coffee,
  // 常用拓展图标（管理员新增板块可能用到）
  BookOpen: null, Star: null, Flame: null, Music: null, Camera: null,
  Globe: null, Flag: null, Award: null, Zap
}

// 图标名称转换（支持横杠命名：graduation-cap -> GraduationCap）
const getLucideIcon = (name) => {
  if (!name) return LucideIcons.Hash
  const pascalName = name.split('-').map(w => w.charAt(0).toUpperCase() + w.slice(1).toLowerCase()).join('')
  return LucideIcons[pascalName] || LucideIcons[name] || LucideIcons.Hash
}

// 动态分区列表（从后端加载）
const allCategories = ref([])

// 加载分区列表
const loadCategories = async () => {
  try {
    const res = await request.get('/index/categories')
    if (res.code === '200') {
      allCategories.value = res.data || []
    }
  } catch (e) {
    console.error('加载分区失败', e)
  }
}

// 前8个基础分区（静态颜色配置保持原有卡片风格）
const CORE_STYLES = {
  1: { color: 'text-pink-500',   bg: 'bg-pink-50',   border: 'border-pink-200', desc: '勇敢说爱' },
  2: { color: 'text-blue-500',   bg: 'bg-blue-50',   border: 'border-blue-200', desc: '失物招领' },
  3: { color: 'text-orange-500', bg: 'bg-orange-50', border: 'border-orange-200', desc: '二手交易' },
  4: { color: 'text-purple-500', bg: 'bg-purple-50', border: 'border-purple-200', desc: '畅所欲言' },
  5: { color: 'text-green-500',  bg: 'bg-green-50',  border: 'border-green-200', desc: '校园活动' },
  6: { color: 'text-cyan-500',   bg: 'bg-cyan-50',   border: 'border-cyan-200', desc: '互帮互助' },
  7: { color: 'text-red-500',    bg: 'bg-red-50',    border: 'border-red-200', desc: '吃货天堂' },
  9: { color: 'text-gray-500',   bg: 'bg-gray-50',   border: 'border-gray-200', desc: '自由话题' },
}

const CORE_IDS = [1, 2, 3, 4, 5, 6, 7, 9]

// 前8个基础板块（id 1-7, 9）
const coreCategories = computed(() =>
  allCategories.value
    .filter(c => CORE_IDS.includes(c.id))
    .sort((a, b) => CORE_IDS.indexOf(a.id) - CORE_IDS.indexOf(b.id))
    .map(c => ({
      ...c,
      icon: getLucideIcon(c.icon),
      ...((CORE_STYLES[c.id] || { color: 'text-gray-500', bg: 'bg-gray-50', border: 'border-gray-200', desc: '' }))
    }))
)

// 扩展板块
const extraCategories = computed(() =>
  allCategories.value
    .filter(c => !CORE_IDS.includes(c.id))
    .map(c => ({
      ...c,
      icon: getLucideIcon(c.icon),
    }))
)

// 字数进度
const contentProgress = computed(() => Math.min((form.content.length / 2000) * 100, 100))
const progressColor = computed(() => {
  if (form.content.length > 1800) return 'bg-red-500'
  if (form.content.length > 1500) return 'bg-yellow-500'
  return 'bg-[rgb(33,111,85)]'
})

onMounted(() => {
  loadCategories()
})

watch(() => props.open, (val) => {
  show.value = val
  if (val) {
    isSubmitted.value = false
    currentPlaceholder.value = placeholders[Math.floor(Math.random() * placeholders.length)]
    try {
      const draft = localStorage.getItem(`draft_${props.user?.id}`)
      if (draft) {
        Object.assign(form, JSON.parse(draft))
        appStore.showToast('已恢复草稿 📝', 'info')
      }
    } catch (e) { }
  } else {
    if (isSubmitted.value) {
      localStorage.removeItem(`draft_${props.user?.id}`)
      resetForm()
    } else if (form.content || form.images.length) {
      localStorage.setItem(`draft_${props.user?.id}`, JSON.stringify(form))
      appStore.showToast('草稿已自动保存', 'info')
    }
  }
})
watch(show, (val) => {
  emit('update:open', val)

  // 对话框打开时，自动聚焦到输入框
  if (val) {
    nextTick(() => {
      const textarea = document.querySelector('textarea[placeholder*="想说"]')
      if (textarea) {
        textarea.focus()
      }
    })
  }
})

// 添加键盘导航支持：Esc 关闭对话框
useDialogKeyboard(show, () => {
  show.value = false
})

watch(showMapDialog, (val) => {
  if (!val && mapInstance) {
    mapInstance.destroy()
    mapInstance = null
    placeSearch = null
  }
})

const resetForm = () => {
  Object.assign(form, {
    content: '', category: null, isAnonymous: false, visibility: 0,
    images: [], video: '', videoCover: '',
    poll: { enabled: false, options: ['', ''], endTime: '' },
    schedule: { enabled: false, time: '' }
  })
  previewFiles.value = []
  locationText.value = ''
  poiList.value = []
  aiAssisted.value = false  // 重置AI使用状态
}

const handleFileChange = async (e, type) => {
  const files = Array.from(e.target.files)

  // 定义允许的文件类型
  const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp']
  const ALLOWED_VIDEO_TYPES = ['video/mp4', 'video/webm', 'video/ogg', 'video/quicktime']

  if (type === 'image') {
    // 计算还能添加多少张图片（排除已有的视频）
    const currentImages = previewFiles.value.filter(f => f.type === 'image').length
    const remaining = 9 - currentImages
    if (files.length > remaining) appStore.showToast(`最多只能再选 ${remaining} 张图片`, 'error')

    // 验证文件类型和大小
    const validFiles = []
    for (const file of files.slice(0, remaining)) {
      // 检查文件类型
      if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
        appStore.showToast(`${file.name} 不是有效的图片格式（支持JPG/PNG/GIF/WebP）`, 'error')
        continue
      }
      // 检查文件大小
      if (file.size > 10 * 1024 * 1024) {
        appStore.showToast(`${file.name} 太大（最大10MB）`, 'error')
        continue
      }
      validFiles.push(file)
    }

    // 上传有效文件
    for (const file of validFiles) {
      const reader = new FileReader()
      reader.onload = async (ev) => {
        const index = previewFiles.value.length
        previewFiles.value.push({ url: ev.target.result, type: 'image', file, uploading: true, compressing: true })
        try {
          const compressedFile = await compressImage(file, { maxWidth: 1920, maxHeight: 1920, quality: 0.8, maxSize: 500 })
          previewFiles.value[index].compressing = false
          uploadRealFile(compressedFile, index)
        } catch (err) {
          previewFiles.value[index].compressing = false
          uploadRealFile(file, index)
        }
      }
      reader.readAsDataURL(file)
    }
  } else if (type === 'video') {
    // 检查是否已有视频
    const hasVideo = previewFiles.value.some(f => f.type === 'video')
    if (hasVideo) return appStore.showToast('只能上传1个视频', 'error')

    const file = files[0]

    // 检查文件类型
    if (!ALLOWED_VIDEO_TYPES.includes(file.type)) {
      return appStore.showToast('不支持的视频格式（支持MP4/WebM/OGG/MOV）', 'error')
    }

    // 检查文件大小
    const maxSize = 200 * 1024 * 1024 // 200MB
    if (file.size > maxSize) {
      return appStore.showToast(`视频最大200MB，当前${(file.size / 1024 / 1024).toFixed(1)}MB`, 'error')
    }

    const index = previewFiles.value.length
    previewFiles.value.push({
      url: URL.createObjectURL(file),
      type: 'video',
      file,
      uploading: true
    })

    // 直接上传
    uploadRealFile(file, index)
  }
  e.target.value = ''
}



const uploadRealFile = async (file, index) => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('userId', props.user.id)
  previewFiles.value[index].progress = 0
  previewFiles.value[index].uploadError = false
  try {
    const res = await request.post('/file/upload', formData, {
      onUploadProgress: (progressEvent) => {
        const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        if (previewFiles.value[index]) previewFiles.value[index].progress = percent
      }
    })
    if (res.code === '200') {
      previewFiles.value[index].uploading = false
      previewFiles.value[index].uploadSuccess = true
      previewFiles.value[index].progress = 100
      if (previewFiles.value[index].type === 'image') form.images.push(res.data)
      else form.video = res.data
    } else {
      throw new Error(res.msg || '上传失败')
    }
  } catch (e) {
    previewFiles.value[index].uploading = false
    previewFiles.value[index].uploadError = true
    previewFiles.value[index].errorMessage = e.message || '上传失败，请重试'
    appStore.showToast(previewFiles.value[index].errorMessage, 'error')
  }
}

// 重试上传
const retryUpload = (index) => {
  const file = previewFiles.value[index]
  if (!file || !file.file) return

  file.uploading = true
  file.uploadError = false
  file.progress = 0

  uploadRealFile(file.file, index)
}

const removeFile = (idx) => {
  const file = previewFiles.value[idx]
  previewFiles.value.splice(idx, 1)
  if (file.type === 'image') {
    // 重新计算图片索引
    const imageIndex = previewFiles.value.slice(0, idx).filter(f => f.type === 'image').length
    form.images.splice(imageIndex, 1)
  } else {
    form.video = ''
  }
}

// 地图相关函数
const openMapSelector = async () => {
  showMapDialog.value = true
  poiList.value = []
  mapLoading.value = true
  mapSearchLoading.value = false
  mapSearchKeyword.value = '' // 清空搜索关键词
  loadRecentLocations() // 加载历史位置
  await nextTick()
  setTimeout(() => initMap(), 300)
}

const initMap = async () => {
  try {
    // 初始化安全配置
    initAmapSecurity()

    // 加载地图脚本
    await loadAmapScript()

    // 创建地图
    createMap()
  } catch (error) {
    console.error('地图初始化失败:', error)
    appStore.showToast('地图加载失败，请刷新重试', 'error')
    mapLoading.value = false
    showMapDialog.value = false
  }
}

// 定位失败状态
const locationError = ref(false)
const locationErrorMsg = ref('')

// 重新定位
const retryLocation = () => {
  locationError.value = false
  locationErrorMsg.value = ''
  mapLoading.value = true
  if (mapInstance) {
    const geolocation = new AMap.Geolocation({
      enableHighAccuracy: true,
      timeout: 10000,
      GeoLocationFirst: true,
      getCityWhenFail: true,
      needAddress: true,
      extensions: 'all'
    })
    geolocation.getCurrentPosition((status, result) => {
      if (status === 'complete' && result.position) {
        reverseGeocode(result.position)
      } else {
        useCitySearch()
      }
    })
  }
}

const createMap = () => {
  if (!document.getElementById('map-container')) return
  locationError.value = false
  locationErrorMsg.value = ''
  try {
    mapInstance = new AMap.Map('map-container', {
      resizeEnable: true,
      zoom: 14,
      touchZoom: true,       // 支持触摸缩放
      scrollWheel: true,     // 支持滚轮缩放
      doubleClickZoom: true  // 支持双击缩放
    })

    // 添加缩放控件
    AMap.plugin(['AMap.ToolBar', 'AMap.Scale'], () => {
      // 问题10修复：缩放工具栏放左上角，避免与右下角的定位按钮重叠
      const toolbar = new AMap.ToolBar({
        position: 'LT',  // 左上角
        liteStyle: true
      })
      mapInstance.addControl(toolbar)

      // 比例尺放左下角
      const scale = new AMap.Scale({
        position: 'LB'
      })
      mapInstance.addControl(scale)
    })
  } catch (e) {
    console.error('地图创建失败:', e)
    mapLoading.value = false
    locationError.value = true
    locationErrorMsg.value = '地图加载失败，请检查网络'
    return
  }

  // 初始化插件
  const initPlugins = () => {
    try {
      // 初始化 PlaceSearch
      placeSearch = new AMap.PlaceSearch({
        type: '学校|餐饮服务|商务住宅|生活服务|风景名胜|地名地址信息|交通设施服务',
        pageSize: 20, pageIndex: 1, extensions: 'base'
      })

      // 初始化逆地理编码
      geocoder = new AMap.Geocoder({
        radius: 500,
        extensions: 'all'
      })

      // 初始化定位 - 优化配置
      const geolocation = new AMap.Geolocation({
        enableHighAccuracy: true,  // 高精度
        timeout: 10000,            // 10秒超时
        buttonPosition: 'RB',
        zoomToAccuracy: true,      // 定位成功后自动调整缩放
        GeoLocationFirst: true,    // 优先使用浏览器定位
        noIpLocate: 0,             // IP定位作为备选
        getCityWhenFail: true,     // 定位失败时获取城市
        needAddress: true,         // 需要地址信息
        extensions: 'all'          // 返回详细信息
      })
      mapInstance.addControl(geolocation)

      // 开始定位（position 本身已是 AMap.LngLat）
      geolocation.getCurrentPosition((status, result) => {
        if (status === 'complete' && result.position) {
          reverseGeocode(result.position)
        } else {
          console.warn('GPS定位失败，降级到城市定位:', result?.info)
          useCitySearch()
        }
      })

      // 地图移动结束事件 - 仅在用户手动拖拽时才重新计算位置
      mapInstance.on('moveend', () => {
        // isProgrammaticMove 为 true 说明是 setCenter 触发的，跳过避免无限循环
        if (isProgrammaticMove) return
        const center = mapInstance.getCenter()
        reverseGeocode(center)
      })
    } catch (e) {
      console.error('插件初始化失败:', e)
      locationError.value = true
      locationErrorMsg.value = '地图服务初始化失败'
      // 用 LngLat 对象包装，避免 searchNearBy 静默失败
      const defaultCenter = new AMap.LngLat(DEFAULT_CENTER[0], DEFAULT_CENTER[1])
      handleLocationFinal(defaultCenter, '张家界学院')
    }
  }

  // 检查插件是否已加载
  if (AMap.Geolocation && AMap.PlaceSearch) {
    initPlugins()
  } else {
    // 动态加载插件
    AMap.plugin(['AMap.Geolocation', 'AMap.CitySearch', 'AMap.PlaceSearch'], () => {
      initPlugins()
    })
  }
}

const useCitySearch = () => {
  // 确保 AMap.CitySearch 已加载
  const doSearch = () => {
    const citySearch = new AMap.CitySearch()
    citySearch.getLocalCity((status, result) => {
      if (status === 'complete' && result.info === 'OK' && result.bounds) {
        const center = result.bounds.getCenter()
        // 城市定位成功后，也用逆地理编码获取详细地址
        reverseGeocode(center)
      } else {
        // 定位失败，使用默认位置
        locationError.value = true
        locationErrorMsg.value = '自动定位失败，已切换到默认位置'
        // 用 LngLat 对象包装默认坐标，避免 searchNearBy 参数格式错误
        const defaultCenter = new AMap.LngLat(DEFAULT_CENTER[0], DEFAULT_CENTER[1])
        handleLocationFinal(defaultCenter, '张家界学院')
      }
    })
  }

  if (AMap.CitySearch) {
    doSearch()
  } else {
    AMap.plugin(['AMap.CitySearch'], doSearch)
  }
}

// 逆地理编码 - 坐标转详细地址
const reverseGeocode = (position) => {
  // 统一将坐标转为 AMap.LngLat 对象，避免搜索接口因格式不同静默失败
  let lngLat
  if (position instanceof AMap.LngLat) {
    lngLat = position
  } else if (Array.isArray(position)) {
    lngLat = new AMap.LngLat(position[0], position[1])
  } else {
    lngLat = position
  }

  try {
    // 标记为程序移动，防止 moveend 再次触发 reverseGeocode 造成死循环
    isProgrammaticMove = true
    mapInstance.setCenter(lngLat)
  } catch (e) {
    console.error('设置地图中心失败:', e)
  } finally {
    // 延迟清除标志，等 moveend 事件完成派发后再放开用户拖拽监听
    setTimeout(() => { isProgrammaticMove = false }, 300)
  }

  if (!geocoder) {
    geocoder = new AMap.Geocoder({ radius: 500, extensions: 'all' })
  }

  geocoder.getAddress(lngLat, (status, result) => {
    if (status === 'complete' && result.regeocode) {
      const regeo = result.regeocode
      const addressComponent = regeo.addressComponent
      const pois = regeo.pois || []

      // 优先使用最近的POI名称（更精确）
      let displayName = ''
      if (pois.length > 0) {
        const nearestPoi = pois[0]
        displayName = nearestPoi.name
        if (nearestPoi.distance > 50) {
          displayName += ' 附近'
        }
      } else if (addressComponent.building) {
        displayName = addressComponent.building
      } else if (addressComponent.street && addressComponent.streetNumber) {
        displayName = addressComponent.street + addressComponent.streetNumber
      } else if (addressComponent.neighborhood) {
        displayName = addressComponent.neighborhood
      } else {
        displayName = (addressComponent.district || '') + (addressComponent.township || '')
      }

      handleLocationFinal(lngLat, displayName || '当前位置')
    } else {
      // 逆地理编码失败，直接搜索周边
      handleLocationFinal(lngLat, '当前位置')
    }
  })
}

const handleLocationFinal = (position, addrText) => {
  mapLoading.value = false
  locationText.value = addrText
  updatePoiListWithCurrent(addrText, '当前地图中心')
  searchNearby(position)
}

const updatePoiListWithCurrent = (name, address) => {
  const currentItem = { id: 'current', name: '📍 ' + name, address }
  if (poiList.value.length > 0 && poiList.value[0].id === 'current') {
    poiList.value[0] = currentItem
  } else {
    poiList.value.unshift(currentItem)
  }
}

const searchNearby = (position) => {
  // 明确用 AMap.plugin 确保 PlaceSearch 插件已就绪再执行，避免 v2.0 插件异步问题
  const doSearch = () => {
    if (!placeSearch) {
      try {
        placeSearch = new AMap.PlaceSearch({ pageSize: 20, pageIndex: 1, extensions: 'base' })
      } catch (e) {
        console.error('PlaceSearch 初始化失败:', e)
        return
      }
    }

    // 统一确保是 AMap.LngLat，兼容数组格式
    let lngLat
    if (Array.isArray(position)) {
      lngLat = new AMap.LngLat(position[0], position[1])
    } else {
      lngLat = position
    }

    placeSearch.searchNearBy('', lngLat, 2000, (status, result) => {
      if (status === 'complete' && result.poiList && result.poiList.pois) {
        const current = poiList.value.find(p => p.id === 'current')
        const newPois = result.poiList.pois.map(poi => ({
          id: poi.id,
          name: poi.name,
          address: poi.address || (poi.cityname || '') + (poi.adname || '')
        }))
        poiList.value = current ? [current, ...newPois] : newPois

        if (current && current.name === '📍 当前位置' && newPois.length > 0) {
          current.name = '📍 ' + newPois[0].name + ' 附近'
          locationText.value = newPois[0].name + ' 附近'
        }
      } else {
        // 把错误信息直接显示在列表里，便于在无法打开控制台时也能诊断
        const errInfo = result?.info || result?.message || status || '未知错误'
        console.warn('周边搜索失败:', status, result)
        const current = poiList.value.find(p => p.id === 'current')
        const errItem = {
          id: 'poi-error',
          name: `⚠️ 周边搜索失败`,
          address: `错误码：${errInfo}（可能是 Key 权限或域名未授权）`
        }
        poiList.value = current ? [current, errItem] : [errItem]
      }
    })
  }

  if (AMap.PlaceSearch) {
    doSearch()
  } else {
    AMap.plugin(['AMap.PlaceSearch'], doSearch)
  }
}


const selectPoi = (poi) => {
  locationText.value = poi.name.replace('📍 ', '')
  saveRecentLocation(locationText.value) // 保存到历史
  showMapDialog.value = false
  appStore.showToast(`已选择：${locationText.value}`, 'success')
}

// 选择历史位置
const selectRecentLocation = (name) => {
  locationText.value = name
  showMapDialog.value = false
  appStore.showToast(`已选择：${name}`, 'success')
}

// 不显示位置
const clearLocationAndClose = () => {
  locationText.value = ''
  showMapDialog.value = false
}

// 地图搜索功能
const handleMapSearch = () => {
  clearTimeout(mapSearchTimer)
  mapSearchTimer = setTimeout(() => {
    if (mapSearchKeyword.value.trim()) {
      performMapSearch(mapSearchKeyword.value.trim())
    } else {
      // 清空搜索时恢复周边搜索结果
      if (mapInstance) {
        const center = mapInstance.getCenter()
        searchNearby(center)
      }
    }
  }, 300) // 300ms 防抖
}

const performMapSearch = (keyword) => {
  if (!placeSearch) return

  mapSearchLoading.value = true

  // 使用关键词搜索
  placeSearch.search(keyword, (status, result) => {
    mapSearchLoading.value = false

    if (status === 'complete' && result.poiList && result.poiList.pois) {
      const searchResults = result.poiList.pois.map(poi => ({
        id: poi.id || Math.random().toString(),
        name: poi.name,
        address: poi.address || (poi.cityname || '') + (poi.adname || '')
      }))

      // 如果有当前位置，保留在列表顶部
      const currentLocation = poiList.value.find(p => p.id === 'current')
      poiList.value = currentLocation ? [currentLocation, ...searchResults] : searchResults
    } else {
      // 搜索无结果时只保留当前位置
      poiList.value = poiList.value.filter(p => p.id === 'current')
    }
  })
}

const clearLocation = () => { locationText.value = '' }

// AI 润色应用回调 — 标记已使用 AI
const aiAssisted = ref(false)
const handleAiApply = (content) => {
  form.content = content
  aiAssisted.value = true  // 点击“使用此结果”后标记
}

// 媒体预览
const previewVideo = (file) => {
  previewVideoUrl.value = file.url
  showVideoPreview.value = true
}

const previewImage = (file) => {
  previewImageUrl.value = file.url
  showImagePreview.value = true
}

// 点击预览媒体
const handleMediaClick = (file) => {
  if (file.uploading) return
  if (file.type === 'video') {
    previewVideo(file)
  } else {
    previewImage(file)
  }
}

const removePollOption = (index) => {
  if (form.poll.options.length <= 2) return appStore.showToast('至少保留两个选项', 'error')
  form.poll.options.splice(index, 1)
}

// 问题10修复：投票截止时间快捷选择 - 修复时区问题
const showCustomEndTime = ref(false)

const getQuickEndTime = (hours) => {
  const date = new Date()
  date.setHours(date.getHours() + hours)

  // 使用本地时间格式化，而不是UTC时间
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')

  return `${year}-${month}-${day}T${hour}:${minute}`
}

const setQuickEndTime = (hours) => {
  form.poll.endTime = getQuickEndTime(hours)
  showCustomEndTime.value = false
}

const getMinDateTime = () => {
  return new Date().toISOString().slice(0, 16)
}

const formatEndTimeDisplay = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = date - now
  const hours = Math.floor(diff / (1000 * 60 * 60))
  const days = Math.floor(hours / 24)
  if (days > 0) return `${days}天${hours % 24}小时后截止`
  if (hours > 0) return `${hours}小时后截止`
  return '即将截止'
}

const isContentError = ref(false)

const submit = async (force = false) => {
  // 防止重复提交
  if (loading.value) return

  // 验证分类
  if (!form.category) return appStore.showToast('请选择一个分类标签！', 'error')

  // 验证内容：纯文字即可，不强制需要图片/视频
  const trimmedContent = form.content.trim()
  if (!trimmedContent && !form.images.length && !form.video) {
    appStore.showToast('请输入帖子内容（纯文字即可）', 'error')
    isContentError.value = true
    setTimeout(() => { isContentError.value = false }, 500)
    return
  }

  // 验证文件上传状态
  const uploadingFiles = previewFiles.value.filter(f => f.uploading || f.compressing)
  if (uploadingFiles.length > 0) {
    appStore.showToast(`还有${uploadingFiles.length}个文件正在上传，请稍候...`, 'warning')
    return
  }

  const failedFiles = previewFiles.value.filter(f => f.uploadError)
  if (failedFiles.length > 0) {
    appStore.showToast('有文件上传失败，请删除或重试', 'error')
    return
  }

  // 验证投票选项
  if (form.poll.enabled) {
    const validOptions = form.poll.options.filter(o => o.trim())
    if (validOptions.length < 2) {
      appStore.showToast('投票至少需要2个有效选项', 'error')
      return
    }
    if (!form.poll.endTime) {
      appStore.showToast('请设置投票截止时间', 'error')
      return
    }
    // 改进时间验证：使用统一的时间基准，添加1分钟缓冲
    const now = Date.now()
    const endTime = new Date(form.poll.endTime).getTime()
    const oneMinute = 60 * 1000

    if (endTime <= now + oneMinute) {
      appStore.showToast('投票截止时间必须晚于当前时间至少1分钟', 'error')
      return
    }
  }

  // 验证定时发布
  if (form.schedule.enabled) {
    if (!form.schedule.time) {
      appStore.showToast('请设置定时发布时间', 'error')
      return
    }
    const scheduleTime = new Date(form.schedule.time)
    if (scheduleTime <= new Date()) {
      appStore.showToast('定时发布时间必须晚于当前时间', 'error')
      return
    }
  }

  loading.value = true
  try {
    const postData = {
      userId: props.user.id,
      content: form.content,
      category: form.category,
      isAnonymous: form.isAnonymous ? 1 : 0,
      visibility: form.isAnonymous ? 0 : form.visibility,
      images: form.images.length > 0 ? JSON.stringify(form.images) : '',
      video: form.video,
      location: locationText.value,
      pollOptions: form.poll.enabled ? JSON.stringify(form.poll.options.filter(o => o)) : null,
      pollEndTime: form.poll.enabled && form.poll.endTime ? form.poll.endTime : null,
      scheduledTime: form.schedule.enabled ? form.schedule.time : null,
      forceSubmit: force,
      isAiAssisted: aiAssisted.value ? 1 : 0  // 是否使用了AI润色
    }
    const res = await request.post('/post/create', postData)
    if (res.code === '200') {
      isSubmitted.value = true
      const msg = props.user.creditScore >= 100 ? '功德 +1 🙏' : '信誉分 +1 🌟'
      appStore.showToast(`发布成功！${msg}`, 'success')
      localStorage.setItem('last_post_time', Date.now())
      localStorage.removeItem(`draft_${props.user.id}`)
      show.value = false
      resetForm()
      emit('refresh')
    } else if (res.code === '202') {
      if (confirm(res.msg)) { submit(true); return }
    } else {
      appStore.showToast(res.msg, 'error')
    }
  } catch (e) {
    appStore.showToast('发布出错', 'error')
  } finally {
    loading.value = false
  }
}

onUnmounted(() => { if (mapInstance) mapInstance.destroy() })
</script>

<template>
  <div>
    <Dialog :open="show" @update:open="show = $event">
      <DialogContent
        class="sm:max-w-[620px] p-0 rounded-3xl max-h-[92vh] overflow-hidden border-none shadow-2xl bg-white flex flex-col"
        :show-close="false">
        <DialogDescription class="sr-only">发帖表单</DialogDescription>

        <!-- 顶部区域 - 渐变背景 + 用户信息 -->
        <DialogHeader class="relative overflow-hidden">
          <!-- 装饰背景 -->
          <div class="absolute inset-0 bg-gradient-to-r from-[rgb(33,111,85)] via-emerald-500 to-teal-500 opacity-95">
          </div>
          <div class="absolute inset-0 opacity-20">
            <div class="absolute top-4 right-8 w-20 h-20 border border-white/30 rounded-full"></div>
            <div class="absolute bottom-2 left-12 w-12 h-12 border border-white/20 rounded-full"></div>
            <div class="absolute top-8 left-1/3 w-6 h-6 bg-white/10 rounded-full"></div>
          </div>

          <div class="relative z-10 p-5 pb-6">
            <div class="flex items-center justify-between mb-4">
              <div class="flex items-center gap-3">
                <div class="relative">
                  <Avatar class="h-12 w-12 border-2 border-white/30 shadow-lg ring-2 ring-white/20">
                    <AvatarImage :src="props.user?.avatar" />
                    <AvatarFallback class="bg-white/20 text-white font-bold">{{ props.user?.nickname?.[0] || 'U' }}
                    </AvatarFallback>
                  </Avatar>
                  <div
                    class="absolute -bottom-1 -right-1 w-5 h-5 bg-white rounded-full flex items-center justify-center shadow-md">
                    <Zap class="w-3 h-3 text-[rgb(33,111,85)]" />
                  </div>
                </div>
                <div>
                  <h3 class="text-white font-bold text-base drop-shadow-sm">{{ props.user?.nickname || '用户' }}</h3>
                  <p class="text-white/70 text-xs flex items-center gap-1">
                    <span class="w-1.5 h-1.5 bg-green-300 rounded-full animate-pulse"></span>
                    正在创作中...
                  </p>
                </div>
              </div>
              <button type="button"
                class="h-9 w-9 rounded-full bg-white/10 hover:bg-white/20 text-white backdrop-blur-sm flex items-center justify-center transition-colors"
                @click="show = false">
                <X class="w-5 h-5" />
              </button>
            </div>

            <DialogTitle class="text-white/90 text-sm font-normal flex items-center gap-2">
              <Sparkles class="w-4 h-4 text-yellow-300 animate-pulse" />
              分享你的校园生活，让更多人看到~
            </DialogTitle>
          </div>
        </DialogHeader>

        <!-- 主内容区域 -->
        <div class="overflow-y-auto flex-1 min-h-0 no-scrollbar">
          <div class="p-5 space-y-5">

            <!-- 输入框区域 - 带动画效果 -->
            <div class="relative group">
              <div
                class="absolute -inset-1 bg-gradient-to-r from-[rgb(33,111,85)]/20 via-emerald-500/20 to-teal-500/20 rounded-2xl blur-sm opacity-0 group-focus-within:opacity-100 transition-opacity duration-300">
              </div>
              <div
                class="relative bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden transition-all duration-300"
                :class="textareaFocused ? 'ring-2 ring-[rgb(33,111,85)]/30 border-[rgb(33,111,85)]/50' : ''">
                <textarea v-model="form.content" :placeholder="currentPlaceholder" maxlength="2000"
                  @focus="textareaFocused = true" @blur="textareaFocused = false"
                  class="w-full min-h-[140px] text-base border-none bg-transparent resize-none p-4 pr-10 focus:outline-none placeholder:text-gray-400"></textarea>
                <!-- 放大按钮 -->
                <button @click="expandedEditor = true"
                  class="absolute top-3 right-3 w-7 h-7 rounded-lg bg-gray-100/80 hover:bg-[rgb(33,111,85)] text-gray-400 hover:text-white flex items-center justify-center transition-all duration-200 opacity-0 group-hover:opacity-100"
                  title="全屏编辑" aria-label="全屏编辑">
                  <Maximize2 class="w-4 h-4" />
                </button>
              </div>
              <!-- 字数统计 + 进度条 - 移到输入框外部 -->
              <div class="mt-2 flex items-center justify-between px-1">
                <div class="flex-1 h-1.5 bg-gray-100 rounded-full overflow-hidden mr-3">
                  <div class="h-full transition-all duration-300 rounded-full" :class="progressColor"
                    :style="{ width: contentProgress + '%' }"></div>
                </div>
                <span class="text-xs font-medium shrink-0"
                  :class="form.content.length > 1800 ? 'text-red-500 animate-pulse' : 'text-gray-400'">
                  {{ form.content.length }}/2000
                  <span v-if="form.content.length > 1800" class="ml-1">⚠️ 即将达到上限</span>
                </span>
              </div>
              <!-- 字数警告提示 - 问题3修复：更直观的字数限制提示 -->
              <div v-if="form.content.length > 1500"
                class="mt-2 px-3 py-2 rounded-lg text-xs flex items-center gap-2 animate-in fade-in transition-all"
                :class="form.content.length > 1900 ? 'bg-red-100 border border-red-300 text-red-700' : form.content.length > 1800 ? 'bg-red-50 border border-red-200 text-red-600' : 'bg-yellow-50 border border-yellow-200 text-yellow-700'">
                <span>{{ form.content.length > 1900 ? '🚨' : form.content.length > 1800 ? '⚠️' : '💡' }}</span>
                <span v-if="form.content.length > 1900">
                  <strong>仅剩 {{ 2000 - form.content.length }} 字！</strong> 请立即精简内容
                </span>
                <span v-else-if="form.content.length > 1800">
                  还可输入 {{ 2000 - form.content.length }} 字，建议精简内容
                </span>
                <span v-else>
                  已输入 {{ form.content.length }} 字，接近限制（2000字）
                </span>
              </div>
            </div>

            <!-- 图片/视频预览 - 统一缩略图样式 -->
            <div v-if="previewFiles.length" class="space-y-2">
              <!-- 上传进度提示 -->
              <div v-if="previewFiles.some(f => f.uploading || f.uploadSuccess)"
                class="flex items-center gap-2 text-xs text-gray-500 bg-gray-50 px-3 py-2 rounded-lg">
                <Loader2 v-if="previewFiles.some(f => f.uploading)" class="w-3 h-3 animate-spin text-blue-500" />
                <CheckCircle2 v-else class="w-3 h-3 text-green-500" />
                <span>
                  已上传 {{previewFiles.filter(f => f.uploadSuccess).length}} / {{ previewFiles.length }} 张
                  <span v-if="previewFiles.some(f => f.compressing)" class="text-amber-500">（压缩中...）</span>
                  <span v-else-if="previewFiles.some(f => f.uploading)" class="text-blue-500">（上传中...）</span>
                  <span v-else-if="previewFiles.every(f => f.uploadSuccess)" class="text-green-500">（全部完成）</span>
                </span>
              </div>

              <div class="flex flex-wrap gap-2">
                <!-- 遍历所有文件（图片和视频） -->
                <div v-for="(file, idx) in previewFiles" :key="idx"
                  class="relative w-20 h-20 rounded-lg overflow-hidden border border-gray-200 shadow-sm group cursor-pointer"
                  @click="handleMediaClick(file)">
                  <!-- 图片 -->
                  <img v-if="file.type === 'image'" :src="file.url"
                    class="w-full h-full object-cover transition-transform duration-200 group-hover:scale-105" />
                  <!-- 视频缩略图 -->
                  <template v-else>
                    <video :src="file.url" class="w-full h-full object-cover" muted></video>
                    <!-- 视频播放图标 -->
                    <div v-if="!file.uploading"
                      class="absolute inset-0 flex items-center justify-center bg-black/20 group-hover:bg-black/40 transition-colors">
                      <div class="w-8 h-8 rounded-full bg-white/90 flex items-center justify-center shadow-lg">
                        <div
                          class="w-0 h-0 border-t-[6px] border-t-transparent border-l-[10px] border-l-blue-500 border-b-[6px] border-b-transparent ml-1">
                        </div>
                      </div>
                    </div>
                    <!-- 视频标签 -->
                    <div
                      class="absolute bottom-1 left-1 px-1.5 py-0.5 bg-blue-500 text-white text-[10px] rounded flex items-center gap-0.5">
                      <Video class="w-2.5 h-2.5" />
                    </div>
                  </template>

                  <!-- 上传进度遮罩 -->
                  <div v-if="file.uploading || file.compressing"
                    class="absolute inset-0 bg-black/50 backdrop-blur-sm flex flex-col items-center justify-center gap-1">
                    <!-- 压缩状态 -->
                    <template v-if="file.compressing">
                      <Loader2 class="w-6 h-6 animate-spin text-amber-400" />
                      <span class="text-[10px] text-white font-medium">压缩中</span>
                    </template>

                    <!-- 上传状态 -->
                    <template v-else>
                      <div class="relative w-8 h-8">
                        <svg class="w-8 h-8 -rotate-90">
                          <circle cx="16" cy="16" r="12" stroke="white" stroke-opacity="0.3" stroke-width="2"
                            fill="none" />
                          <circle cx="16" cy="16" r="12" stroke="white" stroke-width="2" fill="none"
                            :stroke-dasharray="75.4" :stroke-dashoffset="75.4 - (75.4 * (file.progress || 0) / 100)"
                            class="transition-all duration-300" />
                        </svg>
                        <span
                          class="absolute inset-0 flex items-center justify-center text-white text-[10px] font-bold">
                          {{ file.progress || 0 }}
                        </span>
                      </div>
                    </template>
                  </div>

                  <!-- 上传成功标记 -->
                  <div v-if="file.uploadSuccess && !file.uploading"
                    class="absolute top-1 right-1 w-6 h-6 bg-green-500 rounded-full flex items-center justify-center shadow-md animate-in zoom-in">
                    <CheckCircle2 class="w-4 h-4 text-white" />
                  </div>

                  <!-- 上传失败遮罩 + 重试按钮 -->
                  <div v-if="file.uploadError"
                    class="absolute inset-0 bg-red-500/90 backdrop-blur-sm flex flex-col items-center justify-center gap-1">
                    <AlertTriangle class="w-5 h-5 text-white" />
                    <button @click.stop="retryUpload(idx)"
                      class="px-2 py-1 bg-white text-red-500 text-[10px] font-bold rounded hover:bg-red-50 transition-colors">
                      重试
                    </button>
                  </div>

                  <!-- 删除按钮 -->
                  <button @click.stop="removeFile(idx)"
                    class="absolute -top-1 -right-1 w-5 h-5 bg-black/70 hover:bg-red-500 text-white rounded-full flex items-center justify-center transition-all duration-200 shadow-md z-10">
                    <X class="w-3 h-3" />
                  </button>
                </div>

                <!-- 添加更多图片按钮 -->
                <div v-if="previewFiles.filter(f => f.type === 'image').length < 9" @click="fileInput.click()"
                  class="w-20 h-20 rounded-lg border-2 border-dashed border-gray-300 hover:border-[rgb(33,111,85)] flex flex-col items-center justify-center cursor-pointer transition-colors bg-gray-50 hover:bg-emerald-50/50 group">
                  <Plus class="w-5 h-5 text-gray-400 group-hover:text-[rgb(33,111,85)] transition-colors" />
                  <span class="text-[10px] text-gray-400 group-hover:text-[rgb(33,111,85)] mt-0.5">图片</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 工具栏 - 更精致的设计 -->
          <div class="relative">
            <div class="flex justify-between">
              <div @click="fileInput.click()" class="tool-btn-new group">
                <div
                  class="tool-icon bg-emerald-100 text-[rgb(33,111,85)] group-hover:bg-[rgb(33,111,85)] group-hover:text-white">
                  <ImagePlus class="w-4 h-4" />
                </div>
                <span class="tool-text">图片</span>
              </div>
              <div @click="videoInput.click()" class="tool-btn-new group">
                <div class="tool-icon bg-blue-100 text-blue-600 group-hover:bg-blue-600 group-hover:text-white">
                  <Video class="w-4 h-4" />
                </div>
                <span class="tool-text">视频</span>
              </div>
              <div @click="form.poll.enabled = !form.poll.enabled" class="tool-btn-new group"
                :class="form.poll.enabled ? 'ring-2 ring-orange-300' : ''">
                <div class="tool-icon bg-orange-100 text-orange-600 group-hover:bg-orange-600 group-hover:text-white"
                  :class="form.poll.enabled ? 'bg-orange-600 text-white' : ''">
                  <BarChart2 class="w-4 h-4" />
                </div>
                <span class="tool-text">投票</span>
              </div>
              <div @click="openMapSelector" class="tool-btn-new group"
                :class="locationText ? 'ring-2 ring-cyan-300' : ''">
                <div class="tool-icon bg-cyan-100 text-cyan-600 group-hover:bg-cyan-600 group-hover:text-white"
                  :class="locationText ? 'bg-cyan-600 text-white' : ''">
                  <MapPin class="w-4 h-4" />
                </div>
                <span class="tool-text truncate max-w-[60px]">{{ locationText || '定位' }}</span>
                <X v-if="locationText" @click.stop="clearLocation"
                  class="w-3 h-3 ml-0.5 text-gray-400 hover:text-red-500" />
              </div>
              <div @click="form.schedule.enabled = !form.schedule.enabled" class="tool-btn-new group"
                :class="form.schedule.enabled ? 'ring-2 ring-purple-300' : ''">
                <div class="tool-icon bg-purple-100 text-purple-600 group-hover:bg-purple-600 group-hover:text-white"
                  :class="form.schedule.enabled ? 'bg-purple-600 text-white' : ''">
                  <Clock class="w-4 h-4" />
                </div>
                <span class="tool-text">定时</span>
              </div>
              <div @click="showAiAssistant = true" class="tool-btn-new group">
                <div
                  class="tool-icon bg-gradient-to-br from-pink-100 to-purple-100 text-pink-600 group-hover:from-pink-500 group-hover:to-purple-500 group-hover:text-white">
                  <Sparkles class="w-4 h-4" />
                </div>
                <span class="tool-text">AI</span>
              </div>
            </div>
          </div>

          <input type="file" ref="fileInput" multiple accept="image/*" class="hidden"
            @change="handleFileChange($event, 'image')" />
          <input type="file" ref="videoInput" accept="video/*" class="hidden"
            @change="handleFileChange($event, 'video')" />

          <!-- 投票选项 -->
          <div v-if="form.poll.enabled"
            class="bg-gradient-to-br from-orange-50 to-amber-50 p-4 rounded-2xl border border-orange-100 space-y-3 animate-in fade-in slide-in-from-top-2">
            <div class="flex justify-between items-center">
              <div class="flex items-center gap-2 text-sm font-bold text-orange-700">
                <BarChart2 class="w-4 h-4" /> 创建投票
              </div>
              <X class="w-4 h-4 text-orange-400 cursor-pointer hover:text-orange-600"
                @click="form.poll.enabled = false" />
            </div>
            <div v-for="(_, i) in form.poll.options" :key="i" class="flex items-center gap-2">
              <div
                class="w-6 h-6 rounded-full bg-orange-200 text-orange-700 flex items-center justify-center text-xs font-bold">
                {{ i + 1 }}</div>
              <Input v-model="form.poll.options[i]" :placeholder="'选项 ' + (i + 1)"
                class="flex-1 bg-white border-orange-200 focus:border-orange-400" />
              <Trash2 class="w-4 h-4 text-orange-300 cursor-pointer hover:text-red-500 transition-colors"
                @click="removePollOption(i)" />
            </div>
            <Button variant="ghost" size="sm"
              class="w-full text-xs border border-dashed border-orange-300 text-orange-600 hover:bg-orange-100"
              @click="form.poll.options.push('')">
              <Plus class="w-3 h-3 mr-1" />添加选项
            </Button>
            <!-- 投票截止时间 -->
            <div class="pt-2 border-t border-orange-200/50 space-y-2">
              <div class="flex items-center gap-2 text-xs text-orange-600">
                <Clock class="w-3.5 h-3.5" />
                <span>截止时间</span>
                <span class="text-orange-400">(可选)</span>
              </div>
              <div class="flex flex-wrap gap-2">
                <span v-for="opt in [
                  { label: '1小时', hours: 1 },
                  { label: '6小时', hours: 6 },
                  { label: '1天', hours: 24 },
                  { label: '3天', hours: 72 },
                  { label: '7天', hours: 168 }
                ]" :key="opt.hours" class="px-3 py-1.5 text-xs rounded-full cursor-pointer transition-all" :class="form.poll.endTime === getQuickEndTime(opt.hours)
                  ? 'bg-orange-500 text-white'
                  : 'bg-white border border-orange-200 text-orange-600 hover:bg-orange-50'"
                  @click="setQuickEndTime(opt.hours)">
                  {{ opt.label }}
                </span>
                <span
                  class="px-3 py-1.5 text-xs rounded-full cursor-pointer transition-all bg-white border border-orange-200 text-orange-600 hover:bg-orange-50"
                  @click="showCustomEndTime = !showCustomEndTime">
                  自定义
                </span>
              </div>
              <div v-if="showCustomEndTime" class="flex items-center gap-2">
                <Input type="datetime-local" v-model="form.poll.endTime"
                  class="flex-1 h-8 text-xs bg-white border-orange-200" :min="getMinDateTime()" />
                <span v-if="form.poll.endTime" class="text-xs text-orange-500">
                  {{ formatEndTimeDisplay(form.poll.endTime) }}
                </span>
              </div>
            </div>
          </div>

          <!-- 定时发布 -->
          <div v-if="form.schedule.enabled"
            class="bg-gradient-to-br from-purple-50 to-indigo-50 p-4 rounded-2xl border border-purple-100 flex items-center justify-between animate-in fade-in slide-in-from-top-2">
            <div class="flex items-center gap-2 text-sm font-bold text-purple-700">
              <Clock class="w-4 h-4" /> 定时发布
            </div>
            <Input type="datetime-local" v-model="form.schedule.time"
              class="w-48 h-9 text-sm bg-white border-purple-200" />
          </div>

          <!-- 分区选择 - 卡片式设计 -->
          <div class="space-y-3">
            <div class="text-sm font-bold text-gray-700 flex items-center gap-2">
              <span class="w-1 h-4 bg-[rgb(33,111,85)] rounded-full"></span>
              选择分区
              <span class="text-xs font-normal text-gray-400">(必选)</span>
            </div>

            <!-- 该区域在数据未加载时显示骨架屏 -->
            <div v-if="allCategories.length === 0" class="grid grid-cols-4 gap-3">
              <div v-for="i in 8" :key="i" class="h-[88px] rounded-xl bg-gray-100 animate-pulse"></div>
            </div>

            <!-- 前8个基础板块：卡片式 4列 2行 -->
            <div v-else class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3">
              <div v-for="cat in coreCategories" :key="cat.id"
                class="relative cursor-pointer p-4 rounded-xl border-2 transition-all duration-200 flex flex-col items-center gap-2 group hover:scale-105"
                :class="form.category === cat.id
                  ? 'border-[rgb(33,111,85)] bg-emerald-50 shadow-md scale-105'
                  : 'border-gray-100 bg-white hover:border-gray-200 hover:shadow-sm'" @click="form.category = cat.id">
                <div class="w-10 h-10 rounded-xl flex items-center justify-center transition-all duration-200"
                  :class="form.category === cat.id ? 'bg-[rgb(33,111,85)] text-white' : cat.bg + ' ' + cat.color">
                  <component :is="cat.icon" class="w-5 h-5" />
                </div>
                <span class="text-sm font-medium text-center"
                  :class="form.category === cat.id ? 'text-[rgb(33,111,85)]' : 'text-gray-600'">{{ cat.name }}</span>
                <span class="text-[10px] text-gray-400 text-center leading-tight">{{ cat.desc }}</span>
                <!-- 选中标记 -->
                <div v-if="form.category === cat.id"
                  class="absolute -top-1.5 -right-1.5 w-6 h-6 bg-[rgb(33,111,85)] rounded-full flex items-center justify-center shadow-md">
                  <CheckCircle2 class="w-4 h-4 text-white" />
                </div>
              </div>
            </div>

            <!-- 扩展板块：水平标签排列，仅在有扩展板块时显示 -->
            <div v-if="extraCategories.length > 0" class="pt-2">
              <div class="flex items-center gap-2 mb-2">
                <div class="flex-1 h-px bg-gray-100"></div>
                <span class="text-[10px] text-gray-400 px-2 whitespace-nowrap">自定义板块</span>
                <div class="flex-1 h-px bg-gray-100"></div>
              </div>
              <div class="flex flex-wrap gap-2">
                <button
                  v-for="cat in extraCategories" :key="cat.id"
                  type="button"
                  class="inline-flex items-center gap-1.5 px-3 py-2 rounded-full border-2 text-sm font-medium transition-all duration-200"
                  :class="form.category === cat.id
                    ? 'border-[rgb(33,111,85)] bg-emerald-50 text-[rgb(33,111,85)] shadow-sm'
                    : 'border-gray-200 bg-white text-gray-600 hover:border-emerald-300 hover:bg-emerald-50/50'"
                  @click="form.category = cat.id">
                  <component :is="cat.icon" class="w-4 h-4" />
                  {{ cat.name }}
                  <CheckCircle2 v-if="form.category === cat.id" class="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- 底部操作栏 - 固定在底部 -->
        <div class="border-t border-gray-100 bg-white/95 backdrop-blur-sm p-4 shrink-0">
          <div class="flex justify-between items-center">
            <div class="flex items-center gap-3">
              <!-- 匿名开关 -->
              <div
                class="flex items-center gap-2 bg-gray-50 px-3 py-2 rounded-full border border-gray-100 hover:border-gray-200 transition-colors">
                <Switch id="ano" :checked="form.isAnonymous" @update:checked="form.isAnonymous = $event" />
                <Label for="ano" class="cursor-pointer text-sm text-gray-600 flex items-center gap-1">
                  <Eye v-if="!form.isAnonymous" class="w-3.5 h-3.5" />
                  <span v-else>🎭</span>
                  {{ form.isAnonymous ? '匿名' : '实名' }}
                </Label>
              </div>

            </div>

            <!-- 发布按钮 -->
            <Button
              class="relative overflow-hidden bg-gradient-to-r from-[rgb(33,111,85)] to-emerald-600 hover:from-[rgb(28,95,72)] hover:to-emerald-700 text-white px-8 py-2.5 rounded-full font-bold shadow-lg shadow-emerald-500/25 transition-all duration-300 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed"
              :disabled="loading" @click="submit(false)">
              <span v-if="loading" class="flex items-center gap-2">
                <Loader2 class="w-4 h-4 animate-spin" />
                发送中...
              </span>
              <span v-else class="flex items-center gap-2">
                <Send class="w-4 h-4" />
                发 布
              </span>
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>

    <!-- AI助手弹窗 -->
    <AiAssistant :open="showAiAssistant" @update:open="val => showAiAssistant = val" :user-id="props.user?.id"
      :current-content="form.content" @apply="handleAiApply" />

    <!-- 地图选择弹窗 -->
    <Dialog :open="showMapDialog" @update:open="showMapDialog = $event">
      <DialogContent
        class="sm:max-w-[500px] bg-white p-0 h-[80vh] max-h-[600px] flex flex-col overflow-hidden rounded-2xl"
        :show-close="false">
        <DialogDescription class="sr-only">地图选点窗口</DialogDescription>
        <DialogHeader class="p-4 border-b shrink-0 bg-gradient-to-r from-cyan-50 to-teal-50">
          <DialogTitle class="flex items-center gap-2 text-cyan-700">
            <MapPin class="w-5 h-5" /> 选择所在位置
          </DialogTitle>
        </DialogHeader>
        <div class="h-[220px] shrink-0 w-full relative bg-gray-100">
          <div id="map-container" class="w-full h-full"></div>

          <!-- 定位失败遮罩层 + 重试按钮 -->
          <div v-if="locationError && !mapLoading"
            class="absolute inset-0 z-20 bg-white/95 backdrop-blur-sm flex flex-col items-center justify-center p-6">
            <AlertTriangle class="w-16 h-16 text-yellow-500 mb-4 animate-bounce" />
            <h3 class="text-lg font-bold text-gray-800 mb-2">定位失败</h3>
            <p class="text-sm text-gray-600 text-center mb-6 max-w-xs">
              {{ locationErrorMsg || '无法获取您的位置，请检查定位权限或网络连接' }}
            </p>
            <div class="flex gap-3">
              <Button @click="retryLocation"
                class="bg-[rgb(33,111,85)] hover:bg-[rgb(28,95,72)] text-white rounded-xl px-6">
                <RefreshCw class="w-4 h-4 mr-2" />
                重新定位
              </Button>
              <Button @click="clearLocationAndClose" variant="outline" class="rounded-xl">
                取消
              </Button>
            </div>
            <p class="text-xs text-gray-400 mt-4">
              💡 提示：您也可以直接搜索地点名称
            </p>
          </div>

          <!-- 中心标记 -->
          <div v-if="!locationError"
            class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 text-red-500 pointer-events-none z-10 mb-4">
            <MapPin class="w-8 h-8 fill-current animate-bounce drop-shadow-lg" />
          </div>

          <!-- 缩放提示 -->
          <div v-if="!locationError"
            class="absolute bottom-2 left-2 bg-black/50 text-white text-[10px] px-2 py-1 rounded-full z-10">
            拖动地图选择位置
          </div>
        </div>
        <!-- 搜索框 -->
        <div class="p-3 border-b bg-white shrink-0">
          <div class="relative">
            <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input v-model="mapSearchKeyword" @input="handleMapSearch" placeholder="搜索地点..."
              class="w-full h-9 pl-9 pr-3 text-sm border border-gray-200 rounded-lg focus:outline-none focus:border-cyan-400 focus:ring-1 focus:ring-cyan-100 bg-gray-50" />
          </div>
        </div>
        <div class="flex-1 min-h-[200px] overflow-y-auto p-2 bg-zinc-50 custom-scrollbar">
          <!-- 历史位置 -->
          <div v-if="!mapSearchKeyword && recentLocations.length > 0 && !mapLoading" class="mb-3">
            <div class="text-xs text-gray-500 px-2 mb-2 flex items-center gap-1">
              <Clock class="w-3 h-3" /> 最近使用
            </div>
            <div class="flex flex-wrap gap-2 px-2">
              <span v-for="loc in recentLocations" :key="loc"
                class="group flex items-center px-3 py-1.5 bg-white border border-gray-200 rounded-full text-xs text-gray-600 hover:border-cyan-400 hover:bg-cyan-50 transition">
                <span class="cursor-pointer" @click="selectRecentLocation(loc)">{{ loc }}</span>
                <X class="w-3 h-3 ml-1.5 text-gray-300 hover:text-red-500 cursor-pointer transition-colors"
                  @click.stop="removeRecentLocation(loc)" title="删除记录" />
              </span>
            </div>
          </div>

          <div v-if="mapLoading" class="text-center py-4 text-gray-400 text-xs">📍 正在定位...</div>
          <div v-else-if="mapSearchLoading" class="text-center py-4 text-gray-400 text-xs">🔍 搜索中...</div>
          <div v-else-if="poiList.length === 0" class="text-center py-4 text-gray-400 text-xs">
            {{ mapSearchKeyword ? '未找到相关地点' : '暂无结果' }}
          </div>
          <div v-else class="space-y-1">
            <div v-for="(poi, i) in poiList" :key="i"
              class="p-3 bg-white border border-gray-100 rounded-xl cursor-pointer hover:border-cyan-400 hover:bg-cyan-50 transition flex justify-between items-center group"
              @click="selectPoi(poi)">
              <div>
                <div class="text-sm font-bold text-gray-800">{{ poi.name }}</div>
                <div class="text-xs text-gray-500 mt-0.5">{{ poi.address }}</div>
              </div>
              <CheckCircle2 class="w-5 h-5 text-cyan-500 opacity-0 group-hover:opacity-100 transition-opacity" />
            </div>
          </div>
        </div>
        <DialogFooter class="p-3 border-t shrink-0">
          <Button variant="outline" class="w-full rounded-xl" @click="showMapDialog = false">关闭</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- 视频预览弹窗 -->
    <Dialog :open="showVideoPreview" @update:open="showVideoPreview = $event">
      <DialogContent class="sm:max-w-[90vw] md:max-w-[600px] p-0 rounded-2xl overflow-hidden bg-black"
        :show-close="false">
        <DialogDescription class="sr-only">视频预览</DialogDescription>
        <div class="relative">
          <video :src="previewVideoUrl" class="w-full max-h-[70vh] object-contain" controls autoplay></video>
          <button @click="showVideoPreview = false"
            class="absolute top-3 right-3 w-8 h-8 bg-black/60 hover:bg-black/80 text-white rounded-full flex items-center justify-center transition-all">
            <X class="w-5 h-5" />
          </button>
        </div>
      </DialogContent>
    </Dialog>

    <!-- 图片预览弹窗 -->
    <Dialog :open="showImagePreview" @update:open="showImagePreview = $event">
      <DialogContent class="sm:max-w-[90vw] md:max-w-[800px] p-0 rounded-2xl overflow-hidden bg-black"
        :show-close="false">
        <DialogDescription class="sr-only">图片预览</DialogDescription>
        <div class="relative">
          <img :src="previewImageUrl" class="w-full max-h-[80vh] object-contain" />
          <button @click="showImagePreview = false"
            class="absolute top-3 right-3 w-8 h-8 bg-black/60 hover:bg-black/80 text-white rounded-full flex items-center justify-center transition-all">
            <X class="w-5 h-5" />
          </button>
        </div>
      </DialogContent>
    </Dialog>

    <!-- 全屏编辑模式 -->
    <Dialog :open="expandedEditor" @update:open="expandedEditor = $event">
      <DialogContent
        class="sm:max-w-[90vw] md:max-w-[800px] h-[85vh] p-0 rounded-2xl border-none shadow-2xl flex flex-col overflow-hidden"
        :show-close="false">
        <DialogDescription class="sr-only">全屏编辑模式</DialogDescription>
        <!-- 顶部栏 -->
        <div
          class="flex items-center justify-between px-5 py-4 border-b border-gray-100 bg-gradient-to-r from-emerald-50 to-teal-50 shrink-0">
          <div class="flex items-center gap-3">
            <div
              class="w-10 h-10 rounded-xl bg-gradient-to-br from-[rgb(33,111,85)] to-emerald-600 flex items-center justify-center shadow-md">
              <Sparkles class="w-5 h-5 text-white" />
            </div>
            <div>
              <h3 class="font-bold text-gray-800">专注写作模式</h3>
              <p class="text-xs text-gray-500">更大的空间，更好的创作体验</p>
            </div>
          </div>
          <Button variant="ghost" size="icon" class="h-9 w-9 rounded-full hover:bg-white/80"
            @click="expandedEditor = false">
            <Minimize2 class="w-5 h-5 text-gray-500" />
          </Button>
        </div>

        <!-- 编辑区域 -->
        <div class="flex-1 p-5 overflow-hidden flex flex-col">
          <textarea ref="expandedTextareaRef" v-model="form.content" :placeholder="currentPlaceholder" maxlength="2000"
            class="flex-1 w-full text-lg leading-relaxed border-none bg-transparent resize-none p-4 focus:outline-none placeholder:text-gray-400 custom-scrollbar"></textarea>
        </div>

        <!-- 底部状态栏 -->
        <div class="px-5 py-4 border-t border-gray-100 bg-gray-50/50 shrink-0">
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-4">
              <!-- 进度条 -->
              <div class="w-32 h-2 bg-gray-200 rounded-full overflow-hidden">
                <div class="h-full transition-all duration-300 rounded-full" :class="progressColor"
                  :style="{ width: contentProgress + '%' }"></div>
              </div>
              <span class="text-sm" :class="form.content.length > 1800 ? 'text-red-500 font-medium' : 'text-gray-500'">
                {{ form.content.length }} / 2000 字
              </span>
            </div>
            <div class="flex items-center gap-3">
              <Button variant="outline" class="rounded-xl" @click="expandedEditor = false">
                返回
              </Button>
              <Button
                class="bg-gradient-to-r from-[rgb(33,111,85)] to-emerald-600 hover:from-[rgb(28,95,72)] hover:to-emerald-700 text-white rounded-xl px-6"
                @click="expandedEditor = false">
                <CheckCircle2 class="w-4 h-4 mr-2" />
                完成编辑
              </Button>
            </div>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  </div>
</template>

<style scoped>
.tool-btn-new {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.375rem 0.5rem;
  border-radius: 0.75rem;
  cursor: pointer;
  transition: all 0.2s;
  scroll-snap-align: start;
  flex-shrink: 0;
}

.tool-btn-new:hover {
  background-color: rgb(249 250 251);
}

.tool-icon {
  width: 2rem;
  height: 2rem;
  border-radius: 0.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.tool-text {
  font-size: 0.75rem;
  font-weight: 500;
  color: rgb(75 85 99);
}

.no-scrollbar::-webkit-scrollbar {
  display: none;
}

.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background: #ddd;
  border-radius: 2px;
}

/* 动画 */
@keyframes shimmer {
  0% {
    background-position: -200% 0;
  }

  100% {
    background-position: 200% 0;
  }
}

.animate-shimmer {
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.4), transparent);
  background-size: 200% 100%;
  animation: shimmer 2s infinite;
}

@keyframes shake {

  0%,
  100% {
    transform: translateX(0);
  }

  25% {
    transform: translateX(-10px);
  }

  75% {
    transform: translateX(10px);
  }
}

.animate-shake {
  animation: shake 0.5s ease-in-out;
}
</style>
