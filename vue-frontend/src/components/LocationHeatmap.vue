<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { MapPin, Flame, TrendingUp, X, Maximize2, Minimize2 } from 'lucide-vue-next'
import request from '@/api/request'
import { useAppStore } from '@/stores/app'
import { AMAP_KEY, AMAP_SECRET, DEFAULT_CENTER, loadAmapScript, initAmapSecurity } from '@/utils/mapConfig'

const props = defineProps({
  open: Boolean,
  category: { type: Number, default: 1 }
})
const emit = defineEmits(['update:open'])

const appStore = useAppStore()
const show = ref(false)
const loading = ref(true)
const heatmapData = ref({ points: [], totalPosts: 0, maxHeat: 0, locationCount: 0 })
const selectedDays = ref(30)
const isFullscreen = ref(false)

let mapInstance = null

watch(() => props.open, (val) => {
  show.value = val
  if (val) {
    // 等待 Dialog 完全渲染后再初始化地图
    nextTick(() => {
      setTimeout(() => {
        fetchHeatmapData()
      }, 300)
    })
  } else {
    // 关闭时销毁地图
    if (mapInstance) {
      mapInstance.destroy()
      mapInstance = null
    }
  }
})
watch(show, (val) => emit('update:open', val))

const fetchHeatmapData = async () => {
  loading.value = true
  try {
    const res = await request.get('/stats/location-heatmap', {
      params: { category: props.category, days: selectedDays.value }
    })
    if (res.code === '200') {
      heatmapData.value = res.data
      initMap()
    }
  } catch (e) {
    appStore.showToast('加载热力图数据失败', 'error')
  } finally {
    loading.value = false
  }
}

const initMap = async () => {
  try {
    // 初始化安全配置
    initAmapSecurity()

    // 加载地图脚本
    await loadAmapScript()

    // 加载热力图插件（Loca）
    if (!window.Loca) {
      const loca = document.createElement('script')
      loca.src = `https://webapi.amap.com/loca?v=2.0&key=${AMAP_KEY}`
      await new Promise((resolve, reject) => {
        loca.onload = resolve
        loca.onerror = reject
        document.head.appendChild(loca)
      })
    }

    // 创建地图
    createMap()
  } catch (error) {
    console.error('地图初始化失败:', error)
    appStore.showToast('地图加载失败，请刷新重试', 'error')
    loading.value = false
  }
}

const createMap = async () => {
  const container = document.getElementById('heatmap-container')
  if (!container) {
    console.error('热力图容器未找到')
    return
  }

  // 先销毁旧地图
  if (mapInstance) {
    mapInstance.destroy()
    mapInstance = null
  }

  // 确保容器有正确的尺寸
  await nextTick()

  //确保容器有明确的尺寸
  const rect = container.getBoundingClientRect()
  if (rect.width === 0 || rect.height === 0) {
    console.warn('热力图容器尺寸为0，延迟初始化')
    setTimeout(() => createMap(), 200)
    return
  }

  mapInstance = new AMap.Map(container, {
    zoom: 15,
    center: DEFAULT_CENTER,
    mapStyle: 'amap://styles/whitesmoke',
    resizeEnable: true,
    touchZoom: true,       // 支持触摸缩放
    scrollWheel: true,     // 支持滚轮缩放
    doubleClickZoom: true  // 支持双击缩放
  })

  // 添加缩放控件
  AMap.plugin(['AMap.ToolBar', 'AMap.Scale'], () => {
    const toolbar = new AMap.ToolBar({
      position: 'RB',
      liteStyle: true
    })
    mapInstance.addControl(toolbar)

    const scale = new AMap.Scale()
    mapInstance.addControl(scale)
  })

  // 地图加载完成后调整
  mapInstance.on('complete', async () => {
    // 强制刷新地图尺寸
    setTimeout(() => {
      if (mapInstance) {
        mapInstance.resize()
        mapInstance.setCenter(DEFAULT_CENTER)
        mapInstance.setZoom(15)
      }
    }, 100)

    // 如果有数据点，进行地理编码并显示标记
    if (heatmapData.value.points.length > 0) {
      await addHeatPoints()
    }
  })
}

const addHeatPoints = async () => {
  const geocoder = new AMap.Geocoder({ city: '张家界' })
  const points = heatmapData.value.points
  const maxHeat = heatmapData.value.maxHeat || 1

  for (const point of points.slice(0, 20)) { // 最多显示20个点
    try {
      // 地理编码获取坐标
      const result = await new Promise((resolve) => {
        geocoder.getLocation(point.location, (status, result) => {
          if (status === 'complete' && result.geocodes.length > 0) {
            resolve(result.geocodes[0].location)
          } else {
            resolve(null)
          }
        })
      })

      if (result) {
        // 计算圆的大小和颜色
        const ratio = point.heat / maxHeat
        const radius = 30 + ratio * 70 // 30-100
        const opacity = 0.3 + ratio * 0.5 // 0.3-0.8

        // 添加圆形覆盖物
        const circle = new AMap.Circle({
          center: result,
          radius: radius,
          fillColor: getHeatColor(ratio),
          fillOpacity: opacity,
          strokeWeight: 0
        })
        mapInstance.add(circle)

        // 添加标记
        const marker = new AMap.Marker({
          position: result,
          content: `<div class="heatmap-marker" style="background: ${getHeatColor(ratio)}">
            <span class="count">${point.count}</span>
          </div>`,
          offset: new AMap.Pixel(-15, -15)
        })

        // 点击显示详情
        marker.on('click', () => {
          const info = new AMap.InfoWindow({
            content: `<div style="padding: 8px; min-width: 120px;">
              <div style="font-weight: bold; margin-bottom: 4px;">${point.location}</div>
              <div style="font-size: 12px; color: #666;">
                帖子数: ${point.count}<br/>
                点赞数: ${point.likes}
              </div>
            </div>`,
            offset: new AMap.Pixel(0, -20)
          })
          info.open(mapInstance, result)
        })

        mapInstance.add(marker)
      }
    } catch (e) {
      console.warn('地理编码失败:', point.location)
    }
  }

  // 自适应视野
  mapInstance.setFitView()
}

const getHeatColor = (ratio) => {
  // 从绿色到红色的渐变
  if (ratio < 0.3) return '#22c55e' // 绿色
  if (ratio < 0.6) return '#eab308' // 黄色
  if (ratio < 0.8) return '#f97316' // 橙色
  return '#ef4444' // 红色
}

const changeDays = (days) => {
  selectedDays.value = days
  fetchHeatmapData()
}

const toggleFullscreen = () => {
  isFullscreen.value = !isFullscreen.value
  // 切换全屏后重新调整地图
  nextTick(() => {
    setTimeout(() => {
      if (mapInstance) {
        mapInstance.resize()
        mapInstance.setCenter(DEFAULT_CENTER)
      }
    }, 300)
  })
}

onUnmounted(() => {
  if (mapInstance) {
    mapInstance.destroy()
    mapInstance = null
  }
})
</script>

<template>
  <Dialog :open="show" @update:open="show = $event">
    <DialogContent :class="isFullscreen
      ? 'fixed inset-4 max-w-none w-auto h-auto max-h-[95vh]'
      : 'sm:max-w-[700px] max-h-[85vh]'" class="bg-white p-0 rounded-2xl overflow-hidden flex flex-col"
      :showClose="false">
      <DialogDescription class="sr-only">表白热力图</DialogDescription>

      <DialogHeader class="p-4 border-b bg-gradient-to-r from-pink-50 to-red-50 flex-shrink-0">
        <div class="flex items-center justify-between">
          <DialogTitle class="text-lg font-bold text-gray-800 flex items-center gap-2">
            <Flame class="w-5 h-5 text-red-500" />
            表白热力图
            <Badge class="ml-2 bg-pink-100 text-pink-600 border-none">
              {{ heatmapData.locationCount }} 个地点
            </Badge>
          </DialogTitle>
          <div class="flex items-center gap-2">
            <Button variant="ghost" size="icon" class="h-8 w-8 hover:bg-pink-100" @click="toggleFullscreen" title="全屏">
              <Maximize2 v-if="!isFullscreen" class="w-4 h-4" />
              <Minimize2 v-else class="w-4 h-4" />
            </Button>
            <Button variant="ghost" size="icon" class="h-8 w-8 hover:bg-gray-100" @click="show = false" title="关闭">
              <X class="w-4 h-4" />
            </Button>
          </div>
        </div>

        <!-- 时间筛选 -->
        <div class="flex gap-2 mt-3">
          <Badge v-for="d in [7, 30, 90]" :key="d" :class="selectedDays === d
            ? 'bg-pink-500 text-white cursor-pointer'
            : 'bg-gray-100 text-gray-600 cursor-pointer hover:bg-gray-200'" @click="changeDays(d)">
            {{ d === 7 ? '近一周' : d === 30 ? '近一月' : '近三月' }}
          </Badge>
        </div>
      </DialogHeader>

      <!-- 地图容器 - 修复：使用固定高度确保地图正确显示 -->
      <div class="relative" :style="{ height: isFullscreen ? 'calc(100vh - 200px)' : '350px' }">
        <div id="heatmap-container" class="w-full h-full"></div>

        <!-- 加载中 -->
        <div v-if="loading" class="absolute inset-0 bg-white/80 flex items-center justify-center z-20">
          <div class="text-center">
            <div class="w-8 h-8 border-4 border-pink-500 border-t-transparent rounded-full animate-spin mx-auto"></div>
            <p class="mt-2 text-sm text-gray-500">加载中...</p>
          </div>
        </div>

        <!-- 缩放提示 -->
        <div v-if="!loading"
          class="absolute bottom-2 left-2 bg-black/50 text-white text-[10px] px-2 py-1 rounded-full z-10">
          双指/滚轮缩放地图
        </div>
      </div>

      <!-- 热门地点排行 -->
      <div class="p-4 border-t bg-gray-50 flex-shrink-0 max-h-[200px] overflow-y-auto">
        <div class="text-sm font-bold text-gray-700 mb-2 flex items-center gap-1">
          <TrendingUp class="w-4 h-4 text-pink-500" /> 热门表白地点
        </div>
        <div class="grid grid-cols-2 md:grid-cols-3 gap-2">
          <div v-for="(point, idx) in heatmapData.points.slice(0, 6)" :key="idx"
            class="flex items-center gap-2 p-2 bg-white rounded-lg border border-gray-100">
            <span class="w-5 h-5 rounded-full text-xs flex items-center justify-center font-bold"
              :class="idx < 3 ? 'bg-pink-500 text-white' : 'bg-gray-200 text-gray-600'">
              {{ idx + 1 }}
            </span>
            <div class="flex-1 min-w-0">
              <div class="text-sm font-medium text-gray-800 truncate">{{ point.location }}</div>
              <div class="text-xs text-gray-400">{{ point.count }} 条表白</div>
            </div>
          </div>
        </div>
      </div>
    </DialogContent>
  </Dialog>
</template>

<style>
.heatmap-marker {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 12px;
  font-weight: bold;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}
</style>
