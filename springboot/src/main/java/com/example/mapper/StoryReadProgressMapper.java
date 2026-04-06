package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.StoryReadProgress;
import org.apache.ibatis.annotations.Mapper;

/**
 * 故事阅读进度Mapper接口
 */
@Mapper
public interface StoryReadProgressMapper extends BaseMapper<StoryReadProgress> {
}
