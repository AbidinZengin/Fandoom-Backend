package com.example.fandoom_backend.tag.service;

import com.example.fandoom_backend.blog.service.BlogService;
import com.example.fandoom_backend.common.exception.DuplicateResourceException;
import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.movie.service.MovieService;
import com.example.fandoom_backend.person.service.CharacterService;
import com.example.fandoom_backend.person.service.PersonService;
import com.example.fandoom_backend.series.service.SeriesService;
import com.example.fandoom_backend.tag.dto.TagAssignmentRequest;
import com.example.fandoom_backend.tag.dto.TagAssignmentResponse;
import com.example.fandoom_backend.tag.dto.TagFacetOptionResponse;
import com.example.fandoom_backend.tag.entity.Tag;
import com.example.fandoom_backend.tag.entity.TagAssignment;
import com.example.fandoom_backend.tag.entity.TagType;
import com.example.fandoom_backend.tag.entity.TaggableType;
import com.example.fandoom_backend.tag.mapper.TagAssignmentMapper;
import com.example.fandoom_backend.tag.repository.TagAssignmentRepository;
import com.example.fandoom_backend.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagAssignmentServiceImpl implements TagAssignmentService {

    private final TagAssignmentRepository tagAssignmentRepository;
    private final TagRepository tagRepository;
    private final TagAssignmentMapper tagAssignmentMapper;
    private final MovieService movieService;
    private final SeriesService seriesService;
    private final PersonService personService;
    private final CharacterService characterService;
    private final BlogService blogService;

    @Override
    public List<TagAssignmentResponse> listForTarget(TaggableType taggableType, Long taggableId) {
        return tagAssignmentMapper.toResponseList(
                tagAssignmentRepository.findByTaggableTypeAndTaggableId(taggableType, taggableId));
    }

    @Override
    @Transactional
    public TagAssignmentResponse assign(Long tagId, TagAssignmentRequest request) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag bulunamadı: id=" + tagId));
        assertTaggableExists(request.taggableType(), request.taggableId());
        if (tagAssignmentRepository.existsByTagIdAndTaggableTypeAndTaggableId(
                tagId, request.taggableType(), request.taggableId())) {
            throw new DuplicateResourceException("Bu etiket zaten bu hedefe atanmış");
        }
        TagAssignment tagAssignment = TagAssignment.builder()
                .tag(tag)
                .taggableType(request.taggableType())
                .taggableId(request.taggableId())
                .build();
        return tagAssignmentMapper.toResponse(tagAssignmentRepository.save(tagAssignment));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!tagAssignmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Tag assignment bulunamadı: id=" + id);
        }
        tagAssignmentRepository.deleteById(id);
    }

    @Override
    public List<TagFacetOptionResponse> findFacetOptions(TagType type, TaggableType taggableType) {
        return tagRepository.findFacetOptions(type, taggableType);
    }

    private void assertTaggableExists(TaggableType taggableType, Long taggableId) {
        boolean exists = switch (taggableType) {
            case MOVIE -> movieService.existsById(taggableId);
            case SERIES -> seriesService.existsById(taggableId);
            case PERSON -> personService.existsById(taggableId);
            case CHARACTER -> characterService.existsById(taggableId);
            case BLOG -> blogService.existsById(taggableId);
        };
        if (!exists) {
            throw new InvalidReferenceException(
                    "Geçersiz " + taggableType + " id: " + taggableId);
        }
    }
}
