package com.example.controller;

import com.example.common.Result;
import com.example.service.AiService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI助手接口
 */
@RestController
@RequestMapping("/ai")
@SuppressWarnings("rawtypes")
public class AiController {

  @Resource
  private AiService aiService;

  /**
   * 生成表白文案
   */
  @PostMapping("/generate")
  public Result generate(@RequestBody Map<String, String> params) {
    Long userId = Long.parseLong(params.getOrDefault("userId", "0"));
    if (userId == 0) {
      return Result.error("请先登录");
    }
    try {
      List<String> contents = aiService.generateContent(userId, params);
      Map<String, Object> data = new HashMap<>();
      data.put("contents", contents);
      data.put("remaining", aiService.getRemainingCount(userId));
      return Result.success(data);
    } catch (Exception e) {
      return Result.error(e.getMessage());
    }
  }

  /**
   * 润色内容
   */
  @PostMapping("/polish")
  public Result polish(@RequestBody Map<String, String> params) {
    Long userId = Long.parseLong(params.getOrDefault("userId", "0"));
    String content = params.get("content");
    String style = params.get("style");

    if (userId == 0) {
      return Result.error("请先登录");
    }
    if (content == null || content.trim().isEmpty()) {
      return Result.error("请输入要润色的内容");
    }

    try {
      String result = aiService.polishContent(userId, content, style);
      Map<String, Object> data = new HashMap<>();
      data.put("content", result);
      data.put("remaining", aiService.getRemainingCount(userId));
      return Result.success(data);
    } catch (Exception e) {
      return Result.error(e.getMessage());
    }
  }

  /**
   * 获取剩余使用次数
   */
  @GetMapping("/remaining")
  public Result getRemaining(@RequestParam Long userId) {
    if (userId == null || userId == 0) {
      return Result.error("请先登录");
    }
    int remaining = aiService.getRemainingCount(userId);
    return Result.success(remaining);
  }
}
