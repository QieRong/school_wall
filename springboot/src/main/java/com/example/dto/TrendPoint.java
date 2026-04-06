package com.example.dto;

/**
 * 趋势数据点DTO
 */
public class TrendPoint {
  private String date;
  private Integer value;

  public TrendPoint() {
  }

  public TrendPoint(String date, Integer value) {
    this.date = date;
    this.value = value;
  }

  public String getDate() {
    return date;
  }

  public void setDate(String date) {
    this.date = date;
  }

  public Integer getValue() {
    return value;
  }

  public void setValue(Integer value) {
    this.value = value;
  }
}
