package com.example.dto;

/**
 * 词云数据DTO
 */
public class WordCloudDTO {
  private String word;
  private Integer count;

  public WordCloudDTO() {
  }

  public WordCloudDTO(String word, Integer count) {
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
