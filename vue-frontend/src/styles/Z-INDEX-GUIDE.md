# Z-Index 层级系统使用指南

## 问题背景

在原有设计中，UI元素存在严重的重叠问题：
- 个人中心下拉菜单与校园公告内容重叠
- 浮层/下拉菜单错位遮挡底层文本/按钮
- 交互组件与提示类内容视觉叠加

## 解决方案

建立统一的Z-Index层级系统，确保所有UI元素按照正确的优先级显示。

## Z-Index 层级定义

### 基础层级 (0-10)
- `z-base` (0): 默认内容层
- `z-content` (1): 普通内容

### 固定元素层级 (999-1030)
- `z-sidebar` (999): 侧边栏
- `z-header` (1000): 顶部导航栏
- `z-sticky` (1020): 粘性定位元素
- `z-fixed` (1030): 固定定位元素

### 浮层层级 (1040-1070)
- `z-dropdown` (1040): 下拉菜单
- `z-announcement` (1045): 公告/提示
- `z-user-menu` (1050): 用户菜单（个人中心）
- `z-popover` (1060): 弹出层
- `z-tooltip` (1070): 工具提示

### 模态框层级 (1080-1090)
- `z-modal-backdrop` (1080): 模态框背景遮罩
- `z-modal` (1090): 模态框内容

### 通知层级 (1100-1110) - 最高优先级
- `z-notification` (1100): 通知消息
- `z-toast` (1110): Toast提示

## 使用规范

### 1. 顶部导航栏
```vue
<header class="fixed top-0 w-full z-header">
  <!-- 导航内容 -->
</header>
```

### 2. 用户下拉菜单
```vue
<div class="dropdown-menu z-user-menu">
  <!-- 用户菜单内容 -->
</div>
```

### 3. 公告/提示
```vue
<div class="announcement z-announcement">
  <!-- 公告内容 -->
</div>
```

### 4. 模态框
```vue
<!-- 背景遮罩 -->
<div class="modal-backdrop z-modal-backdrop"></div>
<!-- 模态框内容 -->
<div class="modal z-modal">
  <!-- 内容 -->
</div>
```

### 5. Toast通知
```vue
<div class="toast z-toast">
  <!-- Toast内容 -->
</div>
```

## 重要原则

1. **严格遵守层级**: 不要随意使用自定义z-index值
2. **使用Tailwind类**: 优先使用 `z-{name}` 类而不是内联样式
3. **避免冲突**: 
   - 用户菜单 (z-user-menu: 1050) > 公告 (z-announcement: 1045)
   - 确保交互元素始终在静态内容之上
4. **测试验证**: 每次添加浮层元素时，测试与其他元素的层级关系

## 常见场景

### 场景1: 个人中心菜单与公告重叠
**错误做法**:
```vue
<div class="user-menu">  <!-- 没有z-index -->
<div class="announcement">  <!-- 没有z-index -->
```

**正确做法**:
```vue
<div class="user-menu z-user-menu">  <!-- z-index: 1050 -->
<div class="announcement z-announcement">  <!-- z-index: 1045 -->
```

### 场景2: 下拉菜单被内容遮挡
**错误做法**:
```vue
<div class="dropdown" style="z-index: 10">  <!-- 太低 -->
```

**正确做法**:
```vue
<div class="dropdown z-dropdown">  <!-- z-index: 1040 -->
```

## 调试技巧

1. 使用浏览器开发工具查看元素的computed z-index值
2. 临时添加背景色来可视化层级关系
3. 使用3D视图查看元素堆叠顺序

## 更新日志

- 2024-01-16: 初始版本，建立完整的Z-index层级系统
