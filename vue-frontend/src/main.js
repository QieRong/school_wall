// File: vue-frontend/src/main.js
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { MotionPlugin } from '@vueuse/motion'
import './style.css'
import App from './App.vue'
import router from './router'
import { logger } from './utils/logger'

logger.log('🚀 开始初始化Vue应用...')

const app = createApp(App)

app.use(createPinia()) // 启用 Pinia
app.use(router)
app.use(MotionPlugin)  // 启用 Motion 动画

// 【修复白屏】添加错误处理
app.config.errorHandler = (err, instance, info) => {
  logger.error('Vue应用错误:', err, info)
}

app.mount('#app')

logger.log('✅ Vue应用挂载完成')