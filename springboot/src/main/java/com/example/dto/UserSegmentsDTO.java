package com.example.dto;

/**
 * 用户分层DTO
 */
public class UserSegmentsDTO {
  private Integer creators; // 内容创作者
  private Integer interactors; // 高频互动者
  private Integer lurkers; // 普通用户

  public Integer getCreators() {
    return creators;
  }

  public void setCreators(Integer creators) {
    this.creators = creators;
  }

  public Integer getInteractors() {
    return interactors;
  }

  public void setInteractors(Integer interactors) {
    this.interactors = interactors;
  }

  public Integer getLurkers() {
    return lurkers;
  }

  public void setLurkers(Integer lurkers) {
    this.lurkers = lurkers;
  }
}
