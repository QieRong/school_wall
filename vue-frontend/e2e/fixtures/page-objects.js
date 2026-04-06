/**
 * Page Object Model
 * 封装页面操作，提高测试代码可维护性
 * 
 * 注意：所有按钮文本都已根据实际页面确认
 */

export class LoginPage {
  constructor(page) {
    this.page = page
  }

  async goto() {
    await this.page.goto('/login')
    await this.page.waitForLoadState('networkidle')
  }

  async login(account, password, expectedUrl = null) {
    // 确保在登录页面
    await this.page.goto('/login')
    await this.page.waitForLoadState('networkidle')
    await this.page.waitForTimeout(500)
    
    // 如果当前是注册表单，切换到登录表单
    const loginSwitchButton = this.page.locator('button:has-text("已有账号？去登录")')
    if (await loginSwitchButton.isVisible().catch(() => false)) {
      await loginSwitchButton.click({ force: true })
      await this.page.waitForTimeout(1000)
    }
    
    // 填写登录表单
    await this.page.fill('input[placeholder*="学号"]', account)
    await this.page.waitForTimeout(300)
    
    await this.page.fill('input[type="password"]', password)
    await this.page.waitForTimeout(300)
    
    // 点击登录按钮
    const loginButton = this.page.locator('button:has-text("立 即 登 录")')
    await loginButton.waitFor({ state: 'visible', timeout: 5000 })
    await loginButton.click({ force: true })
    
    // 等待跳转（管理员跳转到/admin/dashboard，普通用户跳转到/home）
    if (expectedUrl) {
      await this.page.waitForURL(expectedUrl, { timeout: 10000 })
    } else {
      // 等待URL变化（不指定具体路径）
      await this.page.waitForURL(/\/(home|admin)/, { timeout: 10000 })
    }
  }

  async register(account, password) {
    // 点击"去注册"按钮
    const registerButton = this.page.locator('button:has-text("没有账号？去注册")')
    await registerButton.waitFor({ state: 'visible', timeout: 10000 })
    await registerButton.click({ force: true })
    
    // 等待注册表单出现
    await this.page.waitForSelector('input[placeholder*="学号"]', { timeout: 5000 })
    await this.page.waitForTimeout(500)
    
    // 填写注册表单（只有账号、密码、确认密码）
    await this.page.fill('input[placeholder*="学号"]', account)
    await this.page.waitForTimeout(300)
    
    await this.page.fill('input[placeholder*="请输入密码"]', password)
    await this.page.waitForTimeout(300)
    
    await this.page.fill('input[placeholder*="请再次输入密码"]', password)
    await this.page.waitForTimeout(500)
    
    // 点击注册按钮（使用aria-label定位）
    const submitButton = this.page.locator('button[aria-label="注册"]')
    await submitButton.waitFor({ state: 'visible', timeout: 5000 })
    await this.page.waitForTimeout(500)
    await submitButton.click({ force: true })
    
    // 等待注册成功提示
    await this.page.waitForTimeout(2000)
  }
}

export class HomePage {
  constructor(page) {
    this.page = page
  }

  async goto() {
    await this.page.goto('/home')
    await this.page.waitForLoadState('networkidle')
  }

  async openPostModal() {
    // 等待页面加载完成
    await this.page.waitForTimeout(1000)
    
    // 方式1：点击侧边栏的发布卡片（包含"分享你的校园生活"文本）
    const publishCard = this.page.locator('text=分享你的校园生活').first()
    if (await publishCard.isVisible().catch(() => false)) {
      await publishCard.click()
      await this.page.waitForTimeout(1000)
      return
    }
    
    // 方式2：点击空状态的"发布第一条内容"按钮
    const emptyStateButton = this.page.locator('button:has-text("发布第一条内容")')
    if (await emptyStateButton.isVisible().catch(() => false)) {
      await emptyStateButton.click()
      await this.page.waitForTimeout(1000)
      return
    }
    
    throw new Error('无法找到发帖入口')
  }

  async createPost(content, category = '表白') {
    await this.openPostModal()
    
    // 等待对话框出现
    await this.page.waitForSelector('[role="dialog"]', { timeout: 5000 })
    await this.page.waitForTimeout(500)
    
    // 填写内容（使用第一个textarea）
    const textarea = this.page.locator('[role="dialog"] textarea').first()
    await textarea.waitFor({ state: 'visible', timeout: 5000 })
    await textarea.fill(content)
    await this.page.waitForTimeout(300)
    
    // 选择分类
    const categoryButton = this.page.locator('[role="dialog"] button').filter({ hasText: category })
    if (await categoryButton.isVisible().catch(() => false)) {
      await categoryButton.click()
      await this.page.waitForTimeout(300)
    }
    
    // 点击发布按钮（图片显示按钮文本是"发 布"，中间有空格）
    const publishButton = this.page.locator('[role="dialog"] button').filter({ hasText: /发\s*布/ })
    await publishButton.waitFor({ state: 'visible', timeout: 5000 })
    await publishButton.click()
    
    // 等待对话框关闭（发布成功后对话框会自动关闭）
    await this.page.waitForSelector('[role="dialog"]', { state: 'hidden', timeout: 10000 })
    
    // 等待页面刷新完成
    await this.page.waitForTimeout(2000)
  }

  async likePost(index = 0) {
    await this.page.waitForTimeout(2000)
    
    // 找到所有帖子的操作按钮组
    const actionButtons = this.page.locator('button[class*="rounded-xl"]').filter({
      has: this.page.locator('svg')
    })
    
    // 点赞按钮是每组的第1个（index * 3 + 0）
    await actionButtons.nth(index * 3).click()
    await this.page.waitForTimeout(500)
  }

  async openPost(index = 0) {
    await this.page.waitForTimeout(2000)
    
    // 点击帖子卡片 - 使用更精确的选择器
    const postCards = this.page.locator('[class*="rounded-2xl"][class*="bg-white"][class*="cursor-pointer"]').filter({
      has: this.page.locator('svg') // 确保是帖子卡片（包含图标）
    })
    
    const targetCard = postCards.nth(index)
    await targetCard.waitFor({ state: 'visible', timeout: 5000 })
    await targetCard.click()
    
    // 等待详情页加载
    await this.page.waitForTimeout(2000)
    
    // 验证是否成功跳转到详情页
    const currentUrl = this.page.url()
    if (!currentUrl.includes('/post/')) {
      throw new Error('未能成功跳转到帖子详情页')
    }
  }

  async sharePost(index = 0) {
    await this.page.waitForTimeout(2000)
    
    // 找到所有帖子的操作按钮组
    const actionButtons = this.page.locator('button[class*="rounded-xl"]').filter({
      has: this.page.locator('svg')
    })
    
    // 分享按钮是每组的第3个（index * 3 + 2）
    await actionButtons.nth(index * 3 + 2).click()
    
    // 等待分享操作完成（剪贴板写入需要时间）
    await this.page.waitForTimeout(1500)
  }

  async searchPosts(keyword) {
    await this.page.fill('input[placeholder*="搜索"]', keyword)
    await this.page.press('input[placeholder*="搜索"]', 'Enter')
    await this.page.waitForTimeout(1000)
  }

  async filterByCategory(category) {
    await this.page.click(`button:has-text("${category}")`)
    await this.page.waitForTimeout(1000)
  }
}

export class BottlePage {
  constructor(page) {
    this.page = page
  }

  async goto() {
    await this.page.goto('/bottle')
    await this.page.waitForLoadState('networkidle')
  }

  async throwBottle(content, direction = '樱花海岸') {
    // 点击"投放瓶子"按钮
    const throwButton = this.page.locator('button').filter({ hasText: /投放瓶子|投掷瓶子/ })
    await throwButton.click()
    await this.page.waitForTimeout(500)
    
    // 填写内容
    await this.page.fill('textarea', content)
    await this.page.waitForTimeout(300)
    
    // 选择方向（支持多种方向名称）
    const directionButton = this.page.locator('button').filter({ hasText: direction })
    if (await directionButton.isVisible().catch(() => false)) {
      await directionButton.click()
    }
    await this.page.waitForTimeout(300)
    
    // 点击"投入大海"按钮
    const submitButton = this.page.locator('button').filter({ hasText: /投入大海|确认投放|投放/ })
    await submitButton.click()
    await this.page.waitForTimeout(2000)
  }

  async fishBottle() {
    // 点击"打捞瓶子"按钮（支持多种文本）
    const fishButton = this.page.locator('button').filter({ hasText: /打捞瓶子|捞瓶子|打捞/ })
    await fishButton.click()
    await this.page.waitForTimeout(2000)
  }

  async viewMyBottles() {
    await this.page.click('text=我的瓶子')
    await this.page.waitForTimeout(1000)
  }

  async viewCollection() {
    await this.page.click('text=珍藏馆')
    await this.page.waitForTimeout(1000)
  }
}

export class HotwordPage {
  constructor(page) {
    this.page = page
  }

  async goto() {
    await this.page.goto('/hotword')
    await this.page.waitForLoadState('networkidle')
  }

  async submitHotword(name, definition, example) {
    const submitButton = this.page.locator('button:has-text("投稿热词")')
    await submitButton.click()
    await this.page.waitForTimeout(500)
    
    await this.page.fill('input[placeholder*="热词名称"]', name)
    await this.page.waitForTimeout(200)
    
    await this.page.fill('textarea[placeholder*="释义"]', definition)
    await this.page.waitForTimeout(200)
    
    await this.page.fill('input[placeholder*="例句"]', example)
    await this.page.waitForTimeout(200)
    
    const confirmButton = this.page.locator('button:has-text("提交")')
    await confirmButton.click()
    await this.page.waitForTimeout(1000)
  }

  async voteHotword(index = 0) {
    const voteButtons = this.page.locator('button:has-text("投票")')
    await voteButtons.nth(index).click()
    await this.page.waitForTimeout(1000)
  }

  async switchTab(tabName) {
    await this.page.click(`text=${tabName}`)
    await this.page.waitForTimeout(1000)
  }
}

export class StoryPage {
  constructor(page) {
    this.page = page
  }

  async goto() {
    await this.page.goto('/story')
    await this.page.waitForLoadState('networkidle')
  }

  async createStory(title, category, worldSetting, content) {
    const createButton = this.page.locator('button:has-text("创建故事")')
    await createButton.click()
    await this.page.waitForTimeout(500)
    
    await this.page.fill('input[placeholder*="标题"]', title)
    await this.page.waitForTimeout(200)
    
    await this.page.selectOption('select', { value: String(category) })
    await this.page.waitForTimeout(200)
    
    await this.page.fill('textarea[placeholder*="世界观"]', worldSetting)
    await this.page.waitForTimeout(200)
    
    await this.page.fill('textarea[placeholder*="开篇"]', content)
    await this.page.waitForTimeout(200)
    
    const submitButton = this.page.locator('button:has-text("创建")')
    await submitButton.click()
    await this.page.waitForTimeout(1000)
  }

  async continueStory(content) {
    await this.page.click('button:has-text("续写")')
    await this.page.fill('textarea', content)
    await this.page.click('button:has-text("提交")')
  }

  async likeStory() {
    await this.page.click('button:has-text("点赞")')
    await this.page.waitForTimeout(500)
  }
}

export class AdminPage {
  constructor(page) {
    this.page = page
  }

  async gotoDashboard() {
    await this.page.goto('/admin/dashboard')
    await this.page.waitForLoadState('networkidle')
  }

  async gotoAudit() {
    await this.page.goto('/admin/audit')
    await this.page.waitForLoadState('networkidle')
  }

  async gotoUserManage() {
    await this.page.goto('/admin/users')
    await this.page.waitForLoadState('networkidle')
  }

  async deletePost(reason = '违规内容') {
    const deleteButtons = this.page.locator('button:has-text("删除")')
    if (await deleteButtons.count() > 0) {
      await deleteButtons.first().click()
      await this.page.fill('textarea', reason)
      await this.page.click('button:has-text("确认")')
      await this.page.waitForTimeout(1000)
    }
  }

  async banUser(duration = 1) {
    const banButtons = this.page.locator('button:has-text("封禁")')
    if (await banButtons.count() > 0) {
      await banButtons.first().click()
      await this.page.selectOption('select', { value: String(duration) })
      await this.page.fill('textarea', '测试封禁')
      await this.page.click('button:has-text("确认")')
      await this.page.waitForTimeout(1000)
    }
  }
}

export class ProfilePage {
  constructor(page) {
    this.page = page
  }

  async goto(userId = 'me') {
    // 支持两种路由格式：/profile/me 和 /user/me
    const url = userId === 'me' ? '/user/me' : `/user/${userId}`
    await this.page.goto(url)
    await this.page.waitForLoadState('networkidle')
  }

  async editProfile(nickname, bio) {
    await this.page.click('button:has-text("编辑资料")')
    if (nickname) {
      await this.page.fill('input[placeholder*="昵称"]', nickname)
    }
    if (bio) {
      await this.page.fill('textarea[placeholder*="签名"], textarea[placeholder*="简介"]', bio)
    }
    await this.page.click('button:has-text("保存")')
    await this.page.waitForTimeout(1000)
  }

  async follow() {
    await this.page.click('button:has-text("关注")')
    await this.page.waitForTimeout(1000)
  }

  async unfollow() {
    await this.page.click('button:has-text("已关注")')
    await this.page.waitForTimeout(1000)
  }

  async sendMessage() {
    await this.page.click('button:has-text("私信")')
    await this.page.waitForURL('/message')
  }

  async switchTab(tabName) {
    await this.page.click(`text=${tabName}`)
    await this.page.waitForTimeout(1000)
  }
}

export class MessagePage {
  constructor(page) {
    this.page = page
  }

  async goto() {
    await this.page.goto('/message')
    await this.page.waitForLoadState('networkidle')
  }

  async selectContact(index = 0) {
    const contacts = this.page.locator('.contact-item, .conversation-item')
    await contacts.nth(index).click()
    await this.page.waitForTimeout(1000)
  }

  async sendMessage(content) {
    const messageInput = this.page.locator('textarea[placeholder*="消息"], input[placeholder*="消息"]')
    await messageInput.fill(content)
    await this.page.click('button:has-text("发送")')
    await this.page.waitForTimeout(1000)
  }

  async recallMessage() {
    const myMessages = this.page.locator('.message-item.mine, .message.sent').last()
    await myMessages.hover()
    
    const recallButton = this.page.locator('button:has-text("撤回")').last()
    if (await recallButton.isVisible()) {
      await recallButton.click()
      const confirmButton = this.page.locator('button:has-text("确认")')
      if (await confirmButton.isVisible()) {
        await confirmButton.click()
        await this.page.waitForTimeout(1000)
      }
    }
  }

  async deleteConversation(index = 0) {
    const contacts = this.page.locator('.contact-item, .conversation-item')
    await contacts.nth(index).hover()
    
    const deleteButton = this.page.locator('button:has-text("删除")').first()
    if (await deleteButton.isVisible()) {
      await deleteButton.click()
      const confirmButton = this.page.locator('button:has-text("确认")')
      if (await confirmButton.isVisible()) {
        await confirmButton.click()
        await this.page.waitForTimeout(1000)
      }
    }
  }
}

export class SocialPage {
  constructor(page) {
    this.page = page
  }

  async followUser(userId) {
    await this.page.goto(`/user/${userId}`)
    await this.page.waitForLoadState('networkidle')
    
    const followButton = this.page.locator('button:has-text("关注")')
    if (await followButton.isVisible()) {
      await followButton.click()
      await this.page.waitForTimeout(1000)
    }
  }

  async unfollowUser(userId) {
    await this.page.goto(`/user/${userId}`)
    await this.page.waitForLoadState('networkidle')
    
    const followedButton = this.page.locator('button:has-text("已关注")')
    if (await followedButton.isVisible()) {
      await followedButton.click()
      await this.page.waitForTimeout(1000)
    }
  }

  async blockUser(userId) {
    await this.page.goto(`/user/${userId}`)
    await this.page.waitForLoadState('networkidle')
    
    const moreButton = this.page.locator('button:has-text("更多")').or(this.page.locator('button[aria-label*="更多"]'))
    if (await moreButton.isVisible()) {
      await moreButton.click()
      await this.page.click('text=拉黑')
      
      const confirmButton = this.page.locator('button:has-text("确认")')
      if (await confirmButton.isVisible()) {
        await confirmButton.click()
        await this.page.waitForTimeout(1000)
      }
    }
  }

  async reportUser(userId, reason) {
    await this.page.goto(`/user/${userId}`)
    await this.page.waitForLoadState('networkidle')
    
    const moreButton = this.page.locator('button:has-text("更多")').or(this.page.locator('button[aria-label*="更多"]'))
    if (await moreButton.isVisible()) {
      await moreButton.click()
      await this.page.click('text=举报')
      
      await this.page.fill('textarea', reason)
      await this.page.click('button:has-text("提交")')
      await this.page.waitForTimeout(1000)
    }
  }

  async viewFollowList() {
    await this.page.goto('/user/me')
    await this.page.waitForLoadState('networkidle')
    await this.page.click('text=关注')
    await this.page.waitForTimeout(1000)
  }

  async viewFansList() {
    await this.page.goto('/user/me')
    await this.page.waitForLoadState('networkidle')
    await this.page.click('text=粉丝')
    await this.page.waitForTimeout(1000)
  }

  async viewBlacklist() {
    await this.page.goto('/user/me')
    await this.page.waitForLoadState('networkidle')
    
    const settingsButton = this.page.locator('button:has-text("设置")').or(this.page.locator('text=设置'))
    if (await settingsButton.isVisible()) {
      await settingsButton.click()
      await this.page.click('text=黑名单')
      await this.page.waitForTimeout(1000)
    }
  }
}
