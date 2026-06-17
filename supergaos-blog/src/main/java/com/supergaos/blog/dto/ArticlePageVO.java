package com.supergaos.blog.dto;
import lombok.Data;
import java.util.List;
@Data
public class ArticlePageVO {
    private List<ArticleVO> articles;
    private int total;
    private int page;
    private int size;
}
