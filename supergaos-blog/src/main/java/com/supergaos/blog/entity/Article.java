package com.supergaos.blog.entity;
import lombok.Data;
import java.time.LocalDateTime;
@Data
public class Article {
    private Long id;
    private String title;
    private String content;
    private String summary;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
