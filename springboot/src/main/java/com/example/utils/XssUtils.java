package com.example.utils;

import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;

/**
 * XSS 防护工具类
 * 对用户输入进行 HTML 转义，防止跨站脚本攻击
 */
@Component
public class XssUtils {

  /**
   * 转义 HTML 特殊字符
   * 
   * @param input 用户输入
   * @return 转义后的安全字符串
   */
  public static String escapeHtml(String input) {
    if (StrUtil.isBlank(input)) {
      return input;
    }

    StringBuilder escaped = new StringBuilder(input.length() + 16);
    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      switch (c) {
        case '<':
          escaped.append("&lt;");
          break;
        case '>':
          escaped.append("&gt;");
          break;
        case '&':
          escaped.append("&amp;");
          break;
        case '"':
          escaped.append("&quot;");
          break;
        case '\'':
          escaped.append("&#x27;");
          break;
        case '/':
          escaped.append("&#x2F;");
          break;
        default:
          escaped.append(c);
      }
    }
    return escaped.toString();
  }

  /**
   * 清理危险的 HTML 标签和属性
   * 保留纯文本内容
   */
  public static String stripHtml(String input) {
    if (StrUtil.isBlank(input)) {
      return input;
    }
    // 移除所有 HTML 标签
    return input.replaceAll("<[^>]*>", "")
        .replaceAll("javascript:", "")
        .replaceAll("vbscript:", "")
        .replaceAll("expression\\(", "")
        .replaceAll("on\\w+\\s*=", "");
  }
}
