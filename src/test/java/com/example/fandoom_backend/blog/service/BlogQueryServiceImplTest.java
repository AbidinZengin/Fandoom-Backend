package com.example.fandoom_backend.blog.service;

import com.example.fandoom_backend.blog.dto.BlogFilterCriteria;
import com.example.fandoom_backend.blog.dto.BlogFilterableSummaryResponse;
import com.example.fandoom_backend.blog.dto.BlogHubFacetsResponse;
import com.example.fandoom_backend.blog.dto.FranchiseFacetOptionResponse;
import com.example.fandoom_backend.blog.entity.Blog;
import com.example.fandoom_backend.blog.entity.BlogTag;
import com.example.fandoom_backend.blog.repository.BlogRepository;
import com.example.fandoom_backend.blog.repository.BlogTagRepository;
import com.example.fandoom_backend.blog.sort.BlogSortStrategyRegistry;
import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.franchise.dto.FranchiseDetailResponse;
import com.example.fandoom_backend.franchise.service.FranchiseService;
import com.example.fandoom_backend.tag.dto.TagAssignmentResponse;
import com.example.fandoom_backend.tag.dto.TagFacetOptionResponse;
import com.example.fandoom_backend.tag.entity.TagType;
import com.example.fandoom_backend.tag.entity.TaggableType;
import com.example.fandoom_backend.tag.service.TagAssignmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlogQueryServiceImplTest {

    @Mock
    private BlogRepository blogRepository;
    @Mock
    private BlogTagRepository blogTagRepository;
    @Mock
    private TagAssignmentService tagAssignmentService;
    @Mock
    private FranchiseService franchiseService;
    @Mock
    private BlogSortStrategyRegistry blogSortStrategyRegistry;

    private BlogQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BlogQueryServiceImpl(blogRepository, blogTagRepository, tagAssignmentService, franchiseService,
                blogSortStrategyRegistry);
        lenient().when(blogSortStrategyRegistry.resolve(any()))
                .thenReturn(Sort.by(Sort.Direction.DESC, "publishedAt"));
    }

    @Test
    void findFilterable_mapsPageContentUsingTagAssignmentsAndFranchiseService() {
        Blog blog = Blog.builder().id(1L).slug("s1").title("T1")
                .imageUrl("img.jpg").imageAlt("alt")
                .readingTimeMinutes(5).publishedAt(LocalDateTime.now())
                .viewCount(10L).spoilerFree(true).build();
        blog.addTag(BlogTag.builder().franchiseId(7L).build());

        when(blogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(blog), PageRequest.of(0, 20), 1));
        when(tagAssignmentService.listForTarget(TaggableType.BLOG, 1L)).thenReturn(List.of(
                new TagAssignmentResponse(1L, 10L, "Listicle", "listicle", TagType.FORMAT, TaggableType.BLOG, 1L),
                new TagAssignmentResponse(2L, 11L, "Dark", "dark", TagType.MOOD, TaggableType.BLOG, 1L),
                new TagAssignmentResponse(3L, 12L, "Hopeful", "hopeful", TagType.MOOD, TaggableType.BLOG, 1L),
                new TagAssignmentResponse(4L, 13L, "Betrayal", "betrayal", TagType.THEME, TaggableType.BLOG, 1L)));
        when(franchiseService.getById(7L)).thenReturn(franchiseDetail(7L, "got"));

        BlogFilterCriteria criteria = new BlogFilterCriteria(null, null, null, null, null);
        PageResponse<BlogFilterableSummaryResponse> result =
                service.findFilterable(criteria, "latest", PageRequest.of(0, 20));

        assertThat(result.content()).hasSize(1);
        BlogFilterableSummaryResponse summary = result.content().get(0);
        assertThat(summary.id()).isEqualTo(1L);
        assertThat(summary.slug()).isEqualTo("s1");
        assertThat(summary.format()).isEqualTo("listicle");
        assertThat(summary.moods()).containsExactly("dark", "hopeful");
        assertThat(summary.themes()).containsExactly("betrayal");
        assertThat(summary.franchiseSlug()).isEqualTo("got");
        assertThat(summary.spoilerFree()).isTrue();
    }

    @Test
    void findFilterable_blankFranchiseSlug_neverResolvesViaFranchiseService() {
        Blog blog = Blog.builder().id(2L).slug("s2").title("T2").build();
        when(blogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(blog), PageRequest.of(0, 20), 1));
        when(tagAssignmentService.listForTarget(any(), any())).thenReturn(List.of());

        BlogFilterCriteria criteria = new BlogFilterCriteria(null, "   ", null, null, null);
        service.findFilterable(criteria, "latest", PageRequest.of(0, 20));

        verify(franchiseService, never()).getBySlug(any());
    }

    @Test
    void findFilterable_nullFranchiseSlug_neverResolvesViaFranchiseService() {
        Blog blog = Blog.builder().id(3L).slug("s3").title("T3").build();
        when(blogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(blog), PageRequest.of(0, 20), 1));
        when(tagAssignmentService.listForTarget(any(), any())).thenReturn(List.of());

        BlogFilterCriteria criteria = new BlogFilterCriteria(null, null, null, null, null);
        service.findFilterable(criteria, "latest", PageRequest.of(0, 20));

        verify(franchiseService, never()).getBySlug(any());
    }

    @Test
    void findFilterable_franchiseSlugProvided_resolvesViaFranchiseService() {
        when(franchiseService.getBySlug("got")).thenReturn(franchiseDetail(9L, "got"));
        when(blogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        BlogFilterCriteria criteria = new BlogFilterCriteria(null, "got", null, null, null);
        service.findFilterable(criteria, "latest", PageRequest.of(0, 20));

        verify(franchiseService).getBySlug("got");
    }

    @Test
    void findFilterable_sortKeyResolvedByRegistryAndAppliedToPageable() {
        Sort sort = Sort.by(Sort.Direction.ASC, "publishedAt");
        when(blogSortStrategyRegistry.resolve("oldest")).thenReturn(sort);
        when(blogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        BlogFilterCriteria criteria = new BlogFilterCriteria(null, null, null, null, null);
        service.findFilterable(criteria, "oldest", PageRequest.of(2, 15));

        verify(blogSortStrategyRegistry).resolve("oldest");
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(blogRepository).findAll(any(Specification.class), pageableCaptor.capture());
        Pageable captured = pageableCaptor.getValue();
        assertThat(captured.getSort()).isEqualTo(sort);
        assertThat(captured.getPageNumber()).isEqualTo(2);
        assertThat(captured.getPageSize()).isEqualTo(15);
    }

    @Test
    void getFacets_combinesTagAndFranchiseCountsIntoSingleResponse() {
        List<TagFacetOptionResponse> formats = List.of(new TagFacetOptionResponse(1L, "Listicle", "listicle", 3L));
        List<TagFacetOptionResponse> moods = List.of(new TagFacetOptionResponse(2L, "Dark", "dark", 5L));
        List<TagFacetOptionResponse> themes = List.of(new TagFacetOptionResponse(3L, "Betrayal", "betrayal", 2L));
        when(tagAssignmentService.findFacetOptions(TagType.FORMAT, TaggableType.BLOG)).thenReturn(formats);
        when(tagAssignmentService.findFacetOptions(TagType.MOOD, TaggableType.BLOG)).thenReturn(moods);
        when(tagAssignmentService.findFacetOptions(TagType.THEME, TaggableType.BLOG)).thenReturn(themes);
        List<Object[]> franchiseCounts = java.util.Collections.singletonList(new Object[]{7L, 4L});
        when(blogTagRepository.countDistinctBlogsByFranchiseId()).thenReturn(franchiseCounts);
        when(franchiseService.getById(7L)).thenReturn(franchiseDetail(7L, "got"));

        BlogHubFacetsResponse result = service.getFacets();

        assertThat(result.formats()).isEqualTo(formats);
        assertThat(result.moods()).isEqualTo(moods);
        assertThat(result.themes()).isEqualTo(themes);
        assertThat(result.franchises()).containsExactly(
                new FranchiseFacetOptionResponse(7L, "Name", "got", null, 4L));
    }

    private FranchiseDetailResponse franchiseDetail(Long id, String slug) {
        return new FranchiseDetailResponse(id, "Name", slug, null, null, null, null, null);
    }
}
