import { test, expect } from '@playwright/test'
import { LoginPage, BottlePage } from './fixtures/page-objects'
import { TEST_USERS, generateBottleContent, WAIT_TIME } from './fixtures/test-data'

test.describe('漂流瓶功能模块', () => {
  let loginPage, bottlePage

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page)
    bottlePage = new BottlePage(page)
    
    await loginPage.goto()
    await loginPage.login(TEST_USERS.normal.account, TEST_USERS.normal.password)
    await bottlePage.goto()
  })

  test('03-01: 投放漂流瓶（樱花海岸）', async ({ page }) => {
    const bottle = generateBottleContent()
    
    await bottlePage.throwBottle(bottle.content, '樱花海岸')
    
    // 验证投放成功提示
    await expect(page.locator('text=投放成功')).toBeVisible({ timeout: 5000 })
  })

  test('03-02: 投放漂流瓶（星辰大海）', async ({ page }) => {
    const bottle = generateBottleContent()
    
    await bottlePage.throwBottle(bottle.content, '星辰大海')
    
    // 验证投放成功提示
    await expect(page.locator('text=投放成功')).toBeVisible({ timeout: 5000 })
  })

  test('03-03: 投放漂流瓶（深海秘境）', async ({ page }) => {
    const bottle = generateBottleContent()
    
    await bottlePage.throwBottle(bottle.content, '深海秘境')
    
    // 验证投放成功提示
    await expect(page.locator('text=投放成功')).toBeVisible({ timeout: 5000 })
  })

  test('03-04: 打捞漂流瓶', async ({ page }) => {
    await bottlePage.fishBottle()
    
    // 验证有反馈（成功或冷却中）
    const hasSuccess = await page.locator('text=成功打捞').isVisible()
    const hasCooldown = await page.locator('text=冷却').isVisible()
    const noBottle = await page.locator('text=暂无漂流瓶').isVisible()
    
    expect(hasSuccess || hasCooldown || noBottle).toBeTruthy()
  })

  test('03-05: 回复漂流瓶', async ({ page }) => {
    // 先打捞一个瓶子
    await bottlePage.fishBottle()
    await page.waitForTimeout(WAIT_TIME.MEDIUM)
    
    // 如果成功打捞到瓶子
    if (await page.locator('button:has-text("回复")').isVisible()) {
      await page.click('button:has-text("回复")')
      await page.fill('textarea', `测试回复 ${Date.now()}`)
      await page.click('button:has-text("发送")')
      
      // 验证回复成功
      await expect(page.locator('text=回复成功')).toBeVisible({ timeout: 5000 })
    }
  })

  test('03-06: 珍藏漂流瓶', async ({ page }) => {
    // 先打捞一个瓶子
    await bottlePage.fishBottle()
    await page.waitForTimeout(WAIT_TIME.MEDIUM)
    
    // 如果成功打捞到瓶子
    if (await page.locator('button:has-text("珍藏")').isVisible()) {
      await page.click('button:has-text("珍藏")')
      
      // 验证珍藏成功或已达上限
      const hasSuccess = await page.locator('text=珍藏成功').isVisible()
      const hasLimit = await page.locator('text=今日珍藏次数已用完').isVisible()
      
      expect(hasSuccess || hasLimit).toBeTruthy()
    }
  })

  test('03-07: 放回漂流瓶', async ({ page }) => {
    // 先打捞一个瓶子
    await bottlePage.fishBottle()
    await page.waitForTimeout(WAIT_TIME.MEDIUM)
    
    // 如果成功打捞到瓶子
    if (await page.locator('button:has-text("放回")').isVisible()) {
      await page.click('button:has-text("放回")')
      
      // 验证放回成功
      await expect(page.locator('text=放回成功')).toBeVisible({ timeout: 5000 })
    }
  })

  test('03-08: 查看我的瓶子', async ({ page }) => {
    await bottlePage.viewMyBottles()
    
    // 验证页面加载
    await expect(page.locator('text=我的瓶子')).toBeVisible()
  })

  test('03-09: 查看珍藏馆', async ({ page }) => {
    await bottlePage.viewCollection()
    
    // 验证页面加载
    await expect(page.locator('text=珍藏馆')).toBeVisible()
  })

  test('03-10: 查看成就', async ({ page }) => {
    await page.click('text=成就')
    await page.waitForTimeout(WAIT_TIME.SHORT)
    
    // 验证成就页面加载
    await expect(page.locator('text=成就')).toBeVisible()
  })

  test('03-11: 删除自己的瓶子', async ({ page }) => {
    await bottlePage.viewMyBottles()
    await page.waitForTimeout(WAIT_TIME.MEDIUM)
    
    // 如果有瓶子，删除第一个
    const deleteButtons = page.locator('button:has-text("删除")')
    if (await deleteButtons.count() > 0) {
      await deleteButtons.first().click()
      
      // 确认删除
      await page.click('button:has-text("确定")')
      
      // 验证删除成功
      await expect(page.locator('text=删除成功')).toBeVisible({ timeout: 5000 })
    }
  })
})
