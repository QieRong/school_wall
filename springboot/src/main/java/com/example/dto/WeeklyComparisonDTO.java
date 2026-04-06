package com.example.dto;

/**
 * 周对比DTO
 */
public class WeeklyComparisonDTO {
  private ComparisonItem posts;
  private ComparisonItem dau;
  private ComparisonItem interactions;

  public ComparisonItem getPosts() {
    return posts;
  }

  public void setPosts(ComparisonItem posts) {
    this.posts = posts;
  }

  public ComparisonItem getDau() {
    return dau;
  }

  public void setDau(ComparisonItem dau) {
    this.dau = dau;
  }

  public ComparisonItem getInteractions() {
    return interactions;
  }

  public void setInteractions(ComparisonItem interactions) {
    this.interactions = interactions;
  }
}
