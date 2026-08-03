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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagAssignmentServiceImplTest {

    @Mock
    private TagAssignmentRepository tagAssignmentRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private TagAssignmentMapper tagAssignmentMapper;
    @Mock
    private MovieService movieService;
    @Mock
    private SeriesService seriesService;
    @Mock
    private PersonService personService;
    @Mock
    private CharacterService characterService;
    @Mock
    private BlogService blogService;

    private TagAssignmentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TagAssignmentServiceImpl(tagAssignmentRepository, tagRepository, tagAssignmentMapper,
                movieService, seriesService, personService, characterService, blogService);

        lenient().when(tagRepository.findById(1L)).thenReturn(Optional.of(Tag.builder().id(1L).build()));
        lenient().when(tagAssignmentRepository.existsByTagIdAndTaggableTypeAndTaggableId(any(), any(), any()))
                .thenReturn(false);
        lenient().when(tagAssignmentRepository.save(any(TagAssignment.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(tagAssignmentMapper.toResponse(any()))
                .thenReturn(new TagAssignmentResponse(1L, 1L, "Tag", "tag", null, TaggableType.BLOG, 42L));
    }

    @Test
    void assign_blogTaggableExists_savesAssignment() {
        when(blogService.existsById(42L)).thenReturn(true);
        TagAssignmentRequest request = new TagAssignmentRequest(TaggableType.BLOG, 42L);

        service.assign(1L, request);

        verify(blogService).existsById(42L);
        verify(tagAssignmentRepository).save(any(TagAssignment.class));
    }

    @Test
    void assign_blogTaggableDoesNotExist_throwsInvalidReferenceException() {
        when(blogService.existsById(99L)).thenReturn(false);
        TagAssignmentRequest request = new TagAssignmentRequest(TaggableType.BLOG, 99L);

        assertThatThrownBy(() -> service.assign(1L, request))
                .isInstanceOf(InvalidReferenceException.class);

        verify(tagAssignmentRepository, never()).save(any());
    }

    @Test
    void assign_blogCase_neverConsultsOtherTaggableServices() {
        when(blogService.existsById(42L)).thenReturn(true);
        TagAssignmentRequest request = new TagAssignmentRequest(TaggableType.BLOG, 42L);

        service.assign(1L, request);

        verify(movieService, never()).existsById(any());
        verify(seriesService, never()).existsById(any());
        verify(personService, never()).existsById(any());
        verify(characterService, never()).existsById(any());
    }

    @Test
    void assign_duplicateAssignment_throwsDuplicateResourceException() {
        when(blogService.existsById(42L)).thenReturn(true);
        when(tagAssignmentRepository.existsByTagIdAndTaggableTypeAndTaggableId(1L, TaggableType.BLOG, 42L))
                .thenReturn(true);
        TagAssignmentRequest request = new TagAssignmentRequest(TaggableType.BLOG, 42L);

        assertThatThrownBy(() -> service.assign(1L, request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void assign_tagNotFound_throwsResourceNotFoundException() {
        when(tagRepository.findById(404L)).thenReturn(Optional.empty());
        TagAssignmentRequest request = new TagAssignmentRequest(TaggableType.BLOG, 42L);

        assertThatThrownBy(() -> service.assign(404L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findFacetOptions_delegatesToTagRepositoryWithGivenTypeAndTaggableType() {
        List<TagFacetOptionResponse> expected = List.of(
                new TagFacetOptionResponse(1L, "Listicle", "listicle", 3L),
                new TagFacetOptionResponse(2L, "Ranked", "ranked", 0L));
        when(tagRepository.findFacetOptions(TagType.FORMAT, TaggableType.BLOG)).thenReturn(expected);

        List<TagFacetOptionResponse> result = service.findFacetOptions(TagType.FORMAT, TaggableType.BLOG);

        assertThat(result).isEqualTo(expected);
        verify(tagRepository).findFacetOptions(TagType.FORMAT, TaggableType.BLOG);
    }
}
