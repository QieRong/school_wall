import { test, expect } from '@playwright/test'
import { LoginPage, StoryPage } from './fixtures/page-objects'
import { TEST_USERS, generateStoryContent, WAIT_TIME } from './fixtures/test-data'

test.describe('故事接龙功能模块', () => {
  let loginPage, storyPage

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page)
    storyPage = new StoryPage(page)
    
    await loginPage.goto()
    await loginPage.login(TEST_USERS.normal.account, TEST_USERS.normal.password)
    await storyPage.goto()
  })

  test('05-01: 创建故事（奇幻校园）', async ({ page }) => {
    const story = generateStoryContent()
    story.category = 1
    
    await storyPage.createStory(story.title, story.category, story.worldSetting, story.content)
    
    // 验证创建成功
    await expect(page.locator('text=创建成功')).toBeVisible({ timeout: 5000 })
  })

  test('05-02: 创建故事（悬疑推理）', async ({ page }) => {
    const story = generateStoryContent()
    story.category = 2
    
    await storyPage.createStory(story.title, story.category, story.worldSetting, story.content)
    
    // 验证创建成功
    await expect(page.locator('text=创建成功')).toBeVisible({ timeout: 5000 })
  })

  test('05-03: 创建故事（浪漫物语）', async ({ page }) => {
    const story = generateStoryContent()
    story.category = 3
    
    await storyPage.createStory(story.title, story.category, story.worldSetting, story.content)
    
    // 验证创建成功
    await expect(page.locator('text=创建成功')).toBeVisible({ timeout: 5000 })
  })

  test('05-04: 查看故事详情', async ({ page }) => {
    await page.waitForTimeout(WAIT_TIME.MEDIUM)
    
    // 点击第一个故事
    const stories = page.locator('.story-card')
    if (await stories.count() > 0) {
      await stories.first().click()
      
      // 验证详情页加载
      await page.waitForURL(/\/story\/\d+/)
      await expect(page.locator('text=世界观')).toBeVisible()
    }
  })

  test('05-05: 续写故事', async ({ page }) => {
    await page.waitForTimeout(WAIT_TIME.MEDIUM)
    
    // 点击第一个故事
    const stories = page.locator('.story-card')
    if (await stories.count() > 0) {
      await stories.first().click()
      await page.waitForURL(/\/story\/\d+/)
      
      // 尝试续写
      const continueButton = page.locator('button:has-text("续写")')
      if (await continueButton.isVisible()) {
        await continueButton.click()
        
        const content = `测试续写 ${Date.now()}\n这是续写的内容`
        await page.fill('textarea', content)
        await page.click('button:has-text("提交")')
        
        // 验证续写结果（成功或冷却中）
        await page.waitForTimeout(WAIT_TIME.SHORT)
        const hasSuccess = await page.locator('text=续写成功').isVisible()
        const hasCooldown = await page.locator('text=冷却').isVisible()
        const needRead = await page.locator('text=阅读').isVisible()
        
        expect(hasSuccess || hasCooldown || needRead).toBeTruthy()
      }
    }
  })

  test('05-06: 点赞段落', async ({ page }) => {
    await page.waitForTimeout(WAIT_TIME.MEDIUM)
    
    // 点击第一个故事
    const stories = page.locator('.story-card')
    if (await stories.count() > 0) {
      await stories.first().click()
      await page.waitForURL(/\/story\/\d+/)
      
      // 点赞第一个段落
      const likeButtons = page.locator('button[aria-label*="点赞"]')
      if (await likeButtons.count() > 0) {
        await likeButtons.first().click()
        await page.waitForTimeout(WAIT_TIME.ANIMATION)
      }
    }
  })

  test('05-07: 收藏故事', async ({ page }) => {
    await page.waitForTimeout(WAIT_TIME.MEDIUM)
    
    // 点击第一个故事
    const stories = page.locator('.story-card')
    if (await stories.count() > 0) {
      await stories.first().click()
      await page.waitForURL(/\/story\/\d+/)
      
      // 点击收藏按钮
      await page.click('button[aria-label*="收藏"]')
      await page.waitForTimeout(WAIT_TIME.ANIMATION)
    }
  })

  test('05-08: 查看贡献度排行', async ({ page }) => {
    await page.waitForTimeout(WAIT_TIME.MEDIUM)
    
    // 点击第一个故事
    const stories = page.locator('.story-card')
    if (await stories.count() > 0) {
      await stories.first().click()
      await page.waitForURL(/\/story\/\d+/)
      
      // 验证贡献度排行显示
      await expect(page.locator('text=贡献度排行')).toBeVisible()
    }
  })

  test('05-09: 查看我的故事', async ({ page }) => {
    await page.goto('/story/my')
    await page.waitForLoadState('networkidle')
    
    // 验证页面加载
    await expect(page.locator('text=我的故事')).toBeVisible()
  })

  test('05-10: 访问故事档案馆', async ({ page }) => {
    await page.goto('/story/archive')
    await page.waitForLoadState('networkidle')
    
    // 验证档案馆页面加载
    await expect(page.locator('text=故事档案馆')).toBeVisible()
  })
})
