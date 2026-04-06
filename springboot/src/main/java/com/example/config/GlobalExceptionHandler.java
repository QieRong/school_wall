package com.example.config;

import com.example.common.BizException;
import com.example.common.Result;
import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理所有Controller层抛出的异常
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * 处理客户端中断连接异常（用户切换页面、关闭浏览器等）
   * 这不是真正的错误，只需要静默忽略
   */
  @ExceptionHandler(ClientAbortException.class)
  public void handleClientAbortException(ClientAbortException e) {
    // 客户端主动断开连接，静默忽略，不记录错误日志
    logger.debug("客户端中断连接: {}", e.getMessage());
  }

  /**
   * 处理业务异常
   */
  @ExceptionHandler(BizException.class)
  public Result<?> handleBizException(BizException e) {
    logger.warn("业务异常: {}", e.getMessage());
    Result<Object> result = new Result<>();
    result.setCode(e.getCode());
    result.setMsg(e.getMessage());
    return result;
  }

  /**
   * 处理参数校验异常 - @RequestBody
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Result<?> handleValidationException(MethodArgumentNotValidException e) {
    String message = e.getBindingResult().getFieldErrors().stream()
        .map(FieldError::getDefaultMessage)
        .collect(Collectors.joining(", "));
    logger.warn("参数校验失败: {}", message);
    return Result.error(message);
  }

  /**
   * 处理参数校验异常 - @RequestParam
   */
  @ExceptionHandler(ConstraintViolationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Result<?> handleConstraintViolationException(ConstraintViolationException e) {
    String message = e.getConstraintViolations().stream()
        .map(ConstraintViolation::getMessage)
        .collect(Collectors.joining(", "));
    logger.warn("参数校验失败: {}", message);
    return Result.error(message);
  }

  /**
   * 处理参数绑定异常
   */
  @ExceptionHandler(BindException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Result<?> handleBindException(BindException e) {
    String message = e.getFieldErrors().stream()
        .map(FieldError::getDefaultMessage)
        .collect(Collectors.joining(", "));
    logger.warn("参数绑定失败: {}", message);
    return Result.error(message);
  }

  /**
   * 处理缺少请求参数异常
   */
  @ExceptionHandler(MissingServletRequestParameterException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Result<?> handleMissingParamException(MissingServletRequestParameterException e) {
    String message = "缺少必要参数: " + e.getParameterName();
    logger.warn(message);
    return Result.error(message);
  }

  /**
   * 处理参数类型不匹配异常
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Result<?> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
    String message = "参数类型错误: " + e.getName();
    logger.warn(message);
    return Result.error(message);
  }

  /**
   * 处理运行时异常
   */
  @ExceptionHandler(RuntimeException.class)
  public Result<?> handleRuntimeException(RuntimeException e) {
    logger.error("运行时异常: ", e);
    return Result.error(e.getMessage() != null ? e.getMessage() : "操作失败");
  }

  /**
   * 处理所有未捕获的异常
   */
  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public Result<?> handleException(Exception e) {
    logger.error("系统异常: ", e);
    return Result.error("系统繁忙，请稍后重试");
  }
}
