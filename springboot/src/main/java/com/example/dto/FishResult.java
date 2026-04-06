// File: springboot/src/main/java/com/example/dto/FishResult.java
package com.example.dto;

import com.example.entity.DriftBottle;

/**
 * 打捞结果DTO
 */
public class FishResult {

  /** 是否成功 */
  private boolean success;

  /** 打捞到的瓶子(成功时返回) */
  private DriftBottle bottle;

  /** 剩余冷却秒数(冷却中时返回) */
  private Integer cooldownSeconds;

  /** 提示信息 */
  private String message;

  // 静态工厂方法
  public static FishResult success(DriftBottle bottle) {
    FishResult result = new FishResult();
    result.setSuccess(true);
    result.setBottle(bottle);
    result.setMessage("打捞成功");
    return result;
  }

  public static FishResult cooldown(int seconds) {
    FishResult result = new FishResult();
    result.setSuccess(false);
    result.setCooldownSeconds(seconds);
    result.setMessage("打捞冷却中，请" + seconds + "秒后再试");
    return result;
  }

  public static FishResult empty() {
    FishResult result = new FishResult();
    result.setSuccess(false);
    result.setMessage("海洋中暂无漂流瓶");
    return result;
  }

  // Getter/Setter
  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public DriftBottle getBottle() {
    return bottle;
  }

  public void setBottle(DriftBottle bottle) {
    this.bottle = bottle;
  }

  public Integer getCooldownSeconds() {
    return cooldownSeconds;
  }

  public void setCooldownSeconds(Integer cooldownSeconds) {
    this.cooldownSeconds = cooldownSeconds;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
