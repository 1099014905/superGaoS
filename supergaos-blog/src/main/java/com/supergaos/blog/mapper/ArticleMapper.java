package com.supergaos.blog.mapper;
import com.supergaos.blog.entity.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
@Mapper
public interface ArticleMapper {
    List<Article> findAll(@Param("status") Integer status,
                          @Param("offset") int offset,
                          @Param("limit") int limit);
    int countAll(@Param("status") Integer status);
    Article findById(@Param("id") Long id);
    void insert(Article article);
    void update(Article article);
    void deleteById(@Param("id") Long id);
    void insertArticleCategory(@Param("articleId") Long articleId, @Param("categoryId") Long categoryId);
    void insertArticleTag(@Param("articleId") Long articleId, @Param("tagId") Long tagId);
    void deleteArticleCategories(@Param("articleId") Long articleId);
    void deleteArticleTags(@Param("articleId") Long articleId);
}
