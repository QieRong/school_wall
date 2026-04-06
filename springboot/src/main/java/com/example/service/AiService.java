package com.example.service;

import java.util.List;
import java.util.Map;

/**
 * AI服务接口
 */
public interface AiService {

  /**
   * 生成表白文案
   * 
   * @param userId 用户ID
   * @param params 参数（target: 对象, style: 风格, keywords: 关键词）
   * @return 生成的文案列表
   */
  List<String> generateContent(Long userId, Map<String, String> params);

  /**
   * 润色内容
   * 
   * @param userId  用户ID
   * @param content 原始内容
   * @param style   润色风格
   * @return 润色后的内容
   */
  String polishContent(Long userId, String content, String style);

  /**
   * 获取用户今日剩余次数
   */
  int getRemainingCount(Long userId);
}
