import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath } from 'url'

export default defineConfig({
  plugins: [vue()],
  test: {
    // 测试环境
    environment: 'happy-dom',
    
    // 全局 API
    globals: true,
    
    // 测试文件匹配模式
    include: ['src/**/*.{test,spec}.{js,ts}', 'tests/**/*.{test,spec}.{js,ts}'],
    
    // 排除目录
    exclude: ['node_modules', 'dist'],
    
    // 覆盖率配置
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
      exclude: [
        'node_modules/',
        'src/main.js',
        '**/*.d.ts',
        'tests/setup.js'
      ]
    },
    
    // 设置文件
    setupFiles: ['./tests/setup.js'],
    
    // 超时时间
    testTimeout: 10000,
    
    // 属性测试默认迭代次数
    fakeTimers: {
      toFake: ['setTimeout', 'clearTimeout', 'setInterval', 'clearInterval']
    },
    
    // 属性测试配置
    // fast-check 默认运行 100 次迭代
    // 可以在单个测试中通过 { numRuns: N } 覆盖
    propertyTestDefaults: {
      numRuns: 100,
      verbose: true
    }
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  }
})
