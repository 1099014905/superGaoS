package com.supergaos.blog.mapper;
import com.supergaos.blog.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
@Mapper
public interface CategoryMapper {
    List<Category> findAll();
    Category findById(@Param("id") Long id);
    void insert(Category category);
    List<String> findByArticleId(@Param("articleId") Long articleId);
}
