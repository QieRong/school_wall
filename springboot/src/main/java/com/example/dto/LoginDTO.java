package com.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 登录请求DTO
 */
public class LoginDTO {

  @NotBlank(message = "账号不能为空")
  @Pattern(regexp = "^(\\d{10}|\\d{11}|admin)$", message = "账号格式错误，请输入10位学号、11位手机号或管理员账号")
  private String account;

  @NotBlank(message = "密码不能为空")
  private String password;

  public String getAccount() {
    return account;
  }

  public void setAccount(String account) {
    this.account = account;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
