package com.supergaos.blog.feign;

import com.supergaos.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "supergaos-comment")
public interface CommentClient {
    @GetMapping("/api/comment/articles/{articleId}/count")
    Result<Integer> getCommentCount(@PathVariable("articleId") Long articleId);
}
