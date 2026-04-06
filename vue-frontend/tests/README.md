# 测试框架文档

本项目使用 **Vitest** 作为测试框架，并集成了 **@fast-check/vitest** 进行属性测试（Property-Based Testing）。

## 目录结构

```
tests/
├── helpers/              # 测试辅助工具
│   ├── propertyGenerators.js  # 属性测试数据生成器
│   └── testHelpers.js         # 通用测试工具函数
├── property/            # 属性测试
├── unit/                # 单元测试
├── integration/         # 集成测试
├── components/          # 组件测试
├── utils/               # 工具函数测试
├── setup.js             # 测试环境设置
└── README.md            # 本文档
```

## 快速开始

### 运行测试

```bash
# 运行所有测试
npm run test

# 运行测试（单次运行，不监听）
npm run test:run

# 运行测试并生成覆盖率报告
npm run test:coverage

# 运行特定文件的测试
npm run test -- validation.test.js

# 运行特定目录的测试
npm run test -- tests/property/
```

### 编写单元测试

```javascript
import { describe, it, expect } from "vitest";
import { myFunction } from "@/utils/myFunction";

describe("myFunction", () => {
  it("should return expected result", () => {
    const result = myFunction("input");
    expect(result).toBe("expected");
  });
});
```

### 编写属性测试

```javascript
import { describe, it } from "vitest";
import fc from "fast-check";
import { myFunction } from "@/utils/myFunction";

describe("Property: myFunction correctness", () => {
  it("should satisfy property for all inputs", () => {
    fc.assert(
      fc.property(
        fc.string(), // 生成随机字符串
        (input) => {
          const result = myFunction(input);
          // 验证属性
          return result.length >= input.length;
        }
      ),
      { numRuns: 100 } // 运行 100 次迭代
    );
  });
});
```

## 属性测试生成器

`tests/helpers/propertyGenerators.js` 提供了丰富的数据生成器：

### 文件生成器

```javascript
import { fileGenerators } from "@/tests/helpers/propertyGenerators";

// 生成有效的图片 MIME 类型
fileGenerators.validImageMimeType();

// 生成图片文件
fileGenerators.imageFile(10); // 最大 10MB

// 生成文件数组
fileGenerators.fileArray({ minLength: 1, maxLength: 5 });
```

### WebSocket 生成器

```javascript
import { websocketGenerators } from "@/tests/helpers/propertyGenerators";

// 生成连接状态
websocketGenerators.connectionState();

// 生成 WebSocket 消息
websocketGenerators.message();
```

### 内容验证生成器

```javascript
import { contentGenerators } from "@/tests/helpers/propertyGenerators";

// 生成空内容
contentGenerators.emptyContent();

// 生成有效内容
contentGenerators.validContent();

// 生成帖子表单数据
contentGenerators.postForm();
```

### 其他生成器

- `locationGenerators` - 地理位置数据
- `aiServiceGenerators` - AI 服务数据
- `buttonStateGenerators` - 按钮状态
- `timeGenerators` - 时间相关数据
- `dashboardGenerators` - 数据大屏数据

## 测试辅助工具

`tests/helpers/testHelpers.js` 提供了多种测试工具：

### Mock 工具

```javascript
import { mockUtils } from "@/tests/helpers/testHelpers";

// 创建 mock 文件
const file = mockUtils.createMockFile({
  name: "test.jpg",
  type: "image/jpeg",
  size: 1024 * 1024,
});

// 创建 mock WebSocket
const ws = mockUtils.createMockWebSocket();
ws.connect(); // 模拟连接
ws.receiveMessage({ type: "NOTICE", content: "test" }); // 模拟接收消息
```

### 断言工具

```javascript
import { assertUtils } from "@/tests/helpers/testHelpers";

// 检查验证结果
assertUtils.isValidationResult(result);
assertUtils.isFailedValidation(result);
assertUtils.hasChineseMessage(result);
```

### 异步工具

```javascript
import { asyncUtils } from "@/tests/helpers/testHelpers";

// 等待指定时间
await asyncUtils.wait(1000);

// 等待条件满足
await asyncUtils.waitFor(() => element.isVisible, { timeout: 5000 });

// 模拟网络延迟
await asyncUtils.simulateNetworkDelay(100, 500);
```

### DOM 工具

```javascript
import { domUtils } from "@/tests/helpers/testHelpers";

// 查找元素
const button = domUtils.findElement(".submit-button");

// 设置输入值
domUtils.setInputValue(input, "test value");

// 点击元素
domUtils.clickElement(button);

// 检查元素状态
domUtils.isVisible(element);
domUtils.isDisabled(button);
```

## 属性测试最佳实践

### 1. 明确属性

每个属性测试应该验证一个清晰的属性：

```javascript
// ✅ 好的属性：明确且可验证
it("属性：文件大小小于等于限制时应该通过验证", () => {
  fc.assert(
    fc.property(fc.integer({ min: 1, max: 10 }), (sizeMB) => {
      const file = { size: sizeMB * 1024 * 1024 };
      const result = validateFileSize(file, 10);
      return result.valid === true;
    })
  );
});

// ❌ 不好的属性：模糊且难以验证
it("属性：验证应该正确工作", () => {
  // 太模糊了
});
```

### 2. 使用前置条件

使用 `fc.pre()` 过滤无效的测试用例：

```javascript
fc.assert(
  fc.property(
    fc.integer({ min: 1, max: 100 }),
    fc.integer({ min: 1, max: 100 }),
    (a, b) => {
      fc.pre(a < b); // 确保 a 小于 b
      return myFunction(a, b) > 0;
    }
  )
);
```

### 3. 足够的迭代次数

对于关键功能，使用更多的迭代次数：

```javascript
fc.assert(
  fc.property(/* ... */),
  { numRuns: 1000 } // 运行 1000 次而不是默认的 100 次
);
```

### 4. 添加标签注释

为每个属性测试添加标签注释，关联到设计文档：

```javascript
/**
 * Feature: pre-defense-quality-fixes
 * Property 1: File MIME Type Validation
 * Validates: Requirements 1.1, 1.2, 1.3
 */
it("属性：文件 MIME 类型验证", () => {
  // 测试代码
});
```

### 5. 测试边界情况

确保生成器覆盖边界情况：

```javascript
fc.assert(
  fc.property(
    fc.integer({ min: 0, max: 100 }), // 包含 0 和 100
    (value) => {
      // 测试代码
    }
  )
);
```

## 单元测试最佳实践

### 1. 描述性的测试名称

```javascript
// ✅ 好的测试名称
it("should reject empty content and show error message", () => {});

// ❌ 不好的测试名称
it("test1", () => {});
```

### 2. AAA 模式

遵循 Arrange-Act-Assert 模式：

```javascript
it("should validate file size correctly", () => {
  // Arrange - 准备测试数据
  const file = { size: 5 * 1024 * 1024 };
  const maxSize = 10;

  // Act - 执行被测试的代码
  const result = validateFileSize(file, maxSize);

  // Assert - 验证结果
  expect(result.valid).toBe(true);
});
```

### 3. 隔离测试

每个测试应该独立，不依赖其他测试：

```javascript
describe("myFunction", () => {
  beforeEach(() => {
    // 每个测试前重置状态
    vi.clearAllMocks();
  });

  it("test 1", () => {
    // 独立的测试
  });

  it("test 2", () => {
    // 不依赖 test 1
  });
});
```

### 4. 测试边界和异常

```javascript
describe("validateFileSize", () => {
  it("should accept file at exact size limit", () => {
    const file = { size: 10 * 1024 * 1024 };
    expect(validateFileSize(file, 10).valid).toBe(true);
  });

  it("should reject file exceeding size limit by 1 byte", () => {
    const file = { size: 10 * 1024 * 1024 + 1 };
    expect(validateFileSize(file, 10).valid).toBe(false);
  });

  it("should handle zero size file", () => {
    const file = { size: 0 };
    expect(validateFileSize(file, 10).valid).toBe(false);
  });
});
```

## 集成测试

集成测试验证多个组件协同工作：

```javascript
import { mount } from "@vue/test-utils";
import PostModal from "@/components/PostModal.vue";

describe("PostModal Integration", () => {
  it("should validate and upload file end-to-end", async () => {
    const wrapper = mount(PostModal);

    // 模拟文件选择
    const file = mockUtils.createMockFile({
      name: "test.jpg",
      type: "image/jpeg",
      size: 1024 * 1024,
    });

    // 触发文件上传
    await wrapper.vm.handleFileChange({ target: { files: [file] } });

    // 验证文件被接受
    expect(wrapper.vm.form.images).toHaveLength(1);
  });
});
```

## 覆盖率目标

- **单元测试**: 目标 80% 以上
- **属性测试**: 覆盖所有关键业务逻辑
- **集成测试**: 覆盖主要用户流程

## 调试测试

### 查看详细输出

```bash
npm run test -- --reporter=verbose
```

### 调试单个测试

```javascript
it.only("should debug this test", () => {
  console.log("Debug info");
  // 测试代码
});
```

### 跳过测试

```javascript
it.skip("should skip this test", () => {
  // 暂时跳过
});
```

## 常见问题

### Q: 属性测试失败时如何调试？

A: fast-check 会提供失败的反例。使用 `verbose: true` 查看详细信息：

```javascript
fc.assert(fc.property(/* ... */), { numRuns: 100, verbose: true });
```

### Q: 如何测试异步代码？

A: 使用 `async/await` 或返回 Promise：

```javascript
it("should handle async operation", async () => {
  const result = await asyncFunction();
  expect(result).toBe("expected");
});
```

### Q: 如何 mock 外部依赖？

A: 使用 Vitest 的 `vi.mock()`：

```javascript
import { vi } from "vitest";

vi.mock("@/api/request", () => ({
  default: {
    post: vi.fn().mockResolvedValue({ code: "200", data: "success" }),
  },
}));
```

## 参考资源

- [Vitest 文档](https://vitest.dev/)
- [fast-check 文档](https://fast-check.dev/)
- [Vue Test Utils 文档](https://test-utils.vuejs.org/)
- [属性测试介绍](https://fast-check.dev/docs/introduction/)

## 贡献指南

编写新测试时：

1. 选择合适的测试类型（单元/属性/集成）
2. 使用提供的生成器和工具函数
3. 添加清晰的注释和标签
4. 确保测试独立且可重复
5. 运行测试确保通过
6. 检查覆盖率报告

---

**最后更新**: 2024-12-19
