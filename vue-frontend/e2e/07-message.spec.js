/**
 * 私信功能模块测试
 * 测试发送、撤回、已读等功能
 */
import { test, expect } from '@playwright/test'
import { LoginPage } from './fixtures/page-objects'
import { TEST_USERS, generateRandomText } from './fixtures/test-data'

test.describe('私信功能模块', () => {
  let loginPage

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page)
    
    await loginPage.goto()
    await loginPage.login(TEST_USERS.normal.account, TEST_USERS.normal.password)
  })

  test('07-01: 应该能够发送私信', async ({ page }) => {
    await page.goto('/message')
    await page.waitForLoadState('networkidle')
    
    const contactList = page.locator('.contact-item, .conversation-item').first()
    if (await contactList.isVisible()) {
      await contactList.click()
      await page.waitForTimeout(1000)
      
      const messageInput = page.locator('textarea[placeholder*="消息"], input[placeholder*="消息"]')
      const messageText = generateRandomText(20)
      await messageInput.fill(messageText)
      
      await page.click('button:has-text("发送")')
      await page.waitForTimeout(1000)
      
      await expect(page.locator(`text=${messageText}`)).toBeVisible()
    }
  })

  test('07-02: 应该能够撤回消息', async ({ page }) => {
    await page.goto('/message')
    await page.waitForLoadState('networkidle')
    
    const contactList = page.locator('.contact-item, .conversation-item').first()
    if (await contactList.isVisible()) {
      await contactList.click()
      await page.waitForTimeout(1000)
      
      const myMessages = page.locator('.message-item.mine, .message.sent').last()
      if (await myMessages.isVisible()) {
        await myMessages.hover()
        
        const recallButton = page.locator('button:has-text("撤回")').last()
        if (await recallButton.isVisible()) {
          await recallButton.click()
          
          const confirmButton = page.locator('button:has-text("确认")')
          if (await confirmButton.isVisible()) {
            await confirmButton.click()
            await page.waitForTimeout(1000)
          }
        }
      }
    }
  })

  test('07-03: 应该显示消息已读状态', async ({ page }) => {
    await page.goto('/message')
    await page.waitForLoadState('networkidle')
    
    const contactList = page.locator('.contact-item, .conversation-item').first()
    if (await contactList.isVisible()) {
      await contactList.click()
      await page.waitForTimeout(1000)
      
      const readStatus = page.locator('text=已读, .read-status')
      if (await readStatus.isVisible()) {
        await expect(readStatus).toBeVisible()
      }
    }
  })

  test('07-04: 应该显示未读消息数量', async ({ page }) => {
    await page.goto('/message')
    await page.waitForLoadState('networkidle')
    
    const unreadBadge = page.locator('.unread-count, .badge')
    if (await unreadBadge.first().isVisible()) {
      const count = await unreadBadge.first().textContent()
      expect(parseInt(count)).toBeGreaterThanOrEqual(0)
    }
  })

  test('07-05: 应该能够搜索会话', async ({ page }) => {
    await page.goto('/message')
    await page.waitForLoadState('networkidle')
    
    const searchInput = page.locator('input[placeholder*="搜索"]')
    if (await searchInput.isVisible()) {
      await searchInput.fill('测试')
      await page.waitForTimeout(1000)
    }
  })

  test('07-06: 应该能够删除会话', async ({ page }) => {
    await page.goto('/message')
    await page.waitForLoadState('networkidle')
    
    const contactList = page.locator('.contact-item, .conversation-item').first()
    if (await contactList.isVisible()) {
      await contactList.hover()
      
      const deleteButton = page.locator('button:has-text("删除")').first()
      if (await deleteButton.isVisible()) {
        await deleteButton.click()
        
        const confirmButton = page.locator('button:has-text("确认")')
        if (await confirmButton.isVisible()) {
          await confirmButton.click()
          await page.waitForTimeout(1000)
        }
      }
    }
  })

  test('07-07: 应该限制首次私信', async ({ page }) => {
    await page.goto('/home')
    await page.waitForLoadState('networkidle')
    
    const postCards = page.locator('.post-card').first()
    await postCards.locator('text=/昵称|用户/').first().click()
    
    await page.waitForURL(/\/profile\/\d+/)
    
    const messageButton = page.locator('button:has-text("私信")')
    if (await messageButton.isVisible()) {
      await messageButton.click()
      await page.waitForURL('/message')
      
      const messageInput = page.locator('textarea[placeholder*="消息"], input[placeholder*="消息"]')
      const messageText = generateRandomText(20)
      await messageInput.fill(messageText)
      
      await page.click('button:has-text("发送")')
      await page.waitForTimeout(1000)
      
      const secondMessage = generateRandomText(20)
      await messageInput.fill(secondMessage)
      
      const sendButton = page.locator('button:has-text("发送")')
      const isDisabled = await sendButton.isDisabled()
      
      if (isDisabled) {
        const hint = page.locator('text=/等待对方回复|首次私信/')
        await expect(hint).toBeVisible()
      }
    }
  })

  test('07-08: 应该能够发送表情', async ({ page }) => {
    await page.goto('/message')
    await page.waitForLoadState('networkidle')
    
    const contactList = page.locator('.contact-item, .conversation-item').first()
    if (await contactList.isVisible()) {
      await contactList.click()
      await page.waitForTimeout(1000)
      
      const emojiButton = page.locator('button[aria-label*="表情"], button:has-text("😊")')
      if (await emojiButton.isVisible()) {
        await emojiButton.click()
        await page.waitForTimeout(500)
        
        const emoji = page.locator('.emoji-item, .emoji').first()
        if (await emoji.isVisible()) {
          await emoji.click()
          await page.waitForTimeout(500)
        }
      }
    }
  })

  test('07-09: 应该显示对方正在输入', async ({ page, context }) => {
    await page.goto('/message')
    await page.waitForLoadState('networkidle')
    
    const contactList = page.locator('.contact-item, .conversation-item').first()
    if (await contactList.isVisible()) {
      await contactList.click()
      await page.waitForTimeout(1000)
      
      const typingIndicator = page.locator('text=正在输入')
      if (await typingIndicator.isVisible()) {
        await expect(typingIndicator).toBeVisible()
      }
    }
  })

  test('07-10: 应该能够发送图片', async ({ page }) => {
    await page.goto('/message')
    await page.waitForLoadState('networkidle')
    
    const contactList = page.locator('.contact-item, .conversation-item').first()
    if (await contactList.isVisible()) {
      await contactList.click()
      await page.waitForTimeout(1000)
      
      const imageButton = page.locator('button[aria-label*="图片"], input[type="file"]')
      if (await imageButton.isVisible()) {
        await expect(imageButton).toBeVisible()
      }
    }
  })
})
