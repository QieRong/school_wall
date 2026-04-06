package com.example.common;

/**
 * 业务异常类
 * 【优化】统一业务异常处理，便于区分异常类型
 */
public class BizException extends RuntimeException {

  private String code;

  public BizException(String message) {
    super(message);
    this.code = "500";
  }

  public BizException(String code, String message) {
    super(message);
    this.code = code;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }
}
