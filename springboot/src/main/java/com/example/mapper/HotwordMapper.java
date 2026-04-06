package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.dto.HotwordVO;
import com.example.dto.RankingVO;
import com.example.entity.Hotword;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface HotwordMapper extends BaseMapper<Hotword> {

  /**
   * 获取热词列表（带作者信息）
   */
  List<HotwordVO> selectHotwordList(@Param("status") Integer status);

  /**
   * 获取榜单数据
   * 
   * @param type day/week/month/all
   */
  List<RankingVO> selectRanking(@Param("type") String type, @Param("limit") Integer limit);

  /**
   * 获取上一周期的排名（用于计算排名变化）
   */
  List<RankingVO> selectPreviousRanking(@Param("type") String type, @Param("limit") Integer limit);

  /**
   * 搜索热词
   */
  List<HotwordVO> searchHotwords(@Param("keyword") String keyword);

  /**
   * 获取用户投稿的热词
   */
  List<HotwordVO> selectByUserId(@Param("userId") Long userId);

  /**
   * 获取需要归档的热词ID（30天无投票）
   */
  List<Long> selectExpiredHotwordIds(@Param("expireTime") LocalDateTime expireTime);

  /**
   * 批量归档热词
   */
  int batchArchive(@Param("ids") List<Long> ids);

  /**
   * 获取热词详情（带作者信息）
   */
  HotwordVO selectDetailById(@Param("id") Long id);
}
