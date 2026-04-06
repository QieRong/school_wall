package com.example.config;

import com.example.utils.SensitiveFilter;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Component
@SuppressWarnings("null")
public class SensitiveWordLoader {

    @Resource
    private JdbcTemplate jdbcTemplate; // 直接操作数据库，比 MyBatis 更快更适合这种批量任务

    @Resource
    private SensitiveFilter sensitiveFilter;

    // 垃圾词正则：纯数字、纯符号、或者包含 www/http 的网址
    private static final Pattern TRASH_PATTERN = Pattern.compile("^([\\d\\p{Punct}]+|.*www\\..*|.*http.*)$");

    @PostConstruct
    public void init() {
        // 1. 检查数据库是否有词
        Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM sys_sensitive", Integer.class);

        if (count != null && count == 0) {
            System.out.println(">>> [系统启动] 检测到词库为空，开始从文件执行【深度清洗】并导入...");
            importFromTxt();
        } else {
            System.out.println(">>> [系统启动] 词库已有 " + count + " 条数据，跳过文件导入");
        }

        // 2. 从数据库加载到内存 (DFA)
        loadToMemory();
    }

    private void importFromTxt() {
        try {
            InputStream in = this.getClass().getClassLoader().getResourceAsStream("sensitive_words.txt");
            if (in == null) {
                System.err.println("!!! 未找到 sensitive_words.txt 文件");
                return;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            Set<String> cleanWords = new HashSet<>(); // 使用 Set 自动去重
            String line;

            while ((line = reader.readLine()) != null) {
                String word = line.trim();

                // === 核心清洗逻辑 ===
                // 1. 长度必须 > 1 (防止误杀单字 "做", "日")
                if (word.length() <= 1)
                    continue;

                // 2. 剔除纯数字/符号/网址垃圾
                if (TRASH_PATTERN.matcher(word).matches())
                    continue;

                // 3. 剔除包含 "&" "|" 等特殊技术符号的
                if (word.contains("&") || word.contains("|"))
                    continue;

                cleanWords.add(word);
            }

            // 批量入库
            System.out.println(">>> 清洗完成，准备入库 " + cleanWords.size() + " 条有效词汇...");
            String sql = "INSERT IGNORE INTO sys_sensitive (word) VALUES (?)";
            List<Object[]> batchArgs = new ArrayList<>();
            for (String w : cleanWords) {
                batchArgs.add(new Object[] { w });
            }

            // 分批执行防止 SQL 过大
            int batchSize = 1000;
            for (int i = 0; i < batchArgs.size(); i += batchSize) {
                int end = Math.min(i + batchSize, batchArgs.size());
                jdbcTemplate.batchUpdate(sql, batchArgs.subList(i, end));
            }

            System.out.println(">>> 敏感词入库完成！");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadToMemory() {
        List<String> words = jdbcTemplate.queryForList("SELECT word FROM sys_sensitive", String.class);
        // 调用 SensitiveFilter 的刷新方法 (需要您确认 SensitiveFilter 里有 refresh 方法，或者直接重写 init)
        // 这里为了适配您之前的代码，我们假设 SensitiveFilter 有一个 public 的 addWords 方法或者重置方法
        // 简单起见，我们直接在这里调用 filter 的重新构建逻辑，或者通知 filter
        sensitiveFilter.refresh(words);
    }
}