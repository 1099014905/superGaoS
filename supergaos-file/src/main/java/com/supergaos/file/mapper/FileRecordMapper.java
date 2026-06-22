package com.supergaos.file.mapper;

import com.supergaos.file.entity.FileRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FileRecordMapper {
    void insert(FileRecord record);

    FileRecord findById(@Param("id") Long id);

    void deleteById(@Param("id") Long id);

    void updateUrlById(@Param("id") Long id, @Param("url") String url);
}
