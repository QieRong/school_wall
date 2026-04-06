package com.example.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.entity.AiUsage;
import com.example.mapper.AiMapper;
import com.example.service.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AiServiceImpl implements AiService {

  private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);

  @Resource
  private AiMapper aiMapper;

  @Value("${deepseek.api-key}")
  private String apiKey;

  @Value("${deepseek.api-url}")
  private String apiUrl;

  @Value("${deepseek.model}")
  private String model;

  @Value("${deepseek.daily-limit}")
  private int dailyLimit;

  @jakarta.annotation.PostConstruct
  public void init() {
    if (apiKey == null || apiKey.trim().isEmpty()) {
      log.warn("DeepSeek API Key 未配置！AI功能将不可用。如需使用AI功能，请设置环境变量 DEEPSEEK_API_KEY");
      log.warn("应用将继续启动，但AI相关接口将返回错误提示");
    } else {
      log.info("DeepSeek AI 服务已初始化，每日限额: {}", dailyLimit);
    }
  }

  @Override
  public List<String> generateContent(Long userId, Map<String, String> params) {
    // 检查使用次数
    checkUsageLimit(userId);

    String target = params.getOrDefault("target", "TA");
    String style = params.getOrDefault("style", "温柔浪漫");
    String keywords = params.getOrDefault("keywords", "");
    String scene = params.getOrDefault("scene", "校园");

    String prompt = buildGeneratePrompt(target, style, keywords, scene);
    String result = callDeepSeek(prompt);

    // 解析多个结果
    List<String> contents = parseMultipleResults(result);

    // 记录使用
    saveUsage(userId, "generate", prompt, result);

    return contents;
  }

  @Override
  public String polishContent(Long userId, String content, String style) {
    checkUsageLimit(userId);

    String prompt = buildPolishPrompt(content, style);
    String result = callDeepSeek(prompt);

    // 记录使用
    saveUsage(userId, "polish", prompt, result);

    return result;
  }

  @Override
  public int getRemainingCount(Long userId) {
    int used = aiMapper.countTodayUsage(userId, LocalDate.now());
    return Math.max(0, dailyLimit - used);
  }

  private void checkUsageLimit(Long userId) {
    int remaining = getRemainingCount(userId);
    if (remaining <= 0) {
      throw new RuntimeException("今日AI使用次数已用完，明天再来吧~");
    }
  }

  /**
   * 构建文案生成提示词 - 人类作者模拟风格
   */
  private String buildGeneratePrompt(String target, String style, String keywords, String scene) {
    StringBuilder sb = new StringBuilder();
    // 系统角色设定
    sb.append("# 角色：校园表白文案创作者\n\n");
    sb.append("## 简介\n");
    sb.append("- 语言：中文\n");
    sb.append("- 描述：专业将情感转化为真挚动人文字的表白文案专家\n");
    sb.append("- 背景：具有丰富校园生活经验的资深文案创作者，擅长不同风格的情感表达\n");
    sb.append("- 个性：富有创造力、细腻观察力、略带个人主观色彩\n");
    sb.append("- 专长：情感文案、表白创作、风格转换\n\n");

    sb.append("## 技能\n");
    sb.append("- 风格转换：能将普通表达转换为自然流畅的人类写作风格\n");
    sb.append("- 情感注入：为内容添加适当的情感色彩和主观表达\n");
    sb.append("- 个性化处理：加入合理的个人观点和独特视角\n");
    sb.append("- 修辞优化：运用多样的修辞手法提升表达效果\n\n");

    sb.append("## 规则\n");
    sb.append("- 必须完全原创而非套用模板\n");
    sb.append("- 风格转换要自然不做作\n");
    sb.append("- 适当保留人类写作的不完美特征\n");
    sb.append("- 不同风格采用相应的表达方式\n\n");

    sb.append("## 任务\n");
    sb.append("请帮我生成3条不同的表白文案。\n\n");
    sb.append("创作要求：\n");
    sb.append("1. 表白对象：").append(target).append("\n");
    sb.append("2. 风格：").append(style).append("\n");
    sb.append("3. 场景：").append(scene).append("\n");
    if (keywords != null && !keywords.isEmpty()) {
      sb.append("4. 包含关键词：").append(keywords).append("\n");
    }
    sb.append("\n## 输出格式\n");
    sb.append("- 格式：纯文本，每条用【1】【2】【3】标记开头\n");
    sb.append("- 每条文案100-200字\n");
    sb.append("- 要真挚感人，像真人撰写的高质量内容\n");
    sb.append("- 适合大学生群体，符合校园氛围\n");
    sb.append("- 直接输出文案，不要有其他解释\n");
    return sb.toString();
  }

  /**
   * 构建内容润色提示词 - 人类作者模拟风格
   */
  private String buildPolishPrompt(String content, String style) {
    StringBuilder sb = new StringBuilder();
    // 系统角色设定
    sb.append("# 角色：人类作者模拟\n\n");
    sb.append("## 简介\n");
    sb.append("- 语言：中文\n");
    sb.append("- 描述：专业将AI生成内容转化为人类作者写作风格的内容改写专家\n");
    sb.append("- 背景：具有丰富写作经验的资深编辑，擅长不同文体风格的转换\n");
    sb.append("- 个性：富有创造力、细腻观察力、略带个人主观色彩\n");
    sb.append("- 专长：文学创作、内容改写、风格转换\n\n");

    sb.append("## 技能\n");
    sb.append("- 风格转换：能将机械式表达转换为自然流畅的人类写作风格\n");
    sb.append("- 情感注入：为内容添加适当的情感色彩和主观表达\n");
    sb.append("- 个性化处理：加入合理的个人观点和独特视角\n");
    sb.append("- 修辞优化：运用多样的修辞手法提升表达效果\n");
    sb.append("- 内容理解：精准把握原文核心信息\n");
    sb.append("- 创意发散：进行合理的思维延伸和联想\n");
    sb.append("- 细节补充：添加生动具体的细节描述\n");
    sb.append("- 真实性把控：保持改写内容的可信度\n\n");

    sb.append("## 规则\n");
    sb.append("- 必须完全重写而非简单改写\n");
    sb.append("- 核心信息必须保持准确无误\n");
    sb.append("- 风格转换要自然不做作\n");
    sb.append("- 个人色彩添加要适度合理\n");
    sb.append("- 拒绝直接复制原句结构\n");
    sb.append("- 避免过于完美工整的表达\n");
    sb.append("- 适当保留人类写作的不完美特征\n");
    sb.append("- 不改变原文核心事实\n");
    sb.append("- 不添加过度主观偏激内容\n\n");

    sb.append("## 工作流程\n");
    sb.append("1. 深度理解原文核心观点和关键信息\n");
    sb.append("2. 完全放下原文表达方式，仅保留信息骨架\n");
    sb.append("3. 以全新的人类视角和表达方式重新创作\n");
    sb.append("4. 加入适当的情感表达和个性化元素\n");
    sb.append("5. 进行自然度检查和风格一致性确认\n\n");

    sb.append("## 任务\n");
    sb.append("请帮我润色以下内容：\n\n");
    sb.append("原文：\n").append(content).append("\n\n");
    sb.append("润色风格：").append(style != null ? style : "更加优美流畅").append("\n\n");

    sb.append("## 输出格式\n");
    sb.append("- 格式：纯文本，自然段落结构\n");
    sb.append("- 风格：符合人类作者的写作风格\n");
    sb.append("- 读起来像真人撰写的高质量内容\n");
    sb.append("- 适合校园社交平台发布\n");
    sb.append("- 直接输出润色后的内容，不要有其他解释\n");
    return sb.toString();
  }

  private String callDeepSeek(String prompt) {
    try {
      JSONObject requestBody = new JSONObject();
      requestBody.set("model", model);

      JSONArray messages = new JSONArray();
      JSONObject message = new JSONObject();
      message.set("role", "user");
      message.set("content", prompt);
      messages.add(message);
      requestBody.set("messages", messages);

      requestBody.set("temperature", 0.8);
      requestBody.set("max_tokens", 1000);

      log.info("正在调用DeepSeek API, URL: {}", apiUrl);

      HttpResponse response = HttpRequest.post(apiUrl)
          .header("Authorization", "Bearer " + apiKey)
          .header("Content-Type", "application/json")
          .body(requestBody.toString())
          .timeout(60000) // 增加超时时间到60秒
          .execute();

      log.info("DeepSeek API响应状态: {}", response.getStatus());

      if (response.isOk()) {
        String responseBody = response.body();
        log.info("DeepSeek API响应内容长度: {}", responseBody.length());

        JSONObject result = JSONUtil.parseObj(responseBody);
        JSONArray choices = result.getJSONArray("choices");
        if (choices != null && !choices.isEmpty()) {
          String content = choices.getJSONObject(0)
              .getJSONObject("message")
              .getStr("content");
          if (content != null && !content.isEmpty()) {
            log.info("DeepSeek API调用成功，返回内容长度: {}", content.length());
            return content;
          }
        }
        // 如果choices为空，记录完整响应
        log.error("DeepSeek API返回格式异常: {}", responseBody);
        throw new RuntimeException("AI返回内容为空，请重试");
      } else {
        String errorBody = response.body();
        log.error("DeepSeek API调用失败, 状态码: {}, 响应: {}", response.getStatus(), errorBody);
        // 尝试解析错误信息
        try {
          JSONObject errorJson = JSONUtil.parseObj(errorBody);
          String errorMsg = errorJson.getByPath("error.message", String.class);
          if (errorMsg != null) {
            throw new RuntimeException("AI服务错误: " + errorMsg);
          }
        } catch (Exception ignored) {
        }
        throw new RuntimeException("AI服务暂时不可用(错误码:" + response.getStatus() + ")");
      }
    } catch (RuntimeException e) {
      throw e; // 重新抛出已处理的异常
    } catch (Exception e) {
      log.error("DeepSeek API调用异常", e);
      throw new RuntimeException("AI服务连接失败: " + e.getMessage());
    }
  }

  private List<String> parseMultipleResults(String result) {
    List<String> contents = new ArrayList<>();
    if (result == null || result.isEmpty()) {
      return contents;
    }

    // 尝试按【1】【2】【3】分割
    String[] parts = result.split("【[123]】");
    for (String part : parts) {
      String trimmed = part.trim();
      if (!trimmed.isEmpty()) {
        contents.add(trimmed);
      }
    }

    // 如果分割失败，直接返回整个结果
    if (contents.isEmpty()) {
      contents.add(result.trim());
    }

    return contents;
  }

  private void saveUsage(Long userId, String type, String prompt, String result) {
    AiUsage usage = new AiUsage();
    usage.setUserId(userId);
    usage.setType(type);
    usage.setPrompt(prompt.length() > 500 ? prompt.substring(0, 500) : prompt);
    usage.setResult(result.length() > 2000 ? result.substring(0, 2000) : result);
    usage.setUsageDate(LocalDate.now());
    usage.setCreateTime(LocalDateTime.now());
    aiMapper.insertUsage(usage);
  }
}
