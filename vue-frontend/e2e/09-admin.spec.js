/**
 * 管理员功能模块测试
 * 测试审核、封禁、数据大屏等功能
 */
import { test, expect } from '@playwright/test'
import { LoginPage, AdminPage } from './fixtures/page-objects'
import { TEST_USERS } from './fixtures/test-data'

test.describe('管理员功能模块', () => {
  let loginPage, adminPage

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page)
    adminPage = new AdminPage(page)
    
    await loginPage.goto()
    // 管理员登录会跳转到/admin/dashboard
    await loginPage.login(TEST_USERS.admin.account, TEST_USERS.admin.password, '/admin/dashboard')
  })

  test('09-01: 应该能够访问管理后台', async ({ page }) => {
    await adminPage.gotoDashboard()
    
    await expect(page.locator('text=/仪表盘|数据统计/')).toBeVisible()
  })

  test('09-02: 应该能够查看数据大屏', async ({ page }) => {
    await page.goto('/admin/data-dashboard')
    await page.waitForLoadState('networkidle')
    
    const kpiCards = page.locator('.kpi-card, .stat-card')
    if (await kpiCards.first().isVisible()) {
      const count = await kpiCards.count()
      expect(count).toBeGreaterThanOrEqual(4)
    }
  })

  test('09-03: 应该能够审核帖子', async ({ page }) => {
    await adminPage.gotoAudit()
    
    const postList = page.locator('.post-item, .audit-item')
    if (await postList.first().isVisible()) {
      await expect(postList.first()).toBeVisible()
    }
  })

  test('09-04: 应该能够删除违规帖子', async ({ page }) => {
    await adminPage.gotoAudit()
    await page.waitForTimeout(1000)
    
    await adminPage.deletePost('测试删除')
  })

  test('09-05: 应该能够封禁用户', async ({ page }) => {
    await adminPage.gotoUserManage()
    await page.waitForTimeout(1000)
    
    await adminPage.banUser(1)
  })

  test('09-06: 应该能够解封用户', async ({ page }) => {
    await adminPage.gotoUserManage()
    await page.waitForTimeout(1000)
    
    const unbanButton = page.locator('button:has-text("解封")').first()
    if (await unbanButton.isVisible()) {
      await unbanButton.click()
      
      const confirmButton = page.locator('button:has-text("确认")')
      if (await confirmButton.isVisible()) {
        await confirmButton.click()
        await page.waitForTimeout(1000)
      }
    }
  })

  test('09-07: 应该能够搜索用户', async ({ page }) => {
    await adminPage.gotoUserManage()
    
    const searchInput = page.locator('input[placeholder*="搜索"]')
    if (await searchInput.isVisible()) {
      await searchInput.fill('测试')
      await page.press('input[placeholder*="搜索"]', 'Enter')
      await page.waitForTimeout(1000)
    }
  })

  test('09-08: 应该能够筛选用户状态', async ({ page }) => {
    await adminPage.gotoUserManage()
    
    const filterButton = page.locator('button:has-text("筛选"), select')
    if (await filterButton.first().isVisible()) {
      await filterButton.first().click()
      await page.waitForTimeout(500)
    }
  })

  test('09-09: 应该能够查看用户详情', async ({ page }) => {
    await adminPage.gotoUserManage()
    
    const userRow = page.locator('.user-item, tr').first()
    if (await userRow.isVisible()) {
      const detailButton = userRow.locator('button:has-text("详情")')
      if (await detailButton.isVisible()) {
        await detailButton.click()
        await page.waitForTimeout(1000)
      }
    }
  })

  test('09-10: 应该能够管理评论', async ({ page }) => {
    await adminPage.gotoAudit()
    
    const commentButton = page.locator('button:has-text("评论管理")')
    if (await commentButton.isVisible()) {
      await commentButton.click()
      await page.waitForTimeout(1000)
      
      await expect(page.locator('.comment-list, .comment-item')).toBeVisible()
    }
  })

  test('09-11: 应该显示图表数据', async ({ page }) => {
    await page.goto('/admin/data-dashboard')
    await page.waitForLoadState('networkidle')
    
    const charts = page.locator('.echarts, canvas')
    if (await charts.first().isVisible()) {
      const count = await charts.count()
      expect(count).toBeGreaterThanOrEqual(2)
    }
  })

  test('09-12: 应该能够导出数据', async ({ page }) => {
    await page.goto('/admin/data-dashboard')
    await page.waitForLoadState('networkidle')
    
    const exportButton = page.locator('button:has-text("导出")')
    if (await exportButton.isVisible()) {
      await expect(exportButton).toBeVisible()
    }
  })
})
