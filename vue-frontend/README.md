# 张家界学院表白墙 - 前端项目

基于 Vue 3 + Vite 构建的现代化表白墙应用前端。

## 技术栈

- **框架**: Vue 3 (Composition API + `<script setup>`)
- **构建工具**: Vite
- **状态管理**: Pinia
- **路由**: Vue Router
- **UI 组件**: Radix Vue + Element Plus
- **样式**: Tailwind CSS
- **图表**: ECharts
- **测试**: Vitest + @fast-check/vitest

## 快速开始

### 安装依赖

```bash
npm install
```

### 开发模式

```bash
npm run dev
```

### 生产构建

```bash
npm run build
```

### 预览生产构建

```bash
npm run preview
```

### 运行测试

```bash
# 运行所有测试
npm run test

# 运行测试（单次运行）
npm run test:run

# 生成覆盖率报告
npm run test:coverage
```

## 项目结构

```
vue-frontend/
├── src/
│   ├── components/     # Vue 组件
│   ├── views/          # 页面视图
│   ├── stores/         # Pinia 状态管理
│   ├── router/         # 路由配置
│   ├── utils/          # 工具函数
│   └── assets/         # 静态资源
├── tests/              # 测试文件
│   ├── unit/           # 单元测试
│   ├── property/       # 属性测试
│   ├── integration/    # 集成测试
│   └── helpers/        # 测试工具
├── docs/               # 项目文档
└── public/             # 公共资源
```

## 核心功能

- 📝 **表白墙**: 发布和浏览表白帖子
- 💬 **实时聊天**: WebSocket 实时消息
- 🎲 **漂流瓶**: 随机匿名消息
- 📖 **故事接龙**: 协作创作故事
- 🔥 **热词墙**: 实时热门话题
- 📊 **数据大屏**: 可视化统计数据
- 🗺️ **地图定位**: 高德地图集成
- 🤖 **AI 助手**: 内容优化建议

## 质量改进

本项目在答辩前实施了全面的质量改进，包括：

- ✅ 文件上传 MIME 类型验证
- ✅ 地图定位重试机制
- ✅ WebSocket 连接状态指示器
- ✅ 空内容验证增强
- ✅ 重复提交防护
- ✅ AI 服务超时处理

详细信息请查看 [质量改进文档](./docs/QUALITY_IMPROVEMENTS.md)。

## 测试覆盖

- **单元测试**: 124 个测试用例
- **属性测试**: 30 个属性测试（100+ 迭代）
- **集成测试**: 15 个端到端测试
- **组件测试**: 54 个组件测试
- **总计**: 223 个测试，100% 通过率

详细测试文档请查看 [测试框架文档](./tests/README.md)。

## 文档

- [质量改进文档](./docs/QUALITY_IMPROVEMENTS.md) - 答辩前质量改进详情
- [测试框架文档](./tests/README.md) - 测试框架使用指南
- [API 文档](./docs/API.md) - 后端 API 接口文档（待补充）

## 浏览器支持

- Chrome >= 90
- Firefox >= 88
- Safari >= 14
- Edge >= 90

## 开发指南

### 代码规范

- 使用 ESLint 进行代码检查
- 遵循 Vue 3 官方风格指南
- 使用 Prettier 格式化代码

### 提交规范

遵循 Conventional Commits 规范：

- `feat`: 新功能
- `fix`: 修复 bug
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 代码重构
- `test`: 测试相关
- `chore`: 构建/工具相关

### 测试要求

- 所有新功能必须包含单元测试
- 关键业务逻辑需要属性测试
- 用户流程需要集成测试
- 目标覆盖率 > 80%

## 常见问题

### 开发环境问题

**Q: 启动失败，提示端口被占用**  
A: 修改 `vite.config.js` 中的端口配置，或关闭占用端口的进程。

**Q: 依赖安装失败**  
A: 尝试删除 `node_modules` 和 `package-lock.json`，然后重新运行 `npm install`。

### 功能问题

**Q: 文件上传失败**  
A: 检查文件格式和大小是否符合要求（图片 ≤ 10MB，视频 ≤ 100MB）。

**Q: 定位服务不工作**  
A: 确保使用 HTTPS 连接，并授予浏览器位置权限。

**Q: WebSocket 连接断开**  
A: 检查网络连接，点击重连按钮或刷新页面。

更多问题请查看 [故障排除文档](./docs/QUALITY_IMPROVEMENTS.md#故障排除)。

## 贡献

欢迎提交 Issue 和 Pull Request！

## 许可证

MIT License

## 联系方式

- 项目地址: [GitHub](https://github.com/your-repo)
- 问题反馈: [Issues](https://github.com/your-repo/issues)

---

**最后更新**: 2024-12-19
