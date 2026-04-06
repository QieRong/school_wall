package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.AdminAuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 管理员审计日志Mapper接口
 */
@Mapper
public interface AdminAuditLogMapper extends BaseMapper<AdminAuditLog> {

  /**
   * 查询最近的审计日志
   */
  @Select("SELECT * FROM admin_audit_log ORDER BY create_time DESC LIMIT #{limit}")
  List<AdminAuditLog> selectRecentLogs(int limit);

  /**
   * 按管理员查询日志
   */
  @Select("SELECT * FROM admin_audit_log WHERE admin_id = #{adminId} ORDER BY create_time DESC")
  List<AdminAuditLog> selectByAdminId(Long adminId);

  /**
   * 按操作类型查询日志
   */
  @Select("SELECT * FROM admin_audit_log WHERE action = #{action} ORDER BY create_time DESC LIMIT #{limit}")
  List<AdminAuditLog> selectByAction(String action, int limit);
}
