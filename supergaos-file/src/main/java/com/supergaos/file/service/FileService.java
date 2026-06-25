package com.supergaos.file.service;

import com.supergaos.common.exception.BusinessException;
import com.supergaos.file.entity.FileRecord;
import com.supergaos.file.mapper.FileRecordMapper;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class FileService {
    private final MinioClient minioClient;
    private final FileRecordMapper fileRecordMapper;

    @Value("${minio.bucket}")
    private String bucket;

    public FileService(MinioClient minioClient, FileRecordMapper fileRecordMapper) {
        this.minioClient = minioClient;
        this.fileRecordMapper = fileRecordMapper;
    }

    public FileRecord upload(MultipartFile file, Long articleId) {
        try {
            String originalName = file.getOriginalFilename();
            String ext = originalName != null && originalName.contains(".")
                    ? originalName.substring(originalName.lastIndexOf(".")) : "";
            String storagePath = UUID.randomUUID() + ext;

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(storagePath)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            FileRecord record = new FileRecord();
            record.setOriginalName(originalName);
            record.setStoragePath(storagePath);
            record.setUrl("");
            record.setFileSize(file.getSize());
            record.setMimeType(file.getContentType());
            record.setArticleId(articleId);
            fileRecordMapper.insert(record);

            // Update URL with the generated ID — a relative path through the gateway
            String url = "/api/file/" + record.getId() + "/download";
            record.setUrl(url);
            fileRecordMapper.updateUrlById(record.getId(), url);

            return record;
        } catch (Exception e) {
            throw new BusinessException(4001, "文件上传失败: " + e.getMessage());
        }
    }

    public void delete(Long id) {
        FileRecord record = fileRecordMapper.findById(id);
        if (record == null) throw BusinessException.notFound(4, "文件");
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(record.getStoragePath())
                    .build());
        } catch (Exception ignored) {
        }
        fileRecordMapper.deleteById(id);
    }

    public ResponseEntity<InputStreamResource> download(Long id, String rangeHeader) {
        FileRecord record = fileRecordMapper.findById(id);
        if (record == null) throw BusinessException.notFound(4, "文件");
        try {
            // Get file info from MinIO for content length
            var stat = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(record.getStoragePath())
                    .build());
            long fileSize = stat.size();

            // Default: return full file (HTTP 200)
            long start = 0;
            long end = fileSize - 1;
            HttpStatus status = HttpStatus.OK;

            // Parse Range header for video seeking support
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                String range = rangeHeader.substring("bytes=".length()).trim();
                int dashIdx = range.indexOf('-');
                if (dashIdx > 0) {
                    start = Long.parseLong(range.substring(0, dashIdx));
                    if (dashIdx + 1 < range.length()) {
                        end = Long.parseLong(range.substring(dashIdx + 1));
                    }
                } else if (dashIdx == 0) {
                    // suffix range: bytes=-X → last X bytes
                    long suffix = Long.parseLong(range.substring(1));
                    start = Math.max(0, fileSize - suffix);
                }
                if (start >= fileSize) {
                    return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                            .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize)
                            .build();
                }
                if (end >= fileSize) end = fileSize - 1;
                status = HttpStatus.PARTIAL_CONTENT;
            }

            long contentLength = end - start + 1;
            GetObjectResponse minioResponse = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(record.getStoragePath())
                            .offset(start)
                            .length(contentLength)
                            .build());

            return ResponseEntity.status(status)
                    .contentType(MediaType.parseMediaType(record.getMimeType()))
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileSize)
                    .contentLength(contentLength)
                    .header("Content-Disposition", "inline; filename=\"" + record.getOriginalName() + "\"")
                    .body(new InputStreamResource(minioResponse));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(4002, "文件下载失败: " + e.getMessage());
        }
    }
}
