package com.supergaos.blog.controller;
import com.supergaos.blog.entity.Category;
import com.supergaos.blog.service.CategoryService;
import com.supergaos.common.result.Result;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/blog/categories")
public class CategoryController {
    private final CategoryService categoryService;
    public CategoryController(CategoryService categoryService) { this.categoryService = categoryService; }
    @GetMapping
    public Result<List<Category>> list() { return Result.success(categoryService.findAll()); }
    @PostMapping
    public Result<Category> create(@RequestBody Category category) { return Result.success(categoryService.create(category)); }
}
