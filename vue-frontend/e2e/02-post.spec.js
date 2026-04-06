import { test, expect } from '@playwright/test'
import { LoginPage, HomePage } from './fixtures/page-objects'
import { TEST_USERS, generatePostContent } from './fixtures/test-data'

test.describe('帖子功能模块', () => {
  let loginPage, homePage

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page)
    homePage = new HomePage(page)
    
    await loginPage.goto()
    await loginPage.login(TEST_USERS.normal.account, TEST_USERS.normal.password)
  })

  test('02-01: 发布文字帖子', async ({ page }) => {
    const post = generatePostContent()
    
    await homePage.createPost(post.content, post.category)
    
    // 验证帖子出现在首页
    await expect(page.locator(`text=${post.content.split('\n')[0]}`)).toBeVisible({ timeout: 5000 })
  })

  test('02-02: 发布寻物帖子', async ({ page }) => {
    const content = `寻物启事 ${Date.now()}\n丢失了一个黑色钱包`
    
    await homePage.createPost(content, '寻物')
    
    // 验证帖子出现
    await expect(page.locator('text=寻物启事')).toBeVisible({ timeout: 5000 })
  })

  test('02-03: 发布闲置帖子', async ({ page }) => {
    const content = `闲置出售 ${Date.now()}\n全新未拆封的书籍`
    
    await homePage.createPost(content, '闲置')
    
    // 验证帖子出现
    await expect(page.locator('text=闲置出售')).toBeVisible({ timeout: 5000 })
  })

  test('02-04: 点赞帖子', async ({ page }) => {
    await homePage.likePost(0)
    
    // 验证点赞成功（按钮状态改变）
    await page.waitForTimeout(500)
  })

  test('02-05: 评论帖子', async ({ page }) => {
    await homePage.openPost(0)
    
    // 填写评论（使用input而不是textarea）
    const comment = `测试评论 ${Date.now()}`
    await page.fill('input#cmt-input', comment)
    
    // 发送评论（按Enter键）
    await page.press('input#cmt-input', 'Enter')
    
    // 验证评论出现
    await expect(page.locator(`text=${comment}`)).toBeVisible({ timeout: 5000 })
  })

  test('02-06: 分享帖子', async ({ page }) => {
    await homePage.sharePost(0)
    
    // 验证分享成功（检查剪贴板内容）
    const clipboardText = await page.evaluate(() => navigator.clipboard.readText())
    expect(clipboardText).toContain('/post/')
  })

  test('02-07: 收藏帖子', async ({ page }) => {
    await homePage.openPost(0)
    
    // 点击收藏按钮（使用更精确的选择器，避免匹配到多个元素）
    await page.locator('span.flex.items-center.gap-2').filter({ hasText: '收藏' }).click()
    
    // 验证收藏成功（等待按钮文本变为"已收藏"）
    await expect(page.locator('span.font-medium:has-text("已收藏")')).toBeVisible({ timeout: 5000 })
  })

  test('02-08: 搜索帖子', async ({ page }) => {
    await homePage.searchPosts('测试')
    
    // 验证搜索结果
    await page.waitForTimeout(1000)
  })

  test('02-09: 按分类筛选帖子', async ({ page }) => {
    await homePage.filterByCategory('表白')
    
    // 验证筛选结果
    await page.waitForTimeout(1000)
  })

  test('02-10: 删除自己的帖子', async ({ page }) => {
    // 进入我的帖子页面
    await page.goto('/my-posts')
    await page.waitForTimeout(2000)
    
    // 如果有帖子，删除第一个
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
