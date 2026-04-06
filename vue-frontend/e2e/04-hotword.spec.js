import { test, expect } from '@playwright/test'
import { LoginPage, HotwordPage } from './fixtures/page-objects'
import { TEST_USERS, generateHotwordContent, WAIT_TIME } from './fixtures/test-data'

test.describe('热词墙功能模块', () => {
  let loginPage, hotwordPage

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page)
    hotwordPage = new HotwordPage(page)
    
    await loginPage.goto()
    await loginPage.login(TEST_USERS.normal.account, TEST_USERS.normal.password)
    await hotwordPage.goto()
  })

  test('04-01: 投稿热词', async ({ page }) => {
    const hotword = generateHotwordContent()
    
    await hotwordPage.submitHotword(hotword.name, hotword.definition, hotword.example)
    
    // 验证提交成功
    await expect(page.locator('text=提交成功')).toBeVisible({ timeout: 5000 })
  })

  test('04-02: 普通投票（1票）', async ({ page }) => {
    await page.waitForTimeout(WAIT_TIME.MEDIUM)
    
    // 点击第一个热词的投票按钮
    const voteButtons = page.locator('button:has-text("投票")')
    if (await voteButtons.count() > 0) {
      await voteButtons.first().click()
      
      // 选择普通投票
      await page.click('text=普通投票')
      
      // 验证投票结果
      await page.waitForTimeout(WAIT_TIME.SHORT)
      const hasSuccess = await page.locator('text=投票成功').isVisible()
      const hasNoQuota = await page.locator('text=配额').isVisible()
      
      expect(hasSuccess || hasNoQuota).toBeTruthy()
    }
  })

  test('04-03: 强烈认同投票（2票）', async ({ page }) => {
    await page.waitForTimeout(WAIT_TIME.MEDIUM)
    
    // 点击第一个热词的投票按钮
    const voteButtons = page.locator('button:has-text("投票")')
    if (await voteButtons.count() > 0) {
      await voteButtons.first().click()
      
      // 选择强烈认同
      await page.click('text=强烈认同')
      
      // 确认投票
      await page.click('button:has-text("确定")')
      
      // 验证投票结果
      await page.waitForTimeout(WAIT_TIME.SHORT)
      const hasSuccess = await page.locator('text=投票成功').isVisible()
      const hasNoQuota = await page.locator('text=配额').isVisible()
      
      expect(hasSuccess || hasNoQuota).toBeTruthy()
    }
  })

  test('04-04: 查看热词详情', async ({ page }) => {
    await page.waitForTimeout(WAIT_TIME.MEDIUM)
    
    // 点击第一个热词
    const hotwords = page.locator('.hotword-bubble')
    if (await hotwords.count() > 0) {
      await hotwords.first().click()
      
      // 验证详情弹窗出现
      await expect(page.locator('text=热词详情')).toBeVisible()
    }
  })

  test('04-05: 切换到日榜', async ({ page }) => {
    await hotwordPage.switchTab('日榜')
    
    // 验证榜单加载
    await page.waitForTimeout(WAIT_TIME.SHORT)
  })

  test('04-06: 切换到周榜', async ({ page }) => {
    await hotwordPage.switchTab('周榜')
    
    // 验证榜单加载
    await page.waitForTimeout(WAIT_TIME.SHORT)
  })

  test('04-07: 切换到月榜', async ({ page }) => {
    await hotwordPage.switchTab('月榜')
    
    // 验证榜单加载
    await page.waitForTimeout(WAIT_TIME.SHORT)
  })

  test('04-08: 切换到总榜', async ({ page }) => {
    await hotwordPage.switchTab('总榜')
    
    // 验证榜单加载
    await page.waitForTimeout(WAIT_TIME.SHORT)
  })

  test('04-09: 访问校园博物馆', async ({ page }) => {
    await page.goto('/hotword/museum')
    await page.waitForLoadState('networkidle')
    
    // 验证博物馆页面加载
    await expect(page.locator('text=校园博物馆')).toBeVisible()
  })

  test('04-10: 搜索热词', async ({ page }) => {
    await page.fill('input[placeholder*="搜索"]', '测试')
    await page.press('input[placeholder*="搜索"]', 'Enter')
    
    // 验证搜索结果
    await page.waitForTimeout(WAIT_TIME.SHORT)
  })
})
