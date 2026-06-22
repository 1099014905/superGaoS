package com.supergaos.comment.service;

import com.supergaos.comment.dto.CommentPageVO;
import com.supergaos.comment.dto.CommentVO;
import com.supergaos.comment.entity.Comment;
import com.supergaos.comment.mapper.CommentMapper;
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
class CommentServiceTest {

    @Mock
    private CommentMapper commentMapper;

    @InjectMocks
    private CommentService commentService;

    @Test
    void getComments_shouldReturnPagedResults() {
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setArticleId(10L);
        comment.setContent("Great article!");
        comment.setNickname("Alice");

        when(commentMapper.findByArticleId(10L, 0, 10)).thenReturn(List.of(comment));
        when(commentMapper.countByArticleId(10L)).thenReturn(1);

        CommentPageVO result = commentService.getComments(10L, 1, 10);

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getPage());
        assertEquals(10, result.getSize());
        assertEquals(1, result.getRecords().size());

        CommentVO vo = result.getRecords().get(0);
        assertEquals("Great article!", vo.getContent());
        assertEquals("Alice", vo.getNickname());
    }

    @Test
    void getComments_withSecondPage_shouldCalculateCorrectOffset() {
        when(commentMapper.findByArticleId(10L, 10, 10)).thenReturn(List.of());
        when(commentMapper.countByArticleId(10L)).thenReturn(5);

        CommentPageVO result = commentService.getComments(10L, 2, 10);

        assertEquals(5, result.getTotal());
        assertEquals(2, result.getPage());
        assertEquals(0, result.getRecords().size());

        verify(commentMapper).findByArticleId(10L, 10, 10);
    }

    @Test
    void getComments_withNoComments_shouldReturnEmptyPage() {
        when(commentMapper.findByArticleId(10L, 0, 10)).thenReturn(List.of());
        when(commentMapper.countByArticleId(10L)).thenReturn(0);

        CommentPageVO result = commentService.getComments(10L, 1, 10);

        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }

    @Test
    void addComment_shouldInsertAndReturnVO() {
        Comment comment = new Comment();
        comment.setArticleId(10L);
        comment.setContent("Nice post!");
        comment.setNickname("Bob");

        CommentVO vo = commentService.addComment(comment);

        assertNotNull(vo);
        assertEquals("Nice post!", vo.getContent());
        assertEquals("Bob", vo.getNickname());
        verify(commentMapper).insert(comment);
    }

    @Test
    void deleteComment_shouldCallMapper() {
        commentService.deleteComment(5L);
        verify(commentMapper).deleteById(5L);
    }

    @Test
    void getCommentCount_shouldReturnTotalFromMapper() {
        when(commentMapper.countByArticleId(10L)).thenReturn(7);

        int count = commentService.getCommentCount(10L);

        assertEquals(7, count);
    }

    @Test
    void getCommentCount_withNoComments_shouldReturnZero() {
        when(commentMapper.countByArticleId(10L)).thenReturn(0);

        int count = commentService.getCommentCount(10L);

        assertEquals(0, count);
    }

    @Test
    void getComments_shouldCopyAllPropertiesToVO() {
        Comment comment = new Comment();
        comment.setId(99L);
        comment.setArticleId(10L);
        comment.setParentId(5L);
        comment.setNickname("Charlie");
        comment.setEmail("charlie@test.com");
        comment.setContent("Well written!");
        comment.setStatus(1);

        when(commentMapper.findByArticleId(10L, 0, 20)).thenReturn(List.of(comment));
        when(commentMapper.countByArticleId(10L)).thenReturn(1);

        CommentPageVO result = commentService.getComments(10L, 1, 20);
        CommentVO vo = result.getRecords().get(0);

        assertEquals(99L, vo.getId());
        assertEquals(10L, vo.getArticleId());
        assertEquals(5L, vo.getParentId());
        assertEquals("Charlie", vo.getNickname());
        assertEquals("charlie@test.com", vo.getEmail());
        assertEquals("Well written!", vo.getContent());
        assertEquals(1, vo.getStatus());
    }
}
