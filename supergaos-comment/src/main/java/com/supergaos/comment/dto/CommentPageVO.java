package com.supergaos.comment.dto;

import lombok.Data;
import java.util.List;

@Data
public class CommentPageVO {
    private List<CommentVO> records;
    private long total;
    private int page;
    private int size;
}
