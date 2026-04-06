# E2E 测试文档

## 概述

本项目使用 Playwright 进行端到端自动化测试，覆盖所有核心功能模块。

## 测试模块

### 1. 用户认证模块 (01-auth.spec.js)
- 用户注册
- 用户登录
- 登出功能
- 表单验证
- 错误处理

### 2. 帖子功能模块 (02-post.spec.js)
- 发布帖子
- 查看帖子详情
- 点赞/取消点赞
- 评论功能
- 搜索帖子
- 分类筛选
- 匿名发帖
- 地图定位

### 3. 漂流瓶模块 (03-bottle.spec.js)
- 投放漂流瓶
- 打捞漂流瓶
- 查看我的瓶子
- 珍藏馆
- 删除瓶子
- 冷却机制
- 成就系统

### 4. 热词墙模块 (04-hotword.spec.js)
- 投稿热词
- 投票功能
- 查看热词详情
- 我的投稿
- 投票配额检查
- 热度排序

### 5. 故事接龙模块 (05-story.spec.js)
- 创建故事
- 续写故事
- 点赞故事
- 查看故事详情
- 分类筛选
- 防止连续续写

### 6. 社交功能模块 (06-social.spec.js)
- 关注用户
- 取消关注
- 查看关注列表
- 查看粉丝列表
- 拉黑用户
- 黑名单管理
- 举报用户

### 7. 私信功能模块 (07-message.spec.js)
- 发送私信
- 撤回消息
- 消息已读状态
- 未读消息数量
- 搜索会话
- 删除会话
- 首次私信限制
- 发送表情

### 8. 个人主页模块 (08-profile.spec.js)
- 查看个人主页
- 编辑个人资料
- 修改个性签名
- 上传头像
- 查看我的帖子
- 查看我的收藏
- 访客记录
- 统计数据

### 9. 管理员功能模块 (09-admin.spec.js)
- 访问管理后台
- 数据大屏
- 审核帖子
- 删除违规帖子
- 封禁用户
- 解封用户
- 搜索用户
- 用户详情
- 评论管理

### 10. 集成测试 (10-integration.spec.js)
- 新用户完整体验流程
- 用户社交互动流程
- 管理员审核流程
- 漂流瓶完整流程
- 故事接龙完整流程

## 运行测试

### 安装依赖
```bash
npm install
```

### 运行所有测试
```bash
npm run test:e2e
```

### 使用UI模式运行
```bash
npm run test:e2e:ui
```

### 有头模式运行（显示浏览器）
```bash
npm run test:e2e:headed
```

### 调试模式
```bash
npm run test:e2e:debug
```

### 查看测试报告
```bash
npm run test:e2e:report
```

### 运行特定测试文件
```bash
npx playwright test e2e/01-auth.spec.js
```

### 运行特定测试用例
```bash
npx playwright test -g "应该能够注册新用户"
```

## 测试配置

测试配置位于 `playwright.config.js`：

- **baseURL**: http://localhost:5173
- **timeout**: 30秒
- **retries**: CI环境2次，本地1次
- **workers**: CI环境1个，本地2个
- **浏览器**: Chromium (可扩展到Firefox、WebKit)

## Page Object Model

测试使用 Page Object Model 模式，封装在 `e2e/fixtures/page-objects.js`：

- **LoginPage**: 登录页面操作
- **HomePage**: 首页操作
- **BottlePage**: 漂流瓶页面操作
- **HotwordPage**: 热词墙页面操作
- **StoryPage**: 故事接龙页面操作
- **AdminPage**: 管理后台操作

## 测试数据

测试数据生成器位于 `e2e/fixtures/test-data.js`：

- **TEST_USERS**: 预定义测试用户
- **generateTestUser()**: 生成随机测试用户
- **generateRandomText()**: 生成随机文本
- **POST_CATEGORIES**: 帖子分类
- **BOTTLE_DIRECTIONS**: 漂流瓶方向

## 注意事项

### 测试前准备
1. 确保后端服务已启动（默认端口8080）
2. 确保数据库已初始化
3. 确保前端开发服务器已启动（默认端口5173）

### 测试账号
- **普通用户**: 20240001 / password123
- **管理员**: admin / admin123

### 测试数据清理
测试会创建新的测试数据，建议定期清理测试数据库。

### CI/CD集成
测试配置已针对CI环境优化：
- 减少并行数量
- 增加重试次数
- 自动启动开发服务器

## 测试报告

测试完成后会生成以下报告：

- **HTML报告**: `playwright-report/index.html`
- **JSON报告**: `playwright-report/results.json`
- **截图**: 失败时自动截图
- **视频**: 失败时保留视频

## 故障排查

### 测试超时
- 检查网络连接
- 增加timeout配置
- 检查后端服务是否正常

### 元素未找到
- 检查选择器是否正确
- 增加waitForLoadState
- 使用waitForSelector等待元素

### 测试不稳定
- 增加适当的waitForTimeout
- 使用更可靠的选择器
- 检查是否有竞态条件

## 最佳实践

1. **使用Page Object**: 封装页面操作，提高可维护性
2. **等待策略**: 使用networkidle等待网络请求完成
3. **选择器**: 优先使用文本选择器，其次是data-testid
4. **独立性**: 每个测试应该独立运行，不依赖其他测试
5. **清理**: 测试后清理创建的数据
6. **断言**: 使用明确的断言，提供清晰的错误信息

## 扩展测试

### 添加新测试
1. 在 `e2e/` 目录创建新的 `.spec.js` 文件
2. 导入必要的Page Object和测试数据
3. 编写测试用例
4. 运行测试验证

### 添加新Page Object
1. 在 `e2e/fixtures/page-objects.js` 添加新类
2. 封装页面操作方法
3. 在测试文件中使用

## 性能测试

Playwright也支持性能测试：

```javascript
test('页面加载性能', async ({ page }) => {
  const startTime = Date.now()
  await page.goto('/home')
  const loadTime = Date.now() - startTime
  expect(loadTime).toBeLessThan(3000)
})
```

## 可访问性测试

可以集成 axe-core 进行可访问性测试：

```bash
npm install -D @axe-core/playwright
```

## 总结

本测试套件提供了完整的E2E测试覆盖，确保应用的核心功能正常工作。定期运行测试可以及早发现问题，提高代码质量。
