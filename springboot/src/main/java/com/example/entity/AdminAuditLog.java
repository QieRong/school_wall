package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 管理员操作审计日志实体类
 */
@Data
@TableName("admin_audit_log")
public class AdminAuditLog {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long adminId;

  private String adminName;

  private String action;

  private String targetType;

  private Long targetId;

  private String reason;

  private String ipAddress;

  private LocalDateTime createTime;
}
