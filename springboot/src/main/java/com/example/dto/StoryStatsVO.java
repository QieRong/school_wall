package com.example.dto;

/**
 * 故事统计数据VO
 */
public class StoryStatsVO {

  private Long totalStories;
  private Long todayNewStories;
  private Long totalParagraphs;
  private Long todayNewParagraphs;
  private Long activeUsers; // 7天内有续写的用户数
  private Long ongoingStories;
  private Long finishedStories;
  private Long archivedStories;

  // ========== Getter/Setter ==========

  public Long getTotalStories() {
    return totalStories;
  }

  public void setTotalStories(Long totalStories) {
    this.totalStories = totalStories;
  }

  public Long getTodayNewStories() {
    return todayNewStories;
  }

  public void setTodayNewStories(Long todayNewStories) {
    this.todayNewStories = todayNewStories;
  }

  public Long getTotalParagraphs() {
    return totalParagraphs;
  }

  public void setTotalParagraphs(Long totalParagraphs) {
    this.totalParagraphs = totalParagraphs;
  }

  public Long getTodayNewParagraphs() {
    return todayNewParagraphs;
  }

  public void setTodayNewParagraphs(Long todayNewParagraphs) {
    this.todayNewParagraphs = todayNewParagraphs;
  }

  public Long getActiveUsers() {
    return activeUsers;
  }

  public void setActiveUsers(Long activeUsers) {
    this.activeUsers = activeUsers;
  }

  public Long getOngoingStories() {
    return ongoingStories;
  }

  public void setOngoingStories(Long ongoingStories) {
    this.ongoingStories = ongoingStories;
  }

  public Long getFinishedStories() {
    return finishedStories;
  }

  public void setFinishedStories(Long finishedStories) {
    this.finishedStories = finishedStories;
  }

  public Long getArchivedStories() {
    return archivedStories;
  }

  public void setArchivedStories(Long archivedStories) {
    this.archivedStories = archivedStories;
  }
}
