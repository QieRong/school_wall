// File: springboot/src/main/java/com/example/mapper/BottleMapper.java
package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.DriftBottle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 漂流瓶Mapper接口
 */
@Mapper
public interface BottleMapper extends BaseMapper<DriftBottle> {

        /**
         * 随机获取一个可打捞的漂流瓶(排除自己的瓶子)
         */
        DriftBottle getRandomBottleExcludeUser(@Param("excludeUserId") Long excludeUserId);

        /**
         * 获取用户投放的瓶子列表(带回复数)
         */
        List<DriftBottle> getMySentBottles(@Param("userId") Long userId);

        /**
         * 获取用户珍藏的瓶子列表
         */
        List<DriftBottle> getMyCollectedBottles(@Param("userId") Long userId);

        /**
         * 获取瓶子详情(带回复列表)
         */
        DriftBottle getBottleWithReplies(@Param("bottleId") Long bottleId);

        /**
         * 管理员查询瓶子列表(支持过滤)
         */
        List<DriftBottle> getAdminBottleList(
                        @Param("status") Integer status,
                        @Param("direction") Integer direction,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        /**
         * 统计总数
         */
        @Select("SELECT COUNT(*) FROM drift_bottle")
        Long countTotal();

        /**
         * 按状态统计
         */
        @Select("SELECT COUNT(*) FROM drift_bottle WHERE status = #{status}")
        Long countByStatus(@Param("status") int status);

        /**
         * 统计今日新增
         */
        @Select("SELECT COUNT(*) FROM drift_bottle WHERE DATE(create_time) = CURDATE()")
        Long countTodayNew();

        /**
         * 增加瓶子查看次数
         */
        @Update("UPDATE drift_bottle SET view_count = view_count + 1 WHERE id = #{bottleId}")
        int incrementViewCount(@Param("bottleId") Long bottleId);

        /**
         * 统计漂流中的瓶子数量
         */
        @Select("SELECT COUNT(*) FROM drift_bottle WHERE status = 0")
        Long countActiveBottles();

        /**
         * 统计今日新增瓶子数量
         */
        @Select("SELECT COUNT(*) FROM drift_bottle WHERE DATE(create_time) = CURDATE()")
        Long countTodayNewBottles();

        /**
         * 统计已沉没瓶子数量
         */
        @Select("SELECT COUNT(*) FROM drift_bottle WHERE status = 3")
        Long countSunkenBottles();

        /**
         * 统计被珍藏瓶子数量
         */
        @Select("SELECT COUNT(*) FROM drift_bottle WHERE status = 2")
        Long countCollectedBottles();

        /**
         * 批量更新过期瓶子状态为已沉没
         * 
         * @return 更新的记录数
         */
        @Update("UPDATE drift_bottle SET status = 3 WHERE status = 0 AND expire_time < NOW()")
        int updateExpiredBottles();

        /**
         * 获取用户收到的回复总数
         */
        @Select("SELECT COUNT(*) FROM bottle_reply br " +
                        "INNER JOIN drift_bottle db ON br.bottle_id = db.id " +
                        "WHERE db.user_id = #{userId}")
        Long countUserReceivedReplies(@Param("userId") Long userId);

        /**
         * 获取用户最近投放的瓶子
         */
        @Select("SELECT * FROM drift_bottle WHERE user_id = #{userId} ORDER BY create_time DESC LIMIT 1")
        DriftBottle getLastBottleByUser(@Param("userId") Long userId);
}
