package com.example.entity;

public class Banner {
    private Long id;
    private String imageUrl;
    private String title;
    private Integer sort;
    // Getter & Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
}