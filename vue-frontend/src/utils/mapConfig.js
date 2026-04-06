/**
 * 高德地图配置
 * 统一管理地图API Key和默认配置
 * 密钥通过 Vite 环境变量注入，不在源代码中硬编码
 * 本地开发：复制 .env.local.example 为 .env.local 并填入真实 Key
 */

// 高德地图API Key和Secret，从环境变量读取
export const AMAP_KEY = import.meta.env.VITE_AMAP_KEY || ''
export const AMAP_SECRET = import.meta.env.VITE_AMAP_SECRET || ''

// 默认中心点坐标（张家界学院）
export const DEFAULT_CENTER = [110.4802, 29.1312]

// 地图默认配置
export const MAP_CONFIG = {
  zoom: 14,
  mapStyle: 'amap://styles/light',
  resizeEnable: true,
  touchZoom: true,
  scrollWheel: true
}

// 地点搜索配置
export const PLACE_SEARCH_CONFIG = {
  type: '学校|餐饮服务|商务住宅|生活服务|风景名胜|地名地址信息|交通设施服务',
  pageSize: 20,
  pageIndex: 1,
  extensions: 'base'
}

// 定位配置
export const GEOLOCATION_CONFIG = {
  enableHighAccuracy: true,
  timeout: 10000,
  maximumAge: 0,
  convert: true,
  showButton: false,
  showMarker: false,
  showCircle: false,
  panToLocation: false,
  zoomToAccuracy: false
}

// 地理编码配置
export const GEOCODER_CONFIG = {
  radius: 500,
  extensions: 'all'
}

/**
 * 加载高德地图脚本
 * @returns {Promise<void>}
 */
export function loadAmapScript() {
  return new Promise((resolve, reject) => {
    if (window.AMap) {
      resolve()
      return
    }

    const script = document.createElement('script')
    // 使用securityJsCode方式加载（适用于需要Secret的服务）
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${AMAP_KEY}&plugin=AMap.PlaceSearch,AMap.Geolocation,AMap.CitySearch,AMap.Geocoder,AMap.ToolBar,AMap.Scale`
    
    script.onload = () => {
      // 设置安全密钥
      if (window._AMapSecurityConfig) {
        window._AMapSecurityConfig.securityJsCode = AMAP_SECRET
      } else {
        window._AMapSecurityConfig = {
          securityJsCode: AMAP_SECRET
        }
      }
      console.log('高德地图加载成功，安全密钥已配置')
      resolve()
    }
    
    script.onerror = () => {
      console.error('高德地图加载失败')
      reject(new Error('高德地图加载失败'))
    }
    
    document.head.appendChild(script)
  })
}

/**
 * 初始化地图安全配置
 * 必须在加载地图脚本之前调用
 */
export function initAmapSecurity() {
  window._AMapSecurityConfig = {
    securityJsCode: AMAP_SECRET
  }
}
