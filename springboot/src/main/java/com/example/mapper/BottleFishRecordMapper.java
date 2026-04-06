// File: springboot/src/main/java/com/example/mapper/BottleFishRecordMapper.java
package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.BottleFishRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * 打捞记录Mapper接口
 */
@Mapper
public interface BottleFishRecordMapper extends BaseMapper<BottleFishRecord> {

  /**
   * 获取用户最后一次打捞时间
   */
  @Select("SELECT create_time FROM bottle_fish_record WHERE user_id = #{userId} ORDER BY create_time DESC LIMIT 1")
  LocalDateTime getLastFishTime(@Param("userId") Long userId);

  /**
   * 获取用户最后一次打捞记录（加行锁，防止并发）
   */
  @Select("SELECT * FROM bottle_fish_record WHERE user_id = #{userId} ORDER BY create_time DESC LIMIT 1 FOR UPDATE")
  BottleFishRecord getLastRecordForUpdate(@Param("userId") Long userId);

  /**
   * 统计用户总打捞次数
   */
  @Select("SELECT COUNT(*) FROM bottle_fish_record WHERE user_id = #{userId}")
  Long countByUserId(@Param("userId") Long userId);

  /**
   * 统计总打捞次数
   */
  @Select("SELECT COUNT(*) FROM bottle_fish_record")
  Long countTotal();

  /**
   * 获取用户最后一次打捞记录（不加锁）
   */
  @Select("SELECT * FROM bottle_fish_record WHERE user_id = #{userId} ORDER BY create_time DESC LIMIT 1")
  BottleFishRecord getLastRecord(@Param("userId") Long userId);
}
