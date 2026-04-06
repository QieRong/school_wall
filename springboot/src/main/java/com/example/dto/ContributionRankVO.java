package com.example.dto;

/**
 * 贡献度排行VO
 */
public class ContributionRankVO {

  private Integer rank;
  private Long userId;
  private String nickname;
  private String avatar;
  private Integer points;
  private Integer paragraphCount;
  private Integer likeReceived;
  private Integer keyPointCount;

  // ========== Getter/Setter ==========

  public Integer getRank() {
    return rank;
  }

  public void setRank(Integer rank) {
    this.rank = rank;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getNickname() {
    return nickname;
  }

  public void setNickname(String nickname) {
    this.nickname = nickname;
  }

  public String getAvatar() {
    return avatar;
  }

  public void setAvatar(String avatar) {
    this.avatar = avatar;
  }

  public Integer getPoints() {
    return points;
  }

  public void setPoints(Integer points) {
    this.points = points;
  }

  public Integer getParagraphCount() {
    return paragraphCount;
  }

  public void setParagraphCount(Integer paragraphCount) {
    this.paragraphCount = paragraphCount;
  }

  public Integer getLikeReceived() {
    return likeReceived;
  }

  public void setLikeReceived(Integer likeReceived) {
    this.likeReceived = likeReceived;
  }

  public Integer getKeyPointCount() {
    return keyPointCount;
  }

  public void setKeyPointCount(Integer keyPointCount) {
    this.keyPointCount = keyPointCount;
  }
}
