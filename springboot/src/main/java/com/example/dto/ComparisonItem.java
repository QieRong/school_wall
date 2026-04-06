package com.example.dto;

/**
 * 对比项DTO
 */
public class ComparisonItem {
  private Integer current;
  private Integer previous;
  private Double changePercent;

  public ComparisonItem() {
  }

  public ComparisonItem(Integer current, Integer previous) {
    this.current = current;
    this.previous = previous;
    if (previous != null && previous != 0) {
      this.changePercent = ((double) (current - previous) / previous) * 100;
    } else if (current != null && current > 0) {
      this.changePercent = 100.0;
    } else {
      this.changePercent = 0.0;
    }
  }

  public Integer getCurrent() {
    return current;
  }

  public void setCurrent(Integer current) {
    this.current = current;
  }

  public Integer getPrevious() {
    return previous;
  }

  public void setPrevious(Integer previous) {
    this.previous = previous;
  }

  public Double getChangePercent() {
    return changePercent;
  }

  public void setChangePercent(Double changePercent) {
    this.changePercent = changePercent;
  }
}
