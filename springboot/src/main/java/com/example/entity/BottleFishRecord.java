// File: springboot/src/main/java/com/example/entity/BottleFishRecord.java
package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 打捞记录实体类
 * 对应数据库表 bottle_fish_record
 */
@TableName("bottle_fish_record")
public class BottleFishRecord implements Serializable {

  private static final long serialVersionUID = 1L;

  @TableId(type = IdType.AUTO)
  private Long id;

  /** 打捞者ID */
  private Long userId;

  /** 漂流瓶ID */
  private Long bottleId;

  /** 打捞时间 */
  private LocalDateTime createTime;

  // ========== Getter/Setter ==========

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public Long getBottleId() {
    return bottleId;
  }

  public void setBottleId(Long bottleId) {
    this.bottleId = bottleId;
  }

  public LocalDateTime getCreateTime() {
    return createTime;
  }

  public void setCreateTime(LocalDateTime createTime) {
    this.createTime = createTime;
  }
}
