package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.FileRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

@Mapper
public interface FileRecordMapper extends BaseMapper<FileRecord> {
    // 查找7天前且未被使用的垃圾文件
    @Select("SELECT * FROM sys_file_record WHERE is_used = 0 AND create_time < DATE_SUB(NOW(), INTERVAL 7 DAY)")
    List<FileRecord> selectExpiredFiles();

    // 标记文件为已使用 (根据URL)
    @Update("UPDATE sys_file_record SET is_used = 1 WHERE file_url = #{url}")
    void markAsUsed(String url);
}