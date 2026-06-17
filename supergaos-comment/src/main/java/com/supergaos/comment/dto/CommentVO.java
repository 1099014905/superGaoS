package com.supergaos.comment.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CommentVO {
    private Long id;
    private Long articleId;
    private Long parentId;
    private String nickname;
    private String email;
    private String content;
    private LocalDateTime createTime;
}
