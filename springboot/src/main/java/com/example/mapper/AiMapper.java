package com.example.mapper;

import com.example.entity.AiUsage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

@Mapper
public interface AiMapper {

  /**
   * 查询用户当天使用次数
   */
  int countTodayUsage(@Param("userId") Long userId, @Param("date") LocalDate date);

  /**
   * 插入使用记录
   */
  void insertUsage(AiUsage usage);
}
