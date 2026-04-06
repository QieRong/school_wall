import { test, expect } from '@playwright/test'
import { LoginPage } from './fixtures/page-objects'
import { TEST_USERS, generateTestUser } from './fixtures/test-data'

test.describe('用户认证模块', () => {
  let loginPage

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page)
    await loginPage.goto()
  })

  test('01-01: 用户注册成功', async ({ page }) => {
    const user = generateTestUser()
    
    await loginPage.register(user.account, user.password)
    
    // 验证注册成功提示
    await expect(page.locator('text=注册成功')).toBeVisible({ timeout: 5000 })
  })

  test('01-02: 用户登录成功', async ({ page }) => {
    await loginPage.login(TEST_USERS.normal.account, TEST_USERS.normal.password)
    
    // 验证跳转到首页
    await expect(page).toHaveURL('/home')
    
    // 验证用户头像显示（使用Avatar组件）
    const avatar = page.locator('.h-10.w-10').first()
    await expect(avatar).toBeVisible({ timeout: 5000 })
  })

  test('01-03: 错误密码登录失败', async ({ page }) => {
    // 确保在登录页面
    await page.goto('/login')
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(500)
    
    // 填写表单
    await page.fill('input[placeholder*="学号"]', TEST_USERS.normal.account)
    await page.waitForTimeout(300)
    
    await page.fill('input[type="password"]', 'wrongpassword123')
    await page.waitForTimeout(500)
    
    // 点击登录按钮（使用aria-label）
    const loginButton = page.locator('button[aria-label="登录"]')
    await loginButton.waitFor({ state: 'visible', timeout: 5000 })
    await loginButton.click({ force: true })
    
    // 验证错误提示（等待toast消息）
    await page.waitForTimeout(1000)
    const errorMessage = page.locator('text=/密码错误|密码不正确|登录失败/')
    await expect(errorMessage).toBeVisible({ timeout: 5000 })
  })

  test('01-04: 不存在的用户登录失败', async ({ page }) => {
    // 确保在登录页面
    await page.goto('/login')
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(500)
    
    // 填写表单
    await page.fill('input[placeholder*="学号"]', '9999999999')
    await page.waitForTimeout(300)
    
    await page.fill('input[type="password"]', '123456')
    await page.waitForTimeout(500)
    
    // 点击登录按钮（使用aria-label）
    const loginButton = page.locator('button[aria-label="登录"]')
    await loginButton.waitFor({ state: 'visible', timeout: 5000 })
    await loginButton.click({ force: true })
    
    // 验证错误提示
    await page.waitForTimeout(1000)
    const errorMessage = page.locator('text=/用户不存在|账号不存在|登录失败/')
    await expect(errorMessage).toBeVisible({ timeout: 5000 })
  })

  test('01-05: 管理员登录成功', async ({ page }) => {
    // 管理员登录会跳转到管理后台，不是首页
    await page.goto('/login')
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(500)
    
    // 如果是注册表单，切换到登录
    const loginSwitchButton = page.locator('button:has-text("已有账号？去登录")')
    if (await loginSwitchButton.isVisible().catch(() => false)) {
      await loginSwitchButton.click({ force: true })
      await page.waitForTimeout(1000)
    }
    
    // 填写管理员账号
    await page.fill('input[placeholder*="学号"]', TEST_USERS.admin.account)
    await page.waitForTimeout(300)
    
    await page.fill('input[type="password"]', TEST_USERS.admin.password)
    await page.waitForTimeout(300)
    
    // 点击登录
    const loginButton = page.locator('button:has-text("立 即 登 录")')
    await loginButton.waitFor({ state: 'visible', timeout: 5000 })
    await loginButton.click({ force: true })
    
    // 管理员会跳转到 /admin/dashboard
    await page.waitForURL('/admin/dashboard', { timeout: 10000 })
    
    // 验证在管理后台
    await expect(page).toHaveURL('/admin/dashboard')
  })

  test('01-06: 用户退出登录', async ({ page }) => {
    await loginPage.login(TEST_USERS.normal.account, TEST_USERS.normal.password)
    
    // 等待页面完全加载
    await page.waitForTimeout(2000)
    
    // 点击头像区域打开下拉菜单（使用aria-haspopup定位DropdownMenuTrigger）
    const avatarTrigger = page.locator('[aria-haspopup="menu"]').filter({ has: page.locator('.h-10.w-10') })
    await avatarTrigger.click()
    await page.waitForTimeout(1000)
    
    // 在下拉菜单中点击"退出登录"（DropdownMenuItem）
    const logoutMenuItem = page.locator('[role="menuitem"]').filter({ hasText: '退出登录' })
    await logoutMenuItem.waitFor({ state: 'visible', timeout: 5000 })
    await logoutMenuItem.click()
    await page.waitForTimeout(500)
    
    // 在确认对话框中点击"确认退出"按钮
    const confirmButton = page.locator('button:has-text("确认退出")')
    await confirmButton.waitFor({ state: 'visible', timeout: 5000 })
    await confirmButton.click()
    
    // 验证跳转到登录页
    await expect(page).toHaveURL('/login', { timeout: 5000 })
  })
})
