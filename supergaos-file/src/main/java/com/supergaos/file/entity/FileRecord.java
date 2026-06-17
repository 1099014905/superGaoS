package com.supergaos.file.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FileRecord {
    private Long id;
    private String originalName;
    private String storagePath;
    private String url;
    private Long fileSize;
    private String mimeType;
    private Long articleId;
    private LocalDateTime createTime;
}
