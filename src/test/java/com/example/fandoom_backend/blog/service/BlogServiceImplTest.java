package com.example.fandoom_backend.blog.service;

import com.example.fandoom_backend.blog.dto.BlogBlockRequest;
import com.example.fandoom_backend.blog.dto.BlogDetailResponse;
import com.example.fandoom_backend.blog.dto.BlogRequest;
import com.example.fandoom_backend.blog.dto.BlogSummaryResponse;
import com.example.fandoom_backend.blog.dto.BlogTagRequest;
import com.example.fandoom_backend.blog.entity.Blog;
import com.example.fandoom_backend.blog.entity.BlogBlockType;
import com.example.fandoom_backend.blog.entity.BlogStatus;
import com.example.fandoom_backend.blog.entity.SubjectType;
import com.example.fandoom_backend.blog.mapper.BlogMapper;
import com.example.fandoom_backend.blog.repository.BlogRelationRepository;
import com.example.fandoom_backend.blog.repository.BlogRepository;
import com.example.fandoom_backend.blog.repository.BlogTagRepository;
import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.franchise.service.FranchiseService;
import com.example.fandoom_backend.media.service.ImageStorageService;
import com.example.fandoom_backend.movie.service.MovieService;
import com.example.fandoom_backend.series.dto.SeriesDetailResponse;
import com.example.fandoom_backend.series.service.SeriesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlogServiceImplTest {

    @Mock
    private BlogRepository blogRepository;
    @Mock
    private BlogTagRepository blogTagRepository;
    @Mock
    private BlogRelationRepository blogRelationRepository;
    @Mock
    private BlogMapper blogMapper;
    @Mock
    private MovieService movieService;
    @Mock
    private SeriesService seriesService;
    @Mock
    private FranchiseService franchiseService;
    @Mock
    private ImageStorageService imageStorageService;

    private BlogServiceImpl service;

    private final AtomicLong idSequence = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        service = new BlogServiceImpl(blogRepository, blogTagRepository,
                blogRelationRepository, blogMapper, movieService, seriesService,
                franchiseService, imageStorageService);

        lenient().when(blogRepository.existsBySlug(anyString())).thenReturn(false);
        lenient().when(blogRepository.save(any(Blog.class))).thenAnswer(inv -> {
            Blog blog = inv.getArgument(0);
            if (blog.getId() == null) {
                blog.setId(idSequence.getAndIncrement());
            }
            return blog;
        });
        lenient().when(blogRelationRepository.findBySourceBlogIdOrderByOrderIndexAsc(anyLong()))
                .thenReturn(List.of());
        lenient().when(blogTagRepository.findByExactEpisode(any(), any(), any(), any()))
                .thenReturn(List.of());
        lenient().when(blogTagRepository.findBySameSeason(any(), any(), any())).thenReturn(List.of());
        lenient().when(blogTagRepository.findBySameProduction(any(), any())).thenReturn(List.of());
        lenient().when(blogTagRepository.findBySameFranchise(any())).thenReturn(List.of());
        lenient().when(blogRepository.findByStatusOrderByPublishedAtDescViewCountDesc(any(), any()))
                .thenReturn(List.of());
        lenient().when(blogRepository
                        .findByStatusAndIdNotInOrderByPublishedAtDescViewCountDesc(any(), any(), any()))
                .thenReturn(List.of());

        lenient().when(blogMapper.toSummaryResponse(any(Blog.class)))
                .thenAnswer(inv -> summaryOf(inv.getArgument(0)));
        lenient().when(blogMapper.toSummaryResponseList(any()))
                .thenAnswer(inv -> {
                    List<Blog> list = inv.getArgument(0);
                    return list.stream().map(this::summaryOf).toList();
                });
        lenient().when(blogMapper.toDetailResponse(any(Blog.class), any()))
                .thenAnswer(inv -> detailOf(inv.getArgument(0), inv.getArgument(1)));
    }

    // ---- create ----

    @Test
    void create_publishedStatus_setsPublishedAtAndComputesReadingTime() {
        String longText = String.join(" ", Collections.nCopies(450, "kelime"));
        BlogRequest request = new BlogRequest(
                "The Sword Called Ice", "kicker", "axis",
                "img.jpg", "img-large.jpg", "alt",
                1, 1, BlogStatus.PUBLISHED,
                List.of(new BlogBlockRequest(BlogBlockType.PARAGRAPH, longText, null, null)),
                null);

        BlogDetailResponse response = service.create(request);

        assertThat(response.slug()).isEqualTo("the-sword-called-ice");
        assertThat(response.status()).isEqualTo(BlogStatus.PUBLISHED);
        assertThat(response.publishedAt()).isNotNull();
        // 450 kelime / 200 wpm = 2.25 -> yuvarlanınca 2
        assertThat(response.readingTimeMinutes()).isEqualTo(2);
    }

    @Test
    void create_draftStatus_leavesPublishedAtNull() {
        BlogDetailResponse response = service.create(minimalRequest("Draft Title", BlogStatus.DRAFT));

        assertThat(response.publishedAt()).isNull();
    }

    @Test
    void create_noBlocks_readingTimeIsNull() {
        BlogDetailResponse response = service.create(minimalRequest("No Body", BlogStatus.DRAFT));

        assertThat(response.readingTimeMinutes()).isNull();
    }

    @Test
    void create_tagWithBothSubjectAndFranchise_throwsInvalidReferenceException() {
        BlogRequest request = requestWithTags(
                new BlogTagRequest(SubjectType.MOVIE, 1L, null, null, 2L));

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(InvalidReferenceException.class);
    }

    @Test
    void create_tagWithNeitherSubjectNorFranchise_throwsInvalidReferenceException() {
        BlogRequest request = requestWithTags(
                new BlogTagRequest(null, null, null, null, null));

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(InvalidReferenceException.class);
    }

    @Test
    void create_tagWithNonExistentMovie_throwsInvalidReferenceException() {
        when(movieService.existsById(99L)).thenReturn(false);
        BlogRequest request = requestWithTags(
                new BlogTagRequest(SubjectType.MOVIE, 99L, null, null, null));

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(InvalidReferenceException.class);
    }

    @Test
    void create_tagWithNonExistentFranchise_throwsInvalidReferenceException() {
        when(franchiseService.existsById(77L)).thenReturn(false);
        BlogRequest request = requestWithTags(
                new BlogTagRequest(null, null, null, null, 77L));

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(InvalidReferenceException.class);
    }

    @Test
    void create_validSeriesTag_validatesReferenceAndPersists() {
        when(seriesService.existsById(5L)).thenReturn(true);
        BlogRequest request = requestWithTags(
                new BlogTagRequest(SubjectType.SERIES, 5L, 1, 1, null));

        service.create(request);

        verify(seriesService).existsById(5L);
    }

    // ---- update ----

    @Test
    void update_titleUnchanged_keepsSlug() {
        Blog existing = Blog.builder().id(5L).title("Same Title").slug("same-title")
                .status(BlogStatus.DRAFT).build();
        when(blogRepository.findById(5L)).thenReturn(Optional.of(existing));

        service.update(5L, minimalRequest("Same Title", BlogStatus.DRAFT));

        verify(blogRepository, never()).existsBySlugAndIdNot(anyString(), anyLong());
        assertThat(existing.getSlug()).isEqualTo("same-title");
    }

    @Test
    void update_titleChanged_regeneratesSlug() {
        Blog existing = Blog.builder().id(6L).title("Old Title").slug("old-title")
                .status(BlogStatus.DRAFT).build();
        when(blogRepository.findById(6L)).thenReturn(Optional.of(existing));
        when(blogRepository.existsBySlugAndIdNot("new-title", 6L)).thenReturn(false);

        service.update(6L, minimalRequest("New Title", BlogStatus.DRAFT));

        assertThat(existing.getSlug()).isEqualTo("new-title");
    }

    @Test
    void update_transitionToPublished_setsPublishedAt() {
        Blog existing = Blog.builder().id(7L).title("T").slug("t")
                .status(BlogStatus.DRAFT).publishedAt(null).build();
        when(blogRepository.findById(7L)).thenReturn(Optional.of(existing));

        service.update(7L, minimalRequest("T", BlogStatus.PUBLISHED));

        assertThat(existing.getPublishedAt()).isNotNull();
    }

    @Test
    void update_leavingPublished_clearsPublishedAt() {
        Blog existing = Blog.builder().id(8L).title("T").slug("t")
                .status(BlogStatus.PUBLISHED).publishedAt(java.time.LocalDateTime.now().minusDays(1)).build();
        when(blogRepository.findById(8L)).thenReturn(Optional.of(existing));

        service.update(8L, minimalRequest("T", BlogStatus.DRAFT));

        assertThat(existing.getPublishedAt()).isNull();
    }

    @Test
    void update_imageUrlsChanged_deletesOldImagesViaImageStorageService() {
        Blog existing = Blog.builder().id(9L).title("T").slug("t")
                .status(BlogStatus.DRAFT).imageUrl("old.jpg").imageUrlLarge("old-large.jpg").build();
        when(blogRepository.findById(9L)).thenReturn(Optional.of(existing));

        BlogRequest request = new BlogRequest("T", null, null,
                "new.jpg", "new-large.jpg", null, null, null, BlogStatus.DRAFT, null, null);

        service.update(9L, request);

        verify(imageStorageService).deleteIfChanged("old.jpg", "new.jpg");
        verify(imageStorageService).deleteIfChanged("old-large.jpg", "new-large.jpg");
    }

    @Test
    void update_notFound_throwsResourceNotFoundException() {
        when(blogRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(404L, minimalRequest("T", BlogStatus.DRAFT)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- delete ----

    @Test
    void delete_deletesBothImagesThenEntity() {
        Blog existing = Blog.builder().id(10L).title("T").slug("t")
                .imageUrl("a.jpg").imageUrlLarge("b.jpg").build();
        when(blogRepository.findById(10L)).thenReturn(Optional.of(existing));

        service.delete(10L);

        verify(imageStorageService).delete("a.jpg");
        verify(imageStorageService).delete("b.jpg");
        verify(blogRepository).deleteById(10L);
    }

    // ---- getBySlug ----

    @Test
    void getBySlug_notFound_throwsResourceNotFoundException() {
        when(blogRepository.findBySlugAndStatus("missing", BlogStatus.PUBLISHED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBySlug("missing")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getBySlug_found_incrementsViewCount() {
        Blog existing = Blog.builder().id(11L).title("T").slug("t")
                .status(BlogStatus.PUBLISHED).viewCount(4L).build();
        when(blogRepository.findBySlugAndStatus("t", BlogStatus.PUBLISHED))
                .thenReturn(Optional.of(existing));

        BlogDetailResponse response = service.getBySlug("t");

        verify(blogRepository).incrementViewCount(11L);
        assertThat(response.viewCount()).isEqualTo(5L);
    }

    // ---- replaceRelated ----

    @Test
    void replaceRelated_containsSelf_throwsInvalidReferenceException() {
        Blog existing = Blog.builder().id(30L).title("T").slug("t").build();
        when(blogRepository.findById(30L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.replaceRelated(30L, List.of(30L)))
                .isInstanceOf(InvalidReferenceException.class);
    }

    @Test
    void replaceRelated_validTargets_buildsOrderedRelations() {
        Blog source = Blog.builder().id(31L).title("Source").slug("source").build();
        Blog target1 = Blog.builder().id(32L).title("A").slug("a").build();
        Blog target2 = Blog.builder().id(33L).title("B").slug("b").build();
        when(blogRepository.findById(31L)).thenReturn(Optional.of(source));
        when(blogRepository.findById(32L)).thenReturn(Optional.of(target1));
        when(blogRepository.findById(33L)).thenReturn(Optional.of(target2));

        List<BlogSummaryResponse> result = service.replaceRelated(31L, List.of(32L, 33L));

        assertThat(result).extracting(BlogSummaryResponse::id).containsExactly(32L, 33L);
        assertThat(source.getOutgoingRelations()).hasSize(2);
        assertThat(source.getOutgoingRelations().get(0).getOrderIndex()).isZero();
        assertThat(source.getOutgoingRelations().get(1).getOrderIndex()).isEqualTo(1);
    }

    // ---- findRelatedForProduction (kademeli-geri-düşüş merdiveni) ----

    @Test
    void findRelatedForProduction_exactEpisodeMatch_stopsLadderEarly() {
        stubSeriesLookup(100L, "got", null);
        Blog match = Blog.builder().id(200L).title("Match").slug("match").build();
        when(blogTagRepository.findByExactEpisode(SubjectType.SERIES, 100L, 1, 1))
                .thenReturn(List.of(match));

        List<BlogSummaryResponse> result =
                service.findRelatedForProduction(SubjectType.SERIES, "got", 1, 1, 1);

        assertThat(result).extracting(BlogSummaryResponse::id).containsExactly(200L);
        verify(blogTagRepository, never()).findBySameSeason(any(), any(), any());
        verify(blogTagRepository, never()).findBySameProduction(any(), any());
        verify(blogRepository, never())
                .findByStatusOrderByPublishedAtDescViewCountDesc(any(), any());
    }

    @Test
    void findRelatedForProduction_noTagMatches_fallsBackToPopularity() {
        stubSeriesLookup(100L, "got", null);
        Blog popular = Blog.builder().id(300L).title("Popular").slug("popular").build();
        when(blogRepository.findByStatusOrderByPublishedAtDescViewCountDesc(eq(BlogStatus.PUBLISHED), any()))
                .thenReturn(List.of(popular));

        List<BlogSummaryResponse> result =
                service.findRelatedForProduction(SubjectType.SERIES, "got", 1, 1, 3);

        assertThat(result).extracting(BlogSummaryResponse::id).containsExactly(300L);
    }

    @Test
    void findRelatedForProduction_deduplicatesAcrossTiers() {
        stubSeriesLookup(100L, "got", null);
        Blog shared = Blog.builder().id(400L).title("Shared").slug("shared").build();
        when(blogTagRepository.findByExactEpisode(any(), any(), any(), any())).thenReturn(List.of(shared));
        when(blogTagRepository.findBySameSeason(any(), any(), any())).thenReturn(List.of(shared));

        List<BlogSummaryResponse> result =
                service.findRelatedForProduction(SubjectType.SERIES, "got", 1, 1, 5);

        assertThat(result).extracting(BlogSummaryResponse::id).containsExactly(400L);
    }

    @Test
    void findRelatedForProduction_unknownProductionSlug_throwsResourceNotFoundException() {
        when(seriesService.getBySlug("missing")).thenThrow(new ResourceNotFoundException("Series bulunamadı"));

        assertThatThrownBy(() -> service.findRelatedForProduction(SubjectType.SERIES, "missing", 1, 1, 3))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- yardımcılar ----

    private void stubSeriesLookup(Long id, String slug, Long franchiseId) {
        SeriesDetailResponse detail = seriesDetail(id, franchiseId);
        lenient().when(seriesService.getBySlug(slug)).thenReturn(detail);
        lenient().when(seriesService.getById(id)).thenReturn(detail);
    }

    private SeriesDetailResponse seriesDetail(Long id, Long franchiseId) {
        return new SeriesDetailResponse(id, "Title", null, "slug", null,
                null, null, null,
                null, null, null,
                null, null, null,
                null, null, null,
                null, null,
                franchiseId, Set.of(), Set.of(),
                List.of(),
                null, null);
    }

    private BlogRequest minimalRequest(String title, BlogStatus status) {
        return new BlogRequest(title, null, null, null, null, null, null, null, status, null, null);
    }

    private BlogRequest requestWithTags(BlogTagRequest... tags) {
        return new BlogRequest("T", null, null, null, null, null, null, null,
                BlogStatus.DRAFT, null, List.of(tags));
    }

    private BlogSummaryResponse summaryOf(Blog blog) {
        return new BlogSummaryResponse(blog.getId(), blog.getSlug(), blog.getTitle(),
                blog.getImageUrl(), blog.getImageAlt(), blog.getReadingTimeMinutes());
    }

    private BlogDetailResponse detailOf(Blog blog, List<BlogSummaryResponse> related) {
        return new BlogDetailResponse(blog.getId(), blog.getSlug(), blog.getTitle(),
                blog.getKicker(), blog.getAxis(),
                blog.getImageUrl(), blog.getImageUrlLarge(), blog.getImageAlt(),
                blog.getSpoilerThroughSeasonNumber(), blog.getSpoilerThroughEpisodeNumber(),
                blog.getStatus(), blog.getPublishedAt(), blog.getViewCount(),
                blog.getReadingTimeMinutes(),
                List.of(), List.of(), related,
                blog.getCreatedAt(), blog.getUpdatedAt());
    }
}
