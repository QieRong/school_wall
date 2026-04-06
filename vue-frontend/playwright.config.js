import { defineConfig, devices } from '@playwright/test'

/**
 * Playwright 配置文件
 * 用于端到端测试
 */
export default defineConfig({
  testDir: './e2e',
  
  // 测试超时时间（30秒）
  timeout: 30 * 1000,
  
  // 每个断言的超时时间（5秒）
  expect: {
    timeout: 5000
  },
  
  // 失败时重试次数
  retries: process.env.CI ? 2 : 1,
  
  // 禁用并行执行，确保测试按顺序运行
  fullyParallel: false,
  
  // 单线程执行，避免数据竞争
  workers: 1,
  
  // 测试报告
  reporter: [
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
    ['list'],
    ['json', { outputFile: 'playwright-report/results.json' }]
  ],
  
  use: {
    // 基础 URL
    baseURL: 'http://localhost:3000',
    
    // 失败时截图
    screenshot: 'only-on-failure',
    
    // 录制视频
    video: 'retain-on-failure',
    
    // 追踪信息
    trace: 'on-first-retry',
    
    // 浏览器上下文选项
    viewport: { width: 1280, height: 720 },
    
    // 忽略 HTTPS 错误
    ignoreHTTPSErrors: true,
    
    // 操作超时
    actionTimeout: 10000,
    
    // 导航超时
    navigationTimeout: 30000,
    
    // 设置浏览器语言为中文
    locale: 'zh-CN',
    
    // 设置时区为中国
    timezoneId: 'Asia/Shanghai',
  },

  // 测试前启动开发服务器
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:3000',
    reuseExistingServer: !process.env.CI,
    timeout: 120 * 1000,
    stdout: 'ignore',
    stderr: 'pipe',
  },

  // 测试的浏览器
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    
    // 可选：移动端测试
    // {
    //   name: 'mobile-chrome',
    //   use: { ...devices['Pixel 5'] },
    // },
    
    // 可选：其他浏览器
    // {
    //   name: 'firefox',
    //   use: { ...devices['Desktop Firefox'] },
    // },
    // {
    //   name: 'webkit',
    //   use: { ...devices['Desktop Safari'] },
    // },
  ],
})
