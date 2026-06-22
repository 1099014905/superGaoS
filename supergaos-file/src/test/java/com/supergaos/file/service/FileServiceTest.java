package com.supergaos.file.service;

import com.supergaos.common.exception.BusinessException;
import com.supergaos.file.entity.FileRecord;
import com.supergaos.file.mapper.FileRecordMapper;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private MinioClient minioClient;

    @Mock
    private FileRecordMapper fileRecordMapper;

    @InjectMocks
    private FileService fileService;

    @BeforeEach
    void setUp() {
    }

    // ===== upload tests =====

    @Test
    void upload_shouldStoreToMinioAndSaveRecord(@TempDir Path tempDir) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test-image.png", "image/png", "fake-image-content".getBytes());

        // Mock the insert so it sets the ID (useGeneratedKeys = true)
        when(fileRecordMapper.findById(any())).thenReturn(null);
        doAnswer(invocation -> {
            FileRecord record = invocation.getArgument(0);
            record.setId(100L); // Simulate auto-generated key
            return null;
        }).when(fileRecordMapper).insert(any(FileRecord.class));

        FileRecord result = fileService.upload(file, 42L);

        // Verify MinIO upload happened
        ArgumentCaptor<PutObjectArgs> putCaptor = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minioClient).putObject(putCaptor.capture());

        PutObjectArgs putArgs = putCaptor.getValue();
        assertEquals("supergaos-images", putArgs.bucket());
        assertEquals("image/png", putArgs.contentType());

        // Verify DB record was inserted then URL updated
        verify(fileRecordMapper).insert(any(FileRecord.class));
        verify(fileRecordMapper).updateUrlById(eq(100L), eq("/api/file/100/download"));

        // Verify returned record
        assertNotNull(result);
        assertEquals("test-image.png", result.getOriginalName());
        assertEquals("/api/file/100/download", result.getUrl());
        assertEquals("image/png", result.getMimeType());
        assertEquals(42L, result.getArticleId());
    }

    @Test
    void upload_withoutArticleId_shouldStoreWithNullArticle(@TempDir Path tempDir) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "pdf-content".getBytes());

        doAnswer(invocation -> {
            FileRecord record = invocation.getArgument(0);
            record.setId(200L);
            return null;
        }).when(fileRecordMapper).insert(any(FileRecord.class));

        FileRecord result = fileService.upload(file, null);

        assertEquals("doc.pdf", result.getOriginalName());
        assertEquals("/api/file/200/download", result.getUrl());
        assertNull(result.getArticleId());
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    void upload_withoutExtension_shouldGenerateStoragePathWithoutExtension(@TempDir Path tempDir) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "noext", "text/plain", "data".getBytes());

        doAnswer(invocation -> {
            FileRecord record = invocation.getArgument(0);
            record.setId(300L);
            return null;
        }).when(fileRecordMapper).insert(any(FileRecord.class));

        FileRecord result = fileService.upload(file, null);

        assertFalse(result.getStoragePath().contains("."),
                "Storage path should not contain extension if original has none");
    }

    @Test
    void upload_whenMinioFails_shouldThrowBusinessException(@TempDir Path tempDir) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "fail.txt", "text/plain", "data".getBytes());

        when(minioClient.putObject(any(PutObjectArgs.class)))
                .thenThrow(new RuntimeException("MinIO connection refused"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> fileService.upload(file, null));
        assertEquals(4001, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("MinIO connection refused"));

        // DB insert should NOT happen if MinIO fails
        verify(fileRecordMapper, never()).insert(any());
    }

    // ===== download tests =====

    @Test
    void download_withExistingFile_shouldReturnStreamResponse() throws Exception {
        FileRecord record = new FileRecord();
        record.setId(1L);
        record.setOriginalName("photo.jpg");
        record.setStoragePath("uuid-photo.jpg");
        record.setMimeType("image/jpeg");

        when(fileRecordMapper.findById(1L)).thenReturn(record);
        when(minioClient.getObject(any(GetObjectArgs.class)))
                .thenReturn(new GetObjectResponse(null, null, null, "photo.jpg",
                        new ByteArrayInputStream("image-data".getBytes())));

        ResponseEntity<InputStreamResource> response = fileService.download(1L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(MediaType.parseMediaType("image/jpeg"), response.getHeaders().getContentType());
        assertEquals("inline; filename=\"photo.jpg\"",
                response.getHeaders().getFirst("Content-Disposition"));

        // Verify MinIO was called with correct args
        ArgumentCaptor<GetObjectArgs> getCaptor = ArgumentCaptor.forClass(GetObjectArgs.class);
        verify(minioClient).getObject(getCaptor.capture());
        assertEquals("supergaos-images", getCaptor.getValue().bucket());
        assertEquals("uuid-photo.jpg", getCaptor.getValue().object());
    }

    @Test
    void download_withNonExistentFile_shouldThrowNotFoundException() {
        when(fileRecordMapper.findById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> fileService.download(999L));
        assertEquals(4001, ex.getErrorCode());
        assertEquals("文件不存在", ex.getMessage());
    }

    @Test
    void download_whenMinioFails_shouldThrowBusinessException() throws Exception {
        FileRecord record = new FileRecord();
        record.setId(1L);
        record.setStoragePath("uuid-file");

        when(fileRecordMapper.findById(1L)).thenReturn(record);
        when(minioClient.getObject(any(GetObjectArgs.class)))
                .thenThrow(new RuntimeException("MinIO read failed"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> fileService.download(1L));
        assertEquals(4002, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("MinIO read failed"));
    }

    // ===== delete tests =====

    @Test
    void delete_withExistingFile_shouldRemoveFromMinioAndDb() throws Exception {
        FileRecord record = new FileRecord();
        record.setId(1L);
        record.setStoragePath("uuid-to-delete");

        when(fileRecordMapper.findById(1L)).thenReturn(record);

        fileService.delete(1L);

        // Verify MinIO deletion
        ArgumentCaptor<RemoveObjectArgs> removeCaptor = ArgumentCaptor.forClass(RemoveObjectArgs.class);
        verify(minioClient).removeObject(removeCaptor.capture());
        assertEquals("supergaos-images", removeCaptor.getValue().bucket());
        assertEquals("uuid-to-delete", removeCaptor.getValue().object());

        // Verify DB deletion
        verify(fileRecordMapper).deleteById(1L);
    }

    @Test
    void delete_withNonExistentFile_shouldThrowNotFoundException() {
        when(fileRecordMapper.findById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> fileService.delete(999L));
        assertEquals(4001, ex.getErrorCode());
    }

    @Test
    void delete_whenMinioRemoveFails_shouldStillDeleteFromDb() throws Exception {
        FileRecord record = new FileRecord();
        record.setId(1L);
        record.setStoragePath("uuid-file");

        when(fileRecordMapper.findById(1L)).thenReturn(record);
        when(minioClient.removeObject(any(RemoveObjectArgs.class)))
                .thenThrow(new RuntimeException("MinIO remove failed"));

        // Should not propagate MinIO error — delete is best-effort
        assertDoesNotThrow(() -> fileService.delete(1L));

        // Should still delete from DB
        verify(fileRecordMapper).deleteById(1L);
    }

    // ===== upload file extension test =====

    @Test
    void upload_shouldPreserveOriginalExtension(@TempDir Path tempDir) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "report.tar.gz", "application/gzip", "data".getBytes());

        doAnswer(invocation -> {
            FileRecord record = invocation.getArgument(0);
            record.setId(400L);
            return null;
        }).when(fileRecordMapper).insert(any(FileRecord.class));

        FileRecord result = fileService.upload(file, null);

        // FileService splits on last "." so ext = ".gz", not ".tar.gz"
        assertTrue(result.getStoragePath().endsWith(".gz"));
        assertEquals("report.tar.gz", result.getOriginalName());
    }
}
