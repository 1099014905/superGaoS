package com.supergaos.blog.controller;
import com.supergaos.blog.entity.Tag;
import com.supergaos.blog.service.TagService;
import com.supergaos.common.result.Result;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/blog/tags")
public class TagController {
    private final TagService tagService;
    public TagController(TagService tagService) { this.tagService = tagService; }
    @GetMapping
    public Result<List<Tag>> list() { return Result.success(tagService.findAll()); }
    @PostMapping
    public Result<Tag> create(@RequestBody Tag tag) { return Result.success(tagService.create(tag)); }
}
