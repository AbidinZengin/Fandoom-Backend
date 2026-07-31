package com.example.fandoom_backend.contentdrop.service;

import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.contentdrop.dto.ContentDropBlockRequest;
import com.example.fandoom_backend.contentdrop.dto.ContentDropDetailResponse;
import com.example.fandoom_backend.contentdrop.dto.ContentDropRequest;
import com.example.fandoom_backend.contentdrop.dto.ContentDropSummaryResponse;
import com.example.fandoom_backend.contentdrop.dto.ContentDropTagRequest;
import com.example.fandoom_backend.contentdrop.entity.ContentDrop;
import com.example.fandoom_backend.contentdrop.entity.ContentDropBlockType;
import com.example.fandoom_backend.contentdrop.entity.ContentDropStatus;
import com.example.fandoom_backend.contentdrop.entity.SubjectType;
import com.example.fandoom_backend.contentdrop.mapper.ContentDropMapper;
import com.example.fandoom_backend.contentdrop.repository.ContentDropRelationRepository;
import com.example.fandoom_backend.contentdrop.repository.ContentDropRepository;
import com.example.fandoom_backend.contentdrop.repository.ContentDropTagRepository;
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
class ContentDropServiceImplTest {

    @Mock
    private ContentDropRepository contentDropRepository;
    @Mock
    private ContentDropTagRepository contentDropTagRepository;
    @Mock
    private ContentDropRelationRepository contentDropRelationRepository;
    @Mock
    private ContentDropMapper contentDropMapper;
    @Mock
    private MovieService movieService;
    @Mock
    private SeriesService seriesService;
    @Mock
    private FranchiseService franchiseService;
    @Mock
    private ImageStorageService imageStorageService;

    private ContentDropServiceImpl service;

    private final AtomicLong idSequence = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        service = new ContentDropServiceImpl(contentDropRepository, contentDropTagRepository,
                contentDropRelationRepository, contentDropMapper, movieService, seriesService,
                franchiseService, imageStorageService);

        lenient().when(contentDropRepository.existsBySlug(anyString())).thenReturn(false);
        lenient().when(contentDropRepository.save(any(ContentDrop.class))).thenAnswer(inv -> {
            ContentDrop contentDrop = inv.getArgument(0);
            if (contentDrop.getId() == null) {
                contentDrop.setId(idSequence.getAndIncrement());
            }
            return contentDrop;
        });
        lenient().when(contentDropRelationRepository.findBySourceContentDropIdOrderByOrderIndexAsc(anyLong()))
                .thenReturn(List.of());
        lenient().when(contentDropTagRepository.findByExactEpisode(any(), any(), any(), any()))
                .thenReturn(List.of());
        lenient().when(contentDropTagRepository.findBySameSeason(any(), any(), any())).thenReturn(List.of());
        lenient().when(contentDropTagRepository.findBySameProduction(any(), any())).thenReturn(List.of());
        lenient().when(contentDropTagRepository.findBySameFranchise(any())).thenReturn(List.of());
        lenient().when(contentDropRepository.findByStatusOrderByPublishedAtDescViewCountDesc(any(), any()))
                .thenReturn(List.of());
        lenient().when(contentDropRepository
                        .findByStatusAndIdNotInOrderByPublishedAtDescViewCountDesc(any(), any(), any()))
                .thenReturn(List.of());

        lenient().when(contentDropMapper.toSummaryResponse(any(ContentDrop.class)))
                .thenAnswer(inv -> summaryOf(inv.getArgument(0)));
        lenient().when(contentDropMapper.toSummaryResponseList(any()))
                .thenAnswer(inv -> {
                    List<ContentDrop> list = inv.getArgument(0);
                    return list.stream().map(this::summaryOf).toList();
                });
        lenient().when(contentDropMapper.toDetailResponse(any(ContentDrop.class), any()))
                .thenAnswer(inv -> detailOf(inv.getArgument(0), inv.getArgument(1)));
    }

    // ---- create ----

    @Test
    void create_publishedStatus_setsPublishedAtAndComputesReadingTime() {
        String longText = String.join(" ", Collections.nCopies(450, "kelime"));
        ContentDropRequest request = new ContentDropRequest(
                "The Sword Called Ice", "kicker", "axis",
                "img.jpg", "img-large.jpg", "alt",
                1, 1, ContentDropStatus.PUBLISHED,
                List.of(new ContentDropBlockRequest(ContentDropBlockType.PARAGRAPH, longText, null, null)),
                null);

        ContentDropDetailResponse response = service.create(request);

        assertThat(response.slug()).isEqualTo("the-sword-called-ice");
        assertThat(response.status()).isEqualTo(ContentDropStatus.PUBLISHED);
        assertThat(response.publishedAt()).isNotNull();
        // 450 kelime / 200 wpm = 2.25 -> yuvarlanınca 2
        assertThat(response.readingTimeMinutes()).isEqualTo(2);
    }

    @Test
    void create_draftStatus_leavesPublishedAtNull() {
        ContentDropDetailResponse response = service.create(minimalRequest("Draft Title", ContentDropStatus.DRAFT));

        assertThat(response.publishedAt()).isNull();
    }

    @Test
    void create_noBlocks_readingTimeIsNull() {
        ContentDropDetailResponse response = service.create(minimalRequest("No Body", ContentDropStatus.DRAFT));

        assertThat(response.readingTimeMinutes()).isNull();
    }

    @Test
    void create_tagWithBothSubjectAndFranchise_throwsInvalidReferenceException() {
        ContentDropRequest request = requestWithTags(
                new ContentDropTagRequest(SubjectType.MOVIE, 1L, null, null, 2L));

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(InvalidReferenceException.class);
    }

    @Test
    void create_tagWithNeitherSubjectNorFranchise_throwsInvalidReferenceException() {
        ContentDropRequest request = requestWithTags(
                new ContentDropTagRequest(null, null, null, null, null));

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(InvalidReferenceException.class);
    }

    @Test
    void create_tagWithNonExistentMovie_throwsInvalidReferenceException() {
        when(movieService.existsById(99L)).thenReturn(false);
        ContentDropRequest request = requestWithTags(
                new ContentDropTagRequest(SubjectType.MOVIE, 99L, null, null, null));

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(InvalidReferenceException.class);
    }

    @Test
    void create_tagWithNonExistentFranchise_throwsInvalidReferenceException() {
        when(franchiseService.existsById(77L)).thenReturn(false);
        ContentDropRequest request = requestWithTags(
                new ContentDropTagRequest(null, null, null, null, 77L));

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(InvalidReferenceException.class);
    }

    @Test
    void create_validSeriesTag_validatesReferenceAndPersists() {
        when(seriesService.existsById(5L)).thenReturn(true);
        ContentDropRequest request = requestWithTags(
                new ContentDropTagRequest(SubjectType.SERIES, 5L, 1, 1, null));

        service.create(request);

        verify(seriesService).existsById(5L);
    }

    // ---- update ----

    @Test
    void update_titleUnchanged_keepsSlug() {
        ContentDrop existing = ContentDrop.builder().id(5L).title("Same Title").slug("same-title")
                .status(ContentDropStatus.DRAFT).build();
        when(contentDropRepository.findById(5L)).thenReturn(Optional.of(existing));

        service.update(5L, minimalRequest("Same Title", ContentDropStatus.DRAFT));

        verify(contentDropRepository, never()).existsBySlugAndIdNot(anyString(), anyLong());
        assertThat(existing.getSlug()).isEqualTo("same-title");
    }

    @Test
    void update_titleChanged_regeneratesSlug() {
        ContentDrop existing = ContentDrop.builder().id(6L).title("Old Title").slug("old-title")
                .status(ContentDropStatus.DRAFT).build();
        when(contentDropRepository.findById(6L)).thenReturn(Optional.of(existing));
        when(contentDropRepository.existsBySlugAndIdNot("new-title", 6L)).thenReturn(false);

        service.update(6L, minimalRequest("New Title", ContentDropStatus.DRAFT));

        assertThat(existing.getSlug()).isEqualTo("new-title");
    }

    @Test
    void update_transitionToPublished_setsPublishedAt() {
        ContentDrop existing = ContentDrop.builder().id(7L).title("T").slug("t")
                .status(ContentDropStatus.DRAFT).publishedAt(null).build();
        when(contentDropRepository.findById(7L)).thenReturn(Optional.of(existing));

        service.update(7L, minimalRequest("T", ContentDropStatus.PUBLISHED));

        assertThat(existing.getPublishedAt()).isNotNull();
    }

    @Test
    void update_leavingPublished_clearsPublishedAt() {
        ContentDrop existing = ContentDrop.builder().id(8L).title("T").slug("t")
                .status(ContentDropStatus.PUBLISHED).publishedAt(java.time.LocalDateTime.now().minusDays(1)).build();
        when(contentDropRepository.findById(8L)).thenReturn(Optional.of(existing));

        service.update(8L, minimalRequest("T", ContentDropStatus.DRAFT));

        assertThat(existing.getPublishedAt()).isNull();
    }

    @Test
    void update_imageUrlsChanged_deletesOldImagesViaImageStorageService() {
        ContentDrop existing = ContentDrop.builder().id(9L).title("T").slug("t")
                .status(ContentDropStatus.DRAFT).imageUrl("old.jpg").imageUrlLarge("old-large.jpg").build();
        when(contentDropRepository.findById(9L)).thenReturn(Optional.of(existing));

        ContentDropRequest request = new ContentDropRequest("T", null, null,
                "new.jpg", "new-large.jpg", null, null, null, ContentDropStatus.DRAFT, null, null);

        service.update(9L, request);

        verify(imageStorageService).deleteIfChanged("old.jpg", "new.jpg");
        verify(imageStorageService).deleteIfChanged("old-large.jpg", "new-large.jpg");
    }

    @Test
    void update_notFound_throwsResourceNotFoundException() {
        when(contentDropRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(404L, minimalRequest("T", ContentDropStatus.DRAFT)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- delete ----

    @Test
    void delete_deletesBothImagesThenEntity() {
        ContentDrop existing = ContentDrop.builder().id(10L).title("T").slug("t")
                .imageUrl("a.jpg").imageUrlLarge("b.jpg").build();
        when(contentDropRepository.findById(10L)).thenReturn(Optional.of(existing));

        service.delete(10L);

        verify(imageStorageService).delete("a.jpg");
        verify(imageStorageService).delete("b.jpg");
        verify(contentDropRepository).deleteById(10L);
    }

    // ---- getBySlug ----

    @Test
    void getBySlug_notFound_throwsResourceNotFoundException() {
        when(contentDropRepository.findBySlugAndStatus("missing", ContentDropStatus.PUBLISHED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBySlug("missing")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getBySlug_found_incrementsViewCount() {
        ContentDrop existing = ContentDrop.builder().id(11L).title("T").slug("t")
                .status(ContentDropStatus.PUBLISHED).viewCount(4L).build();
        when(contentDropRepository.findBySlugAndStatus("t", ContentDropStatus.PUBLISHED))
                .thenReturn(Optional.of(existing));

        ContentDropDetailResponse response = service.getBySlug("t");

        verify(contentDropRepository).incrementViewCount(11L);
        assertThat(response.viewCount()).isEqualTo(5L);
    }

    // ---- replaceRelated ----

    @Test
    void replaceRelated_containsSelf_throwsInvalidReferenceException() {
        ContentDrop existing = ContentDrop.builder().id(30L).title("T").slug("t").build();
        when(contentDropRepository.findById(30L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.replaceRelated(30L, List.of(30L)))
                .isInstanceOf(InvalidReferenceException.class);
    }

    @Test
    void replaceRelated_validTargets_buildsOrderedRelations() {
        ContentDrop source = ContentDrop.builder().id(31L).title("Source").slug("source").build();
        ContentDrop target1 = ContentDrop.builder().id(32L).title("A").slug("a").build();
        ContentDrop target2 = ContentDrop.builder().id(33L).title("B").slug("b").build();
        when(contentDropRepository.findById(31L)).thenReturn(Optional.of(source));
        when(contentDropRepository.findById(32L)).thenReturn(Optional.of(target1));
        when(contentDropRepository.findById(33L)).thenReturn(Optional.of(target2));

        List<ContentDropSummaryResponse> result = service.replaceRelated(31L, List.of(32L, 33L));

        assertThat(result).extracting(ContentDropSummaryResponse::id).containsExactly(32L, 33L);
        assertThat(source.getOutgoingRelations()).hasSize(2);
        assertThat(source.getOutgoingRelations().get(0).getOrderIndex()).isZero();
        assertThat(source.getOutgoingRelations().get(1).getOrderIndex()).isEqualTo(1);
    }

    // ---- findRelatedForProduction (kademeli-geri-düşüş merdiveni) ----

    @Test
    void findRelatedForProduction_exactEpisodeMatch_stopsLadderEarly() {
        stubSeriesLookup(100L, "got", null);
        ContentDrop match = ContentDrop.builder().id(200L).title("Match").slug("match").build();
        when(contentDropTagRepository.findByExactEpisode(SubjectType.SERIES, 100L, 1, 1))
                .thenReturn(List.of(match));

        List<ContentDropSummaryResponse> result =
                service.findRelatedForProduction(SubjectType.SERIES, "got", 1, 1, 1);

        assertThat(result).extracting(ContentDropSummaryResponse::id).containsExactly(200L);
        verify(contentDropTagRepository, never()).findBySameSeason(any(), any(), any());
        verify(contentDropTagRepository, never()).findBySameProduction(any(), any());
        verify(contentDropRepository, never())
                .findByStatusOrderByPublishedAtDescViewCountDesc(any(), any());
    }

    @Test
    void findRelatedForProduction_noTagMatches_fallsBackToPopularity() {
        stubSeriesLookup(100L, "got", null);
        ContentDrop popular = ContentDrop.builder().id(300L).title("Popular").slug("popular").build();
        when(contentDropRepository.findByStatusOrderByPublishedAtDescViewCountDesc(eq(ContentDropStatus.PUBLISHED), any()))
                .thenReturn(List.of(popular));

        List<ContentDropSummaryResponse> result =
                service.findRelatedForProduction(SubjectType.SERIES, "got", 1, 1, 3);

        assertThat(result).extracting(ContentDropSummaryResponse::id).containsExactly(300L);
    }

    @Test
    void findRelatedForProduction_deduplicatesAcrossTiers() {
        stubSeriesLookup(100L, "got", null);
        ContentDrop shared = ContentDrop.builder().id(400L).title("Shared").slug("shared").build();
        when(contentDropTagRepository.findByExactEpisode(any(), any(), any(), any())).thenReturn(List.of(shared));
        when(contentDropTagRepository.findBySameSeason(any(), any(), any())).thenReturn(List.of(shared));

        List<ContentDropSummaryResponse> result =
                service.findRelatedForProduction(SubjectType.SERIES, "got", 1, 1, 5);

        assertThat(result).extracting(ContentDropSummaryResponse::id).containsExactly(400L);
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

    private ContentDropRequest minimalRequest(String title, ContentDropStatus status) {
        return new ContentDropRequest(title, null, null, null, null, null, null, null, status, null, null);
    }

    private ContentDropRequest requestWithTags(ContentDropTagRequest... tags) {
        return new ContentDropRequest("T", null, null, null, null, null, null, null,
                ContentDropStatus.DRAFT, null, List.of(tags));
    }

    private ContentDropSummaryResponse summaryOf(ContentDrop contentDrop) {
        return new ContentDropSummaryResponse(contentDrop.getId(), contentDrop.getSlug(), contentDrop.getTitle(),
                contentDrop.getImageUrl(), contentDrop.getImageAlt(), contentDrop.getReadingTimeMinutes());
    }

    private ContentDropDetailResponse detailOf(ContentDrop contentDrop, List<ContentDropSummaryResponse> related) {
        return new ContentDropDetailResponse(contentDrop.getId(), contentDrop.getSlug(), contentDrop.getTitle(),
                contentDrop.getKicker(), contentDrop.getAxis(),
                contentDrop.getImageUrl(), contentDrop.getImageUrlLarge(), contentDrop.getImageAlt(),
                contentDrop.getSpoilerThroughSeasonNumber(), contentDrop.getSpoilerThroughEpisodeNumber(),
                contentDrop.getStatus(), contentDrop.getPublishedAt(), contentDrop.getViewCount(),
                contentDrop.getReadingTimeMinutes(),
                List.of(), List.of(), related,
                contentDrop.getCreatedAt(), contentDrop.getUpdatedAt());
    }
}
