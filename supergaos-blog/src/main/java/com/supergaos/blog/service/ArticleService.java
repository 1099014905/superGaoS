package com.supergaos.blog.service;

import com.supergaos.blog.dto.ArticleCreateDTO;
import com.supergaos.blog.dto.ArticlePageVO;
import com.supergaos.blog.dto.ArticleVO;
import com.supergaos.blog.entity.*;
import com.supergaos.blog.feign.CommentClient;
import com.supergaos.blog.mapper.*;
import com.supergaos.common.exception.BusinessException;
import com.supergaos.common.result.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ArticleService {
    private final ArticleMapper articleMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final CommentClient commentClient;

    public ArticleService(ArticleMapper articleMapper, CategoryMapper categoryMapper,
                          TagMapper tagMapper, CommentClient commentClient) {
        this.articleMapper = articleMapper;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.commentClient = commentClient;
    }

    public ArticlePageVO list(Integer status, int page, int size) {
        ArticlePageVO result = new ArticlePageVO();
        result.setTotal(articleMapper.countAll(status));
        result.setPage(page);
        result.setSize(size);

        List<Article> articles = articleMapper.findAll(status, (page - 1) * size, size);
        result.setArticles(articles.stream().map(a -> {
            ArticleVO vo = toVO(a);
            vo.setCategories(categoryMapper.findByArticleId(a.getId()));
            vo.setTags(tagMapper.findByArticleId(a.getId()));
            try {
                Result<Integer> countResult = commentClient.getCommentCount(a.getId());
                if (countResult != null && countResult.getData() != null) {
                    vo.setCommentCount(countResult.getData());
                }
            } catch (Exception e) {
                vo.setCommentCount(0);
            }
            return vo;
        }).collect(Collectors.toList()));
        return result;
    }

    public ArticleVO getById(Long id) {
        Article article = articleMapper.findById(id);
        if (article == null) throw BusinessException.notFound(2, "文章");
        ArticleVO vo = toVO(article);
        vo.setCategories(categoryMapper.findByArticleId(id));
        vo.setTags(tagMapper.findByArticleId(id));
        try {
            Result<Integer> countResult = commentClient.getCommentCount(id);
            if (countResult != null && countResult.getData() != null) {
                vo.setCommentCount(countResult.getData());
            }
        } catch (Exception e) {
            vo.setCommentCount(0);
        }
        return vo;
    }

    @Transactional
    public ArticleVO create(ArticleCreateDTO dto) {
        Article article = new Article();
        article.setTitle(dto.getTitle());
        article.setContent(dto.getContent());
        article.setSummary(dto.getSummary());
        article.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        articleMapper.insert(article);

        if (dto.getCategoryIds() != null) {
            dto.getCategoryIds().forEach(cid -> articleMapper.insertArticleCategory(article.getId(), cid));
        }
        if (dto.getTagIds() != null) {
            dto.getTagIds().forEach(tid -> articleMapper.insertArticleTag(article.getId(), tid));
        }
        return getById(article.getId());
    }

    @Transactional
    public ArticleVO update(Long id, ArticleCreateDTO dto) {
        Article article = articleMapper.findById(id);
        if (article == null) throw BusinessException.notFound(2, "文章");
        article.setTitle(dto.getTitle());
        article.setContent(dto.getContent());
        article.setSummary(dto.getSummary());
        article.setStatus(dto.getStatus() != null ? dto.getStatus() : article.getStatus());
        articleMapper.update(article);

        articleMapper.deleteArticleCategories(id);
        articleMapper.deleteArticleTags(id);
        if (dto.getCategoryIds() != null) {
            dto.getCategoryIds().forEach(cid -> articleMapper.insertArticleCategory(id, cid));
        }
        if (dto.getTagIds() != null) {
            dto.getTagIds().forEach(tid -> articleMapper.insertArticleTag(id, tid));
        }
        return getById(id);
    }

    @Transactional
    public void delete(Long id) {
        Article article = articleMapper.findById(id);
        if (article == null) throw BusinessException.notFound(2, "文章");
        articleMapper.deleteArticleCategories(id);
        articleMapper.deleteArticleTags(id);
        articleMapper.deleteById(id);
    }

    private ArticleVO toVO(Article a) {
        ArticleVO vo = new ArticleVO();
        org.springframework.beans.BeanUtils.copyProperties(a, vo);
        return vo;
    }
}
