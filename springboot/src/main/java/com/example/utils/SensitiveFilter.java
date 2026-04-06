// File: springboot/src/main/java/com/example/utils/SensitiveFilter.java
package com.example.utils;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 敏感词过滤器 (自定义 DFA 算法 - 防误杀版)
 * 特性：
 * 1. 自动过滤单字 (长度<2 的词不加载)
 * 2. 忽略特殊符号 (如 "色-情" 也能识别)
 * 3. 全词完全匹配才屏蔽
 */
@Component
public class SensitiveFilter {

    private static final Logger logger = LoggerFactory.getLogger(SensitiveFilter.class);
    private static final String REPLACEMENT = "***";

    // 根节点
    private final TrieNode rootNode = new TrieNode();

    @PostConstruct
    public void init() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                ResourceUtil.getStream("sensitive_words.txt"), StandardCharsets.UTF_8))) {

            List<String> words = reader.lines().collect(Collectors.toList());
            refresh(words); // 复用 refresh 逻辑进行初始化

        } catch (Exception e) {
            logger.error("敏感词文件加载失败", e);
        }
    }

    /**
     * 刷新词库 (清空旧词，加载新词)
     * 同时过滤掉所有单字，防止误杀
     */
    public void refresh(List<String> newWords) {
        // 1. 清空现有树
        rootNode.subNodes.clear();

        int count = 0;
        int skipCount = 0;

        for (String word : newWords) {
            if (StrUtil.isBlank(word)) continue;
            String key = word.trim();

            // 【核心逻辑】只保留长度 >= 2 的词
            // 这样像 "个"、"a"、"的" 这种单字绝对不会被加载进去
            if (key.length() < 2) {
                skipCount++;
                continue;
            }

            addWord(key);
            count++;
        }
        logger.info("敏感词库加载完成。有效词条: {}, 已自动过滤单字/无效词: {}", count, skipCount);
    }

    /**
     * 添加敏感词到字典树
     */
    public void addWord(String text) {
        TrieNode tempNode = rootNode;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            // 跳过符号，只存有效字符
            if (isSymbol(c)) continue;

            TrieNode subNode = tempNode.getSubNode(c);
            if (subNode == null) {
                subNode = new TrieNode();
                tempNode.addSubNode(c, subNode);
            }
            tempNode = subNode;

            if (i == text.length() - 1) {
                tempNode.setKeywordEnd(true);
            }
        }
    }

    /**
     * 过滤文本 (敏感词替换为 ***)
     */
    public String filter(String text) {
        if (StrUtil.isBlank(text)) return text;

        StringBuilder result = new StringBuilder();
        TrieNode tempNode = rootNode;
        int begin = 0;
        int position = 0;

        while (position < text.length()) {
            char c = text.charAt(position);

            // 1. 跳过符号 (如 "色&情" -> 跳过 & 继续匹配 "情")
            if (isSymbol(c)) {
                if (tempNode == rootNode) {
                    result.append(c);
                    begin++;
                }
                position++;
                continue;
            }

            // 2. 检查节点
            tempNode = tempNode.getSubNode(c);
            if (tempNode == null) {
                // 匹配失败，说明以 begin 开头的这段不是敏感词
                result.append(text.charAt(begin));
                position = begin + 1;
                begin = position;
                tempNode = rootNode;
            } else if (tempNode.isKeywordEnd()) {
                // 匹配成功，发现敏感词 -> 替换
                result.append(REPLACEMENT);
                position++;
                begin = position;
                tempNode = rootNode;
            } else {
                // 匹配中，继续往后看
                position++;
            }
        }

        // 收尾：把最后剩下的字符拼上去
        result.append(text.substring(begin));
        return result.toString();
    }

    /**
     * 判断是否包含敏感词
     */
    public boolean hasSensitiveWord(String text) {
        if (StrUtil.isBlank(text)) return false;
        TrieNode tempNode = rootNode;
        int begin = 0;
        int position = 0;

        while (position < text.length()) {
            char c = text.charAt(position);
            if (isSymbol(c)) {
                if (tempNode == rootNode) begin++;
                position++;
                continue;
            }
            tempNode = tempNode.getSubNode(c);
            if (tempNode == null) {
                position = begin + 1;
                begin = position;
                tempNode = rootNode;
            } else if (tempNode.isKeywordEnd()) {
                return true;
            } else {
                position++;
            }
        }
        return false;
    }

    /**
     * 判断是否为特殊符号
     */
    private boolean isSymbol(char c) {
        // 0x2E80 ~ 0x9FFF 是东亚文字范围
        return !Character.isLetterOrDigit(c) && (c < 0x2E80 || c > 0x9FFF);
    }

    /**
     * 内部类：前缀树节点
     */
    private static class TrieNode {
        private boolean isKeywordEnd = false;
        private Map<Character, TrieNode> subNodes = new HashMap<>();

        public boolean isKeywordEnd() { return isKeywordEnd; }
        public void setKeywordEnd(boolean keywordEnd) { isKeywordEnd = keywordEnd; }
        public void addSubNode(Character c, TrieNode node) { subNodes.put(c, node); }
        public TrieNode getSubNode(Character c) { return subNodes.get(c); }
    }
}