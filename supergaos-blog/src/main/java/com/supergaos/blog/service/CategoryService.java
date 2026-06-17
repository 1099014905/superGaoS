package com.supergaos.blog.service;
import com.supergaos.blog.entity.Category;
import com.supergaos.blog.mapper.CategoryMapper;
import org.springframework.stereotype.Service;
import java.util.List;
@Service
public class CategoryService {
    private final CategoryMapper categoryMapper;
    public CategoryService(CategoryMapper categoryMapper) { this.categoryMapper = categoryMapper; }
    public List<Category> findAll() { return categoryMapper.findAll(); }
    public Category create(Category category) { categoryMapper.insert(category); return category; }
}
