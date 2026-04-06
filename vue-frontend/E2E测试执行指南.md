# E2E测试执行指南

## 测试环境要求

- Node.js 16+
- MySQL 8.0
- 后端服务运行在 `http://localhost:19090`
- 前端服务运行在 `http://localhost:3000`

## 测试前准备

### 1. 数据库准备

确保数据库中有充足的测试数据：

```bash
# 导入完整数据库（如果是全新环境）
mysql -u root -p < biaobaiqiang.sql

# 或者只准备测试用户和重置状态
mysql -u root -p biaobaiqiang < vue-frontend/e2e/setup-test-data.sql
```

### 2. 启动后端服务

```bash
cd springboot
mvn spring-boot:run
```

等待后端服务启动完成，确认可以访问 `http://localhost:19090`

### 3. 安装前端依赖

```bash
cd vue-frontend
npm install
```

## 运行测试

### 方式一：自动启动前端（推荐）

Playwright会自动启动前端开发服务器：

```bash
# 运行所有测试
npm run test:e2e

# 使用UI模式运行（可视化调试）
npm run test:e2e:ui

# 显示浏览器窗口运行
npm run test:e2e:headed

# 调试模式（逐步执行）
npm run test:e2e:debug
```

### 方式二：手动启动前端

如果需要手动控制前端服务：

```bash
# 终端1：启动前端
npm run dev

# 终端2：运行测试（需要修改playwright.config.js注释掉webServer配置）
npx playwright test
```

## 查看测试报告

测试完成后，查看HTML报告：

```bash
npm run test:e2e:report
```

报告文件位置：`vue-frontend/playwright-report/index.html`

## 测试文件说明

测试文件按功能模块组织，编号表示执行顺序：

- `01-auth.spec.js` - 登录注册认证（10个测试）
- `02-post.spec.js` - 发帖功能（10个测试）
- `03-bottle.spec.js` - 漂流瓶（10个测试）
- `04-hotword.spec.js` - 热词墙（10个测试）
- `05-story.spec.js` - 故事接龙（10个测试）
- `06-social.spec.js` - 社交互动（10个测试）
- `07-message.spec.js` - 私信功能（10个测试）
- `08-profile.spec.js` - 个人中心（10个测试）
- `09-admin.spec.js` - 管理后台（10个测试）
- `10-integration.spec.js` - 集成测试（5个测试）

总计：95个测试用例

## 测试配置

测试配置文件：`vue-frontend/playwright.config.js`

关键配置：
- 单线程执行（`workers: 1`）避免数据竞争
- 顺序执行（`fullyParallel: false`）确保测试顺序
- 失败时截图和录像
- 超时时间：30秒

## 测试数据清理

测试完成后，清理测试数据：

```bash
mysql -u root -p biaobaiqiang < vue-frontend/e2e/cleanup-test-data.sql
```

## 常见问题

### 1. 测试超时

- 确认后端服务已启动
- 检查网络连接
- 增加超时时间（修改`playwright.config.js`中的`timeout`）

### 2. 选择器找不到元素

- 检查前端页面是否正确渲染
- 使用`--headed`模式查看浏览器实际状态
- 使用`--debug`模式逐步调试

### 3. 登录失败

- 确认测试用户已创建（执行`setup-test-data.sql`）
- 检查密码是否正确（默认：123456）
- 确认后端认证服务正常

### 4. 数据冲突

- 执行`cleanup-test-data.sql`清理旧数据
- 重新执行`setup-test-data.sql`准备数据

## 测试账号

- 普通用户1：`2212040241` / `123456`
- 普通用户2：`2212040201` / `123456`
- 管理员：`admin` / `123456`

## 注意事项

1. 测试会修改数据库数据，建议使用独立的测试数据库
2. 测试按顺序执行，不要随意调整文件编号
3. 某些测试依赖前置测试的数据，不建议单独运行
4. 测试过程中不要手动操作浏览器窗口
5. 确保测试环境网络稳定，避免超时失败
