package com.example.fandoom_backend.video.service;

import com.example.fandoom_backend.common.exception.DuplicateResourceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.movie.dto.MovieDetailResponse;
import com.example.fandoom_backend.movie.service.MovieService;
import com.example.fandoom_backend.series.service.SeriesService;
import com.example.fandoom_backend.video.dto.VideoCreateRequest;
import com.example.fandoom_backend.video.dto.VideoResponse;
import com.example.fandoom_backend.video.dto.VideoUpdateRequest;
import com.example.fandoom_backend.video.entity.Video;
import com.example.fandoom_backend.video.entity.VideoItemType;
import com.example.fandoom_backend.video.mapper.VideoMapper;
import com.example.fandoom_backend.video.repository.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideoServiceImplTest {

    @Mock VideoRepository videoRepository;
    @Mock MovieService movieService;
    @Mock SeriesService seriesService;

    VideoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new VideoServiceImpl(videoRepository, Mappers.getMapper(VideoMapper.class),
                movieService, seriesService);
    }

    private void movieExists(long id) {
        MovieDetailResponse movie = mock(MovieDetailResponse.class);
        when(movie.id()).thenReturn(id);
        when(movieService.getBySlug("the-lighthouse")).thenReturn(movie);
    }

    private static VideoCreateRequest createRequest(Integer sortOrder) {
        return new VideoCreateRequest("Hyag7lR8CPA", " Trailer ", "A24", "https://youtube.com/@A24", sortOrder);
    }

    @Test
    void create_withoutSortOrder_appendsAfterMax() {
        movieExists(7L);
        when(videoRepository.existsByItemTypeAndItemIdAndYoutubeId(VideoItemType.MOVIE, 7L, "Hyag7lR8CPA"))
                .thenReturn(false);
        when(videoRepository.findMaxSortOrder(VideoItemType.MOVIE, 7L)).thenReturn(4);
        when(videoRepository.save(any(Video.class))).thenAnswer(inv -> inv.getArgument(0));

        VideoResponse response = service.create(VideoItemType.MOVIE, "the-lighthouse", createRequest(null));

        ArgumentCaptor<Video> captor = ArgumentCaptor.forClass(Video.class);
        verify(videoRepository).save(captor.capture());
        assertThat(captor.getValue().getSortOrder()).isEqualTo(5);
        assertThat(captor.getValue().getItemId()).isEqualTo(7L);
        assertThat(response.title()).isEqualTo("Trailer");
    }

    @Test
    void create_firstVideo_startsAtZero() {
        movieExists(7L);
        when(videoRepository.findMaxSortOrder(VideoItemType.MOVIE, 7L)).thenReturn(null);
        when(videoRepository.save(any(Video.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.create(VideoItemType.MOVIE, "the-lighthouse", createRequest(null)).sortOrder()).isZero();
    }

    @Test
    void create_duplicateYoutubeId_throwsConflict() {
        movieExists(7L);
        when(videoRepository.existsByItemTypeAndItemIdAndYoutubeId(VideoItemType.MOVIE, 7L, "Hyag7lR8CPA"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(VideoItemType.MOVIE, "the-lighthouse", createRequest(1)))
                .isInstanceOf(DuplicateResourceException.class);
        verify(videoRepository, never()).save(any());
    }

    @Test
    void list_unknownProduction_propagatesNotFound() {
        when(seriesService.getBySlug("yok")).thenThrow(new ResourceNotFoundException("yok"));

        assertThatThrownBy(() -> service.listByProduction(VideoItemType.SERIES, "yok"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void list_noVideos_returnsEmptyList() {
        movieExists(7L);
        when(videoRepository.findByItemTypeAndItemIdOrderBySortOrderAscIdAsc(VideoItemType.MOVIE, 7L))
                .thenReturn(List.of());

        assertThat(service.listByProduction(VideoItemType.MOVIE, "the-lighthouse")).isEmpty();
    }

    @Test
    void update_keepsYoutubeIdAndSortOrderWhenNull() {
        Video video = Video.builder().id(1L).itemId(7L).itemType(VideoItemType.MOVIE)
                .youtubeId("Hyag7lR8CPA").title("a").channelName("b").channelUrl("https://x.y").sortOrder(3).build();
        when(videoRepository.findById(1L)).thenReturn(Optional.of(video));

        VideoResponse response = service.update(1L,
                new VideoUpdateRequest("Yeni", "Kanal", "https://youtube.com/@k", null));

        assertThat(response.youtubeId()).isEqualTo("Hyag7lR8CPA");
        assertThat(response.sortOrder()).isEqualTo(3);
        assertThat(response.title()).isEqualTo("Yeni");
    }

    @Test
    void delete_missing_throwsNotFound() {
        when(videoRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(9L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
