package com.supergaos.comment.mapper;

import com.supergaos.comment.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface CommentMapper {
    List<Comment> findByArticleId(@Param("articleId") Long articleId,
                                  @Param("offset") int offset,
                                  @Param("limit") int limit);
    int countByArticleId(@Param("articleId") Long articleId);
    void insert(Comment comment);
    void deleteById(@Param("id") Long id);
}
