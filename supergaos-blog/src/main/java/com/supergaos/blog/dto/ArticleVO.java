package com.supergaos.blog.dto;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ArticleVO {
    private Long id;
    private String title;
    private String summary;
    private String content;
    private Integer status;
    private List<String> categories;
    private List<String> tags;
    private Integer commentCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
