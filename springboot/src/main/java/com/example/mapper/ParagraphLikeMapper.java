package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.ParagraphLike;
import org.apache.ibatis.annotations.Mapper;

/**
 * 段落点赞Mapper接口
 */
@Mapper
public interface ParagraphLikeMapper extends BaseMapper<ParagraphLike> {
}
