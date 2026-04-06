package com.example.task;

import cn.hutool.core.io.FileUtil;
import com.example.entity.FileRecord;
import com.example.mapper.FileRecordMapper;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@EnableScheduling
public class FileCleanupTask {

    @Resource
    private FileRecordMapper fileRecordMapper;

    // 每天凌晨 3 点执行清理
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanUnusedFiles() {
        System.out.println(">>> [定时任务] 开始清理7天前的垃圾图片...");

        // 1. 查询过期且未使用的文件
        List<FileRecord> expiredFiles = fileRecordMapper.selectExpiredFiles();

        for (FileRecord file : expiredFiles) {
            // 2. 删除物理文件
            boolean deleted = FileUtil.del(file.getFilePath());
            if (deleted) {
                System.out.println("已删除垃圾文件: " + file.getFilePath());
                // 3. 删除数据库记录
                fileRecordMapper.deleteById(file.getId());
            }
        }
        System.out.println(">>> [定时任务] 清理结束，共清理 " + expiredFiles.size() + " 个文件");
    }
}