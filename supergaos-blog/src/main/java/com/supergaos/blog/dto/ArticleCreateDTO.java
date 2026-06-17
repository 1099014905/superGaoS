package com.supergaos.blog.dto;
import lombok.Data;
import java.util.List;
@Data
public class ArticleCreateDTO {
    private String title;
    private String content;
    private String summary;
    private Integer status;
    private List<Long> categoryIds;
    private List<Long> tagIds;
}
