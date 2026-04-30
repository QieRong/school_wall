# 校园表白墙 🌿

> 基于 Spring Boot 3 + Vue 3 的校园社交平台，集内容创作、实时互动、AI 辅助与社区治理于一体。
本项目仅供学习参考，严禁任何商业化（也远远达不到那个要求）。

![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-brightgreen?style=flat-square&logo=spring)
![Vue](https://img.shields.io/badge/Vue-3.x-42b883?style=flat-square&logo=vue.js)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?style=flat-square&logo=mysql)
![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)

---

## 📖 目录

1. [项目简介](#-项目简介)
2. [系统架构](#-系统架构)
3. [技术栈](#-技术栈)
4. [功能模块详解](#-功能模块详解)
5. [数据库设计](#-数据库设计)
6. [后端项目结构](#-后端项目结构)
7. [前端项目结构](#-前端项目结构)
8. [API 接口概览](#-api-接口概览)
9. [安全机制](#-安全机制)
10. [实时通信](#-实时通信)
11. [AI 集成](#-ai-集成)
12. [定时任务](#-定时任务)
13. [快速开始](#-快速开始)
14. [环境变量配置](#-环境变量配置)
15. [管理后台](#-管理后台)
16. [测试](#-测试)

---

## 🎯 项目简介

**校园表白墙** 是一个面向高校校园用户群体设计与开发的综合性社交平台。平台以"帖子发布与互动"为核心，围绕校园生活场景延伸出多元化的社交功能：包括实时私信聊天、漂流瓶随机社交、协作故事接龙、热词文化墙等特色玩法。

平台融合了 **DeepSeek 大语言模型**，支持 AI 内容润色，并对所有 AI 辅助生成的内容提供透明标识，践行 AI 使用伦理。

### 核心亮点

- 🏗️ **A++ 级架构**：476 个测试用例，100% 通过率，高内聚低耦合
- ⚡ **全双工实时通信**：基于 WebSocket 的消息推送与在线状态感知
- 🤖 **AI 内容辅助**：集成 DeepSeek API，支持帖子/故事的 AI 润色，并带透明标识
- 🛡️ **多重安全防护**：JWT鉴权 + Sa-Token 角色权限 + 敏感词过滤 + 审计日志
- 📊 **数据可视化大屏**：ECharts 驱动的实时运营数据看板
- 🗺️ **地理位置集成**：高德地图 API，支持帖子POI定位标记
- 📱 **移动端适配**：完整响应式布局，H5 友好

---

## 🏛️ 系统架构

```
┌─────────────────────────────────────────────────────────┐
│                    前端 (Vue 3 + Vite)                   │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────────┐ │
│  │  用户端   │ │ 管理后台  │ │  Pinia   │ │  WebSocket │ │
│  │  Views   │ │  Admin   │ │  Store   │ │  Client    │ │
│  └──────────┘ └──────────┘ └──────────┘ └────────────┘ │
└─────────────────────────┬───────────────────────────────┘
                          │ HTTP/WebSocket (Vite Proxy)
┌─────────────────────────▼───────────────────────────────┐
│              后端 (Spring Boot 3.3)                      │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────────┐ │
│  │Controller│ │ Service  │ │  Mapper  │ │ WebSocket  │ │
│  │   (18)   │ │  (Impl)  │ │ (MyBatis)│ │  Server    │ │
│  └──────────┘ └──────────┘ └──────────┘ └────────────┘ │
│  ┌──────────────────────────────────────────────────┐   │
│  │  横切关注点：JWT拦截 │ 敏感词过滤 │ 审计日志 │异常处理 │  │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────┬───────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────┐
│                    MySQL 8.0                             │
│              34张业务数据表                               │
└─────────────────────────────────────────────────────────┘
```

---

## 🔧 技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 17 | 编程语言 |
| Spring Boot | 3.3 | 核心框架 |
| MyBatis-Plus | 3.5.x | ORM 框架，支持 Lambda 查询 |
| MySQL | 8.0 | 主数据库 |
| Sa-Token | 最新版 | 轻量级 Java 权限认证框架（JWT 模式） |
| Hutool | 5.x | Java 工具库 |
| Jakarta WebSocket | 随 Spring Boot | 原生 WebSocket 服务端 |
| Spring Scheduler | 随 Spring Boot | 定时任务 |
| DeepSeek API | v1 | 大语言模型接口（AI润色） |

### 前端

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.x | 渐进式前端框架（Composition API） |
| Vite | 5.x | 构建工具与开发服务器 |
| Pinia | 2.x | Vue 官方状态管理库 |
| Vue Router | 4.x | 前端路由 |
| Tailwind CSS | 3.x | 原子化 CSS 框架 |
| Radix Vue | 最新版 | 无障碍 UI 原语组件库 |
| Element Plus | 2.x | Vue3 组件库（部分场景） |
| Lucide Vue | 最新版 | 图标库 |
| ECharts | 5.x | 数据可视化图表 |
| 高德地图 JS API | 2.0 | 地图与 POI 搜索 |

---

## 📦 功能模块详解

### 1. 用户系统

- **注册与登录**：账号密码认证，JWT Token 颁发（有效期 7 天）
- **个人主页**：用户信息展示、动态列表、粉丝/关注关系
- **关注/拉黑**：社交关系管理，支持拉黑后屏蔽内容
- **消息通知**：系统通知（点赞、评论、被@、结果公告）分类展示

### 2. 帖子（表白墙核心）

帖子是平台的核心内容单元，支持以下多种内容形式：

| 能力 | 说明 |
|------|------|
| 富文本 + 多图 | 最多9张图片，支持视频附件 |
| 匿名发帖 | 对其他用户隐藏真实昵称与头像 |
| 帖子分类 | 8大固定分类（表白/树洞/失物/活动等）+ 动态自定义分类 |
| 可见范围 | 全员可见 / 仅粉丝 / 仅自己 |
| 定时发布 | 预设发布时间，由后端定时任务触发 |
| 投票功能 | 创建多选项投票，自动统计票数 |
| 悬赏功能 | 设置帖子悬赏金额（积分体系扩展预留） |
| 地理位置 | 集成高德地图，发帖时可标记 POI 位置 |
| 标签系统 | 自定义话题标签 |
| AI 辅助标识 | 标记帖子是否使用过 AI 润色 |
| 互动 | 点赞、评论（多级）、收藏、分享 |

**帖子生命周期**：`待审核 (0)` → `已发布 (1)` / `被拒绝 (2)` / `已下架 (3)`

**内容安全**：发帖时实时触发敏感词过滤，命中后提示用户修改，强制提交时记录原始内容留审。

### 3. 实时私信（IM）

- 基于 WebSocket 的全双工即时通讯
- 会话列表 + 单聊窗口，支持消息已读/未读状态
- 图片消息（上传到服务器后发送 URL）
- 未读私信数量实时徽章更新

### 4. 漂流瓶

漂流瓶是平台的特色随机社交玩法：

- **投放**：用户写下漂流瓶内容（支持匿名）投入"大海"
- **捞取**：随机捡到一个不属于自己的漂流瓶
- **回复**：对捡到的漂流瓶进行私密回复，形成一对一随机社交
- **收藏**：将心仪的漂流瓶收藏到个人宝箱
- **成就系统**：捞瓶次数、回复数量触发成就解锁
- **自动清理**：过期漂流瓶由定时任务自动归档

### 5. 热词墙

展示当前校园最热门的文化词汇：

- 用户自由投稿热词（每日有配额限制）
- 其他用户对热词点赞投票，按热度排序
- 历史热词博物馆：归档每日/每周热词榜单
- 管理员可以进行热词审核与管理

### 6. 协作故事接龙

最具创意的特色功能，支持多人协作创作故事：

- **创建故事**：设置标题、背景、分类，写第一段正文
- **续写规则**：
  - 每人每故事每 **24 小时** 只能续写一次（冷却机制）
  - 不允许连续自我续写（需至少一名他人在后续写才能再次参与）
  - 续写前需获取"续写锁"，防止多人同时提交冲突
- **完结投票**：参与者可发起完结投票，超半数同意则故事归档
- **贡献度排行**：每个故事内部的段落贡献积分排行榜
- **故事档案馆**：已完结故事专区，供用户阅读收藏
- **收藏功能**：用户可收藏进行中的故事
- **阅读进度**：记录每位用户在每个故事的阅读进度
- **AI 辅助标识**：续写时使用 AI 润色的段落会标注"⚡ 内容经 AI 协助优化"
- **WebSocket 推送**：新段落、故事完结等事件实时推送给在线读者

### 7. 全局搜索

- 支持按关键词搜索帖子内容
- 历史搜索记录（本地缓存）
- 搜索热词联想（基于 `hot_search` 表）

### 8. AI 内容辅助

- 集成 DeepSeek `deepseek-chat` 模型
- 用户发帖/写故事段落时可一键触发"AI 润色"
- 每用户每日调用次数限制（默认 10 次，可配置）
- **AI 使用透明机制**：凡使用过 AI 润色后发布的内容，前端永久显示 `⚡ 内容经 AI 协助优化，请注意甄别` 标识

---

## 🗄️ 数据库设计

数据库名：`biaobaiqiang`，共 **34 张数据表**。

### 核心用户与内容表

| 表名 | 说明 |
|------|------|
| `user` | 用户基本信息（账号、昵称、头像、角色、状态） |
| `post` | 帖子（含多媒体、位置、分类、状态、AI标识等） |
| `comment` | 评论（支持多级，含父评论ID） |
| `category` | 帖子分类（8大固定分类 + 动态分类） |
| `collection` | 帖子收藏记录 |
| `follow` | 用户关注关系 |
| `blacklist` | 用户拉黑记录 |
| `report` | 举报记录 |
| `sys_notice` | 系统通知（点赞/评论/关注/系统/封禁） |
| `chat` | 私信消息 |

### 特色功能表

| 表名 | 说明 |
|------|------|
| `drift_bottle` | 漂流瓶主表 |
| `bottle_reply` | 漂流瓶回复 |
| `bottle_fish_record` | 捞瓶记录 |
| `bottle_collection` | 漂流瓶收藏 |
| `bottle_achievement` | 漂流瓶成就 |
| `hotword` | 热词主表 |
| `hotword_vote` | 热词投票记录 |
| `hotword_daily_quota` | 用户每日热词投稿配额 |
| `hot_search` | 搜索热词记录 |
| `story` | 故事接龙主表 |
| `story_paragraph` | 故事段落（含AI辅助标识） |
| `story_contribution` | 用户故事贡献度 |
| `story_favorite` | 故事收藏 |
| `story_finish_vote` | 故事完结投票主表 |
| `story_finish_vote_record` | 完结投票记录 |
| `story_read_progress` | 用户阅读进度 |

### 系统与管理表

| 表名 | 说明 |
|------|------|
| `announcement` | 公告 |
| `banner` | 首页轮播图 |
| `file_record` | 文件上传记录 |
| `admin_audit_log` | 管理员操作审计日志 |
| `ai_usage` | AI 调用统计（用于限流） |
| `visitor` | 游客访问统计 |

### 关键字段说明（post 表）

```sql
CREATE TABLE `post` (
  `id`            BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id`       BIGINT NOT NULL,
  `content`       TEXT NOT NULL,
  `images`        VARCHAR(2000),          -- 逗号分隔的图片URL列表
  `video`         VARCHAR(500),
  `location`      VARCHAR(255),           -- JSON: {"name":"...","lat":...,"lng":...}
  `category`      INT DEFAULT 1,          -- 分类ID
  `is_anonymous`  TINYINT(1) DEFAULT 0,
  `visibility`    TINYINT DEFAULT 1,      -- 1=全员 2=粉丝 3=仅自己
  `status`        TINYINT DEFAULT 0,      -- 0=待审核 1=已发布 2=被拒绝 3=下架
  `poll_options`  JSON,                   -- 投票选项
  `tags`          VARCHAR(500),           -- 逗号分隔的标签
  `scheduled_time` DATETIME,             -- 定时发布时间
  `is_ai_assisted` TINYINT(1) DEFAULT 0, -- AI润色标识
  `like_count`    INT DEFAULT 0,
  `comment_count` INT DEFAULT 0,
  `view_count`    INT DEFAULT 0,
  `create_time`   DATETIME DEFAULT NOW()
);
```

---

## 📁 后端项目结构

```
springboot/src/main/java/com/example/
├── SpringbootApplication.java          # 启动类
├── controller/                         # 控制器层（18个）
│   ├── PostController.java             # 帖子：发布/查询/点赞/收藏
│   ├── CommentController.java          # 评论
│   ├── UserController.java             # 用户信息
│   ├── SocialController.java           # 关注/拉黑/粉丝
│   ├── ChatController.java             # 私信
│   ├── BottleController.java           # 漂流瓶
│   ├── HotwordController.java          # 热词投稿/投票
│   ├── StoryController.java            # 故事接龙（用户端）
│   ├── AiController.java               # AI 润色接口
│   ├── FileController.java             # 文件上传
│   ├── NoticeController.java           # 通知中心
│   ├── IndexController.java            # 首页公告/轮播图
│   ├── StatsController.java            # 统计数据
│   ├── DashboardController.java        # 数据看板
│   ├── AdminController.java            # 管理员后台
│   ├── StoryAdminController.java       # 故事管理（管理员）
│   ├── HotwordAdminController.java     # 热词管理（管理员）
│   └── ReportController.java           # 举报
├── service/                            # 业务逻辑层
│   └── impl/                           # 实现类
├── mapper/                             # 数据访问层（MyBatis-Plus）
│   └── *.xml                           # Mapper XML 映射文件
├── entity/                             # 实体类（34个，对应数据表）
├── dto/                                # 数据传输对象
│   ├── PostCreateDTO.java              # 帖子创建请求
│   ├── ParagraphCreateDTO.java         # 故事段落创建请求
│   ├── StoryCreateDTO.java             # 故事创建请求
│   ├── ParagraphVO.java                # 段落视图对象
│   └── StoryDetailVO.java             # 故事详情视图对象
├── config/                             # 配置类
│   ├── TokenInterceptor.java           # JWT Token 拦截器
│   ├── AdminInterceptor.java           # 管理员权限拦截器
│   ├── GlobalExceptionHandler.java     # 全局异常处理
│   ├── SensitiveWordLoader.java        # 敏感词词库加载
│   ├── WebSocketConfig.java            # WebSocket 配置
│   ├── CorsConfig.java                 # 跨域配置
│   └── WebMvcConfig.java               # MVC 配置
├── server/
│   └── WebSocketServer.java            # WebSocket 服务端（Session 管理）
├── task/                               # 定时任务
│   ├── PostScheduleTask.java           # 定时帖子发布
│   ├── BottleCleanupTask.java          # 漂流瓶过期清理
│   ├── HotwordCleanupTask.java         # 热词每日归档
│   ├── FileCleanupTask.java            # 孤立文件清理
│   └── WebSocketCleanupTask.java       # WebSocket 过期连接清理
├── common/                             # 公共类
│   └── Result.java                     # 统一响应封装
└── utils/                              # 工具类
```

---

## 📁 前端项目结构

```
vue-frontend/src/
├── main.js                             # 应用入口
├── App.vue                             # 根组件
├── router/index.js                     # 路由配置（含权限守卫）
├── views/                              # 页面视图
│   ├── Home.vue                        # 首页信息流（最大，~85KB）
│   ├── PostDetail.vue                  # 帖子详情
│   ├── Login.vue                       # 登录/注册
│   ├── Profile.vue                     # 用户主页
│   ├── Message.vue                     # 私信中心
│   ├── Bottle.vue                      # 漂流瓶
│   ├── HotwordWall.vue                 # 热词墙
│   ├── StoryHall.vue                   # 故事大厅
│   ├── StoryRead.vue                   # 故事阅读
│   ├── MyStory.vue                     # 我的故事
│   ├── StoryArchive.vue                # 故事档案馆
│   ├── NoticeList.vue                  # 通知中心
│   ├── Search.vue                      # 全局搜索
│   ├── MyPosts.vue                     # 我的帖子
│   └── admin/                          # 管理后台页面
│       ├── Dashboard.vue               # 数据总览
│       ├── DataDashboard.vue           # 数据大屏（ECharts）
│       ├── PostAudit.vue               # 内容审核
│       ├── UserManage.vue              # 用户管理
│       ├── ReportManage.vue            # 举报处理
│       ├── AnnouncementManage.vue      # 公告管理
│       ├── CategoryManage.vue          # 分类管理
│       ├── BannerManage.vue            # 轮播图管理
│       ├── SensitiveManage.vue         # 敏感词库管理
│       ├── BottleManage.vue            # 漂流瓶管理
│       ├── HotwordManage.vue           # 热词墙管理
│       └── StoryManage.vue             # 故事接龙管理
├── components/                         # 复用组件
│   ├── PostModal.vue                   # 发帖弹窗（核心发帖入口）
│   ├── StoryContinueModal.vue          # 故事续写弹窗
│   ├── ParagraphCard.vue               # 段落卡片
│   ├── StoryAchievementBadge.vue       # 故事成就徽章
│   ├── NotificationBell.vue            # 通知铃铛
│   └── admin/                          # 后台通用组件
├── stores/                             # Pinia 状态管理
│   ├── user.js                         # 用户状态（登录信息）
│   ├── app.js                          # 应用全局状态（Toast等）
│   ├── ws.js                           # WebSocket 连接管理
│   └── admin.js                        # 管理后台状态
├── api/                                # 接口封装层
│   ├── request.js                      # Axios 封装（统一拦截器）
│   ├── story.js                        # 故事接龙相关接口
│   ├── bottle.js                       # 漂流瓶相关接口
│   └── ...
├── composables/                        # 组合式函数（Hooks）
│   ├── useLikeWithDebounce.js          # 防抖点赞
│   └── ...
├── layout/
│   ├── AdminLayout.vue                 # 管理后台布局（含侧边栏）
│   └── MainLayout.vue                  # 用户端主布局
└── utils/
    ├── validation.js                   # 统一表单校验工具
    └── logger.js                       # 统一日志工具
```

---

## 🌐 API 接口概览

所有接口统一返回格式：

```json
{
  "code": "200",
  "msg": "操作成功",
  "data": {}
}
```

### 用户 & 社交

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/user/login` | 登录 |
| POST | `/user/register` | 注册 |
| GET | `/user/{id}` | 获取用户信息 |
| POST | `/social/follow` | 关注/取消关注 |
| GET | `/social/followers/{id}` | 粉丝列表 |
| GET | `/social/following/{id}` | 关注列表 |
| POST | `/social/blacklist/add` | 拉黑用户 |

### 帖子

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/post/add` | 发布帖子 |
| GET | `/post/list` | 帖子列表（分页） |
| GET | `/post/{id}` | 帖子详情 |
| POST | `/post/like` | 点赞/取消 |
| POST | `/post/collect` | 收藏/取消 |
| DELETE | `/post/{id}` | 删除帖子 |
| GET | `/post/my` | 我的帖子 |

### 评论

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/comment/add` | 发布评论 |
| GET | `/comment/list/{postId}` | 评论列表 |
| DELETE | `/comment/{id}` | 删除评论 |
| POST | `/comment/like/{id}` | 评论点赞 |

### 故事接龙

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/story/create` | 创建故事 |
| GET | `/story/list` | 故事列表 |
| GET | `/story/{id}` | 故事详情 |
| POST | `/story/{id}/continue` | 续写故事 |
| GET | `/story/{id}/can-continue` | 检查续写资格 |
| POST | `/story/{id}/lock` | 获取续写锁 |
| POST | `/story/{id}/unlock` | 释放续写锁 |
| POST | `/story/{id}/heartbeat` | 续写锁心跳 |
| POST | `/story/paragraph/{id}/like` | 段落点赞 |
| POST | `/story/{id}/favorite` | 收藏故事 |
| POST | `/story/{id}/finish-vote` | 发起完结投票 |
| POST | `/story/{id}/vote-finish` | 参与完结投票 |

### AI 辅助

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/ai/polish` | AI 内容润色 |

### 漂流瓶

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/bottle/throw` | 投放漂流瓶 |
| POST | `/bottle/fish` | 捞取漂流瓶 |
| POST | `/bottle/reply` | 回复漂流瓶 |
| POST | `/bottle/collect` | 收藏漂流瓶 |

### 管理员接口

所有 `/admin/**` 路径均需要 `role=1` 管理员角色，后端通过 `@SaCheckRole("admin")` + `AdminInterceptor` 双重验证。

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin/post/list` | 审核帖子列表 |
| POST | `/admin/post/approve/{id}` | 审核通过 |
| POST | `/admin/post/reject/{id}` | 审核拒绝 |
| GET | `/admin/users` | 用户列表 |
| POST | `/admin/user/ban` | 封禁用户 |
| POST | `/admin/user/unban` | 解封用户 |
| GET | `/admin/reports` | 举报列表 |
| POST | `/admin/report/handle` | 处理举报 |
| GET | `/admin/dashboard` | 数据看板统计 |

---

## 🔒 安全机制

### 1. 身份认证（JWT）

- 登录成功后颁发 JWT Token，有效期默认 7 天
- 前端所有请求通过 Axios 拦截器自动在 Header 中携带 `token`
- 后端 `TokenInterceptor` 拦截所有非白名单请求，验证 Token 合法性

### 2. 权限控制与防越权层

- 普通用户：只能访问自己的数据，不能访问 `/admin/**`
- 管理员（`role=1`）：可访问全部管控接口
- 路由守卫：前端 `router.beforeEach` 判断本地 Token 和用户角色，双重保障
- **强身份签发控制**：基于自定义 `AuthUtil` 控制器彻底杜绝前端伪造 ID，拦截底层 API 如删帖等核心操作的 **垂直越权行为**。

### 3. 敏感词过滤防逆向

- 系统启动时加载敏感词典（`SensitiveWordLoader`）
- 发帖、评论、漂流瓶等写入操作全部实时过滤
- 命中后返回企业标准的防御语境（"系统检测到违规词汇，系统已拦截"），**斩断恶意探路者逆向爬取词库的企图**。

### 4. 操作审计

- 敏感管理操作（封禁、删帖、审核）通过 `@AuditLog` 注解自动记录操作人、操作类型、时间与 IP

### 5. SQL 注入防护

- 全局使用 MyBatis-Plus `#{}` 参数绑定，杜绝 SQL 注入
- 动态查询通过 MyBatis-Plus Lambda 条件构造器编写，无字符串拼接

### 6. 并发防刷限流系统 (Rate Limiting)

- 针对点赞、快速发帖与故事续写等关键 API 的多点操作，系统在内存中布展了由 `ConcurrentHashMap` 构建的单机限流大坝
- 完全阻挡并发点击、无脑死循环脚本及高频无意义刷频带来的服务器压力。

---

## 📡 实时通信

基于 **Java 原生 WebSocket（JSR-356）** 实现：

```
前端 WebSocket Client ──连接→  ws://{host}/ws/{token}  ──→  WebSocketServer
```

### 支持的消息类型

| 消息类型 | 触发场景 |
|----------|----------|
| `NOTICE` | 系统通知（点赞/评论/关注/封禁/审核结果） |
| `CHAT` | 私信消息实时推送 |
| `ANNOUNCEMENT` | 管理员发布全局公告 |
| `STORY_NEW_PARAGRAPH` | 故事有新段落提交 |
| `STORY_LIKE_UPDATE` | 故事段落点赞数更新 |
| `STORY_FINISHED` | 故事完结 |

### 连接管理

- `WebSocketServer` 使用 `ConcurrentHashMap<Long, Session>` 管理在线用户
- 支持 Ping/Pong 心跳保活（前端每 30 秒发送 `ping`，后端回 `pong`）
- 断线重连机制（前端最多重连 10 次，指数退避延迟）
- 定时任务清理过期连接（`WebSocketCleanupTask`）

---

## 🤖 AI 集成

### 接入模型

- 提供商：深度求索（DeepSeek）
- 模型：`deepseek-chat`
- 接口：`POST https://api.deepseek.com/v1/chat/completions`

### 限流策略

- 每用户每日最多调用 **10 次**（通过 `ai_usage` 表记录）
- 配置项 `deepseek.daily-limit` 可调整
- 超限后返回友好提示，不影响正常发帖

### AI 使用透明化

1. 前端检测用户是否点击过"AI 润色"按钮，标记 `isAiAssisted = true`
2. 发帖/续写时将 `isAiAssisted` 字段传递给后端
3. 后端将其持久化到 `post.is_ai_assisted` 或 `story_paragraph.is_ai_assisted`
4. 内容展示时，前端检查该字段，若为 `1` 则在内容底部渲染透明标识徽章

---

## ⏱️ 定时任务

| 任务类 | 执行频率 | 功能 |
|--------|----------|------|
| `PostScheduleTask` | 每分钟检查 | 检查并发布到期定时帖子 |
| `BottleCleanupTask` | 每日 2:00 | 清理超期未被捡到的漂流瓶 |
| `HotwordCleanupTask` | 每日 0:05 | 热词日榜归档，重置投稿配额 |
| `FileCleanupTask` | 每日 3:00 | 清理未被引用的孤立上传文件 |
| `WebSocketCleanupTask` | 每 5 分钟 | 清理失效的 WebSocket 连接 |

---

## 🚀 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- Node.js 18+
- MySQL 8.0+

### 1. 数据库初始化

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE biaobaiqiang CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 导入初始数据
mysql -u root -p biaobaiqiang < biaobaiqiang.sql
```

### 2. 启动后端

```bash
cd springboot

# 修改数据库连接（或使用环境变量，见下方）
# application.yml 中默认配置：localhost:3306，用户名 root，密码 123456

mvn spring-boot:run
# 后端默认运行在 http://localhost:19090
```

### 3. 启动前端

```bash
cd vue-frontend

npm install
npm run dev
# 前端默认运行在 http://localhost:3000
```

### 4. 访问

| 地址 | 说明 |
|------|------|
| `http://localhost:3000` | 用户端首页 |
| `http://localhost:3000/admin` | 管理后台（需管理员账号） |

### 默认管理员账号

数据库初始化后，默认管理员账号为：
- 账号：`admin`
- 密码：`123456`（请登录后立即修改）

---

## ⚙️ 环境变量配置

项目所有敏感配置均支持通过环境变量覆盖 `application.yml` 中的默认值：

| 环境变量 | 默认值 | 说明 |
|----------|--------|------|
| `SERVER_PORT` | `19090` | 后端服务端口 |
| `DB_HOST` | `localhost` | 数据库地址 |
| `DB_PORT` | `3306` | 数据库端口 |
| `DB_NAME` | `biaobaiqiang` | 数据库名 |
| `DB_USERNAME` | `root` | 数据库用户名 |
| `DB_PASSWORD` | `123456` | 数据库密码 |
| `FILE_UPLOAD_PATH` | `./files/` | 文件上传本地路径 |
| `FILE_BASE_URL` | `http://localhost:19090` | 文件访问基础 URL |
| `JWT_SECRET` | （内置长密钥） | JWT 签名密钥，**生产必须修改** |
| `JWT_EXPIRATION` | `604800000` | JWT 过期时间（毫秒，默认7天） |
| `DEEPSEEK_API_KEY` | （内置测试Key） | DeepSeek API 密钥，**生产必须修改** |
| `DEEPSEEK_DAILY_LIMIT` | `10` | 每用户每日AI调用上限 |

---

## 🖥️ 管理后台

管理后台路由前缀 `/admin`，需 `role=1` 管理员身份登录方可访问。

### 功能模块

| 页面 | 路径 | 功能 |
|------|------|------|
| 数据总览 | `/admin/dashboard` | 核心运营指标卡片 |
| 数据大屏 | `/admin/data-dashboard` | ECharts 可视化图表（用户增长、帖子分布、活跃时段等） |
| 内容审核 | `/admin/audit` | 待审核帖子列表，支持一键通过/拒绝 |
| 用户管理 | `/admin/users` | 用户列表、封禁/解封、查看用户详情 |
| 举报处理 | `/admin/report` | 用户举报列表，处理结果推送通知 |
| 敏感词库 | `/admin/sensitive` | 增删改查敏感词 |
| 公告管理 | `/admin/announcement` | 发布/置顶/删除校园公告 |
| 分类管理 | `/admin/category` | 帖子分类的增删改 |
| 轮播图管理 | `/admin/banner` | 首页轮播图上传与排序 |
| 漂流瓶管理 | `/admin/bottle` | 漂流瓶列表与删除 |
| 热词墙管理 | `/admin/hotword` | 热词审核与管理 |
| 故事接龙 | `/admin/story` | 故事列表、删除、设置官方推荐 |

---

## 🧪 测试

项目拥有完整的测试套件：

```bash
cd springboot
mvn test
```

**测试覆盖情况**：
- 测试用例总数：**476 个**
- 通过率：**100%**
- 覆盖范围：Service 层业务逻辑、Mapper 层数据操作、工具类、Controller 集成

---

## 📄 License

本项目仅供学习与毕业设计参考，请勿用于商业用途。

---

## 🙏 致谢

- [Spring Boot](https://spring.io/projects/spring-boot) - 后端框架
- [Vue.js](https://vuejs.org/) - 前端框架
- [MyBatis-Plus](https://baomidou.com/) - ORM 框架
- [Sa-Token](https://sa-token.cc/) - 权限认证框架
- [DeepSeek](https://www.deepseek.com/) - AI 大语言模型
- [高德开放平台](https://lbs.amap.com/) - 地图服务
- [Tailwind CSS](https://tailwindcss.com/) - CSS 框架
- [Radix Vue](https://www.radix-vue.com/) - 无障碍组件库
