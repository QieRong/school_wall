// File: springboot/src/main/java/com/example/mapper/BottleAchievementMapper.java
package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.BottleAchievement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 漂流瓶成就Mapper接口
 */
@Mapper
public interface BottleAchievementMapper extends BaseMapper<BottleAchievement> {

  /**
   * 获取用户所有成就
   */
  @Select("SELECT * FROM bottle_achievement WHERE user_id = #{userId} ORDER BY create_time ASC")
  List<BottleAchievement> getByUserId(@Param("userId") Long userId);

  /**
   * 检查用户是否已获得某成就(返回id表示已获得)
   */
  @Select("SELECT id FROM bottle_achievement WHERE user_id = #{userId} AND achievement_type = #{type} LIMIT 1")
  Long checkAchievement(@Param("userId") Long userId, @Param("type") String type);
}
