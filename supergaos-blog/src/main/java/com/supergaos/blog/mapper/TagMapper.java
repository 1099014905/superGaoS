package com.supergaos.blog.mapper;
import com.supergaos.blog.entity.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
@Mapper
public interface TagMapper {
    List<Tag> findAll();
    Tag findById(@Param("id") Long id);
    void insert(Tag tag);
    List<String> findByArticleId(@Param("articleId") Long articleId);
}
