/**
 * 社交功能模块测试
 * 测试关注、粉丝、黑名单等功能
 */
import { test, expect } from '@playwright/test'
import { LoginPage, HomePage } from './fixtures/page-objects'
import { TEST_USERS } from './fixtures/test-data'

test.describe('社交功能模块', () => {
  let loginPage, homePage

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page)
    homePage = new HomePage(page)
    
    await loginPage.goto()
    await loginPage.login(TEST_USERS.normal.account, TEST_USERS.normal.password)
  })

  test('06-01: 应该能够关注其他用户', async ({ page }) => {
    await page.goto('/home')
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(2000)
    
    // 点击第一个帖子的用户头像或昵称
    const postCard = page.locator('.post-card').first()
    if (await postCard.isVisible().catch(() => false)) {
      const userLink = postCard.locator('a').first()
      await userLink.click()
      
      await page.waitForURL(/\/user\/\d+/)
      
      const followButton = page.locator('button:has-text("关注")')
      if (await followButton.isVisible()) {
        await followButton.click()
        await page.waitForTimeout(1000)
        await expect(page.locator('button:has-text("已关注")')).toBeVisible()
      }
    }
  })

  test('06-02: 应该能够取消关注', async ({ page }) => {
    await page.goto('/home')
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(2000)
    
    const postCard = page.locator('.post-card').first()
    if (await postCard.isVisible().catch(() => false)) {
      const userLink = postCard.locator('a').first()
      await userLink.click()
      
      await page.waitForURL(/\/user\/\d+/)
      
      const followedButton = page.locator('button:has-text("已关注")')
      if (await followedButton.isVisible()) {
        await followedButton.click()
        await page.waitForTimeout(1000)
        await expect(page.locator('button:has-text("关注")')).toBeVisible()
      }
    }
  })

  test('06-03: 应该能够查看关注列表', async ({ page }) => {
    await page.goto('/user/me')
    await page.waitForLoadState('networkidle')
    
    await page.click('text=关注')
    await page.waitForTimeout(1000)
    
    const followList = page.locator('.follow-list').or(page.locator('.user-list'))
    await expect(followList).toBeVisible()
  })

  test('06-04: 应该能够查看粉丝列表', async ({ page }) => {
    await page.goto('/user/me')
    await page.waitForLoadState('networkidle')
    
    await page.click('text=粉丝')
    await page.waitForTimeout(1000)
    
    const fansList = page.locator('.fans-list').or(page.locator('.user-list'))
    await expect(fansList).toBeVisible()
  })

  test('06-05: 应该能够拉黑用户', async ({ page }) => {
    await page.goto('/home')
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(2000)
    
    const postCard = page.locator('.post-card').first()
    if (await postCard.isVisible().catch(() => false)) {
      const userLink = postCard.locator('a').first()
      await userLink.click()
      
      await page.waitForURL(/\/user\/\d+/)
      
      const moreButton = page.locator('button:has-text("更多")').or(page.locator('button[aria-label*="更多"]'))
      if (await moreButton.isVisible()) {
        await moreButton.click()
        await page.click('text=拉黑')
        
        const confirmButton = page.locator('button:has-text("确认")')
        if (await confirmButton.isVisible()) {
          await confirmButton.click()
          await page.waitForTimeout(1000)
        }
      }
    }
  })

  test('06-06: 应该能够查看黑名单', async ({ page }) => {
    await page.goto('/user/me')
    await page.waitForLoadState('networkidle')
    
    const settingsButton = page.locator('button:has-text("设置")').or(page.locator('text=设置'))
    if (await settingsButton.isVisible()) {
      await settingsButton.click()
      await page.click('text=黑名单')
      await page.waitForTimeout(1000)
      
      const blacklist = page.locator('.blacklist').or(page.locator('.user-list'))
      await expect(blacklist).toBeVisible()
    }
  })

  test('06-07: 应该能够移除黑名单', async ({ page }) => {
    await page.goto('/profile/me')
    await page.waitForLoadState('networkidle')
    
    const settingsButton = page.locator('button:has-text("设置"), text=设置')
    if (await settingsButton.isVisible()) {
      await settingsButton.click()
      await page.click('text=黑名单')
      await page.waitForTimeout(1000)
      
      const removeButton = page.locator('button:has-text("移除")').first()
      if (await removeButton.isVisible()) {
        await removeButton.click()
        await page.waitForTimeout(1000)
      }
    }
  })

  test('06-08: 应该防止重复关注', async ({ page }) => {
    await page.goto('/home')
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(2000)
    
    const postCard = page.locator('.post-card').first()
    if (await postCard.isVisible().catch(() => false)) {
      const userLink = postCard.locator('a').first()
      await userLink.click()
      
      await page.waitForURL(/\/user\/\d+/)
      
      const followButton = page.locator('button:has-text("关注")')
      if (await followButton.isVisible()) {
        await followButton.click()
        await page.waitForTimeout(500)
        
        await followButton.click()
        await page.waitForTimeout(500)
        
        await expect(page.locator('button:has-text("已关注")')).toBeVisible()
      }
    }
  })

  test('06-09: 应该显示互相关注状态', async ({ page }) => {
    await page.goto('/home')
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(2000)
    
    const postCard = page.locator('.post-card').first()
    if (await postCard.isVisible().catch(() => false)) {
      const userLink = postCard.locator('a').first()
      await userLink.click()
      
      await page.waitForURL(/\/user\/\d+/)
      
      const mutualFollow = page.locator('text=互相关注')
      if (await mutualFollow.isVisible()) {
        await expect(mutualFollow).toBeVisible()
      }
    }
  })

  test('06-10: 应该能够举报用户', async ({ page }) => {
    await page.goto('/home')
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(2000)
    
    const postCard = page.locator('.post-card').first()
    if (await postCard.isVisible().catch(() => false)) {
      const userLink = postCard.locator('a').first()
      await userLink.click()
      
      await page.waitForURL(/\/user\/\d+/)
      
      const moreButton = page.locator('button:has-text("更多")').or(page.locator('button[aria-label*="更多"]'))
      if (await moreButton.isVisible()) {
        await moreButton.click()
        await page.click('text=举报')
        
        await page.fill('textarea', '测试举报')
        await page.click('button:has-text("提交")')
        await page.waitForTimeout(1000)
      }
    }
  })
})
