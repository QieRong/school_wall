package com.example.dto;

/**
 * 点赞操作结果
 */
public class LikeResult {

  private Boolean liked; // true=点赞成功, false=取消点赞
  private Integer likeCount; // 当前点赞数

  public LikeResult() {
  }

  public LikeResult(Boolean liked, Integer likeCount) {
    this.liked = liked;
    this.likeCount = likeCount;
  }

  // ========== Getter/Setter ==========

  public Boolean getLiked() {
    return liked;
  }

  public void setLiked(Boolean liked) {
    this.liked = liked;
  }

  public Integer getLikeCount() {
    return likeCount;
  }

  public void setLikeCount(Integer likeCount) {
    this.likeCount = likeCount;
  }
}
