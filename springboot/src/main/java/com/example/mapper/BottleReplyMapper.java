// File: springboot/src/main/java/com/example/mapper/BottleReplyMapper.java
package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.BottleReply;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 漂流瓶回复Mapper接口
 */
@Mapper
public interface BottleReplyMapper extends BaseMapper<BottleReply> {

  /**
   * 获取瓶子的所有回复(带用户信息)
   */
  @Select("SELECT br.*, u.nickname, u.avatar FROM bottle_reply br " +
      "LEFT JOIN sys_user u ON br.user_id = u.id " +
      "WHERE br.bottle_id = #{bottleId} ORDER BY br.create_time ASC")
  List<BottleReply> selectByBottleIdWithUser(@Param("bottleId") Long bottleId);

  /**
   * 统计瓶子的回复数
   */
  @Select("SELECT COUNT(*) FROM bottle_reply WHERE bottle_id = #{bottleId}")
  Integer countByBottleId(@Param("bottleId") Long bottleId);

  /**
   * 统计用户收到的回复数(用户投放的瓶子收到的回复)
   */
  @Select("SELECT COUNT(*) FROM bottle_reply br " +
      "INNER JOIN drift_bottle db ON br.bottle_id = db.id " +
      "WHERE db.user_id = #{userId}")
  Long countRepliesReceived(@Param("userId") Long userId);
}
