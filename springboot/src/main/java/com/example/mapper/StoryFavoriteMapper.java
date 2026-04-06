package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.StoryFavorite;
import org.apache.ibatis.annotations.Mapper;

/**
 * 故事收藏Mapper接口
 */
@Mapper
public interface StoryFavoriteMapper extends BaseMapper<StoryFavorite> {
}
