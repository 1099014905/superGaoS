package com.supergaos.comment.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Comment {
    private Long id;
    private Long articleId;
    private Long parentId;
    private String nickname;
    private String email;
    private String content;
    private Integer status;
    private LocalDateTime createTime;
}
