// File: vite.config.js
import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import autoprefixer from 'autoprefixer'
import tailwind from 'tailwindcss'

export default defineConfig({
  css: {
    postcss: {
      plugins: [tailwind(), autoprefixer()],
    },
  },
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  // 【核心配置】解决跨域和端口问题
  server: {
    host: 'localhost', // 【备选方案1】只监听localhost
    port: 3000, // 【备选方案2】使用不同端口
    strictPort: false, // 如果端口被占用，自动尝试下一个端口
    proxy: {
      '/api': {
        target: 'http://localhost:19090', // 后端地址
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '') // 去掉 /api 前缀
      },
      '/files': {
        target: 'http://localhost:19090', // 文件访问代理
        changeOrigin: true
      },
      '/ws': {
        target: 'ws://localhost:19090', // WebSocket代理
        ws: true, // 启用WebSocket代理
        changeOrigin: true
      }
    }
  }
})