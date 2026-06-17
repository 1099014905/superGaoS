package com.supergaos.blog.controller;
import com.supergaos.blog.dto.ArticleCreateDTO;
import com.supergaos.blog.dto.ArticlePageVO;
import com.supergaos.blog.dto.ArticleVO;
import com.supergaos.blog.service.ArticleService;
import com.supergaos.common.result.Result;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/blog/articles")
public class ArticleController {
    private final ArticleService articleService;
    public ArticleController(ArticleService articleService) { this.articleService = articleService; }

    @GetMapping
    public Result<ArticlePageVO> list(@RequestParam(required = false) Integer status,
                                      @RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int size) {
        return Result.success(articleService.list(status, page, size));
    }

    @GetMapping("/{id}")
    public Result<ArticleVO> get(@PathVariable Long id) {
        return Result.success(articleService.getById(id));
    }

    @PostMapping
    public Result<ArticleVO> create(@RequestBody ArticleCreateDTO dto) {
        return Result.success(articleService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<ArticleVO> update(@PathVariable Long id, @RequestBody ArticleCreateDTO dto) {
        return Result.success(articleService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        articleService.delete(id);
        return Result.success();
    }
}
