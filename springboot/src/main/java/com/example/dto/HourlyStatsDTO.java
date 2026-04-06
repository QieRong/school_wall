package com.example.dto;

import java.util.List;

/**
 * 24小时统计DTO
 */
public class HourlyStatsDTO {
  private List<Integer> posts; // 24个小时的发帖数
  private List<Integer> comments; // 24个小时的评论数
  private Integer postPeakHour;
  private Integer commentPeakHour;

  public List<Integer> getPosts() {
    return posts;
  }

  public void setPosts(List<Integer> posts) {
    this.posts = posts;
  }

  public List<Integer> getComments() {
    return comments;
  }

  public void setComments(List<Integer> comments) {
    this.comments = comments;
  }

  public Integer getPostPeakHour() {
    return postPeakHour;
  }

  public void setPostPeakHour(Integer postPeakHour) {
    this.postPeakHour = postPeakHour;
  }

  public Integer getCommentPeakHour() {
    return commentPeakHour;
  }

  public void setCommentPeakHour(Integer commentPeakHour) {
    this.commentPeakHour = commentPeakHour;
  }
}
