package com.supergaos.blog.service;
import com.supergaos.blog.entity.Tag;
import com.supergaos.blog.mapper.TagMapper;
import org.springframework.stereotype.Service;
import java.util.List;
@Service
public class TagService {
    private final TagMapper tagMapper;
    public TagService(TagMapper tagMapper) { this.tagMapper = tagMapper; }
    public List<Tag> findAll() { return tagMapper.findAll(); }
    public Tag create(Tag tag) { tagMapper.insert(tag); return tag; }
}
