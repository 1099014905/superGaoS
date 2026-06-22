package com.supergaos.blog.service;

import com.supergaos.blog.dto.ArticleCreateDTO;
import com.supergaos.blog.dto.ArticlePageVO;
import com.supergaos.blog.dto.ArticleVO;
import com.supergaos.blog.entity.Article;
import com.supergaos.blog.feign.CommentClient;
import com.supergaos.blog.mapper.ArticleMapper;
import com.supergaos.blog.mapper.CategoryMapper;
import com.supergaos.blog.mapper.TagMapper;
import com.supergaos.common.exception.BusinessException;
import com.supergaos.common.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private TagMapper tagMapper;

    @Mock
    private CommentClient commentClient;

    @InjectMocks
    private ArticleService articleService;

    private Article article;
    private ArticleCreateDTO createDTO;

    @BeforeEach
    void setUp() {
        article = new Article();
        article.setId(1L);
        article.setTitle("Test Title");
        article.setContent("Test Content");
        article.setSummary("Test Summary");
        article.setStatus(1);

        createDTO = new ArticleCreateDTO();
        createDTO.setTitle("New Article");
        createDTO.setContent("New Content");
        createDTO.setSummary("New Summary");
        createDTO.setStatus(1);
        createDTO.setCategoryIds(List.of(10L, 20L));
        createDTO.setTagIds(List.of(100L, 200L));
    }

    @Test
    void create_shouldInsertArticleCategoriesAndTags() {
        doAnswer(inv -> {
            Article a = inv.getArgument(0);
            a.setId(1L);
            return null;
        }).when(articleMapper).insert(any(Article.class));

        when(articleMapper.findById(1L)).thenReturn(article);
        when(categoryMapper.findByArticleId(1L)).thenReturn(List.of("Tech"));
        when(tagMapper.findByArticleId(1L)).thenReturn(List.of("Java"));
        when(commentClient.getCommentCount(1L)).thenReturn(Result.success(0));

        ArticleVO result = articleService.create(createDTO);

        assertEquals("New Article", result.getTitle());
        assertEquals("New Content", result.getContent());

        verify(articleMapper).insert(any(Article.class));
        verify(articleMapper).insertArticleCategory(1L, 10L);
        verify(articleMapper).insertArticleCategory(1L, 20L);
        verify(articleMapper).insertArticleTag(1L, 100L);
        verify(articleMapper).insertArticleTag(1L, 200L);
    }

    @Test
    void create_withoutCategoriesOrTags_shouldInsertOnlyArticle() {
        ArticleCreateDTO dto = new ArticleCreateDTO();
        dto.setTitle("Minimal");
        dto.setContent("Just content");
        dto.setSummary("Just summary");

        doAnswer(inv -> {
            Article a = inv.getArgument(0);
            a.setId(2L);
            return null;
        }).when(articleMapper).insert(any(Article.class));

        when(articleMapper.findById(2L)).thenReturn(article);
        when(categoryMapper.findByArticleId(2L)).thenReturn(List.of());
        when(tagMapper.findByArticleId(2L)).thenReturn(List.of());
        when(commentClient.getCommentCount(2L)).thenReturn(Result.success(0));

        articleService.create(dto);

        verify(articleMapper, never()).insertArticleCategory(anyLong(), anyLong());
        verify(articleMapper, never()).insertArticleTag(anyLong(), anyLong());
    }

    @Test
    void getById_whenExists_shouldReturnVOWithCategoriesTagsAndCommentCount() {
        when(articleMapper.findById(1L)).thenReturn(article);
        when(categoryMapper.findByArticleId(1L)).thenReturn(List.of("Tech", "Programming"));
        when(tagMapper.findByArticleId(1L)).thenReturn(List.of("Java", "Spring"));
        when(commentClient.getCommentCount(1L)).thenReturn(Result.success(5));

        ArticleVO result = articleService.getById(1L);

        assertEquals("Test Title", result.getTitle());
        assertEquals(2, result.getCategories().size());
        assertEquals(2, result.getTags().size());
        assertEquals(5, result.getCommentCount());
    }

    @Test
    void getById_whenNotExists_shouldThrowNotFound() {
        when(articleMapper.findById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> articleService.getById(999L));
        assertEquals(2001, ex.getErrorCode());
        assertEquals("文章不存在", ex.getMessage());
    }

    @Test
    void getById_whenFeignFails_shouldSetCommentCountToZero() {
        when(articleMapper.findById(1L)).thenReturn(article);
        when(categoryMapper.findByArticleId(1L)).thenReturn(List.of());
        when(tagMapper.findByArticleId(1L)).thenReturn(List.of());
        when(commentClient.getCommentCount(1L)).thenThrow(new RuntimeException("Feign timeout"));

        ArticleVO result = articleService.getById(1L);

        assertEquals(0, result.getCommentCount());
    }

    @Test
    void list_shouldReturnPagedResultsWithCommentCount() {
        when(articleMapper.countAll(1)).thenReturn(1);
        when(articleMapper.findAll(1, 0, 10)).thenReturn(List.of(article));
        when(categoryMapper.findByArticleId(1L)).thenReturn(List.of("Tech"));
        when(tagMapper.findByArticleId(1L)).thenReturn(List.of("Java"));
        when(commentClient.getCommentCount(1L)).thenReturn(Result.success(3));

        ArticlePageVO result = articleService.list(1, 1, 10);

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getPage());
        assertEquals(10, result.getSize());
        assertEquals(1, result.getArticles().size());

        ArticleVO vo = result.getArticles().get(0);
        assertEquals("Test Title", vo.getTitle());
        assertEquals(3, vo.getCommentCount());
    }

    @Test
    void list_withPage2_shouldCalculateCorrectOffset() {
        when(articleMapper.countAll(1)).thenReturn(15);
        when(articleMapper.findAll(1, 10, 10)).thenReturn(List.of());

        ArticlePageVO result = articleService.list(1, 2, 10);

        assertEquals(15, result.getTotal());
        assertEquals(2, result.getPage());
        assertEquals(0, result.getArticles().size());
        verify(articleMapper).findAll(1, 10, 10);
    }

    @Test
    void update_shouldModifyArticleAndRecreateAssociations() {
        ArticleCreateDTO updateDTO = new ArticleCreateDTO();
        updateDTO.setTitle("Updated Title");
        updateDTO.setContent("Updated Content");
        updateDTO.setSummary("Updated Summary");
        updateDTO.setStatus(2);
        updateDTO.setCategoryIds(List.of(30L));
        updateDTO.setTagIds(List.of(300L));

        when(articleMapper.findById(1L)).thenReturn(article);
        when(articleMapper.findById(1L)).thenReturn(article);
        when(categoryMapper.findByArticleId(1L)).thenReturn(List.of("Updated"));
        when(tagMapper.findByArticleId(1L)).thenReturn(List.of("UpdatedTag"));
        when(commentClient.getCommentCount(1L)).thenReturn(Result.success(0));

        articleService.update(1L, updateDTO);

        verify(articleMapper).update(argThat(a -> {
            assertEquals("Updated Title", a.getTitle());
            assertEquals("Updated Content", a.getContent());
            assertEquals(2, a.getStatus());
            return true;
        }));
        verify(articleMapper).deleteArticleCategories(1L);
        verify(articleMapper).deleteArticleTags(1L);
        verify(articleMapper).insertArticleCategory(1L, 30L);
        verify(articleMapper).insertArticleTag(1L, 300L);
    }

    @Test
    void update_withNullStatus_shouldKeepOriginalStatus() {
        ArticleCreateDTO dto = new ArticleCreateDTO();
        dto.setTitle("Only Title");
        dto.setContent("Only Content");
        dto.setSummary("Only Summary");

        when(articleMapper.findById(1L)).thenReturn(article);
        when(articleMapper.findById(1L)).thenReturn(article);
        when(categoryMapper.findByArticleId(1L)).thenReturn(List.of());
        when(tagMapper.findByArticleId(1L)).thenReturn(List.of());
        when(commentClient.getCommentCount(1L)).thenReturn(Result.success(0));

        articleService.update(1L, dto);

        verify(articleMapper).update(argThat(a -> a.getStatus() == 1)); // Keep original
    }

    @Test
    void update_whenNotExists_shouldThrowNotFound() {
        when(articleMapper.findById(999L)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> articleService.update(999L, new ArticleCreateDTO()));
    }

    @Test
    void delete_shouldRemoveAssociationsThenArticle() {
        when(articleMapper.findById(1L)).thenReturn(article);

        articleService.delete(1L);

        verify(articleMapper).deleteArticleCategories(1L);
        verify(articleMapper).deleteArticleTags(1L);
        verify(articleMapper).deleteById(1L);
    }

    @Test
    void delete_whenNotExists_shouldThrowNotFound() {
        when(articleMapper.findById(999L)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> articleService.delete(999L));
    }
}
