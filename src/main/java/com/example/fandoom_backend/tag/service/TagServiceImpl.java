package com.example.fandoom_backend.tag.service;

import com.example.fandoom_backend.common.exception.DuplicateResourceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.common.util.SlugGenerator;
import com.example.fandoom_backend.tag.dto.TagRequest;
import com.example.fandoom_backend.tag.dto.TagResponse;
import com.example.fandoom_backend.tag.entity.Tag;
import com.example.fandoom_backend.tag.entity.TagType;
import com.example.fandoom_backend.tag.mapper.TagMapper;
import com.example.fandoom_backend.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;
    private final TagMapper tagMapper;

    @Override
    public List<TagResponse> list(TagType type) {
        return tagMapper.toResponseList(
                type == null ? tagRepository.findAll() : tagRepository.findByType(type));
    }

    @Override
    public TagResponse getById(Long id) {
        return tagMapper.toResponse(findEntityById(id));
    }

    @Override
    @Transactional
    public TagResponse create(TagRequest request) {
        if (tagRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Bu isimde bir tag zaten var: " + request.name());
        }
        Tag tag = Tag.builder()
                .name(request.name())
                .slug(SlugGenerator.generateUnique(request.name(), tagRepository::existsBySlug))
                .description(request.description())
                .type(request.type())
                .build();
        return tagMapper.toResponse(tagRepository.save(tag));
    }

    @Override
    @Transactional
    public List<TagResponse> createBatch(List<TagRequest> requests) {
        return requests.stream()
                .map(this::create)
                .toList();
    }

    @Override
    @Transactional
    public TagResponse update(Long id, TagRequest request) {
        Tag tag = findEntityById(id);
        if (!tag.getName().equals(request.name())) {
            if (tagRepository.existsByNameAndIdNot(request.name(), id)) {
                throw new DuplicateResourceException("Bu isimde bir tag zaten var: " + request.name());
            }
            tag.setSlug(SlugGenerator.generateUnique(request.name(),
                    slug -> tagRepository.existsBySlugAndIdNot(slug, id)));
        }
        tag.setName(request.name());
        tag.setDescription(request.description());
        tag.setType(request.type());
        return tagMapper.toResponse(tag);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!tagRepository.existsById(id)) {
            throw new ResourceNotFoundException("Tag bulunamadı: id=" + id);
        }
        tagRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return tagRepository.existsById(id);
    }

    private Tag findEntityById(Long id) {
        return tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag bulunamadı: id=" + id));
    }
}
