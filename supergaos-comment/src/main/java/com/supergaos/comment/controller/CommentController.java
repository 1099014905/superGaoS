package com.supergaos.comment.controller;

import com.supergaos.common.result.Result;
import com.supergaos.comment.dto.CommentPageVO;
import com.supergaos.comment.dto.CommentVO;
import com.supergaos.comment.entity.Comment;
import com.supergaos.comment.service.CommentService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/comment")
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/articles/{articleId}")
    public Result<CommentPageVO> list(@PathVariable Long articleId,
                                       @RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        return Result.success(commentService.getComments(articleId, page, size));
    }

    @GetMapping("/articles/{articleId}/count")
    public Result<Integer> count(@PathVariable Long articleId) {
        return Result.success(commentService.getCommentCount(articleId));
    }

    @PostMapping("/articles/{articleId}")
    public Result<CommentVO> add(@PathVariable Long articleId, @RequestBody Comment comment) {
        comment.setArticleId(articleId);
        return Result.success(commentService.addComment(comment));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        commentService.deleteComment(id);
        return Result.success();
    }
}
