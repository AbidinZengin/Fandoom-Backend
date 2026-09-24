package com.example.fandoom_backend.trivia.service;

import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.movie.service.MovieService;
import com.example.fandoom_backend.series.service.SeriesService;
import com.example.fandoom_backend.trivia.dto.TriviaRequest;
import com.example.fandoom_backend.trivia.entity.Trivia;
import com.example.fandoom_backend.trivia.entity.TriviaItemType;
import com.example.fandoom_backend.trivia.entity.TriviaTag;
import com.example.fandoom_backend.trivia.mapper.TriviaMapper;
import com.example.fandoom_backend.trivia.repository.TriviaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TriviaServiceImplTest {

    @Mock
    private TriviaRepository triviaRepository;
    @Mock
    private MovieService movieService;
    @Mock
    private SeriesService seriesService;

    private TriviaServiceImpl service;

    @BeforeEach
    void setUp() {
        TriviaMapper mapper = Mappers.getMapper(TriviaMapper.class);
        service = new TriviaServiceImpl(triviaRepository, mapper, movieService, seriesService);
    }

    private TriviaRequest request(TriviaItemType type, Boolean spoiler) {
        return new TriviaRequest(45L, type, "  Çöl sahneleri Ürdün'de çekildi.  ",
                TriviaTag.BEHIND_THE_SCENES, spoiler, null);
    }

    @Test
    void create_movie_validatesViaMovieServiceAndDefaultsSpoilerToFalse() {
        when(movieService.existsById(45L)).thenReturn(true);
        when(triviaRepository.save(any(Trivia.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.create(request(TriviaItemType.MOVIE, null), 7L);

        assertThat(response.isSpoiler()).isFalse();
        assertThat(response.content()).isEqualTo("Çöl sahneleri Ürdün'de çekildi.");
        assertThat(response.createdBy()).isEqualTo(7L);
        verify(seriesService, never()).existsById(anyLong());
    }

    @Test
    void create_series_validatesViaSeriesService() {
        when(seriesService.existsById(45L)).thenReturn(false);

        assertThatThrownBy(() -> service.create(request(TriviaItemType.SERIES, true), 7L))
                .isInstanceOf(InvalidReferenceException.class);
        verify(triviaRepository, never()).save(any());
    }

    @Test
    void list_random_usesRandomQueryWithDefaultCap() {
        when(triviaRepository.findRandom(45L, "MOVIE", 100)).thenReturn(List.of());

        service.list(45L, TriviaItemType.MOVIE, null, true);

        verify(triviaRepository).findRandom(45L, "MOVIE", 100);
        verify(triviaRepository, never()).findByItemIdAndItemType(any(), any(), any());
    }

    @Test
    void list_ordered_passesLimitAsPageSizeAndCapsIt() {
        when(triviaRepository.findByItemIdAndItemType(eq(45L), eq(TriviaItemType.SERIES), any(Pageable.class)))
                .thenReturn(List.of());

        service.list(45L, TriviaItemType.SERIES, 5000, false);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(triviaRepository).findByItemIdAndItemType(eq(45L), eq(TriviaItemType.SERIES), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
        verify(triviaRepository, never()).findRandom(anyLong(), any(), anyInt());
    }

    @Test
    void list_rejectsNonPositiveLimit() {
        assertThatThrownBy(() -> service.list(45L, TriviaItemType.MOVIE, 0, false))
                .isInstanceOf(InvalidReferenceException.class);
    }

    @Test
    void update_changingItemRevalidatesReference() {
        Trivia existing = Trivia.builder().id(1L).itemId(99L).itemType(TriviaItemType.MOVIE)
                .content("x").tag(TriviaTag.LORE).build();
        when(triviaRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(movieService.existsById(45L)).thenReturn(true);

        var response = service.update(1L, request(TriviaItemType.MOVIE, true));

        assertThat(response.itemId()).isEqualTo(45L);
        assertThat(response.isSpoiler()).isTrue();
        verify(movieService).existsById(45L);
    }

    @Test
    void update_sameItemSkipsReferenceCheck() {
        Trivia existing = Trivia.builder().id(1L).itemId(45L).itemType(TriviaItemType.MOVIE)
                .content("x").tag(TriviaTag.LORE).build();
        when(triviaRepository.findById(1L)).thenReturn(Optional.of(existing));

        service.update(1L, request(TriviaItemType.MOVIE, false));

        verify(movieService, never()).existsById(anyLong());
    }

    @Test
    void delete_missingThrowsNotFound() {
        when(triviaRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
