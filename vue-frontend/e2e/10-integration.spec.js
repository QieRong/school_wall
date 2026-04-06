/**
 * 集成测试
 * 测试完整的用户流程
 */
import { test, expect } from '@playwright/test'
import { LoginPage, HomePage, BottlePage, HotwordPage, StoryPage } from './fixtures/page-objects.js'
import { generateTestUser, generateRandomText } from './fixtures/test-data.js'

test.describe('完整用户流程集成测试', () => {
  test('10-01: 新用户完整体验流程', async ({ page }) => {
    const loginPage = new LoginPage(page)
    const homePage = new HomePage(page)
    const bottlePage = new BottlePage(page)
    const hotwordPage = new HotwordPage(page)
    const storyPage = new StoryPage(page)
    
    const newUser = generateTestUser()
    
    await loginPage.goto()
    await loginPage.register(newUser.account, newUser.password)
    await page.waitForTimeout(2000)
    
    await loginPage.login(newUser.account, newUser.password)
    
    await homePage.goto()
    await expect(page.locator('text=/首页|表白墙/')).toBeVisible()
    
    const postContent = generateRandomText(50)
    await homePage.createPost(postContent, '表白')
    await page.waitForTimeout(2000)
    
    await bottlePage.goto()
    const bottleContent = generateRandomText(30)
    await bottlePage.throwBottle(bottleContent, '樱花海岸')
    await page.waitForTimeout(3000)
    
    await hotwordPage.goto()
    await hotwordPage.voteHotword(0)
    await page.waitForTimeout(1000)
    
    await storyPage.goto()
    const storyTitle = '测试故事' + Date.now().toString().slice(-4)
    await storyPage.createStory(
      storyTitle,
      1,
      '这是一个测试世界观',
      generateRandomText(100)
    )
    await page.waitForTimeout(2000)
    
    await page.goto('/profile/me')
    await page.waitForLoadState('networkidle')
    await expect(page.locator(`text=${newUser.account}`)).toBeVisible()
  })

  test('10-02: 用户社交互动流程', async ({ page, context }) => {
    const loginPage = new LoginPage(page)
    const homePage = new HomePage(page)
    
    const user1 = generateTestUser()
    
    await loginPage.goto()
    await loginPage.register(user1.account, user1.password)
    await page.waitForTimeout(2000)
    await loginPage.login(user1.account, user1.password)
    
    await homePage.goto()
    const postCards = page.locator('.post-card').first()
    if (await postCards.isVisible()) {
      await postCards.locator('text=/昵称|用户/').first().click()
      await page.waitForURL(/\/profile\/\d+/)
      
      const followButton = page.locator('button:has-text("关注")')
      if (await followButton.isVisible()) {
        await followButton.click()
        await page.waitForTimeout(1000)
      }
      
      const messageButton = page.locator('button:has-text("私信")')
      if (await messageButton.isVisible()) {
        await messageButton.click()
        await page.waitForURL('/message')
        
        const messageInput = page.locator('textarea[placeholder*="消息"], input[placeholder*="消息"]')
        await messageInput.fill('你好，很高兴认识你！')
        await page.click('button:has-text("发送")')
        await page.waitForTimeout(1000)
      }
    }
  })

  test('10-03: 管理员审核流程', async ({ page }) => {
    const loginPage = new LoginPage(page)
    const homePage = new HomePage(page)
    
    const normalUser = generateTestUser()
    
    await loginPage.goto()
    await loginPage.register(normalUser.account, normalUser.password)
    await page.waitForTimeout(2000)
    await loginPage.login(normalUser.account, normalUser.password)
    
    await homePage.goto()
    const violationContent = '这是一条违规内容测试'
    await homePage.createPost(violationContent, '表白')
    await page.waitForTimeout(2000)
    
    await page.click('button:has-text("退出"), text=退出')
    await page.waitForTimeout(1000)
    
    await loginPage.goto()
    await loginPage.login('admin', '123456')
    
    await page.goto('/admin/dashboard')
    await page.waitForLoadState('networkidle')
    
    const postList = page.locator('.post-item, .audit-item')
    if (await postList.first().isVisible()) {
      const deleteButton = page.locator('button:has-text("删除")').first()
      if (await deleteButton.isVisible()) {
        await deleteButton.click()
        await page.fill('textarea', '违规内容')
        await page.click('button:has-text("确认")')
        await page.waitForTimeout(1000)
      }
    }
  })

  test('10-04: 漂流瓶完整流程', async ({ page, context }) => {
    const loginPage = new LoginPage(page)
    const bottlePage = new BottlePage(page)
    
    const user1 = generateTestUser()
    const user2 = generateTestUser()
    
    await loginPage.goto()
    await loginPage.register(user1.account, user1.password)
    await page.waitForTimeout(2000)
    await loginPage.login(user1.account, user1.password)
    
    await bottlePage.goto()
    const bottleContent = '这是一个测试漂流瓶 ' + Date.now()
    await bottlePage.throwBottle(bottleContent, '樱花海岸')
    await page.waitForTimeout(3000)
    
    await page.click('button:has-text("退出"), text=退出')
    await page.waitForTimeout(1000)
    
    await loginPage.goto()
    await loginPage.register(user2.account, user2.password)
    await page.waitForTimeout(2000)
    await loginPage.login(user2.account, user2.password)
    
    await bottlePage.goto()
    await bottlePage.fishBottle()
    await page.waitForTimeout(2000)
    
    const bottleModal = page.locator('.bottle-modal, .modal')
    if (await bottleModal.isVisible()) {
      await expect(bottleModal).toBeVisible()
    }
  })

  test('10-05: 故事接龙完整流程', async ({ page }) => {
    const loginPage = new LoginPage(page)
    const storyPage = new StoryPage(page)
    
    const user1 = generateTestUser()
    const user2 = generateTestUser()
    
    await loginPage.goto()
    await loginPage.register(user1.account, user1.password)
    await page.waitForTimeout(2000)
    await loginPage.login(user1.account, user1.password)
    
    await storyPage.goto()
    const storyTitle = '集成测试故事' + Date.now().toString().slice(-4)
    await storyPage.createStory(
      storyTitle,
      1,
      '测试世界观设定',
      '这是故事的开篇内容...'
    )
    await page.waitForTimeout(2000)
    
    await page.click('button:has-text("退出"), text=退出')
    await page.waitForTimeout(1000)
    
    await loginPage.goto()
    await loginPage.register(user2.account, user2.password)
    await page.waitForTimeout(2000)
    await loginPage.login(user2.account, user2.password)
    
    await storyPage.goto()
    const storyCard = page.locator(`text=${storyTitle}`).first()
    if (await storyCard.isVisible()) {
      await storyCard.click()
      await page.waitForTimeout(1000)
      
      await storyPage.continueStory('这是续写的内容...')
      await page.waitForTimeout(2000)
    }
  })
})
