package com.example.dto;

/**
 * 续写资格检查结果
 */
public class ContinueCheckResult {

  private Boolean canContinue;
  private String reason;
  private Integer remainingSeconds; // 距离可续写的剩余秒数
  private Integer unreadCount; // 未阅读的段落数

  // ===== 锁定机制 =====
  private Boolean lockedByOther;
  private String lockingUserNickname;
  private Integer lockExpireSeconds;

  public ContinueCheckResult() {
  }

  public ContinueCheckResult(Boolean canContinue, String reason) {
    this.canContinue = canContinue;
    this.reason = reason;
  }

  public static ContinueCheckResult success() {
    return new ContinueCheckResult(true, null);
  }

  public static ContinueCheckResult fail(String reason) {
    return new ContinueCheckResult(false, reason);
  }

  public static ContinueCheckResult failWithCooldown(String reason, int remainingSeconds) {
    ContinueCheckResult result = new ContinueCheckResult(false, reason);
    result.setRemainingSeconds(remainingSeconds);
    return result;
  }

  public static ContinueCheckResult failWithUnread(String reason, int unreadCount) {
    ContinueCheckResult result = new ContinueCheckResult(false, reason);
    result.setUnreadCount(unreadCount);
    return result;
  }

  public static ContinueCheckResult failWithLock(String reason, String lockingUserNickname, int lockExpireSeconds) {
    ContinueCheckResult result = new ContinueCheckResult(false, reason);
    result.setLockedByOther(true);
    result.setLockingUserNickname(lockingUserNickname);
    result.setLockExpireSeconds(lockExpireSeconds);
    return result;
  }

  // ========== Getter/Setter ==========

  public Boolean getCanContinue() {
    return canContinue;
  }

  public void setCanContinue(Boolean canContinue) {
    this.canContinue = canContinue;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public Integer getRemainingSeconds() {
    return remainingSeconds;
  }

  public void setRemainingSeconds(Integer remainingSeconds) {
    this.remainingSeconds = remainingSeconds;
  }

  public Integer getUnreadCount() {
    return unreadCount;
  }

  public void setUnreadCount(Integer unreadCount) {
    this.unreadCount = unreadCount;
  }

  public Boolean getLockedByOther() {
    return lockedByOther;
  }

  public void setLockedByOther(Boolean lockedByOther) {
    this.lockedByOther = lockedByOther;
  }

  public String getLockingUserNickname() {
    return lockingUserNickname;
  }

  public void setLockingUserNickname(String lockingUserNickname) {
    this.lockingUserNickname = lockingUserNickname;
  }

  public Integer getLockExpireSeconds() {
    return lockExpireSeconds;
  }

  public void setLockExpireSeconds(Integer lockExpireSeconds) {
    this.lockExpireSeconds = lockExpireSeconds;
  }
}
