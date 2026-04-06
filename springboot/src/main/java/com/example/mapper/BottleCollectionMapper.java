// File: springboot/src/main/java/com/example/mapper/BottleCollectionMapper.java
package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.BottleCollection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 漂流瓶珍藏Mapper接口
 */
@Mapper
public interface BottleCollectionMapper extends BaseMapper<BottleCollection> {

  /**
   * 统计用户今日珍藏数量
   */
  @Select("SELECT COUNT(*) FROM bottle_collection WHERE user_id = #{userId} AND DATE(create_time) = CURDATE()")
  Integer countTodayCollections(@Param("userId") Long userId);

  /**
   * 统计用户总珍藏数量
   */
  @Select("SELECT COUNT(*) FROM bottle_collection WHERE user_id = #{userId}")
  Long countByUserId(@Param("userId") Long userId);

  /**
   * 检查是否已珍藏
   */
  @Select("SELECT id FROM bottle_collection WHERE user_id = #{userId} AND bottle_id = #{bottleId} LIMIT 1")
  Long checkCollected(@Param("userId") Long userId, @Param("bottleId") Long bottleId);
}
