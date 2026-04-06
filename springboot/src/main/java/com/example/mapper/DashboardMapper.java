package com.example.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 数据大屏Mapper
 */
@Mapper
public interface DashboardMapper {

  // 用户统计查询
  Integer countTotalUsers();

  Integer countTodayNewUsers();

  Integer countTodayActiveUsers();

  Integer countTodayPosts();

  Integer countTodayInteractions();

  Integer countYesterdayPosts();

  Integer countYesterdayInteractions();

  List<Map<String, Object>> selectUserTrend(@Param("days") int days);

  List<Map<String, Object>> selectCreditDistribution();

  // 用户分层查询
  Integer countCreators(@Param("minPosts") int minPosts);

  Integer countInteractors(@Param("minComments") int minComments);

  // 用户排行查询
  List<Map<String, Object>> selectTopLikedUsers(@Param("limit") int limit);

  List<Map<String, Object>> selectTopReporters(@Param("limit") int limit);

  List<Map<String, Object>> selectTopCommenters(@Param("limit") int limit);

  // 内容统计查询
  Integer countPendingAudit();

  Integer countTodayAudited();

  Integer countTodayPassed();

  List<Map<String, Object>> selectSensitiveWordStats(@Param("days") int days, @Param("limit") int limit);

  // 举报统计查询
  Integer countPendingReports();

  Integer countTodayReports();

  Integer countHandledReports();

  List<Map<String, Object>> selectReportDistribution();

  // 安全警报查询
  List<Map<String, Object>> selectHighReportPosts(@Param("minutes") int minutes, @Param("threshold") int threshold);

  List<Map<String, Object>> selectSpamUsers(@Param("minutes") int minutes, @Param("threshold") int threshold);

  // 时间维度查询
  List<Map<String, Object>> selectHourlyPosts();

  List<Map<String, Object>> selectHourlyComments();

  // 词云查询
  List<Map<String, Object>> selectPostContents(@Param("days") int days);

  // 周对比查询
  Integer countPostsBetween(@Param("startDate") String startDate, @Param("endDate") String endDate);

  Integer countActiveUsersBetween(@Param("startDate") String startDate, @Param("endDate") String endDate);

  Integer countInteractionsBetween(@Param("startDate") String startDate, @Param("endDate") String endDate);
}
