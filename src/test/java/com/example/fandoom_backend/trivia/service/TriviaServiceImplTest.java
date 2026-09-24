package com.example.fandoom_backend.trivia.service;

import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.media.service.ImageStorageService;
import com.example.fandoom_backend.movie.service.MovieService;
import com.example.fandoom_backend.series.service.SeriesService;
import com.example.fandoom_backend.trivia.dto.TriviaRequest;
import com.example.fandoom_backend.trivia.entity.Trivia;
import com.example.fandoom_backend.trivia.entity.TriviaItemType;
import com.example.fandoom_backend.trivia.mapper.TriviaMapper;
import com.example.fandoom_backend.trivia.repository.TriviaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Locale;
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
    @Mock
    private ImageStorageService imageStorageService;

    private TriviaServiceImpl service;

    @BeforeEach
    void setUp() {
        TriviaMapper mapper = Mappers.getMapper(TriviaMapper.class);
        service = new TriviaServiceImpl(triviaRepository, mapper, movieService, seriesService, imageStorageService);
    }

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    private TriviaRequest request(TriviaItemType type, Boolean spoiler) {
        return new TriviaRequest(45L, type, "Desert", "  ", "  Desert scenes were shot in Jordan.  ",
                "Çöl sahneleri Ürdün'de çekildi.", "https://res.cloudinary.com/x/image/upload/new.webp",
                "behind the scenes", List.of("Cinematography", "  lore "), spoiler, null);
    }

    @Test
    void create_movie_validatesViaMovieServiceAndDefaultsSpoilerToFalse() {
        when(movieService.existsById(45L)).thenReturn(true);
        when(triviaRepository.save(any(Trivia.class))).thenAnswer(inv -> inv.getArgument(0));

        LocaleContextHolder.setLocale(Locale.forLanguageTag("tr"));

        var response = service.create(request(TriviaItemType.MOVIE, null), 7L);

        assertThat(response.isSpoiler()).isFalse();
        // content ana (EN) alan, contentTr ham döner; title/content istek diline göre çözülür
        assertThat(response.content()).isEqualTo("Çöl sahneleri Ürdün'de çekildi.");
        assertThat(response.contentTr()).isEqualTo("Çöl sahneleri Ürdün'de çekildi.");
        assertThat(response.title()).isEqualTo("Desert"); // titleTr boş → EN'e düşer
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
                .content("x").tags(new java.util.ArrayList<>(List.of("LORE"))).build();
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
                .content("x").tags(new java.util.ArrayList<>(List.of("LORE"))).build();
        when(triviaRepository.findById(1L)).thenReturn(Optional.of(existing));

        service.update(1L, request(TriviaItemType.MOVIE, false));

        verify(movieService, never()).existsById(anyLong());
    }

    @Test
    void delete_missingThrowsNotFound() {
        when(triviaRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_removesImageFromStorage() {
        Trivia existing = Trivia.builder().id(1L).itemId(45L).itemType(TriviaItemType.MOVIE)
                .content("x").tags(new java.util.ArrayList<>(List.of("LORE"))).imageUrl("https://res.cloudinary.com/x/image/upload/old.webp").build();
        when(triviaRepository.findById(1L)).thenReturn(Optional.of(existing));

        service.delete(1L);

        verify(imageStorageService).delete("https://res.cloudinary.com/x/image/upload/old.webp");
        verify(triviaRepository).delete(existing);
    }

    @Test
    void update_replacesImageAndStoresTranslations() {
        Trivia existing = Trivia.builder().id(1L).itemId(45L).itemType(TriviaItemType.MOVIE)
                .content("x").tags(new java.util.ArrayList<>(List.of("LORE"))).imageUrl("https://res.cloudinary.com/x/image/upload/old.webp").build();
        when(triviaRepository.findById(1L)).thenReturn(Optional.of(existing));

        var response = service.update(1L, request(TriviaItemType.MOVIE, false));

        verify(imageStorageService).deleteIfChanged("https://res.cloudinary.com/x/image/upload/old.webp",
                "https://res.cloudinary.com/x/image/upload/new.webp");
        assertThat(response.imageUrl()).endsWith("new.webp");
        assertThat(response.titleTr()).isNull(); // boş string null'a çevrilir
        assertThat(response.contentTr()).isEqualTo("Çöl sahneleri Ürdün'de çekildi.");
    }

    @Test
    void create_mergesTagAndTags_normalizesPredefinedKeepsCustom() {
        when(movieService.existsById(45L)).thenReturn(true);
        when(triviaRepository.save(any(Trivia.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.create(request(TriviaItemType.MOVIE, false), 7L);

        // "behind the scenes" → hazır değere, "  lore " → LORE, "Cinematography" serbest olarak kalır; sıra korunur
        assertThat(response.tags()).containsExactly("BEHIND_THE_SCENES", "Cinematography", "LORE");
        assertThat(response.tag()).isEqualTo("BEHIND_THE_SCENES");
    }

    @Test
    void create_dedupesCaseInsensitiveAndRejectsMoreThanFive() {
        when(movieService.existsById(45L)).thenReturn(true);
        when(triviaRepository.save(any(Trivia.class))).thenAnswer(inv -> inv.getArgument(0));
        var dup = new TriviaRequest(45L, TriviaItemType.MOVIE, null, null, "c", null, null,
                "Lore", List.of("LORE", "lore", "Music"), false, null);

        assertThat(service.create(dup, 1L).tags()).containsExactly("LORE", "Music");

        var tooMany = new TriviaRequest(45L, TriviaItemType.MOVIE, null, null, "c", null, null,
                "a", List.of("b", "c", "d", "e", "f"), false, null);
        assertThatThrownBy(() -> service.create(tooMany, 1L)).isInstanceOf(InvalidReferenceException.class);
    }

    @Test
    void create_requiresAtLeastOneTag() {
        when(movieService.existsById(45L)).thenReturn(true);
        var none = new TriviaRequest(45L, TriviaItemType.MOVIE, null, null, "c", null, null,
                "  ", List.of(), false, null);

        assertThatThrownBy(() -> service.create(none, 1L)).isInstanceOf(InvalidReferenceException.class);
    }
}
