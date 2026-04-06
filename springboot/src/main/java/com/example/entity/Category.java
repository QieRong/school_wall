package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_category")
public class Category {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String icon;     // 前端图标组件名
    private String color;    // 文字颜色类名
    private String bgColor;  // 背景颜色类名
    private Integer sort;    // 排序
    private Integer status;  // 1启用 0停用
    private LocalDateTime createTime;
}