package com.supergaos.file.service;

import com.supergaos.common.exception.BusinessException;
import com.supergaos.file.entity.FileRecord;
import com.supergaos.file.mapper.FileRecordMapper;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class FileService {
    private final MinioClient minioClient;
    private final FileRecordMapper fileRecordMapper;

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${minio.public-endpoint}")
    private String publicEndpoint;

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

            String url = publicEndpoint + "/" + bucket + "/" + storagePath;

            FileRecord record = new FileRecord();
            record.setOriginalName(originalName);
            record.setStoragePath(storagePath);
            record.setUrl(url);
            record.setFileSize(file.getSize());
            record.setMimeType(file.getContentType());
            record.setArticleId(articleId);
            fileRecordMapper.insert(record);
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
}
