package com.example.dto;

/**
 * 敏感词统计DTO
 */
public class SensitiveWordStat {
  private String word;
  private Integer count;

  public SensitiveWordStat() {
  }

  public SensitiveWordStat(String word, Integer count) {
    this.word = word;
    this.count = count;
  }

  public String getWord() {
    return word;
  }

  public void setWord(String word) {
    this.word = word;
  }

  public Integer getCount() {
    return count;
  }

  public void setCount(Integer count) {
    this.count = count;
  }
}
