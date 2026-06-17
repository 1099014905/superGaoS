package com.supergaos.comment.service;

import com.supergaos.comment.dto.CommentPageVO;
import com.supergaos.comment.dto.CommentVO;
import com.supergaos.comment.entity.Comment;
import com.supergaos.comment.mapper.CommentMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {
    private final CommentMapper commentMapper;

    public CommentService(CommentMapper commentMapper) {
        this.commentMapper = commentMapper;
    }

    public CommentPageVO getComments(Long articleId, int page, int size) {
        int offset = (page - 1) * size;
        List<Comment> list = commentMapper.findByArticleId(articleId, offset, size);
        int total = commentMapper.countByArticleId(articleId);

        List<CommentVO> records = list.stream().map(this::toVO).collect(Collectors.toList());

        CommentPageVO pageVO = new CommentPageVO();
        pageVO.setRecords(records);
        pageVO.setTotal(total);
        pageVO.setPage(page);
        pageVO.setSize(size);
        return pageVO;
    }

    public int getCommentCount(Long articleId) {
        return commentMapper.countByArticleId(articleId);
    }

    public CommentVO addComment(Comment comment) {
        commentMapper.insert(comment);
        return toVO(comment);
    }

    public void deleteComment(Long id) {
        commentMapper.deleteById(id);
    }

    private CommentVO toVO(Comment c) {
        CommentVO vo = new CommentVO();
        BeanUtils.copyProperties(c, vo);
        return vo;
    }
}
