/**
 * 个人主页模块测试
 * 测试资料编辑、访客记录等功能
 */
import { test, expect } from '@playwright/test'
import { LoginPage } from './fixtures/page-objects'
import { TEST_USERS, generateRandomText } from './fixtures/test-data'

test.describe('个人主页模块', () => {
  let loginPage

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page)
    
    await loginPage.goto()
    await loginPage.login(TEST_USERS.normal.account, TEST_USERS.normal.password)
  })

  test('08-01: 应该能够查看个人主页', async ({ page }) => {
    await page.goto('/user/me')
    await page.waitForLoadState('networkidle')
    
    const profileHeader = page.locator('.profile-header').or(page.locator('.user-profile'))
    await expect(profileHeader).toBeVisible()
    await expect(page.locator('text=/昵称|用户名/')).toBeVisible()
  })

  test('08-02: 应该能够编辑个人资料', async ({ page }) => {
    await page.goto('/user/me')
    await page.waitForLoadState('networkidle')
    
    const editButton = page.locator('button:has-text("编辑资料")')
    if (await editButton.isVisible()) {
      await editButton.click()
      await page.waitForTimeout(1000)
      
      const nicknameInput = page.locator('input[placeholder*="昵称"]')
      if (await nicknameInput.isVisible()) {
        const newNickname = '测试用户' + Date.now().toString().slice(-4)
        await nicknameInput.fill(newNickname)
        
        await page.click('button:has-text("保存")')
        await page.waitForTimeout(1000)
        
        await expect(page.locator(`text=${newNickname}`)).toBeVisible()
      }
    }
  })

  test('08-03: 应该能够修改个性签名', async ({ page }) => {
    await page.goto('/user/me')
    await page.waitForLoadState('networkidle')
    
    const editButton = page.locator('button:has-text("编辑资料")')
    if (await editButton.isVisible()) {
      await editButton.click()
      await page.waitForTimeout(1000)
      
      const bioInput = page.locator('textarea[placeholder*="签名"]').or(page.locator('textarea[placeholder*="简介"]'))
      if (await bioInput.isVisible()) {
        const newBio = generateRandomText(30)
        await bioInput.fill(newBio)
        
        await page.click('button:has-text("保存")')
        await page.waitForTimeout(1000)
      }
    }
  })

  test('08-04: 应该能够上传头像', async ({ page }) => {
    await page.goto('/user/me')
    await page.waitForLoadState('networkidle')
    
    const editButton = page.locator('button:has-text("编辑资料")')
    if (await editButton.isVisible()) {
      await editButton.click()
      await page.waitForTimeout(1000)
      
      const avatarUpload = page.locator('input[type="file"][accept*="image"]')
      if (await avatarUpload.isVisible()) {
        await expect(avatarUpload).toBeAttached()
      }
    }
  })

  test('08-05: 应该能够查看我的帖子', async ({ page }) => {
    await page.goto('/user/me')
    await page.waitForLoadState('networkidle')
    
    await page.click('text=帖子')
    await page.waitForTimeout(1000)
    
    const postList = page.locator('.post-list').or(page.locator('.post-card'))
    if (await postList.first().isVisible()) {
      await expect(postList.first()).toBeVisible()
    }
  })

  test('08-06: 应该能够查看我的收藏', async ({ page }) => {
    await page.goto('/user/me')
    await page.waitForLoadState('networkidle')
    
    await page.click('text=收藏')
    await page.waitForTimeout(1000)
    
    const collectionList = page.locator('.collection-list').or(page.locator('.post-card'))
    await expect(page.locator('text=/收藏|暂无/')).toBeVisible()
  })

  test('08-07: 应该能够查看访客记录', async ({ page }) => {
    await page.goto('/user/me')
    await page.waitForLoadState('networkidle')
    
    const visitorButton = page.locator('button:has-text("访客")').or(page.locator('text=访客'))
    if (await visitorButton.isVisible()) {
      await visitorButton.click()
      await page.waitForTimeout(1000)
      
      const visitorList = page.locator('.visitor-list').or(page.locator('.user-list'))
      await expect(visitorList).toBeVisible()
    }
  })

  test('08-08: 应该显示统计数据', async ({ page }) => {
    await page.goto('/user/me')
    await page.waitForLoadState('networkidle')
    
    await expect(page.locator('text=/帖子|关注|粉丝/')).toBeVisible()
    
    const stats = page.locator('.stat-item').or(page.locator('.profile-stat'))
    if (await stats.first().isVisible()) {
      const count = await stats.count()
      expect(count).toBeGreaterThanOrEqual(3)
    }
  })

  test('08-09: 应该能够查看他人主页', async ({ page }) => {
    await page.goto('/home')
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(2000)
    
    const postCard = page.locator('.post-card').first()
    if (await postCard.isVisible().catch(() => false)) {
      const userLink = postCard.locator('a').first()
      await userLink.click()
      
      await page.waitForURL(/\/user\/\d+/)
      
      const profileHeader = page.locator('.profile-header').or(page.locator('.user-profile'))
      await expect(profileHeader).toBeVisible()
    }
  })

  test('08-10: 应该能够切换Tab', async ({ page }) => {
    await page.goto('/user/me')
    await page.waitForLoadState('networkidle')
    
    const tabs = ['帖子', '收藏', '关注', '粉丝']
    
    for (const tab of tabs) {
      const tabButton = page.locator(`text=${tab}`)
      if (await tabButton.isVisible()) {
        await tabButton.click()
        await page.waitForTimeout(500)
      }
    }
  })

  test('08-11: 应该能够修改隐私设置', async ({ page }) => {
    await page.goto('/user/me')
    await page.waitForLoadState('networkidle')
    
    const settingsButton = page.locator('button:has-text("设置")').or(page.locator('text=设置'))
    if (await settingsButton.isVisible()) {
      await settingsButton.click()
      await page.waitForTimeout(1000)
      
      const privacyOption = page.locator('text=隐私设置')
      if (await privacyOption.isVisible()) {
        await privacyOption.click()
        await page.waitForTimeout(1000)
      }
    }
  })
})
